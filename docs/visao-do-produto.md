# Mapaê - Visão do Produto

Documento de referência do produto. Reúne a visão geral, os problemas, o objetivo, as telas, as funcionalidades e os requisitos do Mapaê. As decisões técnicas do backend ficam em `docs/decisões.md` e as especificações de cada entrega em `.specs/features/`, pasta local que não é versionada.

## Visão geral

O projeto consiste no desenvolvimento de uma plataforma de descoberta urbana inicialmente focada em Aracaju, Sergipe. A aplicação tem como propósito conectar pessoas, lugares e experiências, facilitando a descoberta do que fazer na cidade em um único ambiente.

A plataforma centralizará informações sobre restaurantes, bares, eventos, shows, praias, pontos turísticos e outros locais, combinando um mapa interativo com informações dos estabelecimentos, movimentação em tempo real, roteiros, conteúdos em vídeo e recursos de interação entre usuários.

Um dos principais diferenciais será o Gerômetro, um indicador de movimentação dos locais baseado na atividade da comunidade dentro da plataforma, como localização e check-ins. A proposta é permitir que o usuário não apenas descubra quais lugares existem, mas também tenha uma percepção de quais estão mais movimentados e "gerando" naquele momento.

A plataforma também terá uma experiência de descoberta baseada em roteiros e vídeos curtos. O usuário poderá encontrar sugestões de lugares e atividades, assistir a conteúdos sobre a cidade e acessar diretamente as informações do local apresentado, reduzindo a necessidade de alternar entre diferentes aplicativos.

Além da descoberta, a plataforma contará com recursos sociais, permitindo que usuários realizem check-ins, publiquem conteúdos, avaliem locais, adicionem amigos e compartilhem experiências.

## Problemas

- Fragmentação das informações: dados sobre lugares, eventos, horários, avaliações e experiências estão espalhados em diferentes plataformas.
- Dificuldade de descoberta: usuários têm dificuldade para encontrar novas opções de lazer, gastronomia, turismo e entretenimento na própria cidade.
- Falta de informação sobre a movimentação: não existe uma forma simples e centralizada de saber quais lugares estão mais movimentados ou em alta naquele momento.
- Distância entre descoberta e experiência: o usuário encontra um lugar em uma plataforma, mas precisa utilizar outras ferramentas para pesquisar informações, decidir se vale a pena ir e chegar até ele.

Quem sente esses problemas: moradores que querem novidades na própria cidade e turistas que não conhecem as opções.

## Objetivo e proposta

O objetivo do projeto é facilitar a descoberta de experiências em Aracaju, permitindo que o usuário encontre, em um único aplicativo, informações sobre lugares, eventos e atividades que possam fazer parte do seu dia.

A proposta é transformar a pergunta "O que tem para fazer em Aracaju hoje?" em uma experiência simples de descoberta e decisão.

Para isso, a plataforma conecta três elementos principais: Pessoas, Lugares e Experiências.

O usuário poderá descobrir um local por meio do mapa, de um roteiro, de um vídeo ou de uma publicação de outro usuário. A partir dessa descoberta, terá acesso às informações necessárias para decidir se deseja conhecer o local, como fotos, avaliações, comentários, horário de funcionamento, preço, localização e rota.

Ao mesmo tempo, a interação da própria comunidade ajudará a tornar a plataforma mais dinâmica. Check-ins, publicações, avaliações e movimentação dos usuários poderão contribuir para identificar lugares que estão em alta e alimentar o Gerômetro, criando uma percepção mais próxima da realidade da cidade naquele momento.

A longo prazo, a proposta é construir uma plataforma de descoberta urbana local, começando por Aracaju e podendo posteriormente ser adaptada para outras cidades que enfrentem problemas semelhantes de fragmentação de informações e descoberta de experiências.

## Público-alvo

| Perfil | Conta | O que faz |
| ------ | ----- | --------- |
| Visitante | Sem conta | Explora o mapa, busca lugares e vê as informações públicas dos locais. |
| Usuário autenticado | Com conta | Além de explorar, cria perfil, adiciona amigos, faz check-in, publica e interage com o conteúdo de outros usuários. |

## Telas

- Home (mapa)
- Configurações
- Perfil do usuário (público)
- Perfil da empresa
  - Sobre
  - Avaliações
  - História
- Favoritos
- Amigos
- Feed
- Login
- Cadastro
- Pesquisa
  - De amigo
  - De local

## Funcionalidades

- Like em estabelecimento
- Cadastro de estabelecimento
- Pesquisa de amigo
- Pesquisa de estabelecimento
- Post com foto ou vídeo
- Post junto com amigo

## Requisitos do produto

Requisitos de comportamento do sistema, levantados para o produto completo. O recorte do semestre está na seção Escopo do MVP.

