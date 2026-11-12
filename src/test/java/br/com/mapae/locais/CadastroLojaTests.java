package br.com.mapae.locais;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

/** POST /api/v1/lojas (spec conta-e-login: CONTA-12 a CONTA-15 e casos de borda). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class CadastroLojaTests {

    static final String SENHA = "senha-da-loja";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder senhas;

    static Map<String, Object> valida() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("username", "bar.do.ze");
        corpo.put("nome", "Bar do Zé");
        corpo.put("email", "contato@bardoze.com");
        corpo.put("cep", "49010-000");
        corpo.put("tipo", "bar");
        corpo.put("senha", SENHA);
        return corpo;
    }

    static String json(Map<String, Object> corpo) {
        return corpo.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + (e.getValue() instanceof String s ? "\"" + s + "\"" : e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    ResultActions cadastrar(Map<String, Object> corpo) throws Exception {
        return mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content(json(corpo)));
    }

    // CONTA-12
    @Test
    void cadastroValidoResponde201ComOsDadosDaLoja() throws Exception {
        Instant antes = Instant.now().minusSeconds(1);
        String criadoEm = cadastrar(valida())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("bar.do.ze"))
                .andExpect(jsonPath("$.nome").value("Bar do Zé"))
                .andExpect(jsonPath("$.email").value("contato@bardoze.com"))
                .andExpect(jsonPath("$.tipo").value("bar"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("LOJA"))
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("senhaHash"))))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"criadoEm\":\"([^\"]+)\".*", "$1");
        assertThat(Instant.parse(criadoEm)).isBetween(antes, Instant.now().plusSeconds(1));
        String hash = jdbc.queryForObject("SELECT senha_hash FROM local WHERE username = 'bar.do.ze'", String.class);
        assertThat(hash).startsWith("$2");
        assertThat(senhas.matches(SENHA, hash)).isTrue();
    }

    // CONTA-12: o tipo é opcional
    @Test
    void cadastroSemTipoTambemEhAceito() throws Exception {
        Map<String, Object> corpo = valida();
        corpo.remove("tipo");
        cadastrar(corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").doesNotExist());
    }

    // CONTA-12: o cliente não escolhe perfil, status nem primeiro acesso
    @Test
    void camposControladosPeloServidorSaoIgnorados() throws Exception {
        Map<String, Object> corpo = valida();
        corpo.put("tipoPerfil", "USUARIO");
        corpo.put("status", "INATIVO");
        corpo.put("primeiroAcesso", false);
        cadastrar(corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoPerfil").value("LOJA"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true));
    }

    // CONTA-13
    @Test
    void lojaCadastradaFicaSemPosicaoEForaDoMapa() throws Exception {
        cadastrar(valida()).andExpect(status().isCreated());
        Boolean semPosicao = jdbc.queryForObject(
                "SELECT posicao IS NULL FROM local WHERE username = 'bar.do.ze'", Boolean.class);
        assertThat(semPosicao).isTrue();
        mvc.perform(get("/api/v1/locais/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", "-37.05"))
                .andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(get("/api/v1/locais/area")
                        .param("oeste", "-180").param("sul", "-90").param("leste", "180").param("norte", "90"))
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    // CONTA-14: campo obrigatório ausente
    @ParameterizedTest
    @ValueSource(strings = {"username", "nome", "email", "cep", "senha"})
    void campoObrigatorioAusenteResponde400ListandoOCampo(String campo) throws Exception {
        Map<String, Object> corpo = valida();
        corpo.remove(campo);
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos." + campo).exists());
    }

    // CONTA-14: campo fora do formato
    @ParameterizedTest
    @CsvSource({
            "username, ab",
            "username, bar do ze",
            "email, nao-e-email",
            "cep, 490100",
            "senha, curta",
            "tipo, abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijk",
    })
    void campoForaDoFormatoResponde400ListandoOCampo(String campo, String valor) throws Exception {
        Map<String, Object> corpo = valida();
        corpo.put(campo, valor);
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos." + campo).exists());
    }

    // CONTA-15
    @Test
    void usernameRepetidoComOutraCaixaResponde409NoCampoUsername() throws Exception {
        cadastrar(valida()).andExpect(status().isCreated());
        Map<String, Object> outra = valida();
        outra.put("username", "BAR.DO.ZE");
        outra.put("email", "outro@bar.com");
        cadastrar(outra)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campo").value("username"));
    }

    // CONTA-15
    @Test
    void emailRepetidoComOutraCaixaResponde409NoCampoEmail() throws Exception {
        cadastrar(valida()).andExpect(status().isCreated());
        Map<String, Object> outra = valida();
        outra.put("username", "outro.bar");
        outra.put("email", "CONTATO@bardoze.com");
        cadastrar(outra)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    // CONTA-14: a senha cabe em 72 bytes; acento ocupa 2 bytes (antes dava 500)
    @Test
    void senhaAcentuadaAcimaDe72BytesResponde400ListandoOCampo() throws Exception {
        Map<String, Object> corpo = valida();
        corpo.put("senha", "ç".repeat(72));
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    // Borda: corrida. Só o índice único barra a linha, e a rota responde o mesmo 409.
    @Test
    void corridaNoUsernameDa409PeloIndiceDoBanco() throws Exception {
        jdbc.update("INSERT INTO local (nome, username, email, cep, senha_hash, primeiro_acesso, status, tipo_perfil, "
                + "criado_em, atualizado_em) VALUES ('L', 'BAR.DO.ZE', 'outro@bar.com', '49000000', 'h', true, "
                + "'ATIVO', 'LOJA', now(), now())");
        cadastrar(valida())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("username"));
    }

    // Borda: corrida no email
    @Test
    void corridaNoEmailDa409PeloIndiceDoBanco() throws Exception {
        jdbc.update("INSERT INTO local (nome, username, email, cep, senha_hash, primeiro_acesso, status, tipo_perfil, "
                + "criado_em, atualizado_em) VALUES ('L', 'outro.bar', 'CONTATO@BARDOZE.COM', '49000000', 'h', true, "
                + "'ATIVO', 'LOJA', now(), now())");
        cadastrar(valida())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    // Borda: o mesmo email pode existir como usuário e como loja
    @Test
    void mesmoEmailDeUmUsuarioEhAceitoNaLoja() throws Exception {
        jdbc.update("INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash) "
                + "VALUES ('bar.do.ze', 'Zé', DATE '1990-01-01', 'contato@bardoze.com', '49000000', 'OUTRO', 'h')");
        cadastrar(valida()).andExpect(status().isCreated());
    }

    // Borda: maiúsculas viram minúsculas e o CEP fica só com dígitos
    @Test
    void usernameEEmailFicamEmMinusculasECepSoComDigitos() throws Exception {
        Map<String, Object> corpo = valida();
        corpo.put("username", "Bar.Do.Ze");
        corpo.put("email", "Contato@BarDoZe.com");
        cadastrar(corpo).andExpect(status().isCreated());
        Map<String, Object> linha = jdbc.queryForMap("SELECT username, email, cep FROM local");
        assertThat(linha).containsEntry("username", "bar.do.ze").containsEntry("email", "contato@bardoze.com")
                .containsEntry("cep", "49010000");
    }
}
