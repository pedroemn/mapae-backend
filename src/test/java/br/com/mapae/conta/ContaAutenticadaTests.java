package br.com.mapae.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** GET /api/v1/contas/eu e conclusão do primeiro acesso (spec conta-e-login: CONTA-22 a CONTA-25). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class ContaAutenticadaTests {

    static final String SENHA = "senha-forte-1";
    static final Instant ANTIGO = Instant.parse("2020-01-01T00:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager em;

    String tokenDeUsuario() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ana.souza","nome":"Ana Souza","dataNascimento":"2000-05-10",
                         "email":"ana@exemplo.com","cep":"49000-000","genero":"FEMININO","bio":"Oi",
                         "senha":"%s"}""".formatted(SENHA)))
                .andExpect(status().isCreated());
        return entrar("/api/v1/auth/usuarios/login", "ana.souza");
    }

    String tokenDeLoja() throws Exception {
        mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"bar.do.ze","nome":"Bar do Zé","email":"contato@bardoze.com",
                         "cep":"49010000","tipo":"bar","senha":"%s"}""".formatted(SENHA)))
                .andExpect(status().isCreated());
        return entrar("/api/v1/auth/lojas/login", "bar.do.ze");
    }

    String entrar(String rota, String login) throws Exception {
        String resposta = mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, SENHA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    /** Lê do banco o que a requisição gravou na transação do teste. */
    Instant atualizadoEm(String tabela, String username) {
        em.flush();
        return jdbc.queryForObject("SELECT atualizado_em FROM " + tabela + " WHERE username = ?",
                Timestamp.class, username).toInstant();
    }

    void envelhecer(String tabela, String username) {
        em.flush();
        jdbc.update("UPDATE " + tabela + " SET atualizado_em = ? WHERE username = ?", Timestamp.from(ANTIGO), username);
        em.clear();
    }

    // CONTA-22: usuário
    @Test
    void usuarioVeOsDadosDaPropriaContaSemSenha() throws Exception {
        String token = tokenDeUsuario();
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("ana.souza"))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.cep").value("49000000"))
                .andExpect(jsonPath("$.dataNascimento").value("2000-05-10"))
                .andExpect(jsonPath("$.genero").value("FEMININO"))
                .andExpect(jsonPath("$.bio").value("Oi"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("USUARIO"))
                .andExpect(jsonPath("$.criadoEm").isString())
                .andExpect(jsonPath("$.atualizadoEm").isString())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("senhaHash"))));
    }

    // CONTA-22: loja
    @Test
    void lojaVeOsDadosDaPropriaContaSemSenha() throws Exception {
        String token = tokenDeLoja();
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bar.do.ze"))
                .andExpect(jsonPath("$.nome").value("Bar do Zé"))
                .andExpect(jsonPath("$.email").value("contato@bardoze.com"))
                .andExpect(jsonPath("$.cep").value("49010000"))
                .andExpect(jsonPath("$.tipo").value("bar"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("LOJA"))
                .andExpect(jsonPath("$.criadoEm").isString())
                .andExpect(jsonPath("$.atualizadoEm").isString())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("senhaHash"))))
                .andExpect(jsonPath("$", not(hasKey("dataNascimento"))));
    }

    // CONTA-23: usuário
    @Test
    void usuarioConcluiOPrimeiroAcesso() throws Exception {
        String token = tokenDeUsuario();
        envelhecer("usuario", "ana.souza");
        mvc.perform(post("/api/v1/contas/eu/primeiro-acesso/conclusao").header("Authorization", token))
                .andExpect(status().isNoContent());
        assertThat(atualizadoEm("usuario", "ana.souza")).isAfter(Instant.now().minusSeconds(60));
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", token))
                .andExpect(jsonPath("$.primeiroAcesso").value(false));
        String login = mvc.perform(post("/api/v1/auth/usuarios/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana.souza\",\"senha\":\"%s\"}".formatted(SENHA)))
                .andReturn().getResponse().getContentAsString();
        assertThat((Boolean) JsonPath.read(login, "$.primeiroAcesso")).isFalse();
    }

    // CONTA-23: loja
    @Test
    void lojaConcluiOPrimeiroAcesso() throws Exception {
        String token = tokenDeLoja();
        envelhecer("local", "bar.do.ze");
        mvc.perform(post("/api/v1/contas/eu/primeiro-acesso/conclusao").header("Authorization", token))
                .andExpect(status().isNoContent());
        assertThat(atualizadoEm("local", "bar.do.ze")).isAfter(Instant.now().minusSeconds(60));
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", token))
                .andExpect(jsonPath("$.primeiroAcesso").value(false));
    }

    // CONTA-24
    @Test
    void concluirDeNovoResponde204SemMudarAtualizadoEm() throws Exception {
        String token = tokenDeUsuario();
        mvc.perform(post("/api/v1/contas/eu/primeiro-acesso/conclusao").header("Authorization", token))
                .andExpect(status().isNoContent());
        envelhecer("usuario", "ana.souza");
        mvc.perform(post("/api/v1/contas/eu/primeiro-acesso/conclusao").header("Authorization", token))
                .andExpect(status().isNoContent());
        assertThat(atualizadoEm("usuario", "ana.souza")).isEqualTo(ANTIGO);
    }

    // CONTA-25 na conclusão do primeiro acesso
    @Test
    void conclusaoSemTokenResponde401() throws Exception {
        mvc.perform(post("/api/v1/contas/eu/primeiro-acesso/conclusao"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // Token válido de uma conta que deixou de existir
    @Test
    void tokenDeContaApagadaResponde401() throws Exception {
        String token = tokenDeUsuario();
        em.flush();
        jdbc.update("DELETE FROM usuario WHERE username = 'ana.souza'");
        em.clear();
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
