package br.com.mapae.locais.aplicacao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ConsultaGeograficaInvalidaException extends ErrorResponseException {

    public ConsultaGeograficaInvalidaException(String detalhe) {
        super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe), null);
    }
}
