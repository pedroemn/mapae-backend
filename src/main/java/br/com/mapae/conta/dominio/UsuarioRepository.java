package br.com.mapae.conta.dominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Username e email são gravados em minúsculas; o parâmetro já chega em minúsculas. */
    @Query("SELECT u FROM Usuario u WHERE u.acesso.username = :login OR u.acesso.email = :login")
    Optional<Usuario> buscarPorLogin(@Param("login") String login);

    boolean existsByAcessoUsername(String username);

    boolean existsByAcessoEmail(String email);
}
