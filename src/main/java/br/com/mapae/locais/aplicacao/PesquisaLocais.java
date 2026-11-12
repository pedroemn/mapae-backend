package br.com.mapae.locais.aplicacao;

import br.com.mapae.locais.aplicacao.ConsultaLocais.Pagina;
import br.com.mapae.locais.dominio.LocalRepository;
import br.com.mapae.locais.dominio.LocalRepository.LocalNaArea;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pesquisa no catálogo ativo por nome, categoria ou termo. Termo e categoria vazios valem como
 * não informados. O resultado é sempre paginado e usa o mesmo item do mapa.
 */
@Service
@Transactional(readOnly = true)
public class PesquisaLocais {

    private final LocalRepository locais;

    public PesquisaLocais(LocalRepository locais) {
        this.locais = locais;
    }

    public Resultado pesquisar(String termo, String categoria, Integer pagina, Integer tamanho) {
        String trecho = vazioComoNulo(termo);
        Page<LocalNaArea> resultado = locais.pesquisar(trecho == null ? null : escaparLike(trecho),
                vazioComoNulo(categoria), ConsultaLocais.pedido(pagina, tamanho));
        return new Resultado(resultado.getContent(), Pagina.de(resultado));
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    /** %, _ e \ são literais: o usuário não consegue listar tudo com um curinga. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record Resultado(List<LocalNaArea> itens, Pagina pagina) {
    }
}
