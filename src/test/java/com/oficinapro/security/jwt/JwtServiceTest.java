package com.oficinapro.security.jwt;

import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.role.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Usa o JwtConfig real (e não mocks) para que a fiação encoder/decoder também seja testada.
 */
class JwtServiceTest {

    private static final String SECRET = "segredo-exclusivo-de-testes-com-mais-de-32-bytes";
    private static final String ISSUER = "oficinapro-test";

    private JwtService jwtService;

    private Usuario administrativo;
    private Usuario adminSaas;

    private static JwtService construir(String secret, String issuer, Duration expiration) {
        JwtProperties properties = new JwtProperties(secret, issuer, expiration);
        JwtConfig config = new JwtConfig(properties);
        SecretKey key = config.jwtSecretKey();
        return new JwtService(config.jwtEncoder(key), config.jwtDecoder(key), properties);
    }

    @BeforeEach
    void setUp() {
        jwtService = construir(SECRET, ISSUER, Duration.ofHours(8));

        Oficina oficina = new Oficina();
        oficina.setId(7L);

        administrativo = new Usuario();
        administrativo.setUsername("ana.administrativo");
        administrativo.setRole(Role.ADMINISTRATIVO);
        administrativo.setOficina(oficina);

        adminSaas = new Usuario();
        adminSaas.setUsername("admin.saas");
        adminSaas.setRole(Role.ADMIN);
        adminSaas.setOficina(null);
    }

    @Test
    @DisplayName("gerarToken() e decodificar() devem preservar username, role e oficinaId")
    void roundTrip_preservaClaims() {
        Jwt jwt = jwtService.decodificar(jwtService.gerarToken(administrativo));

        assertThat(jwt.getSubject()).isEqualTo("ana.administrativo");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMINISTRATIVO");
        assertThat(jwt.getClaim("oficinaId").toString()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("Token do ADMIN do SaaS não deve conter a claim oficinaId")
    void adminSaas_semClaimOficina() {
        Jwt jwt = jwtService.decodificar(jwtService.gerarToken(adminSaas));

        assertThat(jwt.getSubject()).isEqualTo("admin.saas");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.hasClaim("oficinaId")).isFalse();
    }

    @Test
    @DisplayName("Token assinado com outro segredo deve ser rejeitado")
    void assinaturaInvalida_rejeitada() {
        JwtService outroEmissor =
                construir("outro-segredo-de-testes-com-mais-de-32-bytes!!", ISSUER, Duration.ofHours(8));

        String tokenForjado = outroEmissor.gerarToken(administrativo);

        assertThatThrownBy(() -> jwtService.decodificar(tokenForjado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token de outro issuer deve ser rejeitado")
    void issuerInvalido_rejeitado() {
        JwtService outroIssuer =
                construir(SECRET, "atacante", Duration.ofHours(8));

        String tokenForjado = outroIssuer.gerarToken(administrativo);

        assertThatThrownBy(() -> jwtService.decodificar(tokenForjado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token expirado deve ser rejeitado")
    void tokenExpirado_rejeitado() throws InterruptedException {
        JwtService emissor =
                construir(SECRET, ISSUER, Duration.ofMillis(100));

        String token = emissor.gerarToken(administrativo);

        Thread.sleep(200);

        assertThatThrownBy(() -> jwtService.decodificar(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Segredo curto demais para HS256 deve impedir a subida da aplicação")
    void segredoCurto_falhaNaConstrucao() {
        assertThatThrownBy(() -> construir("curto", ISSUER, Duration.ofHours(8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("Segredo ausente deve impedir a subida da aplicação")
    void segredoAusente_falhaNaConstrucao() {
        assertThatThrownBy(() -> construir(null, ISSUER, Duration.ofHours(8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
