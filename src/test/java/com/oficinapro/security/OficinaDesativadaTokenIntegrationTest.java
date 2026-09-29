package com.oficinapro.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.enums.Role;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Token emitido antes da desativacao da oficina nao pode continuar valendo. O usuario e recarregado
 * do banco a cada requisicao, entao a oficina precisa ser conferida no mesmo ponto.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OficinaDesativadaTokenIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private EntityManager entityManager;
  @Autowired private JwtService jwtService;

  private MockMvc mockMvc() {
    return MockMvcBuilders.webAppContextSetup(context)
        .apply(
            org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                .springSecurity())
        .build();
  }

  @Test
  @DisplayName("token de gerente deve parar de valer assim que a oficina e desativada")
  void tokenDeveSerRecusadoAposDesativarOficina() throws Exception {
    Oficina oficina = new Oficina();
    oficina.setNome("OFICINA TOKEN");
    oficina.setCnpj("55555555000155");
    oficina.setAtivo(true);
    entityManager.persist(oficina);

    Usuario gerente = new Usuario();
    gerente.setNome("GERENTE TOKEN");
    gerente.setOficina(oficina);
    gerente.setUsername("gerente.token");
    gerente.setPassword("nao-usada");
    gerente.setRole(Role.GERENTE);
    entityManager.persist(gerente);
    entityManager.flush();
    entityManager.clear();

    String token = jwtService.gerarToken(gerente);
    String header = "Bearer " + token;

    mockMvc()
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, header))
        .andExpect(status().isOk());

    entityManager
        .createQuery("update Oficina o set o.ativo = false where o.id = :id")
        .setParameter("id", oficina.getId())
        .executeUpdate();
    entityManager.clear();

    mockMvc()
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, header))
        .andExpect(status().isUnauthorized());
  }
}
