# Contas de usuário e de loja, e login Specification

## Problem Statement

O Mapaê ainda não tem contas: a tabela `local` só guarda nome e posição, e não existe tabela de usuários. Sem conta não há login, e sem login o front não consegue identificar quem usa o app nem liberar as funções que dependem de usuário autenticado (confirmação de ida, feed, amizades). Esta task modela as duas contas no banco e entrega o cadastro e o login de usuário e de loja.

## Goals

- [ ] As tabelas de usuário e de loja existem no Supabase, criadas por migração Flyway e testadas antes no PostGIS em container.
- [ ] O front cadastra e autentica usuário e loja por rotas próprias, recebendo um JWT (AD-004).
- [ ] O front sabe, pelo login e pelo perfil, se a conta está em primeiro acesso, o status dela e o tipo de perfil.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Login social (Google, Apple, GitHub) | Fica para entrega futura (AD-006) |
| Recuperação de senha por email e verificação de email | Exige envio de email, task própria |
| Refresh token e logout no servidor | AD-004: o token vale até expirar |
| Converter CEP em posição (geocoding) | Serviço externo; a posição da loja vem na task de cadastro de locais |
| Alterar status de conta (bloquear, inativar) | Não há perfil administrador nesta task |
| Editar perfil (nome, bio, CEP, foto) | Task própria |
| Rate limit no login | Sem infraestrutura de limite nesta entrega |
| Proteger rotas já existentes (`/api/v1/locais/area` e `/proximos`) | Continuam públicas; cada task futura protege as suas |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Onde ficam os dados de conta da loja | Na tabela `local`, com `posicao` passando a ser opcional | Escolha do usuário em 2026-11-11 | y |
| Sexo e gênero | Só o usuário tem `genero`, pedido no cadastro; a loja não tem | Escolha do usuário em 2026-11-11 | y |
| Significado de `firstacess` | `primeiro_acesso` começa `true`; o login devolve o valor e uma rota marca o onboarding como concluído | Escolha do usuário em 2026-11-11 | y |
| Identificador do login | Um campo aceita email ou username; rotas separadas para usuário e loja | Escolha do usuário em 2026-11-11 | y |
| "senha" e "password hash" na lista | Só `senha_hash` (BCrypt) é gravado; a senha em texto nunca vai para o banco | Guardar senha em texto é falha de segurança; AD-004 já define BCrypt | n |
| "data de criação" no cadastro | É o `criado_em`, preenchido pelo servidor, não pedido ao cliente | Data de criação vinda do cliente seria manipulável | n |
| Valores de `status` | `ATIVO`, `INATIVO`, `BLOQUEADO`; toda conta nasce `ATIVO` | Cobre os casos comuns sem antecipar regras de moderação | n |
| Valores de tipo de perfil | `USUARIO` para usuário e `LOJA` para loja, definidos pelo servidor | O cliente não escolhe o próprio perfil (pedido do usuário) | n |
| Coluna `tipo` da loja | Texto livre opcional (até 50 caracteres) com o tipo do estabelecimento, como "bar" | A tabela de categorias do MVP é task própria | n |
| Valores de `genero` | `FEMININO`, `MASCULINO`, `NAO_BINARIO`, `OUTRO`, `PREFIRO_NAO_INFORMAR` | Lista fechada facilita filtros e respeita quem não quer informar | n |
| Formato do username | 3 a 30 caracteres, só `a-z`, `0-9`, `.` e `_`, guardado em minúsculas | Funciona como @ no app e evita duplicatas por maiúsculas | n |
| Unicidade de username e email | Únicos dentro de cada tabela, sem diferenciar maiúsculas | Cada tipo de conta tem a sua rota de login | n |
| Formato do CEP | Aceita `00000-000` ou `00000000` e guarda só os 8 dígitos | Formato brasileiro; sem consulta de existência | n |
| Tamanho da senha | De 8 caracteres a 72 bytes em UTF-8 (letra acentuada conta 2 bytes); no login, senha acima de 72 bytes é senha errada | O BCrypt recusa mais de 72 bytes; contar caracteres deixava passar senha acentuada e dava 500 (achado do Verificador) | n |
| Data de nascimento | Obrigatória, no formato `AAAA-MM-DD` e anterior ao dia atual; sem idade mínima. Outro formato dá 400 com `campos.dataNascimento` | A equipe não definiu idade mínima; ISO é o formato do JSON da API | n |
| Validade do token | 24 horas, configurável por variável de ambiente | Sem refresh token, uma validade curta demais derruba o usuário | n |
| Segredo do JWT | Vem de `MAPAE_JWT_SEGREDO`; sem ele a aplicação não sobe | AD-010: credenciais só no ambiente ou no `.env` | n |
| Loja sem posição | Não aparece em `/api/v1/locais/area` nem em `/proximos` até ter posição | A posição vem da task de cadastro de locais | n |
| Aplicar as migrações no Supabase | Só depois dos testes passarem no container, subindo a aplicação com o `.env` | Ordem pedida pelo usuário | y |
| Commits | O agente não faz commit; o usuário revisa e commita | AD-001 | y |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Tabelas de conta no banco ⭐ MVP

