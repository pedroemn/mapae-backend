# Locais no mapa e pesquisa - Validação (iteração 2)

## Validation: locais-mapa-e-pesquisa - PASS

**Data**: 2026-11-12
**Spec**: `.specs/features/locais-mapa-e-pesquisa/spec.md` (25 ACs: CAT-01 a CAT-03, MAPA-01 a MAPA-10, BUSCA-01 a BUSCA-12; MAPA-02, MAPA-06, BUSCA-07 e BUSCA-09 emendadas; 3 linhas novas de premissa)
**Cards**: BACK-022 (Consultar locais por área visível do mapa) e BACK-023 (Implementar pesquisa de locais)
**Diff range**: working tree sobre HEAD `5c04b71` (branch `PesquisaLocais`; nada commitado, AD-001). Mesma superfície da iteração 1, com as correções de G1 a G4, R3, R4 e a constante única `CATALOGO_ATIVO` em `LocalRepository.java`.
**Verificador**: sub-agente independente (autor diferente do verificador), iteração 2 de 3. Não alterou código nem testes reais; sondas e mutantes só em cópias scratch. Não conectou no Supabase nem leu valores do `.env`.

**Veredito: PASS.** Gate 219/0/0 na árvore real (rodado de novo depois da constante `CATALOGO_ATIVO`). As 25 ACs têm evidência `file:line` com asserção no resultado da spec. **Sensor: 20/20 mutantes mortos**, inclusive os dois que sobreviveram na iteração 1 (M7 e M18), os cinco do filtro de status por consulta, os três da própria constante, os dois de `PAGINA_MAXIMA` ±1, os quatro de anotação (`@Pattern`, `@Max(pagina)`) e a amostra de regressão (leitor de token nos dois sentidos, escape de `%`). As sondas no servidor real confirmam G1 (página extrema dá 400 em Problem Details; `pagina=21474836&tamanho=100` dá 200), G2 (NUL dá 400) e R4 (loja `BLOQUEADO`/`INATIVO` fora das cinco consultas e dos totais). Nenhuma brecha de segurança nem de conexão. Fica só uma ação do desenvolvedor fora da feature (A1: variáveis `CLOUDFLARE_*` no `.env`).

Histórico desta iteração: a primeira corrida do sensor foi encerrada pelo Claude Code por memória baixa antes de medir qualquer mutante. A pedido do orquestrador, o sensor rodou de novo em lotes de 3 mutantes por chamada, com JVMs menores (`MAVEN_OPTS=-Xmx384m`, `-DargLine=-Xmx512m -XX:+UseSerialGC`) e o resultado de cada mutante gravado assim que terminava. Nenhuma corrida foi encerrada.

---

## Task Completion

Não há `tasks.md` nem `design.md`; a conferência é contra a spec e os cards.

| Entrega | Status | Notas |
| ------- | ------ | ----- |
| Catálogo pesquisável (migração `categoria`, `termos`, `ativo`, `unaccent`) | Feito | `CatalogoSchemaTests` 4/4; migração igual à da iteração 1 |
| BACK-022: `/area` só ativos, `categoria`, paginação opcional, teto de `pagina` | Feito | `MapaAreaTests` 23/23 (+2) |
| BACK-022: `/proximos` só ativos e `categoria`, sem loja bloqueada/inativa | Feito | `MapaAreaTests.java:111-129` |
| BACK-023: `/pesquisa` paginada, teto de `pagina`, NUL recusado | Feito | `PesquisaLocaisTests` 33/33 (+4) |
| G1 (Major, página extrema dava 500) | Corrigido | `ConsultaLocais.java:28` (`PAGINA_MAXIMA = Integer.MAX_VALUE / TAMANHO_MAXIMO`), `LocaisGeograficosController.java:46`, `PesquisaLocaisController.java:43`; P1, P2, P5, P6 mortos |
| G2 (Minor, NUL dava 500) | Corrigido | `PesquisaLocaisController.java:27`, `:41-42`; P3, P4 mortos |
| G3 (Minor, M7 sobrevivia) | Corrigido | `PesquisaLocaisTests.java:263-278`; M7 morto |
| G4 (Minor, M18 sobrevivia) | Corrigido | `PesquisaLocaisTests.java:182-195`; M18 morto |
| R3 (redação da AD-043) | Corrigido | AD-043 e javadoc do `PESQUISA` em `LocalRepository.java` dizem que só o `unaccent` é qualificado e que o PostGIS depende do `connection-init-sql` (AD-017) |
| R4 (loja bloqueada no catálogo) | Implementado | `CATALOGO_ATIVO` em `LocalRepository.java:22`, usado em `:35` (área, nas duas variantes e no `count`), `:48` (pesquisa e `count`), `:98` (`listar`), `:113` (`buscarPorDistancia`), `:130` (`buscarProximos`); spec MAPA-02/BUSCA-09 e AD-040 atualizadas; premissa marcada "Confirmado? n" |
| Gap opcional 3 da iteração anterior (filtro repetido) | Feito | Constante única `CATALOGO_ATIVO`; sem mudança de comportamento (gate e sensor) |

