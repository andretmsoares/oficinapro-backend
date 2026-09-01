package com.oficinapro.controller;

import com.oficinapro.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.service.mecanico.MecanicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MecanicoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class MecanicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MecanicoService mecanicoService;

    private MecanicoResponseDTO responseDTO;
    private MecanicoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new MecanicoResponseDTO(
                1L, "Carlos Mecânico", "83911112222", "98765432100",
                1L, BigDecimal.valueOf(3500), "Especialista em motor"
        );
        requestDTO = new MecanicoRequestDTO(
                "Carlos Mecânico", "83911112222", "98765432100",
                1L, BigDecimal.valueOf(3500), "Especialista em motor"
        );
    }

    // ─── GET /api/mecanicos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/mecanicos - ADMIN deve retornar 200 com página de mecânicos")
    @WithMockUser(roles = "ADMIN")
    void deveListarMecanicosComoAdmin() throws Exception {
        when(mecanicoService.listar(any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        mockMvc.perform(get("/api/mecanicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Carlos Mecânico"));
    }

    @Test
    @DisplayName("GET /api/mecanicos - MECANICO deve retornar 403")
    @WithMockUser(roles = "MECANICO")
    void deveNegarAcessoParaMecanico() throws Exception {
        mockMvc.perform(get("/api/mecanicos"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/mecanicos/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/mecanicos/{id} - ADMIN deve retornar 200 com o mecânico correto")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarMecanicoPorId() throws Exception {
        when(mecanicoService.buscarPorId(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/mecanicos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Carlos Mecânico"))
                .andExpect(jsonPath("$.documento").value("98765432100"));
    }

    @Test
    @DisplayName("GET /api/mecanicos/{id} - Deve retornar 404 quando mecânico não existir")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarMecanicoInexistente() throws Exception {
        when(mecanicoService.buscarPorId(99L)).thenThrow(new MecanicoNotFoundException());

        mockMvc.perform(get("/api/mecanicos/99"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /api/mecanicos ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/mecanicos - ADMIN deve criar mecânico e retornar 201")
    @WithMockUser(roles = "ADMIN")
    void deveCriarMecanico() throws Exception {
        when(mecanicoService.criar(any(MecanicoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/mecanicos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Carlos Mecânico"));
    }

    // ─── PUT /api/mecanicos/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/mecanicos/{id} - ADMIN deve atualizar mecânico e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarMecanico() throws Exception {
        when(mecanicoService.atualizar(eq(1L), any(MecanicoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/mecanicos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Carlos Mecânico"));
    }

    // ─── DELETE /api/mecanicos/{id} ───────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/mecanicos/{id} - ADMIN deve excluir mecânico e retornar 204")
    @WithMockUser(roles = "ADMIN")
    void deveDeletarMecanico() throws Exception {
        doNothing().when(mecanicoService).deletar(1L);

        mockMvc.perform(delete("/api/mecanicos/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
