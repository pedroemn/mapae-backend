package br.com.mapae.compartilhado;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** CORS para o front web (AD-039): origens do padrão de desenvolvimento. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
class CorsTests {

    static final String FRONT = "http://localhost:5173";

    @Autowired
    MockMvc mvc;

    @Test
    void preVooDoLoginDeOrigemLiberadaPassa() throws Exception {
        mvc.perform(options("/api/v1/auth/usuarios/login")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("POST")))
                // O token vai no cabeçalho, sem cookies: credenciais de navegador ficam desligadas.
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preVooDeRotaProtegidaPassaSemToken() throws Exception {
        // O navegador não manda o token no pré-voo; ele não pode dar 401.
        mvc.perform(options("/api/v1/contas/eu")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    @Test
    void erro401LevaOCabecalhoCorsParaOFrontLer() throws Exception {
        mvc.perform(get("/api/v1/contas/eu").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @Test
    void origemNaoLiberadaERecusada() throws Exception {
        mvc.perform(options("/api/v1/auth/usuarios/login")
                        .header(HttpHeaders.ORIGIN, "https://site-qualquer.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
