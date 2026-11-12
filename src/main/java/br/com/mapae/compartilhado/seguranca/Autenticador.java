package br.com.mapae.compartilhado.seguranca;

import br.com.mapae.compartilhado.seguranca.EmissorDeToken.TokenEmitido;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Login comum às contas de usuário e de loja: confere a senha, depois o status, e emite o token.
 * Login inexistente e senha errada dão a mesma resposta.
 */
@Component
public class Autenticador {

    private final PasswordEncoder senhas;
    private final EmissorDeToken emissor;
    /** Conferido quando o login não existe, para a resposta levar o mesmo tempo da senha errada. */
    private final String hashSemConta;

    public Autenticador(PasswordEncoder senhas, EmissorDeToken emissor) {
        this.senhas = senhas;
        this.emissor = emissor;
        this.hashSemConta = senhas.encode(UUID.randomUUID().toString());
    }

    /** @param acesso nulo quando nenhuma conta tem esse login */
    public RespostaLogin autenticar(Long id, DadosDeAcesso acesso, String senha) {
        if (acesso == null) {
            confere(senha, hashSemConta);
            throw new CredenciaisInvalidasException();
        }
        if (!confere(senha, acesso.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        if (acesso.getStatus() != StatusConta.ATIVO) {
            throw new ContaIndisponivelException(acesso.getStatus());
        }
        TokenEmitido token = emissor.emitir(id, acesso.getTipoPerfil());
        return new RespostaLogin(token.token(), "Bearer", token.expiraEm(), acesso.isPrimeiroAcesso(),
                acesso.getStatus(), acesso.getTipoPerfil());
    }

    /** Troca a senha só se a atual conferir; o hash antigo continua se ela estiver errada. */
    public void trocarSenha(DadosDeAcesso acesso, String senhaAtual, String novaSenha) {
        if (!confere(senhaAtual, acesso.getSenhaHash())) {
            throw new ProvedorDeConta.SenhaAtualIncorretaException();
        }
        acesso.trocarSenhaHash(senhas.encode(novaSenha));
    }

    /** Uma senha acima de 72 bytes nunca foi cadastrada, e o BCrypt recusa conferi-la: é só uma senha errada. */
    private boolean confere(String senha, String hash) {
        return SenhaValida.Validador.cabeNoBcrypt(senha) && senhas.matches(senha, hash);
    }

    /** Corpo das rotas de login: email ou username, sem diferenciar maiúsculas, e a senha. */
    public record LoginRequest(@NotBlank String login, @NotBlank String senha) {
    }

    public record RespostaLogin(String token, String tipoToken, Instant expiraEm, boolean primeiroAcesso,
                                StatusConta status, TipoPerfil tipoPerfil) {
    }
}
