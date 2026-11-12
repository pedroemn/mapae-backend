package br.com.mapae.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import br.com.mapae.compartilhado.seguranca.CredenciaisInvalidasException;
import br.com.mapae.compartilhado.seguranca.EmissorDeToken;
import br.com.mapae.compartilhado.seguranca.JwtPropriedades;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Login de usuário e de loja (spec conta-e-login: CONTA-16 a CONTA-21). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class LoginTests {

    static final String ROTA_USUARIO = "/api/v1/auth/usuarios/login";
    static final String ROTA_LOJA = "/api/v1/auth/lojas/login";
    static final String SENHA = "senha-forte-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JwtDecoder decoder;

    @Autowired
    JwtEncoder encoder;

    @Autowired
    EntityManager em;

    /** Muda o status direto no banco e limpa o cache do Hibernate da transação do teste. */
    void mudarStatus(String tabela, String username, String situacao) {
        jdbc.update("UPDATE " + tabela + " SET status = ? WHERE username = ?", situacao, username);
        em.clear();
    }

    long criarUsuario() throws Exception {
        String corpo = """
                {"username":"ana.souza","nome":"Ana","dataNascimento":"2000-05-10","email":"ana@exemplo.com",
                 "cep":"49000000","genero":"FEMININO","senha":"%s"}""".formatted(SENHA);
        String resposta = mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    long criarLoja() throws Exception {
        String corpo = """
                {"username":"bar.do.ze","nome":"Bar do Zé","email":"contato@bardoze.com","cep":"49010000",
                 "senha":"%s"}""".formatted(SENHA);
        String resposta = mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    ResultActions entrar(String rota, String login, String senha) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)));
    }

    Jwt tokenDa(ResultActions resposta) throws Exception {
        return decoder.decode(JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.token"));
    }

    // CONTA-16: email ou username, sem diferenciar maiúsculas
    @ParameterizedTest
    @ValueSource(strings = {"ana.souza", "ana@exemplo.com", "ANA.Souza", "Ana@Exemplo.COM"})
    void usuarioAtivoEntraComEmailOuUsername(String login) throws Exception {
        criarUsuario();
        entrar(ROTA_USUARIO, login, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("USUARIO"));
    }

    // CONTA-17
    @ParameterizedTest
    @ValueSource(strings = {"bar.do.ze", "contato@bardoze.com", "BAR.DO.ZE"})
    void lojaAtivaEntraComEmailOuUsername(String login) throws Exception {
        criarLoja();
        entrar(ROTA_LOJA, login, SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("LOJA"));
    }

    // CONTA-18: a mesma resposta para senha errada e login inexistente
    @Test
    void senhaErradaELoginInexistenteDao401ComAMesmaMensagem() throws Exception {
        criarUsuario();
        entrar(ROTA_USUARIO, "ana.souza", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM))
                .andExpect(jsonPath("$", not(hasKey("token"))));
        entrar(ROTA_USUARIO, "ninguem", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
    }

    // CONTA-18 na rota da loja
    @Test
    void senhaErradaDaLojaDa401() throws Exception {
        criarLoja();
        entrar(ROTA_LOJA, "bar.do.ze", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
        entrar(ROTA_LOJA, "nenhuma.loja", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
    }

    // CONTA-19
    @ParameterizedTest
    @ValueSource(strings = {"INATIVO", "BLOQUEADO"})
    void usuarioNaoAtivoComSenhaCertaRecebe403SemToken(String situacao) throws Exception {
        criarUsuario();
        mudarStatus("usuario", "ana.souza", situacao);
        entrar(ROTA_USUARIO, "ana.souza", SENHA)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.statusConta").value(situacao))
                .andExpect(jsonPath("$", not(hasKey("token"))));
    }

    // CONTA-19 na rota da loja
    @ParameterizedTest
    @ValueSource(strings = {"INATIVO", "BLOQUEADO"})
    void lojaNaoAtivaComSenhaCertaRecebe403SemToken(String situacao) throws Exception {
        criarLoja();
        mudarStatus("local", "bar.do.ze", situacao);
        entrar(ROTA_LOJA, "bar.do.ze", SENHA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusConta").value(situacao))
                .andExpect(jsonPath("$", not(hasKey("token"))));
    }

    // CONTA-19 só vale com a senha certa: conta bloqueada com senha errada continua 401
    @Test
    void contaBloqueadaComSenhaErradaDa401() throws Exception {
        criarUsuario();
        mudarStatus("usuario", "ana.souza", "BLOQUEADO");
        entrar(ROTA_USUARIO, "ana.souza", "senha-errada").andExpect(status().isUnauthorized());
    }

    // CONTA-20: usuário
    @Test
    void tokenDoUsuarioEhHs256ComIdNoSubEPerfilNaClaim() throws Exception {
        long id = criarUsuario();
        ResultActions resposta = entrar(ROTA_USUARIO, "ana.souza", SENHA).andExpect(status().isOk());
        Jwt token = tokenDa(resposta);
        assertThat(token.getHeaders()).containsEntry("alg", "HS256");
        assertThat(token.getSubject()).isEqualTo(Long.toString(id));
        assertThat(token.getClaimAsString("perfil")).isEqualTo("USUARIO");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofHours(24));
        Instant expiraEm = Instant.parse(JsonPath.read(resposta.andReturn().getResponse().getContentAsString(),
                "$.expiraEm"));
        assertThat(expiraEm).isEqualTo(token.getExpiresAt());
    }

    // CONTA-20: loja
    @Test
    void tokenDaLojaTemIdDaLojaEPerfilLoja() throws Exception {
        long id = criarLoja();
        Jwt token = tokenDa(entrar(ROTA_LOJA, "bar.do.ze", SENHA).andExpect(status().isOk()));
        assertThat(token.getSubject()).isEqualTo(Long.toString(id));
        assertThat(token.getClaimAsString("perfil")).isEqualTo("LOJA");
    }

    // CONTA-08 e CONTA-16: a resposta do login traz só estes campos, sem senha nem hash
    @Test
    void respostaDoLoginDoUsuarioTemSoOsCamposDaSpec() throws Exception {
        criarUsuario();
        String corpo = entrar(ROTA_USUARIO, "ana.souza", SENHA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> campos = JsonPath.read(corpo, "$");
        assertThat(campos.keySet()).containsExactlyInAnyOrder("token", "tipoToken", "expiraEm", "primeiroAcesso",
                "status", "tipoPerfil");
    }

    // CONTA-08 e CONTA-17
    @Test
    void respostaDoLoginDaLojaTemSoOsCamposDaSpec() throws Exception {
        criarLoja();
        String corpo = entrar(ROTA_LOJA, "bar.do.ze", SENHA).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> campos = JsonPath.read(corpo, "$");
        assertThat(campos.keySet()).containsExactlyInAnyOrder("token", "tipoToken", "expiraEm", "primeiroAcesso",
                "status", "tipoPerfil");
    }

    // CONTA-18: senha acima de 72 bytes nunca foi cadastrada, então é só senha errada (antes dava 500)
    @Test
    void senhaAcimaDe72BytesNoLoginDa401() throws Exception {
        criarUsuario();
        entrar(ROTA_USUARIO, "ana.souza", "é".repeat(40))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
        entrar(ROTA_USUARIO, "ninguem", "é".repeat(40))
                .andExpect(status().isUnauthorized());
    }

    // CONTA-18: o BCrypt só lê os primeiros 72 bytes. Sem a guarda de tamanho, a senha cadastrada
    // de 72 bytes seguida de qualquer sufixo entraria.
    @Test
    void senhaCadastradaDe72BytesComSufixoNaoEntra() throws Exception {
        String senha72 = "a".repeat(72);
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ana.souza","nome":"Ana","dataNascimento":"2000-05-10","email":"ana@exemplo.com",
                         "cep":"49000000","genero":"FEMININO","senha":"%s"}""".formatted(senha72)))
                .andExpect(status().isCreated());
        entrar(ROTA_USUARIO, "ana.souza", senha72 + "b")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM))
                .andExpect(jsonPath("$", not(hasKey("token"))));
        entrar(ROTA_USUARIO, "ana.souza", senha72).andExpect(status().isOk());
    }

    // CONTA-20: a validade vem da configuração, não é fixa em 24 h
    @Test
    void validadeDoTokenSegueAConfiguracao() {
        EmissorDeToken emissor = new EmissorDeToken(encoder,
                new JwtPropriedades("segredo-de-teste-com-mais-de-32-caracteres", Duration.ofHours(2)));
        Jwt token = decoder.decode(emissor.emitir(7, TipoPerfil.LOJA).token());
        assertThat(token.getSubject()).isEqualTo("7");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofHours(2));
    }

    // CONTA-21
    @Test
    void usuarioNaoEntraPelaRotaDaLoja() throws Exception {
        criarUsuario();
        entrar(ROTA_LOJA, "ana.souza", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
    }

    // CONTA-21
    @Test
    void lojaNaoEntraPelaRotaDoUsuario() throws Exception {
        criarLoja();
        entrar(ROTA_USUARIO, "bar.do.ze", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM));
    }

    // Borda: JSON malformado
    @Test
    void jsonMalformadoNoLoginResponde400() throws Exception {
        mvc.perform(post(ROTA_USUARIO).contentType(MediaType.APPLICATION_JSON).content("{\"login\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
