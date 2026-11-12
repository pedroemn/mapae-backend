package br.com.mapae.locais;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.mapae.BancoDeTesteConfig;
import br.com.mapae.locais.dominio.Categoria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Filtros da pesquisa, busca tolerante e loja na categoria certa (spec filtros-de-locais, BACK-024).
 * O relógio fica parado em 23:30 UTC, que é 20:30 em Aracaju: um local das 18h às 23h está
 * aberto no horário de Aracaju e fechado no de UTC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({BancoDeTesteConfig.class, FiltrosPesquisaTests.RelogioParado.class})
@Transactional
class FiltrosPesquisaTests {

    static final double LAT = -10.95;
    static final double LNG = -37.05;

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioParado {
        @Bean
        @Primary
        Clock relogioParado() {
            return Clock.fixed(Instant.parse("2026-11-12T23:30:00Z"), ZoneOffset.UTC);
        }
    }

    static TimeZone fusoOriginal;

    /**
     * A JVM roda em UTC durante esta classe: se o código usasse o fuso padrão em vez do de Aracaju,
     * a máquina de desenvolvimento (UTC-3) esconderia o erro.
     */
    @BeforeAll
    static void jvmEmUtc() {
        fusoOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void restaurarFuso() {
        TimeZone.setDefault(fusoOriginal);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    CatalogoDeTeste catalogo;

    long padaria;
    long barRock;
    long pizzaria;
    long lanche;
    long cafe;
    long boate;

    /** Em ordem de distância da origem (LAT, LNG): 0 m, ~110 m, ~1,1 km, ~1,7 km, ~2,2 km, ~22 km. */
    @BeforeEach
    void preparar() {
        catalogo = new CatalogoDeTeste(jdbc);
        padaria = catalogo.ativo("Padaria Pão Quente", "padaria", "", LAT, LNG);
        catalogo.detalhes(padaria, null, null, null);
        barRock = catalogo.ativo("Bar do Rock", "bar", "cerveja|petiscos", LAT - 0.001, LNG);
        catalogo.detalhes(barRock, 2, "18:00", "02:00");
        pizzaria = catalogo.ativo("Pizzaria Napoli", "restaurante", "pizza|massas", LAT - 0.01, LNG);
        catalogo.detalhes(pizzaria, 3, "18:00", "23:00");
        lanche = catalogo.ativo("Lanche Feliz", "lanchonete", "", LAT + 0.015, LNG);
        catalogo.detalhes(lanche, 1, "00:00", "00:00");
        cafe = catalogo.ativo("Café Central", "cafe", "wifi", LAT - 0.02, LNG);
        catalogo.detalhes(cafe, 1, "07:00", "20:30");
        boate = catalogo.ativo("Boate Noite", "balada", "", LAT - 0.2, LNG);
        catalogo.detalhes(boate, 4, "20:30", "04:00");
    }

    static MockHttpServletRequestBuilder pesquisa(String... parametros) {
        MockHttpServletRequestBuilder pedido = get("/api/v1/locais/pesquisa");
        for (int i = 0; i < parametros.length; i += 2) {
            pedido.param(parametros[i], parametros[i + 1]);
        }
        return pedido;
    }

    ResultActions pesquisar(String... parametros) throws Exception {
        return mvc.perform(pesquisa(parametros));
    }

    static ResultActions ids(ResultActions resposta, long... esperados) throws Exception {
        resposta.andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(esperados.length));
        for (int i = 0; i < esperados.length; i++) {
            resposta.andExpect(jsonPath("$.itens[" + i + "].id").value(esperados[i]));
        }
        return resposta;
    }

    static ResultActions idsEmQualquerOrdem(ResultActions resposta, long... esperados) throws Exception {
        Integer[] numeros = Arrays.stream(esperados).mapToObj(id -> (int) id).toArray(Integer[]::new);
        return resposta.andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(esperados.length))
                .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder(numeros)));
    }

    static void problema(ResultActions resposta) throws Exception {
        resposta.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$", not(hasKey("trace"))))
                .andExpect(jsonPath("$", not(hasKey("exception"))));
    }

    // ---------- FILT: filtros ----------

    // FILT-01: uma categoria, por código ou nome, sem diferenciar maiúsculas nem acentos
    @ParameterizedTest
    @ValueSource(strings = {"cafe", "Café", "CAFE", " café "})
    void filtraPorUmaCategoria(String categoria) throws Exception {
        ids(pesquisar("categoria", categoria), cafe);
    }

    // FILT-01: várias categorias, repetindo o parâmetro ou separadas por vírgula
    @Test
    void filtraPorVariasCategorias() throws Exception {
        idsEmQualquerOrdem(mvc.perform(pesquisa().param("categoria", "bar", "Restaurante")), barRock, pizzaria);
        idsEmQualquerOrdem(pesquisar("categoria", "bar,restaurante"), barRock, pizzaria);
    }

    // FILT-02: uma ou mais faixas de preço
    @Test
    void filtraPorFaixaDePreco() throws Exception {
        idsEmQualquerOrdem(pesquisar("preco", "1"), lanche, cafe);
        idsEmQualquerOrdem(mvc.perform(pesquisa().param("preco", "1", "3")), lanche, cafe, pizzaria);
        ids(pesquisar("preco", "4"), boate);
    }

    // FILT-03: às 20:30 de Aracaju. Bar 18-02 vira a meia-noite; pizzaria 18-23 (fechada em UTC);
    // lanchonete 24 h; boate abre às 20:30 (inclusivo); café fecha às 20:30 (exclusivo); padaria sem horário.
    @Test
    void abertoAgoraNoHorarioDeAracaju() throws Exception {
        idsEmQualquerOrdem(pesquisar("abertoAgora", "true"), barRock, pizzaria, lanche, boate);
    }

    // FILT-03: bordas nos dois tipos de horário, às 20:30. Abre às 20:30 sem virar a meia-noite:
    // aberto. Fecha às 20:30 virando a meia-noite (22:00 às 20:30): fechado.
    @Test
    void bordasDoHorario() throws Exception {
        long abreAgora = catalogo.ativo("Sushi Tardio", "outro", "", LAT, LNG);
        catalogo.detalhes(abreAgora, null, "20:30", "23:00");
        long fechaAgora = catalogo.ativo("Pub Longo", "outro", "", LAT, LNG);
        catalogo.detalhes(fechaAgora, null, "22:00", "20:30");
        pesquisar("termo", "sushi").andExpect(jsonPath("$.itens[0].abertoAgora").value(true));
        pesquisar("termo", "longo").andExpect(jsonPath("$.itens[0].abertoAgora").value(false));
        idsEmQualquerOrdem(pesquisar("abertoAgora", "true", "categoria", "outro"), abreAgora);
    }

    // FILT-03 e FILT-07: o item diz se está aberto; sem horário vem null
    @Test
    void itemDizSeEstaAberto() throws Exception {
        pesquisar("termo", "central").andExpect(jsonPath("$.itens[0].abertoAgora").value(false));
        pesquisar("termo", "napoli").andExpect(jsonPath("$.itens[0].abertoAgora").value(true));
        pesquisar("termo", "padaria")
                .andExpect(jsonPath("$.itens[0]", hasKey("abertoAgora")))
                .andExpect(jsonPath("$.itens[0].abertoAgora").value(nullValue()));
    }

    // FILT-04 e FILT-11: com origem, todos com distância, do mais perto ao mais longe
    @Test
    void comOrigemOrdenaPorDistancia() throws Exception {
        ids(pesquisar("latitude", "-10.95", "longitude", "-37.05"), padaria, barRock, pizzaria, lanche, cafe, boate)
                .andExpect(jsonPath("$.itens[0].distanciaMetros").value(0.0))
                .andExpect(jsonPath("$.itens[1].distanciaMetros").value(
                        org.hamcrest.Matchers.closeTo(110.6, 1.0)));
    }

    // FILT-11: com origem e mesma distância, desempata por nome (o de nome menor tem id maior)
    @Test
    void mesmaDistanciaDesempataPorNome() throws Exception {
        long zeta = catalogo.ativo("Zeta", "outro", "", LAT + 0.05, LNG);
        long alfa = catalogo.ativo("Alfa", "outro", "", LAT + 0.05, LNG);
        ids(pesquisar("categoria", "outro", "latitude", "-10.95", "longitude", "-37.05"), alfa, zeta);
    }

    // FILT-04: o raio filtra
    @Test
    void raioFiltra() throws Exception {
        ids(pesquisar("latitude", "-10.95", "longitude", "-37.05", "raioMetros", "1500"), padaria, barRock, pizzaria);
        ids(pesquisar("latitude", "-10.95", "longitude", "-37.05", "raioMetros", "100000"),
                padaria, barRock, pizzaria, lanche, cafe, boate);
    }

    // FILT-04: sem origem não há distância, e a ordem é por nome
    @Test
    void semOrigemNaoHaDistancia() throws Exception {
        ids(pesquisar(), barRock, boate, cafe, lanche, padaria, pizzaria)
                .andExpect(jsonPath("$.itens[0]", hasKey("distanciaMetros")))
                .andExpect(jsonPath("$.itens[0].distanciaMetros").value(nullValue()));
    }

    // FILT-05: todos os filtros juntos, com termo e paginação
    @Test
    void filtrosCombinados() throws Exception {
        ids(pesquisar("termo", "pizza", "categoria", "restaurante", "preco", "3", "abertoAgora", "true",
                "latitude", "-10.95", "longitude", "-37.05", "raioMetros", "1500"), pizzaria);
        ids(pesquisar("termo", "pizza", "categoria", "restaurante", "preco", "2", "abertoAgora", "true",
                "latitude", "-10.95", "longitude", "-37.05", "raioMetros", "1500"));
        ids(pesquisar("termo", "pizza", "categoria", "bar"));
        ids(pesquisar("preco", "1", "abertoAgora", "true"), lanche);
        ids(pesquisar("categoria", "cafe", "raioMetros", "1500", "latitude", "-10.95", "longitude", "-37.05"));
    }

    // FILT-05: filtros paginam com os totais certos
    @Test
    void filtrosPaginam() throws Exception {
        ids(pesquisar("abertoAgora", "true", "tamanho", "3", "pagina", "1"), pizzaria)
                .andExpect(jsonPath("$.pagina.totalItens").value(4))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(2));
    }

    // FILT-06: filtro ausente, vazio ou abertoAgora=false não filtra
    @Test
    void filtroLimpoNaoFiltra() throws Exception {
        pesquisar().andExpect(jsonPath("$.pagina.totalItens").value(6));
        pesquisar("abertoAgora", "false").andExpect(jsonPath("$.pagina.totalItens").value(6));
        pesquisar("categoria", "").andExpect(jsonPath("$.pagina.totalItens").value(6));
        pesquisar("categoria", " ", "termo", " ").andExpect(jsonPath("$.pagina.totalItens").value(6));
    }

    // FILT-07: local sem preço ou sem horário fica fora do filtro; sem filtro, o campo vem null
    @Test
    void dadoDesconhecidoFicaForaDoFiltro() throws Exception {
        pesquisar("preco", "1", "preco", "2", "preco", "3", "preco", "4").andExpect(jsonPath("$.pagina.totalItens").value(5));
        pesquisar("abertoAgora", "true").andExpect(jsonPath("$.itens[*].id", not(org.hamcrest.Matchers.hasItem((int) padaria))));
        pesquisar("termo", "padaria")
                .andExpect(jsonPath("$.itens[0].faixaPreco").value(nullValue()))
                .andExpect(jsonPath("$.itens[0]", hasKey("faixaPreco")));
    }

    // FILT-08: origem incompleta ou raio sem origem
    @ParameterizedTest
    @CsvSource({"raioMetros,1000,,", "latitude,-10.95,,", "longitude,-37.05,,",
            "latitude,-10.95,raioMetros,1000"})
    void origemAusenteDa400(String p1, String v1, String p2, String v2) throws Exception {
        problema(p2 == null ? pesquisar(p1, v1) : pesquisar(p1, v1, p2, v2));
    }

    // FILT-09: valores fora da faixa
    @ParameterizedTest
    @CsvSource({"categoria,academia", "categoria,cafeteria", "categoria,b\u0000ar", "preco,0", "preco,5", "preco,abc",
            "abertoAgora,talvez", "raioMetros,0", "raioMetros,100001", "latitude,91", "longitude,-181"})
    void valorInvalidoDa400(String parametro, String valor) throws Exception {
        if (parametro.equals("raioMetros")) {
            problema(pesquisar(parametro, valor, "latitude", "-10.95", "longitude", "-37.05"));
        } else if (parametro.equals("latitude")) {
            problema(pesquisar(parametro, valor, "longitude", "-37.05"));
        } else if (parametro.equals("longitude")) {
            problema(pesquisar(parametro, valor, "latitude", "-10.95"));
        } else {
            problema(pesquisar(parametro, valor));
        }
    }

    // FILT-09: no máximo 20 valores de categoria e de preço
    @Test
    void valoresDemaisDa400() throws Exception {
        String[] vinteCategorias = new String[20];
        Arrays.fill(vinteCategorias, "bar");
        ids(mvc.perform(pesquisa().param("categoria", vinteCategorias)), barRock);
        String[] vinteEUma = Arrays.copyOf(vinteCategorias, 21);
        vinteEUma[20] = "bar";
        problema(mvc.perform(pesquisa().param("categoria", vinteEUma)));
        String[] precos = new String[21];
        Arrays.fill(precos, "1");
        problema(mvc.perform(pesquisa().param("preco", precos)));
    }

    // FILT-10: campos exatos do item
    @Test
    void itemTemOsCamposDoFiltro() throws Exception {
        pesquisar("termo", "napoli", "latitude", "-10.95", "longitude", "-37.05")
                .andExpect(jsonPath("$.itens[0].id").value(pizzaria))
                .andExpect(jsonPath("$.itens[0].nome").value("Pizzaria Napoli"))
                .andExpect(jsonPath("$.itens[0].categoria").value("restaurante"))
                .andExpect(jsonPath("$.itens[0].latitude").value(LAT - 0.01))
                .andExpect(jsonPath("$.itens[0].longitude").value(LNG))
                .andExpect(jsonPath("$.itens[0].faixaPreco").value(3))
                .andExpect(jsonPath("$.itens[0].abertoAgora").value(true))
                .andExpect(jsonPath("$.itens[0].distanciaMetros").value(org.hamcrest.Matchers.closeTo(1106, 5)))
                .andExpect(jsonPath("$.itens[0]", org.hamcrest.Matchers.aMapWithSize(8)));
    }

    // ---------- ACHAR: busca que encontra ----------

    // ACHAR-01: palavras em qualquer ordem
    @Test
    void palavrasEmQualquerOrdem() throws Exception {
        ids(pesquisar("termo", "napoli pizzaria"), pizzaria);
        ids(pesquisar("termo", "rock bar"), barRock);
        ids(pesquisar("termo", "massas napoli"), pizzaria);
    }

    // ACHAR-02: palavras de ligação são ignoradas; só elas, valem como palavra
    @Test
    void palavrasDeLigacao() throws Exception {
        ids(pesquisar("termo", "pizzaria de napoli"), pizzaria);
        ids(pesquisar("termo", "café do central"), cafe);
        ids(pesquisar("termo", "do"), barRock);
        ids(pesquisar("termo", "café com wifi"), cafe);
        ids(pesquisar("termo", "pizza para viagem"));
    }

    // ACHAR-03: plural casa com o singular ("bares" não casa por semelhança nem por categoria)
    @Test
    void pluralCasaComSingular() throws Exception {
        ids(pesquisar("termo", "bares"), barRock);
        ids(pesquisar("termo", "cafés"), cafe);
        ids(pesquisar("termo", "padarias"), padaria);
    }

    // ACHAR-04: erro leve de digitação no nome (semelhança) e na categoria (uma letra)
    @Test
    void erroDeDigitacao() throws Exception {
        ids(pesquisar("termo", "pizaria"), pizzaria);
        ids(pesquisar("termo", "napolli"), pizzaria);
        ids(pesquisar("termo", "restaurnte"), pizzaria);
        ids(pesquisar("termo", "lanchonet"), lanche);
        // 4 letras com uma trocada: "cafr" não casa com o nome por trigramas, só com a categoria cafe
        ids(pesquisar("termo", "cafr"), cafe);
    }

    // ACHAR-04: palavra curta não usa semelhança; sem relação não casa
    @Test
    void semelhancaTemLimite() throws Exception {
        ids(pesquisar("termo", "roc"), barRock);
        ids(pesquisar("termo", "rok"));
        ids(pesquisar("termo", "padoca"));
    }

    // ACHAR-05: sinônimo ou nome da categoria acha os locais dela
    @Test
    void sinonimoDaCategoria() throws Exception {
        ids(pesquisar("termo", "boteco"), barRock);
        ids(pesquisar("termo", "festa"), boate);
        ids(pesquisar("termo", "hamburgueria"), lanche);
        ids(pesquisar("termo", "restaurante"), pizzaria);
    }

    // ACHAR-05: a categoria casa pelo começo do nome ou do sinônimo, não por trecho: "ria" acha os
    // nomes "Pizzaria" e "Padaria", e não os bares (choperia) nem os cafés (cafeteria)
    @Test
    void categoriaCasaPeloComeco() throws Exception {
        idsEmQualquerOrdem(pesquisar("termo", "ria"), pizzaria, padaria);
        ids(pesquisar("termo", "teria"));
    }

    // ACHAR-07: uma palavra sem relação tira o local, mesmo que as outras casem
    @Test
    void palavraSemRelacaoExclui() throws Exception {
        ids(pesquisar("termo", "pizzaria xyzabc"));
        ids(pesquisar("termo", "rock pizza"));
    }

    // ACHAR-06: começa com o termo; contém; casa sem semelhança; só por semelhança
    @Test
    void ordemPorRelevancia() throws Exception {
        long contem = catalogo.ativo("Casa Pizzaria", "outro", "", LAT, LNG);
        long nosTermos = catalogo.ativo("Forno Bom", "outro", "pizzaria", LAT, LNG);
        // "Arte" vem antes de "Forno" no alfabeto: só a relevância põe o parecido depois dos termos
        long parecido = catalogo.ativo("Arte Pizzarria", "outro", "", LAT, LNG);
        ids(pesquisar("termo", "pizzaria"), pizzaria, contem, nosTermos, parecido);
    }

    // ACHAR-06: a relevância vem antes da distância; no mesmo nível, a distância decide
    @Test
    void relevanciaAntesDaDistancia() throws Exception {
        long contemLonge = catalogo.ativo("Casa Pizzaria", "outro", "", LAT - 0.05, LNG);
        long contemPerto = catalogo.ativo("Outra Pizzaria", "outro", "", LAT, LNG);
        ids(pesquisar("termo", "pizzaria"), pizzaria, contemLonge, contemPerto);
        ids(pesquisar("termo", "pizzaria", "latitude", "-10.95", "longitude", "-37.05"),
                pizzaria, contemPerto, contemLonge);
    }

    // Borda: %, _ e \ continuam literais nas palavras
    @Test
    void curingasContinuamLiterais() throws Exception {
        long promocao = catalogo.ativo("Bar 50% off", "outro", "", LAT, LNG);
        ids(pesquisar("termo", "50%"), promocao);
        ids(pesquisar("termo", "%"), promocao);
        ids(pesquisar("termo", "_"));
    }

    // ---------- LOJA: loja na categoria certa ----------

    ResultActions cadastrarLoja(String username, String tipo) throws Exception {
        String campoTipo = tipo == null ? "" : "\"tipo\":\"%s\",".formatted(tipo);
        return mvc.perform(post("/api/v1/lojas").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","nome":"Loja %s","email":"%s@exemplo.com","cep":"49010000",%s
                 "senha":"senha-forte-1"}""".formatted(username, username, username, campoTipo)));
    }

    // LOJA-01: tipo por código ou nome vira o código em tipo e categoria
    @ParameterizedTest
    @CsvSource({"Café,cafe", "BAR,bar", "restaurante,restaurante", "Lanchonete,lanchonete"})
    void tipoViraCategoria(String tipo, String codigo) throws Exception {
        cadastrarLoja("loja.nova", tipo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value(codigo));
        Map<String, Object> linha = jdbc.queryForMap("SELECT tipo, categoria FROM local WHERE username = 'loja.nova'");
        assertThat(linha).containsEntry("tipo", codigo).containsEntry("categoria", codigo);
    }

    // LOJA-01: com posição, a loja aparece no filtro da categoria
    @Test
    void lojaApareceNoFiltroDaCategoria() throws Exception {
        cadastrarLoja("boteco.ze", "bar").andExpect(status().isCreated());
        jdbc.update("UPDATE local SET posicao = CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography) "
                + "WHERE username = 'boteco.ze'", LNG, LAT);
        long loja = jdbc.queryForObject("SELECT id FROM local WHERE username = 'boteco.ze'", Long.class);
        idsEmQualquerOrdem(pesquisar("categoria", "bar"), barRock, loja);
    }

    // LOJA-02: tipo fora da lista dá 400 em campos.tipo e não cria a conta
    @ParameterizedTest
    @ValueSource(strings = {"academia", "cafeteria", "ba"})
    void tipoForaDaListaDa400(String tipo) throws Exception {
        cadastrarLoja("loja.errada", tipo).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos.tipo").exists());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM local WHERE username = 'loja.errada'", Integer.class))
                .isZero();
    }

    // LOJA-03: sem tipo (ou vazio), categoria nula
    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void tipoVazioFicaSemCategoria(String tipo) throws Exception {
        cadastrarLoja("loja.sem.tipo", tipo).andExpect(status().isCreated());
        cadastrarLoja("loja.sem.campo", null).andExpect(status().isCreated());
        assertThat(jdbc.queryForList("SELECT categoria FROM local WHERE username LIKE 'loja.sem.%'", String.class))
                .containsExactly(null, null);
    }

    // ---------- DADOS ----------

    // DADOS-01: colunas novas e pg_trgm no schema extensions
    @Test
    void migracaoCriaColunasEExtensao() {
        assertThat(jdbc.queryForObject("""
                SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace
                WHERE e.extname = 'pg_trgm'""", String.class)).isEqualTo("extensions");
        assertThat(jdbc.queryForList("""
                SELECT column_name || ':' || data_type || ':' || is_nullable FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'local'
                  AND column_name IN ('faixa_preco', 'abre', 'fecha')""", String.class))
                .containsExactlyInAnyOrder("faixa_preco:smallint:YES", "abre:time without time zone:YES",
                        "fecha:time without time zone:YES");
    }

    // DADOS-02: o banco recusa linha inválida, pela regra certa (a linha tem posição, então a
    // única regra violada é a da coluna testada)
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "categoria | 'academia' | ck_local_categoria",
            "categoria | 'Bar'      | ck_local_categoria",
            "faixa_preco | 0        | ck_local_faixa_preco",
            "faixa_preco | 5        | ck_local_faixa_preco",
            "abre      | '08:00'    | ck_local_horario",
            "fecha     | '18:00'    | ck_local_horario"})
    void bancoRecusaLinhaInvalida(String coluna, String valor, String regra) {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO local (nome, posicao, " + coluna + ") VALUES ('X', "
                + "CAST(ST_SetSRID(ST_MakePoint(-37.05, -10.9), 4326) AS geography), " + valor + ")"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(regra);
    }

    // DADOS-02: linha válida passa
    @Test
    void bancoAceitaLinhaValida() {
        assertThat(jdbc.update("INSERT INTO local (nome, posicao, categoria, faixa_preco, abre, fecha) "
                + "VALUES ('X', CAST(ST_SetSRID(ST_MakePoint(-37.05, -10.9), 4326) AS geography), 'outro', 4, '18:00', '02:00')")).isEqualTo(1);
    }

    // DADOS-03: a lista do CHECK é a mesma do enum
    @Test
    void listaDoBancoIgualADoCodigo() {
        String definicao = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_local_categoria'", String.class);
        Matcher codigo = Pattern.compile("'([a-z]+)'").matcher(definicao);
        Set<String> doBanco = new java.util.HashSet<>();
        while (codigo.find()) {
            doBanco.add(codigo.group(1));
        }
        assertThat(doBanco).isEqualTo(Arrays.stream(Categoria.values()).map(Categoria::codigo)
                .collect(Collectors.toSet()));
    }
}
