package br.com.mapae.locais.dominio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalRepository extends JpaRepository<Local, Long> {

    /** Username e email da loja são gravados em minúsculas; o parâmetro já chega em minúsculas. */
    @Query("SELECT l FROM Local l WHERE l.acesso.username = :login OR l.acesso.email = :login")
    Optional<Local> buscarLojaPorLogin(@Param("login") String login);

    boolean existsByAcessoUsername(String username);

    boolean existsByAcessoEmail(String email);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude"
            FROM local l
            WHERE CAST(l.posicao AS geometry) && ST_MakeEnvelope(:oeste, :sul, :leste, :norte, 4326)
            ORDER BY l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalNaArea> buscarNaArea(@Param("oeste") double oeste,
                                   @Param("sul") double sul,
                                   @Param("leste") double leste,
                                   @Param("norte") double norte,
                                   @Param("limite") int limite);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   CAST(NULL AS double precision) AS "distanciaMetros"
            FROM local l
            WHERE l.posicao IS NOT NULL
            ORDER BY l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> listar(@Param("limite") int limite);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   ST_Distance(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distanciaMetros"
            FROM local l
            WHERE l.posicao IS NOT NULL
            ORDER BY "distanciaMetros", l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> buscarPorDistancia(@Param("latitude") double latitude,
                                          @Param("longitude") double longitude,
                                          @Param("limite") int limite);

    @Query(value = """
            SELECT l.id AS "id",
                   l.nome AS "nome",
                   ST_Y(CAST(l.posicao AS geometry)) AS "latitude",
                   ST_X(CAST(l.posicao AS geometry)) AS "longitude",
                   ST_Distance(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)) AS "distanciaMetros"
            FROM local l
            WHERE ST_DWithin(l.posicao, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), :raioMetros)
            ORDER BY "distanciaMetros", l.id
            LIMIT :limite
            """, nativeQuery = true)
    List<LocalProximo> buscarProximos(@Param("latitude") double latitude,
                                      @Param("longitude") double longitude,
                                      @Param("raioMetros") double raioMetros,
                                      @Param("limite") int limite);

    interface LocalNaArea {
        Long getId();

        String getNome();

        double getLatitude();

        double getLongitude();
    }

    interface LocalProximo extends LocalNaArea {
        /** Nulo quando a consulta não informou coordenadas. */
        Double getDistanciaMetros();
    }
}
