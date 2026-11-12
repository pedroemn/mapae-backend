# STATE

## Decisions

- **AD-001** (2026-10-31): O agente nunca faz commit nem push; o usuário commita. Sobrepõe a regra de "um commit atômico por tarefa" do processo — cada tarefa termina com o gate verde e as mudanças ficam no working tree.
- **AD-002** (2026-10-31; Java 25 confirmado em 2026-11-04): Stack Java 25 + Spring Boot 4.1 + Maven + Spring Modulith; pacote base `br.com.mapae`.
- **AD-003** (2026-10-31): Camada de banco (JPA, PostgreSQL/PostGIS, Flyway, Redis) fica fora da entrega `estrutura-backend`; o domínio é Java puro e os repositórios são só interfaces.
- **AD-004** (2026-10-31): Autenticação por JWT próprio (Spring Security stateless, HS256, BCrypt para senha).

- **AD-005** (2026-10-31): Organização em camadas leves por módulo (`dominio/`, `web/`, `aplicacao/` quando houver casos de uso). Módulos se referenciam só por ID. O módulo `compartilhado` é aberto (`Type.OPEN`).
- **AD-006** (2026-10-31): A spec segue o MVP apresentado ao PO (amizade com solicitação). "Seguir perfis", login social, avaliações, favoritos e URLs pré-assinadas descritos em `docs/visao-do-produto.md` são do produto completo e ficam para entregas futuras.
- **AD-007** (2026-11-04): Substitui AD-003 quanto a JPA e PostgreSQL: a persistência usa Spring Data JPA com PostgreSQL no Supabase. PostGIS e Redis continuam fora.
- **AD-008** (2026-11-04): Substitui AD-003 quanto ao Flyway: o Flyway é o dono do schema e o Hibernate usa `ddl-auto=validate`.
- **AD-009** (2026-11-04): Testes com H2 em memória (modo PostgreSQL), sem Docker/Testcontainers.
- **AD-010** (2026-11-04): Credenciais só em `MAPAE_DB_URL`, `MAPAE_DB_USUARIO` e `MAPAE_DB_SENHA`, pelo ambiente ou por um `.env` não versionado.
- **AD-011** (2026-11-04): Conexão pelo Session Pooler do Supabase, com no máximo 5 conexões no Hikari.
- **AD-012** (2026-11-04): `open-in-view` desligado e horários em UTC.
- **AD-013** (2026-11-04): Estrutura zerada: as pastas dos módulos existem vazias e cada task se encaixa nelas. O restante da spec `estrutura-backend` vira tasks futuras.
- **AD-014** (2026-11-04): Um único projeto privado no Supabase; os 4 integrantes podem alterar tabelas e schemas, e o Flyway migra ao subir a aplicação. Sem schema por integrante.
- **AD-015** (2026-11-04): O frontend só consome a API do backend, e só o backend acessa o banco. A API de dados do Supabase não é usada e deve ser desativada.
- **AD-016** (2026-11-04): Sem conexão com banco por enquanto, nem nos testes. As dependências ficam, com as autoconfigurações de banco excluídas; a conexão será uma task própria, que retoma AD-009 a AD-012.
- **AD-017** (2026-11-05): PostGIS com hibernate-spatial e `geography(Point, 4326)`; extensão no schema `extensions`.
- **AD-018** (2026-11-05): Busca livre e rotas ficam com serviços externos chamados pelo app, sem proxy; provedor a escolher.
- **AD-019** (2026-11-05): Substitui AD-009: testes com PostGIS real em Docker (Testcontainers); `verify` exige Docker.
- **AD-020** (2026-11-05): Substitui AD-016: aplicação conectada, retoma AD-010 a AD-012; sem `MAPAE_DB_URL`, usa o banco do `compose.yaml`.
- **AD-021** (2026-11-05, proposta): Tabela `local` mínima (`id`, `nome`, `posicao`); `confirmacao_ida` fica para depois.
- **AD-022** (2026-11-05): Substituída por AD-026. Limites da confirmação de ida sem valor no código; sem eles, a validação respondia 503.
- **AD-023** (2026-11-05): Substituída por AD-028. Limites de 50, 200 e 50 km.
- **AD-028** (2026-11-05): Consultas sem limite técnico; latitude, longitude, raio e limite são opcionais.
- **AD-024** (2026-11-05, proposta): RLS sem políticas em toda tabela do `public`; histórico do Flyway no schema `flyway`. Migração que altera o histórico trava a subida.
- **AD-025** (2026-11-05): Substituída por AD-026. `confirmacaoida` usava `locais` pela interface pública `ConsultaDistanciaLocal`.
- **AD-027** (2026-11-05): "Check-in" passa a se chamar "confirmação de ida"; módulo `confirmacaoida`.
- **AD-026** (2026-11-05): A confirmação de ida é feita de qualquer lugar, sem validação de localização. A equipe precisa decidir como o Gerômetro conta essas confirmações de ida.
- **AD-033** (2026-11-11): Conta da loja na tabela `local`; `posicao` opcional e consultas do mapa filtram `posicao IS NOT NULL`. Substitui AD-021 quanto às colunas.
- **AD-034** (2026-11-11, proposta): Só `senha_hash` BCrypt; servidor define `tipo_perfil`, `status` (ATIVO/INATIVO/BLOQUEADO), `primeiro_acesso` e datas.
- **AD-035** (2026-11-11): Login por email ou username; rotas separadas para usuário e loja; 401 igual para login e senha errados; 403 para conta não ATIVO.
- **AD-036** (2026-11-11, proposta): JWT HS256 via oauth2-resource-server (Nimbus); `sub` = id, claim `perfil`; segredo obrigatório `MAPAE_JWT_SEGREDO`, validade 24h.
- **AD-037** (2026-11-11, proposta): Só `/api/v1/contas/**` exige token; cada task futura protege as suas rotas no `SegurancaConfig`.
- **AD-038** (2026-11-11, proposta): `/contas/eu` usa a porta `ProvedorDeConta` em `compartilhado` (módulo OPEN), sem `conta` depender de `locais`.
- **AD-039** (2026-11-12, proposta): CORS para `/api/**` com origens de `MAPAE_CORS_ORIGENS` (padrão localhost:3000 e 5173); só `Authorization` e `Content-Type`, sem cookies.
- **AD-040** (2026-11-12): Catálogo: `categoria`, `termos TEXT[]` e `ativo` em `local`; `unaccent` em `extensions`; mapa e pesquisa só mostram ativos com posição e, se loja, com status ATIVO.
- **AD-041** (2026-11-12, proposta): Só `ROTAS_PROTEGIDAS` (`/api/v1/contas/**`) leem o token; rotas públicas ignoram token expirado ou inválido.
- **AD-042** (2026-11-12): Paginação `pagina` (0, máx. 21474836) e `tamanho` (20, máx. 100) com `{numero, tamanho, totalItens, totalPaginas}`; opcional na `/area`, sempre na `/pesquisa`.
- **AD-043** (2026-11-12, proposta): Pesquisa por trecho de nome/categoria/termos e categoria por igualdade, sem maiúsculas nem acentos; curingas literais; ordem por nome.
- **AD-044** (2026-11-12): `software.amazon.awssdk:s3` restaurada no `pom.xml` (perdida no merge 45ff2d0); variáveis do R2 documentadas no `.env.example`.
- **AD-045** (2026-11-12): Categorias fixas (`bar`, `restaurante`, `cafe`, `padaria`, `lanchonete`, `balada`, `evento`, `outro`) no enum `Categoria` e no CHECK `ck_local_categoria`; categoria desconhecida dá 400.
- **AD-046** (2026-11-12, proposta): `faixa_preco` 1 a 4; filtro `preco` multivalorado.
- **AD-047** (2026-11-12, proposta): `abre`/`fecha` diários, vira a meia-noite, `abre = fecha` é 24 h; `abertoAgora` no fuso `America/Maceio`, hora enviada como texto.
- **AD-048** (2026-11-12, proposta): Distância na pesquisa com `latitude`/`longitude` e `raioMetros` (1 a 100000); sem origem, sem distância e raio dá 400.
- **AD-049** (2026-11-12, proposta): Busca por palavras em qualquer ordem, sem palavras de ligação, plural simples, `pg_trgm` para erro de digitação, sinônimos de categoria e ordem por relevância.
- **AD-050** (2026-11-12, proposta): `tipo` da loja só aceita categoria da lista e grava o código em `tipo` e `categoria`.

