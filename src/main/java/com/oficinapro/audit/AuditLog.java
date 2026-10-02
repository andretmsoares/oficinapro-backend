package com.oficinapro.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ocorrido_em", nullable = false)
  private LocalDateTime ocorridoEm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AcaoAuditoria acao;

  @Column(name = "ator_id")
  private Long atorId;

  @Column(name = "ator_role", length = 50)
  private String atorRole;

  @Column(name = "oficina_id")
  private Long oficinaId;

  @Column(name = "alvo_tipo", length = 50)
  private String alvoTipo;

  @Column(name = "alvo_id")
  private Long alvoId;

  @Column(length = 500)
  private String detalhe;

  @Column(length = 45)
  private String ip;
}
