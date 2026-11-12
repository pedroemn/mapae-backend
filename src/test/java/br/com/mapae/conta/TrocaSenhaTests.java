package br.com.mapae.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** PUT /api/v1/contas/eu/senha (spec conta-e-login: CONTA-27 e CONTA-28). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class TrocaSenhaTests {

    static final String SENHA = "senha-forte-1";
    static final String NOVA = "outra-senha-2";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager em;

    @Autowired
    PasswordEncoder senhas;

    String tokenDeUsuario() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ana.souza","nome":"Ana","dataNascimento":"2000-05-10",
                         "email":"ana@exemplo.com","cep":"49000000","genero":"FEMININO","senha":"%s"}"""
                        .formatted(SENHA)))
                .andExpect(status().isCreated());
        return "Bearer " + JsonPath.read(entrar("/api/v1/auth/usuarios/login", "ana.souza", SENHA)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    String tokenDeLoja() throws Exception {
        mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"bar.do.ze","nome":"Bar","email":"contato@bardoze.com","cep":"49010000",
                         "senha":"%s"}""".formatted(SENHA)))
                .andExpect(status().isCreated());
        return "Bearer " + JsonPath.read(entrar("/api/v1/auth/lojas/login", "bar.do.ze", SENHA)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    ResultActions entrar(String rota, String login, String senha) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)));
    }

    ResultActions trocar(String token, String atual, String nova) throws Exception {
        return mvc.perform(put("/api/v1/contas/eu/senha").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"%s\",\"novaSenha\":\"%s\"}".formatted(atual, nova)));
    }

    String hash(String tabela) {
        em.flush();
        return jdbc.queryForObject("SELECT senha_hash FROM " + tabela, String.class);
    }

    // CONTA-27: usuário
    @Test
    void usuarioTrocaASenhaComASenhaAtualCerta() throws Exception {
        String token = tokenDeUsuario();
        em.flush();
        jdbc.update("UPDATE usuario SET atualizado_em = ?", Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));
        em.clear();

        trocar(token, SENHA, NOVA).andExpect(status().isNoContent());

        String novoHash = hash("usuario");
        assertThat(novoHash).startsWith("$2");
        assertThat(senhas.matches(NOVA, novoHash)).isTrue();
        Instant atualizadoEm = jdbc.queryForObject("SELECT atualizado_em FROM usuario", Timestamp.class).toInstant();
        assertThat(atualizadoEm).isAfter(Instant.now().minusSeconds(60));
        entrar("/api/v1/auth/usuarios/login", "ana.souza", NOVA).andExpect(status().isOk());
        entrar("/api/v1/auth/usuarios/login", "ana.souza", SENHA).andExpect(status().isUnauthorized());
    }

    // CONTA-27: loja
    @Test
    void lojaTrocaASenhaComASenhaAtualCerta() throws Exception {
        String token = tokenDeLoja();
        trocar(token, SENHA, NOVA).andExpect(status().isNoContent());
        assertThat(senhas.matches(NOVA, hash("local"))).isTrue();
        entrar("/api/v1/auth/lojas/login", "bar.do.ze", NOVA).andExpect(status().isOk());
        entrar("/api/v1/auth/lojas/login", "bar.do.ze", SENHA).andExpect(status().isUnauthorized());
    }

    // CONTA-28
    @Test
    void senhaAtualErradaResponde400SemMudarOHash() throws Exception {
        String token = tokenDeUsuario();
        String antes = hash("usuario");
        trocar(token, "senha-errada", NOVA).andExpect(status().isBadRequest());
        assertThat(hash("usuario")).isEqualTo(antes);
        entrar("/api/v1/auth/usuarios/login", "ana.souza", SENHA).andExpect(status().isOk());
    }

    // A nova senha segue a mesma regra do cadastro (8 a 72 caracteres)
    @Test
    void novaSenhaCurtaResponde400ListandoOCampo() throws Exception {
        String token = tokenDeUsuario();
        trocar(token, SENHA, "curta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.novaSenha").exists());
    }

    // A nova senha também cabe em 72 bytes (antes dava 500)
    @Test
    void novaSenhaAcentuadaAcimaDe72BytesResponde400ListandoOCampo() throws Exception {
        String token = tokenDeUsuario();
        trocar(token, SENHA, "é".repeat(37)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.novaSenha").exists());
    }

    // CONTA-28: senha atual errada e acima de 72 bytes continua sendo só senha errada
    @Test
    void senhaAtualAcimaDe72BytesResponde400SemMudarOHash() throws Exception {
        String token = tokenDeUsuario();
        String antes = hash("usuario");
        trocar(token, "é".repeat(40), NOVA).andExpect(status().isBadRequest());
        assertThat(hash("usuario")).isEqualTo(antes);
    }

    // CONTA-28: o BCrypt só lê os primeiros 72 bytes. Sem a guarda de tamanho, a senha atual
    // de 72 bytes seguida de qualquer sufixo seria aceita.
    @Test
    void senhaAtualDe72BytesComSufixoResponde400SemMudarOHash() throws Exception {
        String token = tokenDeUsuario();
        String senha72 = "a".repeat(72);
        trocar(token, SENHA, senha72).andExpect(status().isNoContent());
        String antes = hash("usuario");
        trocar(token, senha72 + "b", NOVA).andExpect(status().isBadRequest());
        assertThat(hash("usuario")).isEqualTo(antes);
        entrar("/api/v1/auth/usuarios/login", "ana.souza", senha72).andExpect(status().isOk());
    }

    // CONTA-25 na troca de senha
    @Test
    void trocaSemTokenResponde401() throws Exception {
        mvc.perform(put("/api/v1/contas/eu/senha").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"a\",\"novaSenha\":\"b\"}"))
                .andExpect(status().isUnauthorized());
    }
}
