package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.enums.Role;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedUserProviderTest {

  private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider();

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static Usuario usuario(Role role, Long oficinaId) {
    Usuario usuario = new Usuario();
    usuario.setUsername("teste");
    usuario.setRole(role);
    if (oficinaId != null) {
      Oficina oficina = new Oficina();
      oficina.setId(oficinaId);
      usuario.setOficina(oficina);
    }
    return usuario;
  }

  private static void autenticarComo(Usuario usuario) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
  }

  @Test
  @DisplayName("getUsuarioAutenticado: deve devolver o Usuario que é o principal do contexto")
  void deveDevolverUsuarioDoContexto() {
    Usuario gerente = usuario(Role.GERENTE, 3L);
    autenticarComo(gerente);

    assertThat(provider.getUsuarioAutenticado()).isSameAs(gerente);
  }

  @Test
  @DisplayName("getUsuarioAutenticado: sem autenticação no contexto deve lançar exceção")
  void deveLancarSemAutenticacao() {
    assertThatThrownBy(provider::getUsuarioAutenticado)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  @DisplayName("getUsuarioAutenticado: autenticação marcada como não autenticada deve lançar")
  void deveLancarQuandoAutenticacaoNaoEstaAutenticada() {
    UsernamePasswordAuthenticationToken naoAutenticado =
        new UsernamePasswordAuthenticationToken(usuario(Role.GERENTE, 3L), "senha");
    SecurityContextHolder.getContext().setAuthentication(naoAutenticado);

    assertThat(naoAutenticado.isAuthenticated()).isFalse();
    assertThatThrownBy(provider::getUsuarioAutenticado)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  @DisplayName("getUsuarioAutenticado: principal que não é Usuario (anônimo) deve lançar")
  void deveLancarParaPrincipalQueNaoEUsuario() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThatThrownBy(provider::getUsuarioAutenticado)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  @DisplayName("getUsuarioAutenticado: principal String (ex.: mock) deve lançar")
  void deveLancarParaPrincipalString() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("alguem", "x", "ROLE_ADMIN"));

    assertThatThrownBy(provider::getUsuarioAutenticado)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  @DisplayName("getOficinaIdUsuarioLogado: deve devolver o id da oficina do usuário")
  void deveDevolverOficinaDoUsuario() {
    autenticarComo(usuario(Role.GERENTE, 3L));

    assertThat(provider.getOficinaIdUsuarioLogado()).isEqualTo(3L);
  }

  @Test
  @DisplayName("getOficinaIdUsuarioLogado: usuário sem oficina deve lançar UsuarioAcessDenied")
  void deveLancarQuandoUsuarioNaoTemOficina() {
    autenticarComo(usuario(Role.ADMIN, null));

    assertThatThrownBy(provider::getOficinaIdUsuarioLogado)
        .isInstanceOf(UsuarioAcessDeniedException.class);
  }

  @Test
  @DisplayName("getOficinaIdUsuarioLogado: sem autenticação deve lançar exceção de credenciais")
  void deveLancarOficinaSemAutenticacao() {
    assertThatThrownBy(provider::getOficinaIdUsuarioLogado)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }
}
