package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.exception.oficina.OficinaAlreadyActivatedException;
import com.oficinapro.exception.oficina.OficinaAlreadyDisabledException;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.service.oficina.OficinaService;
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

@WebMvcTest(OficinaController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class OficinaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OficinaService oficinaService;

  private OficinaResponseDTO oficinaResponseDTO;
  private OficinaRequestDTO oficinaRequestDTO;

  @BeforeEach
  void setUp() {
    oficinaResponseDTO =
        new OficinaResponseDTO(1L, "Oficina Central", "12345678000195", "83999998888", true);
    oficinaRequestDTO = new OficinaRequestDTO("Oficina Central", "12345678000195", "83999998888");
  }

  @Test
  @DisplayName("GET /api/oficinas - Deve retornar status 200 e lista de oficinas")
  @WithMockUser(roles = "ADMIN")
  void deveListarOficinas() throws Exception {
    when(oficinaService.listar()).thenReturn(List.of(oficinaResponseDTO));

    mockMvc
        .perform(get("/api/oficinas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].nome").value("Oficina Central"));
  }

  @Test
  @DisplayName("GET /api/oficinas/{id} - Deve retornar status 200 ao buscar por ID existente")
  @WithMockUser(roles = "ADMIN")
  void deveBuscarPorId() throws Exception {
    when(oficinaService.buscarPorId(1L)).thenReturn(oficinaResponseDTO);

    mockMvc
        .perform(get("/api/oficinas/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.cnpj").value("12345678000195"));
  }

  @Test
  @DisplayName("GET /api/oficinas/{id} - Deve retornar status 404 quando ID não existir")
  @WithMockUser(roles = "ADMIN")
  void deveRetornar404AoBuscarInexistente() throws Exception {
    when(oficinaService.buscarPorId(99L)).thenThrow(new OficinaNotFoundException(99L));

    mockMvc.perform(get("/api/oficinas/99")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/oficinas - Deve retornar status 201 ao criar oficina")
  @WithMockUser(roles = "ADMIN")
  void deveCriarOficina() throws Exception {
    when(oficinaService.criar(any(OficinaRequestDTO.class))).thenReturn(oficinaResponseDTO);

    mockMvc
        .perform(
            post("/api/oficinas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oficinaRequestDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @DisplayName("PUT /api/oficinas/{id} - Deve retornar status 200 ao atualizar")
  @WithMockUser(roles = "ADMIN")
  void deveAtualizarOficina() throws Exception {
    when(oficinaService.atualizar(eq(1L), any(OficinaRequestDTO.class)))
        .thenReturn(oficinaResponseDTO);

    mockMvc
        .perform(
            put("/api/oficinas/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oficinaRequestDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Oficina Central"));
  }

  @Test
  @DisplayName("DELETE /api/oficinas/{id} - Deve retornar status 204 ao excluir com sucesso")
  @WithMockUser(roles = "ADMIN")
  void deveDeletarOficina() throws Exception {
    doNothing().when(oficinaService).deletar(1L);

    mockMvc.perform(delete("/api/oficinas/1").with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("GET /api/oficinas - Deve retornar status 403 quando usuário não é ADMIN")
  @WithMockUser(roles = "USER")
  void deveNegarAcessoParaUsuarioNaoAdmin() throws Exception {
    mockMvc.perform(get("/api/oficinas")).andExpect(status().isForbidden());
  }

  // ─────────────── ativar / desativar (exclusão lógica) ───────────────

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/oficinas/{id}/desativar - ADMIN desativa e recebe 204")
  void desativar_admin_retorna204() throws Exception {
    doNothing().when(oficinaService).desativar(1L);

    mockMvc
        .perform(patch("/api/oficinas/1/desativar").with(csrf()))
        .andExpect(status().isNoContent());

    verify(oficinaService).desativar(1L);
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("PATCH /api/oficinas/{id}/desativar - GERENTE recebe 403")
  void desativar_gerente_retorna403() throws Exception {
    mockMvc
        .perform(patch("/api/oficinas/1/desativar").with(csrf()))
        .andExpect(status().isForbidden());

    verify(oficinaService, never()).desativar(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/oficinas/{id}/ativar - ADMIN reativa e recebe 204")
  void ativar_admin_retorna204() throws Exception {
    doNothing().when(oficinaService).ativar(1L);

    mockMvc
        .perform(patch("/api/oficinas/1/ativar").with(csrf()))
        .andExpect(status().isNoContent());

    verify(oficinaService).ativar(1L);
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("PATCH /api/oficinas/{id}/ativar - MECANICO recebe 403")
  void ativar_mecanico_retorna403() throws Exception {
    mockMvc.perform(patch("/api/oficinas/1/ativar").with(csrf())).andExpect(status().isForbidden());

    verify(oficinaService, never()).ativar(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/oficinas/{id}/desativar - oficina já desativada retorna 409")
  void desativar_jaDesativada_retorna409() throws Exception {
    doThrow(new OficinaAlreadyDisabledException()).when(oficinaService).desativar(1L);

    mockMvc
        .perform(patch("/api/oficinas/1/desativar").with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/oficinas/{id}/ativar - oficina já ativa retorna 409")
  void ativar_jaAtiva_retorna409() throws Exception {
    doThrow(new OficinaAlreadyActivatedException()).when(oficinaService).ativar(1L);

    mockMvc.perform(patch("/api/oficinas/1/ativar").with(csrf())).andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/oficinas/{id} - resposta expõe o flag ativo")
  void buscarPorId_exponeFlagAtivo() throws Exception {
    when(oficinaService.buscarPorId(1L))
        .thenReturn(new OficinaResponseDTO(1L, "Oficina Central", "12345678000195", "8399", false));

    mockMvc
        .perform(get("/api/oficinas/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(false));
  }
}