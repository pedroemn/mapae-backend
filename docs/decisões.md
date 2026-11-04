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
| AD-003 | Banco de dados fora da entrega da estrutura | Estrutura do backend | Substituída em parte por AD-007 e AD-008 |
| AD-004 | Autenticação com JWT próprio | Estrutura do backend | Confirmada |
| AD-005 | Camadas por módulo e referências só por ID | Estrutura do backend | Camadas Confirmadas; referências por ID Proposta |
| AD-006 | A spec segue o MVP apresentado ao PO | Estrutura do backend | Confirmada |
| AD-007 | Spring Data JPA com PostgreSQL no Supabase | Ambiente JPA | Confirmada; conexão desligada (AD-016) |
| AD-008 | O Flyway é o dono do schema; o Hibernate só valida | Ambiente JPA | Confirmada; desligado até a conexão (AD-016) |
| AD-009 | Testes com H2 em memória, sem Docker | Ambiente JPA | Adiada (AD-016) |
| AD-010 | Credenciais só em variáveis de ambiente ou `.env` | Ambiente JPA | Adiada (AD-016) |
| AD-011 | Conexão pelo Session Pooler, com no máximo 5 conexões | Ambiente JPA | Adiada (AD-016) |
| AD-012 | `open-in-view` desligado e horários em UTC | Ambiente JPA | Adiada (AD-016) |
| AD-013 | Estrutura zerada: as pastas existem e cada task se encaixa nelas | Estrutura zerada | Confirmada |
| AD-014 | Um único banco, e os 4 integrantes podem alterar tabelas e schemas | Estrutura zerada | Confirmada |
| AD-015 | O frontend só consome a API do backend; só o backend acessa o banco | Estrutura zerada | Confirmada |
| AD-016 | Sem conexão com banco por enquanto; só as dependências ficam | Estrutura zerada | Confirmada |

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

- **Status:** Substituída em parte por AD-007 e AD-008. PostGIS e Redis continuam fora.
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
  - Toda entidade nova precisa de uma migração, senão a aplicação não sobe.
  - Nunca edite uma migração já aplicada: o Flyway compara o checksum e bloqueia a subida. Corrija com uma nova migração.
  - Combine o próximo número antes de criar uma migração, para que duas pessoas não usem a mesma versão.
- **Consequências:**
  - Na primeira subida, o Flyway cria a tabela `flyway_schema_history` no banco, mesmo sem migrações.
  - Se o schema `public` já tiver tabelas e ainda não tiver essa tabela de histórico, o Flyway recusa a migração. Nesse caso a equipe precisa decidir como fazer o baseline.

### AD-009: Testes com H2 em memória, sem Docker

- **Status:** Adiada (AD-016). O arquivo de configuração citado abaixo foi removido; a dependência do H2 ficou.
- **Decisão:** os testes usam H2 em modo PostgreSQL, configurado em `src/test/resources/application.properties`. Esse arquivo substitui o principal durante os testes. O Flyway e o `ddl-auto=validate` também rodam neles.
- **Motivo:** já estava decidido não usar Docker nem Testcontainers, e nenhum teste deve tocar no banco compartilhado.
- **Alternativa descartada:** rodar os testes no Supabase, o que deixaria os dados da equipe expostos aos testes e os testes dependentes de rede.
- **Consequência:** o H2 não reproduz tudo do PostgreSQL. Uma migração com recurso específico, como PostGIS ou extensões, vai quebrar os testes e exigirá outra solução.

### AD-010: Credenciais só em variáveis de ambiente ou `.env`

- **Status:** Adiada (AD-016). O `.env.example` e a leitura do `.env` foram removidos; o `.env` continua no `.gitignore`.
- **Decisão:** o banco é configurado por `MAPAE_DB_URL`, `MAPAE_DB_USUARIO` e `MAPAE_DB_SENHA`. Em desenvolvimento, os valores ficam em um `.env` na raiz do projeto, fora do git; o modelo é o `.env.example`.
- **Motivo:** nenhum segredo entra no repositório.
- **Para configurar:** copie `.env.example` para `.env` e preencha com os dados do Supabase.
- **Consequências:**
  - Variáveis de ambiente do sistema têm prioridade sobre o `.env`.
  - O `.env` é procurado na pasta de onde a aplicação é executada.
  - Sem as variáveis, o erro é `'url' must start with "jdbc"`, que não cita `MAPAE_DB_URL`. Essa mensagem normalmente indica que falta configurar o `.env`.