| Requisito | Descrição |
| --------- | --------- |
| Busca por proximidade | O sistema deve permitir buscar locais em um raio definido a partir da localização atual do usuário. |
| Filtro por categoria e status | O sistema deve permitir filtrar locais por categoria (restaurantes, bares, praias) e status (aberto, fechado). |
| Gestão de locais | O sistema deve permitir o gerenciamento de estabelecimentos. |
| Detalhamento do local | O sistema deve exibir informações sobre locais e estabelecimentos. |
| Check-in | O sistema deve permitir que o usuário realize check-in com base na localização. |
| Avaliações com notas e mídia | O sistema deve permitir avaliações e comentários dos estabelecimentos, com anexo de fotos. |
| Conexões sociais | O sistema deve permitir que usuários sigam outros perfis e os visualizem. |
| Feed de atividades | O sistema deve gerar um feed cronológico contendo as ações recentes dos usuários seguidos. |
| Perfil de usuário | O sistema deve exibir o perfil de cada usuário com foto, bio, histórico de avaliações públicas e total de check-ins realizados. |
| Cadastro e login | O sistema deve autenticar usuários via e-mail e senha ou login social (Google, Apple, GitHub). |
| Geração de URLs | O sistema deve trabalhar com URLs pré-assinadas para envio de imagens, vídeos e mídias em geral. |

## Requisitos funcionais do MVP

Definidos na apresentação ao Product Owner (outubro de 2026).

| ID | Nome | Descrição |
| -- | ---- | --------- |
| RF01 | Conta e sessão | Criar conta, fazer login e encerrar a sessão com segurança. |
| RF02 | Mapa e locais | Ver o mapa, buscar lugares, aplicar filtros e abrir os detalhes de um local. |
| RF03 | Amizades | Ver perfis, enviar e responder solicitações de amizade e remover amizades. |
| RF04 | Feed e check-in | Criar publicações individuais ou colaborativas, ver o feed, curtir, comentar e fazer check-in. |

## Requisitos não funcionais

| ID | Nome | Descrição |
| -- | ---- | --------- |
| RNF01 | Desempenho | Principais telas e consultas respondem em até 3 segundos, com conexão estável. |
| RNF02 | Segurança | Autenticação segura, comunicação criptografada e acesso aos dados conforme usuário e regras de privacidade. |
| RNF03 | Usabilidade | Interface responsiva, intuitiva e acessível, compatível com Android e iOS. |

## Escopo do MVP

Funcionalidades do MVP por prioridade:

| # | Funcionalidade | Prioridade |
| - | -------------- | ---------- |
| 1 | Cadastro e login de usuários | Essencial |
| 2 | Mapa interativo com busca, filtros e visualização de locais | Essencial |
| 3 | Informações detalhadas dos locais | Essencial |
| 4 | Perfis de usuários e sistema de amizades | Essencial |
| 5 | Feed social com publicações individuais e colaborativas, curtidas e comentários | Essencial |
| 6 | Check-in em locais e estrutura inicial para o Gerômetro | Desejável |

Fica para depois:

- Expansão para outras cidades
- Gerômetro completo em tempo real
- Reservas, pagamentos ou compras
- Painel comercial e anúncios pagos
- Roteiros turísticos automáticos
- Integração com transporte público
- Versões completas para Web e Desktop

## Modelo de domínio

Modelo proposto a partir dos requisitos RF01 a RF04.

| Classe | Atributos | Operações |
| ------ | --------- | --------- |
| Usuario | id, nome, email, senhaHash, fotoUrl | autenticar(), editarPerfil() |
| Local | id, nome, endereco, posicao, horario, faixaPreco, fotos, categoriaId | distanciaAte(p) |
| Categoria | id, nome, icone | - |
| Amizade | id, solicitanteId, destinatarioId, status, criadaEm, respondidaEm | aceitar(), recusar() |
| Publicacao | id, texto, tipo, midias, criadaEm, localId | publicar(), adicionarAutor(u) |
| Comentario | id, texto, criadoEm, publicacaoId, usuarioId | - |
| Curtida | id, criadaEm, publicacaoId, usuarioId | desfazer() |
| CheckIn | id, dataHora, posicao, usuarioId, localId | registrar() |

Enumerações:

- StatusAmizade: PENDENTE, ACEITA, RECUSADA
- TipoPublicacao: INDIVIDUAL, COLABORATIVA

## Tecnologias

| Camada | Tecnologia | Uso |
| ------ | ---------- | --- |
| App mobile | React Native, Expo, TypeScript | Android e iOS |
| Backend | Java, Spring Boot | API do aplicativo e autenticação |
| Banco de dados | PostgreSQL, PostGIS, Redis | Consultas geográficas e por proximidade |
| Apoio | Git, GitHub, GitHub Actions, Figma, Postman, Google e Apple Developer | Versionamento, CI, design, testes de API e publicação |

## Metodologia

Scrum em ciclos de duas semanas, com acompanhamento no Trello (colunas A fazer, Em andamento, Em revisão e Concluído).

1. Backlog: funcionalidades quebradas em tarefas menores, com responsável por área.
2. Sprint: cerca de duas semanas de desenvolvimento das tarefas priorizadas.
3. Revisão: entregas revisadas, problemas registrados e prioridades do próximo ciclo definidas.

## Cronograma

| Etapa | Data | Entrega |
| ----- | ---- | ------- |
| 1 | 13/09 | Requisitos, casos de uso e protótipo das telas |
| 2 | 15/10 | Configuração do projeto, banco de dados, autenticação e estrutura inicial do mapa |
| 3 | 15/11 | Funcionalidades sociais e integração entre mapa, locais e usuários |
| 4 | 06/12 | Testes, correções, documentação e apresentação final |

## Equipe

Curso de Análise e Desenvolvimento de Sistemas, disciplina de Engenharia de Software, Unit, 2026.2.

- Mateus Santos de Araujo
- Pedro Henrique de Almeida Dantas
- Pedro Emmanuel
- Ivys Gabriel
