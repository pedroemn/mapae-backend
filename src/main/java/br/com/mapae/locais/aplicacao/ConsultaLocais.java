package br.com.mapae.locais.aplicacao;

import br.com.mapae.locais.dominio.LocalRepository;
import br.com.mapae.locais.dominio.LocalRepository.LocalNaArea;
import br.com.mapae.locais.dominio.LocalRepository.LocalProximo;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sem limites técnicos: o usuário pode consultar de qualquer lugar, com ou sem coordenadas, raio ou limite.
 * Só o catálogo ativo entra no mapa.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaLocais {

    public static final int TAMANHO_PADRAO = 20;

    public static final int TAMANHO_MAXIMO = 100;

    /**
     * Com {@link #TAMANHO_MAXIMO}, o deslocamento {@code pagina * tamanho} ainda cabe num int;
     * acima disso o Spring Data recusa a consulta e a resposta seria 500.
     */
    public static final int PAGINA_MAXIMA = Integer.MAX_VALUE / TAMANHO_MAXIMO;

    private static final int SEM_LIMITE = Integer.MAX_VALUE - 1;

    private final LocalRepository locais;

    public ConsultaLocais(LocalRepository locais) {
        this.locais = locais;
    }

    /**
     * Sem {@code pagina}, {@code tamanho} e {@code limite}, devolve todos (AD-028).
     * Com {@code pagina} ou {@code tamanho}, pagina; {@code limite} não combina com eles.
     */
    public ResultadoArea buscarNaArea(double oeste, double sul, double leste, double norte,
                                      Integer limite, Integer pagina, Integer tamanho) {
        if (oeste > leste) {
            throw new ConsultaGeograficaInvalidaException("oeste não pode ser maior que leste");
        }
        if (sul > norte) {
            throw new ConsultaGeograficaInvalidaException("sul não pode ser maior que norte");
        }
        if (pagina != null || tamanho != null) {
            if (limite != null) {
                throw new ConsultaGeograficaInvalidaException("limite não pode ser usado com pagina ou tamanho");
            }
            Page<LocalNaArea> resultado = locais.buscarNaArea(oeste, sul, leste, norte, pedido(pagina, tamanho));
            return new ResultadoArea(resultado.getContent(), resultado.hasNext(), Pagina.de(resultado));
        }
        int limiteEfetivo = limiteEfetivo(limite);
        List<LocalNaArea> encontrados = locais.buscarNaArea(oeste, sul, leste, norte, limiteEfetivo + 1);
        boolean truncado = encontrados.size() > limiteEfetivo;
        return new ResultadoArea(truncado ? encontrados.subList(0, limiteEfetivo) : encontrados, truncado, null);
    }

    public List<LocalProximo> buscarLocais(Double latitude, Double longitude, Double raioMetros, Integer limite) {
        if ((latitude == null) != (longitude == null)) {
            throw new ConsultaGeograficaInvalidaException("latitude e longitude devem ser informadas juntas");
        }
        if (latitude == null) {
            if (raioMetros != null) {
                throw new ConsultaGeograficaInvalidaException("raioMetros exige latitude e longitude");
            }
            return locais.listar(limiteEfetivo(limite));
        }
        if (raioMetros == null) {
            return locais.buscarPorDistancia(latitude, longitude, limiteEfetivo(limite));
        }
        return locais.buscarProximos(latitude, longitude, raioMetros, limiteEfetivo(limite));
    }

    /** Os limites de {@code pagina} e {@code tamanho} são validados na rota. */
    static PageRequest pedido(Integer pagina, Integer tamanho) {
        return PageRequest.of(pagina == null ? 0 : pagina, tamanho == null ? TAMANHO_PADRAO : tamanho);
    }

    private int limiteEfetivo(Integer limite) {
        // Teto interno só para o `limite + 1` da área não estourar o int; não é um limite de negócio.
        return limite == null ? SEM_LIMITE : Math.min(limite, SEM_LIMITE);
    }

    /** @param pagina nula quando a consulta não foi paginada */
    public record ResultadoArea(List<LocalNaArea> itens, boolean truncado, Pagina pagina) {
    }

    public record Pagina(int numero, int tamanho, long totalItens, int totalPaginas) {
        static Pagina de(Page<?> pagina) {
            return new Pagina(pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
        }
    }
}
