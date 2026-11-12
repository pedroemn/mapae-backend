package br.com.mapae.compartilhado.seguranca;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Senha nova: pelo menos 8 caracteres e no máximo 72 bytes em UTF-8, o limite do BCrypt.
 * Letras acentuadas ocupam 2 bytes, então 72 caracteres com acento não cabem.
 * Nulo é aceito aqui; use junto com @NotBlank.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValida.Validador.class)
public @interface SenhaValida {

    int MINIMO_CARACTERES = 8;
    int MAXIMO_BYTES = 72;

    String message() default "deve ter de 8 caracteres a 72 bytes (letras acentuadas contam 2 bytes)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<SenhaValida, String> {

        public static boolean cabeNoBcrypt(String senha) {
            return senha.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
        }

        @Override
        public boolean isValid(String senha, ConstraintValidatorContext contexto) {
            return senha == null || (senha.length() >= MINIMO_CARACTERES && cabeNoBcrypt(senha));
        }
    }
}
