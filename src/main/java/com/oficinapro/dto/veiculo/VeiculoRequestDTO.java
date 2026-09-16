package com.oficinapro.dto.veiculo;

import jakarta.validation.constraints.*;

public record VeiculoRequestDTO(
    @NotBlank(message = "Modelo é obrigatório")
        @Size(max = 100, message = "Modelo deve ter no máximo 100 caracteres")
        String modelo,
    @Min(value = 1900, message = "Ano deve ser maior ou igual a 1900")
    Integer ano,
    @Size(max = 100, message = "Marca deve ter no máximo 100 caracteres") String marca,
    @NotBlank(message = "Placa é obrigatória")
        @Pattern(
            regexp = "^[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}$",
            message = "Placa inválida. Use o formato ABC1234 ou ABC1D23")
        String placa,
    @NotNull(message = "O ID da oficina é obrigatório") Long oficinaId) {}
