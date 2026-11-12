# Contas e login - Validação (iteração 3 de 3)

## Validation: conta-e-login - PASS

**Data**: 2026-11-12
**Spec**: `.specs/features/conta-e-login/spec.md`
**Diff range**: working tree sobre HEAD `983d6ca` (branch `login/banco`; nada commitado, AD-001). Superfície: `compartilhado/seguranca/*` (inclui `SenhaValida.java` e o novo `CorsPropriedades.java`), `compartilhado/web/TratadorDeErros.java`, `conta/**`, `locais/aplicacao/ContasDeLoja.java`, `locais/web/LojasController.java`, `locais/dominio/Local.java`, `locais/dominio/LocalRepository.java`, `V20261111103000__criar_contas.sql`, `pom.xml`, `application.properties`, `.env.example`, `OpenApiConfig.java`, e os testes, inclusive o novo `CorsTests.java`.
**Verificador**: sub-agente independente (autor diferente do verificador), re-verificação final depois do Fix A da iteração 2 e da entrada do CORS (AD-039). Tudo foi derivado de novo; o relatório anterior não foi usado como evidência. Não alterou código nem testes; mutações e sondas só em cópia scratch.

**Veredito: PASS.** Os 28 ACs têm evidência `file:line` e discriminam. A suíte está verde (159/0/0). O sobrevivente da iteração 2 (m24) agora morre, pelos dois testes novos. Nenhum mutante de autenticação refeito voltou a sobreviver. O CORS, que está fora do escopo da spec, não enfraqueceu nenhuma regra de segurança: o pré-voo só dispensa token em `OPTIONS` com `Origin` e `Access-Control-Request-Method`; qualquer outra requisição a `/api/v1/contas/**` sem token continua dando 401. Dois mutantes de CORS sobreviveram (c5 e c9). Os dois ficam fora da spec e têm risco baixo, então entram como recomendações e não bloqueiam (ver "CORS").

---

## Task Completion

| Task | Status | Notas |
| ---- | ------ | ----- |
| T1 Migração e mapeamento | Feito | `ContasSchemaTests` 11/11 |
| T2 Migração no Supabase | Não verificado | Fora do alcance do Verificador (banco compartilhado proibido). Informativo, sem mudança desde a iteração 1. |
| T3 Segurança e JWT | Feito | `SegurancaTests` 10/10 |
| T4 Cadastro de usuário | Feito | `CadastroUsuarioTests` 35/35 |
| T5 Cadastro de loja | Feito | `CadastroLojaTests` 22/22 |
| T6 Login | Feito | `LoginTests` 24/24 (+1, Fix A); m24 morto |
| T7 Conta autenticada | Feito | `ContaAutenticadaTests` 7/7 |
| T8 Troca de senha | Feito | `TrocaSenhaTests` 8/8 (+1, Fix A); m24 morto |
| Extra: CORS (AD-039, fora da spec) | Feito | `CorsTests` 4/4; 7 de 9 mutantes mortos |

---

## Spec-Anchored Acceptance Criteria

Legenda: OK = a asserção mira o resultado da spec e um mutante na regra morre; GAP = falta evidência ou o teste não discrimina; PRECISÃO = a spec não define o resultado com exatidão.

