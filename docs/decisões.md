# Decisões do projeto

Registro das decisões tomadas em cada task do `mapae-backend`, com o motivo de cada uma, para que qualquer integrante da equipe entenda por que o projeto é como é.

## Como usar este arquivo

- Cada task tem a sua seção, da mais antiga para a mais recente.
- Toda decisão recebe um ID único (`AD-001`, `AD-002`...).
- Este é o registro oficial para a equipe. A pasta `.specs/` citada aqui é local e não é versionada.
- Uma decisão antiga não é apagada. Se ela mudar, marque como **Substituída** e aponte para a nova.
- Status possíveis:
  - **Confirmada**: aprovada pela equipe.
  - **Proposta**: valor adotado que ainda espera aprovação.
  - **Substituída**: não vale mais.
  - **Adiada**: a configuração foi removida e a decisão será retomada quando o banco for conectado.

## Resumo

| ID | Decisão | Task | Status |
| -- | ------- | ---- | ------ |
| AD-001 | O agente de IA não faz commit nem push | Estrutura do backend | Confirmada |
| AD-002 | Java 25, Spring Boot 4.1, Maven e Spring Modulith; pacote `br.com.mapae` | Estrutura do backend | Confirmada |
| AD-003 | Banco de dados fora da entrega da estrutura | Estrutura do backend | Substituída em parte por AD-007, AD-008 e AD-017 |
| AD-004 | Autenticação com JWT próprio | Estrutura do backend | Confirmada |
| AD-005 | Camadas por módulo e referências só por ID | Estrutura do backend | Camadas Confirmadas; referências por ID Proposta |
| AD-006 | A spec segue o MVP apresentado ao PO | Estrutura do backend | Confirmada |
| AD-007 | Spring Data JPA com PostgreSQL no Supabase | Ambiente JPA | Confirmada |
| AD-008 | O Flyway é o dono do schema; o Hibernate só valida | Ambiente JPA | Confirmada |
| AD-009 | Testes com H2 em memória, sem Docker | Ambiente JPA | Substituída por AD-019 |
| AD-010 | Credenciais só em variáveis de ambiente ou `.env` | Ambiente JPA | Confirmada; retomada em AD-020 |
| AD-011 | Conexão pelo Session Pooler, com no máximo 5 conexões | Ambiente JPA | Confirmada; retomada em AD-020 |
| AD-012 | `open-in-view` desligado e horários em UTC | Ambiente JPA | Confirmada; retomada em AD-020 |
| AD-013 | Estrutura zerada: as pastas existem e cada task se encaixa nelas | Estrutura zerada | Confirmada |
| AD-014 | Um único banco, e os 4 integrantes podem alterar tabelas e schemas | Estrutura zerada | Confirmada |
| AD-015 | O frontend só consome a API do backend; só o backend acessa o banco | Estrutura zerada | Confirmada |
| AD-016 | Sem conexão com banco por enquanto; só as dependências ficam | Estrutura zerada | Substituída por AD-020 |
| AD-017 | PostGIS com hibernate-spatial, no schema `extensions` | Consultas geográficas | Confirmada |
| AD-018 | Busca livre e rotas ficam com serviços externos chamados pelo app, sem proxy | Consultas geográficas | Confirmada; provedor a escolher |
| AD-019 | Testes com PostGIS real em Docker (Testcontainers) | Consultas geográficas | Confirmada |
| AD-020 | Aplicação conectada ao banco, com o local como padrão | Consultas geográficas | Conexão Confirmada; padrão local Proposta |
| AD-021 | Tabela `local` mínima | Consultas geográficas | Proposta |
| AD-022 | Limites da confirmação de ida sem valor no código; sem eles, 503 | Consultas geográficas | Substituída por AD-026 |
| AD-023 | Limites técnicos das consultas | Consultas geográficas | Substituída por AD-028 |
| AD-024 | RLS em toda tabela do `public` e histórico do Flyway no schema `flyway` | Consultas geográficas | Proposta |
| AD-025 | `confirmacaoida` usa `locais` por uma interface pública | Consultas geográficas | Substituída por AD-026 |
| AD-026 | A confirmação de ida não valida localização | Consultas geográficas | Confirmada |
| AD-027 | "Check-in" passa a se chamar "confirmação de ida" | Consultas geográficas | Confirmada |
| AD-028 | Consultas sem limite técnico e com coordenadas opcionais | Consultas geográficas | Confirmada |
| AD-029 | Busca livre no mapa pela API; a tabela `local` guarda só locais de clientes | Consultas geográficas | Confirmada |
| AD-030 | Migrações com nome e timestamp; o histórico do Flyway é a tabela de migrações | Versionamento e documentação | Proposta |
| AD-031 | Documentação da API com Swagger (springdoc-openapi) | Versionamento e documentação | Proposta |
| AD-032 | Versionamento da API pelo caminho: `/api/v1` | Versionamento e documentação | Proposta |
| AD-033 | Conta da loja na própria tabela `local`, com posição opcional | Contas e login | Confirmada |
| AD-034 | Senha só como hash BCrypt; servidor define perfil, status e primeiro acesso | Contas e login | Proposta |
| AD-035 | Login por email ou username, com rotas separadas para usuário e loja | Contas e login | Confirmada |
| AD-036 | JWT HS256 com Nimbus, validade de 24 h e segredo obrigatório | Contas e login | Proposta |
| AD-037 | Só `/api/v1/contas/**` exige token; as outras rotas continuam públicas | Contas e login | Proposta |
| AD-038 | `/api/v1/contas/eu` atende usuário e loja pela porta `ProvedorDeConta` | Contas e login | Proposta |

