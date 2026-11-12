package br.com.mapae.conta.dominio;

import br.com.mapae.compartilhado.seguranca.DadosDeAcesso;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 8)
    private String cep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Genero genero;

    @Column(length = 500)
    private String bio;

    @Embedded
    private DadosDeAcesso acesso;

    protected Usuario() {
    }

    public Usuario(String username, String nome, LocalDate dataNascimento, String email, String cep,
                   Genero genero, String bio, String senhaHash) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.cep = cep;
        this.genero = genero;
        this.bio = bio;
        this.acesso = DadosDeAcesso.novos(username, email, senhaHash, TipoPerfil.USUARIO);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCep() {
        return cep;
    }

    public Genero getGenero() {
        return genero;
    }

    public String getBio() {
        return bio;
    }

    public DadosDeAcesso getAcesso() {
        return acesso;
    }
}
