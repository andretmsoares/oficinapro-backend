package com.oficinapro.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.UsuarioDetailsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

  private SecurityConfig config;

  @BeforeEach
  void setUp() {
    config = new SecurityConfig();
    ReflectionTestUtils.setField(
        config, "allowedOrigins", List.of("http://localhost:5173", "https://app.exemplo.com"));
  }

  @Test
  @DisplayName("passwordEncoder: deve usar BCrypt (hash não reversível e verificável)")
  void passwordEncoderDeveSerBcrypt() {
    PasswordEncoder encoder = config.passwordEncoder();

    String hash = encoder.encode("senha1234");

    assertThat(hash).startsWith("$2").isNotEqualTo("senha1234");
    assertThat(encoder.matches("senha1234", hash)).isTrue();
    assertThat(encoder.matches("outra-senha", hash)).isFalse();
  }

  @Test
  @DisplayName("CORS: deve liberar apenas as origens configuradas e os métodos da API")
  void corsDeveRespeitarOrigensConfiguradas() {
    CorsConfigurationSource source = config.corsConfigurationSource();

    CorsConfiguration cors =
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/clientes"));

    assertThat(cors).isNotNull();
    assertThat(cors.getAllowedOrigins())
        .containsExactly("http://localhost:5173", "https://app.exemplo.com");
    assertThat(cors.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(cors.getAllowedHeaders()).containsExactly("*");
  }

  @Test
  @DisplayName("authenticationManager: credenciais corretas devem autenticar o usuário")
  void authenticationManagerAutenticaCredenciaisCorretas() {
    PasswordEncoder encoder = config.passwordEncoder();
    UsuarioDetailsService detailsService = mock(UsuarioDetailsService.class);

    Usuario usuario = new Usuario();
    usuario.setUsername("ana");
    usuario.setPassword(encoder.encode("senha1234"));
    usuario.setRole(Role.GERENTE);
    when(detailsService.loadUserByUsername("ana")).thenReturn(usuario);

    AuthenticationManager manager = config.authenticationManager(detailsService, encoder);
    Authentication resultado =
        manager.authenticate(new UsernamePasswordAuthenticationToken("ana", "senha1234"));

    assertThat(resultado.isAuthenticated()).isTrue();
    assertThat(resultado.getPrincipal()).isSameAs(usuario);
  }

  @Test
  @DisplayName("authenticationManager: senha errada e usuário inexistente dão o mesmo erro")
  void authenticationManagerNaoDiferenciaSenhaErradaDeUsuarioInexistente() {
    PasswordEncoder encoder = config.passwordEncoder();
    UsuarioDetailsService detailsService = mock(UsuarioDetailsService.class);

    Usuario usuario = new Usuario();
    usuario.setUsername("ana");
    usuario.setPassword(encoder.encode("senha1234"));
    usuario.setRole(Role.GERENTE);
    when(detailsService.loadUserByUsername("ana")).thenReturn(usuario);
    when(detailsService.loadUserByUsername("fantasma"))
        .thenThrow(new UsernameNotFoundException("Credenciais inválidas"));

    AuthenticationManager manager = config.authenticationManager(detailsService, encoder);

    assertThatThrownBy(
            () -> manager.authenticate(new UsernamePasswordAuthenticationToken("ana", "errada")))
        .isInstanceOf(BadCredentialsException.class);
    assertThatThrownBy(
            () ->
                manager.authenticate(
                    new UsernamePasswordAuthenticationToken("fantasma", "senha1234")))
        .isInstanceOf(BadCredentialsException.class);
  }
}
