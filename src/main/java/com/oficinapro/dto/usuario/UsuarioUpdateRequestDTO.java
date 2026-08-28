package com.oficinapro.dto.usuario;

import com.oficinapro.security.role.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,

        @Size(max = 20, message = "Telefone deve possuir 20 caracteres")
        String telefone,

        @Size(max = 14, message = "Documento deve ter no máximo 14 caracteres")
        String documento,

        @NotNull(message = "O ID da oficina é obrigatório")
        Long oficinaId,

        @NotBlank(message = "Username é obrigatório")
        @Size(max = 100, message = "Username deve ter no máximo 100 caracteres")
        String username,

        // Opcional: se vier null, mantém a senha atual. Se vier preenchida, precisa ter tamanho mínimo.
        @Size(min = 8, max = 255, message = "Password deve ter entre 8 e 255 caracteres")
        String password,

        @NotNull(message = "O cargo(Role) é obrigatório")
        Role role
) {}