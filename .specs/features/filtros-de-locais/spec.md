# Filtros de locais - Especificação

Card: **BACK-024** (Aplicar filtros de consulta de locais).

## Problem Statement

A pesquisa (`GET /api/v1/locais/pesquisa`) acha locais por um trecho exato do texto e filtra por uma categoria em texto livre. O usuário não consegue refinar por preço, distância ou horário. A busca também falha em casos comuns: palavras fora de ordem ("centro café"), plural ("bares"), erro de digitação ("pizaria") ou o nome da categoria ("lanche" para uma lanchonete). E a loja que se cadastra como "bar" não aparece no filtro de bar, porque o `tipo` do cadastro não vira `categoria`.

## Goals

- [ ] O usuário refina a pesquisa por categoria, faixa de preço, distância e "aberto agora", combinando os filtros com o termo e com a paginação.
- [ ] Quem busca encontra o local mesmo com palavras fora de ordem, plural, acento, erro de digitação leve ou sinônimo da categoria, e o mais relevante vem primeiro.
- [ ] A loja cadastrada com um tipo aparece no filtro da categoria correspondente.

## Out of Scope

| Item | Motivo |
| ---- | ------ |
| Filtros no mapa (`/area`) | O mapa é só visual; a pesquisa é independente dele (decisão do usuário em 2026-11-12) |
| Telas, navegação, componentes visuais, permissão de localização do aparelho | Fora do escopo do BACK-024 |
| Filtro de movimentação (Gerômetro) e preferências aprendidas | Limites do produto no card |
| Tabela de categorias e rota que lista categorias | A lista é fixa e pequena; o front a conhece pelo contrato (decisão do usuário: escopo enxuto) |
| Horário diferente por dia da semana | Um horário diário cobre o MVP; por dia entra com o cadastro administrativo (BACK-012) |
| Cadastro de preço, horário e termos do local | Não há cadastro administrativo ainda (BACK-012); os testes inserem locais fictícios |

---

## Assumptions & Open Questions

| Assunto | Decisão | Motivo | Confirmado? |
| ------- | ------- | ------- | ----------- |
| Lista de categorias | `bar`, `restaurante`, `cafe`, `padaria`, `lanchonete`, `balada`, `evento`, `outro`, fixa no código e garantida no banco por CHECK | Aprovada pelo usuário em 2026-11-12 | s |
| Onde ficam os filtros | Só em `/pesquisa` | Decisão do usuário em 2026-11-12 | s |
| Faixa de preço | `faixa_preco` de 1 a 4 ($ a $$$$); o filtro `preco` aceita um ou mais valores | Padrão conhecido pelos usuários de apps de lugares | n |
| Funcionamento | `abre` e `fecha` (TIME), o mesmo horário todo dia. `fecha` menor que `abre` vira a meia-noite; `abre = fecha` é 24 horas | Bares de Aracaju fecham depois da meia-noite | n |
| Fuso dos horários | `America/Maceio` (horário de Aracaju, UTC-3), independente do fuso do servidor e do banco | O servidor pode rodar em UTC | n |
| Dado desconhecido | Local sem preço ou sem horário fica fora quando o filtro correspondente está ligado; no item vem `null` | Não dá para garantir que ele atende ao filtro | n |
| Origem ausente | Sem `latitude`/`longitude`, a distância fica indisponível: sem `distanciaMetros` e `raioMetros` dá 400. Os outros filtros funcionam | Card pede tratar origem ausente | n |
| Raio máximo | `raioMetros` de 1 a 100000 (100 km) | Cobre a Grande Aracaju e limita a consulta | n |
| Categoria desconhecida | 400 em Problem Details. Aceita o código ou o nome, sem diferenciar maiúsculas nem acentos ("Café" = `cafe`) | O app descobre o erro em vez de receber lista vazia | n |
| Limpeza de filtros | Filtro ausente ou vazio não filtra; `abertoAgora=false` também não filtra | Limpar um filtro é tirá-lo da URL | n |
| Busca por palavras | Cada palavra do termo precisa casar com o nome, os termos ou a categoria do local, em qualquer ordem. Ignora `de`, `da`, `do`, `das`, `dos`, `e`, `em`, `na`, `no`, `a`, `o`, `as`, `os`, `com`, `sem`, `para`, `pra` (salvo se o termo só tiver essas palavras) | "café do centro" acha "Centro Café" | n |
| Plural | Palavra com 4 letras ou mais terminada em `s` casa também sem o `s` (`es` também sai quando sobram 3 letras ou mais) | "bares" acha "Bar do Zé" | n |
| Erro de digitação | Palavra com 4 letras ou mais casa por semelhança de trigramas (`pg_trgm`, `strict_word_similarity` ≥ 0,5) com nome e termos; na categoria, por distância de edição até 1 com o código, o nome ou um sinônimo | "pizaria" acha "Pizzaria Napoli" | n |
| Sinônimos de categoria | Cada categoria tem nome e sinônimos fixos (ex.: bar: boteco, pub, choperia, cervejaria). A palavra cita a categoria quando o código, o nome ou um sinônimo começa com ela (3 letras ou mais) | "boteco" e "cafeter" acham a categoria; "ria" não cita choperia, pizzaria e cafeteria (Verificador, iteração 1) | n |
| Ordem | Com termo: nome começa com o termo; nome contém o termo; casa sem semelhança; casa por semelhança. Depois distância (se houver origem), nome sem acento e `id` | O que o usuário digitou exatamente vem primeiro | n |
| Cadastro da loja | `tipo` passa a aceitar só uma categoria da lista (código ou nome); grava o código em `tipo` e em `categoria`; outro valor dá 400 em `campos.tipo` | Sem isso a loja nunca aparece no filtro de categoria | n |