| AC | Resultado definido na spec | `file:line` + asserção | Resultado |
| -- | -------------------------- | ---------------------- | --------- |
| CONTA-01 | `usuario` com as 14 colunas | `src/test/java/br/com/mapae/conta/ContasSchemaTests.java:67` - `assertThat(colunas("usuario")).containsExactlyInAnyOrderEntriesOf(...)` | OK |
| CONTA-02 | `local` ganha 10 colunas e `posicao` opcional | `ContasSchemaTests.java:87` - `containsExactlyInAnyOrderEntriesOf(... "posicao" ...)` | OK |
| CONTA-03 | Linhas antigas preservadas, sem conta | `ContasSchemaTests.java:225` - `assertThat(linhas).hasSize(1)` e as entradas `posicao`, `username` nulo, `senha_hash` nulo | OK |
| CONTA-04 | Username e email únicos sem diferenciar maiúsculas, por tabela | `ContasSchemaTests.java:147`, `:155`, `:171`, `:179` - `hasMessageContaining("uq_usuario_username")` / `uq_usuario_email` / `uq_local_username` / `uq_local_email` | OK |
| CONTA-05 | RLS ligado e sem políticas em `usuario` | `ContasSchemaTests.java:110-111` - `assertThat(rls).isTrue(); assertThat(politicas).isZero();` | OK |
| CONTA-06 | Sobe com `ddl-auto=validate` | `src/main/resources/application.properties:19` (`validate`) + `ContasSchemaTests.java:116`, `:127` | OK |
| CONTA-07 | 201 com id, username, nome, email, `primeiroAcesso: true`, `status: "ATIVO"`, `tipoPerfil: "USUARIO"`, `criadoEm` | `src/test/java/br/com/mapae/conta/CadastroUsuarioTests.java:81-90` - `isCreated()`, `jsonPath("$.tipoPerfil").value("USUARIO")`, `Instant.parse(criadoEm)).isBetween(...)`; limites `:180`, `:262-264` | OK |
| CONTA-08 | Senha só como BCrypt, nunca em resposta alguma | `CadastroUsuarioTests.java:98-102` - `not(hasKey("senhaHash"))`, hash `startsWith("$2")`; `src/test/java/br/com/mapae/compartilhado/LoginTests.java:216`, `:227` - `campos.keySet()).containsExactlyInAnyOrder("token", ...)` | OK (m08 morto de novo) |
| CONTA-09 | Ignora `tipoPerfil`, `status`, `primeiroAcesso`, `criadoEm` do corpo | `CadastroUsuarioTests.java:113-123` - `jsonPath("$.tipoPerfil").value("USUARIO")` | OK |
| CONTA-10 | 400 Problem Details listando o campo | `CadastroUsuarioTests.java:132-135`, `:154-156`, `:164-166`, `:252-254`, `:272-275` - `isBadRequest()`, `jsonPath("$.campos.<campo>").exists()` | OK (m21 morto de novo) |
| CONTA-11 | 409 indicando qual campo | `CadastroUsuarioTests.java:190-193`, `:203-205` - `isConflict()`, `jsonPath("$.campo").value("username")` / `"email"` | OK |
| CONTA-12 | 201 com os campos da loja e `tipoPerfil: "LOJA"` | `src/test/java/br/com/mapae/locais/CadastroLojaTests.java:76-91` - `isCreated()`, `jsonPath("$.tipoPerfil").value("LOJA")` | OK |
| CONTA-13 | Loja sem posição, fora de `/area` e `/proximos` | `CadastroLojaTests.java:124-132` - `semPosicao isTrue()`, `jsonPath("$.itens.length()").value(0)` | OK |
| CONTA-14 | 400 Problem Details listando o campo | `CadastroLojaTests.java:141-144`, `:160-162`, `:195-197` | OK |
| CONTA-15 | 409 indicando qual campo | `CadastroLojaTests.java:172-175`, `:185-187` - `jsonPath("$.campo").value("username")` / `"email"` | OK |
| CONTA-16 | 200 com token, `tipoToken: "Bearer"`, `expiraEm`, `primeiroAcesso`, `status`, `tipoPerfil: "USUARIO"`; login sem diferenciar maiúsculas | `LoginTests.java:101-108` - `isOk()`, `jsonPath("$.tipoToken").value("Bearer")`, `jsonPath("$.tipoPerfil").value("USUARIO")` | OK |
| CONTA-17 | Idem com `tipoPerfil: "LOJA"` | `LoginTests.java:116-123` | OK |
| CONTA-18 | 401 com a mesma mensagem para login inexistente e senha errada (premissa: senha acima de 72 bytes no login é senha errada) | `LoginTests.java:130-137`, `:144-149` - `isUnauthorized()`, `jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM)`; **`:245-255`** (novo) - cadastro com `"a".repeat(72)`, login com `senha72 + "b"`: `isUnauthorized()`, `$.detail` = `MENSAGEM`, `not(hasKey("token"))`; controle com a senha exata `isOk()` | OK. **Fix A resolvido:** m24 morto por este teste |
| CONTA-19 | 403 informando o status, sem token | `LoginTests.java:158-162`, `:171-174` - `isForbidden()`, `jsonPath("$.statusConta").value(situacao)`, `not(hasKey("token"))`; ordem em `:182` | OK |
| CONTA-20 | HS256, `sub` = id, perfil em claim, validade configurada | `LoginTests.java:191-197` - `containsEntry("alg", "HS256")`, `getSubject()`, `getClaimAsString("perfil")`; `:265` - `Duration.between(...)).isEqualTo(Duration.ofHours(2))` | OK (m13 morto de novo) |
| CONTA-21 | Rota trocada dá 401 como credencial errada | `LoginTests.java:270-274`, `:279-283` - `isUnauthorized()`, `jsonPath("$.detail").value(CredenciaisInvalidasException.MENSAGEM)` | OK |
| CONTA-22 | 200 com dados da conta, sem senha nem hash | `src/test/java/br/com/mapae/conta/ContaAutenticadaTests.java:97-103`, `:117-124` - `jsonPath("$.primeiroAcesso").value(true)`, `not(hasKey("senhaHash"))` | OK |
| CONTA-23 | Grava `primeiro_acesso = false`, atualiza `atualizado_em`, 204 | `ContaAutenticadaTests.java:133-136`, `:149-152` - `isNoContent()`, `atualizadoEm(...)).isAfter(...)` | OK |
| CONTA-24 | Segunda chamada 204 sem mudar `atualizado_em` | `ContaAutenticadaTests.java:163-164` - `isNoContent()`, `atualizadoEm(...)).isEqualTo(ANTIGO)` | OK |
| CONTA-25 | Token ausente, inválido ou expirado: 401 Problem Details | `src/test/java/br/com/mapae/compartilhado/SegurancaTests.java:68-72`, `:78-80`, `:93-95`, `:105-107` - `isUnauthorized()`, `APPLICATION_PROBLEM_JSON`, `$.detail`; `TrocaSenhaTests.java:162-165` | OK (m20 morto de novo). Sondas P13 a P16: com CORS ligado, o 401 continua igual |
| CONTA-26 | Cadastro, login, Swagger e `/api/v1/locais` públicos | `SegurancaTests.java:113-116`, `:122-123` - `isOk()`; `:132-133` - `status().is(not(401))` | OK |
| CONTA-27 | Novo hash BCrypt, `atualizado_em` atualizado, 204 | `src/test/java/br/com/mapae/conta/TrocaSenhaTests.java:91-99` - `isNoContent()`, `senhas.matches(NOVA, novoHash)).isTrue()`, `atualizadoEm isAfter(...)`; `:106-109` loja; `:134-135` | OK |
| CONTA-28 | 400 sem alterar o hash | `TrocaSenhaTests.java:117-118`, `:143-144`; **`:150-157`** (novo) - `senhaAtual` de 72 bytes + "b": `isBadRequest()`, `hash("usuario")).isEqualTo(antes)`, depois login com a senha exata `isOk()` | OK. **Fix A resolvido:** m24 morto por este teste; m06 morto de novo |

