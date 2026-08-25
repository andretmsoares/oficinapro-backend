package com.oficinapro.model;

import jakarta.persistence.*;

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

    public Oficina() {
    }

    public Oficina(String nome, String cnpj, String telefone) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void setId(long l) {
        this.id = l;
    }
}