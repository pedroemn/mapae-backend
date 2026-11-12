package br.com.mapae.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import br.com.mapae.conta.dominio.Genero;
import br.com.mapae.conta.dominio.Usuario;
import br.com.mapae.conta.dominio.UsuarioRepository;
import br.com.mapae.locais.dominio.Local;
import br.com.mapae.locais.dominio.LocalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Tabelas de conta (spec conta-e-login: CONTA-01 a CONTA-06 e CONTA-13). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class ContasSchemaTests {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    LocalRepository locais;

    @Autowired
    PostgreSQLContainer banco;

    @Autowired
    MockMvc mvc;

    private Map<String, String> colunas(String tabela) {
        return jdbc.queryForList("""
                SELECT column_name, data_type || ':' || is_nullable AS tipo
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                """, tabela).stream()
                .collect(Collectors.toMap(c -> (String) c.get("column_name"), c -> (String) c.get("tipo")));
    }

    // CONTA-01
    @Test
    void tabelaUsuarioTemAsColunasDaSpec() {
        assertThat(colunas("usuario")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("id", "bigint:NO"),
                Map.entry("username", "character varying:NO"),
                Map.entry("nome", "character varying:NO"),
                Map.entry("data_nascimento", "date:NO"),
                Map.entry("email", "character varying:NO"),
                Map.entry("cep", "character varying:NO"),
                Map.entry("genero", "character varying:NO"),
                Map.entry("bio", "character varying:YES"),
                Map.entry("senha_hash", "character varying:NO"),
                Map.entry("primeiro_acesso", "boolean:NO"),
                Map.entry("status", "character varying:NO"),
                Map.entry("tipo_perfil", "character varying:NO"),
                Map.entry("criado_em", "timestamp with time zone:NO"),
                Map.entry("atualizado_em", "timestamp with time zone:NO")));
    }

    // CONTA-02
    @Test
    void tabelaLocalGanhaColunasDeContaEPosicaoOpcional() {
        assertThat(colunas("local")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("id", "bigint:NO"),
                Map.entry("nome", "character varying:NO"),
                Map.entry("posicao", "USER-DEFINED:YES"),
                Map.entry("username", "character varying:YES"),
                Map.entry("email", "character varying:YES"),
                Map.entry("cep", "character varying:YES"),
                Map.entry("tipo", "character varying:YES"),
                Map.entry("senha_hash", "character varying:YES"),
                Map.entry("primeiro_acesso", "boolean:YES"),
                Map.entry("status", "character varying:YES"),
                Map.entry("tipo_perfil", "character varying:YES"),
                Map.entry("criado_em", "timestamp with time zone:YES"),
                Map.entry("atualizado_em", "timestamp with time zone:YES")));
    }

    // CONTA-05
    @Test
    void usuarioTemRlsLigadoESemPoliticas() {
        Boolean rls = jdbc.queryForObject(
                "SELECT relrowsecurity FROM pg_class WHERE oid = 'public.usuario'::regclass", Boolean.class);
        Integer politicas = jdbc.queryForObject(
                "SELECT count(*) FROM pg_policy WHERE polrelid = 'public.usuario'::regclass", Integer.class);
        assertThat(rls).isTrue();
        assertThat(politicas).isZero();
    }

    // CONTA-06: o contexto sobe com ddl-auto=validate e o mapeamento grava as colunas certas
    @Test
    void mapeamentoDoUsuarioGravaOsCampos() {
        Usuario salvo = usuarios.saveAndFlush(new Usuario("Ana", "Ana", LocalDate.of(2000, 1, 2), "Ana@X.com",
                "49000000", Genero.FEMININO, "oi", "$2a$10$hash"));
        Map<String, Object> linha = jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", salvo.getId());
        assertThat(linha).containsEntry("username", "ana").containsEntry("email", "ana@x.com")
                .containsEntry("genero", "FEMININO").containsEntry("primeiro_acesso", true)
                .containsEntry("status", "ATIVO").containsEntry("tipo_perfil", "USUARIO");
    }

    // CONTA-06
    @Test
    void mapeamentoDaLojaGravaOsCampos() {
        Local salvo = locais.saveAndFlush(Local.loja("Bar", "Bar", "Bar@X.com", "49000000", "bar", "$2a$10$hash"));
        Map<String, Object> linha = jdbc.queryForMap("SELECT * FROM local WHERE id = ?", salvo.getId());
        assertThat(linha).containsEntry("username", "bar").containsEntry("email", "bar@x.com")
                .containsEntry("tipo", "bar").containsEntry("posicao", null).containsEntry("primeiro_acesso", true)
                .containsEntry("status", "ATIVO").containsEntry("tipo_perfil", "LOJA");
    }

    private void inserirUsuario(String username, String email) {
        jdbc.update("""
                INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash)
                VALUES (?, 'N', DATE '2000-01-01', ?, '49000000', 'OUTRO', 'h')
                """, username, email);
    }

    // CONTA-04
    @Test
    void usernameDeUsuarioEhUnicoSemDiferenciarMaiusculas() {
        inserirUsuario("ana", "a1@x.com");
        assertThatThrownBy(() -> inserirUsuario("ANA", "a2@x.com"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_usuario_username");
    }

    // CONTA-04
    @Test
    void emailDeUsuarioEhUnicoSemDiferenciarMaiusculas() {
        inserirUsuario("ana", "ana@x.com");
        assertThatThrownBy(() -> inserirUsuario("bia", "ANA@X.COM"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_usuario_email");
    }

    private void inserirLoja(String username, String email) {
        jdbc.update("""
                INSERT INTO local (nome, username, email, cep, senha_hash, primeiro_acesso, status, tipo_perfil,
                                   criado_em, atualizado_em)
                VALUES ('L', ?, ?, '49000000', 'h', true, 'ATIVO', 'LOJA', now(), now())
                """, username, email);
    }

    // CONTA-04
    @Test
    void usernameDeLojaEhUnicoSemDiferenciarMaiusculas() {
        inserirLoja("bar", "b1@x.com");
        assertThatThrownBy(() -> inserirLoja("BAR", "b2@x.com"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_local_username");
    }

    // CONTA-04
    @Test
    void emailDeLojaEhUnicoSemDiferenciarMaiusculas() {
        inserirLoja("bar", "bar@x.com");
        assertThatThrownBy(() -> inserirLoja("pub", "BAR@X.COM"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_local_email");
    }

    // CONTA-13
    @Test
    void lojaSemPosicaoNaoApareceNasConsultasDoMapa() throws Exception {
        locais.saveAndFlush(Local.loja("bar", "Bar", "bar@x.com", "49000000", null, "h"));
        Local comPosicao = locais.saveAndFlush(new Local("No mapa", -10.9, -37.05));

        mvc.perform(get("/api/v1/locais/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(comPosicao.getId()));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", "-37.05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(comPosicao.getId()));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", "-37.05")
                        .param("raioMetros", "100000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(get("/api/v1/locais/area")
                        .param("oeste", "-180").param("sul", "-90").param("leste", "180").param("norte", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(comPosicao.getId()));
    }

    // CONTA-03: a migração roda sobre um banco que já tinha locais, sem perder nem alterar as linhas
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void migracaoPreservaLocaisQueJaExistiam() {
        jdbc.execute("DROP DATABASE IF EXISTS migracao_legada");
        jdbc.execute("CREATE DATABASE migracao_legada");
        String url = banco.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/migracao_legada$1");
        DriverManagerDataSource fonte = new DriverManagerDataSource(url, banco.getUsername(), banco.getPassword());
        JdbcTemplate legado = new JdbcTemplate(fonte);

        Flyway.configure().dataSource(fonte).defaultSchema("flyway").target("20261107022500").load().migrate();
        legado.update("INSERT INTO public.local (nome, posicao) VALUES "
                + "('Antigo', extensions.ST_GeogFromText('SRID=4326;POINT(-37.05 -10.9)'))");

        Flyway.configure().dataSource(fonte).defaultSchema("flyway").load().migrate();

        List<Map<String, Object>> linhas = legado.queryForList(
                "SELECT nome, extensions.ST_AsText(posicao) AS posicao, username, senha_hash FROM public.local");
        assertThat(linhas).hasSize(1);
        assertThat(linhas.getFirst()).containsEntry("nome", "Antigo")
                .containsEntry("posicao", "POINT(-37.05 -10.9)")
                .containsEntry("username", null).containsEntry("senha_hash", null);
    }
}
