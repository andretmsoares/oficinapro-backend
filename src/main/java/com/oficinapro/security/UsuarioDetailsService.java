package com.oficinapro.security;

import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o {@link Usuario} que será o principal do contexto de segurança,
 * já com a oficina inicializada (ver {@code findByUsernameComOficina}).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsernameComOficina(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}
