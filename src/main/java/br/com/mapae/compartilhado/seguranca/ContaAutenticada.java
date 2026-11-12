package br.com.mapae.compartilhado.seguranca;

import org.springframework.security.oauth2.jwt.Jwt;

/** Quem fez a requisição, lido do token. */
public record ContaAutenticada(long id, TipoPerfil perfil) {

    public static ContaAutenticada de(Jwt token) {
        return new ContaAutenticada(Long.parseLong(token.getSubject()),
                TipoPerfil.valueOf(token.getClaimAsString(EmissorDeToken.CLAIM_PERFIL)));
    }
}
