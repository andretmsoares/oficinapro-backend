package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "mecanico")
@PrimaryKeyJoinColumn(name = "pessoa_id", foreignKey = @ForeignKey(name = "fk_mecanico_pessoa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mecanico extends Pessoa {

    @Column(precision = 12, scale = 2)
    private BigDecimal salario;

    @Column(columnDefinition = "TEXT")
    private String obs;
}
