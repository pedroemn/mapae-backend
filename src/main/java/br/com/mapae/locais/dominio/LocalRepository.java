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

    /**
     * Sem diferenciar maiúsculas nem acentos. O unaccent e o dicionário vão qualificados; as funções
     * do PostGIS, como no resto do repositório, dependem do search_path da conexão (AD-017).
     * O termo chega com %, _ e \ já escapados, e casa como trecho.
     */
    String PESQUISA = """
            FROM local l
            CROSS JOIN (SELECT lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), CAST(:termo AS text))) AS termo,
                               lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), CAST(:categoria AS text))) AS categoria) p
            WHERE """ + CATALOGO_ATIVO + """
              AND l.posicao IS NOT NULL
              AND (p.categoria IS NULL
                   OR lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), l.categoria)) = p.categoria)
              AND (p.termo IS NULL
                   OR lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), l.nome)) LIKE '%' || p.termo || '%' ESCAPE '\\'
                   OR lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), l.categoria)) LIKE '%' || p.termo || '%' ESCAPE '\\'
                   OR EXISTS (SELECT 1 FROM unnest(l.termos) AS t(termo)
                              WHERE lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), t.termo)) LIKE '%' || p.termo || '%' ESCAPE '\\'))
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

    /** @param termo nulo para não filtrar; já escapado para LIKE */
    @Query(value = MARCADOR + PESQUISA
            + "ORDER BY lower(extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), l.nome)), l.id",
            countQuery = "SELECT count(*) " + PESQUISA, nativeQuery = true)
    Page<LocalNaArea> pesquisar(@Param("termo") String termo,
                                @Param("categoria") String categoria,
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

    interface LocalProximo extends LocalNaArea {
        /** Nulo quando a consulta não informou coordenadas. */
        Double getDistanciaMetros();
    }
}
