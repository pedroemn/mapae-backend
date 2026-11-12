package br.com.mapae.locais;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import br.com.mapae.locais.dominio.Local;
import br.com.mapae.locais.dominio.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
@Transactional
class ConsultasGeograficasTests {

    // 0,001 grau de latitude perto de Aracaju mede cerca de 110,6 m
    private static final double LAT = -10.9;
    private static final double LNG = -37.05;

    @Autowired
    MockMvc mvc;

    @Autowired
    LocalRepository locais;

    private Local salvar(String nome, double latitude, double longitude) {
        return locais.saveAndFlush(new Local(nome, latitude, longitude));
    }

    private org.springframework.test.web.servlet.ResultActions area(String oeste, String sul, String leste,
                                                                    String norte) throws Exception {
        return mvc.perform(get("/api/v1/locais/area")
                .param("oeste", oeste).param("sul", sul).param("leste", leste).param("norte", norte));
    }

    @Test
    void areaDevolveSoOsLocaisDaCaixaOrdenadosPorId() throws Exception {
        Local primeiro = salvar("Dentro 1", -10.95, -37.05);
        Local segundo = salvar("Dentro 2", -10.92, -37.02);
        salvar("Fora ao norte", -10.5, -37.05);
        salvar("Fora a leste", -10.95, -36.5);

        area("-37.1", "-11.0", "-37.0", "-10.9")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(primeiro.getId()))
                .andExpect(jsonPath("$.itens[0].nome").value("Dentro 1"))
                .andExpect(jsonPath("$.itens[0].latitude", closeTo(-10.95, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[0].longitude", closeTo(-37.05, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[1].id").value(segundo.getId()))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    @Test
    void areaIncluiLocalExatamenteNaBorda() throws Exception {
        Local naBorda = salvar("Na borda", -10.9, -37.0);

        area("-37.1", "-11.0", "-37.0", "-10.9")
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(naBorda.getId()));
    }

