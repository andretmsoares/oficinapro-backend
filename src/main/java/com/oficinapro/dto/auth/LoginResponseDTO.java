package com.oficinapro.dto.auth;

import com.oficinapro.dto.usuario.UsuarioResponseDTO;

/**
 * @param accessToken JWT a ser enviado em {@code Authorization: Bearer <token>}
 * @param expiresIn   validade do token em segundos
 * @param usuario     dados do usuário autenticado, para o frontend não precisar
 *                    chamar /api/auth/me logo após o login
 */
public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponseDTO usuario
) {
    public static LoginResponseDTO bearer(String accessToken, long expiresIn, UsuarioResponseDTO usuario) {
        return new LoginResponseDTO(accessToken, "Bearer", expiresIn, usuario);
    }
}
