-- Contas de usuario e de loja (spec conta-e-login).
-- Escreva sempre public.: durante as migracoes, o schema padrao e o flyway (AD-024).
-- A senha nunca e gravada em texto: so o hash BCrypt (AD-004).

CREATE TABLE public.usuario (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(30)  NOT NULL,
    nome            VARCHAR(200) NOT NULL,
    data_nascimento DATE         NOT NULL,
    email           VARCHAR(254) NOT NULL,
    cep             VARCHAR(8)   NOT NULL,
    genero          VARCHAR(30)  NOT NULL,
    bio             VARCHAR(500),
    senha_hash      VARCHAR(100) NOT NULL,
    primeiro_acesso BOOLEAN      NOT NULL DEFAULT true,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    tipo_perfil     VARCHAR(20)  NOT NULL DEFAULT 'USUARIO',
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_cep CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT ck_usuario_genero CHECK (genero IN ('FEMININO', 'MASCULINO', 'NAO_BINARIO', 'OUTRO', 'PREFIRO_NAO_INFORMAR')),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO')),
    CONSTRAINT ck_usuario_tipo_perfil CHECK (tipo_perfil = 'USUARIO')
);

-- Unicos sem diferenciar maiusculas
CREATE UNIQUE INDEX uq_usuario_username ON public.usuario (lower(username));
CREATE UNIQUE INDEX uq_usuario_email ON public.usuario (lower(email));

-- Bloqueia a API de dados do Supabase; o backend e dono da tabela e nao e afetado (AD-024).
ALTER TABLE public.usuario ENABLE ROW LEVEL SECURITY;

-- A conta da loja fica na propria tabela local. As colunas sao opcionais para
-- preservar os locais que ja existiam sem conta; a posicao vem depois do cadastro.
ALTER TABLE public.local
    ALTER COLUMN posicao DROP NOT NULL,
    ADD COLUMN username        VARCHAR(30),
    ADD COLUMN email           VARCHAR(254),
    ADD COLUMN cep             VARCHAR(8),
    ADD COLUMN tipo            VARCHAR(50),
    ADD COLUMN senha_hash      VARCHAR(100),
    ADD COLUMN primeiro_acesso BOOLEAN,
    ADD COLUMN status          VARCHAR(20),
    ADD COLUMN tipo_perfil     VARCHAR(20),
    ADD COLUMN criado_em       TIMESTAMPTZ,
    ADD COLUMN atualizado_em   TIMESTAMPTZ,
    ADD CONSTRAINT ck_local_cep CHECK (cep ~ '^[0-9]{8}$'),
    ADD CONSTRAINT ck_local_status CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO')),
    ADD CONSTRAINT ck_local_tipo_perfil CHECK (tipo_perfil = 'LOJA'),
    -- Ou o local nao tem conta, ou tem todos os dados de conta
    ADD CONSTRAINT ck_local_conta_completa CHECK (
        (senha_hash IS NULL AND username IS NULL AND email IS NULL)
        OR (senha_hash IS NOT NULL AND username IS NOT NULL AND email IS NOT NULL AND cep IS NOT NULL
            AND primeiro_acesso IS NOT NULL AND status IS NOT NULL AND tipo_perfil IS NOT NULL
            AND criado_em IS NOT NULL AND atualizado_em IS NOT NULL)
    ),
    -- Sem conta, o local precisa de posicao, como antes
    ADD CONSTRAINT ck_local_posicao_ou_conta CHECK (posicao IS NOT NULL OR senha_hash IS NOT NULL);

CREATE UNIQUE INDEX uq_local_username ON public.local (lower(username)) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX uq_local_email ON public.local (lower(email)) WHERE email IS NOT NULL;

INSERT INTO public.migracao (nome) VALUES ('V20261111103000__criar_contas');