---

## Task: Estrutura do backend

**Data:** 2026-10-31
**Situação:** não será implementada como uma entrega única (ver AD-013). As decisões abaixo continuam valendo como orientação para as tasks futuras.
**Documentos:** `.specs/features/estrutura-backend/spec.md` e `design.md`

### AD-001: O agente de IA não faz commit nem push

- **Status:** Confirmada
- **Decisão:** o agente só escreve no working tree. O fluxo é: o agente escreve, a mudança passa por review e o usuário faz o commit.
- **Motivo:** o controle do que entra no histórico fica com a equipe.
- **Consequência:** substitui a regra de "um commit atômico por tarefa" do processo. Cada tarefa termina com os testes passando e as mudanças fora do histórico.

### AD-002: Stack do projeto

- **Status:** Confirmada
- **Decisão:** Java 25, Spring Boot 4.1, Maven e Spring Modulith, com pacote base `br.com.mapae`.
- **Motivo:** o Modulith verifica as fronteiras entre os módulos, o que ajuda quatro pessoas a trabalhar em áreas diferentes sem acoplar o código.
- **Consequência:** todos da equipe precisam de um JDK 25 ou mais novo para compilar o projeto.

### AD-003: Banco de dados fora da entrega da estrutura

- **Status:** Substituída em parte por AD-007, AD-008 e AD-017. Redis continua fora.
- **Decisão original:** JPA, PostgreSQL/PostGIS, Flyway e Redis ficariam fora; o domínio seria Java puro e os repositórios, apenas interfaces.
- **Consequência em aberto:** o domínio em Java puro e as interfaces de repositório descritos no design precisam ser conciliados com o JPA quando a estrutura for implementada.

### AD-004: Autenticação com JWT próprio

- **Status:** Confirmada
- **Decisão:** Spring Security stateless, JWT assinado com HS256 e senhas com BCrypt.
- **Motivo:** não exige infraestrutura de chaves nem provedor externo.
- **Consequência:** o logout não invalida o token, que continua válido até expirar. Invalidar exigiria uma lista persistente de tokens, fora desta entrega.

### AD-005: Camadas por módulo e referências só por ID

- **Status:** Confirmada quanto às camadas. As referências só por ID e o `compartilhado` aberto continuam como Proposta.
- **Decisão:** cada módulo se divide em `dominio/`, `aplicacao/` e `web/`, como detalhado na AD-013. Módulos se referenciam apenas por ID, e `compartilhado` é um módulo aberto (`Type.OPEN`).
- **Motivo:** mantém os módulos desacoplados e combina com as chaves estrangeiras do diagrama de classes.
- **Consequência para o JPA:** as entidades guardam o ID de outro módulo (`Long usuarioId`) em vez de usar `@ManyToOne` entre módulos.

### AD-006: A spec segue o MVP apresentado ao PO

- **Status:** Confirmada (spec aprovada)
- **Decisão:** vale a amizade com solicitação. "Seguir perfis", login social, avaliações, favoritos e URLs pré-assinadas, descritos em `docs/visao-do-produto.md`, ficam para entregas futuras.
- **Motivo:** o MVP apresentado ao PO é o contrato das Etapas 3 e 4.

---

## Task: Ambiente JPA

**Data:** 2026-11-04
**Situação:** as dependências ficaram, mas a configuração de conexão foi removida (AD-016). As decisões abaixo orientam a task que conectar o banco.
**Verificado antes da remoção:** os testes passaram com H2, uma migração temporária foi aplicada pelo Flyway e a aplicação tentou conectar pelo driver do PostgreSQL usando um `.env` de teste.
**Nunca verificado:** conexão com o Supabase real.

### AD-007: Spring Data JPA com PostgreSQL no Supabase

- **Status:** Confirmada
- **Decisão:** usar `spring-boot-starter-data-jpa` (Hibernate) e o driver do PostgreSQL, com o banco hospedado no Supabase.
- **Motivo:** o JPA foi a ferramenta de persistência escolhida para o projeto, e o Supabase já era o banco previsto.
- **Consequência:** a aplicação não sobe sem as credenciais do banco. Os testes não dependem delas.

### AD-008: O Flyway é o dono do schema; o Hibernate só valida

- **Status:** Confirmada
- **Decisão:** toda mudança de tabela é uma migração SQL do Flyway, e o Hibernate usa `ddl-auto=validate`.
- **Motivo:** o banco é compartilhado pela equipe. Com o schema versionado, todos têm as mesmas tabelas e cada mudança passa por review.
- **Alternativa descartada:** `ddl-auto=update`, em que qualquer integrante poderia alterar o banco compartilhado só por subir a aplicação, sem histórico da mudança.
- **Regras para a equipe:**
  - As migrações ficam em `src/main/resources/db/migration`, com o nome `V<número>__<descricao>.sql` (exemplo: `V1__criar_usuario.sql`).
  - Nas migrações, escreva o schema das tabelas: `public.usuario` (AD-024).
  - Toda entidade nova precisa de uma migração, senão a aplicação não sobe.
  - Nunca edite uma migração já aplicada: o Flyway compara o checksum e bloqueia a subida. Corrija com uma nova migração.
  - Combine o próximo número antes de criar uma migração, para que duas pessoas não usem a mesma versão.
