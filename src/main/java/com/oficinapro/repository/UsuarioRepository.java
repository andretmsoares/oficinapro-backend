package com.oficinapro.repository;

import com.oficinapro.model.Usuario;
import com.oficinapro.security.role.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends PessoaCrudRepository<Usuario> {

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<Usuario> findByUsername(String username);

    boolean existsByRole(Role role);

    /**
     * Usada na autenticação. O {@code left join fetch} é obrigatório: a aplicação roda com
     * {@code spring.jpa.open-in-view=false} e o usuário autenticado é carregado fora de uma
     * transação, então acessar {@code usuario.getOficina()} depois causaria
     * LazyInitializationException. É "left" porque o ADMIN do SaaS não tem oficina.
     */
    @Query("select u from Usuario u left join fetch u.oficina where u.username = :username")
    Optional<Usuario> findByUsernameComOficina(@Param("username") String username);
}
