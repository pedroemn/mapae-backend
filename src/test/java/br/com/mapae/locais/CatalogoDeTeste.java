package br.com.mapae.locais;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/** Insere locais fictícios no catálogo do banco de teste (não há cadastro administrativo ainda). */
final class CatalogoDeTeste {

    private final JdbcTemplate jdbc;

    CatalogoDeTeste(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @param termos separados por "|"; vazio para nenhum */
    long local(String nome, String categoria, String termos, boolean ativo, double latitude, double longitude) {
        GeneratedKeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            var sql = conexao.prepareStatement("""
                    INSERT INTO local (nome, categoria, termos, ativo, posicao)
                    VALUES (?, ?, string_to_array(?, '|'), ?,
                            CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography))""", new String[] {"id"});
            sql.setString(1, nome);
            sql.setString(2, categoria);
            sql.setString(3, termos);
            sql.setBoolean(4, ativo);
            sql.setDouble(5, longitude);
            sql.setDouble(6, latitude);
            return sql;
        }, chave);
        return chave.getKey().longValue();
    }

    long ativo(String nome, String categoria, String termos, double latitude, double longitude) {
        return local(nome, categoria, termos, true, latitude, longitude);
    }

    /**
     * Preço e horário de um local já inserido (BACK-024).
     *
     * @param abre  "HH:mm" ou nulo
     * @param fecha "HH:mm" ou nulo
     */
    void detalhes(long id, Integer faixaPreco, String abre, String fecha) {
        jdbc.update("UPDATE local SET faixa_preco = ?, abre = CAST(? AS time), fecha = CAST(? AS time) WHERE id = ?",
                faixaPreco, abre, fecha, id);
    }
}
