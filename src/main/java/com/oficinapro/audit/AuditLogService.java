package com.oficinapro.audit;

import com.oficinapro.model.Usuario;
import com.oficinapro.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registra eventos de segurança/financeiros na tabela {@code audit_log} e no log estruturado.
 *
 * <p>Roda na transação do chamador: se a operação de negócio falhar e der rollback, o registro de
 * auditoria some junto, então a trilha nunca afirma algo que não aconteceu. Não recebe nem grava
 * dados pessoais: só ids, o papel do ator e um detalhe curto montado pelo sistema.
 */
@Service
public class AuditLogService {

  private static final Logger log = LoggerFactory.getLogger("AUDIT");

  private final AuditLogRepository repository;
  private final boolean confiarEmHeadersDeProxy;

  public AuditLogService(
      AuditLogRepository repository,
      @Value("${oficinapro.security.trust-proxy-headers:false}") boolean confiarEmHeadersDeProxy) {
    this.repository = repository;
    this.confiarEmHeadersDeProxy = confiarEmHeadersDeProxy;
  }

  /** Registra a ação feita pelo usuário autenticado no contexto de segurança. */
  @Transactional
  public void registrar(AcaoAuditoria acao, String alvoTipo, Long alvoId, String detalhe) {
    registrarComoAtor(atorDoContexto(), acao, alvoTipo, alvoId, detalhe);
  }

  /** Para eventos em que ainda não há usuário no contexto (ex.: o próprio login). */
  @Transactional
  public void registrarComoAtor(
      Usuario ator, AcaoAuditoria acao, String alvoTipo, Long alvoId, String detalhe) {
    AuditLog registro = new AuditLog();
    registro.setOcorridoEm(LocalDateTime.now());
    registro.setAcao(acao);
    registro.setAlvoTipo(alvoTipo);
    registro.setAlvoId(alvoId);
    registro.setDetalhe(truncar(detalhe, 500));
    registro.setIp(ipAtual());

    if (ator != null) {
      registro.setAtorId(ator.getId());
      registro.setAtorRole(ator.getRole() != null ? ator.getRole().name() : null);
      registro.setOficinaId(ator.getOficina() != null ? ator.getOficina().getId() : null);
    }

    repository.save(registro);

    log.info(
        "acao={} ator={} role={} oficina={} alvo={}:{} ip={}",
        acao,
        registro.getAtorId(),
        registro.getAtorRole(),
        registro.getOficinaId(),
        alvoTipo,
        alvoId,
        registro.getIp());
  }

  private Usuario atorDoContexto() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
      return usuario;
    }
    return null;
  }

  private String ipAtual() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      HttpServletRequest request = attrs.getRequest();
      return ClientIpResolver.resolver(request, confiarEmHeadersDeProxy);
    }
    return null;
  }

  private static String truncar(String texto, int max) {
    if (texto == null) {
      return null;
    }
    return texto.length() <= max ? texto : texto.substring(0, max);
  }
}
