package br.com.mapae.locais.aplicacao;

import br.com.mapae.compartilhado.seguranca.Autenticador;
import br.com.mapae.compartilhado.seguranca.Autenticador.RespostaLogin;
import br.com.mapae.compartilhado.seguranca.Cadastro;
import br.com.mapae.compartilhado.seguranca.ContaDuplicadaException;
import br.com.mapae.compartilhado.seguranca.DadosDeAcesso;
import br.com.mapae.compartilhado.seguranca.ProvedorDeConta;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import br.com.mapae.locais.dominio.Categoria;
import br.com.mapae.locais.dominio.Local;
import br.com.mapae.locais.dominio.LocalRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de loja na tabela local: perfil LOJA, status ATIVO e primeiro acesso pendente.
 * A loja nasce sem posição e só aparece no mapa depois de tê-la.
 */
@Service
public class ContasDeLoja implements ProvedorDeConta {

    private final LocalRepository locais;
    private final PasswordEncoder senhas;
    private final Autenticador autenticador;

    public ContasDeLoja(LocalRepository locais, PasswordEncoder senhas, Autenticador autenticador) {
        this.locais = locais;
        this.senhas = senhas;
        this.autenticador = autenticador;
    }

    @Transactional(readOnly = true)
    public RespostaLogin entrar(String login, String senha) {
        Optional<Local> loja = locais.buscarLojaPorLogin(DadosDeAcesso.minusculas(login));
        return autenticador.autenticar(loja.map(Local::getId).orElse(null),
                loja.map(Local::getAcesso).orElse(null), senha);
    }

    @Transactional
    public Local cadastrar(NovaLoja nova) {
        String username = DadosDeAcesso.minusculas(nova.username());
        String email = DadosDeAcesso.minusculas(nova.email());
        if (locais.existsByAcessoUsername(username)) {
            throw new ContaDuplicadaException("username");
        }
        if (locais.existsByAcessoEmail(email)) {
            throw new ContaDuplicadaException("email");
        }
        Categoria categoria = categoriaDoTipo(nova.tipo());
        Local loja = Local.loja(username, nova.nome().trim(), email, Cadastro.cepSoDigitos(nova.cep()), categoria,
                senhas.encode(nova.senha()));
        return Cadastro.salvarSemDuplicar(() -> locais.saveAndFlush(loja), "uq_local_username", "uq_local_email");
    }

    /** Tipo vazio fica sem categoria; outro valor precisa ser uma categoria da lista (LOJA-02). */
    private static Categoria categoriaDoTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        return Categoria.de(tipo).orElseThrow(TipoDeLojaInvalidoException::new);
    }

    @Override
    public TipoPerfil perfil() {
        return TipoPerfil.LOJA;
    }

    @Override
    @Transactional(readOnly = true)
    public DadosDaConta buscar(long id) {
        Local loja = existente(id);
        DadosDeAcesso acesso = loja.getAcesso();
        return new DadosDaConta(loja.getId(), acesso.getUsername(), loja.getNome(), acesso.getEmail(), loja.getCep(),
                acesso.getTipoPerfil(), acesso.getStatus(), acesso.isPrimeiroAcesso(), acesso.getCriadoEm(),
                acesso.getAtualizadoEm(), null, null, null, loja.getTipo());
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

    /** Locais antigos, sem conta, não são lojas. */
    private Local existente(long id) {
        return locais.findById(id).filter(l -> l.getAcesso() != null).orElseThrow(ContaNaoEncontradaException::new);
    }

    public record NovaLoja(String username, String nome, String email, String cep, String tipo, String senha) {
    }
}
