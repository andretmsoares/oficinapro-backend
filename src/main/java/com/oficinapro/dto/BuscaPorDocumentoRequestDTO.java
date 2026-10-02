package com.oficinapro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo das buscas por documento. O CPF/CNPJ trafega no corpo (POST) e não na URL, porque URLs vão
 * parar em logs de acesso, proxies e histórico do navegador.
 */
public record BuscaPorDocumentoRequestDTO(
    @NotBlank(message = "Documento é obrigatório")
        @Size(max = 14, message = "Documento deve ter no máximo 14 caracteres")
        String documento) {}
