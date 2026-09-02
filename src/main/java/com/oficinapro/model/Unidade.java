package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "oficina_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_unidade_oficina")
    )
    private Oficina oficina;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 255)
    private String endereco;

    @Column(length = 20)
    private String telefone;

    public Unidade() {
    }

    public Unidade(Oficina oficina, String nome, String endereco, String telefone) {
        this.oficina = oficina;
        this.nome = nome;
        this.endereco = endereco;
        this.telefone = telefone;
    }
}