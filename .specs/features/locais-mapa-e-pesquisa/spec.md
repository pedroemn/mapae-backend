# Locais no mapa e pesquisa - Especificação

Cards: **BACK-022** (Consultar locais por área visível do mapa) e **BACK-023** (Implementar pesquisa de locais).

## Problem Statement

O mapa já devolve os locais de uma caixa (`/api/v1/locais/area`), mas não tem paginação, não diz a categoria do marcador e não distingue local ativo de inativo. O catálogo também não tem categoria nem palavras-chave, então o app não consegue pesquisar locais do Mapaê pelo nome, pela categoria ou por um termo. As duas tasks entregam o catálogo pesquisável e a consulta por área sobre o mesmo contrato de marcador e de paginação.

## Goals

- [ ] O app mostra no mapa só os locais ativos da área visível, com os campos do marcador e paginação opcional.
- [ ] O app pesquisa locais ativos por nome, categoria ou termo, com filtro de categoria e paginação, e usa o mesmo item no mapa e na lista.

## Out of Scope

| Item | Motivo |
| ---- | ------ |
| Telas, navegação, componentes visuais, permissões do aparelho | Fora do escopo dos dois cards |
| Histórico de pesquisa e sugestões inteligentes | Fora do escopo do BACK-023 |
| Busca de usuários | Fora do escopo do BACK-023 |
| Busca livre de endereços e lugares fora do catálogo | Fica com o provedor externo de mapas, chamado pelo app (AD-018, AD-029) |
| Cadastro e edição de categoria, termos e `ativo` | Não há cadastro administrativo de locais ainda (AD-029); os testes inserem locais fictícios |
| Gerômetro, rastreamento contínuo de pessoas | Fora do escopo do BACK-022 |
| Índice para a pesquisa textual | A base de `local` é pequena (AD-029); entra quando a base crescer |

---

## Assumptions & Open Questions

| Assunto | Decisão | Motivo | Confirmado? |
| ------- | ------- | ------- | ----------- |
| Origem de categoria e termos | Colunas novas em `local`: `categoria VARCHAR(50)` (nula) e `termos TEXT[]` (padrão vazio) | Escolhido pelo usuário em 2026-11-12 | s |
| O que é "catálogo ativo" | Coluna nova `ativo BOOLEAN NOT NULL DEFAULT true`; só entra no mapa e na pesquisa local com `ativo = true` e com posição | Escolhido pelo usuário em 2026-11-12; sem posição o local não tem marcador (AD-033) | s |
| Loja com conta bloqueada ou inativa | Também fica fora do catálogo ativo: conta de loja só aparece com `status = 'ATIVO'`; local sem conta (status nulo) segue a coluna `ativo` | Apontado pelo Verificador (R4): uma loja bloqueada com posição apareceria no mapa | n |
| Página máxima | `pagina` até 21474836; acima, 400 | Com `tamanho` 100, o deslocamento ainda cabe num int; acima disso o Spring Data recusa e a resposta seria 500 (Verificador, G1) | n |
| Caractere NUL | `termo` ou `categoria` com NUL dão 400 | O PostgreSQL não aceita NUL em texto e responderia 500 (Verificador, G2) | n |
| Paginação | `pagina` (começa em 0) e `tamanho` (padrão 20, máximo 100); a resposta traz `pagina` com `numero`, `tamanho`, `totalItens` e `totalPaginas` | Escolhido pelo usuário em 2026-11-12 | s |
| Paginação na `/area` | Opcional. Sem `pagina`, `tamanho` e `limite`, devolve todos, como hoje (AD-028). `limite` continua valendo, mas não combina com `pagina` ou `tamanho` | Mantém o contrato confirmado na AD-028 e o app atual funcionando | n |
| Regra de "ativo" na `/proximos` | Também só ativos | O catálogo do mapa é um só; um local inativo não deve aparecer em nenhuma consulta | n |
| Comparação de texto | Sem diferenciar maiúsculas nem acentos (`unaccent` no schema `extensions`); `termo` casa como trecho de nome, categoria ou de qualquer termo; `categoria` casa por igualdade | Busca natural em português ("cafe" acha "Café") | n |
| Termo vazio ou só com espaços | Igual a não informar o termo: lista todos os ativos, ainda filtrados pela categoria | Card pede testar termo vazio; responder 400 obrigaria o app a tratar um caso comum | n |
| Caracteres `%` e `_` no termo | São literais, não curingas | Evita que o usuário liste tudo com `%` e mantém a busca previsível | n |
| Tamanho máximo | `termo` até 100 caracteres, `categoria` até 50; acima disso 400 | Limita custo da consulta; 50 é o tamanho da coluna | n |
| Ordem da pesquisa | Por nome sem acento e sem maiúsculas, depois por `id` | Ordem estável para a paginação; relevância fica para o futuro | n |
| Token em rota pública | Rotas públicas ignoram o cabeçalho `Authorization`; só `/api/v1/contas/**` lê o token | Um usuário com token expirado não pode perder o mapa por um 401 numa rota pública. Se uma rota protegida for esquecida no leitor, ela falha fechada (401) | n |

