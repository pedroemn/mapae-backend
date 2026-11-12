package br.com.mapae.locais.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Categorias fixas do catálogo (AD-045). A mesma lista está no CHECK {@code ck_local_categoria}
 * da tabela local; um teste confere que as duas não divergem.
 */
public enum Categoria {
    BAR("bar", "Bar", "boteco", "pub", "choperia", "cervejaria"),
    RESTAURANTE("restaurante", "Restaurante", "comida", "almoco", "jantar", "pizzaria", "churrascaria"),
    CAFE("cafe", "Café", "cafeteria"),
    PADARIA("padaria", "Padaria", "panificadora", "pao"),
    LANCHONETE("lanchonete", "Lanchonete", "lanche", "hamburgueria", "sanduiche", "salgado"),
    BALADA("balada", "Balada", "boate", "festa", "night"),
    EVENTO("evento", "Evento", "show", "festival", "feira"),
    OUTRO("outro", "Outro");

    private final String codigo;
    private final String nome;
    /** Código, nome e sinônimos já sem acento e em minúsculas. */
    private final List<String> palavras;

    Categoria(String codigo, String nome, String... sinonimos) {
        this.codigo = codigo;
        this.nome = nome;
        this.palavras = Stream.concat(Stream.of(codigo, nome), Arrays.stream(sinonimos))
                .map(Categoria::normalizar).distinct().toList();
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }

    /** Código ou nome, sem diferenciar maiúsculas nem acentos ("Café" e "CAFE" são {@code cafe}). */
    public static Optional<Categoria> de(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String procurado = normalizar(texto.strip());
        return Arrays.stream(values())
                .filter(c -> c.codigo.equals(procurado) || normalizar(c.nome).equals(procurado))
                .findFirst();
    }

    /**
     * Categorias a que uma palavra da busca se refere: o código, o nome ou um sinônimo começa com
     * a palavra (3 letras ou mais), ou difere dela em no máximo uma letra (4 letras ou mais).
     * Prefixo, e não trecho: "ria" não cita choperia, pizzaria e cafeteria (ACHAR-04, ACHAR-05).
     *
     * @param palavra já normalizada
     */
    public static List<Categoria> citadasPor(String palavra) {
        return Arrays.stream(values())
                .filter(c -> c.palavras.stream().anyMatch(p -> casa(p, palavra)))
                .toList();
    }

    private static boolean casa(String palavraDaCategoria, String palavra) {
        if (palavra.length() >= 3 && palavraDaCategoria.startsWith(palavra)) {
            return true;
        }
        return palavra.length() >= 4 && distanciaDeEdicao(palavraDaCategoria, palavra) <= 1;
    }

    /** Sem acento e em minúsculas, como o {@code lower(unaccent(...))} do banco. */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    static int distanciaDeEdicao(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int troca = anterior[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                atual[j] = Math.min(troca, Math.min(anterior[j] + 1, atual[j - 1] + 1));
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[b.length()];
    }
}
