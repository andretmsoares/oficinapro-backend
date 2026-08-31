package com.oficinapro.model;

import com.oficinapro.enums.StatusOrdemDeServico;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdemDeServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_oficina"))
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_os_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_veiculo"))
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", foreignKey = @ForeignKey(name = "fk_os_unidade"))
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id", foreignKey = @ForeignKey(name = "fk_os_mecanico"))
    private Mecanico mecanico;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDateTime dataFechamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemDeServico status;

    @Column(columnDefinition = "TEXT")
    private String obs;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;
}
