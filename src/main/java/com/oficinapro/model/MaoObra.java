package com.oficinapro.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mao_obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaoObra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "os_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mao_obra_os"))
  private OrdemDeServico ordemDeServico;

  @Column(name = "valor", precision = 12, scale = 2)
  private BigDecimal valor;

  @Column(name = "descricao")
  private String descricao;
}
