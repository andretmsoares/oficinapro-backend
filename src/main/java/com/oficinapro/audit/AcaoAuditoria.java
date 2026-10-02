package com.oficinapro.audit;

/** Eventos de segurança e financeiros que ficam registrados na tabela {@code audit_log}. */
public enum AcaoAuditoria {
  LOGIN_SUCESSO,
  LOGOUT,
  SENHA_ALTERADA,
  USUARIO_CRIADO,
  USUARIO_ALTERADO,
  USUARIO_EXCLUIDO,
  USUARIO_DESBLOQUEADO,
  OFICINA_ATIVADA,
  OFICINA_DESATIVADA,
  OS_EXCLUIDA,
  DESCONTO_APLICADO,
  PAGAMENTO_REGISTRADO,
  PAGAMENTO_ESTORNADO
}
