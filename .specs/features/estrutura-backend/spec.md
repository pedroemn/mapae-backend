# Estrutura do Backend Specification

> **Status:** referência para tasks futuras. Esta spec não será entregue de uma vez: o projeto parte de pastas vazias (AD-013 em `docs/decisões.md`), e a persistência saiu do "fora de escopo" (AD-007 e AD-008).

## Problem Statement

O repositório `mapae-backend` é um projeto Spring Boot recém-gerado (pacote `com.example`, sem módulos, sem segurança). A Etapa 3 do cronograma (15/11) exige construir as funcionalidades sociais sobre uma base já organizada. Sem uma estrutura modular, segurança e modelo de domínio definidos agora, cada integrante da equipe vai criar padrões diferentes.

## Goals

- [ ] Projeto compila e todos os testes passam com `./mvnw verify` em Java 25.
- [ ] Os 5 módulos de negócio do MVP (conta, locais, amizades, feed, confirmacaoida) existem e a verificação do Spring Modulith passa sem violações.
- [ ] O modelo de domínio do diagrama de classes (slide 11) existe em Java puro, com as regras de transição cobertas por testes.
- [ ] Segurança JWT stateless distingue rotas de visitante e de usuário autenticado.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Persistência (JPA, PostgreSQL/PostGIS, Flyway, Redis) | Decisão do usuário: banco fica fora nesta entrega |
| Implementação dos repositórios | Sem banco; só as interfaces (portas) são declaradas |
| Endpoints de negócio de RF01–RF04 (cadastro, login, busca, amizades, feed, confirmação de ida) | Profundidade escolhida: esqueleto + domínio |
| Logout com invalidação de token | Exige uma denylist persistente (Redis), que está fora desta entrega |
| Gerômetro | Fora do MVP (slide 12) |
| Upload de mídia | Não está no RF01–RF04 desta etapa |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Pacote base | `br.com.mapae` | Usuário pediu o pacote do projeto | y |
| Versão do Java | 25 (LTS) | Decisão anterior registrada; JDK 26 instalado compila para 25; Boot 4.1 suporta | n |
| Módulos | `conta`, `locais`, `amizades`, `feed`, `confirmacaoida` + `compartilhado` (infra comum) | Um por RF (RF01–RF04), confirmação de ida separado por causa do Gerômetro futuro | n |
| Tipo `Point` do diagrama | Value object `Coordenada(latitude, longitude)` | Sem PostGIS ainda; troca pelo tipo espacial quando o banco entrar | n |
| Faixa de preço do Local | inteiro de 1 a 4 | Convenção "$ a $$$$" | n |
| Algoritmo de `distanciaAte` | Haversine, raio da Terra 6371 km, retorno em km | Padrão para distância em superfície esférica | n |
| Assinatura do JWT | HS256 com segredo em `MAPAE_JWT_SECRET` (≥ 32 bytes) | Sem infraestrutura de chaves; HS256 exige ≥ 256 bits | n |
| Validade do token | 60 minutos, configurável em `mapae.jwt.expiracao` | Padrão comum para access token | n |
| Prefixo da API | `/api/v1` | Versionamento desde o início | n |
| Formato de erro | `ProblemDetail` (RFC 9457), nativo do Spring | Padrão do Spring, sem DTO próprio | n |
| Commits | Nenhum commit pelo agente; o usuário commita | Instrução explícita do usuário (sobrepõe a regra de commit atômico do processo) | y |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Base modular do projeto ⭐ MVP

**User Story**: As a desenvolvedor da equipe, I want módulos com fronteiras verificadas so that cada pessoa trabalhe em uma área sem acoplar as outras.

**Why P1**: Sem isso, as Etapas 3 e 4 não têm onde se apoiar.

**Acceptance Criteria** (each line is one EARS pattern):

1. The system SHALL use `br.com.mapae` as root package with modules `conta`, `locais`, `amizades`, `feed`, `confirmacaoida` and `compartilhado`.  <!-- EST-01 -->
2. The system SHALL pass `ApplicationModules.of(MapaeApplication.class).verify()` with zero violations.  <!-- EST-02 -->
3. WHEN `GET /actuator/health` is called without credentials THEN the system SHALL respond `200` with body field `status` = `UP`.  <!-- EST-03 -->
4. The system SHALL compile and run tests on Java 25 via `./mvnw verify`.  <!-- EST-04 -->

**Independent Test**: `./mvnw verify` passa; `curl localhost:8080/actuator/health` retorna `{"status":"UP"}`.

---

### P1: Segurança JWT stateless ⭐ MVP

**User Story**: As a usuário autenticado, I want que minhas requisições sejam identificadas por um token so that só eu acesse ações da minha conta, enquanto visitantes exploram o mapa sem conta.

**Why P1**: RNF02 e a distinção visitante/autenticado (slide 5) atravessam todos os módulos.

