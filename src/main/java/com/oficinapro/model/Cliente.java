package com.oficinapro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "pessoa_id", foreignKey = @ForeignKey(name = "fk_cliente_pessoa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente extends Pessoa {
}
