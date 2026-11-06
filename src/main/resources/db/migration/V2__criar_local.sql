-- Tabela minima para as consultas geograficas; a task de cadastro completa as colunas (AD-021).
-- Escreva sempre public.: durante as migracoes, o schema padrao e o flyway (AD-024).
CREATE TABLE public.local (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome    VARCHAR(200) NOT NULL,
    posicao extensions.geography(Point, 4326) NOT NULL
);

-- Consulta por proximidade (ST_DWithin em geography)
CREATE INDEX idx_local_posicao ON public.local USING GIST (posicao);

-- Consulta por area: caixa exata em latitude/longitude
CREATE INDEX idx_local_posicao_geometry ON public.local USING GIST ((posicao::extensions.geometry));

-- Bloqueia a API de dados do Supabase; o backend e dono da tabela e nao e afetado (AD-024).
ALTER TABLE public.local ENABLE ROW LEVEL SECURITY;
