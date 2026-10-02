package com.oficinapro.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

/** Usa o JwtConfig real (e não mocks) para que a fiação encoder/decoder também seja testada. */
class JwtServiceTest {

  private static final String SECRET = "segredo-exclusivo-de-testes-com-mais-de-32-bytes";
  private static final String ISSUER = "oficinapro-test";

  private JwtService jwtService;

  private Usuario gerente;
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

    gerente = new Usuario();
    gerente.setId(42L);
    gerente.setUsername("ana.gerente");
    gerente.setRole(Role.GERENTE);
    gerente.setOficina(oficina);

    adminSaas = new Usuario();
    adminSaas.setId(1L);
    adminSaas.setUsername("admin.saas");
    adminSaas.setRole(Role.ADMIN);
    adminSaas.setOficina(null);
  }

  @Test
  @DisplayName("gerarToken() e decodificar() devem preservar username, role e oficinaId")
  void roundTrip_preservaClaims() {
    Jwt jwt = jwtService.decodificar(jwtService.gerarToken(gerente));

    assertThat(jwt.getSubject()).as("o subject é o id, não o username").isEqualTo("42");
    assertThat(jwt.getClaimAsString("tv")).isEqualTo("0");
    assertThat(jwt.getId()).as("cada token tem um jti próprio").isNotBlank();
    assertThat(jwt.getClaimAsString("role")).isEqualTo("GERENTE");
    assertThat(jwt.getClaim("oficinaId").toString()).isEqualTo("7");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
    assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
  }

  @Test
  @DisplayName("a claim tv acompanha a versão de revogação do usuário")
  void tokenCarregaAVersaoDeRevogacao() {
    gerente.revogarTokens();
    gerente.revogarTokens();

    Jwt jwt = jwtService.decodificar(jwtService.gerarToken(gerente));

    assertThat(jwt.getClaimAsString("tv")).isEqualTo("2");
  }

  @Test
  @DisplayName("dois tokens do mesmo usuário têm jti diferentes")
  void jtiEhUnicoPorToken() {
    String primeiro = jwtService.decodificar(jwtService.gerarToken(gerente)).getId();
    String segundo = jwtService.decodificar(jwtService.gerarToken(gerente)).getId();

    assertThat(primeiro).isNotEqualTo(segundo);
  }

  @Test
  @DisplayName("Token do ADMIN do SaaS não deve conter a claim oficinaId")
  void adminSaas_semClaimOficina() {
    Jwt jwt = jwtService.decodificar(jwtService.gerarToken(adminSaas));

    assertThat(jwt.getSubject()).isEqualTo("1");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    assertThat(jwt.hasClaim("oficinaId")).isFalse();
  }

  @Test
  @DisplayName("Token assinado com outro segredo deve ser rejeitado")
  void assinaturaInvalida_rejeitada() {
    JwtService outroEmissor =
        construir("outro-segredo-de-testes-com-mais-de-32-bytes!!", ISSUER, Duration.ofHours(8));

    String tokenForjado = outroEmissor.gerarToken(gerente);

    assertThatThrownBy(() -> jwtService.decodificar(tokenForjado)).isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("Token de outro issuer deve ser rejeitado")
  void issuerInvalido_rejeitado() {
    JwtService outroIssuer = construir(SECRET, "atacante", Duration.ofHours(8));

    String tokenForjado = outroIssuer.gerarToken(gerente);

    assertThatThrownBy(() -> jwtService.decodificar(tokenForjado)).isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("Token deve possuir expiração")
  void token_devePossuirExpiracao() {
    JwtService emissor = construir(SECRET, ISSUER, Duration.ofSeconds(1));

    String token = emissor.gerarToken(gerente);

    Jwt jwt = emissor.decodificar(token);

    assertThat(jwt.getExpiresAt()).isNotNull();
    assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
  }

  @Test
  @DisplayName("Token expirado deve ser rejeitado")
  void tokenExpirado_rejeitado() {
    // Expirou há 10 min, além da tolerância de 60s do validador padrão do Nimbus.
    JwtProperties properties = new JwtProperties(SECRET, ISSUER, Duration.ofHours(8));
    JwtConfig config = new JwtConfig(properties);
    Instant agora = Instant.now();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject("ana.gerente")
            .issuedAt(agora.minus(Duration.ofMinutes(20)))
            .expiresAt(agora.minus(Duration.ofMinutes(10)))
            .build();

    String tokenExpirado =
        config
            .jwtEncoder(config.jwtSecretKey())
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    assertThatThrownBy(() -> jwtService.decodificar(tokenExpirado))
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
  @DisplayName("Segredo de exemplo ou previsível deve impedir a subida da aplicação")
  void segredoPrevisivel_falhaNaConstrucao() {
    assertThatThrownBy(
            () -> construir("troque-por-uma-chave-aleatoria-de-48-bytes", ISSUER, Duration.ZERO))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exemplo");

    assertThatThrownBy(() -> construir("a".repeat(40), ISSUER, Duration.ZERO))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("previsível");
  }

  @Test
  @DisplayName("Sem expiração configurada, o token deve valer 8 horas")
  void expiracaoNaoConfigurada_usaOitoHorasComoPadrao() {
    JwtService emissor = construir(SECRET, ISSUER, null);

    Jwt jwt = emissor.decodificar(emissor.gerarToken(gerente));

    assertThat(emissor.expiracao()).isEqualTo(Duration.ofHours(8));
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofHours(8));
  }

  @Test
  @DisplayName("Segredo em branco deve impedir a subida da aplicação")
  void segredoEmBranco_falhaNaConstrucao() {
    assertThatThrownBy(() -> construir("   ", ISSUER, Duration.ofHours(8)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  @DisplayName("Segredo ausente deve impedir a subida da aplicação")
  void segredoAusente_falhaNaConstrucao() {
    assertThatThrownBy(() -> construir(null, ISSUER, Duration.ofHours(8)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
  }
}