- **Consequências:**
  - Na primeira subida, o Flyway cria a tabela `flyway_schema_history` no schema `flyway` (AD-024), mesmo sem migrações.
  - Se o schema `flyway` já tiver tabelas e ainda não tiver essa tabela de histórico, o Flyway recusa a migração. Nesse caso a equipe precisa decidir como fazer o baseline.

### AD-009: Testes com H2 em memória, sem Docker

- **Status:** Substituída por AD-019. O H2 saiu do `pom.xml`, porque a consequência prevista abaixo se confirmou: ele não suporta PostGIS.
- **Decisão:** os testes usam H2 em modo PostgreSQL, configurado em `src/test/resources/application.properties`. Esse arquivo substitui o principal durante os testes. O Flyway e o `ddl-auto=validate` também rodam neles.
- **Motivo:** já estava decidido não usar Docker nem Testcontainers, e nenhum teste deve tocar no banco compartilhado.
- **Alternativa descartada:** rodar os testes no Supabase, o que deixaria os dados da equipe expostos aos testes e os testes dependentes de rede.
- **Consequência:** o H2 não reproduz tudo do PostgreSQL. Uma migração com recurso específico, como PostGIS ou extensões, vai quebrar os testes e exigirá outra solução.

### AD-010: Credenciais só em variáveis de ambiente ou `.env`

- **Status:** Confirmada; retomada em AD-020. O `.env.example` e a leitura do `.env` voltaram.
- **Decisão:** o banco é configurado por `MAPAE_DB_URL`, `MAPAE_DB_USUARIO` e `MAPAE_DB_SENHA`. Em desenvolvimento, os valores ficam em um `.env` na raiz do projeto, fora do git; o modelo é o `.env.example`.
- **Motivo:** nenhum segredo entra no repositório.
- **Para configurar:** copie `.env.example` para `.env` e preencha com os dados do Supabase.
- **Consequências:**
  - Variáveis de ambiente do sistema têm prioridade sobre o `.env`.
  - O `.env` é procurado na pasta de onde a aplicação é executada.
  - Sem as variáveis, a aplicação usa o banco local do `compose.yaml` (AD-020).

### AD-011: Conexão pelo Session Pooler, com no máximo 5 conexões

- **Status:** Confirmada; retomada em AD-020.
- **Decisão:** a URL de exemplo usa o Session Pooler do Supabase (porta 5432), e o Hikari fica com no máximo 5 conexões e no mínimo 1 ociosa.
- **Motivo:** o Session Pooler funciona em redes IPv4 e mantém a mesma conexão durante a sessão, de que os prepared statements e os bloqueios do Flyway dependem. O limite de conexões evita esgotar o pooler quando vários integrantes usam o mesmo banco.
- **Alternativa descartada:** o Transaction Pooler, porque troca de conexão a cada transação e é incompatível com esse comportamento.
- **Consequência:** se o limite do plano for atingido, reduza `maximum-pool-size`. Em produção, esse valor deve ser revisto.

### AD-012: `open-in-view` desligado e horários em UTC

- **Status:** Confirmada; retomada em AD-020.
- **Decisão:** `spring.jpa.open-in-view=false` e `hibernate.jdbc.time_zone=UTC`.
- **Motivo:** com `open-in-view` ligado, consultas podem ser disparadas durante a serialização da resposta, sem que ninguém perceba. UTC combina com o uso de `Instant` no domínio e evita variação de horário entre máquinas.
- **Consequência:** dados com carregamento lazy precisam ser buscados dentro da transação; fora dela, o acesso lança `LazyInitializationException`.

### Pendências do review

O review desta task apontou os itens abaixo, atualizados na task de consultas geográficas. O achado sobre o Flyway alterar o banco compartilhado ao subir a aplicação foi aceito pela equipe e saiu da lista (ver AD-014), e o achado sobre o teste do Flyway saiu porque o teste foi removido.

