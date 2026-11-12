package br.com.mapae.locais.web;

import br.com.mapae.locais.aplicacao.ConsultaLocais;
import br.com.mapae.locais.dominio.LocalRepository.LocalNaArea;
import br.com.mapae.locais.dominio.LocalRepository.LocalProximo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Locais", description = "Consultas geográficas dos locais de clientes do Mapaê")
@RequestMapping("/api/v1/locais")
public class LocaisGeograficosController {

    private final ConsultaLocais consulta;

    public LocaisGeograficosController(ConsultaLocais consulta) {
        this.consulta = consulta;
    }

    @Operation(summary = "Locais dentro da região visível do mapa",
            description = "Caixa em latitude/longitude. `truncado` indica que o `limite` cortou o resultado.")
    @GetMapping("/area")
    public AreaResponse area(
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double oeste,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double sul,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double leste,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double norte,
            @RequestParam(required = false) @Min(1) Integer limite) {
        ConsultaLocais.ResultadoArea resultado = consulta.buscarNaArea(oeste, sul, leste, norte, limite);
        return new AreaResponse(resultado.itens().stream().map(LocalResponse::de).toList(), resultado.truncado());
    }

    @Operation(summary = "Locais perto de um ponto, ou todos",
            description = "Sem coordenadas lista todos por id; com coordenadas ordena por distância. O raio só filtra se informado.")
    @GetMapping("/proximos")
    public ProximosResponse proximos(
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @RequestParam(required = false) @Positive Double raioMetros,
            @RequestParam(required = false) @Min(1) Integer limite) {
        List<LocalProximoResponse> itens = consulta.buscarLocais(latitude, longitude, raioMetros, limite)
                .stream().map(LocalProximoResponse::de).toList();
        return new ProximosResponse(itens);
    }

    public record AreaResponse(List<LocalResponse> itens, boolean truncado) {
    }

    public record ProximosResponse(List<LocalProximoResponse> itens) {
    }

    public record LocalResponse(Long id, String nome, double latitude, double longitude) {
        static LocalResponse de(LocalNaArea local) {
            return new LocalResponse(local.getId(), local.getNome(), local.getLatitude(), local.getLongitude());
        }
    }

    public record LocalProximoResponse(Long id, String nome, double latitude, double longitude,
                                       @JsonInclude(JsonInclude.Include.NON_NULL) Double distanciaMetros) {
        static LocalProximoResponse de(LocalProximo local) {
            return new LocalProximoResponse(local.getId(), local.getNome(), local.getLatitude(),
                    local.getLongitude(), local.getDistanciaMetros());
        }
    }
}
