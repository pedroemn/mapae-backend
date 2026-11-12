package br.com.mapae.locais.dominio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * O mapa e a pesquisa só mostram o catálogo ativo: local com {@code ativo = true}, com posição e,
 * se for conta de loja, com status ATIVO (loja bloqueada ou inativa sai do mapa).
 * Todos os textos de busca entram como parâmetro; nada do usuário é concatenado ao SQL.
 */
public interface LocalRepository extends JpaRepository<Local, Long> {

    /**
     * Regra única do catálogo ativo (AD-040), usada por todas as consultas do mapa e da pesquisa.
     * Local sem conta tem status nulo; conta de loja só entra com status ATIVO.
     */
    String CATALOGO_ATIVO = " l.ativo AND (l.status IS NULL OR l.status = 'ATIVO') ";

    /** Campos do marcador, iguais no mapa e na pesquisa. */
    String MARCADOR = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   l.categoria AS "categoria",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude"
            """;

    String NA_AREA = """
            FROM local l
            WHERE """ + CATALOGO_ATIVO + """
              AND CAST(l.posicao AS geometry) && ST_MakeEnvelope(:oeste, :sul, :leste, :norte, 4326)
            """;

    /** Sem acento e em minúsculas; abre com este prefixo e fecha com "))". */
    String SEM_ACENTO = "lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), ";

    String NOME_NORMAL = SEM_ACENTO + "l.nome))";

    /** Nome e termos do local, onde as palavras da busca são procuradas. */
    String TEXTO_NORMAL = SEM_ACENTO + "l.nome || ' ' || array_to_string(l.termos, ' ')))";

    /** Cada palavra da busca, já escapada para LIKE, com os códigos das categorias que ela cita. */
    String PALAVRAS = " unnest(CAST(:palavras AS text[]), CAST(:categoriasDasPalavras AS text[])) AS p(palavra, categorias) ";

    String PALAVRA = SEM_ACENTO + "p.palavra))";

    /**
     * A palavra está no nome ou nos termos, ou cita a categoria do local. O IS NOT NULL evita que
     * uma categoria nula vire NULL no NOT EXISTS e deixe passar um local que não casa.
     */
    String CASA_EXATO = "(" + TEXTO_NORMAL + " LIKE '%' || " + PALAVRA + " || '%' ESCAPE '\\'"
            + " OR (l.categoria IS NOT NULL AND l.categoria = ANY(string_to_array(p.categorias, ' '))))";

    /** Erro leve de digitação, só em palavras de 4 letras ou mais (AD-049). */
    String CASA_PARECIDO = "(" + PALAVRA + " ~ '^[a-z0-9]{4,}$'"
            + " AND extensions.strict_word_similarity(" + PALAVRA + ", " + TEXTO_NORMAL + ") >= 0.5)";

    String ORIGEM = "CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)";

    /**
     * Aberto no horário de Aracaju recebido em :agora. Abertura inclusiva, fechamento exclusivo;
     * fecha antes de abrir vira a meia-noite; abre = fecha é 24 horas; sem horário é desconhecido (nulo).
     */
    String ABERTO = """
            CASE WHEN l.abre IS NULL THEN NULL
                 WHEN l.abre = l.fecha THEN true
                 WHEN l.abre < l.fecha THEN CAST(:agora AS time) >= l.abre AND CAST(:agora AS time) < l.fecha
                 ELSE CAST(:agora AS time) >= l.abre OR CAST(:agora AS time) < l.fecha
            END""";

    /**
     * Pesquisa com filtros (BACK-023 e BACK-024). Lista vazia não filtra. Todos os textos entram
     * como parâmetro; o unaccent e o pg_trgm vão qualificados, e as funções do PostGIS dependem
     * do search_path da conexão (AD-017).
     */
    String PESQUISA = """
            FROM local l
            WHERE """ + CATALOGO_ATIVO + """
              AND l.posicao IS NOT NULL
              AND (cardinality(CAST(:categorias AS text[])) = 0 OR l.categoria = ANY(CAST(:categorias AS text[])))
              AND (cardinality(CAST(:precos AS int[])) = 0 OR l.faixa_preco = ANY(CAST(:precos AS int[])))
              AND (NOT CAST(:soAbertos AS boolean) OR (""" + ABERTO + """
            ) IS TRUE)
              AND (NOT CAST(:comRaio AS boolean) OR ST_DWithin(l.posicao, """ + ORIGEM + """
            , :raioMetros))
              AND NOT EXISTS (SELECT 1 FROM""" + PALAVRAS + "WHERE NOT (" + CASA_EXATO + " OR " + CASA_PARECIDO + """
            ))
            """;

    /**
     * Com termo: nome começa com ele; nome contém; todas as palavras casam sem semelhança;
     * alguma só casa por semelhança (ACHAR-06). Sem termo, todos empatam.
     */
    String RELEVANCIA = "CASE WHEN CAST(:termoNormal AS text) = '' THEN 0"
            + " WHEN " + NOME_NORMAL + " LIKE " + SEM_ACENTO + "CAST(:termoNormal AS text))) || '%' ESCAPE '\\' THEN 0"
            + " WHEN " + NOME_NORMAL + " LIKE '%' || " + SEM_ACENTO + "CAST(:termoNormal AS text))) || '%' ESCAPE '\\' THEN 1"
            + " WHEN NOT EXISTS (SELECT 1 FROM" + PALAVRAS + "WHERE NOT " + CASA_EXATO + ") THEN 2"
            + " ELSE 3 END";

    String ITEM_PESQUISA = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   l.categoria AS "categoria",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   CAST(l.faixa_preco AS integer) AS "faixaPreco",
                   (""" + ABERTO + """
            ) AS "abertoAgora",
                   CASE WHEN CAST(:comOrigem AS boolean) THEN ST_Distance(l.posicao, """ + ORIGEM + """
            ) END AS "distanciaMetros"
            """;

    /** Username e email da loja são gravados em minúsculas; o parâmetro já chega em minúsculas. */
    @Query("SELECT l FROM Local l WHERE l.acesso.username = :login OR l.acesso.email = :login")
    Optional<Local> buscarLojaPorLogin(@Param("login") String login);

    boolean existsByAcessoUsername(String username);

    boolean existsByAcessoEmail(String email);

    @Query(value = MARCADOR + NA_AREA + "ORDER BY l.id LIMIT :limite", nativeQuery = true)
    List<LocalNaArea> buscarNaArea(@Param("oeste") double oeste,
                                   @Param("sul") double sul,
                                   @Param("leste") double leste,
                                   @Param("norte") double norte,
                                   @Param("limite") int limite);

    @Query(value = MARCADOR + NA_AREA + "ORDER BY l.id",
            countQuery = "SELECT count(*) " + NA_AREA, nativeQuery = true)
    Page<LocalNaArea> buscarNaArea(@Param("oeste") double oeste,
                                   @Param("sul") double sul,
                                   @Param("leste") double leste,
                                   @Param("norte") double norte,
                                   Pageable pagina);

    /**
     * @param palavras             palavras da busca, normalizadas e escapadas para LIKE; vazia sem termo
     * @param categoriasDasPalavras para cada palavra, os códigos de categoria que ela cita, separados por espaço
     * @param termoNormal          o termo inteiro, normalizado e escapado; vazio sem termo
     * @param agora                hora atual em Aracaju, "HH:mm:ss". Vai como texto: um LocalTime seria
     *                             convertido pelo fuso da conexão JDBC e mudaria de hora
     */
    @Query(value = ITEM_PESQUISA + PESQUISA
            + "ORDER BY " + RELEVANCIA + ", \"distanciaMetros\", " + NOME_NORMAL + ", l.id",
            countQuery = "SELECT count(*) " + PESQUISA, nativeQuery = true)
    Page<ItemPesquisa> pesquisar(@Param("palavras") String[] palavras,
                                 @Param("categoriasDasPalavras") String[] categoriasDasPalavras,
                                 @Param("termoNormal") String termoNormal,
                                 @Param("categorias") String[] categorias,
                                 @Param("precos") Integer[] precos,
                                 @Param("soAbertos") boolean soAbertos,
                                 @Param("agora") String agora,
                                 @Param("comOrigem") boolean comOrigem,
                                 @Param("latitude") double latitude,
                                 @Param("longitude") double longitude,
                                 @Param("comRaio") boolean comRaio,
                                 @Param("raioMetros") double raioMetros,
                                 Pageable pagina);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   l.categoria AS "categoria",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   CAST(NULL AS double precision) AS "distanciaMetros"
            FROM local l
            WHERE """ + CATALOGO_ATIVO + """
              AND l.posicao IS NOT NULL
            ORDER BY l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> listar(@Param("limite") int limite);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   l.categoria AS "categoria",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   ST_Distance(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distanciaMetros"
            FROM local l
            WHERE """ + CATALOGO_ATIVO + """
              AND l.posicao IS NOT NULL
            ORDER BY "distanciaMetros", l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> buscarPorDistancia(@Param("latitude") double latitude,
                                          @Param("longitude") double longitude,
                                          @Param("limite") int limite);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   l.categoria AS "categoria",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   ST_Distance(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distanciaMetros"
            FROM local l
            WHERE """ + CATALOGO_ATIVO + """
              AND ST_DWithin(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), :raioMetros)
            ORDER BY "distanciaMetros", l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> buscarProximos(@Param("latitude") double latitude,
                                      @Param("longitude") double longitude,
                                      @Param("raioMetros") double raioMetros,
                                      @Param("limite") int limite);

    /** Marcador do mapa e item da pesquisa. */
    interface LocalNaArea {
        Long getId();

        String getNome();

        /** Nula enquanto o local não tem categoria no catálogo. */
        String getCategoria();

        double getLatitude();

        double getLongitude();
    }

    /** Item da pesquisa: o marcador mais preço, situação de agora e distância (nulos quando desconhecidos). */
    interface ItemPesquisa extends LocalNaArea {
        Integer getFaixaPreco();

        Boolean getAbertoAgora();

        Double getDistanciaMetros();
    }

    interface LocalProximo extends LocalNaArea {
        /** Nulo quando a consulta não informou coordenadas. */
        Double getDistanciaMetros();
    }
}
