package br.com.mapae;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Subir o contexto já prova que o Flyway aplicou as migrações e que o Hibernate validou o mapeamento.
 */
@SpringBootTest
@Import(BancoDeTesteConfig.class)
class MapaeApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void postgisFicaNoSchemaExtensions() {
        String schema = jdbc.queryForObject("""
                SELECT n.nspname FROM pg_extension e
                JOIN pg_namespace n ON n.oid = e.extnamespace
                WHERE e.extname = 'postgis'
                """, String.class);
        assertThat(schema).isEqualTo("extensions");
    }

    @Test
    void posicaoEhGeographyPointEmWgs84() {
        String tipo = jdbc.queryForObject("""
                SELECT format_type(a.atttypid, a.atttypmod) FROM pg_attribute a
                WHERE a.attrelid = 'public.local'::regclass AND a.attname = 'posicao'
                """, String.class);
        assertThat(tipo).isEqualTo("geography(Point,4326)");
    }

    @Test
    void tabelaLocalTemRlsLigadoESemPoliticas() {
        Boolean rls = jdbc.queryForObject(
                "SELECT relrowsecurity FROM pg_class WHERE oid = 'public.local'::regclass", Boolean.class);
        Integer politicas = jdbc.queryForObject(
                "SELECT count(*) FROM pg_policy WHERE polrelid = 'public.local'::regclass", Integer.class);
        assertThat(rls).isTrue();
        assertThat(politicas).isZero();
    }

    @Test
    void todaTabelaDoPublicTemRlsLigado() {
        List<String> semRls = jdbc.queryForList("""
                SELECT c.relname FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relkind = 'r' AND NOT c.relrowsecurity
                """, String.class);
        assertThat(semRls).isEmpty();
    }

    @Test
    void historicoDoFlywayFicaForaDoPublic() {
        List<String> schemas = jdbc.queryForList(
                "SELECT schemaname FROM pg_tables WHERE tablename = 'flyway_schema_history'", String.class);
        assertThat(schemas).containsExactly("flyway");
    }
}
