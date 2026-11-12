package br.com.mapae.locais;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Colunas do catálogo pesquisável (spec locais-mapa-e-pesquisa: CAT-01 a CAT-03). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class CatalogoSchemaTests {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    PostgreSQLContainer banco;

    private Map<String, Object> coluna(String nome) {
        return jdbc.queryForMap("""
                SELECT data_type, udt_name, is_nullable, column_default, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'local' AND column_name = ?""", nome);
    }

    // CAT-01
    @Test
    void localGanhaCategoriaTermosEAtivo() {
        assertThat(coluna("categoria")).containsEntry("data_type", "character varying")
                .containsEntry("character_maximum_length", 50).containsEntry("is_nullable", "YES");
        assertThat(coluna("termos")).containsEntry("udt_name", "_text").containsEntry("is_nullable", "NO")
                .containsEntry("column_default", "'{}'::text[]");
        assertThat(coluna("ativo")).containsEntry("data_type", "boolean").containsEntry("is_nullable", "NO")
                .containsEntry("column_default", "true");
    }

    // CAT-01
    @Test
    void unaccentFicaNoSchemaExtensions() {
        String schema = jdbc.queryForObject("""
                SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace
                WHERE e.extname = 'unaccent'""", String.class);
        assertThat(schema).isEqualTo("extensions");
        assertThat(jdbc.queryForObject(
                "SELECT extensions.unaccent(CAST('extensions.unaccent' AS regdictionary), 'Café São João')",
                String.class)).isEqualTo("Cafe Sao Joao");
    }

    // CAT-03
    @Test
    void lojaCadastradaNasceAtivaComTermosVazios() throws Exception {
        mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"bar.do.ze","nome":"Bar do Zé","email":"contato@bardoze.com","cep":"49010000",
                         "senha":"senha-forte-1"}"""))
                .andExpect(status().isCreated());
        Map<String, Object> loja = jdbc.queryForMap(
                "SELECT ativo, cardinality(termos) AS termos, categoria FROM local WHERE username = 'bar.do.ze'");
        assertThat(loja).containsEntry("ativo", true).containsEntry("termos", 0).containsEntry("categoria", null);
    }

    // CAT-02: a migração roda sobre um banco que já tinha locais
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void migracaoPreservaLocaisExistentesComoAtivos() {
        jdbc.execute("DROP DATABASE IF EXISTS catalogo_legado");
        jdbc.execute("CREATE DATABASE catalogo_legado");
        String url = banco.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/catalogo_legado$1");
        DriverManagerDataSource fonte = new DriverManagerDataSource(url, banco.getUsername(), banco.getPassword());
        JdbcTemplate legado = new JdbcTemplate(fonte);

        Flyway.configure().dataSource(fonte).defaultSchema("flyway").target("20261111103000").load().migrate();
        legado.update("INSERT INTO public.local (nome, posicao) VALUES "
                + "('Antigo', extensions.ST_GeogFromText('SRID=4326;POINT(-37.05 -10.9)'))");

        Flyway.configure().dataSource(fonte).defaultSchema("flyway").load().migrate();

        List<Map<String, Object>> linhas = legado.queryForList(
                "SELECT nome, ativo, cardinality(termos) AS termos, categoria FROM public.local");
        assertThat(linhas).hasSize(1);
        assertThat(linhas.getFirst()).containsEntry("nome", "Antigo").containsEntry("ativo", true)
                .containsEntry("termos", 0).containsEntry("categoria", null);
    }
}
