package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "oficina",
    uniqueConstraints = {@UniqueConstraint(name = "uq_oficina_cnpj", columnNames = "cnpj")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Oficina {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String nome;

  @Column(nullable = false, length = 14)
  private String cnpj;

  @Column(length = 20)
  private String telefone;

  @Column(nullable = false)
  private Boolean ativo = true;

  /** Caminho do objeto da logo no bucket; nulo = usa a logo padrão do sistema. */
  @Column(name = "logo_path", length = 255)
  private String logoPath;

  public Oficina(Long id, String nome, String cnpj, String telefone, Boolean ativo) {
    this(id, nome, cnpj, telefone, ativo, null);
  }
}