| Prioridade | Pendência |
| ---------- | --------- |
| Média | As tabelas ficam no schema `public`, exposto pela API de dados do Supabase. O RLS já bloqueia a chave `anon` (AD-024), mas a API ainda precisa ser desativada no painel antes da primeira conexão, porque não é usada (AD-015). |
| Média | Com 4 pessoas, o limite de 5 conexões por aplicação soma até 20, o que pode superar o limite do plano do Supabase (a confirmar). |
| Baixa | O `.env` é lido como `.properties`, então senhas com `\`, aspas ou `${` são alteradas. |
| Baixa | `sslmode=require` criptografa a conexão, mas não verifica o certificado do servidor. |
| Baixa | `spring-boot-starter-data-jpa-test` ainda não é usado. |

---

## Task: Estrutura zerada

**Data:** 2026-11-04
**Situação:** concluída e commitada (`1936201`, `fdbfb81`). Os ajustes finais da revisão (AD-028, AD-029) ficam no working tree até o próximo commit.
**Verificado:** `./mvnw clean verify` com PostGIS 3.5 em Docker (34 testes). Teste de mutação: 10 defeitos injetados (raio, ordenação com e sem raio, ordem da listagem, par latitude/longitude, raio sem coordenadas, `truncado`, `oeste > leste`, `sul > norte`, estouro do `limite`) e os 10 foram pegos. A aplicação subiu contra o Supabase real em 2026-11-05: as migrações V1 e V2 foram aplicadas, o PostGIS 3.3.7 está no schema `extensions`, o histórico está no schema `flyway`, `public.local` tem RLS ligado e os dois endpoints respondem 200. Antes, a aplicação também rodou na imagem `supabase/postgres:17.6.1.066` e no banco local do `compose.yaml`.
**Revisão independente:** feita em 2026-11-05 e achou um defeito, corrigido: `limite=2147483647` na área estourava o inteiro e dava erro 500. Foi adicionado um teste para isso, junto com o de `raioMetros` inválido.
**Pendente:** nenhuma verificação. Fica para as próximas tasks: cadastro dos locais de clientes (AD-029) e paginação, se a lista crescer (AD-028).
**Contrato com o app:** `docs/contrato-geografico.md`.

### AD-017: PostGIS com hibernate-spatial, no schema `extensions`

- **Status:** Confirmada
- **Decisão:** a posição é `geography(Point, 4326)`, com índices GiST. As consultas usam funções do PostGIS, e o mapeamento usa `hibernate-spatial` com JTS. A extensão fica no schema `extensions`.
- **Motivo:** o PostGIS calcula distâncias na forma real da Terra e usa índice, o que dispensa fórmulas escritas à mão. No `extensions`, a tabela `spatial_ref_sys` não fica exposta no `public`.
- **Alternativa descartada:** calcular a distância em Java, que exigiria ler todas as linhas.
- **Consequências:**
  - A conexão define `search_path` como `public, extensions`, pela propriedade `connection-init-sql`. Nas migrações, escreva o schema: `extensions.geography`.
  - A tabela tem dois índices: um em `geography`, para proximidade, e um em `geometry`, para a caixa da área. A caixa em `geometry` é exata em latitude e longitude; em `geography`, as bordas seriam curvas.
  - O Supabase usa PostGIS 3.3.7, e os testes usam 3.5. Use só funções disponíveis nas duas versões.

### AD-018: Busca livre e rotas ficam com serviços externos chamados pelo app

- **Status:** Confirmada. O provedor ainda será escolhido pela equipe do app.
- **Decisão:** a busca livre no mapa não usa o backend. O app chama o provedor de geocoding, recebe as coordenadas e consulta os locais da região em `/api/locais/area`. Para rotas, o app abre o serviço de mapas do aparelho.
- **Motivo:** a busca não é sobre locais cadastrados, e a task proíbe proxy quando o cliente pode usar o serviço direto. Um proxy traria custo, latência e mais um ponto de falha, sem ganho.
- **Ajuste na AD-015:** ela continua valendo para os dados do Mapaê. Serviços externos de mapas são a exceção.
- **Consequências:**
  - O backend não guarda chave de provedor. A chave fica no app, restrita ao pacote Android e ao bundle iOS, e com cota no painel do provedor.
  - Se um provedor exigir chave secreta, esta decisão precisa ser revista.

### AD-019: Testes com PostGIS real em Docker (Testcontainers)

- **Status:** Confirmada. Substitui a AD-009.
- **Decisão:** os testes de banco sobem o PostGIS pelo Testcontainers. O H2 saiu do projeto.
- **Motivo:** o H2 não suporta PostGIS, como a AD-009 previa. O usuário autorizou o Docker nesta task.
- **Consequências:**
  - **`./mvnw verify` agora exige o Docker ligado**, em toda máquina da equipe e no CI.
  - A imagem `postgis/postgis` já vem com o PostGIS no `public`. O script `docker/banco/20_igualar_supabase.sql` o remove, para que a migração o instale como faz no Supabase.
  - Os valores de teste ficam em `src/test/resources/config/application.properties`, que só sobrescreve o que muda.

### AD-020: Aplicação conectada ao banco, com o local como padrão

- **Status:** Conexão Confirmada. O padrão local é Proposta. Substitui a AD-016.
- **Decisão:** `spring.autoconfigure.exclude` saiu, e as AD-010, AD-011 e AD-012 voltaram a valer. O `compose.yaml` sobe um PostGIS na porta 5433, e ele é o destino quando `MAPAE_DB_URL` não está definida. O schema vem só das migrações.
- **Motivo:** as consultas precisam de banco, e a senha do Supabase será configurada depois. Com o padrão local, todos conseguem rodar o projeto sem credenciais e sem risco de tocar no banco compartilhado.
- **Para rodar localmente:** `docker compose up -d --wait` e depois `./mvnw spring-boot:run`. O `--wait` é necessário porque o banco demora a aceitar conexões na primeira subida.
- **Para conectar ao Supabase:**
  1. Desative a API de dados no painel do projeto `mapae`. É recomendado, mas não bloqueia: o RLS já protege as tabelas (AD-024).
  2. Copie `.env.example` para `.env` e preencha a senha do banco. O host e o usuário já estão no exemplo.
  3. Suba a aplicação. O Flyway instala o PostGIS e cria as tabelas sozinho, sem uso do SQL Editor.
- **Consequências:**
  - A aplicação não sobe sem banco acessível.
  - O projeto ganhou `spring-boot-starter-webmvc` e `spring-boot-starter-validation`, porque ainda não tinha API web.
  - A senha `mapae` no repositório só vale para o banco local, preso a `127.0.0.1`.

### AD-021: Tabela `local` mínima

- **Status:** Proposta
- **Decisão:** a tabela tem só `id`, `nome` e `posicao`. A tabela `confirmacao_ida` não foi criada.
- **Motivo:** segue a AD-013, que manda não adiantar funcionalidades. Endereço, horário, categoria e fotos pertencem à task de cadastro, que os adiciona com uma nova migração. O registro de confirmação de ida depende de usuário autenticado.
- **Consequência:** ainda não existe endpoint para cadastrar locais, então as consultas só devolvem dados depois que essa task existir.

### AD-022: Limites da confirmação de ida sem valor no código

- **Status:** Substituída por AD-026. Não vale mais, e fica aqui como histórico.
- **Decisão:** raio, precisão e idade máximos vêm de variáveis de ambiente, sem padrão. Enquanto faltar qualquer um, a aplicação sobe e `POST /api/confirmacoes-ida/validacao` responde 503.
- **Regras:**
  - O app envia latitude, longitude, precisão e horário da captura.
  - As regras são avaliadas nesta ordem: precisão, prazo e raio. Valores iguais ao máximo são aceitos.
  - A precisão não amplia o raio. Somá-la favoreceria quem tem o pior GPS.
  - O prazo vale nos dois sentidos, então um horário no futuro também é recusado.
- **Motivo:** a task pede para não presumir um raio aprovado. Recusar a validação é mais seguro que aprovar com um número inventado.
- **Consequências:**
  - O servidor confia na posição enviada, então GPS falso não é detectado.
  - O endpoint não registra confirmação de ida nem guarda a posição. Ele continua aberto até o JWT existir (AD-004).

### AD-023: Limites técnicos das consultas

- **Status:** Substituída por AD-028. Os limites saíram do código.
- **Decisão:** padrão de 50 resultados, máximo de 200 e raio máximo de 50 km. Quando a área passa do limite, a resposta traz `truncado: true`. Valores acima do máximo recebem 400, em vez de serem reduzidos em silêncio.
- **Motivo:** protege o banco e o app de respostas enormes e ajuda a cumprir a RNF01 (3 segundos). 50 km cobre Aracaju e a região metropolitana.
- **Consequência:** os valores ficam no `application.properties` e podem mudar sem alterar o código.

### AD-024: RLS em toda tabela do `public` e histórico do Flyway no schema `flyway`

- **Status:** Proposta
- **Decisão:** toda tabela do `public` tem RLS ligado e sem políticas, e um teste falha se alguma ficar sem RLS. O histórico do Flyway fica no schema `flyway` (`spring.flyway.default-schema`), sem acesso para `anon` e `authenticated`.
- **Motivo:** desativar a API de dados é um passo manual, que pode ser esquecido. Sem essa proteção, a chave `anon` poderia ler e alterar os dados e o histórico das migrações. O problema do histórico apareceu na verificação com a imagem do Supabase.
- **Alternativa descartada:** ligar o RLS no histórico por uma migração. Ela trava a aplicação ao subir, sem erro nenhum: durante as migrações, outra conexão do Flyway mantém um lock de leitura na tabela, e o `ALTER TABLE` espera para sempre. **Nenhuma migração deve alterar a tabela de histórico.**
- **Consequências:**
  - **Toda migração que criar tabela precisa ter `ENABLE ROW LEVEL SECURITY` e escrever `public.` no nome da tabela.** Durante as migrações, o schema padrão é o `flyway`. Sem o `public.`, a tabela vai para o lugar errado e a aplicação não sobe, porque o Hibernate não a encontra.
  - O backend continua com acesso porque o usuário da conexão é dono das tabelas. Se esse usuário mudar, o acesso precisa ser revisto.

### AD-025: `confirmacaoida` usa `locais` por uma interface pública

- **Status:** Substituída por AD-026. A interface saiu do código, porque o `confirmacaoida` não precisa mais da distância.
- **Decisão:** `br.com.mapae.locais.ConsultaDistanciaLocal` fica na raiz do módulo e devolve só a distância. O `confirmacaoida` não vê a entidade nem o repositório. A regra pura fica em `confirmacaoida.dominio.RegrasConfirmacaoIda`.
- **Motivo:** no Spring Modulith, só a raiz do pacote é pública para outros módulos. `ModularidadeTests` agora verifica essas fronteiras, o que não acontecia antes.
- **Consequência:** a verificação do Modulith passa a fazer parte do `./mvnw verify`.

### AD-026: A confirmação de ida não valida localização

- **Status:** Confirmada. Substitui a AD-022 e a AD-025.
- **Decisão:** a confirmação de ida avisa que o usuário vai ao local e pode ser feita de qualquer lugar, sem raio, precisão ou idade da posição. `POST /api/confirmacoes-ida/validacao`, as regras e as variáveis `MAPAE_CONFIRMACAO_IDA_*` saíram do projeto.
- **Motivo:** mudança de regra de negócio definida pelo usuário em 2026-11-05.
- **Ajustes em outros documentos:** em `docs/visao-do-produto.md`, o requisito de confirmação de ida deixou de citar a localização, e `ConfirmacaoIda` perdeu o campo `posicao`.
- **Consequências:**
  - O card BACK-006 pedia a validação no servidor. Essa parte deixou de valer, e o card no Trello não foi alterado.
  - **Uma confirmação de ida não comprova que a pessoa está no local.** A visão do produto usa confirmações de ida para indicar no Gerômetro o que está movimentado naquele momento, então a equipe precisa decidir como o Gerômetro vai contá-los.
  - Registrar a confirmação de ida continua sendo uma task futura, que depende do login (AD-004). Até lá, as pastas do módulo `confirmacaoida` ficam vazias (AD-013).
  - `ModularidadeTests` continua no `./mvnw verify`.

### AD-027: "Check-in" passa a se chamar "confirmação de ida"

- **Status:** Confirmada.
- **Decisão:** o termo "check-in" sai do produto e do código. O módulo é `confirmacaoida`, a entidade é `ConfirmacaoIda` e a tabela futura é `confirmacao_ida`. As ADs antigas foram reescritas com o novo termo, sem mudar o que decidiram.
- **Motivo:** definido pelo usuário em 2026-11-05. "Check-in" sugere presença no local, e a confirmação vale de qualquer lugar (AD-026).
- **Consequência:** o card BACK-006 e outros cards do Trello ainda usam o termo antigo e não foram alterados.

### AD-028: Consultas sem limite técnico e com coordenadas opcionais

- **Status:** Confirmada. Substitui a AD-023.
- **Decisão:** o raio máximo, o limite padrão e o limite máximo saíram. `GET /api/locais/proximos` aceita `latitude`, `longitude`, `raioMetros` e `limite` como opcionais: sem coordenadas lista todos os locais por `id`; com coordenadas ordena por distância; o raio só filtra quando informado. `latitude` e `longitude` vêm juntas, e `raioMetros` exige as duas, senão 400. `LimitesConsulta` e as propriedades `mapae.locais.*` foram removidas.
- **Motivo:** definido pelo usuário em 2026-11-05: ele pode pesquisar de qualquer lugar, sem depender de um ponto, de um raio ou de um teto.
- **Consequências:**
  - Sem `limite`, a resposta traz todos os locais. Com a base crescendo, isso pesa na RNF01 (3 segundos), e a equipe precisa decidir quando paginar.
  - `distanciaMetros` some da resposta quando não há coordenadas.

### AD-029: Busca livre no mapa pela API; a tabela `local` guarda só locais de clientes

- **Status:** Confirmada. Esclarece as AD-018 e AD-021.
- **Decisão:** o usuário procura qualquer lugar pelo mapa do app, que usa uma API externa de mapas e geocoding, sem passar pelo backend. A tabela `local` do Mapaê não é um cadastro de todos os lugares. Ela guarda os locais de clientes, que terão promoções, anúncios e investimento no sistema, e que o backend devolve em `/api/locais/area` e `/api/locais/proximos`.
- **Motivo:** esclarecido pelo usuário em 2026-11-05.
- **Consequências:**
  - A base de `local` tende a ser pequena, o que torna aceitável devolver todos os locais sem paginar (AD-028). Se a lista crescer, a paginação entra.
  - O cadastro de locais (endereço, horário, categoria, fotos, promoções e anúncios) é uma task futura, e a AD-021 continua valendo até lá.
  - O app deve mostrar os locais do Mapaê sobre o mapa, destacados dos demais lugares que a API de mapas exibe.

---

## Task: Versionamento e documentação da API

**Data:** 2026-11-07
**Situação:** implementada no working tree, sem commit.
**Verificado:** `./mvnw clean verify` com PostGIS em Docker (38 testes, incluindo o contrato OpenAPI, a rota sem versão devolvendo 404 e o padrão de nome das migrações).
**Supabase:** a migração `V20261107022500` foi aplicada em 2026-11-07 pela subida da aplicação; `public.migracao` tem 3 linhas e RLS ligado. `/swagger-ui/index.html`, `/v3/api-docs` e `/api/v1/locais/proximos` responderam 200.

### AD-030: Migrações com nome e timestamp; o histórico do Flyway é a tabela de migrações

- **Status:** Proposta
- **Decisão:** toda migração nova se chama `V<yyyyMMddHHmmss>__<nome_em_snake_case>.sql` (exemplo: `V20261107143000__criar_usuario.sql`) e termina com um `INSERT INTO public.migracao (nome) VALUES ('<nome do arquivo sem .sql>');`. A tabela `public.migracao` (`id`, `nome` único, `aplicada_em timestamptz`) foi criada pela `V20261107022500__criar_tabela_migracao` e já traz V1 e V2. O controle de execução continua sendo do Flyway, em `flyway.flyway_schema_history`.
- **Motivo:** pedido da equipe para ter no banco, em `public`, um registro legível de cada migração com nome e horário. O timestamp no nome também resolve o problema da AD-008 de duas pessoas escolherem o mesmo número.
- **Alternativa descartada:** renomear a tabela do Flyway (`spring.flyway.table`). O Supabase já tem V1 e V2 aplicadas, e o Flyway trataria o schema como sem histórico e recusaria a migração.
- **Risco aceito:** `public.migracao` duplica informação do Flyway e só é preenchida porque cada migração se registra. `NomesDeMigracaoTests` falha se uma migração com timestamp não fizer o `INSERT`, mas uma execução fora do Flyway não preencheria a tabela.
- **Consequências:**
  - V1 e V2 mantêm o nome: renomear mudaria a versão e o checksum de migrações já aplicadas. Como `20261107143000` é maior que 2, a ordem se mantém.
  - `NomesDeMigracaoTests` falha se um arquivo novo fugir do padrão. A regra "combine o próximo número" da AD-008 não vale mais.
  - Se uma migração com timestamp menor for mesclada depois de outra maior já ter sido aplicada, o Flyway acusa migração fora de ordem. A saída é renomear a migração com um timestamp novo antes de aplicá-la.
  - Para ver o histórico: `SELECT version, description, installed_on, success FROM flyway.flyway_schema_history ORDER BY installed_rank;`

### AD-031: Documentação da API com Swagger (springdoc-openapi)

- **Status:** Proposta
- **Decisão:** `springdoc-openapi-starter-webmvc-ui` 3.2.0. A interface fica em `/swagger-ui.html` e o contrato OpenAPI em `/v3/api-docs`. Os metadados ficam em `compartilhado/openapi/OpenApiConfig`, e os controllers descrevem cada rota com `@Tag` e `@Operation`.
- **Motivo:** o contrato sai do próprio código, então não se desatualiza em relação às rotas e serve ao frontend.
- **Consequência:** a documentação está aberta, sem autenticação. Quando entrar o Spring Security (AD-004), decidir se ela fica pública e se ela é desligada em produção (`springdoc.api-docs.enabled=false`).

### AD-032: Versionamento da API pelo caminho: `/api/v1`

- **Status:** Proposta
- **Decisão:** as rotas passam a ser `/api/v1/...`. As rotas antigas, sem versão, deixaram de existir.
- **Motivo:** o caminho é a forma mais simples de o app e o Swagger enxergarem a versão. Não havia cliente em produção, então não houve custo de migração.
- **Alternativa descartada:** o versionamento nativo do Spring (`spring.mvc.apiversion`), que esconde a versão do caminho e é menos direto de documentar.
- **Consequências:**
  - Uma mudança incompatível cria `/api/v2` e mantém a v1 pelo período combinado com o app.
  - O app e o `docs/contrato-geografico.md` usam os novos caminhos.

---

## Task: Contas e login

**Data:** 2026-11-11
**Situação:** implementada no working tree, sem commit (AD-001). Spec, design e tasks em `.specs/features/conta-e-login/`.
**Verificado:** `./mvnw verify` com PostGIS em Docker: 153 testes, 115 deles novos (38 antes da task).
**Supabase:** a migração `V20261111103000__criar_contas` foi aplicada em 2026-11-11. A tabela `usuario` existe com RLS ligado, `local.posicao` aceita nulo e os quatro índices únicos foram criados. A aplicação que estava rodando com devtools reiniciou sozinha quando os testes compilaram e aplicou a migração durante a execução dos testes. O arquivo aplicado é o mesmo que passou nos testes.

### AD-033: Conta da loja na própria tabela `local`, com posição opcional

- **Status:** Confirmada (escolha do usuário em 2026-11-11)
- **Decisão:** as colunas de conta (`username`, `email`, `cep`, `tipo`, `senha_hash`, `primeiro_acesso`, `status`, `tipo_perfil`, `criado_em`, `atualizado_em`) entram na tabela `local`. `posicao` passa a aceitar nulo.
- **Motivo:** a loja é o próprio local de cliente (AD-029); uma tabela à parte duplicaria nome e identidade.
- **Consequências:**
  - As colunas de conta são opcionais, para preservar os locais criados antes delas. Um CHECK exige que, se o local tem senha, tenha todos os dados de conta, e que um local sem conta tenha posição.
  - A loja recém-cadastrada não aparece em `/api/v1/locais/area` nem em `/api/v1/locais/proximos` até ter posição. As consultas filtram `posicao IS NOT NULL`.
  - Substitui a AD-021 quanto às colunas de `local`.

### AD-034: Senha só como hash BCrypt; servidor define perfil, status e primeiro acesso

- **Status:** Proposta
- **Decisão:** a lista da task tinha "senha" e "password hash"; só `senha_hash` (BCrypt) é gravado, e nenhuma resposta traz a senha ou o hash. O cadastro de usuário gera `tipo_perfil = USUARIO` e o de loja gera `LOJA`. Toda conta nasce `ATIVO` e com `primeiro_acesso = true`. `criado_em` e `atualizado_em` são do servidor. Campos com esses nomes no corpo da requisição são ignorados.
- **Valores:** `status` em `ATIVO`, `INATIVO` e `BLOQUEADO`. `genero` (só do usuário) em `FEMININO`, `MASCULINO`, `NAO_BINARIO`, `OUTRO` e `PREFIRO_NAO_INFORMAR`. `tipo` da loja é texto livre opcional.
- **Formatos:** username de 3 a 30 caracteres (`a-z`, `0-9`, `.`, `_`); email e username guardados em minúsculas e únicos por tabela, sem diferenciar maiúsculas; CEP com 8 dígitos (aceito com hífen); senha de 8 caracteres a 72 bytes em UTF-8 (limite do BCrypt; letra acentuada conta 2 bytes); data de nascimento em `AAAA-MM-DD` e anterior a hoje.
- **Motivo:** guardar senha em texto é falha de segurança; o perfil vindo do cliente permitiria a um usuário se cadastrar como loja.

### AD-035: Login por email ou username, com rotas separadas para usuário e loja

- **Status:** Confirmada (escolha do usuário em 2026-11-11)
- **Decisão:** `POST /api/v1/auth/usuarios/login` e `POST /api/v1/auth/lojas/login` recebem `login` (email ou username, sem diferenciar maiúsculas) e `senha`.
- **Regras:** login inexistente e senha errada dão o mesmo 401. Senha certa em conta `INATIVO` ou `BLOQUEADO` dá 403, com `statusConta`, sem token. Um usuário pela rota da loja, ou o contrário, dá 401.
- **Consequência:** o mesmo email pode existir como usuário e como loja, porque cada um entra pela sua rota.

### AD-036: JWT HS256 com Nimbus, validade de 24 h e segredo obrigatório

- **Status:** Proposta. Implementa a AD-004.
- **Decisão:** `spring-boot-starter-security` e `spring-boot-starter-oauth2-resource-server`, que já trazem o Nimbus para emitir e validar o HS256. O token leva o id da conta no `sub` e o perfil na claim `perfil`. O segredo vem de `MAPAE_JWT_SEGREDO`, com pelo menos 32 caracteres; sem ele a aplicação não sobe. A validade vem de `MAPAE_JWT_VALIDADE` (padrão 24h).
- **Consequências:**
  - Cada integrante precisa de `MAPAE_JWT_SEGREDO` no seu `.env` (ver `.env.example`). Os testes usam um segredo fixo em `src/test/resources/config/application.properties`.
  - Trocar o segredo invalida todos os tokens emitidos.

### AD-037: Só `/api/v1/contas/**` exige token; as outras rotas continuam públicas

- **Status:** Proposta
- **Decisão:** o filtro do Spring Security protege só as rotas da conta autenticada. Cadastro, login, Swagger e as consultas de `/api/v1/locais` continuam públicos. Token ausente, inválido ou expirado nas rotas protegidas dá 401 em Problem Details.
- **Motivo:** proteger tudo por padrão mudaria o comportamento das rotas existentes (por exemplo, uma rota inexistente passaria de 404 para 401) e quebraria testes já aprovados.
- **Consequência:** cada task futura precisa incluir as suas rotas no filtro; esquecer deixa a rota aberta. A alternativa, negar tudo por padrão, fica para a equipe decidir. Também responde à pendência da AD-031: o Swagger continua público.

### AD-038: `/api/v1/contas/eu` atende usuário e loja pela porta `ProvedorDeConta`

- **Status:** Proposta
- **Decisão:** `compartilhado/seguranca` define a interface `ProvedorDeConta`, que `ContasDeUsuario` (módulo `conta`) e `ContasDeLoja` (módulo `locais`) implementam. O `ContaController` escolhe o provedor pelo perfil do token. `compartilhado` passou a ser um módulo aberto (`Type.OPEN`), como previa a AD-005.
- **Motivo:** `conta` não depende de `locais`, e o Spring Modulith continua validando as fronteiras.
- **Rotas:** `GET /api/v1/contas/eu`; `POST /api/v1/contas/eu/primeiro-acesso/conclusao` (204, idempotente, sem mexer em `atualizado_em` na segunda vez); `PUT /api/v1/contas/eu/senha` (exige a senha atual; errada dá 400).


### AD-039: CORS liberado só para as origens do front, configuráveis por ambiente

- **Status:** Proposta
- **Decisão:** o `SegurancaConfig` liga o CORS do Spring Security para `/api/**`. As origens vêm de `MAPAE_CORS_ORIGENS` (lista separada por vírgula); sem ela, valem `http://localhost:3000` e `http://localhost:5173`, as portas mais comuns de um front web rodando local. Métodos liberados: GET, POST, PUT, PATCH, DELETE e OPTIONS. Cabeçalhos liberados: `Authorization` e `Content-Type`. O navegador guarda a resposta do pré-voo por 1 hora.
- **Motivo:** um front web em outro endereço (porta, domínio ou protocolo) só consegue ler as respostas da API se ela autorizar a origem; sem isso, o navegador bloqueia as chamadas. App mobile nativo, Postman e o Swagger não passam por essa regra.
- **Sem credenciais de navegador:** o token vai no cabeçalho `Authorization` (AD-004), e não em cookie, então `allowCredentials` fica desligado.
- **Alternativa descartada:** liberar qualquer origem (`*`). Funciona, mas deixa qualquer site chamar a API pelo navegador de quem está logado; listar as origens custa pouco.
- **Consequências:**
  - Em produção, `MAPAE_CORS_ORIGENS` precisa ter o endereço do front publicado, sem barra no final (ver `.env.example`). Se o front rodar em outra porta local, basta definir a variável.
  - O pré-voo (`OPTIONS`) das rotas protegidas passa sem token, e o 401 dessas rotas leva o cabeçalho CORS, para o front conseguir ler o erro e voltar ao login.
  - Origem fora da lista recebe 403 no pré-voo.
  - `CorsTests` cobre o pré-voo do login, o pré-voo de rota protegida, o 401 com cabeçalho CORS e a origem recusada.
