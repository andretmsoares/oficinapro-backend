package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_os_peca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemOsPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_os_peca_os"))
    private OrdemDeServico ordemDeServico;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;
}