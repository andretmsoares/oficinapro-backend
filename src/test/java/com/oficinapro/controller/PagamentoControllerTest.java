package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.pagamento.PagamentoAlreadyExistsException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.service.pagamento.PagamentoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

/**
 * Autorização e contrato HTTP dos endpoints de pagamento.
 *
 * <p>Atenção ao que mudou: {@code PagamentoRequestDTO} passou a ter apenas {@code (osId, obs)} — o
 * valor pago não é mais aceito pelo corpo. E o ADMIN do SaaS deixou de ter acesso a este módulo:
 * todos os endpoints são de GERENTE, exceto a consulta por status, liberada também ao MECANICO.
 */
@WebMvcTest(PagamentoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class PagamentoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PagamentoService pagamentoService;

  private PagamentoResponseDTO responseDTO() {
    return new PagamentoResponseDTO(
        10L,
        1L,
        new BigDecimal("100.00"),
        "obs",
        LocalDateTime.now(),
        StatusPagamento.PAGAMENTO_PENDENTE);
  }

  private String json(Object body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  // ---------------------------------------------------------
  // POST /api/pagamentos
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/pagamentos - GERENTE cria pagamento e recebe 201")
  void criar_gerente_retorna201() throws Exception {
    when(pagamentoService.criar(any())).thenReturn(responseDTO());

    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.osId").value(1))
        .andExpect(jsonPath("$.status").value("PAGAMENTO_PENDENTE"));
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("POST /api/pagamentos - MECANICO recebe 403")
  void criar_mecanico_retorna403() throws Exception {
    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isForbidden());

    verify(pagamentoService, never()).criar(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/pagamentos - ADMIN do SaaS recebe 403: pagamento é dado da oficina")
  void criar_admin_retorna403() throws Exception {
    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isForbidden());

    verify(pagamentoService, never()).criar(any());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/pagamentos - osId ausente recebe 400 com detalhe do campo")
  void criar_semOsId_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(null, "obs"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.osId").exists());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/pagamentos - segundo pagamento para a mesma OS recebe 409")
  void criar_pagamentoDuplicado_retorna409() throws Exception {
    when(pagamentoService.criar(any())).thenThrow(new PagamentoAlreadyExistsException());

    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("POST /api/pagamentos - corpo JSON malformado recebe 400")
  void criar_corpoInvalido_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/pagamentos")
                .with(csrf())
                .contentType("application/json")
                .content("{ isso nao e json }"))
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------
  // GET /api/pagamentos/{id}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/{id} - encontrado retorna 200")
  void buscarPorId_encontrado_retorna200() throws Exception {
    when(pagamentoService.buscarPorId(10L)).thenReturn(responseDTO());

    mockMvc
        .perform(get("/api/pagamentos/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.valorPago").value(100.00));
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/{id} - inexistente ou de outra oficina retorna 404")
  void buscarPorId_naoEncontrado_retorna404() throws Exception {
    when(pagamentoService.buscarPorId(999L)).thenThrow(new PagamentoNotFoundException(999L));

    mockMvc.perform(get("/api/pagamentos/999")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("GET /api/pagamentos/{id} - MECANICO recebe 403")
  void buscarPorId_mecanico_retorna403() throws Exception {
    mockMvc.perform(get("/api/pagamentos/10")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/{id} - id não numérico retorna 400")
  void buscarPorId_idInvalido_retorna400() throws Exception {
    mockMvc.perform(get("/api/pagamentos/abc")).andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------
  // GET /api/pagamentos/os/{osId}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/os/{osId} - retorna o pagamento da OS")
  void buscarPorOsId_retorna200() throws Exception {
    when(pagamentoService.buscarPorOsId(1L)).thenReturn(responseDTO());

    mockMvc
        .perform(get("/api/pagamentos/os/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.osId").value(1));
  }

  // ---------------------------------------------------------
  // GET /api/pagamentos/oficina/**
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/oficina/{id} - GERENTE lista pagamentos da oficina")
  void buscarPorOficina_gerente_retorna200() throws Exception {
    when(pagamentoService.buscarPorOficina(1L)).thenReturn(java.util.List.of(responseDTO()));

    mockMvc
        .perform(get("/api/pagamentos/oficina/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10));
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("GET /api/pagamentos/oficina/{id} - MECANICO recebe 403")
  void buscarPorOficina_mecanico_retorna403() throws Exception {
    mockMvc.perform(get("/api/pagamentos/oficina/1")).andExpect(status().isForbidden());

    verify(pagamentoService, never()).buscarPorOficina(any());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/oficina/{id}/status/{status} - status inexistente retorna 400")
  void buscarPorStatus_statusInvalido_retorna400() throws Exception {
    mockMvc
        .perform(get("/api/pagamentos/oficina/1/status/NAO_EXISTE"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/pagamentos/oficina/{id}/a-receber - retorna o valor pendente")
  void calcularValorParaReceber_retorna200() throws Exception {
    when(pagamentoService.calcularValorParaReceber(1L)).thenReturn(new BigDecimal("1500.00"));

    mockMvc
        .perform(get("/api/pagamentos/oficina/1/a-receber"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(1500.00));
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("GET /api/pagamentos/oficina/{id}/a-receber - MECANICO recebe 403")
  void calcularValorParaReceber_mecanico_retorna403() throws Exception {
    mockMvc.perform(get("/api/pagamentos/oficina/1/a-receber")).andExpect(status().isForbidden());

    verify(pagamentoService, never()).calcularValorParaReceber(any());
  }

  // ---------------------------------------------------------
  // PUT /api/pagamentos/{id}
  // ---------------------------------------------------------

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("PUT /api/pagamentos/{id} - atualiza a observação e retorna 200")
  void atualizar_retorna200() throws Exception {
    when(pagamentoService.atualizar(eq(10L), any())).thenReturn(responseDTO());

    mockMvc
        .perform(
            put("/api/pagamentos/10")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "novo obs"))))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("PUT /api/pagamentos/{id} - valor excedido retorna 409")
  void atualizar_valorExcedido_retorna409() throws Exception {
    when(pagamentoService.atualizar(eq(10L), any()))
        .thenThrow(
            new PagamentoValorExcedidoException(
                new BigDecimal("200.00"), new BigDecimal("100.00")));

    mockMvc
        .perform(
            put("/api/pagamentos/10")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("PUT /api/pagamentos/{id} - MECANICO recebe 403")
  void atualizar_mecanico_retorna403() throws Exception {
    mockMvc
        .perform(
            put("/api/pagamentos/10")
                .with(csrf())
                .contentType("application/json")
                .content(json(new PagamentoRequestDTO(1L, "obs"))))
        .andExpect(status().isForbidden());
  }
}
