package br.com.mapae.compartilhado.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Username ou email já em uso; a propriedade "campo" diz qual dos dois. */
public class ContaDuplicadaException extends ErrorResponseException {

    public ContaDuplicadaException(String campo) {
        super(HttpStatus.CONFLICT, problema(campo), null);
    }

    private static ProblemDetail problema(String campo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, campo + " já está em uso");
        problema.setProperty("campo", campo);
        return problema;
    }
}
