package br.com.mapae.compartilhado;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTesteConfig.class)
class DocumentacaoApiTests {

    @Autowired
    MockMvc mvc;

    @Test
    void contratoOpenApiListaAsRotasVersionadas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Mapaê API"))
                .andExpect(jsonPath("$.paths['/api/v1/locais/area']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/locais/proximos']").exists());
    }

    @Test
    void rotaSemVersaoNaoExiste() throws Exception {
        mvc.perform(get("/api/locais/proximos")).andExpect(status().isNotFound());
    }
}