**Status**: 28/28 ACs com evidência `file:line`, todos OK, 0 GAPs, 0 lacunas de precisão.

---

## Edge Cases

- [x] Mesmo email em `usuario` e `local` é aceito: `CadastroLojaTests.java:225-227` - `status().isCreated()`.
- [x] Dois cadastros simultâneos com o mesmo username dão 409 pela restrição do banco: `CadastroUsuarioTests.java:284-286`, `:294-296`; `CadastroLojaTests.java:206-208`, `:217-219`.
- [x] JSON malformado dá 400 Problem Details: `CadastroUsuarioTests.java:302-304`, `LoginTests.java:288-290`.
- [x] Email e username com maiúsculas são guardados em minúsculas: `CadastroUsuarioTests.java:238-244`, `CadastroLojaTests.java:236-239`.
- [x] Senha cadastrada de 72 bytes seguida de qualquer sufixo não entra (premissa da CONTA-18): `LoginTests.java:245-255`, `TrocaSenhaTests.java:150-157`.

---

## Discrimination Sensor

Execução: cópia do projeto (sem `target/`, `.git/`, `.env`, `.idea/`) em `scratchpad/verifier3/base`. Cada mutante rodou numa cópia própria, aplicado por troca exata de texto (o script aborta se o trecho não aparece uma única vez), com `./mvnw -q test -Dtest=LoginTests,SegurancaTests,CadastroUsuarioTests,ContaAutenticadaTests,TrocaSenhaTests,CadastroLojaTests,ContasSchemaTests,CorsTests` (121 testes), um mutante por vez. O controle sem mutação (m00) passou.

### Autenticação (P0): sobrevivente da iteração 2 e amostra de regressão

