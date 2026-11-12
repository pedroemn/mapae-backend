-- Catalogo pesquisavel de locais (spec locais-mapa-e-pesquisa: BACK-022 e BACK-023).
-- Escreva sempre public.: durante as migracoes, o schema padrao e o flyway (AD-024).

-- Busca sem diferenciar acentos. Fica no schema extensions, como o PostGIS (AD-017).
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;

-- categoria: classificacao do catalogo, como "bar" ou "cafe"; nula ate o cadastro administrativo.
-- termos: palavras-chave pesquisaveis do local.
-- ativo: so local ativo aparece no mapa e na pesquisa. Os locais existentes continuam ativos.
ALTER TABLE public.local
    ADD COLUMN categoria VARCHAR(50),
    ADD COLUMN termos    TEXT[]  NOT NULL DEFAULT '{}',
    ADD COLUMN ativo     BOOLEAN NOT NULL DEFAULT true;

INSERT INTO public.migracao (nome) VALUES ('V20261112060000__catalogo_de_locais');
