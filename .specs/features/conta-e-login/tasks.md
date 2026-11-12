# Contas e login Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

Sem commits do agente (AD-001): cada task termina com o gate verde e as mudanças ficam no working tree.

---

**Design**: `.specs/features/conta-e-login/design.md`
**Status**: Done (Verificador: PASS na iteração 3, 2026-11-12)

---

## Test Coverage Matrix

| Code layer | Test type | Coverage expectation |
| ---------- | --------- | -------------------- |
| Migrações / schema | integration (PostGIS em Testcontainers, consultas ao catálogo) | Cada coluna, restrição e RLS do spec |
| Serviços de conta | integration via rotas (MockMvc + banco real) | ACs 1:1 |
| Rotas HTTP e segurança | integration (MockMvc + banco real) | Caminho feliz, cada erro documentado |
| Aplicação no Supabase | none (operação) | Log do Flyway + consulta ao catálogo |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| quick | Uma classe de teste | `./mvnw -q test -Dtest=<Classe>` |
| full | Integração | `./mvnw test` |
| build | Fim de fase | `./mvnw verify` |

---

## Execution Plan

### Phase 1: Banco

```
T1 → T2
```

### Phase 2: Login

```
T3 → T4 → T5 → T6 → T7 → T8
```

---

## Task Breakdown

### T1: Migração das tabelas de conta e mapeamento JPA

**What**: Migração `V<timestamp>__criar_contas.sql`, `DadosDeAcesso`, entidade `Usuario` e campos de conta em `Local`, com filtro de posição nas consultas.
**Where**: `src/main/resources/db/migration/V20261111103000__criar_contas.sql`
**Depends on**: None
**Reuses**: padrão de `V20261107022500__criar_tabela_migracao.sql`
**Requirement**: CONTA-01, CONTA-02, CONTA-03, CONTA-04, CONTA-05, CONTA-06, CONTA-13

**Done when**:

- [x] Testes de schema em `ContasSchemaTests` passam (colunas, índices únicos sem diferenciar maiúsculas, RLS, linha antiga preservada)
- [x] Loja sem posição não aparece nas consultas do mapa
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T2: Aplicar a migração no Supabase

**What**: Subir a aplicação com o `.env` para o Flyway aplicar a migração no Supabase e conferir o catálogo.
**Where**: `.env` (somente leitura)
**Depends on**: T1
**Requirement**: CONTA-01, CONTA-02

**Done when**:

- [x] Log do Flyway mostra a nova versão aplicada no Supabase
- [x] `usuario` existe no Supabase e `local.posicao` aceita nulo

**Tests**: none
**Gate**: build

---

### T3: Infraestrutura de segurança e JWT

**What**: Dependências de segurança, `SegurancaConfig`, `JwtPropriedades`, `EmissorDeToken`, `Autenticador`, `ProvedorDeConta`, exceções e esquema Bearer no Swagger.
**Where**: `src/main/java/br/com/mapae/compartilhado/seguranca/SegurancaConfig.java`
**Depends on**: None (fase 2 começa depois da fase 1)
**Requirement**: CONTA-20, CONTA-25, CONTA-26

**Done when**:

- [x] Rotas antigas, Swagger e cadastro continuam públicos
- [x] Rota protegida sem token ou com token inválido responde 401 em Problem Details
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T4: Cadastro de usuário

**What**: `POST /api/v1/usuarios` com validação, normalização e 409.
**Where**: `src/main/java/br/com/mapae/conta/aplicacao/ContasDeUsuario.java`
**Depends on**: T3
**Requirement**: CONTA-07, CONTA-08, CONTA-09, CONTA-10, CONTA-11

**Done when**:

- [x] `CadastroUsuarioTests` cobre cada AC
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T5: Cadastro de loja

**What**: `POST /api/v1/lojas` gravando em `local` sem posição.
**Where**: `src/main/java/br/com/mapae/locais/aplicacao/ContasDeLoja.java`
**Depends on**: T4
**Requirement**: CONTA-12, CONTA-13, CONTA-14, CONTA-15

**Done when**:

- [x] `CadastroLojaTests` cobre cada AC
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T6: Login de usuário e de loja

**What**: `POST /api/v1/auth/usuarios/login` e `POST /api/v1/auth/lojas/login`.
**Where**: `src/main/java/br/com/mapae/compartilhado/seguranca/Autenticador.java`
**Depends on**: T5
**Requirement**: CONTA-16, CONTA-17, CONTA-18, CONTA-19, CONTA-20, CONTA-21

**Done when**:

- [x] `LoginTests` cobre cada AC, incluindo as claims do token
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T7: Conta autenticada e primeiro acesso

**What**: `GET /api/v1/contas/eu` e `POST /api/v1/contas/eu/primeiro-acesso/conclusao`.
**Where**: `src/main/java/br/com/mapae/conta/web/ContaController.java`
**Depends on**: T6
**Requirement**: CONTA-22, CONTA-23, CONTA-24, CONTA-25

**Done when**:

- [x] `ContaAutenticadaTests` cobre cada AC para usuário e loja
- [x] Gate check passes: `./mvnw test`

**Tests**: integration
**Gate**: full

---

### T8: Troca de senha

**What**: `PUT /api/v1/contas/eu/senha`.
**Where**: `src/main/java/br/com/mapae/conta/web/ContaController.java` (modify)
**Depends on**: T7
**Requirement**: CONTA-27, CONTA-28

**Done when**:

- [x] Testes de troca de senha passam
- [x] Gate check passes: `./mvnw verify`

**Tests**: integration
**Gate**: build
