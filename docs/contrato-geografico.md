# Contrato geográfico com o app

Como o app usa o mapa e a busca. As decisões e os motivos estão em `docs/decisões.md` (AD-017 a AD-028).

## Quem resolve o quê

| O usuário quer | Quem resolve | Passa pelo backend? |
| -------------- | ------------ | ------------------- |
| Buscar qualquer lugar ou endereço no mapa | Provedor externo de geocoding, chamado pelo app | Não |
| Traçar uma rota | App de mapas do aparelho, aberto por link | Não |
| Ver os locais do Mapaê na região visível | `GET /api/v1/locais/area` | Sim |
| Ver os locais do Mapaê, perto de um ponto ou todos | `GET /api/v1/locais/proximos` | Sim |
| Pesquisar locais do Mapaê por nome, categoria ou termo | `GET /api/v1/locais/pesquisa` | Sim |

Os locais que o backend devolve são os de clientes do Mapaê, os que terão promoções e anúncios (AD-029). Os demais lugares que o usuário vê no mapa vêm só da API de mapas. O app deve destacar os locais do Mapaê.

Fluxo da busca livre: o app chama o provedor, recebe as coordenadas, move o mapa e chama `/api/v1/locais/area` com a nova região visível.

## Convenções

- Coordenadas em graus decimais, no padrão WGS 84 (o mesmo do GPS).
- Distâncias em metros.
- Erros seguem o formato Problem Details (RFC 9457), com `status`, `title` e `detail`.

## Catálogo e acesso

- O mapa e a pesquisa só mostram o catálogo ativo: local com `ativo = true`, com posição e, se for conta de loja, com status `ATIVO` (AD-040).
- As três rotas de `/api/v1/locais` são públicas. Um token enviado a elas é ignorado, mesmo expirado, então o mapa não quebra quando o login vence (AD-041).
- O marcador de `/area` tem `id`, `nome`, `categoria` (pode ser `null`), `latitude` e `longitude`. `/pesquisa` traz os mesmos campos mais `faixaPreco`, `abertoAgora` e `distanciaMetros`; `/proximos`, os mesmos mais `distanciaMetros`.

### Paginação (AD-042)

| Parâmetro | Padrão | Regra |
| --------- | ------ | ----- |
| `pagina` | 0 | De 0 a 21474836 |
| `tamanho` | 20 | De 1 a 100 |

```json
"pagina": { "numero": 0, "tamanho": 20, "totalItens": 42, "totalPaginas": 3 }
```

Uma página depois da última volta com `itens` vazio e os totais corretos.

## `GET /api/v1/locais/area`

Locais ativos dentro da caixa da região visível, com as bordas incluídas e ordenados por `id`.

| Parâmetro | Obrigatório | Regra |
| --------- | ----------- | ----- |
| `oeste`, `leste` | Sim | De -180 a 180; `oeste` não pode ser maior que `leste` |
| `sul`, `norte` | Sim | De -90 a 90; `sul` não pode ser maior que `norte` |
| `pagina`, `tamanho` | Não | Paginação acima; com um deles, a resposta vem paginada |
| `limite` | Não | Pelo menos 1; não combina com `pagina` nem `tamanho` (400) |

Sem `pagina`, `tamanho` e `limite`, devolve todos os locais da caixa, sem o campo `pagina` (AD-028).

```json
{
  "itens": [
    { "id": 1, "nome": "Mercado", "categoria": "mercado", "latitude": -10.9, "longitude": -37.05 }
  ],
  "truncado": false,
  "pagina": { "numero": 0, "tamanho": 20, "totalItens": 1, "totalPaginas": 1 }
}
```

`truncado: true` indica que há mais locais: com `limite`, os que ficaram de fora; com paginação, a próxima página. Os itens vêm por `id`, não por relevância, então o app deve pedir para o usuário aproximar o mapa ou buscar a próxima página.

Uma caixa que cruza o meridiano de 180° não é aceita. Isso não afeta Aracaju.

## `GET /api/v1/locais/pesquisa`

Pesquisa no catálogo ativo, com filtros (BACK-023 e BACK-024). É independente do mapa: não usa a região visível. Sempre paginada.

| Parâmetro | Obrigatório | Regra |
| --------- | ----------- | ----- |
| `termo` | Não | Até 100 caracteres, sem o caractere NUL. Veja "Como o termo casa" |
| `categoria` | Não | Uma ou mais da lista abaixo, pelo código ou pelo nome. Repita o parâmetro (`categoria=bar&categoria=cafe`) ou separe por vírgula. No máximo 20 valores |
| `preco` | Não | Uma ou mais faixas de 1 ($) a 4 ($$$$), repetindo o parâmetro ou separadas por vírgula. No máximo 20 valores |
| `abertoAgora` | Não | `true` mostra só os abertos agora, no horário de Aracaju. `false` ou ausente não filtra |
| `latitude`, `longitude` | Não | Posição do usuário. Precisam vir juntas. Com elas, cada item traz `distanciaMetros` |
| `raioMetros` | Não | De 1 a 100000. Exige `latitude` e `longitude` |
| `pagina`, `tamanho` | Não | Paginação acima |

### Categorias (AD-045)

| Código | Nome | Também acha por |
| ------ | ---- | --------------- |
| `bar` | Bar | boteco, pub, choperia, cervejaria |
| `restaurante` | Restaurante | comida, almoço, jantar, pizzaria, churrascaria |
| `cafe` | Café | cafeteria |
| `padaria` | Padaria | panificadora, pão |
| `lanchonete` | Lanchonete | lanche, hamburgueria, sanduíche, salgado |
| `balada` | Balada | boate, festa, night |
| `evento` | Evento | show, festival, feira |
| `outro` | Outro | |

