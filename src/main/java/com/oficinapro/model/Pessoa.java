package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "pessoa",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pessoa__oficina_doc",
                        columnNames = {"oficina_id", "documento"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", foreignKey = @ForeignKey(name = "fk_pessoa_oficina"))
    private Oficina oficina;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = true, length = 20)
    private String telefone;

    @Column(nullable = true, length = 14)
    private String documento;
}
