package br.com.mapae.compartilhado.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação da API (AD-031). As rotas e os parâmetros vêm dos controllers.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    /** Nome do esquema usado no @SecurityRequirement das rotas que exigem token. */
    public static final String BEARER = "bearer";

    @Bean
    OpenAPI mapaeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mapaê API")
                        .version("v1")
                        .description("API do backend do Mapaê. Todas as rotas ficam sob /api/v1."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
