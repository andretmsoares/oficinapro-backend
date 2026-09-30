package com.oficinapro.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.estatisticas.EstatisticasOficinaResponseDTO;
import com.oficinapro.dto.estatisticas.EstatisticasSistemaResponseDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.service.estatisticas.EstatisticasService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(EstatisticasController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class EstatisticasControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EstatisticasService estatisticasService;

  private EstatisticasOficinaResponseDTO oficina;

  @BeforeEach
  void setUp() {
    oficina =
        new EstatisticasOficinaResponseDTO(
            1L, "Oficina Central", "12345678000195", "83999998888", true, 5, 3, 8, 12);
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas - ADMIN deve receber 200 com totais e lista por oficina")
  @WithMockUser(roles = "ADMIN")
  void deveRetornarResumoDoSistema() throws Exception {
    when(estatisticasService.resumoDoSistema())
        .thenReturn(new EstatisticasSistemaResponseDTO(1, 5, 3, 8, 12, List.of(oficina)));

    mockMvc
        .perform(get("/api/admin/estatisticas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.oficinas").value(1))
        .andExpect(jsonPath("$.clientes").value(5))
        .andExpect(jsonPath("$.mecanicos").value(3))
        .andExpect(jsonPath("$.veiculos").value(8))
        .andExpect(jsonPath("$.ordensDeServico").value(12))
        .andExpect(jsonPath("$.porOficina[0].nome").value("Oficina Central"))
        .andExpect(jsonPath("$.porOficina[0].ordensDeServico").value(12));
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas - GERENTE deve receber 403")
  @WithMockUser(roles = "GERENTE")
  void deveNegarResumoDoSistemaParaGerente() throws Exception {
    mockMvc.perform(get("/api/admin/estatisticas")).andExpect(status().isForbidden());

    verifyNoInteractions(estatisticasService);
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas/oficina/{id} - ADMIN deve receber 200")
  @WithMockUser(roles = "ADMIN")
  void deveRetornarResumoDaOficina() throws Exception {
    when(estatisticasService.resumoDaOficina(1L)).thenReturn(oficina);

    mockMvc
        .perform(get("/api/admin/estatisticas/oficina/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.cnpj").value("12345678000195"))
        .andExpect(jsonPath("$.ativo").value(true))
        .andExpect(jsonPath("$.clientes").value(5))
        .andExpect(jsonPath("$.veiculos").value(8));
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas/oficina/{id} - oficina inexistente deve retornar 404")
  @WithMockUser(roles = "ADMIN")
  void deveRetornar404ParaOficinaInexistente() throws Exception {
    when(estatisticasService.resumoDaOficina(99L)).thenThrow(new OficinaNotFoundException(99L));

    mockMvc.perform(get("/api/admin/estatisticas/oficina/99")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas/oficina/{id} - MECANICO deve receber 403")
  @WithMockUser(roles = "MECANICO")
  void deveNegarResumoDaOficinaParaMecanico() throws Exception {
    mockMvc.perform(get("/api/admin/estatisticas/oficina/1")).andExpect(status().isForbidden());

    verifyNoInteractions(estatisticasService);
  }

  @Test
  @DisplayName("GET /api/admin/estatisticas/oficina/{id} - id não numérico deve retornar 400")
  @WithMockUser(roles = "ADMIN")
  void deveRetornar400ParaIdNaoNumerico() throws Exception {
    mockMvc.perform(get("/api/admin/estatisticas/oficina/abc")).andExpect(status().isBadRequest());

    verifyNoInteractions(estatisticasService);
  }
}