**Open questions:** none - os itens não confirmados ficam com o padrão acima.

---

## User Stories

### P1: Filtros da pesquisa ⭐ MVP

**User Story**: Como visitante ou usuário, quero refinar locais para escolher opções adequadas ao meu interesse.

**Acceptance Criteria**:

1. WHEN a pesquisa informa um ou mais `categoria` THEN o sistema SHALL devolver só os locais de alguma delas, aceitando código ou nome sem diferenciar maiúsculas nem acentos. (FILT-01)
2. WHEN a pesquisa informa um ou mais `preco` THEN o sistema SHALL devolver só os locais com `faixaPreco` entre os informados. (FILT-02)
3. WHEN a pesquisa informa `abertoAgora=true` THEN o sistema SHALL devolver só os locais abertos no horário atual de Aracaju (`America/Maceio`), incluindo horário que vira a meia-noite e 24 horas, com abertura inclusiva e fechamento exclusivo. (FILT-03)
4. WHEN a pesquisa informa `latitude` e `longitude` THEN o sistema SHALL devolver `distanciaMetros` em cada item e, com `raioMetros`, só os locais dentro do raio. (FILT-04)
5. WHEN vários filtros são informados THEN o sistema SHALL devolver só os locais que atendem a todos, junto com o `termo` e paginado como a pesquisa. (FILT-05)
6. WHEN um filtro está ausente, vazio ou `abertoAgora=false` THEN o sistema SHALL não aplicar esse filtro. (FILT-06)
7. IF um filtro está ligado e o local não tem o dado (preço ou horário) THEN o sistema SHALL deixá-lo fora; sem filtro, o item vem com o campo `null`. (FILT-07)
8. IF `raioMetros` vem sem origem, ou só um de `latitude` e `longitude` vem THEN o sistema SHALL responder 400 em Problem Details. (FILT-08)
9. IF `categoria` é desconhecida, `preco` está fora de 1 a 4, `raioMetros` fora de 1 a 100000, coordenada fora da faixa, ou há mais de 20 valores de `categoria` ou de `preco` THEN o sistema SHALL responder 400 em Problem Details sem detalhes internos. (FILT-09)
10. The system SHALL devolver em cada item `id`, `nome`, `categoria`, `latitude`, `longitude`, `faixaPreco`, `abertoAgora` e `distanciaMetros` (`null` quando desconhecido ou sem origem). (FILT-10)
11. WHEN a pesquisa tem origem e não tem termo THEN o sistema SHALL ordenar do mais perto ao mais longe, depois por nome e `id`. (FILT-11)

**Independent Test**: inserir locais fictícios com categorias, preços, horários e posições diferentes, fixar o relógio e combinar os filtros.

---

### P1: Busca que encontra ⭐ MVP

**User Story**: Como visitante ou usuário, quero encontrar o local que procuro mesmo sem escrever o nome exato.

**Acceptance Criteria**:

1. WHEN o termo tem várias palavras THEN o sistema SHALL devolver os locais em que cada palavra casa com o nome, os termos ou a categoria, em qualquer ordem. (ACHAR-01)
2. WHEN o termo tem palavras de ligação (`de`, `do`, `da`, `com`, `para` etc.) THEN o sistema SHALL ignorá-las, salvo se o termo só tiver essas palavras. (ACHAR-02)
3. WHEN uma palavra está no plural THEN o sistema SHALL casar também com o singular. (ACHAR-03)
4. WHEN uma palavra com 4 letras ou mais tem um erro leve de digitação THEN o sistema SHALL casar por semelhança com o nome ou os termos, e por distância de edição até 1 com o código, o nome ou os sinônimos da categoria. (ACHAR-04)
5. WHEN o código, o nome ou um sinônimo de uma categoria começa com uma palavra de 3 letras ou mais THEN o sistema SHALL devolver os locais dessa categoria; um trecho do meio não cita a categoria. (ACHAR-05)
6. The system SHALL ordenar a pesquisa com termo por relevância: nome começa com o termo; nome contém o termo; casa sem semelhança; casa só por semelhança; depois distância (se houver origem), nome e `id`. (ACHAR-06)
7. WHEN uma palavra não casa com nada THEN o sistema SHALL deixar o local fora, mesmo que as outras casem. (ACHAR-07)

