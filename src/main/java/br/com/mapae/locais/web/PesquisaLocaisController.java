package br.com.mapae.locais.web;

import static br.com.mapae.locais.aplicacao.ConsultaLocais.PAGINA_MAXIMA;
import static br.com.mapae.locais.aplicacao.ConsultaLocais.TAMANHO_MAXIMO;

import br.com.mapae.locais.aplicacao.ConsultaLocais.Pagina;
import br.com.mapae.locais.aplicacao.PesquisaLocais;
import br.com.mapae.locais.aplicacao.PesquisaLocais.Filtros;
import br.com.mapae.locais.dominio.LocalRepository.ItemPesquisa;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    @Operation(summary = "Pesquisa locais do catálogo por termo e filtros",
            description = "Só locais ativos e com posição. Cada palavra do `termo` casa com nome, termos ou categoria, "
                    + "em qualquer ordem, sem diferenciar maiúsculas nem acentos, aceitando plural e erro leve de "
                    + "digitação. Filtros: `categoria` e `preco` (repetíveis), `abertoAgora` (horário de Aracaju) e "
                    + "distância (`latitude`, `longitude`, `raioMetros`). Filtro ausente não filtra. Ordem: relevância, "
                    + "distância (com origem), nome e id.")
    @GetMapping("/pesquisa")
    public PesquisaResponse pesquisar(
            @RequestParam(required = false) @Size(max = 100) @Pattern(regexp = SEM_NUL) String termo,
            @RequestParam(required = false) List<@Size(max = 50) @Pattern(regexp = SEM_NUL) String> categoria,
            @RequestParam(required = false) List<Integer> preco,
            @RequestParam(required = false) Boolean abertoAgora,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @RequestParam(required = false) @DecimalMin("1") @DecimalMax("100000") Double raioMetros,
            @RequestParam(required = false) @Min(0) @Max(PAGINA_MAXIMA) Integer pagina,
            @RequestParam(required = false) @Min(1) @Max(TAMANHO_MAXIMO) Integer tamanho) {
        PesquisaLocais.Resultado resultado = pesquisa.pesquisar(
                new Filtros(termo, categoria, preco, abertoAgora, latitude, longitude, raioMetros), pagina, tamanho);
        return new PesquisaResponse(resultado.itens().stream().map(ItemPesquisaResponse::de).toList(),
                resultado.pagina());
    }

    public record PesquisaResponse(List<ItemPesquisaResponse> itens, Pagina pagina) {
    }

    /**
     * O marcador do mapa mais {@code faixaPreco}, {@code abertoAgora} e {@code distanciaMetros}.
     * Os três vêm {@code null} quando o dado é desconhecido ou a pesquisa não tem origem.
     */
    public record ItemPesquisaResponse(Long id, String nome, String categoria, double latitude, double longitude,
                                       Integer faixaPreco, Boolean abertoAgora, Double distanciaMetros) {
        static ItemPesquisaResponse de(ItemPesquisa local) {
            return new ItemPesquisaResponse(local.getId(), local.getNome(), local.getCategoria(), local.getLatitude(),
                    local.getLongitude(), local.getFaixaPreco(), local.getAbertoAgora(), local.getDistanciaMetros());
        }
    }
}
