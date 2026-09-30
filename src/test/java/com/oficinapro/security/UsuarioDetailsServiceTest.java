package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

  @Mock private UsuarioRepository usuarioRepository;

  @InjectMocks private UsuarioDetailsService service;

  @Test
  @DisplayName("deve carregar o usuário (com a oficina) pelo username")
  void deveCarregarUsuarioPeloUsername() {
    Usuario usuario = new Usuario();
    usuario.setUsername("ana.gerente");
    usuario.setRole(Role.GERENTE);
    when(usuarioRepository.findByUsernameComOficina("ana.gerente")).thenReturn(Optional.of(usuario));

    UserDetails resultado = service.loadUserByUsername("ana.gerente");

    assertThat(resultado).isSameAs(usuario);
    assertThat(resultado.getUsername()).isEqualTo("ana.gerente");
  }

  @Test
  @DisplayName("username inexistente deve lançar UsernameNotFoundException genérica")
  void deveLancarQuandoUsernameNaoExiste() {
    when(usuarioRepository.findByUsernameComOficina("fantasma")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("fantasma"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageNotContaining("fantasma");
  }
}
