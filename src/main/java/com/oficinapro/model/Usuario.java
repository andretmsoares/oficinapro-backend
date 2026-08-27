package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.oficinapro.security.role.Role;

@Entity
@Table(name = "usuario")
@PrimaryKeyJoinColumn(name = "pessoa_id", foreignKey = @ForeignKey(name = "fk_usuario_pessoa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends Pessoa {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Role role;
}