**Independent Test**: inserir locais com nomes, termos e categorias e pesquisar com ordem trocada, plural, erro de digitação, sinônimo e palavra sem relação.

---

### P1: Loja na categoria certa ⭐ MVP

**User Story**: Como loja, quero aparecer no filtro da minha categoria quando me cadastro.

**Acceptance Criteria**:

1. WHEN a loja se cadastra com `tipo` igual a um código ou nome de categoria THEN o sistema SHALL gravar o código em `tipo` e em `categoria` e devolvê-lo em `tipo`. (LOJA-01)
2. IF o `tipo` não é uma categoria da lista THEN o sistema SHALL responder 400 em Problem Details com `campos.tipo` e não criar a conta. (LOJA-02)
3. WHEN a loja se cadastra sem `tipo` THEN o sistema SHALL criá-la com `categoria` nula, como antes. (LOJA-03)

**Independent Test**: cadastrar lojas com "Bar", "café", "academia" e sem tipo, e conferir a resposta e a linha no banco.

---

### P1: Dados do catálogo ⭐ MVP

**User Story**: Como equipe do Mapaê, quero que o banco guarde preço e horário e garanta categorias válidas.

**Acceptance Criteria**:

1. WHEN a migração dos filtros roda THEN o sistema SHALL acrescentar a `local` `faixa_preco SMALLINT` (1 a 4, nula), `abre TIME` e `fecha TIME` (os dois nulos ou os dois preenchidos), restringir `categoria` à lista e instalar `pg_trgm` no schema `extensions`. (DADOS-01)
2. IF alguém grava `categoria` fora da lista, `faixa_preco` fora de 1 a 4 ou só um de `abre` e `fecha` THEN o banco SHALL recusar a linha. (DADOS-02)
3. The system SHALL manter a lista de categorias do código igual à do CHECK do banco. (DADOS-03)

**Independent Test**: rodar as migrações e tentar inserir linhas inválidas.

---

## Edge Cases

- WHEN o termo tem `%`, `_` ou `\` THEN o sistema SHALL tratá-los como literais (sem regressão de BUSCA-08).
- WHEN o termo tem SQL THEN o sistema SHALL tratá-lo como texto.
- WHEN o horário é exatamente o de fechamento THEN o local SHALL estar fechado; exatamente o de abertura, aberto.
- WHEN a pesquisa não tem nenhum filtro nem termo THEN o sistema SHALL listar o catálogo ativo por nome, como antes.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| FILT-01 | P1: Filtros | Execute | Verified |
| FILT-02 | P1: Filtros | Execute | Verified |
| FILT-03 | P1: Filtros | Execute | Verified |
| FILT-04 | P1: Filtros | Execute | Verified |
| FILT-05 | P1: Filtros | Execute | Verified |
| FILT-06 | P1: Filtros | Execute | Verified |
| FILT-07 | P1: Filtros | Execute | Verified |
| FILT-08 | P1: Filtros | Execute | Verified |
| FILT-09 | P1: Filtros | Execute | Verified |
| FILT-10 | P1: Filtros | Execute | Verified |
| FILT-11 | P1: Filtros | Execute | Verified |
| ACHAR-01 | P1: Busca | Execute | Verified |
| ACHAR-02 | P1: Busca | Execute | Verified |
| ACHAR-03 | P1: Busca | Execute | Verified |
| ACHAR-04 | P1: Busca | Execute | Verified |
| ACHAR-05 | P1: Busca | Execute | Verified |
| ACHAR-06 | P1: Busca | Execute | Verified |
| ACHAR-07 | P1: Busca | Execute | Verified |
| LOJA-01 | P1: Loja | Execute | Verified |
| LOJA-02 | P1: Loja | Execute | Verified |
| LOJA-03 | P1: Loja | Execute | Verified |
| DADOS-01 | P1: Dados | Execute | Verified |
| DADOS-02 | P1: Dados | Execute | Verified |
| DADOS-03 | P1: Dados | Execute | Verified |

**Coverage:** 24 total, 24 mapeados para Execute.