    @Test
    void areaSemLocaisDevolveListaVazia() throws Exception {
        area("-37.1", "-11.0", "-37.0", "-10.9")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    @Test
    void areaAcimaDoLimiteMarcaTruncado() throws Exception {
        Local primeiro = salvar("A", -10.95, -37.05);
        salvar("B", -10.95, -37.04);
        salvar("C", -10.95, -37.03);

        mvc.perform(get("/api/v1/locais/area").param("oeste", "-37.1").param("sul", "-11.0")
                        .param("leste", "-37.0").param("norte", "-10.9").param("limite", "1"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(primeiro.getId()))
                .andExpect(jsonPath("$.truncado").value(true));
    }

    @Test
    void areaExatamenteNoLimiteNaoMarcaTruncado() throws Exception {
        salvar("A", -10.95, -37.05);
        salvar("B", -10.95, -37.04);

        mvc.perform(get("/api/v1/locais/area").param("oeste", "-37.1").param("sul", "-11.0")
                        .param("leste", "-37.0").param("norte", "-10.9").param("limite", "2"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    @Test
    void areaSemLimiteDevolveTodosSemTruncar() throws Exception {
        salvar("A", -10.95, -37.05);
        salvar("B", -10.95, -37.04);
        salvar("C", -10.95, -37.03);

        area("-37.1", "-11.0", "-37.0", "-10.9")
                .andExpect(jsonPath("$.itens.length()").value(3))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    @Test
    void areaComOesteMaiorQueLesteDevolve400() throws Exception {
        area("-37.0", "-11.0", "-37.1", "-10.9").andExpect(status().isBadRequest());
    }

    @Test
    void areaComSulMaiorQueNorteDevolve400() throws Exception {
        area("-37.1", "-10.9", "-37.0", "-11.0").andExpect(status().isBadRequest());
    }

    @Test
    void proximosDevolveDoMaisPertoAoMaisLongeComDistancia() throws Exception {
        Local longe = salvar("Longe", LAT + 0.002, LNG);
        Local perto = salvar("Perto", LAT + 0.001, LNG);
        salvar("Fora do raio", LAT + 0.01, LNG);

        mvc.perform(get("/api/v1/locais/proximos").param("latitude", String.valueOf(LAT))
                        .param("longitude", String.valueOf(LNG)).param("raioMetros", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(perto.getId()))
                .andExpect(jsonPath("$.itens[0].nome").value("Perto"))
                .andExpect(jsonPath("$.itens[0].latitude", closeTo(LAT + 0.001, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[0].longitude", closeTo(LNG, 1e-9), Double.class))
                .andExpect(jsonPath("$.itens[0].distanciaMetros", closeTo(110.6, 0.5), Double.class))
                .andExpect(jsonPath("$.itens[1].id").value(longe.getId()))
                .andExpect(jsonPath("$.itens[1].distanciaMetros", closeTo(221.2, 1.0), Double.class));
    }

    @Test
    void proximosRespeitaOLimite() throws Exception {
        Local perto = salvar("Perto", LAT + 0.001, LNG);
        salvar("Longe", LAT + 0.002, LNG);

        mvc.perform(get("/api/v1/locais/proximos").param("latitude", String.valueOf(LAT))
                        .param("longitude", String.valueOf(LNG)).param("raioMetros", "500").param("limite", "1"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(perto.getId()));
    }

    @Test
    void proximosNaoTemRaioMaximo() throws Exception {
        Local longe = salvar("Longe", LAT + 5, LNG);

        mvc.perform(get("/api/v1/locais/proximos").param("latitude", String.valueOf(LAT))
                        .param("longitude", String.valueOf(LNG)).param("raioMetros", "5000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(longe.getId()));
    }

    @Test
    void proximosSemRaioDevolveTodosDoMaisPertoAoMaisLonge() throws Exception {
        Local longe = salvar("Longe", LAT + 5, LNG);
        Local perto = salvar("Perto", LAT + 0.001, LNG);

        mvc.perform(get("/api/v1/locais/proximos").param("latitude", String.valueOf(LAT))
                        .param("longitude", String.valueOf(LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(perto.getId()))
                .andExpect(jsonPath("$.itens[1].id").value(longe.getId()))
                .andExpect(jsonPath("$.itens[1].distanciaMetros").isNumber());
    }

    @Test
    void proximosSemCoordenadasListaTodosPorIdSemDistancia() throws Exception {
        Local primeiro = salvar("A", LAT, LNG);
        Local segundo = salvar("B", LAT + 20, LNG + 20);

        mvc.perform(get("/api/v1/locais/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value(primeiro.getId()))
                .andExpect(jsonPath("$.itens[1].id").value(segundo.getId()))
                .andExpect(jsonPath("$.itens[0].distanciaMetros").doesNotExist());
    }

    @Test
    void limiteNaoTemMaximo() throws Exception {
        salvar("A", LAT, LNG);

        mvc.perform(get("/api/v1/locais/proximos").param("limite", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    @Test
    void raioInvalidoDevolve400() throws Exception {
        for (String raio : new String[] {"0", "-5", "NaN", "abc"}) {
            mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", "-37.05")
                            .param("raioMetros", raio))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void limiteGigantePodeSerUsadoNaAreaSemErro() throws Exception {
        salvar("A", -10.95, -37.05);

        mvc.perform(get("/api/v1/locais/area").param("oeste", "-37.1").param("sul", "-11.0")
                        .param("leste", "-37.0").param("norte", "-10.9").param("limite", "2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.truncado").value(false));
    }

    @Test
    void latitudeSemLongitudeDevolve400() throws Exception {
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void raioSemCoordenadasDevolve400() throws Exception {
        mvc.perform(get("/api/v1/locais/proximos").param("raioMetros", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limiteMenorQueUmDevolve400() throws Exception {
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", "-37.05")
                        .param("raioMetros", "100").param("limite", "0"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"90.1", "-90.1", "NaN", "Infinity", "abc"})
    void latitudeInvalidaDevolve400(String latitude) throws Exception {
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", latitude).param("longitude", "-37.05")
                        .param("raioMetros", "100"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"180.1", "-180.1", "NaN"})
    void longitudeInvalidaDevolve400(String longitude) throws Exception {
        mvc.perform(get("/api/v1/locais/proximos").param("latitude", "-10.9").param("longitude", longitude)
                        .param("raioMetros", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void coordenadaDaAreaForaDaFaixaDevolve400() throws Exception {
        area("-181", "-11.0", "-37.0", "-10.9").andExpect(status().isBadRequest());
    }
}