O parâmetro `categoria` aceita só o código ou o nome ("Café" e "cafe" são a mesma). Os sinônimos valem só no `termo`.

### Como o termo casa (AD-049)

- Cada palavra precisa casar com o nome, os termos ou a categoria do local, em qualquer ordem: "napoli pizzaria" acha "Pizzaria Napoli".
- Não diferencia maiúsculas nem acentos: `cafe` acha "Café Central".
- Palavras de ligação (`de`, `do`, `da`, `dos`, `das`, `e`, `em`, `no`, `na`, `a`, `o`, `os`, `as`, `com`, `sem`, `para`, `pra`) são ignoradas.
- Plural acha o singular: "bares" acha "Bar do Rock".
- Erro leve de digitação em palavras com 4 letras ou mais: "pizaria" acha "Pizzaria Napoli"; "restaurnte" acha os restaurantes.
- Sinônimo da categoria acha os locais dela, pelo começo da palavra: "boteco" e "botec" acham os bares.
- `%`, `_` e `\` são caracteres comuns, não curingas.

### Filtros

- Os filtros se combinam: o local precisa atender a todos e ao `termo`.
- Para limpar um filtro, basta tirá-lo da URL.
- Local sem preço ou sem horário cadastrado fica fora quando o filtro correspondente está ligado. Sem o filtro, ele aparece com o campo `null`.
- "Aberto agora" usa o horário de Aracaju (`America/Maceio`), qualquer que seja o fuso do servidor ou do aparelho. Um local das 18h às 2h está aberto às 23h e à 1h.
- Sem `latitude` e `longitude`, o filtro de distância fica indisponível (`raioMetros` dá 400) e os outros funcionam. O app deve esconder ou desabilitar o filtro de distância quando não tiver a localização.

### Ordem

- Com `termo`: primeiro o nome que começa com o termo, depois o nome que contém o termo, depois o que casa por termos ou categoria, por último o que casa só por erro de digitação.
- Em seguida, do mais perto ao mais longe (se houver origem), depois nome e `id`.
- Sem `termo` e com origem: do mais perto ao mais longe. Sem `termo` e sem origem: por nome.

### Resposta

```json
{
  "itens": [
    { "id": 7, "nome": "Pizzaria Napoli", "categoria": "restaurante", "latitude": -10.96, "longitude": -37.05,
      "faixaPreco": 3, "abertoAgora": true, "distanciaMetros": 1105.8 }
  ],
  "pagina": { "numero": 0, "tamanho": 20, "totalItens": 1, "totalPaginas": 1 }
}
```

`faixaPreco`, `abertoAgora` e `distanciaMetros` vêm sempre, com `null` quando o dado é desconhecido ou a pesquisa não tem origem. Sem resultado: 200 com `itens` vazio e `totalItens` 0. Valor inválido (categoria fora da lista, `preco` fora de 1 a 4, raio fora da faixa, origem incompleta): 400 em Problem Details.

A pesquisa só olha os locais do Mapaê. Endereços e outros lugares continuam com o provedor de mapas do app.

### Cadastro da loja (AD-050)

O campo `tipo` do `POST /api/v1/lojas` aceita só uma categoria da lista (código ou nome) e é opcional. A loja passa a aparecer no filtro dessa categoria quando tiver posição. Outro valor dá 400 com `campos.tipo`.

## `GET /api/v1/locais/proximos`

Locais ativos do Mapaê, com ou sem ponto de referência. Todos os parâmetros são opcionais, então o usuário pode consultar de qualquer lugar.

| Parâmetro | Obrigatório | Regra |
| --------- | ----------- | ----- |
| `latitude` | Não | De -90 a 90; deve vir junto com `longitude` |
| `longitude` | Não | De -180 a 180; deve vir junto com `latitude` |
| `raioMetros` | Não | Maior que 0; exige `latitude` e `longitude`; sem ele não há raio |
| `limite` | Não | Pelo menos 1; sem ele devolve todos |

- Sem coordenadas: todos os locais, ordenados por `id`, sem `distanciaMetros`.
- Com coordenadas e sem raio: todos os locais, do mais perto ao mais longe.
- Com coordenadas e raio: só os locais dentro do raio, do mais perto ao mais longe.

```json
{
  "itens": [
    { "id": 1, "nome": "Mercado", "categoria": "mercado", "latitude": -10.9, "longitude": -37.05, "distanciaMetros": 110.61 }
  ]
}
```

## Confirmação de ida

A confirmação de ida avisa que o usuário vai ao local e pode ser feita de qualquer lugar. O backend não valida a localização, e o app não envia posição, precisão nem horário de captura (AD-026). O endpoint de registro será criado em uma task futura.

## Limites

As consultas não têm limite técnico de raio nem de quantidade (AD-028). `limite` é opcional e `truncado` só aparece como `true` quando o app pede um `limite` menor que o total.

## Credenciais

- O backend não guarda nenhuma chave de provedor de mapas, porque não chama esses serviços.
- A chave do provedor de geocoding fica no app, e todo app publicado pode ter a chave extraída. Por isso ela precisa ser restrita ao pacote Android e ao bundle iOS, e ter cota limitada no painel do provedor.
- Chaves do Supabase e a senha do banco nunca vão para o app (AD-015).
- As credenciais do banco ficam só em variáveis de ambiente ou no `.env`, fora do git (AD-010).

## Cuidados

- **Os endpoints ainda estão abertos.** Não há autenticação nem limite de requisições, porque o JWT (AD-004) é uma task futura.
- **Posição é dado pessoal (LGPD).** O backend não registra coordenadas em log. Como `/api/v1/locais/proximos` leva a posição na URL, os logs de acesso de proxy ou de hospedagem não devem gravar a query string.