---

## Spec-Anchored Acceptance Criteria

| AC | Resultado definido na spec | `file:line` + asserção | Resultado |
| -- | -------------------------- | ---------------------- | --------- |
| CAT-01 | Colunas `categoria VARCHAR(50)` nula, `termos TEXT[] NOT NULL DEFAULT '{}'`, `ativo BOOLEAN NOT NULL DEFAULT true`; `unaccent` em `extensions` | `src/test/java/br/com/mapae/locais/CatalogoSchemaTests.java:50-55` - `containsEntry("character_maximum_length", 50)`, `containsEntry("column_default", "'{}'::text[]")`, `containsEntry("column_default", "true")`; `:64` - `isEqualTo("extensions")`; `src/test/java/br/com/mapae/conta/ContasSchemaTests.java:101-104` | OK |
| CAT-02 | Locais antigos: `ativo = true`, `termos` vazio, `categoria` nula | `CatalogoSchemaTests.java:100-102` - `hasSize(1)`, `containsEntry("ativo", true).containsEntry("termos", 0).containsEntry("categoria", null)` | OK |
| CAT-03 | Loja nasce ativa, termos vazios, sem categoria | `CatalogoSchemaTests.java:79` | OK |
| MAPA-01 | 200, ativos com posição na caixa, por `id`, 5 campos | `src/test/java/br/com/mapae/locais/MapaAreaTests.java:79-88` - `isOk()`, `length()).value(2)`, `categoria` = `"cafe"`, `closeTo(-10.95, 1e-9)`, `hasKey("categoria")` nula | OK (K2 morto) |
| MAPA-02 | Inativo, ou loja com `status` ≠ `ATIVO`, fora de `/area` e `/proximos` | `MapaAreaTests.java:97-107` (inativo nas 4 consultas); `:117` área, `:119-121` `listar`, `:122-124` raio, `:126` área com `INATIVO`, `:127-128` distância (`length()).value(0)`) | OK (Q1, Q3, Q4, Q5, K1, K3, K4 mortos, cada um na asserção da sua rota) |
| MAPA-03 | Só a página pedida (0 e 20), `pagina` com 4 campos, `truncado` = há próxima | `MapaAreaTests.java:151-163`, `:172-178` | OK |
| MAPA-04 | Sem paginação: todos, `truncado = false`, sem `pagina` | `MapaAreaTests.java:187-189` - `not(hasKey("pagina"))` | OK |
| MAPA-05 | `limite` + `pagina`/`tamanho`: 400 Problem Details | `MapaAreaTests.java:196-199` - `$.detail` = `"limite não pode ser usado com pagina ou tamanho"` | OK |
| MAPA-06 | `pagina` < 0 ou > 21474836, `tamanho` fora de 1..100: 400 Problem Details | `MapaAreaTests.java:206-209`; `:215-217` - `pagina=21474836&tamanho=100` `isOk()`; `:218-219` - `21474837` `isBadRequest()` + `APPLICATION_PROBLEM_JSON`; `:220` - `2147483647` `isBadRequest()` | OK (P1, P2, P5 mortos) |
| MAPA-07 | Caixa vazia: 200, `itens` vazio, `truncado = false`; paginada, totais 0 | `MapaAreaTests.java:234-241` | OK |
| MAPA-08 | Coordenada fora da faixa, `oeste > leste`, `sul > norte`: 400 | `MapaAreaTests.java:246-251` | OK |
| MAPA-09 | Problem Details sem `trace`, `exception`, `message` nem SQL | `MapaAreaTests.java:258-265`; sondas G1g/G1h: `{"detail":"Validation failure","instance","status":400,"title"}` | OK |
| MAPA-10 | 200 para visitante, token válido, expirado e malformado | `MapaAreaTests.java:272-278` | OK (R2 morto em `:276`) |
| BUSCA-01 | Trecho de nome, categoria ou termo, sem maiúsculas nem acentos | `src/test/java/br/com/mapae/locais/PesquisaLocaisTests.java:89-91`, `:97-105` | OK |
| BUSCA-02 | `categoria` por igualdade, sem maiúsculas nem acentos | `PesquisaLocaisTests.java:112-114`, `:120` | OK |
| BUSCA-03 | `termo` e `categoria` juntos | `PesquisaLocaisTests.java:126-130` | OK |
| BUSCA-04 | Termo ausente, vazio ou só espaços: só os outros filtros | `PesquisaLocaisTests.java:137-139`, `:145-148` | OK |
| BUSCA-05 | Sem resultado: 200, vazio, totais 0 | `PesquisaLocaisTests.java:154-157` | OK |
| BUSCA-06 | Sempre paginada (0 e 20), ordem por nome sem acento e sem maiúsculas, depois `id` | `PesquisaLocaisTests.java:163-169`, `:177-179`; `:190-194` - `itens[0]` = `maiusculo` ("BAR ZETA"), `[1]` = `minusculo`, `[2]` = `comAcento` ("Café Xique"), `[3]` = `semAcento`; `:200-206`, `:215-217` | OK (M18 morto em `:191`) |
| BUSCA-07 | `pagina` < 0 ou > 21474836, `tamanho` fora de 1..100, `termo` > 100, `categoria` > 50, NUL em `termo`/`categoria`: 400 Problem Details sem detalhes internos | `PesquisaLocaisTests.java:224-227`; `:233-235` - 21474836 `isOk()` com `totalItens` 4; `:236-239` - 21474837 e 2147483647 `isBadRequest()`, `APPLICATION_PROBLEM_JSON`, `not(hasKey("trace"))`; `:246-249` - `"caf\u0000"` e `"bar\u0000"` `isBadRequest()` + `APPLICATION_PROBLEM_JSON`; `:255-260` | OK (P1, P2, P3, P4, P6 mortos) |
| BUSCA-08 | `%`, `_` (e `\`, AD-043) literais | `PesquisaLocaisTests.java:271-272` - `"%"` acha só `promocao` e `desconto`; `:273-274` - `"_"` só `sublinhado`; `:275-277` - `"\\"` só `barra`; `:283-287` | OK (M7 morto em `:277`, R3 morto em `:271`) |
| BUSCA-09 | Inativo, sem posição ou loja com `status` ≠ `ATIVO` fora | `PesquisaLocaisTests.java:299-301`; `:310-312` - `BLOQUEADO` fora (`containsInAnyOrder(cafeCentral, padaria, liberada)`); `:314` - `INATIVO` fora (`length()).value(2)`) | OK (Q2, K1, K3, K4 mortos) |
| BUSCA-10 | Mesmos 5 campos do marcador | `PesquisaLocaisTests.java:320-330` | OK |
| BUSCA-11 | Página depois da última: 200, vazio, totais corretos | `PesquisaLocaisTests.java:336-340`; `:233-235` | OK. Página acima de 21474836 é 400 por emenda explícita da BUSCA-07/MAPA-06 |
| BUSCA-12 | 200 para visitante, token válido, expirado e malformado | `PesquisaLocaisTests.java:346-355` | OK (R2 morto em `:353`) |

**Status**: 25/25 ACs com evidência `file:line` e asserção no resultado da spec. 0 lacunas de precisão.

---

## Critérios dos cards

| Card | Critério | Evidência | Resultado |
| ---- | -------- | --------- | --------- |
| BACK-022 | Contrato por limites, paginação e campos do marcador | `docs/contrato-geografico.md` ("Catálogo e acesso", "Paginação" com `pagina` de 0 a 21474836, `/area`); `LocaisGeograficosController.java:39-51`; `DocumentacaoApiTests.java:30` | OK |
| BACK-022 | Validar coordenadas e limites | `LocaisGeograficosController.java:41-47`; `MapaAreaTests.java:196-251` | OK |
| BACK-022 | Só locais ativos na área | `LocalRepository.java:22`, `:35`; `MapaAreaTests.java:97-128` | OK |
| BACK-022 | Área sem locais e erros sem dados internos | `MapaAreaTests.java:234-265`; sondas G1 sem 500 | OK |
| BACK-022 | Testes de consultas geográficas e regras de acesso | `MapaAreaTests` 23 testes; `:272-287` | OK |
| BACK-022 | Visitantes e autenticados | `MapaAreaTests.java:272-278`; sondas T-* | OK |
| BACK-023 | Campos pesquisáveis e contrato de termo, filtros e paginação | `docs/contrato-geografico.md` (`/pesquisa`, "sem o caractere NUL"); AD-043; `PesquisaLocaisController.java:38-46` | OK |
| BACK-023 | Nome, categoria e termos só no catálogo ativo | `LocalRepository.java:44-57`; `PesquisaLocaisTests.java:97-105`, `:299-314` | OK |
| BACK-023 | Resumo dos resultados e paginação consistente com mapa e lista | Mesmo `LocalResponse` (`LocaisGeograficosController.java:75`) e mesmo `Pagina`; `PesquisaLocaisTests.java:320-330` | OK |
| BACK-023 | Termo vazio, sem resultado, filtros combinados | `PesquisaLocaisTests.java:126-158` | OK |

---

## Edge Cases

- [x] Token inválido em `/api/v1/contas/**` continua 401: `MapaAreaTests.java:283-286`; R1 morto (13 testes de conta); sondas P-* (14 variantes × 3 cabeçalhos, nenhuma 200).
- [x] Acento no termo e não no nome (e o contrário): `PesquisaLocaisTests.java:87-91`, `:104-105`, `:110`.
- [x] Termos vazios e categoria nula, achado só pelo nome: `PesquisaLocaisTests.java:328-330`.
- [x] CORS: `PesquisaLocaisTests.java:361-363`; sondas C1 a C4.
- [x] Página extrema (G1): 400 Problem Details; no limite, 200.
- [x] NUL no texto (G2): 400 Problem Details.
- [x] Loja `BLOQUEADO`/`INATIVO` com posição (R4): fora das cinco consultas e dos totais.

---

## Discrimination Sensor

Execução: cópia nova da árvore real (depois da constante `CATALOGO_ATIVO`) em `scratchpad/verifier-locais3`, sem `target/`, `.git/`, `.env`, `.idea/`. Script `scratchpad/sensor3/mutar.py`: troca exata de texto (aborta se o trecho não aparece exatamente uma vez), um mutante por execução do Maven, restauração em `finally`, resultado gravado assim que cada mutante termina. Comando: `./mvnw -q test -Dtest=MapaAreaTests,PesquisaLocaisTests,ConsultasGeograficasTests,CatalogoSchemaTests,ContaAutenticadaTests,TrocaSenhaTests,SegurancaTests,CorsTests,LoginTests,CadastroLojaTests` (163 testes), com `MAVEN_OPTS=-Xmx384m` e `-DargLine=-Xmx512m -XX:+UseSerialGC` por causa da memória da máquina (cerca de 300 MB livres antes; 800 MB durante). O controle sem mutação passou (163/0). Morto = compilou e teve falha ou erro de teste.

| # | `file:line` mutado | Mutação | Morto? | Teste e linha que matou |
| - | ------------------ | ------- | ------ | ----------------------- |
| M7 | `src/main/java/br/com/mapae/locais/aplicacao/PesquisaLocais.java:38` | Não escapa `\` (só `%` e `_`) | Morto | `PesquisaLocaisTests.curingasSaoLiterais:277` - `itens[0].id` esperado `barra`, veio `desconto` |
| M18 | `src/main/java/br/com/mapae/locais/dominio/LocalRepository.java:84` | `ORDER BY l.nome, l.id` (sem `lower`/`unaccent`) | Morto | `PesquisaLocaisTests.empateSemAcentoESemMaiusculasSaiPorId:191` - `itens[0].id` trocado |
| Q1 | `LocalRepository.java:35` (`NA_AREA`: área com e sem paginação e `count`) | `CATALOGO_ATIVO` vira só `l.ativo` | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa:117` - 2 itens, esperado 1 |
| Q2 | `LocalRepository.java:48` (`PESQUISA` e `count`) | Idem | Morto | `PesquisaLocaisTests.lojaSemStatusAtivoFicaFora:310` - 4 itens, esperado 3 |
| Q3 | `LocalRepository.java:98` (`listar`) | Idem | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa:120` |
| Q4 | `LocalRepository.java:113` (`buscarPorDistancia`) | Idem | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa:128` - 2 itens, esperado 0 |
| Q5 | `LocalRepository.java:130` (`buscarProximos`, com raio) | Idem | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa:124` |
| K1 | `LocalRepository.java:22` (constante) | Tira a cláusula de status | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa`, `PesquisaLocaisTests.lojaSemStatusAtivoFicaFora` |
| K2 | `LocalRepository.java:22` | Tira `l.status IS NULL OR` | Morto | 37 testes (todo local sem conta some), entre eles `MapaAreaTests.devolveOsLocaisAtivosDaCaixaComOsCamposDoMarcador` |
| K3 | `LocalRepository.java:22` | `l.status <> 'BLOQUEADO'` (deixa `INATIVO` passar) | Morto | `MapaAreaTests.lojaSemStatusAtivoNaoApareceNoMapa:126`, `PesquisaLocaisTests.lojaSemStatusAtivoFicaFora:314` |
| K4 | `LocalRepository.java:22` | `l.ativo` vira `true` (regressão de M1/M4) | Morto | `MapaAreaTests.localInativoNaoApareceNaAreaNemNosProximos:97`, `PesquisaLocaisTests.inativoESemPosicaoFicamFora:299` |
| P1 | `src/main/java/br/com/mapae/locais/aplicacao/ConsultaLocais.java:28` | `PAGINA_MAXIMA + 1` | Morto | `MapaAreaTests.paginaNoLimiteRespondeEAcimaDaRecusa:218`, `PesquisaLocaisTests.paginaNoLimiteRespondeEAcimaDaRecusa:237` - `InvalidDataAccessApiUsageException: Page offset exceeds Integer.MAX_VALUE` (o 500 de G1 volta) |
| P2 | `ConsultaLocais.java:28` | `PAGINA_MAXIMA - 1` | Morto | `MapaAreaTests...:215`, `PesquisaLocaisTests...:233` - 400 em vez de 200 |
| P3 | `src/main/java/br/com/mapae/locais/web/PesquisaLocaisController.java:41` | Tira `@Pattern(SEM_NUL)` do `termo` | Morto | `PesquisaLocaisTests.caractereNulDa400:246` - `DataIntegrityViolationException` do PostgreSQL (o 500 de G2 volta) |
| P4 | `PesquisaLocaisController.java:42` | Tira `@Pattern(SEM_NUL)` da `categoria` | Morto | `PesquisaLocaisTests.caractereNulDa400:248` |
| P5 | `src/main/java/br/com/mapae/locais/web/LocaisGeograficosController.java:46` | `/area` sem `@Max(PAGINA_MAXIMA)` | Morto | `MapaAreaTests.paginaNoLimiteRespondeEAcimaDaRecusa:218` |
| P6 | `PesquisaLocaisController.java:43` | `/pesquisa` sem `@Max(PAGINA_MAXIMA)` | Morto | `PesquisaLocaisTests.paginaNoLimiteRespondeEAcimaDaRecusa:237` |
| R1 | `src/main/java/br/com/mapae/compartilhado/seguranca/SegurancaConfig.java:86` | Leitor de token nunca resolve (regressão de M10) | Morto | 13 testes: `SegurancaTests.rotaDaContaComTokenExpiradoResponde401`, `ContaAutenticadaTests.*` (5), `TrocaSenhaTests.*` (7) |
| R2 | `SegurancaConfig.java:86` | Leitor sempre resolve (regressão de M11) | Morto | `MapaAreaTests.areaEPublicaComOuSemToken:276`, `PesquisaLocaisTests.pesquisaEPublicaComOuSemToken:353` - 401 em vez de 200 |
| R3 | `PesquisaLocais.java:38` | Não escapa `%` | Morto | `PesquisaLocaisTests.curingasSaoLiterais:271` - 8 itens, esperado 2 |

**Sensor depth**: P0 (autenticação e integridade do catálogo). 20 mutações manuais, cobrindo todos os ramos novos desta iteração e uma amostra de regressão.
**Result**: 20/20 mortos - PASS.

Depois do sensor: os arquivos de produção da cópia scratch têm o mesmo conteúdo dos reais (a única diferença é o fim de linha que o Python gravou em `PesquisaLocais.java` da cópia, LF para CRLF; `diff --strip-trailing-cr` vazio), então nenhum mutante ficou aplicado. Nenhum contêiner das corridas sobrou (o `postgis` parado há 5 horas e os de outros projetos não são destas corridas e não foram tocados).

---

## Sondas do Verificador (servidor real em porta aleatória)

Classe descartável `SondaVerificador2Tests`, só na cópia scratch (rodada antes da constante `CATALOGO_ATIVO`; a constante não muda o SQL gerado, e o gate e o sensor rodaram depois dela): Tomcat real, `java.net.http`, PostGIS em Docker. Cenário: "Café Central" (sem conta), um local inativo e três lojas pelo cadastro real com posição e `termos = {segredo-interno}`: uma `ATIVO`, uma `BLOQUEADO`, uma `INATIVO`.

### Paginação extrema (G1)

| Sonda | Entrada | Obtido |
| ----- | ------- | ------ |
| G1a | `/pesquisa?pagina=21474836&tamanho=100` | 200, `itens` vazio, `totalItens` 2, `totalPaginas` 1 |
| G1b | `/pesquisa?pagina=21474837&tamanho=100` | 400 `application/problem+json`, `detail: "Validation failure"` |
| G1c | `/pesquisa?pagina=2147483647&tamanho=100` | 400 Problem Details |
| G1d | `/pesquisa?pagina=21474837&tamanho=1` | 400 Problem Details (o teto é fixo, como manda a spec) |
| G1e | `/pesquisa?pagina=2147483648` | 400 Problem Details (`Failed to convert 'pagina'`) |
| G1f / G1g / G1h | `/area` com 21474836/100, 21474837/100, 2147483647/100 | 200 com totais; 400 PD; 400 PD |
| G1i | `/area?pagina=21474836` (tamanho 20) | 200 |
| G1j | `/area?limite=2147483647` | 200 (AD-028, inalterado) |

### Caractere NUL e controle (G2)

| Sonda | Entrada | Obtido |
| ----- | ------- | ------ |
| G2a / G2b | `termo=caf%00` / `categoria=bar%00` | 400 Problem Details |
| G2c / G2d | `termo=%00` / `termo=%00%00%00&categoria=x` | 400 Problem Details |
| G2e / G2f | `termo=caf%01` / `termo=caf%0A` | 200 (o PostgreSQL aceita; a quebra de linha final sai no `strip`) |
| G2g | `termo=%C0%80` (NUL em UTF-8 overlong) | 400 do Tomcat no formato padrão, antes da aplicação |
| G2h | `termo=caf%5Cu0000` (texto `\u0000`) | 200, 0 itens |

### Catálogo e status de loja (R4)

| Sonda | Rota | Obtido |
| ----- | ---- | ------ |
| R4a / R4b | `/area` sem e com paginação | Só "Café Central" e "Bar Ativo Loja"; `totalItens` 2 |
| R4c / R4d / R4e | `/proximos` sem ponto, por distância, com raio | Os mesmos 2 |
| R4f / R4h | `/pesquisa?termo=bar`, `/pesquisa` | Só a loja ativa; `totalItens` 2 (o `count` também filtra) |
| R4g | `termo=segredo-interno` | Só a loja ativa; o termo não aparece na resposta |
| R4i | `termo=secreto` (nome do inativo) | 0 |

### Injeção e curingas

`' OR 1=1 --`, `categoria=x' OR '1'='1`, `a' || (SELECT senha_hash FROM local LIMIT 1) || '`: 200 com 0 itens. `%`, `_`, `\` sozinhos: 0 itens (nenhum nome do cenário os contém). Sem brecha.

### Autenticação (P0)

14 variantes de caminho contra `/api/v1/contas/**`, cada uma sem token, com token expirado e com token malformado (42 requisições): `/contas/eu`, `/contas/eu/`, `/%63ontas/eu`, `/contas/EU`, `/contas`, `/contas/eu%20` e `?access_token=<válido>` deram **401 Problem Details**; `//contas/eu`, `;x=1`, `/locais/../contas/eu`, `/locais/%2e%2e/contas/eu` e `/./contas/eu` deram 400 do `StrictHttpFirewall`; `contas%2Feu` deu 400 do Tomcat; `/API/v1/...` deu 404. **Nenhuma chegou ao controller.** Nas rotas públicas (`/area`, `/pesquisa`, `/proximos`), token expirado, malformado, `Basic` e válido deram 200 (AD-041).

### CORS

`/pesquisa` com `Origin: http://localhost:5173` e `/area` com `http://localhost:3000`: 200 com `Access-Control-Allow-Origin` igual à origem. Origem `https://evil.example`: 403 `Invalid CORS request`. Pré-voo de `/pesquisa` com `authorization` em `Access-Control-Request-Headers`: 200 com o cabeçalho.

### `search_path`

Numa única conexão com `SET search_path TO public`, o `SELECT count(*) + PESQUISA` com `termo` rodou (`count=1`): o `unaccent` qualificado não depende do `search_path`, como diz a AD-043 corrigida. O `SELECT` com `MARCADOR` continua dependendo do `connection-init-sql` (PostGIS sem schema), agora documentado (R3 fechado).

---

## Revisão de segurança

| Tema | Resultado |
| ---- | --------- |
| Injeção de SQL | Sem brecha. `termo` e `categoria` são parâmetros ligados (`LocalRepository.java:46-47`); `ORDER BY` fixo (`:84`); `CATALOGO_ATIVO` (`:22`) é constante de compilação, sem entrada do usuário |
| Curingas do LIKE | Sem brecha. `PesquisaLocais.java:38` escapa `\`, depois `%` e `_`; `ESCAPE '\'` em `LocalRepository.java:53-56`. M7 e R3 mortos |
| Leitor de token (AD-041) | Sem brecha. `SegurancaConfig.java:85-86` usa o mesmo `PathPatternRequestMatcher` e a mesma constante `ROTAS_PROTEGIDAS` (`:46`) do `authorizeHttpRequests` (`:55`); falha fechada. R1 e R2 mortos; 42 sondas sem acesso |
| Vazamento em erro | Nenhum corpo trouxe SQL, stack, classe ou mensagem interna. Os 400 de validação dizem só `Validation failure`. Os 500 que P1 e P3 recriam mostram que sem as correções o erro voltaria, e os testes pegam |
| Colunas internas | Respostas só com `id`, `nome`, `categoria`, `latitude`, `longitude` (+ `distanciaMetros`); `termos`, `status`, `email`, `username`, `senha_hash` nunca aparecem |
| Loja bloqueada | Fora do mapa e da pesquisa (R4). O filtro bate com o `CHECK` de `local.status` (`V20261111103000__criar_contas.sql:48`) e com o `ck_local_conta_completa` (conta sempre tem status). A premissa ainda está "Confirmado? n" na spec: confirmar com a equipe |
| DoS | `tamanho` ≤ 100, `pagina` ≤ 21474836, `termo` ≤ 100, `categoria` ≤ 50; `@Pattern` `[^\x00]*` é linear e a query string é limitada pelo Tomcat. Página extrema não chega ao banco. Pesquisa sem índice (aceito na AD-040) |
| CORS | Configuração da AD-039 intacta; rotas novas cobertas |

## Conexão e ambiente

| Tema | Resultado |
| ---- | --------- |
| Migração no Supabase | `V20261112060000__catalogo_de_locais.sql` igual à da iteração 1: `CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions`, `ALTER TABLE public.local ADD COLUMN ...` com padrão constante (só metadado no PG 11+), registro em `public.migracao`. R4 e a constante não exigiram migração nova. Não aplicada no Supabase |
| `search_path` | Pesquisa textual independe do `search_path`; PostGIS depende do `connection-init-sql=SET search_path TO public, extensions` (inalterado), como `/area` sempre dependeu. Trocar para o Transaction Pooler quebraria todas as consultas de PostGIS, não só esta feature |
| Hikari / Session Pooler | `application.properties` principal sem mudança (`maximum-pool-size=5`) |
| AD-044 (R2) | `awssdk:s3` 2.29.51 no `pom.xml`; sem ela a `5c04b71` não compila (confirmado na iteração 1). Valores falsos só em `src/test/resources/config/application.properties`; `.env.example` só com marcadores |
| Subida local | **Ação do desenvolvedor (A1), pendente.** O `.env` local tem só `MAPAE_DB_URL`, `MAPAE_DB_USUARIO`, `MAPAE_DB_SENHA`, `MAPAE_JWT_SEGREDO` (conferidos só os nomes). `R2Config.java:18-27` e `R2Service.java:29` exigem as cinco `CLOUDFLARE_*` sem padrão: a aplicação não sobe até o `.env` ganhá-las |

---

## Code Quality

| Princípio | Status |
| --------- | ------ |
| Código mínimo | OK. As correções são uma constante de página, duas anotações por rota e uma constante única do catálogo |
| Mudanças cirúrgicas | OK |
| Sem escopo extra | Ressalva já registrada: AD-044 |
| Segue os padrões | OK. Constantes derivadas em vez de números mágicos; comentários em português |
| Asserções miram a spec | OK, 25/25 |
| Cobertura por camada | OK. Cada rota tem caminho feliz, bordas e erros; cada consulta do catálogo tem asserção própria de status (Q1 a Q5) |
| Todo teste mapeia um AC | OK (comentários `// MAPA-06`, `// BUSCA-07` etc.) |
| Diretrizes | `docs/decisões.md` (AD-001, AD-005, AD-017, AD-024, AD-028, AD-030, AD-037, AD-039 a AD-044), `docs/contrato-geografico.md`: seguidas |

---

## Gate Check

- **Comando**: `./mvnw test` na árvore real, rodado duas vezes nesta iteração: antes e depois da constante `CATALOGO_ATIVO` (a segunda com `MAVEN_OPTS=-Xmx384m -DargLine="-Xmx512m -XX:+UseSerialGC"` por causa da memória; não muda o comportamento dos testes).
- **Resultado**: 219 testes, 0 falhas, 0 erros, 0 ignorados (`EXIT=0`) nas duas.
- **Antes da feature**: 159. **Iteração 1**: 213. **Agora**: 219 (+6: `MapaAreaTests` 21→23, `PesquisaLocaisTests` 29→33).
- **Removidos**: nenhum. Asserções enfraquecidas: nenhuma (`curingasSaoLiterais` ficou mais forte).

---

## Fix Plans

Nenhum gap da feature. Restam:

### A1 (desenvolvedor, fora da feature)

- Acrescentar ao `.env` as cinco `CLOUDFLARE_*` do `.env.example` (valores falsos bastam sem conta R2). Sem isso a aplicação não sobe localmente.

### C1 (equipe)

- Confirmar a premissa R4 ("loja com conta bloqueada ou inativa fica fora do catálogo"), hoje marcada "Confirmado? n" na spec.

---

## Requirement Traceability Update

| Requisito | Status anterior | Novo status |
| --------- | --------------- | ----------- |
| CAT-01 a CAT-03 | Implementing | Verified |
| MAPA-01 a MAPA-10 | Implementing (MAPA-03, MAPA-09 com G1) | Verified |
| BUSCA-01 a BUSCA-12 | Implementing (BUSCA-11 com G1; BUSCA-06, BUSCA-08 com teste fraco) | Verified |

O Verificador não alterou `spec.md`; a atualização da tabela fica para o orquestrador.

---

## Isolamento e lições

- `git status --porcelain` da árvore real antes e depois de gate, sondas e sensor: idêntico em todas as etapas. O gate criou só `target/`, ignorado pelo git.
- Nenhum `git stash`, commit ou acesso ao Supabase; do `.env` só os nomes das variáveis.
- A primeira corrida do sensor (encerrada por memória) também não deixou nada: a cópia `verifier-locais2` foi conferida igual à real e depois apagada.
- Lições não gravadas. Propostas:
  1. Em máquina com pouca memória, rodar o sensor em lotes pequenos, com JVMs limitadas (`MAVEN_OPTS`, `-DargLine`), e gravar o resultado de cada mutante assim que termina.
  2. Uma regra de filtro usada em várias consultas nativas deve ser uma constante única, e cada consulta precisa de uma asserção própria. Só assim um mutante por consulta morre na rota certa (Q1 a Q5).
  3. Quando um limite existe para evitar um erro de infraestrutura (teto de página, NUL), o teste do limite exato e do limite + 1 recria o erro sob mutação (P1, P3). É a prova de que o teste protege a correção.
  4. As lições 1 a 5 da iteração 1 continuam válidas (teto de `PageRequest`, NUL no PostgreSQL, dado discriminante para escape do LIKE, collation em teste de ordenação, `search_path`).

---

## Summary

**Overall**: Ready

**Spec-anchored check**: 25/25 ACs com evidência e asserção no resultado da spec; 0 lacunas de precisão
**Sensor**: 20/20 mutações mortas
**Gate**: 219 testes, 0 falhas

**O que funciona**: catálogo com `categoria`, `termos` e `ativo`; regra única do catálogo ativo (inclui status de loja) nas cinco consultas; `/area` com paginação opcional e teto de página; `/proximos` só com o catálogo ativo; `/pesquisa` por nome, categoria e termos, sem maiúsculas e acentos, curingas e barra literais, NUL recusado, paginada e ordenada por nome sem acento; token ignorado nas rotas públicas sem abrir `/contas`; o build volta a compilar.

**Problemas**: nenhum na feature. Ação A1 (variáveis do R2 no `.env`) e confirmação C1 (premissa R4).

**Próximo passo**: o orquestrador marca os 25 requisitos como Verified na spec. O desenvolvedor acrescenta as `CLOUDFLARE_*` ao `.env` antes de subir a aplicação.
