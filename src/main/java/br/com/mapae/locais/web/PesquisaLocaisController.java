package br.com.mapae.locais.web;

import static br.com.mapae.locais.aplicacao.ConsultaLocais.PAGINA_MAXIMA;
import static br.com.mapae.locais.aplicacao.ConsultaLocais.TAMANHO_MAXIMO;

import br.com.mapae.locais.aplicacao.ConsultaLocais.Pagina;
import br.com.mapae.locais.aplicacao.PesquisaLocais;
import br.com.mapae.locais.web.LocaisGeograficosController.LocalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Locais", description = "Consultas geográficas dos locais de clientes do Mapaê")
@RequestMapping("/api/v1/locais")
public class PesquisaLocaisController {

    /** O PostgreSQL não aceita o caractere NUL em texto; sem esta regra ele viraria 500. */
    static final String SEM_NUL = "[^\\x00]*";

    private final PesquisaLocais pesquisa;

    public PesquisaLocaisController(PesquisaLocais pesquisa) {
        this.pesquisa = pesquisa;
    }

    @Operation(summary = "Pesquisa locais do catálogo por nome, categoria ou termo",
            description = "Só locais ativos e com posição. `termo` casa como trecho do nome, da categoria ou de um termo; "
                    + "`categoria` filtra por igualdade. Nenhum dos dois diferencia maiúsculas nem acentos. "
                    + "Termo vazio vale como não informado. Ordem: nome, depois id.")
    @GetMapping("/pesquisa")
    public PesquisaResponse pesquisar(
            @RequestParam(required = false) @Size(max = 100) @Pattern(regexp = SEM_NUL) String termo,
            @RequestParam(required = false) @Size(max = 50) @Pattern(regexp = SEM_NUL) String categoria,
            @RequestParam(required = false) @Min(0) @Max(PAGINA_MAXIMA) Integer pagina,
            @RequestParam(required = false) @Min(1) @Max(TAMANHO_MAXIMO) Integer tamanho) {
        PesquisaLocais.Resultado resultado = pesquisa.pesquisar(termo, categoria, pagina, tamanho);
        return new PesquisaResponse(resultado.itens().stream().map(LocalResponse::de).toList(), resultado.pagina());
    }

    public record PesquisaResponse(List<LocalResponse> itens, Pagina pagina) {
    }
}
