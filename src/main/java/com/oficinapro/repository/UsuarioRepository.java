package com.oficinapro.repository;

import com.oficinapro.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends PessoaCrudRepository<Usuario> {

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<Usuario> findByUsername(String username);
}