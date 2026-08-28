package com.oficinapro.controller;

import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.exception.OficinaNotFoundException;
import com.oficinapro.service.oficina.OficinaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OficinaController.class)
@ActiveProfiles("test")
class OficinaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OficinaService oficinaService;

    private OficinaResponseDTO oficinaResponseDTO;
    private OficinaRequestDTO oficinaRequestDTO;

    @BeforeEach
    void setUp() {
        oficinaResponseDTO = new OficinaResponseDTO(1L, "Oficina Central", "12345678000195", "83999998888");
        oficinaRequestDTO = new OficinaRequestDTO("Oficina Central", "12345678000195", "83999998888");
    }

    @Test
    @DisplayName("GET /api/oficinas - Deve retornar status 200 e lista de oficinas")
    @WithMockUser(roles = "ADMIN")
    void deveListarOficinas() throws Exception {
        when(oficinaService.listar()).thenReturn(List.of(oficinaResponseDTO));

        mockMvc.perform(get("/api/oficinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Oficina Central"));
    }

    @Test
    @DisplayName("GET /api/oficinas/{id} - Deve retornar status 200 ao buscar por ID existente")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarPorId() throws Exception {
        when(oficinaService.buscarPorId(1L)).thenReturn(oficinaResponseDTO);

        mockMvc.perform(get("/api/oficinas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));
    }

    @Test
    @DisplayName("GET /api/oficinas/{id} - Deve retornar status 404 quando ID não existir")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarInexistente() throws Exception {
        when(oficinaService.buscarPorId(99L)).thenThrow(new OficinaNotFoundException(99L));

        mockMvc.perform(get("/api/oficinas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/oficinas - Deve retornar status 201 ao criar oficina")
    @WithMockUser(roles = "ADMIN")
    void deveCriarOficina() throws Exception {
        when(oficinaService.criar(any(OficinaRequestDTO.class))).thenReturn(oficinaResponseDTO);

        mockMvc.perform(post("/api/oficinas")
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
        when(oficinaService.atualizar(eq(1L), any(OficinaRequestDTO.class))).thenReturn(oficinaResponseDTO);

        mockMvc.perform(put("/api/oficinas/1")
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

        mockMvc.perform(delete("/api/oficinas/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/oficinas - Deve retornar status 403 quando usuário não é ADMIN")
    @WithMockUser(roles = "USER")
    void deveNegarAcessoParaUsuarioNaoAdmin() throws Exception {
        mockMvc.perform(get("/api/oficinas"))
                .andExpect(status().isForbidden());
    }
}