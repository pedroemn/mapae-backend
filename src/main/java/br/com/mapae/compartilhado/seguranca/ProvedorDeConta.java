package br.com.mapae.compartilhado.seguranca;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Porta que cada módulo dono de conta implementa. A rota /api/v1/contas/eu escolhe o provedor
 * pelo perfil do token, sem que um módulo dependa do outro (AD-005).
 */
public interface ProvedorDeConta {

    TipoPerfil perfil();

    DadosDaConta buscar(long id);

    /** Idempotente: se o primeiro acesso já foi concluído, não altera nada. */
    void concluirPrimeiroAcesso(long id);

    void trocarSenha(long id, String senhaAtual, String novaSenha);

    /** Dados da conta autenticada; campos que não se aplicam ao perfil ficam fora do JSON. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DadosDaConta(Long id, String username, String nome, String email, String cep, TipoPerfil tipoPerfil,
                        StatusConta status, boolean primeiroAcesso, Instant criadoEm, Instant atualizadoEm,
                        LocalDate dataNascimento, String genero, String bio, String tipo) {
    }

    /** Token válido de uma conta que não existe mais. */
    class ContaNaoEncontradaException extends ErrorResponseException {
        public ContaNaoEncontradaException() {
            super(HttpStatus.UNAUTHORIZED, ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                    "A conta do token não existe"), null);
        }
    }

    class SenhaAtualIncorretaException extends ErrorResponseException {
        public SenhaAtualIncorretaException() {
            super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                    "A senha atual está incorreta"), null);
        }
    }
}
