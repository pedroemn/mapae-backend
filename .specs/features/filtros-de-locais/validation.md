# Filtros de locais - Validação (iteração 3)

## Validation: filtros-de-locais - PASS ✅

**Data**: 2026-11-12
**Spec**: `.specs/features/filtros-de-locais/spec.md` (atualizada após a iteração 1: ACHAR-04, ACHAR-05 e palavras de ligação)
**Diff range**: working tree sobre `99e5b2e` (`git diff 99e5b2e` + não rastreados: `Categoria.java`, `RelogioConfig.java`, `TipoDeLojaInvalidoException.java`, `V20261112150000__filtros_de_locais.sql`, `FiltrosPesquisaTests.java`)
**Verifier**: sub-agente independente (autor ≠ verificador)

**Veredito: PASS.** Os 24 ACs têm teste que afirma o resultado da spec, o gate passa (283/0) e os 28 mutantes morreram.

### Iteração 3

- **Correção do autor**: em `ordemPorRelevancia` (`FPT:406-414`), o local que casa só por semelhança passou a se chamar "Arte Pizzarria". Ele vem antes de "Forno Bom" no alfabeto, então só a relevância o põe depois. Nenhum código de produção mudou.
- **M17 rodado de novo**, isolado (worktree no scratch, um único `mvnw`, `-Dtest=FiltrosPesquisaTests`, 64 testes): **morto** (`ordemPorRelevancia:413`, `$.itens[2].id` diferente do esperado).
- **Gate na árvore real**: `./mvnw -o test`, 283 passaram, 0 falharam, BUILD SUCCESS.
- **Isolamento**: worktree removido; `git status --porcelain` igual ao de antes; container `ryuk` do gate parado.

O restante deste relatório é a evidência da iteração 2, atualizada onde a iteração 3 mudou algo.

---

## Iteração 1 → 2: o que mudou e como foi conferido

| Lacuna da iteração 1 | Correção do autor | Conferência |
| -------------------- | ----------------- | ----------- |
| M04: abertura inclusiva sem teste no ramo normal | `FPT:187-198` `bordasDoHorario`: local 20:30-23:00 aberto às 20:30 | M04 morto (`bordasDoHorario:195`) |
| M07: fechamento exclusivo sem teste no ramo da meia-noite | mesmo teste: local 22:00-20:30 fechado às 20:30 | M07 morto (`bordasDoHorario:196`) |
| M03: dependência do fuso da JVM | `FPT:72-87`: JVM em UTC no `@BeforeAll`, restaurada no `@AfterAll` | M03 morto (6 falhas) |
| ACHAR-04: 5 letras no código × 4 na spec | `Categoria.java:72` `palavra.length() >= 4`; `FPT:370-371` `"cafr"` → `cafe` | M26 (voltar a 5) morto |
| ACHAR-05: trecho de sinônimo indefinido | spec define o **começo** (3+ letras); `Categoria.java:69` `startsWith`; `FPT:391-397` `"ria"` → `{pizzaria, padaria}` pelos nomes, `"teria"` → vazio | M25 (voltar a `contains`) morto |
| "com" citava restaurante | `com`, `sem`, `para`, `pra` nas palavras de ligação (`PesquisaLocais.java:35-36`, spec linha 42); `FPT:351-352` | M27 morto |
| FILT-11: desempate por nome com origem | `FPT:219-225` `mesmaDistanciaDesempataPorNome` (nome menor com id maior) | M28 (tirar o nome do ORDER BY) morto |

Observação: `TimeZone.setDefault` muda um estado global da JVM. Hoje é seguro, porque o surefire roda as classes em sequência e o `@AfterAll` restaura o fuso. Se a suíte passar a rodar em paralelo, isso precisa ser revisto.

---

## Spec-Anchored Acceptance Criteria

`FPT` = `src/test/java/br/com/mapae/locais/FiltrosPesquisaTests.java`. Relógio fixo em 23:30 UTC = 20:30 em Aracaju.

