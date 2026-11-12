package br.com.mapae.compartilhado.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * API stateless com JWT HS256 (AD-004). Só as rotas da conta autenticada exigem token;
 * as demais continuam públicas, e cada task futura protege as suas.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtPropriedades.class, CorsPropriedades.class})
public class SegurancaConfig {

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http, AuthenticationEntryPoint semToken) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/api/v1/contas/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(jwt -> jwt.jwt(Customizer.withDefaults()).authenticationEntryPoint(semToken))
                .exceptionHandling(erros -> erros.authenticationEntryPoint(semToken))
                .build();
    }

    /**
     * CORS para o front web (AD-039). O token vai no cabeçalho Authorization, sem cookies,
     * então não há credenciais de navegador. O pré-voo (OPTIONS) passa antes da autenticação.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsPropriedades propriedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propriedades.origens());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cors);
        return fonte;
    }

    /** 401 em Problem Details, pelo mesmo tratador de erros do resto da API. */
    @Bean
    AuthenticationEntryPoint semToken(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver tratador) {
        return (requisicao, resposta, erro) -> tratador.resolveException(requisicao, resposta, null,
                new ErrorResponseException(HttpStatus.UNAUTHORIZED, ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Token ausente, inválido ou expirado"), erro));
    }

    @Bean
    SecretKey chaveJwt(JwtPropriedades propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
