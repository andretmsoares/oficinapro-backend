package com.oficinapro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.oficinapro.security.role.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuario")
@PrimaryKeyJoinColumn(name = "pessoa_id", foreignKey = @ForeignKey(name = "fk_usuario_pessoa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends Pessoa implements UserDetails {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * O prefixo "ROLE_" é o que faz {@code hasRole('ADMIN')} nos @PreAuthorize funcionar.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** ADMIN do SaaS não pertence a nenhuma oficina. */
    public boolean isAdminSaas() {
        return role == Role.ADMIN;
    }
}
