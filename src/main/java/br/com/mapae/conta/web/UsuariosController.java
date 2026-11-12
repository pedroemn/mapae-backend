package br.com.mapae.conta.web;

import br.com.mapae.compartilhado.seguranca.Autenticador.LoginRequest;
import br.com.mapae.compartilhado.seguranca.Autenticador.RespostaLogin;
import br.com.mapae.compartilhado.seguranca.SenhaValida;
import br.com.mapae.compartilhado.seguranca.StatusConta;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import br.com.mapae.conta.aplicacao.ContasDeUsuario;
import br.com.mapae.conta.aplicacao.ContasDeUsuario.NovoUsuario;
import br.com.mapae.conta.dominio.Genero;
import br.com.mapae.conta.dominio.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Usuários", description = "Cadastro e login de usuários comuns")
@RequestMapping("/api/v1")
public class UsuariosController {

    private final ContasDeUsuario contas;

    public UsuariosController(ContasDeUsuario contas) {
        this.contas = contas;
    }

    @Operation(summary = "Cria a conta de um usuário",
            description = "A conta nasce com perfil USUARIO, status ATIVO e primeiro acesso pendente. "
                    + "Username e email são guardados em minúsculas e o CEP só com os 8 dígitos.")
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioCriadoResponse cadastrar(@Valid @RequestBody CadastroUsuarioRequest corpo) {
        Usuario usuario = contas.cadastrar(new NovoUsuario(corpo.username(), corpo.nome(), corpo.dataNascimento(),
                corpo.email(), corpo.cep(), Genero.valueOf(corpo.genero()), corpo.bio(), corpo.senha()));
        return UsuarioCriadoResponse.de(usuario);
    }

    @Operation(summary = "Login do usuário",
            description = "O campo login aceita email ou username, sem diferenciar maiúsculas. "
                    + "Devolve o JWT para o cabeçalho Authorization: Bearer.")
    @PostMapping("/auth/usuarios/login")
    public RespostaLogin entrar(@Valid @RequestBody LoginRequest corpo) {
        return contas.entrar(corpo.login(), corpo.senha());
    }

    public record CadastroUsuarioRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._]{3,30}",
                    message = "deve ter de 3 a 30 caracteres entre letras, números, ponto e sublinhado")
            String username,
            @NotBlank @Size(max = 200) String nome,
            @NotNull @Past LocalDate dataNascimento,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "deve ter 8 dígitos, com ou sem hífen")
            String cep,
            @NotBlank @Pattern(regexp = "FEMININO|MASCULINO|NAO_BINARIO|OUTRO|PREFIRO_NAO_INFORMAR",
                    message = "deve ser FEMININO, MASCULINO, NAO_BINARIO, OUTRO ou PREFIRO_NAO_INFORMAR")
            String genero,
            @Size(max = 500) String bio,
            @NotBlank @SenhaValida String senha) {
    }

    public record UsuarioCriadoResponse(Long id, String username, String nome, String email,
                                        boolean primeiroAcesso, StatusConta status, TipoPerfil tipoPerfil,
                                        Instant criadoEm) {
        static UsuarioCriadoResponse de(Usuario usuario) {
            var acesso = usuario.getAcesso();
            return new UsuarioCriadoResponse(usuario.getId(), acesso.getUsername(), usuario.getNome(),
                    acesso.getEmail(), acesso.isPrimeiroAcesso(), acesso.getStatus(), acesso.getTipoPerfil(),
                    acesso.getCriadoEm());
        }
    }
}
