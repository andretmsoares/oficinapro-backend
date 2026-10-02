package com.oficinapro.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O tamanho mínimo da senha NÃO é validado aqui de propósito: responder 400 com a regra de senha
 * antes de autenticar vazaria a política e distinguiria entradas inválidas de credenciais erradas.
 * A política é aplicada só quando a senha é definida.
 */
public record LoginRequestDTO(
    @NotBlank(message = "Username é obrigatório")
        @Size(max = 100, message = "Username deve ter no máximo 100 caracteres")
        String username,
    @NotBlank(message = "Password é obrigatório")
        @Size(max = 255, message = "Password deve ter no máximo 255 caracteres")
        String password) {}