**Acceptance Criteria**:

1. WHEN a request without `Authorization` header reaches a protected route THEN the system SHALL respond `401`.  <!-- SEG-01 -->
2. WHEN a request without credentials calls `GET /api/v1/locais/**`, `GET /api/v1/categorias/**` or `POST /api/v1/auth/**` THEN the system SHALL NOT respond `401`.  <!-- SEG-02 -->
3. WHEN `TokenService` issues a token for usuário id `42` THEN the system SHALL produce an HS256 JWT with `sub` = `"42"` and `exp` = issue time + `mapae.jwt.expiracao`.  <!-- SEG-03 -->
4. WHEN `GET /api/v1/conta/eu` is called with a valid token for id `42` THEN the system SHALL respond `200` with body field `id` = `42`.  <!-- SEG-04 -->
5. IF the token is expired or signed with another secret THEN the system SHALL respond `401`.  <!-- SEG-05 -->
6. The system SHALL hash passwords with BCrypt through a `PasswordEncoder` bean.  <!-- SEG-06 -->
7. IF `MAPAE_JWT_SECRET` is missing or shorter than 32 bytes THEN the system SHALL fail application startup.  <!-- SEG-07 -->
8. The system SHALL create no HTTP session (`SessionCreationPolicy.STATELESS`).  <!-- SEG-08 -->

**Independent Test**: teste MockMvc chama rota protegida sem token (401), com token válido (200) e com token expirado (401).

---

### P1: Modelo de domínio ⭐ MVP

**User Story**: As a desenvolvedor da equipe, I want as classes do diagrama de domínio com suas regras so that os endpoints das Etapas 3 e 4 reutilizem regras já testadas.

**Why P1**: O diagrama (slide 11) é o contrato acordado com o PO.

**Acceptance Criteria**:

1. The system SHALL declare `Usuario`, `Local`, `Categoria`, `Amizade`, `Publicacao`, `Comentario`, `Curtida`, `ConfirmacaoIda` and enums `StatusAmizade`, `TipoPublicacao` with the attributes of the diagram, each in its module.  <!-- DOM-01 -->
2. WHEN an `Amizade` is created THEN the system SHALL set status `PENDENTE` and `respondidaEm` = null.  <!-- DOM-02 -->
3. WHEN `aceitar()` is called on a `PENDENTE` Amizade THEN the system SHALL set status `ACEITA` and fill `respondidaEm`.  <!-- DOM-03 -->
4. WHEN `recusar()` is called on a `PENDENTE` Amizade THEN the system SHALL set status `RECUSADA` and fill `respondidaEm`.  <!-- DOM-04 -->
5. IF `aceitar()` or `recusar()` is called on an Amizade that is not `PENDENTE` THEN the system SHALL throw `RegraDeNegocioException` and keep the status unchanged.  <!-- DOM-05 -->
6. IF an Amizade is created with `solicitanteId` equal to `destinatarioId` THEN the system SHALL throw `RegraDeNegocioException`.  <!-- DOM-06 -->
7. IF `adicionarAutor(u)` is called on an `INDIVIDUAL` Publicacao that already has 1 autor THEN the system SHALL throw `RegraDeNegocioException`.  <!-- DOM-07 -->
8. WHEN `adicionarAutor(u)` is called on a `COLABORATIVA` Publicacao with a new autor THEN the system SHALL add the autor to the list.  <!-- DOM-08 -->
9. IF `adicionarAutor(u)` is called with an autor already in the list THEN the system SHALL throw `RegraDeNegocioException`.  <!-- DOM-09 -->
10. IF a `Coordenada` is created with latitude outside [-90, 90] or longitude outside [-180, 180] THEN the system SHALL throw `IllegalArgumentException`.  <!-- DOM-10 -->
11. WHEN `distanciaAte(p)` is called between Aracaju (-10.9472, -37.0731) and Salvador (-12.9714, -38.5014) THEN the system SHALL return 271 km ± 2 km.  <!-- DOM-11 -->
12. IF a `Local` is created with `faixaPreco` outside 1..4 THEN the system SHALL throw `IllegalArgumentException`.  <!-- DOM-12 -->
13. IF a `Comentario` is created with blank `texto` THEN the system SHALL throw `IllegalArgumentException`.  <!-- DOM-13 -->
14. The system SHALL declare one repository interface per aggregate (`UsuarioRepositorio`, `LocalRepositorio`, `CategoriaRepositorio`, `AmizadeRepositorio`, `PublicacaoRepositorio`, `ConfirmacaoIdaRepositorio`) with no implementation.  <!-- DOM-14 -->

**Independent Test**: testes unitários JUnit sobre as classes de domínio, sem subir o Spring.

---

### P2: Tratamento de erros padronizado

**User Story**: As a desenvolvedor do app mobile, I want um formato único de erro so that o app trate falhas de qualquer endpoint da mesma forma.

