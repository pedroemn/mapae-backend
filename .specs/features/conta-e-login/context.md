# Contas e login Context

**Gathered:** 2026-11-11
**Spec:** `.specs/features/conta-e-login/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Tabelas de conta de usuário e de loja, cadastro, login com JWT, consulta da própria conta e conclusão do primeiro acesso. Ordem pedida: tabelas, testes em container, migrações no Supabase, login.

---

## Implementation Decisions

### Conta da loja

- Os dados de conta ficam na própria tabela `local`, e `posicao` passa a ser opcional.

### Sexo e gênero

- Só o usuário tem `genero`, pedido no cadastro. A loja não tem sexo nem gênero.

### Primeiro acesso

- `primeiro_acesso` começa `true`. O login e a consulta da conta devolvem o valor, e uma rota marca o onboarding como concluído.

### Login

- Um campo aceita email ou username. Usuário e loja têm rotas de login separadas.

### Agent's Discretion

Nenhuma área foi deixada explicitamente a critério do agente.

### Declined / Undiscussed Gray Areas → Assumptions

Status, tipo de perfil, coluna `tipo`, valores de gênero, formatos, validade do token e segredo do JWT ficaram como premissas na spec.

---

## Specific References

- O tipo de perfil é definido pelo servidor: quem cria a primeira conta entra como usuário comum, e a loja entra como loja.
- O cadastro pede username, nome, email, CEP e senha (usuário também data de nascimento e gênero).

---

## Deferred Ideas

None - discussion stayed within feature scope
