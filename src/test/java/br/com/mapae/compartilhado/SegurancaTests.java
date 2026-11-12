package br.com.mapae.compartilhado;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Fronteira de autenticação (spec conta-e-login: CONTA-25 e CONTA-26). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class SegurancaTests {

    static final String TOKEN_INVALIDO = "Token ausente, inválido ou expirado";

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtEncoder encoder;

    /** Cria um usuário de verdade, para que o único motivo de um 401 seja o próprio token. */
    private long criarUsuario() throws Exception {
        String resposta = mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"username\":\"ana\",\"nome\":\"Ana\",\"dataNascimento\":\"2000-01-01\","
                                + "\"email\":\"ana@x.com\",\"cep\":\"49000000\",\"genero\":\"OUTRO\","
                                + "\"senha\":\"senha-forte-1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private static String assinar(JwtEncoder encoder, long id, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(Long.toString(id)).claim("perfil", "USUARIO")
                .issuedAt(expiraEm.minusSeconds(3600)).expiresAt(expiraEm).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // CONTA-25
    @Test
    void rotaDaContaSemTokenResponde401EmProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/contas/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(TOKEN_INVALIDO));
    }

    // CONTA-25
    @Test
    void rotaDaContaComTokenMalformadoResponde401() throws Exception {
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // CONTA-25
    @Test
    void rotaDaContaComTokenExpiradoResponde401() throws Exception {
        long id = criarUsuario();
        // Controle: o mesmo token, ainda válido, entra
        mvc.perform(get("/api/v1/contas/eu").header("Authorization",
                        "Bearer " + assinar(encoder, id, Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk());
        String expirado = assinar(encoder, id, Instant.now().minusSeconds(120));
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(TOKEN_INVALIDO));
    }

    // CONTA-25
    @Test
    void rotaDaContaComTokenDeOutroSegredoResponde401() throws Exception {
        JwtEncoder outro = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "outro-segredo-com-mais-de-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        long id = criarUsuario();
        String falso = assinar(outro, id, Instant.now().plusSeconds(3600));
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(TOKEN_INVALIDO));
    }

    // CONTA-26
    @Test
    void consultasDoMapaContinuamPublicas() throws Exception {
        mvc.perform(get("/api/v1/locais/proximos")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/locais/area")
                        .param("oeste", "-37.1").param("sul", "-11").param("leste", "-37").param("norte", "-10.9"))
                .andExpect(status().isOk());
    }

    // CONTA-26
    @Test
    void swaggerContinuaPublico() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    // CONTA-26: cadastro e login não exigem token (o comportamento de cada rota é testado na sua classe)
    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/usuarios", "/api/v1/lojas", "/api/v1/auth/usuarios/login",
            "/api/v1/auth/lojas/login"})
    void cadastroELoginNaoExigemToken(String rota) throws Exception {
        mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }
}
