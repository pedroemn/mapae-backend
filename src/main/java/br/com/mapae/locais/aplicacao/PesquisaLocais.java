package br.com.mapae.locais.aplicacao;

import br.com.mapae.locais.aplicacao.ConsultaLocais.Pagina;
import br.com.mapae.locais.dominio.Categoria;
import br.com.mapae.locais.dominio.LocalRepository;
import br.com.mapae.locais.dominio.LocalRepository.ItemPesquisa;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pesquisa no catálogo ativo por termo e filtros (BACK-023 e BACK-024). Filtro ausente ou vazio
 * não filtra. O resultado é sempre paginado.
 */
@Service
@Transactional(readOnly = true)
public class PesquisaLocais {

    /** Horário de Aracaju (UTC-3, sem horário de verão), qualquer que seja o fuso do servidor (AD-047). */
    public static final ZoneId FUSO_ARACAJU = ZoneId.of("America/Maceio");

    public static final int RAIO_MAXIMO_METROS = 100_000;

    public static final int MAXIMO_VALORES = 20;

    /** Palavras de ligação ignoradas na busca, salvo se o termo só tiver elas (ACHAR-02). */
    static final Set<String> PALAVRAS_DE_LIGACAO = Set.of("de", "da", "do", "das", "dos", "e", "em", "na", "no",
            "a", "o", "as", "os", "com", "sem", "para", "pra");

    private final LocalRepository locais;
    private final Clock relogio;

    public PesquisaLocais(LocalRepository locais, Clock relogio) {
        this.locais = locais;
        this.relogio = relogio;
    }

    public Resultado pesquisar(Filtros filtros, Integer pagina, Integer tamanho) {
        String[] categorias = categorias(filtros.categorias());
        Integer[] precos = precos(filtros.precos());
        boolean comOrigem = origem(filtros);
        if (filtros.raioMetros() != null && !comOrigem) {
            throw new ConsultaGeograficaInvalidaException("raioMetros exige latitude e longitude");
        }
        List<String> palavras = palavras(filtros.termo());
        String[] categoriasDasPalavras = palavras.stream()
                .map(p -> Categoria.citadasPor(p).stream().map(Categoria::codigo).collect(Collectors.joining(" ")))
                .toArray(String[]::new);
        String termo = vazioComoNulo(filtros.termo());
        String termoNormal = termo == null ? "" : escaparLike(Categoria.normalizar(termo).replaceAll("\\s+", " "));

        Page<ItemPesquisa> resultado = locais.pesquisar(
                palavras.stream().map(PesquisaLocais::escaparLike).toArray(String[]::new), categoriasDasPalavras,
                termoNormal, categorias, precos,
                Boolean.TRUE.equals(filtros.abertoAgora()), LocalTime.ofInstant(relogio.instant(), FUSO_ARACAJU).withNano(0).toString(),
                comOrigem, comOrigem ? filtros.latitude() : 0, comOrigem ? filtros.longitude() : 0,
                filtros.raioMetros() != null, filtros.raioMetros() == null ? 0 : filtros.raioMetros(),
                ConsultaLocais.pedido(pagina, tamanho));
        return new Resultado(resultado.getContent(), Pagina.de(resultado));
    }

    private static boolean origem(Filtros filtros) {
        if ((filtros.latitude() == null) != (filtros.longitude() == null)) {
            throw new ConsultaGeograficaInvalidaException("latitude e longitude devem ser informadas juntas");
        }
        return filtros.latitude() != null;
    }

    /** Código ou nome de categoria; valor desconhecido é 400 (AD-045). */
    private static String[] categorias(List<String> textos) {
        List<String> informados = informados(textos);
        limitar(informados.size(), "categoria");
        return informados.stream()
                .map(t -> Categoria.de(t).orElseThrow(() -> new ConsultaGeograficaInvalidaException(
                        "categoria deve ser uma de: " + TipoDeLojaInvalidoException.CATEGORIAS)))
                .map(Categoria::codigo).distinct().toArray(String[]::new);
    }

    private static Integer[] precos(List<Integer> valores) {
        List<Integer> informados = valores == null ? List.of() : valores.stream().filter(v -> v != null).toList();
        limitar(informados.size(), "preco");
        if (informados.stream().anyMatch(v -> v < 1 || v > 4)) {
            throw new ConsultaGeograficaInvalidaException("preco deve ser de 1 a 4");
        }
        return informados.stream().distinct().toArray(Integer[]::new);
    }

    private static void limitar(int quantidade, String parametro) {
        if (quantidade > MAXIMO_VALORES) {
            throw new ConsultaGeograficaInvalidaException(parametro + " aceita no máximo " + MAXIMO_VALORES + " valores");
        }
    }

    private static List<String> informados(List<String> textos) {
        return textos == null ? List.of() : textos.stream().filter(t -> t != null && !t.isBlank()).toList();
    }

    /**
     * Palavras do termo, normalizadas, sem as de ligação e no singular simples (ACHAR-01 a ACHAR-03).
     * Vazia sem termo.
     */
    static List<String> palavras(String termo) {
        String texto = vazioComoNulo(termo);
        if (texto == null) {
            return List.of();
        }
        List<String> todas = Arrays.asList(Categoria.normalizar(texto).split("\\s+"));
        List<String> relevantes = todas.stream().filter(p -> !PALAVRAS_DE_LIGACAO.contains(p)).toList();
        List<String> palavras = new ArrayList<>();
        for (String palavra : relevantes.isEmpty() ? todas : relevantes) {
            String singular = singular(palavra);
            if (!palavras.contains(singular)) {
                palavras.add(singular);
            }
        }
        return palavras;
    }

    /** "bares" vira "bar" e "cafes" vira "cafe": o singular é trecho do plural, então casa com os dois. */
    static String singular(String palavra) {
        if (palavra.length() < 4 || !palavra.endsWith("s")) {
            return palavra;
        }
        if (palavra.endsWith("es") && palavra.length() - 2 >= 3) {
            return palavra.substring(0, palavra.length() - 2);
        }
        return palavra.substring(0, palavra.length() - 1);
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    /** %, _ e \ são literais: o usuário não consegue listar tudo com um curinga. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * @param categorias  códigos ou nomes; vazia não filtra
     * @param precos      faixas de 1 a 4; vazia não filtra
     * @param abertoAgora só {@code true} filtra
     * @param raioMetros  exige latitude e longitude
     */
    public record Filtros(String termo, List<String> categorias, List<Integer> precos, Boolean abertoAgora,
                          Double latitude, Double longitude, Double raioMetros) {
    }

    public record Resultado(List<ItemPesquisa> itens, Pagina pagina) {
    }
}