### AD-011: Conexão pelo Session Pooler, com no máximo 5 conexões

- **Status:** Adiada (AD-016). A configuração foi removida.
- **Decisão:** a URL de exemplo usa o Session Pooler do Supabase (porta 5432), e o Hikari fica com no máximo 5 conexões e no mínimo 1 ociosa.
- **Motivo:** o Session Pooler funciona em redes IPv4 e mantém a mesma conexão durante a sessão, de que os prepared statements e os bloqueios do Flyway dependem. O limite de conexões evita esgotar o pooler quando vários integrantes usam o mesmo banco.
- **Alternativa descartada:** o Transaction Pooler, porque troca de conexão a cada transação e é incompatível com esse comportamento.
- **Consequência:** se o limite do plano for atingido, reduza `maximum-pool-size`. Em produção, esse valor deve ser revisto.

### AD-012: `open-in-view` desligado e horários em UTC

- **Status:** Adiada (AD-016). A configuração foi removida.
- **Decisão:** `spring.jpa.open-in-view=false` e `hibernate.jdbc.time_zone=UTC`.
- **Motivo:** com `open-in-view` ligado, consultas podem ser disparadas durante a serialização da resposta, sem que ninguém perceba. UTC combina com o uso de `Instant` no domínio e evita variação de horário entre máquinas.
- **Consequência:** dados com carregamento lazy precisam ser buscados dentro da transação; fora dela, o acesso lança `LazyInitializationException`.

### Pendências do review

O review desta task apontou os itens abaixo. Todos devem ser retomados na task que conectar o banco (AD-016). O achado sobre o Flyway alterar o banco compartilhado ao subir a aplicação foi aceito pela equipe e saiu da lista (ver AD-014), e o achado sobre o teste do Flyway saiu porque o teste foi removido.

