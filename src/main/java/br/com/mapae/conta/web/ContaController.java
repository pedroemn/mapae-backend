package br.com.mapae.conta.web;

import br.com.mapae.compartilhado.openapi.OpenApiConfig;
import br.com.mapae.compartilhado.seguranca.ContaAutenticada;
import br.com.mapae.compartilhado.seguranca.ProvedorDeConta;
import br.com.mapae.compartilhado.seguranca.ProvedorDeConta.DadosDaConta;
import br.com.mapae.compartilhado.seguranca.SenhaValida;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rotas da conta do token, para usuário e para loja. */
@RestController
@Tag(name = "Conta autenticada", description = "Dados da conta logada, primeiro acesso e troca de senha")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@RequestMapping("/api/v1/contas/eu")
public class ContaController {

    private final Map<TipoPerfil, ProvedorDeConta> provedores = new EnumMap<>(TipoPerfil.class);

    public ContaController(List<ProvedorDeConta> provedores) {
        provedores.forEach(p -> this.provedores.put(p.perfil(), p));
    }

    @Operation(summary = "Dados da conta logada",
            description = "Inclui primeiroAcesso, status, tipoPerfil, criadoEm e atualizadoEm. Nunca traz a senha.")
    @GetMapping
    public DadosDaConta eu(@AuthenticationPrincipal Jwt token) {
        ContaAutenticada conta = ContaAutenticada.de(token);
        return provedores.get(conta.perfil()).buscar(conta.id());
    }

    @Operation(summary = "Marca o onboarding como concluído",
            description = "primeiroAcesso passa a false. Chamar de novo não altera nada.")
    @PostMapping("/primeiro-acesso/conclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void concluirPrimeiroAcesso(@AuthenticationPrincipal Jwt token) {
        ContaAutenticada conta = ContaAutenticada.de(token);
        provedores.get(conta.perfil()).concluirPrimeiroAcesso(conta.id());
    }

    @Operation(summary = "Troca a senha", description = "Exige a senha atual. A nova senha tem de 8 caracteres a 72 bytes.")
    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal Jwt token, @Valid @RequestBody TrocaSenhaRequest corpo) {
        ContaAutenticada conta = ContaAutenticada.de(token);
        provedores.get(conta.perfil()).trocarSenha(conta.id(), corpo.senhaAtual(), corpo.novaSenha());
    }

    public record TrocaSenhaRequest(@NotBlank String senhaAtual, @NotBlank @SenhaValida String novaSenha) {
    }
}
