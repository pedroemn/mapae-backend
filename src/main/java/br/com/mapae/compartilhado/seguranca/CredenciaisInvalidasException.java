package br.com.mapae.compartilhado.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A mesma resposta para login inexistente e senha errada. */
public class CredenciaisInvalidasException extends ErrorResponseException {

    public static final String MENSAGEM = "Login ou senha inválidos";

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, MENSAGEM), null);
    }
}
