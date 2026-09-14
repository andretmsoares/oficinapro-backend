package com.oficinapro.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "Username é obrigatório")
        @Size(max = 100, message = "Username deve ter no máximo 100 caracteres")
        String username,

        @NotBlank(message = "Password é obrigatório")
        @Size(min = 5, max = 255, message = "Password deve ter entre 8 e 255 caracteres")
        String password
) {}
