package br.com.mapae.locais.dominio;

import br.com.mapae.compartilhado.seguranca.DadosDeAcesso;
import br.com.mapae.compartilhado.seguranca.TipoPerfil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(name = "local")
public class Local {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    /** Nula na loja recém-cadastrada: ela só aparece no mapa depois de ter posição. */
    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    private Point posicao;

    @Column(length = 8)
    private String cep;

    /** Tipo do estabelecimento em texto livre, como "bar". */
    @Column(length = 50)
    private String tipo;

    /** Nulo nos locais criados antes das contas de loja. */
    @Embedded
    private DadosDeAcesso acesso;

    protected Local() {
    }

    /** Loja criada pelo cadastro, ainda sem posição. */
    public static Local loja(String username, String nome, String email, String cep, String tipo, String senhaHash) {
        Local local = new Local();
        local.nome = nome;
        local.cep = cep;
        local.tipo = tipo;
        local.acesso = DadosDeAcesso.novos(username, email, senhaHash, TipoPerfil.LOJA);
        return local;
    }

    public Local(String nome, double latitude, double longitude) {
        this.nome = nome;
        this.posicao = WGS84.createPoint(new Coordinate(longitude, latitude));
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getLatitude() {
        return posicao.getY();
    }

    public double getLongitude() {
        return posicao.getX();
    }

    public String getCep() {
        return cep;
    }

    public String getTipo() {
        return tipo;
    }

    public DadosDeAcesso getAcesso() {
        return acesso;
    }
}
