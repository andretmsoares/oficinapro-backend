package com.oficinapro.dto.usuario;

import com.oficinapro.security.role.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO
        (
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
                String nome,

                @Size( max = 20, message = "Telefone deve possuir 20 caracteres")
                String telefone,

                @Size(max = 14, message = "Documento deve ter no máximo 14 caracteres")
                String documento,

                // Obrigatório para ADMINISTRATIVO e MECANICO; deve vir nulo para ADMIN,
                // que é o administrador do SaaS e não pertence a nenhuma oficina.
                // A regra é validada em UsuarioServiceImpl, pois depende da role.
                Long oficinaId,

                @NotBlank
                @Size(max = 100, message = "Username deve ter no máximo 100 caracteres")
                String username,

                @NotBlank
                @Size(min = 8, max = 255, message = "Password deve ter entre 8 e 255 caracteres")
                String password,

                @NotNull(message = "O cargo(Role) é obrigatório")
                Role role

        ) {
}
