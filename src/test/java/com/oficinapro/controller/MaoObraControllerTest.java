package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.mao_obra.MaoObraNotFoundException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.service.mao_obra.MaoObraService;
import java.math.BigDecimal;
import java.util.List;
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
import tools.jackson.databind.ObjectMapper;

/** Contrato HTTP e autorização do módulo de mão de obra, criado no refactor. */
@WebMvcTest(MaoObraController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class MaoObraControllerTest {

  private static final Long OS_ID = 100L;
  private static final Long MAO_OBRA_ID = 7L;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MaoObraService maoObraService;

  private MaoObraResponseDTO responseDTO() {
    return new MaoObraResponseDTO(MAO_OBRA_ID, OS_ID, new BigDecimal("250.00"), "Revisão geral");
  }

  private String json(Object body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  private MaoObraRequestDTO requestValido() {
    return new MaoObraRequestDTO(OS_ID, new BigDecimal("250.00"), "Revisão geral");
  }

  // ---------------------------------------------------------
  // POST /api/mao-obra
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/mao-obra - GERENTE cria mão de obra e recebe 201")
  void criar_gerente_retorna201() throws Exception {
    when(maoObraService.criar(any())).thenReturn(responseDTO());

    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(MAO_OBRA_ID))
        .andExpect(jsonPath("$.osId").value(OS_ID))
        .andExpect(jsonPath("$.valor").value(250.00))
        .andExpect(jsonPath("$.descricao").value("Revisão geral"));
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("POST /api/mao-obra - MECANICO também pode lançar mão de obra")
  void criar_mecanico_retorna201() throws Exception {
    when(maoObraService.criar(any())).thenReturn(responseDTO());

    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/mao-obra - ADMIN do SaaS recebe 403: é dado operacional da oficina")
  void criar_admin_retorna403() throws Exception {
    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isForbidden());

    verify(maoObraService, never()).criar(any());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/mao-obra - campos obrigatórios ausentes retornam 400")
  void criar_camposObrigatoriosAusentes_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(new MaoObraRequestDTO(null, null, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.fields.osId").exists())
        .andExpect(jsonPath("$.fields.valor").exists())
        .andExpect(jsonPath("$.fields.descricao").exists());

    verify(maoObraService, never()).criar(any());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/mao-obra - OS inexistente ou de outra oficina retorna 404")
  void criar_osInexistente_retorna404() throws Exception {
    when(maoObraService.criar(any())).thenThrow(new OrdemDeServicoNotFoundException(OS_ID));

    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isNotFound());
  }

  @SuppressWarnings("deprecation")
  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/mao-obra - OS cancelada retorna 422")
  void criar_osCancelada_retorna422() throws Exception {
    when(maoObraService.criar(any())).thenThrow(new OSCanceledException());

    mockMvc
        .perform(
            post("/api/mao-obra")
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isUnprocessableEntity());
  }

  // ---------------------------------------------------------
  // GET /api/mao-obra/os/{osId}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("GET /api/mao-obra/os/{osId} - lista a mão de obra da OS")
  void listarPorOs_retorna200() throws Exception {
    when(maoObraService.listarPorOrdemServico(OS_ID)).thenReturn(List.of(responseDTO()));

    mockMvc
        .perform(get("/api/mao-obra/os/" + OS_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(MAO_OBRA_ID))
        .andExpect(jsonPath("$[0].valor").value(250.00));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/mao-obra/os/{osId} - ADMIN recebe 403")
  void listarPorOs_admin_retorna403() throws Exception {
    mockMvc.perform(get("/api/mao-obra/os/" + OS_ID)).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/mao-obra/os/{osId} - osId não numérico retorna 400")
  void listarPorOs_idInvalido_retorna400() throws Exception {
    mockMvc.perform(get("/api/mao-obra/os/abc")).andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------
  // GET /api/mao-obra/{id}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/mao-obra/{id} - encontrada retorna 200")
  void buscarPorId_retorna200() throws Exception {
    when(maoObraService.buscarPorId(MAO_OBRA_ID)).thenReturn(responseDTO());

    mockMvc
        .perform(get("/api/mao-obra/" + MAO_OBRA_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(MAO_OBRA_ID));
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/mao-obra/{id} - inexistente retorna 404")
  void buscarPorId_inexistente_retorna404() throws Exception {
    when(maoObraService.buscarPorId(404L)).thenThrow(new MaoObraNotFoundException(404L));

    mockMvc.perform(get("/api/mao-obra/404")).andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------
  // PUT /api/mao-obra/{id}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("PUT /api/mao-obra/{id} - atualiza e retorna 200")
  void atualizar_retorna200() throws Exception {
    when(maoObraService.atualizar(eq(MAO_OBRA_ID), any())).thenReturn(responseDTO());

    mockMvc
        .perform(
            put("/api/mao-obra/" + MAO_OBRA_ID)
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/mao-obra/{id} - ADMIN recebe 403")
  void atualizar_admin_retorna403() throws Exception {
    mockMvc
        .perform(
            put("/api/mao-obra/" + MAO_OBRA_ID)
                .with(csrf())
                .contentType("application/json")
                .content(json(requestValido())))
        .andExpect(status().isForbidden());

    verify(maoObraService, never()).atualizar(any(), any());
  }

  // ---------------------------------------------------------
  // DELETE /api/mao-obra/{id}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("DELETE /api/mao-obra/{id} - exclui e retorna 204")
  void deletar_retorna204() throws Exception {
    mockMvc
        .perform(delete("/api/mao-obra/" + MAO_OBRA_ID).with(csrf()))
        .andExpect(status().isNoContent());

    verify(maoObraService).deletar(MAO_OBRA_ID);
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName(
      "DELETE /api/mao-obra/{id} - remoção que deixaria a OS abaixo do valor pago retorna 409")
  void deletar_deixariaOsAbaixoDoPago_retorna409() throws Exception {
    org.mockito.Mockito.doThrow(
            new PagamentoValorExcedidoException(new BigDecimal("70.00"), new BigDecimal("100.00")))
        .when(maoObraService)
        .deletar(MAO_OBRA_ID);

    mockMvc
        .perform(delete("/api/mao-obra/" + MAO_OBRA_ID).with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("DELETE /api/mao-obra/{id} - inexistente retorna 404")
  void deletar_inexistente_retorna404() throws Exception {
    org.mockito.Mockito.doThrow(new MaoObraNotFoundException(404L))
        .when(maoObraService)
        .deletar(404L);

    mockMvc.perform(delete("/api/mao-obra/404").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE /api/mao-obra/{id} - ADMIN recebe 403")
  void deletar_admin_retorna403() throws Exception {
    mockMvc
        .perform(delete("/api/mao-obra/" + MAO_OBRA_ID).with(csrf()))
        .andExpect(status().isForbidden());

    verify(maoObraService, never()).deletar(any());
  }
}