**User Story**: Como equipe, quero as tabelas de usuário e de loja no Supabase para que o cadastro e o login gravem dados reais.

**Why P1**: É a base do login e da primeira etapa pedida.

**Acceptance Criteria**:

1. WHEN as migrações rodam em um banco vazio THEN the system SHALL criar a tabela `usuario` com `id`, `username`, `nome`, `data_nascimento`, `email`, `cep`, `genero`, `bio`, `senha_hash`, `primeiro_acesso`, `status`, `tipo_perfil`, `criado_em` e `atualizado_em` <!-- CONTA-01 -->
2. WHEN as migrações rodam THEN the system SHALL acrescentar à tabela `local` as colunas `username`, `email`, `cep`, `tipo`, `senha_hash`, `primeiro_acesso`, `status`, `tipo_perfil`, `criado_em` e `atualizado_em`, e tornar `posicao` opcional <!-- CONTA-02 -->
3. IF já existirem linhas em `local` antes da migração THEN the system SHALL migrar sem erro, mantendo essas linhas como estão e sem conta de login <!-- CONTA-03 -->
4. The system SHALL impedir no banco dois usernames ou dois emails iguais, sem diferenciar maiúsculas, dentro da mesma tabela <!-- CONTA-04 -->
5. The system SHALL manter RLS ligado e sem políticas em `usuario` (AD-024) <!-- CONTA-05 -->
6. The system SHALL subir com `ddl-auto=validate` aceitando o mapeamento das entidades `Usuario` e `Local` <!-- CONTA-06 -->

**Independent Test**: Os testes com PostGIS em container sobem a aplicação, e as consultas ao catálogo do banco mostram as colunas, as restrições e o RLS.

---

### P1: Cadastro de usuário ⭐ MVP

**User Story**: Como pessoa nova no app, quero criar minha conta informando username, nome, data de nascimento, gênero, email, CEP e senha, para usar o Mapaê.

**Why P1**: Sem cadastro não há quem faça login.

**Acceptance Criteria**:

1. WHEN o cliente envia `POST /api/v1/usuarios` com todos os campos obrigatórios válidos THEN the system SHALL responder 201 com `id`, `username`, `nome`, `email`, `primeiroAcesso: true`, `status: "ATIVO"`, `tipoPerfil: "USUARIO"` e `criadoEm` <!-- CONTA-07 -->
2. The system SHALL gravar a senha só como hash BCrypt e nunca a devolver em resposta alguma, nem em forma de hash <!-- CONTA-08 -->
3. IF o corpo traz `tipoPerfil`, `status`, `primeiroAcesso` ou `criadoEm` THEN the system SHALL ignorar esses campos e usar os valores do servidor <!-- CONTA-09 -->
4. IF um campo obrigatório falta ou está fora do formato definido nas premissas THEN the system SHALL responder 400 em Problem Details, listando o campo inválido <!-- CONTA-10 -->
5. IF o username ou o email já existe em `usuario`, sem diferenciar maiúsculas THEN the system SHALL responder 409 indicando qual dos dois está em uso <!-- CONTA-11 -->

**Independent Test**: `POST /api/v1/usuarios` pelo Swagger cria a conta e a linha aparece em `usuario` com `senha_hash` começando por `$2`.

