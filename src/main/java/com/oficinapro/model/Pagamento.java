package com.oficinapro.model;

import com.oficinapro.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "os_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pagamento_os")
    )
    private OrdemDeServico ordemDeServico;

    @Column(name = "valor_pago", precision = 12, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "obs")
    private String obs;

    @Column(name = "data_pagamento_total")
    private LocalDateTime dataPagamentoTotal;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusPagamento status;
}
