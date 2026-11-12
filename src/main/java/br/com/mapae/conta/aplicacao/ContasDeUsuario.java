package br.com.mapae.conta.aplicacao;

import br.com.mapae.compartilhado.seguranca.Autenticador;
import br.com.mapae.compartilhado.seguranca.Autenticador.RespostaLogin;
import br.com.mapae.compartilhado.seguranca.Cadastro;
import br.com.mapae.compartilhado.seguranca.ContaDuplicadaException;
import br.com.mapae.compartilhado.seguranca.DadosDeAcesso;
import br.com.mapae.compartilhado.seguranca.ProvedorDeConta;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import br.com.mapae.conta.dominio.Genero;
import br.com.mapae.conta.dominio.Usuario;
import br.com.mapae.conta.dominio.UsuarioRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de usuário: perfil USUARIO, status ATIVO e primeiro acesso pendente, sempre definidos aqui. */
@Service
public class ContasDeUsuario implements ProvedorDeConta {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final Autenticador autenticador;

    public ContasDeUsuario(UsuarioRepository usuarios, PasswordEncoder senhas, Autenticador autenticador) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.autenticador = autenticador;
    }

    @Transactional
    public Usuario cadastrar(NovoUsuario novo) {
        String username = DadosDeAcesso.minusculas(novo.username());
        String email = DadosDeAcesso.minusculas(novo.email());
        if (usuarios.existsByAcessoUsername(username)) {
            throw new ContaDuplicadaException("username");
        }
        if (usuarios.existsByAcessoEmail(email)) {
            throw new ContaDuplicadaException("email");
        }
        Usuario usuario = new Usuario(username, novo.nome().trim(), novo.dataNascimento(), email,
                Cadastro.cepSoDigitos(novo.cep()), novo.genero(), novo.bio(), senhas.encode(novo.senha()));
        return Cadastro.salvarSemDuplicar(() -> usuarios.saveAndFlush(usuario),
                "uq_usuario_username", "uq_usuario_email");
    }

    @Transactional(readOnly = true)
    public RespostaLogin entrar(String login, String senha) {
        Optional<Usuario> usuario = usuarios.buscarPorLogin(DadosDeAcesso.minusculas(login));
        return autenticador.autenticar(usuario.map(Usuario::getId).orElse(null),
                usuario.map(Usuario::getAcesso).orElse(null), senha);
    }

    @Override
    public TipoPerfil perfil() {
        return TipoPerfil.USUARIO;
    }

    @Override
    @Transactional(readOnly = true)
    public DadosDaConta buscar(long id) {
        Usuario usuario = existente(id);
        DadosDeAcesso acesso = usuario.getAcesso();
        return new DadosDaConta(usuario.getId(), acesso.getUsername(), usuario.getNome(), acesso.getEmail(),
                usuario.getCep(), acesso.getTipoPerfil(), acesso.getStatus(), acesso.isPrimeiroAcesso(),
                acesso.getCriadoEm(), acesso.getAtualizadoEm(), usuario.getDataNascimento(),
                usuario.getGenero().name(), usuario.getBio(), null);
    }

    @Override
    @Transactional
    public void concluirPrimeiroAcesso(long id) {
        existente(id).getAcesso().concluirPrimeiroAcesso();
    }

    @Override
    @Transactional
    public void trocarSenha(long id, String senhaAtual, String novaSenha) {
        autenticador.trocarSenha(existente(id).getAcesso(), senhaAtual, novaSenha);
    }

    private Usuario existente(long id) {
        return usuarios.findById(id).orElseThrow(ContaNaoEncontradaException::new);
    }

    public record NovoUsuario(String username, String nome, LocalDate dataNascimento, String email, String cep,
                              Genero genero, String bio, String senha) {
    }
}
