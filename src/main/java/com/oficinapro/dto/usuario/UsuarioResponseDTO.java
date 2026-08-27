package com.oficinapro.dto.usuario;

import com.oficinapro.security.role.Role;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String telefone,
        String documento,
        Long oficinaId,
        String username,
        Role role
) {
}
