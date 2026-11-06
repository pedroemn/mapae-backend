# Validação: consultas-geograficas (BACK-006)

**Veredito:** PASS
**Data:** 2026-11-05
**Gate:** `./mvnw clean verify` com PostGIS em Docker: 34 testes, 0 falhas.
**Revisor:** revisão independente do código final, feita por quem não escreveu a mudança de AD-028.

## Evidência por requisito

| Requisito | Resultado | Evidência |
| --------- | --------- | --------- |
| Área devolve só os locais da caixa, por `id`, bordas incluídas | OK | `ConsultasGeograficasTests.java:48` (caixa), `:66` (borda) |
| `truncado` só quando há mais locais que o `limite` pedido | OK | `ConsultasGeograficasTests.java:83`, `:96` |
| Área sem `limite` devolve tudo | OK | `ConsultasGeograficasTests.java:107` |
| Área com bordas invertidas ou fora da faixa devolve 400 | OK | `ConsultasGeograficasTests.java:118`, `:123`, `:259` |
| Próximos ordena do mais perto ao mais longe, com distância | OK | `ConsultasGeograficasTests.java:128` |
| Raio filtra; sem raio devolve todos; sem coordenadas lista por `id` sem distância | OK | `ConsultasGeograficasTests.java:128`, `:168`, `:182` |
| Sem raio máximo nem limite máximo | OK | `ConsultasGeograficasTests.java:158`, `:195` |
| Coordenada sozinha, raio sem coordenadas e raio inválido devolvem 400 | OK | `ConsultasGeograficasTests.java:224`, `:230`, `:204` |
| Banco: PostGIS em `extensions`, `geography(Point,4326)`, RLS, histórico em `flyway` | OK | `MapaeApplicationTests.java:23`, `:33`, `:42`, `:52`, `:62` |
| Fronteiras entre módulos | OK | `ModularidadeTests.java` |

## Sensor de discriminação

10 defeitos injetados em cópia temporária, com os arquivos originais restaurados depois. Os 10 foram pegos:
raio ignorado (`LocalRepository.java:59`), ordenação por distância sem e com raio, listagem em ordem invertida, par latitude/longitude, raio sem coordenadas (`ConsultaLocais.java:44`), `truncado` com `>=` (`ConsultaLocais.java:34`), `oeste > leste`, `sul > norte` e estouro do `limite` (`ConsultaLocais.java:57`).

## Defeito achado na revisão

`limite=2147483647` em `/api/locais/area` estourava o inteiro em `limiteEfetivo + 1` e dava erro 500. Corrigido com o teto interno `SEM_LIMITE` e coberto por teste (`ConsultasGeograficasTests.java:213`).

## Banco real

Aplicação iniciada contra o Supabase em 2026-11-05: V1 e V2 aplicadas com sucesso, PostGIS 3.3.7 em `extensions`, `public.local` com RLS ligado, endpoints respondendo 200.

## Lacunas conhecidas

- Sem `limite`, a resposta traz todos os locais; sem paginação (AD-028). Aceitável enquanto só houver locais de clientes (AD-029).
- Os endpoints estão abertos até o login existir (AD-004).
