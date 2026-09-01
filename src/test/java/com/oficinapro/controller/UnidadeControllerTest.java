package com.oficinapro.controller;

import com.oficinapro.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.service.unidade.UnidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UnidadeController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class UnidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnidadeService unidadeService;

    private UnidadeResponseDTO responseDTO;
    private UnidadeRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new UnidadeResponseDTO(1L, 1L, "Filial Norte", "Av. Principal, 100", "83933334444");
        requestDTO = new UnidadeRequestDTO("Filial Norte", "Av. Principal, 100", "83933334444");
    }

    // ─── GET /api/unidades ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/unidades - ADMIN deve retornar 200 com lista de unidades")
    @WithMockUser(roles = "ADMIN")
    void deveListarUnidadesComoAdmin() throws Exception {
        when(unidadeService.listar()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Filial Norte"));
    }

    @Test
    @DisplayName("GET /api/unidades - MECANICO deve retornar 403")
    @WithMockUser(roles = "MECANICO")
    void deveNegarAcessoParaMecanico() throws Exception {
        mockMvc.perform(get("/api/unidades"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/unidades/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/unidades/{id} - ADMIN deve retornar 200 com a unidade correta")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarUnidadePorId() throws Exception {
        when(unidadeService.buscarPorId(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/unidades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Filial Norte"))
                .andExpect(jsonPath("$.endereco").value("Av. Principal, 100"));
    }

    @Test
    @DisplayName("GET /api/unidades/{id} - Deve retornar 404 quando unidade não existir")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarUnidadeInexistente() throws Exception {
        when(unidadeService.buscarPorId(99L)).thenThrow(new UnidadeNotFoundException(99L));

        mockMvc.perform(get("/api/unidades/99"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /api/unidades/oficina/{oficinaId} ───────────────────────────────────

    @Test
    @DisplayName("POST /api/unidades/oficina/1 - ADMIN deve criar unidade e retornar 201")
    @WithMockUser(roles = "ADMIN")
    void deveCriarUnidade() throws Exception {
        when(unidadeService.criar(eq(1L), any(UnidadeRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/unidades/oficina/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Filial Norte"));
    }

    // ─── PUT /api/unidades/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/unidades/{id} - ADMIN deve atualizar unidade e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarUnidade() throws Exception {
        when(unidadeService.atualizar(eq(1L), any(UnidadeRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/unidades/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Filial Norte"));
    }

    // ─── DELETE /api/unidades/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/unidades/{id} - ADMIN deve excluir unidade e retornar 204")
    @WithMockUser(roles = "ADMIN")
    void deveDeletarUnidade() throws Exception {
        doNothing().when(unidadeService).deletar(1L);

        mockMvc.perform(delete("/api/unidades/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
