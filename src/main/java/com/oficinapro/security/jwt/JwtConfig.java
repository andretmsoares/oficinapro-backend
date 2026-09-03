package com.oficinapro.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private final JwtProperties properties;

    public JwtConfig(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * Falha na subida da aplicação (e não no primeiro login) caso o segredo esteja
     * ausente ou curto demais para o HS256.
     */
    @Bean
    public SecretKey jwtSecretKey() {
        String secret = properties.secret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "oficinapro.jwt.secret não configurado. Defina a variável de ambiente JWT_SECRET.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < JwtProperties.MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "oficinapro.jwt.secret precisa ter no mínimo %d bytes para HS256 (atual: %d)."
                            .formatted(JwtProperties.MIN_SECRET_LENGTH, keyBytes.length));
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Além de assinatura e validade, exige que a claim "iss" seja a nossa.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));

        return decoder;
    }
}