| # | `file:line` mutado | Mutação | Morto? | Teste que matou |
| - | ------------------ | ------- | ------ | --------------- |
| m24 | `src/main/java/br/com/mapae/compartilhado/seguranca/Autenticador.java:55` | `confere` sem a guarda `cabeNoBcrypt` (iteração 2: **sobreviveu**) | **Morto** | `LoginTests.senhaCadastradaDe72BytesComSufixoNaoEntra` (`LoginTests.java:252`), `TrocaSenhaTests.senhaAtualDe72BytesComSufixoResponde400SemMudarOHash` (`TrocaSenhaTests.java:155`) |
| m08 | `Autenticador.java:41-42`, `:62-63` | `senhaHash` na resposta do login | Morto | `LoginTests.respostaDoLoginDoUsuarioTemSoOsCamposDaSpec`, `respostaDoLoginDaLojaTemSoOsCamposDaSpec` (`:216`, `:227`) |
| m13 | `compartilhado/seguranca/EmissorDeToken.java:28` | Validade fixa de 24h | Morto | `LoginTests.validadeDoTokenSegueAConfiguracao` (`:265`) |
| m20 | `compartilhado/seguranca/SegurancaConfig.java:86` | Decoder aceita token expirado | Morto | `SegurancaTests.rotaDaContaComTokenExpiradoResponde401` (`:93`) |
| m21 | `compartilhado/seguranca/SenhaValida.java:35` | Limite `<= 72` vira `<= 73` bytes | Morto | `CadastroUsuarioTests.campoForaDoFormatoResponde400ListandoOCampo`; também os dois testes novos do Fix A |
| m06 | `Autenticador.java:47` | Troca de senha não confere a atual | Morto | `TrocaSenhaTests.senhaAtualErradaResponde400SemMudarOHash`, `senhaAtualAcimaDe72BytesResponde400SemMudarOHash`, `senhaAtualDe72BytesComSufixoResponde400SemMudarOHash` |

Os demais mutantes mortos na iteração 2 (m03, m04, m11, m14u, m14l, m22, m23, m25 a m29) ficam em código que não mudou desde então; a amostra acima cobre todos os trechos de `Autenticador`, `SegurancaConfig` e `SenhaValida` alterados ou vizinhos ao diff novo.

### CORS (AD-039, fora da spec)

| # | `file:line` mutado | Mutação | Morto? | Teste que matou |
| - | ------------------ | ------- | ------ | --------------- |
| c1 | `SegurancaConfig.java:44` | Remove `.cors(Customizer.withDefaults())` | Morto | Os 4 de `CorsTests` |
| c2 | `SegurancaConfig.java:64` | Tira `Authorization` dos cabeçalhos liberados | Morto | `CorsTests.preVooDeRotaProtegidaPassaSemToken` (`CorsTests.java:47`) |
| c3 | `SegurancaConfig.java:62` | Origens = `*` | Morto | Os 4 de `CorsTests` (o `Access-Control-Allow-Origin` passa a ser `*`; origem estranha dá 200) |
| c4 | `SegurancaConfig.java:62` | `setAllowedOriginPatterns("*")` (reflete qualquer origem) | Morto | `CorsTests.origemNaoLiberadaERecusada` (`CorsTests.java:65`) |
| c5 | `SegurancaConfig.java:62` | Acrescenta uma origem fixa fora da lista (`https://outro-site.example`) | **Sobreviveu** | Nenhum. Limite de amostragem: o teste só pode recusar origens que conhece. Ver recomendação R2 |
| c6 | `SegurancaConfig.java:67` | Remove o `registerCorsConfiguration` | Morto | Os 4 de `CorsTests` |
| c7 | `SegurancaConfig.java:67` | Registra só em `/api/v1/locais/**` | Morto | Os 4 de `CorsTests` |
| c8 | `SegurancaConfig.java:63` | Tira `POST` dos métodos | Morto | `CorsTests.preVooDoLoginDeOrigemLiberadaPassa` (`CorsTests.java:34`) |
| c9 | `SegurancaConfig.java:65` | Liga `setAllowCredentials(true)` | **Sobreviveu** | Nenhum. A AD-039 diz que `allowCredentials` fica desligado, mas nenhum teste confere. Ver recomendação R1 |

**Sensor depth**: P0 (autenticação). 6 mutações de autenticação refeitas mais 9 mutações de CORS.
**Result**: autenticação 6/6 mortos; CORS 7/9 mortos (c5 e c9 sobreviveram, fora da spec e de risco baixo, sem bloquear); total 13/15. **PASS** para a spec.

### Sondas do Verificador sobre o CORS (cópia scratch, classe de teste descartada)

