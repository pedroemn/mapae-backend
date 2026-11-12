package br.com.mapae.compartilhado.seguranca;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** JWT HS256 com o id da conta no sub e o tipo de perfil na claim "perfil". */
@Component
public class EmissorDeToken {

    public static final String CLAIM_PERFIL = "perfil";

    private final JwtEncoder encoder;
    private final JwtPropriedades propriedades;

    public EmissorDeToken(JwtEncoder encoder, JwtPropriedades propriedades) {
        this.encoder = encoder;
        this.propriedades = propriedades;
    }

    public TokenEmitido emitir(long id, TipoPerfil perfil) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(propriedades.validade());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(id))
                .claim(CLAIM_PERFIL, perfil.name())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
