# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Teste cada borda de horario (abertura inclusiva e fechamento exclusivo) em todos os ramos da regra, inclusive no que vira a meia-noite
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `locais` · harmful: 0
- features: filtros-de-locais
- evidence: M04 LocalRepository.java:72; M07 LocalRepository.java:73 (locais)
- last seen: 2026-11-12T08:48:57Z

### L-002 - Rode os testes com o fuso da JVM diferente do fuso de negocio para provar que o codigo nao depende do fuso padrao
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `testes` · harmful: 0
- features: filtros-de-locais
- evidence: M03 PesquisaLocais.java:28 (testes)
- last seen: 2026-11-12T08:48:57Z

### L-003 - Teste o limite numerico exato que a spec define (como o tamanho minimo de palavra) com um caso na borda
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `locais` · harmful: 0
- features: filtros-de-locais
- evidence: ACHAR-04 Categoria.java:71 (locais)
- last seen: 2026-11-12T08:48:58Z

### L-004 - Defina na spec se o casamento de texto e por palavra inteira, prefixo ou trecho, com o tamanho minimo
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `busca` · harmful: 0
- features: filtros-de-locais
- evidence: ACHAR-05 Categoria.java:68 (busca)
- last seen: 2026-11-12T08:48:58Z

### L-005 - Em teste de ordenacao por niveis, escolha dados cuja ordem pelo desempate seguinte contrarie a ordem esperada
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `testes` · harmful: 0
- features: filtros-de-locais
- evidence: M17 LocalRepository.java:102; FiltrosPesquisaTests.java:406-413 (testes)
- last seen: 2026-11-12T09:11:50Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