| Sonda | Entrada | Esperado | Obtido |
| ----- | ------- | -------- | ------ |
| P13 | `OPTIONS /api/v1/contas/eu` com `Origin` liberada e sem `Access-Control-Request-Method` (não é pré-voo) | 401 | 401 Problem Details, com `Access-Control-Allow-Origin` |
| P14 | `OPTIONS /api/v1/contas/eu` com `Access-Control-Request-Method` e sem `Origin` | 401 | 401 |
| P15 | `PUT /api/v1/contas/eu/senha` com `Origin` liberada, sem token | 401 | 401 |
| P16 | `GET /api/v1/contas/eu` com `Origin` liberada e token inválido | 401 | 401 |
| P17 | Pré-voo de `PUT /api/v1/contas/eu/senha` com `authorization,content-type` | 200 | 200, `Allow-Headers: authorization, content-type` |
| P18 | Pré-voo de origem fora da lista em rota protegida | 403 | 403 |
| P19 | `GET /api/v1/contas/eu` com token válido e `Origin` liberada | 200 | 200, sem `senhaHash` |
| P20 | O mesmo com `Origin` fora da lista | Recusado | 403 "Invalid CORS request" (mais restrito, mas não quebra nada: clientes sem `Origin` não passam pela regra) |
| P22 | Pré-voo pedindo cabeçalho não liberado (`x-qualquer`) | 403 | 403 |
| P23 | `GET /v3/api-docs` com `Origin` (fora de `/api/**`) | Sem CORS | 200 sem `Access-Control-Allow-Origin` (o Swagger usa a mesma origem) |
| P24 | `POST` de login vindo de origem fora da lista | Recusado | 403 |

**Conclusão de segurança**: o CORS não abre nenhuma brecha. Só um pré-voo de verdade (`OPTIONS` com `Origin` e `Access-Control-Request-Method`) passa sem token, e ele não executa o controller. P13 e P14 mostram que um `OPTIONS` sem esses cabeçalhos continua passando pela autenticação. `allowCredentials` está desligado (`ACAC=null` em todas as sondas), e não se usam cookies (API stateless, AD-004).

---

## Code Quality

| Princípio | Status |
| --------- | ------ |
| Código mínimo | OK. Fix A só acrescentou dois testes. O CORS cabe em um bean e um record de propriedades |
| Mudanças cirúrgicas | OK |
| Sem escopo extra | Ressalva: o CORS não está na spec (nem no Out of Scope). Foi registrado como AD-039 (status "Proposta") e é pequeno e testado. Falta só confirmação explícita, sem bloquear (R3) |
| Segue os padrões | OK. `CorsPropriedades` segue o padrão de `JwtPropriedades` (`@ConfigurationProperties` + `@Validated`, `@NotEmpty` derruba a subida com lista vazia) |
| Asserções miram o resultado da spec | OK, 28/28 |
| Cobertura por camada | OK |
| Todo teste mapeia um AC, borda ou premissa | OK. Os 4 de `CorsTests` mapeiam a AD-039, não a spec |
| Diretrizes do projeto | `docs/decisões.md` (AD-001, AD-004, AD-005, AD-010, AD-024, AD-033 a AD-039): seguidas |

Observações menores (não bloqueiam):
- `SegurancaConfig.java:63` libera `PATCH` e `DELETE`, que nenhuma rota usa hoje. É uma pequena folga prevista na AD-039.
- A recusa de CORS (403, P18/P20/P22/P24) devolve texto puro "Invalid CORS request", fora do padrão Problem Details do resto da API. A spec não cobre esse caso.
- Continua de pé a observação da iteração 2: o `TratadorDeErros` mistura português ("formato inválido") com as mensagens em inglês do Bean Validation.

---

## Gate Check