## Referências

- Decisões detalhadas de cada task: `docs/decisões.md`.

- Visão do produto, requisitos, escopo do MVP e cronograma: `docs/visao-do-produto.md`.

## Handoff

- Feature `filtros-de-locais` (BACK-024): implementada na branch `PesquisaLocais`, sem commit (AD-001); 283 testes passando; Verificador PASS na iteração 3 (2026-11-12), relatório em `.specs/features/filtros-de-locais/validation.md`. Migração `V20261112150000__filtros_de_locais` ainda não aplicada no Supabase. Front do cadastro de loja precisa oferecer a lista de categorias (AD-050).

- Feature `locais-mapa-e-pesquisa` (BACK-022 e BACK-023): implementada na branch `PesquisaLocais`, sem commit (AD-001); 219 testes passando; Verificador PASS na iteração 2 (2026-11-12). Premissa a confirmar com a equipe: loja bloqueada ou inativa fica fora do catálogo. Migração `V20261112060000__catalogo_de_locais` ainda não aplicada no Supabase. Relatório do Verificador em `.specs/features/locais-mapa-e-pesquisa/validation.md`.
- A `main` não compilava (dependência do R2 perdida no merge); corrigido nesta branch (AD-044). Para subir a aplicação, o `.env` precisa das variáveis `CLOUDFLARE_*`.
- Feature `conta-e-login`: já está na main (merge `5c04b71`), com Verificador PASS. A migração `V20261111103000__criar_contas` está aplicada no Supabase: não alterar o arquivo (o checksum está gravado).
- `MAPAE_JWT_SEGREDO` precisa estar no `.env` de cada integrante.
- Próximas tasks: cadastro administrativo de locais (categoria, termos, `ativo` e posição; AD-029, AD-040) e registro da confirmação de ida (AD-026), que já pode usar o token.
