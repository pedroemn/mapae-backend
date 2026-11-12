package br.com.mapae.compartilhado.seguranca;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Colunas de login comuns às contas de usuário e de loja. A senha só existe como hash BCrypt.
 */
@Embeddable
public class DadosDeAcesso {

    @Column(name = "username", length = 30)
    private String username;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Column(name = "primeiro_acesso")
    private Boolean primeiroAcesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StatusConta status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil", length = 20)
    private TipoPerfil tipoPerfil;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    protected DadosDeAcesso() {
    }

    /** Conta nova: ATIVO, em primeiro acesso, com username e email em minúsculas. */
    public static DadosDeAcesso novos(String username, String email, String senhaHash, TipoPerfil tipoPerfil) {
        DadosDeAcesso dados = new DadosDeAcesso();
        dados.username = minusculas(username);
        dados.email = minusculas(email);
        dados.senhaHash = senhaHash;
        dados.primeiroAcesso = true;
        dados.status = StatusConta.ATIVO;
        dados.tipoPerfil = tipoPerfil;
        dados.criadoEm = agora();
        dados.atualizadoEm = dados.criadoEm;
        return dados;
    }

    public static String minusculas(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    /** Devolve false quando o primeiro acesso já estava concluído, sem mexer em atualizado_em. */
    public boolean concluirPrimeiroAcesso() {
        if (!primeiroAcesso) {
            return false;
        }
        primeiroAcesso = false;
        atualizadoEm = agora();
        return true;
    }

    public void trocarSenhaHash(String novoHash) {
        senhaHash = novoHash;
        atualizadoEm = agora();
    }

    private static Instant agora() {
        // O PostgreSQL guarda microssegundos
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isPrimeiroAcesso() {
        return primeiroAcesso;
    }

    public StatusConta getStatus() {
        return status;
    }

    public TipoPerfil getTipoPerfil() {
        return tipoPerfil;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
