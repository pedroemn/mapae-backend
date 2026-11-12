package br.com.mapae.compartilhado.seguranca;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Origens do front que o navegador pode usar para chamar a API (AD-039). */
@Validated
@ConfigurationProperties("mapae.cors")
public record CorsPropriedades(@NotEmpty List<String> origens) {
}