| AC | Resultado definido pela spec | `arquivo:linha` + asserção | Resultado |
| -- | ---------------------------- | -------------------------- | --------- |
| FILT-01 | só locais das categorias; código ou nome, sem caixa/acento | `FPT:158-170` `ids(..."Café"/"CAFE"/" café "), cafe)`; `bar`+`Restaurante` → `{barRock, pizzaria}` | ✅ PASS |
| FILT-02 | só `faixaPreco` entre os informados | `FPT:172-178` | ✅ PASS |
| FILT-03 | Aracaju, meia-noite, 24 h, abertura inclusiva, fechamento exclusivo | `FPT:180-185` `{barRock, pizzaria, lanche, boate}`; `FPT:187-198` bordas nos dois ramos; `FPT:200-208` | ✅ PASS |
| FILT-04 | `distanciaMetros` em cada item; raio filtra | `FPT:210-217`, `FPT:227-233` (1500 → 3, 100000 → 6), `FPT:235-241` (null sem origem) | ✅ PASS |
| FILT-05 | todos os filtros + termo + paginação | `FPT:243-261` (`totalItens` 4, `totalPaginas` 2) | ✅ PASS |
| FILT-06 | ausente, vazio, `abertoAgora=false` não filtra | `FPT:263-270` `totalItens` 6 | ✅ PASS |
| FILT-07 | sem dado fica fora do filtro; sem filtro vem `null` | `FPT:272-280`, `FPT:200-208` | ✅ PASS |
| FILT-08 | raio sem origem / coordenada só → 400 Problem Details | `FPT:282-288` | ✅ PASS |
| FILT-09 | valores fora da faixa e >20 valores → 400 sem detalhes internos | `FPT:290-318` com `not(hasKey("trace"/"exception"))` | ✅ PASS |
| FILT-10 | 8 campos exatos | `FPT:320-333` `aMapWithSize(8)` e valores | ✅ PASS |
| FILT-11 | distância, depois nome e `id` | `FPT:210-217`; `FPT:219-225` `ids(..., alfa, zeta)` | ✅ PASS |
| ACHAR-01 | palavras em qualquer ordem | `FPT:337-343` | ✅ PASS |
| ACHAR-02 | ignora palavras de ligação (agora com `com`, `sem`, `para`, `pra`), salvo se o termo só tem elas | `FPT:345-353` (`"do"` sozinho → `barRock`; `"café com wifi"` → `cafe`; `"pizza para viagem"` → vazio) | ✅ PASS |
| ACHAR-03 | plural casa com o singular | `FPT:355-361` | ✅ PASS |
| ACHAR-04 | 4+ letras: semelhança no nome/termos; distância de edição ≤ 1 com código/nome/sinônimo | `FPT:363-372` (`pizaria`, `napolli`, `restaurnte`, `lanchonet`, `cafr`); `FPT:374-380` | ✅ PASS |
| ACHAR-05 | código/nome/sinônimo **começa** com a palavra (3+); trecho do meio não cita | `FPT:382-389`; `FPT:391-397` | ✅ PASS |
| ACHAR-06 | começa > contém > sem semelhança > só semelhança; distância; nome; `id` | `FPT:406-413` `ids(..., pizzaria, contem, nosTermos, parecido)`; `FPT:415-423` | ✅ PASS (M17 morto na iteração 3) |
| ACHAR-07 | palavra sem relação exclui | `FPT:399-404` | ✅ PASS |
| LOJA-01 | código em `tipo` e `categoria` | `FPT:443-461`; `ContasSchemaTests.java:135-141` | ✅ PASS |
| LOJA-02 | 400 com `campos.tipo`, sem conta | `FPT:463-472` | ✅ PASS |
| LOJA-03 | sem `tipo` → categoria nula | `FPT:474-483` | ✅ PASS |
| DADOS-01 | colunas, CHECKs, `pg_trgm` em `extensions` | `FPT:486-498`; `ContasSchemaTests.java:106-109` | ✅ PASS |
| DADOS-02 | banco recusa linha inválida | `FPT:500-522` | ✅ PASS |
| DADOS-03 | lista do código = CHECK | `FPT:524-536` | ✅ PASS |