- **Comando**: `./mvnw test` na árvore real, depois `./mvnw verify` (gate de build do T8). O Docker já estava de pé (`docker info`: 29.7.2).
- **Resultado**: 159 testes, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS` nos dois comandos.
- **Testes antes da feature**: 38 (as 5 classes que existiam em `983d6ca`)
- **Testes depois**: 159 (`LoginTests` 24, `SegurancaTests` 10, `CadastroUsuarioTests` 35, `ContaAutenticadaTests` 7, `ContasSchemaTests` 11, `TrocaSenhaTests` 8, `CadastroLojaTests` 22, `CorsTests` 4, mais as classes antigas: `ConsultasGeograficasTests` 28, `MapaeApplicationTests` 5, `DocumentacaoApiTests` 2, `NomesDeMigracaoTests` 2, `ModularidadeTests` 1)
- **Delta**: +121 (115 da iteração 2, +2 do Fix A, +4 do CORS). Nenhum teste removido, nenhuma asserção enfraquecida.
- **Ignorados**: nenhum
- **Falhas**: nenhuma

---

## Situação dos fixes

| Fix | Situação | Evidência |
| --- | -------- | --------- |
| 1 a 7 (iteração 1) | Resolvidos (iteração 2), sem regressão | m08, m13, m20, m21, m06 mortos de novo |
| A (iteração 2): guarda de 72 bytes sem teste que discrimine | **Resolvido** | `LoginTests.java:245-255`, `TrocaSenhaTests.java:150-157`; m24 morto pelos dois |

---

## Fix Plans

Nenhum fix obrigatório para a spec. Recomendações sobre o CORS, fora da spec e sem bloquear:

### R1 (Minor): prender `allowCredentials` desligado

- **Causa**: c9 sobreviveu. A AD-039 decide "sem credenciais de navegador", mas nenhum teste confere.
- **Tarefa**: em `CorsTests.preVooDoLoginDeOrigemLiberadaPassa`, acrescentar `.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))`.
- **Pronto quando**: c9 morre.

### R2 (Cosmetic): origem extra fixa no código

- **Causa**: c5 sobreviveu. É um limite próprio de testar listas de permissão: um teste só recusa as origens que conhece. O risco real é baixo, porque as origens vêm da configuração (`application.properties:36`).
- **Opcional**: num teste com `@TestPropertySource(properties = "mapae.cors.origens=http://a.test")`, conferir que `http://localhost:5173` deixa de ser aceita, provando que a lista vem só da propriedade.

### R3: confirmar o escopo do CORS

- A AD-039 está como "Proposta" e o CORS não aparece na spec. Cabe ao usuário confirmar a decisão ou levá-la para a spec ou uma task própria.

---

## Requirement Traceability Update

| Requisito | Status anterior | Novo status |
| --------- | --------------- | ----------- |
| CONTA-01 a CONTA-17 | Implementing | Verified |
| CONTA-18 | Implementing (iteração 2: teste a reforçar) | Verified |
| CONTA-19 a CONTA-27 | Implementing | Verified |
| CONTA-28 | Implementing (iteração 2: teste a reforçar) | Verified |
| AD-039 CORS (fora da spec) | - | Implementado e testado; R1 recomendado |

O Verificador não alterou `spec.md`; a atualização da tabela fica para o orquestrador.

---

## Isolamento e lições

- `git status --porcelain` da árvore real antes e depois do gate, do sensor e das sondas: idênticos. O gate criou só `target/`, ignorado pelo git.
- Nenhum `git stash`, nenhum commit, nenhum acesso ao Supabase nem ao `.env`. Os mutantes rodaram um por vez em cópias scratch, apagadas depois.
- Lições não gravadas (o Verificador só pode escrever este arquivo). Propostas para o orquestrador:
  1. Uma guarda de tamanho antes do BCrypt se testa com a senha cadastrada mais um sufixo, não com uma senha longa qualquer: o BCrypt ignora o que passa de 72 bytes (confirmado: m24 só morreu com esse teste).
  2. Quando uma decisão de segurança diz que algo fica "desligado" (como `allowCredentials`), o teste precisa conferir a ausência do cabeçalho; testar só o caminho feliz deixa a decisão sem proteção (c9).
  3. Mudança fora da spec que entra no diff da feature (como o CORS) deve vir com teste e mutação próprios, e ganhar uma linha na spec ou no Out of Scope.

---

## Summary

**Overall**: Ready

**Spec-anchored check**: 28/28 ACs conferem com a spec e discriminam; 0 lacunas de precisão
**Sensor**: 13/15 mutações mortas (autenticação 6/6, com m24 agora morto; CORS 7/9, sobreviventes c5 e c9 fora da spec e não bloqueantes)
**Gate**: 159 testes, 0 falhas (`./mvnw test` e `./mvnw verify`)

**O que funciona**: tudo o que a spec pede. A guarda de 72 bytes no login e na troca de senha agora tem teste. O CORS libera só as origens configuradas, o pré-voo das rotas protegidas e o cabeçalho CORS no 401, sem dispensar token fora do pré-voo de verdade.

**Problemas**: nenhum que bloqueie. Recomendações R1 (testar `allowCredentials` desligado), R2 (opcional) e R3 (confirmar o escopo da AD-039).

**Próximo passo**: o orquestrador atualiza a rastreabilidade na `spec.md` e decide sobre R1 e R3 com o usuário.
