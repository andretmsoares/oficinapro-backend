package com.oficinapro.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.dashboard.DashboardResponseDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.service.dashboard.DashboardService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DashboardService dashboardService;

  @Test
  @DisplayName("GET /api/dashboard/data - GERENTE deve receber 200 com os indicadores")
  @WithMockUser(roles = "GERENTE")
  void deveRetornarIndicadoresParaGerente() throws Exception {
    when(dashboardService.getData())
        .thenReturn(new DashboardResponseDTO(2, 10, 4, new BigDecimal("350.50"), 3));

    mockMvc
        .perform(get("/api/dashboard/data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ordensAbertas").value(2))
        .andExpect(jsonPath("$.veiculosCadastrados").value(10))
        .andExpect(jsonPath("$.clientesCadastrados").value(4))
        .andExpect(jsonPath("$.aReceber").value(350.50))
        .andExpect(jsonPath("$.pagamentosPendentes").value(3));
  }

  @Test
  @DisplayName("GET /api/dashboard/data - MECANICO deve receber 403")
  @WithMockUser(roles = "MECANICO")
  void deveNegarParaMecanico() throws Exception {
    mockMvc.perform(get("/api/dashboard/data")).andExpect(status().isForbidden());

    verifyNoInteractions(dashboardService);
  }

  @Test
  @DisplayName("GET /api/dashboard/data - ADMIN do SaaS não acessa o dashboard da oficina (403)")
  @WithMockUser(roles = "ADMIN")
  void deveNegarParaAdmin() throws Exception {
    mockMvc.perform(get("/api/dashboard/data")).andExpect(status().isForbidden());

    verifyNoInteractions(dashboardService);
  }

  @Test
  @DisplayName("GET /api/dashboard/data - usuário sem oficina propagado pelo service vira 403")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar403QuandoUsuarioNaoTemOficina() throws Exception {
    when(dashboardService.getData()).thenThrow(new UsuarioAcessDeniedException());

    mockMvc.perform(get("/api/dashboard/data")).andExpect(status().isForbidden());
  }
}
