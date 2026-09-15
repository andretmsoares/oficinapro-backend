package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.service.item_os_peca.ItemOsPecaService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ItemOsPecaController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class ItemOsPecaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ItemOsPecaService itemOsPecaService;

  private ItemOsPecaResponseDTO responseDTO;
  private ItemOsPecaRequestDTO requestDTO;
  private ItemOsPecaUpdateRequestDTO updateRequestDTO;

  @BeforeEach
  void setUp() {
    responseDTO =
        new ItemOsPecaResponseDTO(
            1L,
            1L,
            "Filtro de óleo",
            BigDecimal.valueOf(2),
            BigDecimal.valueOf(45.00),
            BigDecimal.valueOf(90.00));
    requestDTO =
        new ItemOsPecaRequestDTO(
            1L, "Filtro de óleo", BigDecimal.valueOf(2), BigDecimal.valueOf(45.00));
    updateRequestDTO =
        new ItemOsPecaUpdateRequestDTO(
            "Filtro de óleo", BigDecimal.valueOf(2), BigDecimal.valueOf(45.00));
  }

  // ─── GET /api/itens-os-peca/os/{osId} ────────────────────────────────────────

  @Test
  @DisplayName("GET /api/itens-os-peca/os/1 - GERENTE deve retornar 200 com lista de itens da OS")
  @WithMockUser(roles = "GERENTE")
  void deveListarItensPorOsComoGerente() throws Exception {
    when(itemOsPecaService.listarPorOrdemServico(1L)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/itens-os-peca/os/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].nome").value("Filtro de óleo"))
        .andExpect(jsonPath("$[0].osId").value(1));
  }

  // ─── GET /api/itens-os-peca/{id} ─────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/itens-os-peca/{id} - MECANICO deve retornar 200 com o item correto")
  @WithMockUser(roles = "MECANICO")
  void deveBuscarItemPorIdComoMecanico() throws Exception {
    when(itemOsPecaService.buscarPorId(1L)).thenReturn(responseDTO);

    mockMvc
        .perform(get("/api/itens-os-peca/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("Filtro de óleo"));
  }

  // ─── POST /api/itens-os-peca ──────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/itens-os-peca - GERENTE deve criar item e retornar 201")
  @WithMockUser(roles = "GERENTE")
  void deveCriarItem() throws Exception {
    when(itemOsPecaService.criar(any(ItemOsPecaRequestDTO.class))).thenReturn(responseDTO);

    mockMvc
        .perform(
            post("/api/itens-os-peca")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("Filtro de óleo"));
  }

  @Test
  @DisplayName("POST /api/itens-os-peca - Deve retornar 400 com osId nulo (validação)")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ComOsIdNulo() throws Exception {
    ItemOsPecaRequestDTO requestInvalido =
        new ItemOsPecaRequestDTO(
            null, "Filtro de óleo", BigDecimal.valueOf(2), BigDecimal.valueOf(45.00));

    mockMvc
        .perform(
            post("/api/itens-os-peca")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
        .andExpect(status().isBadRequest());
  }

  // ─── PUT /api/itens-os-peca/{id} ─────────────────────────────────────────────

  @Test
  @DisplayName("PUT /api/itens-os-peca/{id} - MECANICO deve atualizar item e retornar 200")
  @WithMockUser(roles = "MECANICO")
  void deveAtualizarItemComoMecanico() throws Exception {
    when(itemOsPecaService.atualizar(eq(1L), any(ItemOsPecaUpdateRequestDTO.class)))
        .thenReturn(responseDTO);

    mockMvc
        .perform(
            put("/api/itens-os-peca/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("Filtro de óleo"));
  }

  // ─── DELETE /api/itens-os-peca/{id} ──────────────────────────────────────────

  @Test
  @DisplayName("DELETE /api/itens-os-peca/{id} - GERENTE deve excluir item e retornar 204")
  @WithMockUser(roles = "GERENTE")
  void deveDeletarItem() throws Exception {
    doNothing().when(itemOsPecaService).deletar(1L);

    mockMvc.perform(delete("/api/itens-os-peca/1").with(csrf())).andExpect(status().isNoContent());
  }
}
