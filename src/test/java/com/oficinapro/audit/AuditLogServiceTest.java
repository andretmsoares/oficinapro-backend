package com.oficinapro.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  @Mock private AuditLogRepository repository;

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  private static Usuario gerente() {
    Oficina oficina = new Oficina();
    oficina.setId(7L);
    Usuario u = new Usuario();
    u.setId(3L);
    u.setRole(Role.GERENTE);
    u.setOficina(oficina);
    return u;
  }

  private AuditLog salvo() {
    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("registrar() usa o usuário autenticado como ator, com oficina e papel")
  void registraComAtorDoContexto() {
    Usuario ator = gerente();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(ator, null, ator.getAuthorities()));

    new AuditLogService(repository, false)
        .registrar(AcaoAuditoria.USUARIO_EXCLUIDO, "USUARIO", 9L, "role=MECANICO");

    AuditLog registro = salvo();
    assertThat(registro.getAcao()).isEqualTo(AcaoAuditoria.USUARIO_EXCLUIDO);
    assertThat(registro.getAtorId()).isEqualTo(3L);
    assertThat(registro.getAtorRole()).isEqualTo("GERENTE");
    assertThat(registro.getOficinaId()).isEqualTo(7L);
    assertThat(registro.getAlvoTipo()).isEqualTo("USUARIO");
    assertThat(registro.getAlvoId()).isEqualTo(9L);
    assertThat(registro.getDetalhe()).isEqualTo("role=MECANICO");
    assertThat(registro.getOcorridoEm()).isNotNull();
  }

  @Test
  @DisplayName("registrarComoAtor() serve para o login, quando o contexto ainda está vazio")
  void registraComAtorExplicito() {
    new AuditLogService(repository, false)
        .registrarComoAtor(gerente(), AcaoAuditoria.LOGIN_SUCESSO, "USUARIO", 3L, null);

    AuditLog registro = salvo();
    assertThat(registro.getAtorId()).isEqualTo(3L);
    assertThat(registro.getAcao()).isEqualTo(AcaoAuditoria.LOGIN_SUCESSO);
  }

  @Test
  @DisplayName("sem usuário no contexto o registro é gravado sem ator")
  void semAtor() {
    new AuditLogService(repository, false).registrar(AcaoAuditoria.LOGOUT, "USUARIO", 1L, null);

    AuditLog registro = salvo();
    assertThat(registro.getAtorId()).isNull();
    assertThat(registro.getAtorRole()).isNull();
  }

  @Test
  @DisplayName("grava o IP da requisição em andamento")
  void gravaOIp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.4");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    new AuditLogService(repository, false).registrar(AcaoAuditoria.LOGOUT, "USUARIO", 1L, null);

    assertThat(salvo().getIp()).isEqualTo("203.0.113.4");
  }

  @Test
  @DisplayName("detalhe maior que 500 caracteres é truncado para caber na coluna")
  void truncaDetalhe() {
    new AuditLogService(repository, false)
        .registrar(AcaoAuditoria.USUARIO_ALTERADO, "USUARIO", 1L, "x".repeat(900));

    assertThat(salvo().getDetalhe()).hasSize(500);
  }
}
