package br.com.mapae.locais;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** GET /api/v1/locais/pesquisa (spec locais-mapa-e-pesquisa: BUSCA-01 a BUSCA-12, BACK-023). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class PesquisaLocaisTests {

    static final double LAT = -10.95;
    static final double LNG = -37.05;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JwtEncoder encoder;

    CatalogoDeTeste catalogo;

    long cafeCentral;
    long padaria;
    long barDoZe;
    long sorveteria;

    @BeforeEach
    void preparar() {
        catalogo = new CatalogoDeTeste(jdbc);
        cafeCentral = catalogo.ativo("Café Central", "Cafeteria", "wifi|café especial", LAT, LNG);
        padaria = catalogo.ativo("Padaria São João", "padaria", "pão|cafe da manha", LAT, LNG + 0.001);
        barDoZe = catalogo.ativo("Bar do Zé", "bar", "petiscos|música ao vivo", LAT, LNG + 0.002);
        sorveteria = catalogo.ativo("Sorveteria Gelato", null, "", LAT, LNG + 0.003);
    }

    static MockHttpServletRequestBuilder pesquisa(String... parametros) {
        MockHttpServletRequestBuilder pedido = get("/api/v1/locais/pesquisa");
        for (int i = 0; i < parametros.length; i += 2) {
            pedido.param(parametros[i], parametros[i + 1]);
        }
        return pedido;
    }

    ResultActions pesquisar(String... parametros) throws Exception {
        return mvc.perform(pesquisa(parametros));
    }

    // BUSCA-01: pelo nome, sem diferenciar maiúsculas nem acentos
    @ParameterizedTest
    @ValueSource(strings = {"central", "CAFÉ CENTRAL", "cafe centr", "Cafe Central"})
    void encontraPeloNomeSemDiferenciarMaiusculasNemAcentos(String termo) throws Exception {
        pesquisar("termo", termo).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(cafeCentral));
    }

    // BUSCA-01: o termo também casa com a categoria e com os termos do local
    @Test
    void encontraPelaCategoriaEPelosTermos() throws Exception {
        pesquisar("termo", "cafeter").andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(cafeCentral));
        pesquisar("termo", "petisco").andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(barDoZe));
        pesquisar("termo", "MUSICA").andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(barDoZe));
        // "café" está no nome do Café Central e nos termos da padaria ("cafe da manha")
        pesquisar("termo", "café").andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder((int) cafeCentral, (int) padaria)));
    }

    // BUSCA-02
    @ParameterizedTest
    @ValueSource(strings = {"padaria", "PADARIA", "Padária"})
    void filtraPorCategoriaPorIgualdade(String categoria) throws Exception {
        pesquisar("categoria", categoria).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(padaria));
    }

    // BUSCA-02: categoria é igualdade, não trecho
    @Test
    void categoriaParcialNaoCasa() throws Exception {
        pesquisar("categoria", "cafe").andExpect(jsonPath("$.itens.length()").value(0));
    }

    // BUSCA-03
    @Test
    void termoECategoriaPrecisamCasarJuntos() throws Exception {
        pesquisar("termo", "cafe", "categoria", "padaria")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(padaria));
        pesquisar("termo", "petisco", "categoria", "padaria")
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    // BUSCA-04: termo ausente, vazio ou só com espaços; em ordem de nome
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void termoVazioListaOCatalogoAtivo(String termo) throws Exception {
        pesquisar("termo", termo).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(4))
                .andExpect(jsonPath("$.pagina.totalItens").value(4));
    }

    // BUSCA-04: sem termo, a categoria ainda filtra
    @Test
    void semTermoACategoriaFiltra() throws Exception {
        pesquisar().andExpect(jsonPath("$.itens.length()").value(4));
        pesquisar("termo", " ", "categoria", "bar")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(barDoZe));
    }

    // BUSCA-05
    @Test
    void semResultadoDevolveListaVaziaComTotaisZero() throws Exception {
        pesquisar("termo", "academia").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.pagina.totalItens").value(0))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(0));
    }

    // BUSCA-06: padrão pagina 0 e tamanho 20; ordem por nome sem acento, depois id
    @Test
    void paginaPorPadraoEOrdenaPorNome() throws Exception {
        pesquisar().andExpect(jsonPath("$.pagina.numero").value(0))
                .andExpect(jsonPath("$.pagina.tamanho").value(20))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(barDoZe))
                .andExpect(jsonPath("$.itens[1].id").value(cafeCentral))
                .andExpect(jsonPath("$.itens[2].id").value(padaria))
                .andExpect(jsonPath("$.itens[3].id").value(sorveteria));
    }

    // BUSCA-06: empate de nome desempata por id; "Água" vem antes de "Bar" sem o acento
    @Test
    void ordemIgnoraAcentoEDesempataPorId() throws Exception {
        long agua1 = catalogo.ativo("Água de Coco", "quiosque", "", LAT, LNG);
        long agua2 = catalogo.ativo("Água de Coco", "quiosque", "", LAT, LNG);
        pesquisar().andExpect(jsonPath("$.itens[0].id").value(agua1))
                .andExpect(jsonPath("$.itens[1].id").value(agua2))
                .andExpect(jsonPath("$.itens[2].id").value(barDoZe));
    }

    // BUSCA-06: nomes que só diferem em acento ou maiúsculas empatam e saem por id. A collation do
    // banco poria "Cafe" antes de "Café" e "bar" antes de "BAR"; a regra da spec põe o de menor id.
    @Test
    void empateSemAcentoESemMaiusculasSaiPorId() throws Exception {
        long comAcento = catalogo.ativo("Café Xique", "x", "", LAT, LNG);
        long semAcento = catalogo.ativo("Cafe Xique", "x", "", LAT, LNG);
        long maiusculo = catalogo.ativo("BAR ZETA", "x", "", LAT, LNG);
        long minusculo = catalogo.ativo("bar zeta", "x", "", LAT, LNG);
        pesquisar("categoria", "x")
                .andExpect(jsonPath("$.itens[0].id").value(maiusculo))
                .andExpect(jsonPath("$.itens[1].id").value(minusculo))
                .andExpect(jsonPath("$.itens[2].id").value(comAcento))
                .andExpect(jsonPath("$.itens[3].id").value(semAcento));
    }

    // BUSCA-06
    @Test
    void paginaPedidaComTotais() throws Exception {
        pesquisar("pagina", "1", "tamanho", "3").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(sorveteria))
                .andExpect(jsonPath("$.pagina.numero").value(1))
                .andExpect(jsonPath("$.pagina.tamanho").value(3))
                .andExpect(jsonPath("$.pagina.totalItens").value(4))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(2));
    }

    // BUSCA-06: o tamanho padrão é 20
    @Test
    void tamanhoPadraoCortaEm20() throws Exception {
        for (int i = 0; i < 17; i++) {
            catalogo.ativo("Extra " + i, "bar", "", LAT, LNG);
        }
        pesquisar().andExpect(jsonPath("$.itens.length()").value(20))
                .andExpect(jsonPath("$.pagina.totalItens").value(21))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(2));
    }

    // BUSCA-07
    @ParameterizedTest
    @CsvSource({"pagina,-1", "tamanho,0", "tamanho,101"})
    void paginacaoForaDaFaixaDa400(String parametro, String valor) throws Exception {
        pesquisar(parametro, valor).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$", not(hasKey("trace"))))
                .andExpect(jsonPath("$", not(hasKey("exception"))));
    }

    // BUSCA-07: a maior página aceita ainda responde 200; acima dela, 400 (antes era 500)
    @Test
    void paginaNoLimiteRespondeEAcimaDaRecusa() throws Exception {
        pesquisar("pagina", "21474836", "tamanho", "100").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.pagina.totalItens").value(4));
        for (String pagina : new String[] {"21474837", "2147483647"}) {
            pesquisar("pagina", pagina, "tamanho", "100").andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$", not(hasKey("trace"))));
        }
    }

    // BUSCA-07: o caractere NUL é recusado com 400 (o PostgreSQL não aceita e daria 500)
    @Test
    void caractereNulDa400() throws Exception {
        pesquisar("termo", "caf\u0000").andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        pesquisar("categoria", "bar\u0000").andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // BUSCA-07: termo até 100 e categoria até 50 caracteres
    @Test
    void textoLongoDemaisDa400() throws Exception {
        pesquisar("termo", "a".repeat(101)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", not(containsString("SQL"))));
        pesquisar("categoria", "a".repeat(51)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        pesquisar("termo", "a".repeat(100), "categoria", "a".repeat(50)).andExpect(status().isOk());
    }

    // BUSCA-08: %, _ e \ são literais. "Desconto 10%" termina em %: se a barra não fosse escapada,
    // o termo "\" viraria "termina com %" e acharia esse local em vez do que tem a barra.
    @Test
    void curingasSaoLiterais() throws Exception {
        long promocao = catalogo.ativo("Bar 50% off", "bar", "", LAT, LNG);
        long desconto = catalogo.ativo("Desconto 10%", "bar", "", LAT, LNG);
        long sublinhado = catalogo.ativo("Bar_Rock", "bar", "", LAT, LNG);
        long barra = catalogo.ativo("Rock \\ Blues", "bar", "", LAT, LNG);
        pesquisar("termo", "%").andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder((int) promocao, (int) desconto)));
        pesquisar("termo", "_").andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(sublinhado));
        pesquisar("termo", "\\").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(barra));
    }

    // BUSCA-08: aspas e SQL no termo são só texto
    @Test
    void termoComSqlEApenasTexto() throws Exception {
        pesquisar("termo", "' OR 1=1 --").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
        pesquisar("termo", "'; DROP TABLE local; --").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
        pesquisar().andExpect(jsonPath("$.itens.length()").value(4));
    }

    // BUSCA-09: inativo e sem posição ficam fora, mesmo casando o nome
    @Test
    void inativoESemPosicaoFicamFora() throws Exception {
        catalogo.local("Café Fechado", "cafeteria", "", false, LAT, LNG);
        mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"cafe.novo","nome":"Café Novo","email":"cafe@novo.com","cep":"49010000",
                         "senha":"senha-forte-1"}"""))
                .andExpect(status().isCreated());

        pesquisar("termo", "café").andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder((int) cafeCentral, (int) padaria)));
        pesquisar().andExpect(jsonPath("$.pagina.totalItens").value(4));
    }

    // BUSCA-09: loja bloqueada ou inativa sai da pesquisa, mesmo com posição e ativo = true
    @Test
    void lojaSemStatusAtivoFicaFora() throws Exception {
        long bloqueada = MapaAreaTests.lojaComPosicao(mvc, jdbc, "cafe.bloqueado", "Café Bloqueado", LAT, LNG);
        long liberada = MapaAreaTests.lojaComPosicao(mvc, jdbc, "cafe.liberado", "Café Liberado", LAT, LNG);
        jdbc.update("UPDATE local SET status = 'BLOQUEADO' WHERE id = ?", bloqueada);
        pesquisar("termo", "café").andExpect(jsonPath("$.itens.length()").value(3))
                .andExpect(jsonPath("$.itens[*].id",
                        containsInAnyOrder((int) cafeCentral, (int) padaria, (int) liberada)));
        jdbc.update("UPDATE local SET status = 'INATIVO' WHERE id = ?", liberada);
        pesquisar("termo", "café").andExpect(jsonPath("$.itens.length()").value(2));
    }

    // BUSCA-10: mesmos campos do marcador do mapa
    @Test
    void itemTemOsCamposDoMarcador() throws Exception {
        pesquisar("termo", "central")
                .andExpect(jsonPath("$.itens[0].id").value(cafeCentral))
                .andExpect(jsonPath("$.itens[0].nome").value("Café Central"))
                .andExpect(jsonPath("$.itens[0].categoria").value("Cafeteria"))
                .andExpect(jsonPath("$.itens[0].latitude").value(LAT))
                .andExpect(jsonPath("$.itens[0].longitude").value(LNG))
                .andExpect(jsonPath("$.itens[0]", not(hasKey("termos"))))
                .andExpect(jsonPath("$.itens[0]", not(hasKey("ativo"))));
        pesquisar("termo", "gelato")
                .andExpect(jsonPath("$.itens[0]", hasKey("categoria")))
                .andExpect(jsonPath("$.itens[0].categoria").doesNotExist());
    }

    // BUSCA-11
    @Test
    void paginaDepoisDaUltimaVemVaziaComTotais() throws Exception {
        pesquisar("pagina", "5").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.pagina.numero").value(5))
                .andExpect(jsonPath("$.pagina.totalItens").value(4))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(1));
    }

    // BUSCA-12
    @Test
    void pesquisaEPublicaComOuSemToken() throws Exception {
        mvc.perform(pesquisa("termo", "central")).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(pesquisa("termo", "central")
                        .header("Authorization", "Bearer " + token(Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(pesquisa("termo", "central")
                        .header("Authorization", "Bearer " + token(Instant.now().minusSeconds(60))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(pesquisa("termo", "central").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
    }

    // Borda: o front web numa origem liberada consegue ler a resposta (AD-039)
    @Test
    void frontWebLeAPesquisa() throws Exception {
        mvc.perform(pesquisa("termo", "central").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    String token(Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").claim("perfil", "USUARIO")
                .issuedAt(expiraEm.minusSeconds(7200)).expiresAt(expiraEm).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
