package br.com.mapae.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import br.com.mapae.compartilhado.seguranca.Cadastro;
import br.com.mapae.compartilhado.seguranca.ContaDuplicadaException;
import java.time.Instant;
import java.time.LocalDate;
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

/** POST /api/v1/usuarios (spec conta-e-login: CONTA-07 a CONTA-11 e casos de borda). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class CadastroUsuarioTests {

    static final String SENHA = "senha-forte-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder senhas;

    static Map<String, Object> valido() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("username", "ana.souza");
        corpo.put("nome", "Ana Souza");
        corpo.put("dataNascimento", "2000-05-10");
        corpo.put("email", "ana@exemplo.com");
        corpo.put("cep", "49000-000");
        corpo.put("genero", "FEMININO");
        corpo.put("bio", "Gosto de praia");
        corpo.put("senha", SENHA);
        return corpo;
    }

    static String json(Map<String, Object> corpo) {
        return corpo.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + (e.getValue() instanceof String s ? "\"" + s + "\"" : e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    ResultActions cadastrar(Map<String, Object> corpo) throws Exception {
        return mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(json(corpo)));
    }

    // CONTA-07
    @Test
    void cadastroValidoResponde201ComOsDadosDaConta() throws Exception {
        Instant antes = Instant.now().minusSeconds(1);
        String criadoEm = cadastrar(valido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("ana.souza"))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.tipoPerfil").value("USUARIO"))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"criadoEm\":\"([^\"]+)\".*", "$1");
        assertThat(Instant.parse(criadoEm)).isBetween(antes, Instant.now().plusSeconds(1));
    }

    // CONTA-08
    @Test
    void senhaFicaSoComoHashBcryptENaoVoltaNaResposta() throws Exception {
        cadastrar(valido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("senha"))))
                .andExpect(jsonPath("$", not(hasKey("senhaHash"))));
        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE username = 'ana.souza'", String.class);
        assertThat(hash).startsWith("$2").isNotEqualTo(SENHA);
        assertThat(senhas.matches(SENHA, hash)).isTrue();
    }

    // CONTA-09
    @Test
    void camposControladosPeloServidorSaoIgnorados() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("tipoPerfil", "LOJA");
        corpo.put("status", "BLOQUEADO");
        corpo.put("primeiroAcesso", false);
        corpo.put("criadoEm", "2000-01-01T00:00:00Z");
        cadastrar(corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoPerfil").value("USUARIO"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.primeiroAcesso").value(true))
                .andExpect(jsonPath("$.criadoEm", not("2000-01-01T00:00:00Z")));
        Map<String, Object> linha = jdbc.queryForMap(
                "SELECT tipo_perfil, status, primeiro_acesso, criado_em > now() - interval '1 minute' AS recente "
                        + "FROM usuario WHERE username = 'ana.souza'");
        assertThat(linha).containsEntry("tipo_perfil", "USUARIO").containsEntry("status", "ATIVO")
                .containsEntry("primeiro_acesso", true).containsEntry("recente", true);
    }

    // CONTA-10: campo obrigatório ausente
    @ParameterizedTest
    @ValueSource(strings = {"username", "nome", "dataNascimento", "email", "cep", "genero", "senha"})
    void campoObrigatorioAusenteResponde400ListandoOCampo(String campo) throws Exception {
        Map<String, Object> corpo = valido();
        corpo.remove(campo);
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos." + campo).exists());
    }

    // CONTA-10: campo fora do formato das premissas
    @ParameterizedTest
    @CsvSource({
            "username, ab",
            "username, ana souza",
            "username, abcdefghijabcdefghijabcdefghija",
            "email, nao-e-email",
            "cep, 4900-000",
            "cep, 49000-0000",
            "genero, HOMEM",
            "senha, 1234567",
            "senha, 1234567890123456789012345678901234567890123456789012345678901234567890123",
    })
    void campoForaDoFormatoResponde400ListandoOCampo(String campo, String valor) throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put(campo, valor);
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos." + campo).exists());
    }

    // CONTA-10: data de nascimento precisa ser anterior a hoje
    @Test
    void dataDeNascimentoDeHojeResponde400() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("dataNascimento", LocalDate.now().toString());
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dataNascimento").exists());
    }

    // Limites aceitos das premissas: username de 3 e 30 caracteres, senha de 8 e 72
    @ParameterizedTest
    @CsvSource({
            "username, abc",
            "username, abcdefghijabcdefghijabcdefghij",
            "senha, 12345678",
            "senha, 123456789012345678901234567890123456789012345678901234567890123456789012",
    })
    void limitesDasPremissasSaoAceitos(String campo, String valor) throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put(campo, valor);
        cadastrar(corpo).andExpect(status().isCreated());
    }

    // CONTA-11
    @Test
    void usernameRepetidoComOutraCaixaResponde409NoCampoUsername() throws Exception {
        cadastrar(valido()).andExpect(status().isCreated());
        Map<String, Object> outro = valido();
        outro.put("username", "ANA.Souza");
        outro.put("email", "outra@exemplo.com");
        cadastrar(outro)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campo").value("username"));
    }

    // CONTA-11
    @Test
    void emailRepetidoComOutraCaixaResponde409NoCampoEmail() throws Exception {
        cadastrar(valido()).andExpect(status().isCreated());
        Map<String, Object> outro = valido();
        outro.put("username", "outra");
        outro.put("email", "ANA@Exemplo.com");
        cadastrar(outro)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    // Borda: corrida entre dois cadastros; o índice único do banco vira o mesmo 409
    @Test
    void violacaoDoIndiceUnicoNaCorridaViraConflitoNoCampoCerto() {
        String inserir = "INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash) "
                + "VALUES (?, 'N', DATE '2000-01-01', ?, '49000000', 'OUTRO', 'h')";
        jdbc.update(inserir, "ana", "ana@x.com");
        assertThatThrownBy(() -> Cadastro.salvarSemDuplicar(() -> jdbc.update(inserir, "ana", "outro@x.com"),
                "uq_usuario_username", "uq_usuario_email"))
                .isInstanceOfSatisfying(ContaDuplicadaException.class,
                        e -> assertThat(e.getBody().getProperties()).containsEntry("campo", "username"));
    }

    // Borda: corrida no email
    @Test
    void violacaoDoIndiceDeEmailNaCorridaViraConflitoNoCampoEmail() {
        String inserir = "INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash) "
                + "VALUES (?, 'N', DATE '2000-01-01', ?, '49000000', 'OUTRO', 'h')";
        jdbc.update(inserir, "ana", "ana@x.com");
        assertThatThrownBy(() -> Cadastro.salvarSemDuplicar(() -> jdbc.update(inserir, "bia", "ANA@x.com"),
                "uq_usuario_username", "uq_usuario_email"))
                .isInstanceOfSatisfying(ContaDuplicadaException.class,
                        e -> assertThat(e.getBody().getProperties()).containsEntry("campo", "email"));
    }

    // Borda: maiúsculas são guardadas em minúsculas, CEP só com dígitos
    @Test
    void usernameEEmailFicamEmMinusculasECepSoComDigitos() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("username", "Ana.Souza");
        corpo.put("email", "Ana@Exemplo.COM");
        cadastrar(corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana.souza"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
        Map<String, Object> linha = jdbc.queryForMap("SELECT username, email, cep FROM usuario");
        assertThat(linha).containsEntry("username", "ana.souza").containsEntry("email", "ana@exemplo.com")
                .containsEntry("cep", "49000000");
    }

    // CONTA-10: a senha cabe em 72 bytes; acento ocupa 2 bytes (antes dava 500)
    @Test
    void senhaAcentuadaAcimaDe72BytesResponde400ListandoOCampo() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("senha", "é".repeat(37));
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    // Limite das premissas: 36 letras acentuadas dão exatamente 72 bytes
    @Test
    void senhaAcentuadaComExatos72BytesEhAceita() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("senha", "é".repeat(36));
        cadastrar(corpo).andExpect(status().isCreated());
        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuario", String.class);
        assertThat(senhas.matches("é".repeat(36), hash)).isTrue();
    }

    // CONTA-10: data de nascimento fora de AAAA-MM-DD
    @Test
    void dataDeNascimentoForaDoFormatoResponde400ListandoOCampo() throws Exception {
        Map<String, Object> corpo = valido();
        corpo.put("dataNascimento", "10/05/2000");
        cadastrar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos.dataNascimento").exists());
    }

    // Borda: corrida. Uma linha gravada sem passar pela checagem do serviço (aqui, em maiúsculas)
    // só é barrada pelo índice único, e a rota responde o mesmo 409.
    @Test
    void corridaNoUsernameDa409PeloIndiceDoBanco() throws Exception {
        jdbc.update("INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash) "
                + "VALUES ('ANA.SOUZA', 'N', DATE '2000-01-01', 'outra@x.com', '49000000', 'OUTRO', 'h')");
        cadastrar(valido())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("username"));
    }

    // Borda: corrida no email
    @Test
    void corridaNoEmailDa409PeloIndiceDoBanco() throws Exception {
        jdbc.update("INSERT INTO usuario (username, nome, data_nascimento, email, cep, genero, senha_hash) "
                + "VALUES ('outra', 'N', DATE '2000-01-01', 'ANA@EXEMPLO.COM', '49000000', 'OUTRO', 'h')");
        cadastrar(valido())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    // Borda: JSON malformado
    @Test
    void jsonMalformadoResponde400EmProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
