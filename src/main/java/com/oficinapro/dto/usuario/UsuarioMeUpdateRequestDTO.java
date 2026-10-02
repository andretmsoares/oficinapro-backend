package com.oficinapro.dto.usuario;

import com.oficinapro.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param senhaAtual obrigatória para trocar a senha ou o username: sem ela, quem roubasse um token
 *     assumiria a conta de forma permanente. Validada no service.
 * @param password nova senha (opcional); segue a mesma política de senha dos demais cadastros.
 */
public record UsuarioMeUpdateRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,
    @NotBlank(message = "O documento é obrigatório")
        @Size(max = 14, message = "Documento deve ter no máximo 14 caracteres")
        String documento,
    @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,
    @NotBlank(message = "O username é obrigatório")
        @Size(max = 100, message = "Username deve ter no máximo 100 caracteres")
        String username,
    @SenhaForte String password,
    @Size(max = 255, message = "Senha atual deve ter no máximo 255 caracteres")
        String senhaAtual) {}
