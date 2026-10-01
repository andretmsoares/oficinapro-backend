package com.oficinapro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.OficinaDisabledException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OficinaAccessValidatorTest {

  private static final Long OFICINA_A = 1L;
  private static final Long OFICINA_B = 2L;

  // O validador delega a obtenção do usuário ao provider. Precisa ser @Mock: o
  // @InjectMocks abaixo é uma instância REAL, e chamar when() sobre ela lança
  // MissingMethodInvocationException, derrubando a classe inteira.
  @Mock private AuthenticatedUserProvider authenticatedUserProvider;

  @InjectMocks private OficinaAccessValidator validator;

  private Usuario usuario(Role role, Long oficinaId) {
    Usuario usuario = new Usuario();
    usuario.setId(10L);
    usuario.setNome("Usuário de teste");
    usuario.setUsername("teste");
    usuario.setRole(role);

    if (oficinaId != null) {
      Oficina oficina = new Oficina();
      oficina.setId(oficinaId);
      usuario.setOficina(oficina);
    }

    return usuario;
  }

  private void logado(Role role, Long oficinaId) {
    when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(usuario(role, oficinaId));
  }

  private void logadoComOficina(Role role, Long oficinaId) {
    logado(role, oficinaId);

    when(authenticatedUserProvider.getOficinaIdUsuarioLogado()).thenReturn(oficinaId);
  }

  @Nested
  @DisplayName("validarRole")
  class ValidarRole {

    @Test
    @DisplayName("permite quando a role do usuário está na lista de roles aceitas")
    void devePermitirQuandoRoleEstaNaListaPermitida() {
      logado(Role.GERENTE, OFICINA_A);

      assertThatCode(() -> validator.validarRole(Role.GERENTE, Role.MECANICO))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nega quando a role do usuário não está na lista, mesmo sendo ADMIN")
    void deveNegarAdminQuandoRoleNaoEstaNaListaPermitida() {
      logado(Role.ADMIN, null);

      assertThatThrownBy(() -> validator.validarRole(Role.GERENTE))
          .as("ADMIN não recebe passe livre em validarRole: a lista de roles é literal")
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("não possui permissão");
    }

    @Test
    @DisplayName("nega MECANICO em operação restrita a GERENTE")
    void deveNegarMecanicoEmOperacaoDeGerente() {
      logado(Role.MECANICO, OFICINA_A);

      assertThatThrownBy(() -> validator.validarRole(Role.GERENTE))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("nega quando nenhuma role é informada")
    void deveNegarQuandoNenhumaRoleEhInformada() {
      logado(Role.ADMIN, null);

      assertThatThrownBy(() -> validator.validarRole())
          .as("lista vazia significa que ninguém passa")
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  @Nested
  @DisplayName("getOficinaIdUsuarioLogado")
  class GetOficinaIdUsuarioLogado {

    @Test
    @DisplayName("retorna a oficina do usuário vinculado")
    void deveRetornarOficinaDoUsuarioVinculado() {
      when(authenticatedUserProvider.getOficinaIdUsuarioLogado()).thenReturn(OFICINA_A);

      assertThat(validator.getOficinaIdUsuarioLogado()).isEqualTo(OFICINA_A);
    }

    @Test
    @DisplayName("nega acesso quando o usuário não tem oficina (caso do ADMIN do SaaS)")
    void deveNegarQuandoUsuarioNaoPossuiOficina() {
      when(authenticatedUserProvider.getOficinaIdUsuarioLogado())
          .thenThrow(new UsuarioAcessDeniedException());

      assertThatThrownBy(() -> validator.getOficinaIdUsuarioLogado())
          .isInstanceOf(UsuarioAcessDeniedException.class);
    }
  }

  @Nested
  @DisplayName("validarAcessoOficina")
  class ValidarAcessoOficina {

    @Test
    @DisplayName("permite o GERENTE operar sobre a própria oficina")
    void devePermitirGerenteNaPropriaOficina() {

      logadoComOficina(Role.GERENTE, OFICINA_A);

      assertThatCode(() -> validator.validarAcessoOficina(OFICINA_A)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nega o GERENTE ao tentar operar sobre outra oficina")
    void deveNegarGerenteEmOficinaDeTerceiro() {
      logadoComOficina(Role.GERENTE, OFICINA_A);

      assertThatThrownBy(() -> validator.validarAcessoOficina(OFICINA_B))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("sua própria oficina");
    }

    @Test
    @DisplayName("nega o MECANICO ao tentar operar sobre outra oficina")
    void deveNegarMecanicoEmOficinaDeTerceiro() {
      logado(Role.MECANICO, OFICINA_A);

      assertThatThrownBy(() -> validator.validarAcessoOficina(OFICINA_B))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ADMIN do SaaS pode acessar qualquer oficina")
    void adminPodeAcessarQualquerOficina() {

      logado(Role.ADMIN, null);

      assertThatCode(() -> validator.validarAcessoOficina(OFICINA_B)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("validarAcessoAoRegistro")
  class ValidarAcessoAoRegistro {

    @Test
    @DisplayName("permite quando o registro pertence à oficina do usuário")
    void devePermitirRegistroDaPropriaOficina() {

      logadoComOficina(Role.GERENTE, OFICINA_A);

      assertThatCode(
              () ->
                  validator.validarAcessoAoRegistro(
                      OFICINA_A, new IllegalStateException("não deveria ser lançada")))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName(
        "lança a exceção de 'não encontrado' fornecida quando o registro é de outra oficina")
    void deveLancarNotFoundFornecidoParaRegistroDeOutraOficina() {

      RuntimeException notFound = new IllegalStateException("registro inexistente");

      logado(Role.GERENTE, OFICINA_A);

      assertThatThrownBy(() -> validator.validarAcessoAoRegistro(OFICINA_B, notFound))
          .as(
              "Deve devolver 'não encontrado' em vez de 'sem permissão' para não"
                  + " revelar que o registro existe em outra oficina")
          .isSameAs(notFound);
    }

    @Test
    @DisplayName("lança a exceção de 'não encontrado' quando o registro não tem oficina")
    void deveLancarNotFoundQuandoRegistroNaoPossuiOficina() {
      RuntimeException notFound = new IllegalStateException("registro inexistente");

      logado(Role.GERENTE, OFICINA_A);

      assertThatThrownBy(() -> validator.validarAcessoAoRegistro(null, notFound))
          .isSameAs(notFound);
    }

    @Test
    @DisplayName("usa a fábrica por id na sobrecarga com Function")
    void deveUsarFabricaPorIdNaSobrecarga() {
      logado(Role.GERENTE, OFICINA_A);

      assertThatThrownBy(
              () ->
                  validator.validarAcessoAoRegistro(
                      OFICINA_B, 99L, id -> new IllegalStateException("registro " + id)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("registro 99");
    }

    @Test
    @DisplayName("ADMIN do SaaS pode acessar registro de qualquer oficina")
    void adminPodeAcessarRegistroDeQualquerOficina() {

      logado(Role.ADMIN, null);

      assertThatCode(
              () ->
                  validator.validarAcessoAoRegistro(
                      OFICINA_B, new IllegalStateException("não deveria ser lançada")))
          .doesNotThrowAnyException();
    }

    @Nested
    @DisplayName("getUsuarioAutenticado")
    class GetUsuarioAutenticado {

      @ParameterizedTest
      @EnumSource(Role.class)
      @DisplayName("delega ao AuthenticatedUserProvider para qualquer role")
      void deveDelegarAoProvider(Role role) {
        Usuario esperado = usuario(role, role == Role.ADMIN ? null : OFICINA_A);
        when(authenticatedUserProvider.getUsuarioAutenticado()).thenReturn(esperado);

        assertThat(validator.getUsuarioAutenticado()).isSameAs(esperado);
      }
    }

    @Nested
    @DisplayName("validarOficinaAtiva")
    class ValidarOficinaAtiva {

      private Usuario comOficina(Role role, Boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setUsername("teste");
        usuario.setRole(role);

        Oficina oficina = new Oficina();
        oficina.setId(OFICINA_A);
        oficina.setAtivo(ativo);
        usuario.setOficina(oficina);

        return usuario;
      }

      @Test
      @DisplayName("permite o acesso quando a oficina está ativa")
      void devePermitirQuandoOficinaAtiva() {
        assertThatCode(() -> validator.validarOficinaAtiva(comOficina(Role.GERENTE, true)))
            .doesNotThrowAnyException();
      }

      @ParameterizedTest
      @EnumSource(
          value = Role.class,
          names = {"GERENTE", "MECANICO"})
      @DisplayName("bloqueia qualquer papel de oficina quando ela está desativada")
      void deveBloquearQuandoOficinaDesativada(Role role) {
        assertThatThrownBy(() -> validator.validarOficinaAtiva(comOficina(role, false)))
            .isInstanceOf(OficinaDisabledException.class);
      }

      @Test
      @DisplayName("trata ativo nulo como desativado, em vez de deixar passar")
      void deveTratarAtivoNuloComoDesativado() {
        assertThatThrownBy(() -> validator.validarOficinaAtiva(comOficina(Role.GERENTE, null)))
            .as(
                "Registro legado sem o flag preenchido não pode virar brecha de acesso."
                    + " A implementação usa Boolean.TRUE.equals, que trata null como falso.")
            .isInstanceOf(OficinaDisabledException.class);
      }

      @Test
      @DisplayName("ADMIN do SaaS não é bloqueado: ele não pertence a oficina")
      void adminNaoEhBloqueado() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        admin.setOficina(null);

        assertThatCode(() -> validator.validarOficinaAtiva(admin)).doesNotThrowAnyException();
      }

      @Test
      @DisplayName("usuário de oficina sem vínculo é negado")
      void usuarioDeOficinaSemVinculoEhNegado() {
        Usuario gerente = new Usuario();
        gerente.setId(2L);
        gerente.setUsername("gerente");
        gerente.setRole(Role.GERENTE);
        gerente.setOficina(null);

        assertThatThrownBy(() -> validator.validarOficinaAtiva(gerente))
            .isInstanceOf(UsuarioAcessDeniedException.class);
      }
    }
  }
}
