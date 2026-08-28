package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "oficina",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_oficina_cnpj",
                        columnNames = "cnpj"
                )
        }
)
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
}