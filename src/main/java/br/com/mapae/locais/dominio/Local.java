package br.com.mapae.locais.dominio;

import jakarta.persistence.Column;
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

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(nullable = false)
    private Point posicao;

    protected Local() {
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
}
