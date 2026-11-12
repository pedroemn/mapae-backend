# Contrato geográfico com o app

Como o app usa o mapa e a busca. As decisões e os motivos estão em `docs/decisões.md` (AD-017 a AD-028).

## Quem resolve o quê

| O usuário quer | Quem resolve | Passa pelo backend? |
| -------------- | ------------ | ------------------- |
| Buscar qualquer lugar ou endereço no mapa | Provedor externo de geocoding, chamado pelo app | Não |
| Traçar uma rota | App de mapas do aparelho, aberto por link | Não |
| Ver os locais do Mapaê na região visível | `GET /api/v1/locais/area` | Sim |
| Ver os locais do Mapaê, perto de um ponto ou todos | `GET /api/v1/locais/proximos` | Sim |

Os locais que o backend devolve são os de clientes do Mapaê, os que terão promoções e anúncios (AD-029). Os demais lugares que o usuário vê no mapa vêm só da API de mapas. O app deve destacar os locais do Mapaê.

Fluxo da busca livre: o app chama o provedor, recebe as coordenadas, move o mapa e chama `/api/v1/locais/area` com a nova região visível.

## Convenções

- Coordenadas em graus decimais, no padrão WGS 84 (o mesmo do GPS).
- Distâncias em metros.
- Erros seguem o formato Problem Details (RFC 9457), com `status`, `title` e `detail`.

## `GET /api/v1/locais/area`

Locais dentro da caixa da região visível, com as bordas incluídas e ordenados por `id`.

| Parâmetro | Obrigatório | Regra |
| --------- | ----------- | ----- |
| `oeste`, `leste` | Sim | De -180 a 180; `oeste` não pode ser maior que `leste` |
| `sul`, `norte` | Sim | De -90 a 90; `sul` não pode ser maior que `norte` |
| `limite` | Não | Pelo menos 1; sem ele devolve todos |

```json
{
  "itens": [
    { "id": 1, "nome": "Mercado", "latitude": -10.9, "longitude": -37.05 }
  ],
  "truncado": false
}
```

`truncado: true` indica que há mais locais do que o `limite`. Os itens devolvidos são os de menor `id`, não os mais relevantes, então o app deve pedir para o usuário aproximar o mapa.

Uma caixa que cruza o meridiano de 180° não é aceita. Isso não afeta Aracaju.

## `GET /api/v1/locais/proximos`

Locais do Mapaê, com ou sem ponto de referência. Todos os parâmetros são opcionais, então o usuário pode consultar de qualquer lugar.

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
    { "id": 1, "nome": "Mercado", "latitude": -10.9, "longitude": -37.05, "distanciaMetros": 110.61 }
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
