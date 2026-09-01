package com.oficinapro.model;

import com.oficinapro.enums.MeioPagamento;
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
public class RegistroPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pagamento_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_registro_pagamento")
    )
    private Pagamento pagamento;

    @Column(name = "valor", precision = 12, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "meio_pagamento", nullable = false)
    private MeioPagamento meio_pagamento;

    @Column(name = "data", nullable = false)
    private LocalDateTime data;
}
