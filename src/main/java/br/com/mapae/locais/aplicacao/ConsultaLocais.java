package br.com.mapae.locais.aplicacao;

import br.com.mapae.locais.dominio.LocalRepository;
import br.com.mapae.locais.dominio.LocalRepository.LocalNaArea;
import br.com.mapae.locais.dominio.LocalRepository.LocalProximo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sem limites técnicos: o usuário pode consultar de qualquer lugar, com ou sem coordenadas, raio ou limite.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaLocais {

    private static final int SEM_LIMITE = Integer.MAX_VALUE - 1;

    private final LocalRepository locais;

    public ConsultaLocais(LocalRepository locais) {
        this.locais = locais;
    }

    public ResultadoArea buscarNaArea(double oeste, double sul, double leste, double norte, Integer limite) {
        if (oeste > leste) {
            throw new ConsultaGeograficaInvalidaException("oeste não pode ser maior que leste");
        }
        if (sul > norte) {
            throw new ConsultaGeograficaInvalidaException("sul não pode ser maior que norte");
        }
        int limiteEfetivo = limiteEfetivo(limite);
        List<LocalNaArea> encontrados = locais.buscarNaArea(oeste, sul, leste, norte, limiteEfetivo + 1);
        boolean truncado = encontrados.size() > limiteEfetivo;
        return new ResultadoArea(truncado ? encontrados.subList(0, limiteEfetivo) : encontrados, truncado);
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

    private int limiteEfetivo(Integer limite) {
        return limite == null ? SEM_LIMITE : limite;
    }

    public record ResultadoArea(List<LocalNaArea> itens, boolean truncado) {
    }
}
