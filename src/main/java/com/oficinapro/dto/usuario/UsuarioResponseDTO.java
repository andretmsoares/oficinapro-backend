package com.oficinapro.dto.usuario;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Usuario;

public record UsuarioResponseDTO(
    Long id,
    String nome,
    String telefone,
    String documento,
    Long oficinaId,
    String username,
    Role role,
    boolean bloqueado) {
  /**
   * Fonte única de mapeamento. {@code oficinaId} vem nulo para o ADMIN do SaaS, que não é vinculado
   * a nenhuma oficina. {@code bloqueado} indica login bloqueado por excesso de tentativas
   * (temporário ainda vigente ou permanente).
   */
  public static UsuarioResponseDTO de(Usuario usuario) {
    return new UsuarioResponseDTO(
        usuario.getId(),
        usuario.getNome(),
        usuario.getTelefone(),
        usuario.getDocumento(),
        usuario.getOficina() != null ? usuario.getOficina().getId() : null,
        usuario.getUsername(),
        usuario.getRole(),
        usuario.isLoginBloqueado());
  }
}
