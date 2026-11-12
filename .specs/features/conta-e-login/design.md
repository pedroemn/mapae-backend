# Contas e login Design

**Spec**: `.specs/features/conta-e-login/spec.md`
**Context**: `.specs/features/conta-e-login/context.md`
**Status**: Approved

---

## Architecture Overview

```
compartilhado/seguranca  (módulo aberto, AD-005)
  ├─ SegurancaConfig       filtro stateless, rotas públicas, resource server JWT HS256, 401 em Problem Details
  ├─ JwtPropriedades       mapae.jwt.segredo / mapae.jwt.validade
  ├─ EmissorDeToken        gera o JWT (sub = id, claim perfil)
  ├─ DadosDeAcesso         @Embeddable: username, email, senha_hash, primeiro_acesso, status, tipo_perfil, criado_em, atualizado_em
  ├─ Autenticador          confere senha e status, emite o token
  ├─ ProvedorDeConta       porta implementada por cada módulo dono de conta
  └─ StatusConta, TipoPerfil, exceções (401, 403, 409, 400)

conta (usuário)                          locais (loja)
  dominio: Usuario, UsuarioRepository      dominio: Local (+ DadosDeAcesso, cep, tipo; posicao opcional)
  aplicacao: ContasDeUsuario               aplicacao: ContasDeLoja
     (cadastro, login, ProvedorDeConta)       (cadastro, login, ProvedorDeConta)
  web: UsuariosController                  web: LojasController
       ContaController (/contas/eu)
```

`ContaController` recebe todos os `ProvedorDeConta` e escolhe pelo perfil do token. Assim `conta` não depende de `locais`, e os módulos continuam desacoplados (AD-005).

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --------- | -------- | ---------- |
| `BancoDeTesteConfig` | `src/test/java/br/com/mapae/BancoDeTesteConfig.java` | PostGIS em container nos testes |
| Padrão `ErrorResponseException` | `locais/aplicacao/ConsultaGeograficaInvalidaException.java` | Exceções que viram Problem Details |
| `OpenApiConfig` | `compartilhado/openapi/OpenApiConfig.java` | Acrescentar o esquema Bearer para o botão "Authorize" |
| Migração com registro em `public.migracao` | `V20261107022500__criar_tabela_migracao.sql` | AD-030 |

### Integration Points

| System | Integration Method |
| ------ | ------------------ |
| Consultas do mapa | `LocalRepository` passa a filtrar `posicao IS NOT NULL` (CONTA-13) |
| Supabase | Flyway aplica a migração ao subir a aplicação com o `.env` |

## Components

- **SegurancaConfig**: só `/api/v1/contas/**` exige token; as demais rotas continuam públicas, e cada task futura protege as suas (AD-037). CSRF desligado, sessão stateless. 401 sem token ou com token inválido, em Problem Details.
- **EmissorDeToken**: `NimbusJwtEncoder` com `SecretKeySpec` HmacSHA256. Claims: `sub` = id, `perfil` = `USUARIO`/`LOJA`, `iat`, `exp`.
- **Autenticador**: recebe a conta encontrada (ou vazia) e a senha. Conta vazia ou senha errada → 401 "Credenciais inválidas". Status diferente de `ATIVO` → 403. Para manter o tempo parecido, confere a senha contra um hash fixo quando a conta não existe.
- **ContasDeUsuario / ContasDeLoja**: normalizam username e email em minúsculas e o CEP para 8 dígitos; checam duplicidade (409 informando o campo); tratam a violação do índice único do banco (corrida) como 409.
- **ContaController**: `GET /api/v1/contas/eu`, `POST /api/v1/contas/eu/primeiro-acesso/conclusao`, `PUT /api/v1/contas/eu/senha`.

## Data Models

### Tabela `usuario` (nova)

| Coluna | Tipo | Regra |
| ------ | ---- | ----- |
| id | BIGINT identity | PK |
| username | VARCHAR(30) | NOT NULL, índice único em `lower(username)` |
| nome | VARCHAR(200) | NOT NULL |
| data_nascimento | DATE | NOT NULL |
| email | VARCHAR(254) | NOT NULL, índice único em `lower(email)` |
| cep | CHAR(8) | NOT NULL, CHECK 8 dígitos |
| genero | VARCHAR(30) | NOT NULL, CHECK na lista |
| bio | VARCHAR(500) | opcional |
| senha_hash | VARCHAR(100) | NOT NULL |
| primeiro_acesso | BOOLEAN | NOT NULL DEFAULT true |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ATIVO', CHECK na lista |
| tipo_perfil | VARCHAR(20) | NOT NULL, CHECK = 'USUARIO' |
| criado_em / atualizado_em | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### Tabela `local` (alterada)

Ganha `username`, `email`, `cep`, `tipo` (VARCHAR(50)), `senha_hash`, `primeiro_acesso`, `status`, `tipo_perfil` (CHECK = 'LOJA'), `criado_em`, `atualizado_em`, todas opcionais para preservar as linhas antigas (CONTA-03). Índices únicos parciais em `lower(username)` e `lower(email)`. `posicao` perde o NOT NULL. Um CHECK garante que, se `senha_hash` existe, os campos de conta também existem.

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| -------------- | -------- | ----------- |
| Bean Validation no corpo | Handler global → 400 Problem Details com `campos` | Front mostra o campo inválido |
| JSON malformado | Problem Details padrão do Spring → 400 | Mensagem genérica |
| Username/email duplicado | `ContaDuplicadaException` → 409 com `campo` | Front indica qual campo |
| Credencial errada | `CredenciaisInvalidasException` → 401 | Mensagem única |
| Conta inativa ou bloqueada | `ContaIndisponivelException` → 403 com `status` | Front explica o bloqueio |
| Token ausente/inválido | `AuthenticationEntryPoint` → 401 Problem Details | Front volta ao login |
| Senha atual errada na troca | 400 | Mensagem |

## Risks & Concerns

| Risk | Mitigation |
| ---- | ---------- |
| Spring Security bloquear rotas já existentes | Teste de rotas públicas (CONTA-26) roda junto com a suíte antiga |
| Loja sem posição quebrar as consultas (lat/lng nulos em `double`) | Filtro `posicao IS NOT NULL` nas três consultas |
| Migração falhar no Supabase com dados existentes | Colunas opcionais em `local`; testes rodam a migração sobre linha pré-existente |

## Tech Decisions

| Decision | Choice | Rationale |
| -------- | ------ | --------- |
| Biblioteca JWT | `spring-boot-starter-oauth2-resource-server` (Nimbus) | Já valida HS256 e evita dependência extra |
| Dados de acesso comuns | `@Embeddable DadosDeAcesso` em `compartilhado` | Mesmas colunas e regras nas duas contas |
| `/contas/eu` sem depender de `locais` | Porta `ProvedorDeConta` | Respeita AD-005 |