---

### P1: Cadastro de loja ⭐ MVP

**User Story**: Como dono de estabelecimento, quero criar a conta da loja informando username, nome, email, CEP e senha, para gerenciar meu local no Mapaê.

**Why P1**: A loja tem login próprio, pedido na task.

**Acceptance Criteria**:

1. WHEN o cliente envia `POST /api/v1/lojas` com username, nome, email, CEP e senha válidos THEN the system SHALL responder 201 com `id`, `username`, `nome`, `email`, `tipo`, `primeiroAcesso: true`, `status: "ATIVO"`, `tipoPerfil: "LOJA"` e `criadoEm` <!-- CONTA-12 -->
2. WHEN uma loja é cadastrada THEN the system SHALL gravá-la em `local` sem posição, de modo que ela não apareça em `/api/v1/locais/area` nem em `/api/v1/locais/proximos` <!-- CONTA-13 -->
3. IF um campo obrigatório falta ou está fora do formato THEN the system SHALL responder 400 em Problem Details, listando o campo inválido <!-- CONTA-14 -->
4. IF o username ou o email já existe em `local`, sem diferenciar maiúsculas THEN the system SHALL responder 409 indicando qual dos dois está em uso <!-- CONTA-15 -->

**Independent Test**: `POST /api/v1/lojas` pelo Swagger cria a loja, e as rotas do mapa continuam sem devolvê-la.

---

### P1: Login de usuário e de loja ⭐ MVP

**User Story**: Como usuário ou loja cadastrada, quero entrar com email ou username e senha, para receber um token e usar o app.

**Why P1**: É o objetivo final da task.

**Acceptance Criteria**:

1. WHEN o cliente envia `POST /api/v1/auth/usuarios/login` com `login` (email ou username, sem diferenciar maiúsculas) e `senha` corretos de um usuário `ATIVO` THEN the system SHALL responder 200 com `token`, `tipoToken: "Bearer"`, `expiraEm`, `primeiroAcesso`, `status` e `tipoPerfil: "USUARIO"` <!-- CONTA-16 -->
2. WHEN o cliente envia `POST /api/v1/auth/lojas/login` com `login` e `senha` corretos de uma loja `ATIVO` THEN the system SHALL responder 200 com os mesmos campos e `tipoPerfil: "LOJA"` <!-- CONTA-17 -->
3. IF o login não existe ou a senha está errada THEN the system SHALL responder 401 com a mesma mensagem nos dois casos <!-- CONTA-18 -->
4. IF a senha está certa mas a conta está `INATIVO` ou `BLOQUEADO` THEN the system SHALL responder 403 informando o status, sem devolver token <!-- CONTA-19 -->
5. The system SHALL emitir JWT HS256 com o `id` da conta no `sub`, o `tipoPerfil` em uma claim e a validade configurada <!-- CONTA-20 -->
6. IF um usuário tenta entrar pela rota de loja, ou uma loja pela rota de usuário THEN the system SHALL responder 401, como em credencial errada <!-- CONTA-21 -->

**Independent Test**: Cadastrar pelo Swagger, fazer login, colar o token no botão "Authorize" e chamar `GET /api/v1/contas/eu`.

---

### P1: Conta autenticada e primeiro acesso ⭐ MVP

**User Story**: Como usuário ou loja logada, quero ver os dados da minha conta e marcar meu onboarding como concluído, para que o app não o mostre de novo.

**Why P1**: É a rota que o front usa para `primeiroAcesso`, `status`, `criadoEm` e `atualizadoEm`, pedida na task.

**Acceptance Criteria**:

1. WHEN o cliente chama `GET /api/v1/contas/eu` com token válido THEN the system SHALL responder 200 com os dados da conta do token, incluindo `primeiroAcesso`, `status`, `tipoPerfil`, `criadoEm` e `atualizadoEm`, e sem senha nem hash <!-- CONTA-22 -->
2. WHEN o cliente chama `POST /api/v1/contas/eu/primeiro-acesso/conclusao` com token válido THEN the system SHALL gravar `primeiro_acesso = false`, atualizar `atualizado_em` e responder 204 <!-- CONTA-23 -->
3. IF a conclusão do primeiro acesso é chamada de novo THEN the system SHALL responder 204 sem alterar `atualizado_em` <!-- CONTA-24 -->
4. IF a rota recebe token ausente, inválido ou expirado THEN the system SHALL responder 401 em Problem Details <!-- CONTA-25 -->
5. The system SHALL manter públicas as rotas de cadastro, de login, do Swagger e as consultas de `/api/v1/locais` <!-- CONTA-26 -->