| Prioridade | Pendência |
| ---------- | --------- |
| Média | As tabelas ficam no schema `public`, exposto pela API de dados do Supabase. Quem obtiver a chave `anon` do projeto pode acessar os dados sem passar pelo JWT. Como nem o frontend nem o backend usam essa API (AD-015), desative-a nas configurações do projeto antes de criar a primeira tabela. |
| Média | Com 4 pessoas, o limite de 5 conexões por aplicação soma até 20, o que pode superar o limite do plano do Supabase (a confirmar). |
| Média | Um `application.properties` em `src/test/resources` substitui o principal, e os testes deixam de ler a configuração real. Ao recriar a configuração de teste, use `src/test/resources/config/`, só com o que muda. |
| Média | Com o H2, os testes usam o dialeto dele, então passar neles não garante que o schema valide no PostgreSQL. |
| Baixa | O `.env` é lido como `.properties`, então senhas com `\`, aspas ou `${` são alteradas. |
| Baixa | Variável ausente gera um erro confuso, que não cita o nome dela. |
| Baixa | `sslmode=require` criptografa a conexão, mas não verifica o certificado do servidor. |
| Baixa | `spring-boot-starter-data-jpa-test` ainda não é usado. |

---

## Task: Estrutura zerada

**Data:** 2026-11-04
**Situação:** implementada no working tree, aguardando commit. `./mvnw clean verify` passa.

### AD-013: As pastas existem e cada task se encaixa nelas

- **Status:** Confirmada, incluindo a divisão das pastas.
- **Decisão:** o projeto começa com a estrutura modelada e vazia. Nenhuma funcionalidade é adiantada: cada task traz só o que precisa e o coloca no lugar já previsto, como aconteceu com o JPA.
- **Motivo:** o projeto será construído aos poucos, e o design anterior descrevia um sistema já pronto.
- **Estrutura:**

```
src/main/java/br/com/mapae
├── MapaeApplication.java
├── conta/            dominio/  aplicacao/  web/
├── locais/           dominio/  aplicacao/  web/
├── amizades/         dominio/  aplicacao/  web/
├── feed/             dominio/  aplicacao/  web/
├── checkin/          dominio/  aplicacao/  web/
└── compartilhado/    dominio/  erro/  seguranca/
src/main/resources/db/migration     migrações do Flyway
src/test/java/br/com/mapae          uma pasta por módulo
```

- **O que vai em cada pasta:**
  - `dominio/`: entidades, regras de negócio e repositórios.
  - `aplicacao/`: casos de uso.
  - `web/`: controllers.
  - `compartilhado/`: código usado por mais de um módulo.
- **Como uma task se encaixa:**
  - Coloque o código no módulo e na camada correspondentes.
  - Apague o `.gitkeep` quando a pasta receber o primeiro arquivo. Ele só existe porque o git não guarda pastas vazias.
  - Se a task criar tabela, adicione a migração junto.
  - Registre as decisões da task neste arquivo.
- **Consequências:**
  - JWT, tratamento de erros, classes de domínio e endpoints da spec `estrutura-backend` viram tasks futuras.
  - A primeira task que usar `compartilhado` por outro módulo precisa criar nele um `package-info.java` com `@ApplicationModule(type = OPEN)`, senão a verificação do Modulith acusa violação.
  - O código saiu de `com.example`: a classe principal agora é `br.com.mapae.MapaeApplication`, e o `groupId` é `br.com.mapae`.

### AD-014: Um único banco, e os 4 integrantes podem alterar tabelas e schemas

- **Status:** Confirmada
- **Decisão:** a equipe usa um único projeto privado no Supabase, com acesso restrito aos 4 integrantes. Quem mexer no código pode alterar tabelas e schemas, e o Flyway continua migrando ao subir a aplicação.
- **Motivo:** o projeto está sendo construído agora e os 4 integrantes têm permissão para mudar o banco.
- **Alternativa descartada:** um schema por integrante, que isolaria as mudanças, mas adicionaria configuração sem necessidade neste momento.
- **Consequências:**
  - Uma migração aplicada vale para todos, mesmo que a branch dela ainda não tenha passado por review.
  - As regras da AD-008 continuam valendo: combine o número da próxima migração e nunca edite uma já aplicada. Quem quebrar essas regras pode impedir a aplicação dos outros de subir.
  - O acesso restrito vale para o painel do Supabase. A API de dados é um ponto separado, tratado nas pendências do review.

### AD-015: O frontend só consome a API do backend; só o backend acessa o banco

- **Status:** Confirmada
- **Decisão:** o fluxo é frontend → API do backend → PostgreSQL no Supabase. O frontend não se conecta ao Supabase.
- **Motivo:** login, permissões e regras de negócio ficam em um único lugar, o backend.
- **Consequências:**
  - O Supabase é usado só como banco. A API de dados dele não é necessária e deve ser desativada.
  - A chave `anon` e as credenciais do banco não devem ir para o frontend.
  - Sem acesso direto do frontend, as tabelas não precisam de regras de RLS para ele.

### AD-016: Sem conexão com banco por enquanto; só as dependências ficam

- **Status:** Confirmada
- **Decisão:** nenhuma parte do projeto conecta em banco por enquanto, nem os testes. As dependências de JPA, Flyway, PostgreSQL e H2 continuam no `pom.xml`, e a conexão será feita em uma task própria.
- **Motivo:** a conexão é uma etapa separada, e a estrutura zerada não deve depender dela.
- **Como funciona:** o `application.properties` exclui as autoconfigurações de datasource, Hibernate, repositórios JPA e Flyway. Sem essa exclusão, a aplicação falha ao subir, porque não encontra um banco.
- **O que foi removido:** as propriedades de conexão, a leitura do `.env`, o `.env.example`, a configuração de teste com H2 e o `AmbienteJpaTests`.
- **Na task que conectar o banco:**
  - Remova `spring.autoconfigure.exclude` do `application.properties`.
  - Retome as AD-009 a AD-012 e as pendências do review.
  - Desative a API de dados do Supabase antes de criar a primeira tabela (AD-015).
  - Teste a conexão real com o Supabase, que nunca foi verificada.
- **Consequência:** enquanto a exclusão existir, entidades, repositórios JPA e migrações não funcionam. Tasks que precisem de persistência dependem da task de conexão.
