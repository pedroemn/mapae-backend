package br.com.mapae.locais;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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

/** GET /api/v1/locais/area (spec locais-mapa-e-pesquisa: MAPA-01 a MAPA-10, BACK-022). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class MapaAreaTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JwtEncoder encoder;

    CatalogoDeTeste catalogo;

    @BeforeEach
    void preparar() {
        catalogo = new CatalogoDeTeste(jdbc);
    }

    /** Caixa de Aracaju usada em todos os testes. */
    static MockHttpServletRequestBuilder caixa() {
        return get("/api/v1/locais/area")
                .param("oeste", "-37.1").param("sul", "-11.0").param("leste", "-37.0").param("norte", "-10.9");
    }

    ResultActions area(String... parametros) throws Exception {
        MockHttpServletRequestBuilder pedido = caixa();
        for (int i = 0; i < parametros.length; i += 2) {
            pedido.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(pedido);
    }

    // MAPA-01
    @Test
    void devolveOsLocaisAtivosDaCaixaComOsCamposDoMarcador() throws Exception {
        long cafe = catalogo.ativo("Café Central", "cafe", "", -10.95, -37.05);
        long bar = catalogo.ativo("Bar da Praia", null, "", -10.92, -37.02);
        catalogo.ativo("Fora da caixa", "bar", "", -10.5, -37.05);

        area().andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(cafe))
                .andExpect(jsonPath("$.itens[0].nome").value("Café Central"))
                .andExpect(jsonPath("$.itens[0].categoria").value("cafe"))
                .andExpect(jsonPath("$.itens[0].latitude", closeTo(-10.95, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[0].longitude", closeTo(-37.05, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[1].id").value(bar))
                .andExpect(jsonPath("$.itens[1].categoria").doesNotExist())
                .andExpect(jsonPath("$.itens[1]", hasKey("categoria")));
    }

    // MAPA-02: dentro da caixa e do raio, mas inativo
    @Test
    void localInativoNaoApareceNaAreaNemNosProximos() throws Exception {
        long ativo = catalogo.ativo("Ativo", "bar", "", -10.95, -37.05);
        catalogo.local("Inativo", "bar", "", false, -10.95, -37.051);

        area().andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(ativo));
        mvc.perform(get("/api/v1/locais/proximos"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(ativo));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.95").param("longitude", "-37.05"))
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.95").param("longitude", "-37.05")
                        .param("raioMetros", "1000"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(ativo));
    }

    // MAPA-02: loja com status diferente de ATIVO sai do mapa, mesmo com posição e ativo = true
    @Test
    void lojaSemStatusAtivoNaoApareceNoMapa() throws Exception {
        long bloqueada = lojaComPosicao(mvc, jdbc, "bar.bloqueado", "Bar Bloqueado", -10.95, -37.05);
        long liberada = lojaComPosicao(mvc, jdbc, "bar.liberado", "Bar Liberado", -10.95, -37.04);
        jdbc.update("UPDATE local SET status = 'BLOQUEADO' WHERE id = ?", bloqueada);

        area().andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(liberada));
        mvc.perform(get("/api/v1/locais/proximos"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(liberada));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.95").param("longitude", "-37.05")
                        .param("raioMetros", "5000"))
                .andExpect(jsonPath("$.itens.length()").value(1));
        jdbc.update("UPDATE local SET status = 'INATIVO' WHERE id = ?", liberada);
        area().andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.95").param("longitude", "-37.05"))
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    /** Loja pelo cadastro de verdade, com a posição que o cadastro administrativo vai gravar. */
    static long lojaComPosicao(MockMvc mvc, JdbcTemplate jdbc, String username, String nome,
                               double latitude, double longitude) throws Exception {
        mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","nome":"%s","email":"%s@exemplo.com","cep":"49010000",
                         "senha":"senha-forte-1"}""".formatted(username, nome, username)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE local SET posicao = CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography) "
                + "WHERE username = ?", longitude, latitude, username);
        return jdbc.queryForObject("SELECT id FROM local WHERE username = ?", Long.class, username);
    }

    // MAPA-03: 5 locais, páginas de 2
    @Test
    void paginaDevolveSoAPaginaPedidaComTotais() throws Exception {
        long[] ids = new long[5];
        for (int i = 0; i < 5; i++) {
            ids[i] = catalogo.ativo("Local " + i, "bar", "", -10.95, -37.09 + i * 0.01);
        }

        area("pagina", "1", "tamanho", "2").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(ids[2]))
                .andExpect(jsonPath("$.itens[1].id").value(ids[3]))
                .andExpect(jsonPath("$.pagina.numero").value(1))
                .andExpect(jsonPath("$.pagina.tamanho").value(2))
                .andExpect(jsonPath("$.pagina.totalItens").value(5))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(3))
                .andExpect(jsonPath("$.truncado").value(true));
        area("pagina", "2", "tamanho", "2")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(ids[4]))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    // MAPA-03: padrão pagina 0 e tamanho 20 quando só um dos dois vem
    @Test
    void paginacaoUsaOsPadroes() throws Exception {
        for (int i = 0; i < 21; i++) {
            catalogo.ativo("Local " + i, "bar", "", -10.95, -37.09 + i * 0.001);
        }
        area("pagina", "0").andExpect(jsonPath("$.itens.length()").value(20))
                .andExpect(jsonPath("$.pagina.tamanho").value(20))
                .andExpect(jsonPath("$.pagina.totalItens").value(21))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(2))
                .andExpect(jsonPath("$.truncado").value(true));
        area("tamanho", "5").andExpect(jsonPath("$.pagina.numero").value(0))
                .andExpect(jsonPath("$.itens.length()").value(5));
    }

    // MAPA-04
    @Test
    void semPaginacaoDevolveTodosSemOCampoPagina() throws Exception {
        for (int i = 0; i < 25; i++) {
            catalogo.ativo("Local " + i, "bar", "", -10.95, -37.09 + i * 0.001);
        }
        area().andExpect(jsonPath("$.itens.length()").value(25))
                .andExpect(jsonPath("$.truncado").value(false))
                .andExpect(jsonPath("$", not(hasKey("pagina"))));
    }

    // MAPA-05
    @ParameterizedTest
    @CsvSource({"pagina,0", "tamanho,10"})
    void limiteComPaginacaoDa400(String parametro, String valor) throws Exception {
        area("limite", "5", parametro, valor)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("limite não pode ser usado com pagina ou tamanho"));
    }

    // MAPA-06
    @ParameterizedTest
    @CsvSource({"pagina,-1", "tamanho,0", "tamanho,101"})
    void paginacaoForaDaFaixaDa400(String parametro, String valor) throws Exception {
        area(parametro, valor)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    // MAPA-06: a maior página aceita ainda responde 200; acima dela, 400 (antes era 500)
    @Test
    void paginaNoLimiteRespondeEAcimaDaRecusa() throws Exception {
        area("pagina", "21474836", "tamanho", "100").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.pagina.totalItens").value(0));
        area("pagina", "21474837", "tamanho", "100").andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        area("pagina", "2147483647").andExpect(status().isBadRequest());
    }

    // MAPA-06: os limites exatos são aceitos
    @Test
    void tamanhoCemEPaginaZeroSaoAceitos() throws Exception {
        area("pagina", "0", "tamanho", "100").andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina.tamanho").value(100));
        area("tamanho", "1").andExpect(status().isOk());
    }

    // MAPA-07
    @Test
    void areaSemLocaisDevolveListaVazia() throws Exception {
        area().andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.truncado").value(false));
        area("pagina", "0").andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.truncado").value(false))
                .andExpect(jsonPath("$.pagina.totalItens").value(0))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(0));
    }

    // MAPA-08
    @ParameterizedTest
    @CsvSource({"-181,-11.0,-37.0,-10.9", "-37.1,-91,-37.0,-10.9", "-37.0,-11.0,-37.1,-10.9", "-37.1,-10.9,-37.0,-11.0"})
    void caixaInvalidaDa400(String oeste, String sul, String leste, String norte) throws Exception {
        mvc.perform(get("/api/v1/locais/area")
                        .param("oeste", oeste).param("sul", sul).param("leste", leste).param("norte", norte))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // MAPA-09: o erro não expõe detalhes internos
    @ParameterizedTest
    @CsvSource({"tamanho,abc", "tamanho,101", "pagina,-1"})
    void erroNaoExpoeDetalhesInternos(String parametro, String valor) throws Exception {
        area(parametro, valor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", not(hasKey("trace"))))
                .andExpect(jsonPath("$", not(hasKey("exception"))))
                .andExpect(jsonPath("$", not(hasKey("message"))))
                .andExpect(jsonPath("$.detail", not(containsString("SQL"))))
                .andExpect(jsonPath("$.detail", not(containsString("org."))))
                .andExpect(jsonPath("$.detail", not(containsString("java."))));
    }

    // MAPA-10: visitante, usuário autenticado e token expirado ou malformado
    @Test
    void areaEPublicaComOuSemToken() throws Exception {
        catalogo.ativo("Café", "cafe", "", -10.95, -37.05);
        mvc.perform(caixa()).andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(caixa().header("Authorization", "Bearer " + token(Instant.now().plusSeconds(3600))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(caixa().header("Authorization", "Bearer " + token(Instant.now().minusSeconds(60))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(caixa().header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1));
    }

    // Borda: as rotas da conta continuam exigindo token válido (sem regressão de CONTA-25)
    @Test
    void rotaDaContaContinuaRecusandoTokenExpirado() throws Exception {
        mvc.perform(get("/api/v1/contas/eu").header("Authorization", "Bearer " + token(Instant.now().minusSeconds(60))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/contas/eu")).andExpect(status().isUnauthorized());
    }

    String token(Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").claim("perfil", "USUARIO")
                .issuedAt(expiraEm.minusSeconds(7200)).expiresAt(expiraEm).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
