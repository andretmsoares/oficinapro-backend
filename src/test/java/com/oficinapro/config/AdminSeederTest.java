package com.oficinapro.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private ApplicationArguments args;

  @BeforeEach
  void setUp() {
    args = mock(ApplicationArguments.class);
  }

  private AdminSeeder seeder(String username, String password) {
    return new AdminSeeder(
        usuarioRepository, passwordEncoder, username, password, "Administrador do SaaS");
  }

  @Test
  @DisplayName("sem username configurado, não deve fazer nada")
  void semUsernameNaoFazNada() {
    seeder("", "senha-forte-123").run(args);

    verifyNoInteractions(usuarioRepository, passwordEncoder);
  }

  @Test
  @DisplayName("sem senha configurada, não deve fazer nada")
  void semSenhaNaoFazNada() {
    seeder("admin.saas", "   ").run(args);

    verifyNoInteractions(usuarioRepository, passwordEncoder);
  }

  @Test
  @DisplayName("já existindo um ADMIN, não deve criar outro nem alterar a senha existente")
  void jaExistindoAdminNaoCria() {
    when(usuarioRepository.existsByRole(Role.ADMIN)).thenReturn(true);

    seeder("admin.saas", "senha-forte-123").run(args);

    verify(usuarioRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  @DisplayName("username já em uso por outro usuário: não deve criar o ADMIN")
  void usernameEmUsoNaoCria() {
    when(usuarioRepository.existsByRole(Role.ADMIN)).thenReturn(false);
    when(usuarioRepository.existsByUsername("admin.saas")).thenReturn(true);

    seeder("admin.saas", "senha-forte-123").run(args);

    verify(usuarioRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  @DisplayName("sem ADMIN e com credenciais livres: deve criar o ADMIN do SaaS sem oficina")
  void criaAdminDoSaas() {
    when(usuarioRepository.existsByRole(Role.ADMIN)).thenReturn(false);
    when(usuarioRepository.existsByUsername("admin.saas")).thenReturn(false);
    when(passwordEncoder.encode("senha-forte-123")).thenReturn("hash-da-senha");

    seeder("admin.saas", "senha-forte-123").run(args);

    ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(captor.capture());
    Usuario admin = captor.getValue();
    assertThat(admin.getUsername()).isEqualTo("admin.saas");
    assertThat(admin.getNome()).isEqualTo("Administrador do SaaS");
    assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    assertThat(admin.getOficina()).isNull();
    // a senha nunca é gravada em texto puro
    assertThat(admin.getPassword()).isEqualTo("hash-da-senha");
  }
}