**Open questions:** none - all resolved or logged above (o que não foi confirmado está registrado com o padrão escolhido).

---

## User Stories

### P1: Catálogo pesquisável ⭐ MVP

**User Story**: Como equipe do Mapaê, quero que cada local tenha categoria, termos e situação ativa, para que o mapa e a pesquisa usem o mesmo catálogo.

**Acceptance Criteria**:

1. WHEN a migração do catálogo roda THEN o sistema SHALL acrescentar a `local` as colunas `categoria VARCHAR(50)` nula, `termos TEXT[] NOT NULL DEFAULT '{}'` e `ativo BOOLEAN NOT NULL DEFAULT true`, e instalar a extensão `unaccent` no schema `extensions`. (CAT-01)
2. WHEN a migração roda sobre locais existentes THEN o sistema SHALL preservá-los com `ativo = true`, `termos` vazio e `categoria` nula. (CAT-02)
3. WHEN uma loja se cadastra THEN o sistema SHALL gravá-la com `ativo = true` e `termos` vazio, sem exigir categoria. (CAT-03)

**Independent Test**: rodar as migrações num banco com um local antigo e conferir colunas, padrões e a linha preservada.

---

### P1: Locais na área visível do mapa (BACK-022) ⭐ MVP

**User Story**: Como morador ou turista, quero explorar os locais no mapa para descobrir opções na cidade.

**Acceptance Criteria**:

1. WHEN o app chama `GET /api/v1/locais/area` com uma caixa válida THEN o sistema SHALL responder 200 com os locais ativos e com posição dentro da caixa (bordas incluídas), ordenados por `id`, cada um com `id`, `nome`, `categoria`, `latitude` e `longitude`. (MAPA-01)
2. IF um local está inativo, ou é conta de loja com `status` diferente de `ATIVO`, THEN o sistema SHALL deixá-lo fora de `/area` e de `/proximos`, mesmo dentro da caixa ou do raio. (MAPA-02)
3. WHEN a consulta informa `pagina` ou `tamanho` THEN o sistema SHALL devolver só aquela página (padrão `pagina` 0 e `tamanho` 20), ordenada por `id`, com `pagina` = `{numero, tamanho, totalItens, totalPaginas}` e `truncado = true` se houver página seguinte. (MAPA-03)
4. WHEN a consulta não informa `pagina`, `tamanho` nem `limite` THEN o sistema SHALL devolver todos os locais da caixa com `truncado = false` e sem o campo `pagina`. (MAPA-04)
5. IF a consulta combina `limite` com `pagina` ou `tamanho` THEN o sistema SHALL responder 400 em Problem Details. (MAPA-05)
6. IF `pagina` < 0, `pagina` > 21474836, `tamanho` < 1 ou `tamanho` > 100 THEN o sistema SHALL responder 400 em Problem Details. (MAPA-06)
7. WHEN a caixa não tem locais THEN o sistema SHALL responder 200 com `itens` vazio e `truncado = false`; com paginação, `totalItens = 0` e `totalPaginas = 0`. (MAPA-07)
8. IF a coordenada está fora da faixa, `oeste > leste` ou `sul > norte` THEN o sistema SHALL responder 400 em Problem Details. (MAPA-08)
9. IF a consulta falha por parâmetro inválido THEN o sistema SHALL responder em Problem Details sem `trace`, `exception`, `message` nem SQL no corpo. (MAPA-09)
10. The system SHALL responder 200 em `/area` para visitante sem token, para usuário com token válido e para usuário com token expirado ou malformado. (MAPA-10)

**Independent Test**: inserir locais fictícios ativos e inativos, dentro e fora da caixa, e chamar `/area` com e sem paginação.

---

### P1: Pesquisa de locais (BACK-023) ⭐ MVP

**User Story**: Como visitante ou usuário, quero pesquisar locais para encontrar uma opção pelo nome, categoria ou termo.

**Acceptance Criteria**:

1. WHEN o app chama `GET /api/v1/locais/pesquisa?termo=X` THEN o sistema SHALL devolver os locais ativos com posição cujo nome, categoria ou algum dos termos contém X, sem diferenciar maiúsculas nem acentos. (BUSCA-01)
2. WHEN a pesquisa informa `categoria` THEN o sistema SHALL devolver só os locais daquela categoria, por igualdade sem diferenciar maiúsculas nem acentos. (BUSCA-02)
3. WHEN a pesquisa informa `termo` e `categoria` THEN o sistema SHALL devolver só os locais que atendem aos dois. (BUSCA-03)
4. WHEN o `termo` está ausente, vazio ou só com espaços THEN o sistema SHALL aplicar só os outros filtros e devolver os locais ativos com posição. (BUSCA-04)
5. WHEN nenhum local atende à pesquisa THEN o sistema SHALL responder 200 com `itens` vazio, `totalItens = 0` e `totalPaginas = 0`. (BUSCA-05)
6. The system SHALL paginar a pesquisa sempre (padrão `pagina` 0 e `tamanho` 20), ordenando por nome sem acento e sem maiúsculas e depois por `id`, e devolver `pagina` = `{numero, tamanho, totalItens, totalPaginas}`. (BUSCA-06)
7. IF `pagina` < 0, `pagina` > 21474836, `tamanho` < 1, `tamanho` > 100, `termo` > 100 caracteres, `categoria` > 50 caracteres, ou `termo` ou `categoria` com o caractere NUL, THEN o sistema SHALL responder 400 em Problem Details sem detalhes internos. (BUSCA-07)
8. WHEN o `termo` contém `%` ou `_` THEN o sistema SHALL tratá-los como caracteres literais. (BUSCA-08)
9. IF um local está inativo, sem posição, ou é conta de loja com `status` diferente de `ATIVO`, THEN o sistema SHALL deixá-lo fora da pesquisa, mesmo que o nome case. (BUSCA-09)
10. The system SHALL devolver em cada item da pesquisa os mesmos campos do marcador do mapa: `id`, `nome`, `categoria`, `latitude` e `longitude`. (BUSCA-10)
11. WHEN a `pagina` passa da última THEN o sistema SHALL responder 200 com `itens` vazio e os totais corretos. (BUSCA-11)
12. The system SHALL responder 200 em `/pesquisa` para visitante sem token, para usuário com token válido e para usuário com token expirado ou malformado. (BUSCA-12)

**Independent Test**: inserir locais fictícios com nomes acentuados, categorias e termos, e pesquisar por termo, categoria, os dois, termo vazio e termo sem resultado.

---

## Edge Cases

- IF o token é inválido numa rota de `/api/v1/contas/**` THEN o sistema SHALL continuar respondendo 401 (sem regressão de CONTA-25).
- WHEN o termo tem acento e o nome não (ou o contrário) THEN o sistema SHALL encontrar o local.
- WHEN um local tem termos vazios e categoria nula THEN o sistema SHALL encontrá-lo só pelo nome, sem erro.
- WHEN o app chama as rotas de uma origem liberada pelo CORS THEN o sistema SHALL devolver o cabeçalho `Access-Control-Allow-Origin` (AD-039).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| CAT-01 | P1: Catálogo | Execute | Verified |
| CAT-02 | P1: Catálogo | Execute | Verified |
| CAT-03 | P1: Catálogo | Execute | Verified |
| MAPA-01 | P1: Área | Execute | Verified |
| MAPA-02 | P1: Área | Execute | Verified |
| MAPA-03 | P1: Área | Execute | Verified |
| MAPA-04 | P1: Área | Execute | Verified |
| MAPA-05 | P1: Área | Execute | Verified |
| MAPA-06 | P1: Área | Execute | Verified |
| MAPA-07 | P1: Área | Execute | Verified |
| MAPA-08 | P1: Área | Execute | Verified |
| MAPA-09 | P1: Área | Execute | Verified |
| MAPA-10 | P1: Área | Execute | Verified |
| BUSCA-01 | P1: Pesquisa | Execute | Verified |
| BUSCA-02 | P1: Pesquisa | Execute | Verified |
| BUSCA-03 | P1: Pesquisa | Execute | Verified |
| BUSCA-04 | P1: Pesquisa | Execute | Verified |
| BUSCA-05 | P1: Pesquisa | Execute | Verified |
| BUSCA-06 | P1: Pesquisa | Execute | Verified |
| BUSCA-07 | P1: Pesquisa | Execute | Verified |
| BUSCA-08 | P1: Pesquisa | Execute | Verified |
| BUSCA-09 | P1: Pesquisa | Execute | Verified |
| BUSCA-10 | P1: Pesquisa | Execute | Verified |
| BUSCA-11 | P1: Pesquisa | Execute | Verified |
| BUSCA-12 | P1: Pesquisa | Execute | Verified |

**Coverage:** 25 total, 25 mapeados para Execute.

---

## Success Criteria

- [ ] `./mvnw test` verde, com os testes novos cobrindo cada AC.
- [ ] Verificador independente com PASS.
