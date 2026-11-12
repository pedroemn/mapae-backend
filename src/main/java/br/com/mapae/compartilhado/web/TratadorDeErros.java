package br.com.mapae.compartilhado.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Problem Details em toda a API. Substitui o tratador padrão do Spring para listar,
 * em "campos", cada campo inválido do corpo com a sua mensagem.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        erro.getBody().setDetail("Campos inválidos: " + String.join(", ", campos.keySet()));
        erro.getBody().setProperty("campos", campos);
        return handleExceptionInternal(erro, null, headers, status, requisicao);
    }

    /** Valor que não converte para o tipo do campo, como uma data fora de AAAA-MM-DD, também lista o campo. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException erro,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest requisicao) {
        ResponseEntity<Object> resposta = super.handleHttpMessageNotReadable(erro, headers, status, requisicao);
        if (erro.getCause() instanceof MismatchedInputException tipoErrado
                && resposta != null && resposta.getBody() instanceof ProblemDetail problema) {
            List<JacksonException.Reference> caminho = tipoErrado.getPath();
            String campo = caminho.isEmpty() ? null : caminho.getLast().getPropertyName();
            if (campo != null) {
                problema.setDetail("Campos inválidos: " + campo);
                problema.setProperty("campos", Map.of(campo, "formato inválido"));
            }
        }
        return resposta;
    }
}
