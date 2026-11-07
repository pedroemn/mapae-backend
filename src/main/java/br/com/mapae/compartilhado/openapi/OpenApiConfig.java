package br.com.mapae.compartilhado.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação da API (AD-031). As rotas e os parâmetros vêm dos controllers.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI mapaeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mapaê API")
                .version("v1")
                .description("API do backend do Mapaê. Todas as rotas ficam sob /api/v1."));
    }
}
