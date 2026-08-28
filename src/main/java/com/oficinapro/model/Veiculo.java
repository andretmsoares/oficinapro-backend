package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "veiculo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_veiculo_placa_oficina",
                        columnNames = {"oficina_id", "placa"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oficina_id", nullable = false, foreignKey = @ForeignKey(name = "fk_veiculo_oficina"))
    private Oficina oficina;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column
    private Integer ano;

    @Column(length = 100)
    private String marca;

    @Column(nullable = false, length = 10)
    private String placa;
}