**Independent Test**: Com o token do login, `GET /api/v1/contas/eu` mostra `primeiroAcesso: true`; depois da conclusão, mostra `false`.

---

### P2: Troca de senha

**User Story**: Como usuário ou loja logada, quero trocar minha senha informando a atual, para manter a conta segura.

**Why P2**: Completa o ciclo da senha, mas o login funciona sem ela.

**Acceptance Criteria**:

1. WHEN o cliente envia `PUT /api/v1/contas/eu/senha` com token válido, `senhaAtual` correta e `novaSenha` válida THEN the system SHALL gravar o novo hash BCrypt, atualizar `atualizado_em` e responder 204 <!-- CONTA-27 -->
2. IF `senhaAtual` está errada THEN the system SHALL responder 400 sem alterar o hash <!-- CONTA-28 -->

**Independent Test**: Trocar a senha e fazer login com a nova; a antiga passa a dar 401.

---

## Edge Cases

- IF o mesmo email existe em `usuario` e em `local` THEN the system SHALL aceitar, já que cada um entra pela sua rota
- IF dois cadastros com o mesmo username chegam ao mesmo tempo THEN the system SHALL responder 409 a quem perder, usando a restrição do banco
- IF o JSON do corpo está malformado THEN the system SHALL responder 400 em Problem Details
- WHEN o email ou o username chega com maiúsculas THEN the system SHALL guardá-lo em minúsculas

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| CONTA-01 | P1: Tabelas | Execute | Verified |
| CONTA-02 | P1: Tabelas | Execute | Verified |
| CONTA-03 | P1: Tabelas | Execute | Verified |
| CONTA-04 | P1: Tabelas | Execute | Verified |
| CONTA-05 | P1: Tabelas | Execute | Verified |
| CONTA-06 | P1: Tabelas | Execute | Verified |
| CONTA-07 | P1: Cadastro de usuário | Execute | Verified |
| CONTA-08 | P1: Cadastro de usuário | Execute | Verified |
| CONTA-09 | P1: Cadastro de usuário | Execute | Verified |
| CONTA-10 | P1: Cadastro de usuário | Execute | Verified |
| CONTA-11 | P1: Cadastro de usuário | Execute | Verified |
| CONTA-12 | P1: Cadastro de loja | Execute | Verified |
| CONTA-13 | P1: Cadastro de loja | Execute | Verified |
| CONTA-14 | P1: Cadastro de loja | Execute | Verified |
| CONTA-15 | P1: Cadastro de loja | Execute | Verified |
| CONTA-16 | P1: Login | Execute | Verified |
| CONTA-17 | P1: Login | Execute | Verified |
| CONTA-18 | P1: Login | Execute | Verified |
| CONTA-19 | P1: Login | Execute | Verified |
| CONTA-20 | P1: Login | Execute | Verified |
| CONTA-21 | P1: Login | Execute | Verified |
| CONTA-22 | P1: Conta autenticada | Execute | Verified |
| CONTA-23 | P1: Conta autenticada | Execute | Verified |
| CONTA-24 | P1: Conta autenticada | Execute | Verified |
| CONTA-25 | P1: Conta autenticada | Execute | Verified |
| CONTA-26 | P1: Conta autenticada | Execute | Verified |
| CONTA-27 | P2: Troca de senha | Execute | Verified |
| CONTA-28 | P2: Troca de senha | Execute | Verified |

**Coverage:** 28 total, 28 mapped to tasks (T1-T8), 0 unmapped

---

## Success Criteria

- [ ] Um usuário e uma loja se cadastram, fazem login e consultam a própria conta pelo Swagger, contra o Supabase.
- [ ] Toda a suíte de testes passa com PostGIS em container antes de as migrações irem para o Supabase.
- [ ] Nenhuma senha em texto aparece no banco nem em resposta da API.
