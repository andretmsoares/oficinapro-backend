package com.oficinapro.dto.usuario;

import jakarta.validation.constraints.NotBlank;

public record UsuarioMeUpdateRequestDTO(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O documento é obrigatório") String documento,
    @NotBlank(message = "O telefone é obrigatório") String telefone,
    @NotBlank(message = "O username é obrigatório") String username,
    String password) {}
