package br.com.mapae.locais.aplicacao;

import br.com.mapae.locais.dominio.Categoria;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 400 no formato das outras validações do cadastro: {@code campos.tipo} com a lista aceita. */
public class TipoDeLojaInvalidoException extends ErrorResponseException {

    public TipoDeLojaInvalidoException() {
        super(HttpStatus.BAD_REQUEST, problema(), null);
    }

    private static ProblemDetail problema() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campos inválidos: tipo");
        problema.setProperty("campos", Map.of("tipo", "deve ser uma das categorias: " + CATEGORIAS));
        return problema;
    }

    static final String CATEGORIAS = Arrays.stream(Categoria.values()).map(Categoria::codigo)
            .collect(Collectors.joining(", "));
}
