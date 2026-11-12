package br.com.mapae.locais.web;

import br.com.mapae.compartilhado.seguranca.Autenticador.LoginRequest;
import br.com.mapae.compartilhado.seguranca.Autenticador.RespostaLogin;
import br.com.mapae.compartilhado.seguranca.SenhaValida;
import br.com.mapae.compartilhado.seguranca.StatusConta;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import br.com.mapae.locais.aplicacao.ContasDeLoja;
import br.com.mapae.locais.aplicacao.ContasDeLoja.NovaLoja;
import br.com.mapae.locais.dominio.Local;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Lojas", description = "Cadastro e login das contas de loja")
@RequestMapping("/api/v1")
public class LojasController {

    private final ContasDeLoja contas;

    public LojasController(ContasDeLoja contas) {
        this.contas = contas;
    }

    @Operation(summary = "Cria a conta de uma loja",
            description = "A loja nasce com perfil LOJA, status ATIVO, primeiro acesso pendente e sem posição: "
                    + "ela só aparece no mapa depois de ter posição.")
    @PostMapping("/lojas")
    @ResponseStatus(HttpStatus.CREATED)
    public LojaCriadaResponse cadastrar(@Valid @RequestBody CadastroLojaRequest corpo) {
        Local loja = contas.cadastrar(new NovaLoja(corpo.username(), corpo.nome(), corpo.email(), corpo.cep(),
                corpo.tipo(), corpo.senha()));
        return LojaCriadaResponse.de(loja);
    }

    @Operation(summary = "Login da loja",
            description = "O campo login aceita email ou username, sem diferenciar maiúsculas. "
                    + "Devolve o JWT para o cabeçalho Authorization: Bearer.")
    @PostMapping("/auth/lojas/login")
    public RespostaLogin entrar(@Valid @RequestBody LoginRequest corpo) {
        return contas.entrar(corpo.login(), corpo.senha());
    }

    public record CadastroLojaRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._]{3,30}",
                    message = "deve ter de 3 a 30 caracteres entre letras, números, ponto e sublinhado")
            String username,
            @NotBlank @Size(max = 200) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "deve ter 8 dígitos, com ou sem hífen")
            String cep,
            @Size(max = 50) String tipo,
            @NotBlank @SenhaValida String senha) {
    }

    public record LojaCriadaResponse(Long id, String username, String nome, String email, String tipo,
                                     boolean primeiroAcesso, StatusConta status, TipoPerfil tipoPerfil,
                                     Instant criadoEm) {
        static LojaCriadaResponse de(Local loja) {
            var acesso = loja.getAcesso();
            return new LojaCriadaResponse(loja.getId(), acesso.getUsername(), loja.getNome(), acesso.getEmail(),
                    loja.getTipo(), acesso.isPrimeiroAcesso(), acesso.getStatus(), acesso.getTipoPerfil(),
                    acesso.getCriadoEm());
        }
    }
}
