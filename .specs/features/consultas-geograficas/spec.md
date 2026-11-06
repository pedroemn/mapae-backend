# Consultas geográficas e validação de localização (BACK-006) Specification

## Problem Statement

O app precisa mostrar os locais cadastrados da região visível do mapa ou próximos do usuário. Hoje o backend não tem banco conectado nem API web. A busca livre no mapa não usa os locais cadastrados e deve ficar com um provedor externo chamado pelo app.

## Goals

- [ ] O app consulta locais por área e por proximidade usando índices espaciais do PostGIS.
- [ ] O schema é criado inteiramente por migrações Flyway ao conectar no Supabase.
- [ ] Contrato, limites e proteção de credenciais ficam registrados para o app.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Registrar confirmação de ida (tabela `confirmacao_ida`) | Depende de usuário autenticado (AD-004), task própria |
| Validação de localização da confirmação de ida | A confirmação de ida pode ser feita de qualquer lugar (AD-026) |
| Cadastro e detalhes de locais | Task própria; aqui a tabela `local` é mínima |
| Proxy de geocoding ou de rotas | A task proíbe proxy quando o app pode chamar o serviço direto |
| Autenticação e rate limit | JWT ainda não existe (AD-004) |
| Conexão real ao Supabase | O usuário fornece a senha depois |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Limites de consulta | Nenhum raio, limite padrão ou máximo (AD-028) | O usuário pesquisa de qualquer lugar; a equipe decide quando paginar |
| Colunas de `local` | `id`, `nome`, `posicao` | A task de cadastro completa o resto | n |
| Provedor de geocoding | A escolher pela equipe do app | Não afeta o backend | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Schema geográfico versionado ⭐ MVP

**User Story**: As a integrante da equipe, I want que o schema seja criado pelo Flyway so that nunca seja preciso usar o SQL Editor.

**Why P1**: Todas as consultas dependem dele.

**Acceptance Criteria**:

1. WHEN a aplicação sobe em um banco vazio THEN the system SHALL instalar o PostGIS no schema `extensions` e criar a tabela `local` com `posicao geography(Point, 4326)` <!-- GEO-01 -->
2. The system SHALL manter `local` com RLS ligado e sem políticas <!-- GEO-02 -->
3. The system SHALL subir com `ddl-auto=validate` aceitando o mapeamento da entidade `Local` <!-- GEO-03 -->
4. The system SHALL manter `flyway_schema_history` no schema `flyway`, fora do `public` <!-- GEO-19 -->

**Independent Test**: subir o contexto em um PostGIS vazio e inspecionar o catálogo.

### P1: Consulta por área ⭐ MVP

**User Story**: As a visitante, I want ver os locais da região visível do mapa so that eu descubra o que há ali.

**Why P1**: É a tela inicial do app (RF02).

**Acceptance Criteria**:

1. WHEN o app chama `GET /api/locais/area` com `oeste`, `sul`, `leste` e `norte` THEN the system SHALL responder 200 com os locais dentro da caixa, bordas inclusive, ordenados por `id` <!-- GEO-04 -->
2. WHEN há mais locais do que o `limite` informado THEN the system SHALL devolver `limite` itens e `truncado=true` <!-- GEO-05 -->
3. IF `oeste > leste` ou `sul > norte` THEN the system SHALL responder 400 <!-- GEO-06 -->

**Independent Test**: inserir locais dentro e fora da caixa e chamar o endpoint.

### P1: Consulta por proximidade ⭐ MVP

**User Story**: As a visitante, I want ver os locais em um raio a partir de um ponto so that eu ache o que está perto.

**Why P1**: Requisito "Busca por proximidade" do produto.

**Acceptance Criteria**:

1. WHEN o app chama `GET /api/locais/proximos` com `latitude`, `longitude` e, opcionalmente, `raioMetros` THEN the system SHALL responder 200 com os locais, do mais perto ao mais longe, com `distanciaMetros`, e só os do raio quando ele vier <!-- GEO-07 -->
2. WHEN o app chama `GET /api/locais/proximos` sem coordenadas THEN the system SHALL responder 200 com todos os locais por `id`, sem `distanciaMetros`, e SHALL NOT impor raio ou limite máximos (AD-028) <!-- GEO-08 -->
3. IF uma coordenada estiver fora da faixa, for `NaN`, vier sem a outra ou `raioMetros` vier sem coordenadas THEN the system SHALL responder 400 <!-- GEO-09 -->

**Independent Test**: inserir locais a distâncias conhecidas e conferir ordem e distância.

### P1: Fronteiras de módulos ⭐ MVP

**User Story**: As a integrante da equipe, I want que os módulos respeitem suas fronteiras so that fiquem desacoplados.

**Why P1**: AD-002 e AD-005.

**Acceptance Criteria**:

1. The system SHALL passar em `ApplicationModules.verify()` <!-- GEO-18 -->

**Independent Test**: rodar o teste de modularidade.

---

## Edge Cases

- WHEN um local está exatamente na borda da caixa THEN the system SHALL incluí-lo (coberto por GEO-04).
- WHEN a área não tem locais THEN the system SHALL responder 200 com lista vazia e `truncado=false`.
- IF `limite` for menor que 1 THEN the system SHALL responder 400.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| GEO-01 | P1: Schema | Execute | Pending |
| GEO-02 | P1: Schema | Execute | Pending |
| GEO-03 | P1: Schema | Execute | Pending |
| GEO-04 | P1: Área | Execute | Pending |
| GEO-05 | P1: Área | Execute | Pending |
| GEO-06 | P1: Área | Execute | Pending |
| GEO-07 | P1: Proximidade | Execute | Pending |
| GEO-08 | P1: Proximidade | Execute | Pending |
| GEO-09 | P1: Proximidade | Execute | Pending |
| GEO-18 | P1: Módulos | Execute | Pending |
| GEO-19 | P1: Schema | Execute | Pending |

**Coverage:** 11 total, 11 mapped, 0 unmapped. GEO-10 a GEO-17 foram removidos pela AD-026, e os números não são reaproveitados.

---

## Success Criteria

- [ ] `./mvnw clean verify` passa com o PostGIS em Docker.
- [ ] As migrações rodam também na imagem do Supabase, com PostGIS 3.3.
