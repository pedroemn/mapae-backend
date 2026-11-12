package br.com.mapae.compartilhado.seguranca;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** HS256 exige chave de pelo menos 256 bits: 32 caracteres. */
@Validated
@ConfigurationProperties("mapae.jwt")
public record JwtPropriedades(@NotNull @Size(min = 32) String segredo, @NotNull Duration validade) {
}
