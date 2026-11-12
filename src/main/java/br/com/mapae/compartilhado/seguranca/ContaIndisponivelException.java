package br.com.mapae.compartilhado.seguranca;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Senha certa, mas a conta não está ATIVO. */
public class ContaIndisponivelException extends ErrorResponseException {

    public ContaIndisponivelException(StatusConta status) {
        super(HttpStatus.FORBIDDEN, problema(status), null);
    }

    private static ProblemDetail problema(StatusConta status) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Conta com status " + status);
        problema.setProperty("statusConta", status.name());
        return problema;
    }
}
