package com.oficinapro.security.jwt;

import com.oficinapro.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emissão e leitura dos access tokens (JWT assinado com HS256).
 *
 * As claims "role" e "oficinaId" existem apenas para conveniência do frontend.
 * A autorização em si nunca usa esses valores: o {@link JwtAuthenticationFilter}
 * recarrega o usuário do banco a cada requisição, de modo que troca de cargo,
 * troca de oficina ou exclusão do usuário passam a valer imediatamente, sem
 * precisar esperar o token expirar.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao()))
                .subject(usuario.getUsername())
                .claim("role", usuario.getRole().name());

        // O ADMIN do SaaS não pertence a nenhuma oficina, então a claim fica ausente.
        if (usuario.getOficina() != null) {
            claims.claim("oficinaId", usuario.getOficina().getId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
    }

    /**
     * Valida assinatura, issuer e validade do token.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException se o token for inválido
     */
    public Jwt decodificar(String token) {
        return decoder.decode(token);
    }

    public Duration expiracao() {
        return properties.expiration() != null
                ? properties.expiration()
                : Duration.ofHours(8);
    }
}
