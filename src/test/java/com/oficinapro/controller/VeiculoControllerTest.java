package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.service.veiculo.VeiculoService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(VeiculoController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@EnableMethodSecurity
class VeiculoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private VeiculoService veiculoService;

  private VeiculoResponseDTO responseDTO;
  private VeiculoRequestDTO requestDTO;

  @BeforeEach
  void setUp() {
    responseDTO = new VeiculoResponseDTO(1L, 1L, "Civic", 2022, "Honda", "ABC1234");
    requestDTO = new VeiculoRequestDTO("Civic", 2022, "Honda", "ABC1234", 1L);
  }

  // ─── GET /api/veiculos ────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/veiculos - GERENTE deve retornar 200 com página de veículos")
  @WithMockUser(roles = "GERENTE")
  void deveListarVeiculosComoGerente() throws Exception {
    when(veiculoService.listar(any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

    mockMvc
        .perform(get("/api/veiculos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].placa").value("ABC1234"));
  }

  // ─── GET /api/veiculos/{id} ───────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/veiculos/{id} - MECANICO deve retornar 200 (acesso de leitura)")
  @WithMockUser(roles = "MECANICO")
  void deveBuscarVeiculoPorIdComoMecanico() throws Exception {
    when(veiculoService.buscarPorId(1L)).thenReturn(responseDTO);

    mockMvc
        .perform(get("/api/veiculos/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.modelo").value("Civic"))
        .andExpect(jsonPath("$.placa").value("ABC1234"));
  }

  @Test
  @DisplayName("GET /api/veiculos/{id} - Deve retornar 404 quando veículo não existir")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar404AoBuscarVeiculoInexistente() throws Exception {
    when(veiculoService.buscarPorId(99L)).thenThrow(new VeiculoNotFoundException(99L));

    mockMvc.perform(get("/api/veiculos/99")).andExpect(status().isNotFound());
  }

  // ─── POST /api/veiculos ───────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/veiculos - GERENTE deve criar veículo e retornar 201")
  @WithMockUser(roles = "GERENTE")
  void deveCriarVeiculo() throws Exception {
    when(veiculoService.criar(any(VeiculoRequestDTO.class))).thenReturn(responseDTO);

    mockMvc
        .perform(
            post("/api/veiculos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.placa").value("ABC1234"));
  }

  @Test
  @DisplayName("POST /api/veiculos - MECANICO deve criar veículo e retornar 201")
  @WithMockUser(roles = "MECANICO")
  void deveCriarVeiculoParaMecanico() throws Exception {
    mockMvc
        .perform(
            post("/api/veiculos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isCreated());
  }

  // ─── PUT /api/veiculos/{id} ───────────────────────────────────────────────────

  @Test
  @DisplayName("PUT /api/veiculos/{id} - GERENTE deve atualizar veículo e retornar 200")
  @WithMockUser(roles = "GERENTE")
  void deveAtualizarVeiculo() throws Exception {
    when(veiculoService.atualizar(eq(1L), any(VeiculoRequestDTO.class))).thenReturn(responseDTO);

    mockMvc
        .perform(
            put("/api/veiculos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.modelo").value("Civic"));
  }

  // ─── DELETE /api/veiculos/{id} ────────────────────────────────────────────────

  @Test
  @DisplayName("DELETE /api/veiculos/{id} - GERENTE deve excluir veículo e retornar 204")
  @WithMockUser(roles = "GERENTE")
  void deveDeletarVeiculo() throws Exception {
    doNothing().when(veiculoService).deletar(1L);

    mockMvc.perform(delete("/api/veiculos/1").with(csrf())).andExpect(status().isNoContent());
  }
}