**Status**: ✅ 24/24 ACs com evidência e valor da spec.

### Testes antigos alterados (`PesquisaLocaisTests`)
Sem mudança desde a iteração 1: legítimos e sem enfraquecimento. `"cafeter"` continua citando `cafe`, porque "cafeteria" começa com ele, coerente com a nova regra do começo.

---

## Edge Cases

- [x] `%`, `_` e `\` literais: `FPT:425-431`, `PesquisaLocaisTests.java:270-285` (M01 morto).
- [x] SQL no termo é texto: `PesquisaLocaisTests.java:287-294` e sondas da iteração 1.
- [x] Fechamento exato fechado / abertura exata aberta, nos dois ramos: `FPT:180-198` (M04 a M07 mortos).
- [x] Sem filtro nem termo, catálogo por nome: `FPT:235-241`.

---

## Gate Check

- **Comando**: `./mvnw -o test` (árvore real)
- **Resultado**: 283 passaram, 0 falharam, 0 ignorados. BUILD SUCCESS.
- **Antes da feature**: 219. **Iteração 1**: 280. **Agora**: 283 (+3: `bordasDoHorario`, `mesmaDistanciaDesempataPorNome`, `categoriaCasaPeloComeco`).

---

## Discrimination Sensor

`git worktree` temporário no scratch, com o diff aplicado. Rodou **um mutante por vez**, com um único `mvnw` e `-Dtest=FiltrosPesquisaTests` (64 testes). A contagem de testes executados foi conferida no log de cada mutante. Ao final, o worktree foi removido, o `git status --porcelain` da árvore real ficou igual ao de antes, e o container `ryuk` restante foi parado.

| # | Arquivo:linha | Mutação | Resultado |
| - | ------------- | ------- | --------- |
| M01 | `PesquisaLocais.java:144` | sem escape do LIKE | ✅ Morto (iteração 1, FPT + PesquisaLocaisTests) |
| M02 | `PesquisaLocais.java:28` | fuso → UTC | ✅ Morto (iteração 1) |
| M03 | `PesquisaLocais.java:28` | fuso → `ZoneId.systemDefault()` | ✅ Morto (6 falhas) |
| M04 | `LocalRepository.java:72` | abertura exclusiva, ramo normal | ✅ Morto |
| M05 | `LocalRepository.java:72` | fechamento inclusivo, ramo normal | ✅ Morto (iteração 1) |
| M06 | `LocalRepository.java:73` | abertura exclusiva, ramo meia-noite | ✅ Morto (iteração 1) |
| M07 | `LocalRepository.java:73` | fechamento inclusivo, ramo meia-noite | ✅ Morto |
| M08 | `LocalRepository.java:73` | virada da meia-noite → `ELSE false` | ✅ Morto |
| M09 | `LocalRepository.java:71` | `abre = fecha` → fechado | ✅ Morto |
| M10 | `LocalRepository.java:57` | sem `l.categoria IS NOT NULL` | ✅ Morto em `PesquisaLocaisTests` (3 falhas). Sobrevive só com FPT, que não tem local sem categoria |
| M11 | `PesquisaLocais.java:116` | palavras de ligação não ignoradas | ✅ Morto |
| M12 | `PesquisaLocais.java:119` | sem singular | ✅ Morto |
| M13 | `LocalRepository.java:61` | limiar 0.5 → 0.3 | ✅ Morto |
| M14 | `LocalRepository.java:61` | limiar 0.5 → 0.7 | ✅ Morto |
| M15 | `Categoria.java:72` | sem distância de edição na categoria | ✅ Morto |
| M16 | `LocalRepository.java:100` | "começa com" empata com "contém" | ✅ Morto |
| M17 | `LocalRepository.java:102` | "casa sem semelhança" (2) empata com "só semelhança" (3) | ✅ Morto (iteração 3; sobreviveu na iteração 2) |
| M18 | `LocalRepository.java:149` | distância antes da relevância | ✅ Morto |
| M19 | `LocalRepository.java:89` | sem filtro de raio | ✅ Morto |
| M20 | `PesquisaLocais.java:82` | categoria desconhecida vira `outro` (sem 400) | ✅ Morto |
| M21 | `Local.java:62` | cadastro não grava `categoria` | ✅ Morto |
| M22 | `PesquisaLocais.java:90` | `preco` 5 aceito | ✅ Morto |
| M23 | `PesquisaLocais.java:97` | sem limite de 20 valores | ✅ Morto |
| M24 | `ContasDeLoja.java` (`categoriaDoTipo`) | tipo inválido aceito | ✅ Morto |
| M25 | `Categoria.java:69` | categoria volta a casar por trecho | ✅ Morto |
| M26 | `Categoria.java:72` | distância de edição só com 5+ letras | ✅ Morto |
| M27 | `PesquisaLocais.java:36` | sem `com`/`sem`/`para`/`pra` | ✅ Morto |
| M28 | `LocalRepository.java:149` | sem desempate por nome depois da distância | ✅ Morto |

**Profundidade**: expandida. **Resultado**: 28/28 mortos. ✅ PASS.

**M17 na iteração 2**: "Forno Bom" (nível 2) e "Pizzarria Roma" (nível 3) já ficavam na ordem esperada pelo desempate por nome. Com "Arte Pizzarria", juntar os dois níveis inverte a ordem, e o teste falha.

---

## Sondas de segurança e conexão

Sem mudança no caminho de entrada desde a iteração 1: o controller e a validação não mudaram, e as sondas daquela iteração continuam válidas. Valores extremos (`NaN`, `Infinity`, `1e309`, `99999999999`) dão 400. SQL, aspas, regex e NUL no termo dão 200 ou 400, nunca 500. Listas acima de 20 valores dão 400. A rota pública ignora o token (AD-041). Nenhuma resposta traz trace, exception ou SQL. `unaccent` e `pg_trgm` vão qualificados com `extensions.`; o PostGIS depende do `search_path` (AD-017, pré-existente). A hora vai ao banco como texto em `America/Maceio`, e agora isso é provado também com a JVM em UTC (M03 morto). A busca não usa índice, o que a AD-049 aceita enquanto a base for pequena.

---

## Code Quality

| Princípio | Status |
| --------- | ------ |
| Código mínimo / mudanças cirúrgicas | ✅ |
| Spec, AD-049 e contrato atualizados junto com o código | ✅ |
| Resultado afirmado = resultado da spec | ✅ |
| Asserções discriminantes | ✅ (28/28) |
| Todo teste mapeia para AC ou borda | ✅ |

---

## Fix Plans

Nenhum. O Fix 1 da iteração 2 (M17) foi aplicado e conferido na iteração 3.

---

## Requirement Traceability Update

| Requisitos | Status anterior | Novo status |
| ---------- | --------------- | ----------- |
| FILT-01 a FILT-11, ACHAR-01 a ACHAR-07, LOJA-01 a LOJA-03, DADOS-01 a DADOS-03 (24) | Implemented | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 24/24 ACs com evidência e resultado da spec
**Sensor**: 28/28 mutantes mortos
**Gate**: 283 passaram, 0 falharam

**O que funciona**: filtros combinados, horário de Aracaju com virada da meia-noite e bordas nos dois ramos (independente do fuso da JVM), distância e raio, busca por palavras, palavras de ligação, plural, semelhança, categoria pelo começo, ordem por relevância, cadastro de loja com categoria, CHECKs no banco. Entradas hostis dão 200 ou 400, nunca 500.

**Pendências fora do escopo da verificação**: migração `V20261112150000__filtros_de_locais` ainda não aplicada no Supabase; a busca não usa índice (AD-049); as funções do PostGIS dependem do `search_path` (AD-017).
