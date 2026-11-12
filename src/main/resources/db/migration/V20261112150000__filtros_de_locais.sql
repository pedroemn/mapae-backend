-- Filtros da pesquisa de locais (spec filtros-de-locais: BACK-024).
-- Escreva sempre public.: durante as migracoes, o schema padrao e o flyway (AD-024).

-- Busca tolerante a erro de digitacao. Fica no schema extensions, como o PostGIS e o unaccent.
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA extensions;

-- faixa_preco: 1 a 4 ($ a $$$$); nula enquanto desconhecida.
-- abre/fecha: o mesmo horario todo dia, no horario de Aracaju. fecha < abre vira a meia-noite;
-- abre = fecha e 24 horas. Os dois nulos quando o horario e desconhecido.
ALTER TABLE public.local
    ADD COLUMN faixa_preco SMALLINT,
    ADD COLUMN abre        TIME,
    ADD COLUMN fecha       TIME,
    ADD CONSTRAINT ck_local_faixa_preco CHECK (faixa_preco BETWEEN 1 AND 4),
    ADD CONSTRAINT ck_local_horario CHECK ((abre IS NULL) = (fecha IS NULL)),
    -- Mesma lista do enum Categoria; um teste confere que as duas nao divergem.
    ADD CONSTRAINT ck_local_categoria CHECK (categoria IN
        ('bar', 'restaurante', 'cafe', 'padaria', 'lanchonete', 'balada', 'evento', 'outro'));

INSERT INTO public.migracao (nome) VALUES ('V20261112150000__filtros_de_locais');