**Why P2**: Necessário assim que os endpoints da Etapa 3 entrarem; barato fazer agora.

**Acceptance Criteria**:

1. IF a request body fails Bean Validation THEN the system SHALL respond `400` with a `ProblemDetail` containing property `erros` listing each invalid field.  <!-- ERR-01 -->
2. IF a `RecursoNaoEncontradoException` is thrown THEN the system SHALL respond `404` as `ProblemDetail`.  <!-- ERR-02 -->
3. IF a `RegraDeNegocioException` is thrown THEN the system SHALL respond `422` as `ProblemDetail`.  <!-- ERR-03 -->

**Independent Test**: teste MockMvc com um controller de teste que lança cada exceção.

---

### P2: Configuração e documentação

**User Story**: As a integrante da equipe, I want rodar o projeto seguindo o README so that eu configure o ambiente sem perguntar a ninguém.

**Why P2**: Quatro pessoas vão rodar o projeto.

**Acceptance Criteria**:

1. The system SHALL read secrets only from environment variables, with `.env.example` listing every variable and no real value.  <!-- CFG-01 -->
2. The system SHALL provide a `README.md` describing modules, how to run, how to test and the decisions of this spec.  <!-- CFG-02 -->

**Independent Test**: `MAPAE_JWT_SECRET=... ./mvnw spring-boot:run` sobe seguindo só o README.

---

## Edge Cases

- IF the `Authorization` header is not `Bearer <token>` THEN the system SHALL respond `401`.
- WHEN `distanciaAte` is called with the same point THEN the system SHALL return 0.
- IF `Publicacao` is created with null `tipo` THEN the system SHALL throw `IllegalArgumentException`.

---

## Implicit-Requirement Dimensions Sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | DOM-10, DOM-12, DOM-13, ERR-01 |
| Failure / partial-failure states | ERR-02, ERR-03 |
| Idempotency / retry / duplicate handling | DOM-09 (autor duplicado); demais N/A because sem endpoints de escrita nesta entrega |
| Auth boundaries & rate limits | SEG-01, SEG-02; rate limit N/A because sem endpoints de negócio ainda |
| Concurrency / ordering | N/A because sem persistência nesta entrega |
| Data lifecycle / expiry | SEG-03 (expiração do token) |
| Observability | EST-03 (health) |
| External-dependency failure | N/A because nenhuma dependência externa (banco fora) |
| State-transition integrity | DOM-02 a DOM-05 |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| EST-01 | P1: Base modular | Design | Pending |
| EST-02 | P1: Base modular | Design | Pending |
| EST-03 | P1: Base modular | Design | Pending |
| EST-04 | P1: Base modular | Design | Pending |
| SEG-01 | P1: Segurança JWT | Design | Pending |
| SEG-02 | P1: Segurança JWT | Design | Pending |
| SEG-03 | P1: Segurança JWT | Design | Pending |
| SEG-04 | P1: Segurança JWT | Design | Pending |
| SEG-05 | P1: Segurança JWT | Design | Pending |
| SEG-06 | P1: Segurança JWT | Design | Pending |
| SEG-07 | P1: Segurança JWT | Design | Pending |
| SEG-08 | P1: Segurança JWT | Design | Pending |
| DOM-01 | P1: Modelo de domínio | Design | Pending |
| DOM-02 | P1: Modelo de domínio | Design | Pending |
| DOM-03 | P1: Modelo de domínio | Design | Pending |
| DOM-04 | P1: Modelo de domínio | Design | Pending |
| DOM-05 | P1: Modelo de domínio | Design | Pending |
| DOM-06 | P1: Modelo de domínio | Design | Pending |
| DOM-07 | P1: Modelo de domínio | Design | Pending |
| DOM-08 | P1: Modelo de domínio | Design | Pending |
| DOM-09 | P1: Modelo de domínio | Design | Pending |
| DOM-10 | P1: Modelo de domínio | Design | Pending |
| DOM-11 | P1: Modelo de domínio | Design | Pending |
| DOM-12 | P1: Modelo de domínio | Design | Pending |
| DOM-13 | P1: Modelo de domínio | Design | Pending |
| DOM-14 | P1: Modelo de domínio | Design | Pending |
| ERR-01 | P2: Erros padronizados | Design | Pending |
| ERR-02 | P2: Erros padronizados | Design | Pending |
| ERR-03 | P2: Erros padronizados | Design | Pending |
| CFG-01 | P2: Configuração | Design | Pending |
| CFG-02 | P2: Configuração | Design | Pending |

**Coverage:** 31 total, 0 mapped to tasks, 31 unmapped ⚠️ (mapped in Tasks phase)

---

## Success Criteria

- [ ] `./mvnw verify` termina com sucesso e zero testes ignorados.
- [ ] Verificação do Spring Modulith sem violações.
- [ ] Todos os 31 requisitos com teste ou evidência citada em `validation.md`.
