-- Registro de cada migracao do projeto: nome + timestamp (AD-030).
-- A tabela flyway.flyway_schema_history continua sendo a que o Flyway usa para controlar o que ja rodou.
-- Escreva sempre public.: durante as migracoes, o schema padrao e o flyway (AD-024).
CREATE TABLE public.migracao (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(200) NOT NULL UNIQUE,
    aplicada_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Bloqueia a API de dados do Supabase; o backend e dono da tabela e nao e afetado (AD-024).
ALTER TABLE public.migracao ENABLE ROW LEVEL SECURITY;

-- V1 e V2 ja rodaram antes desta tabela existir; os horarios vem do historico do Flyway (UTC).
INSERT INTO public.migracao (nome, aplicada_em) VALUES
    ('V1__habilitar_postgis',  TIMESTAMPTZ '2026-11-05 23:56:05.985344+00'),
    ('V2__criar_local',        TIMESTAMPTZ '2026-11-05 23:56:10.365096+00'),
    ('V20261107022500__criar_tabela_migracao', now());
