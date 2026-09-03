package com.oficinapro.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do módulo JWT.
 *
 * @param secret     chave secreta usada para assinar os tokens (HS256).
 *                   Precisa ter no mínimo 32 bytes, exigência do HMAC-SHA256.
 * @param issuer     valor da claim "iss", também validado na leitura do token.
 * @param expiration tempo de vida do access token.
 */
@ConfigurationProperties(prefix = "oficinapro.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration expiration
) {
    public static final int MIN_SECRET_LENGTH = 32;
}
