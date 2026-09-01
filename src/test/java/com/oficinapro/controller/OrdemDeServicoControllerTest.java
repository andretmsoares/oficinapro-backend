package com.oficinapro.controller;

import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.ordemDeServico.AtribuirClienteRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtribuirMecanicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtualizarStatusOSRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.OrdemDeServicoNotFoundException;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdemDeServicoController.class)
@ActiveProfiles("test")
class OrdemDeServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrdemDeServicoService service;

    private OrdemDeServicoResponseDTO responseDTO;
    private OrdemDeServicoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new OrdemDeServicoResponseDTO(
                1L, 1L, 1L, 1L, 1L, 1L,
                LocalDateTime.of(2025, 1, 15, 10, 0, 0),
                null,
                StatusOrdemDeServico.ABERTA,
                "Revisão geral",
                BigDecimal.ZERO
        );
        requestDTO = new OrdemDeServicoRequestDTO(1L, 1L, 1L, 1L, 1L, "Revisão geral");
    }

    // ─── POST /api/ordens-servico ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/ordens-servico - ADMIN deve criar OS e retornar 201")
    @WithMockUser(roles = "ADMIN")
    void deveCriarOrdemDeServicoComoAdmin() throws Exception {
        when(service.criar(any(OrdemDeServicoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/ordens-servico")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @DisplayName("POST /api/ordens-servico - MECANICO deve retornar 403")
    @WithMockUser(roles = "MECANICO")
    void deveNegarCriacaoParaMecanico() throws Exception {
        mockMvc.perform(post("/api/ordens-servico")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/ordens-servico ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/ordens-servico - ADMIN deve retornar 200 com lista de OS")
    @WithMockUser(roles = "ADMIN")
    void deveListarOrdensDeServicoComoAdmin() throws Exception {
        when(service.listar()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/ordens-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ABERTA"));
    }

    @Test
    @DisplayName("GET /api/ordens-servico - MECANICO deve retornar 200 (acesso de leitura)")
    @WithMockUser(roles = "MECANICO")
    void deveListarOrdensDeServicoComoMecanico() throws Exception {
        when(service.listar()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/ordens-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ─── GET /api/ordens-servico/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/ordens-servico/{id} - ADMIN deve retornar 200 com a OS correta")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarOsPorId() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/ordens-servico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.obs").value("Revisão geral"));
    }

    @Test
    @DisplayName("GET /api/ordens-servico/{id} - Deve retornar 404 quando OS não existir")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarOsInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new OrdemDeServicoNotFoundException(99L));

        mockMvc.perform(get("/api/ordens-servico/99"))
                .andExpect(status().isNotFound());
    }

    // ─── PATCH /api/ordens-servico/{id}/status ────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/ordens-servico/{id}/status - ADMIN deve atualizar status e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarStatusOs() throws Exception {
        OrdemDeServicoResponseDTO osEmExecucao = new OrdemDeServicoResponseDTO(
                1L, 1L, 1L, 1L, 1L, 1L,
                LocalDateTime.of(2025, 1, 15, 10, 0, 0),
                null,
                StatusOrdemDeServico.EM_EXECUCAO,
                "Revisão geral",
                BigDecimal.ZERO
        );
        AtualizarStatusOSRequestDTO statusRequest = new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.EM_EXECUCAO);

        when(service.atualizarStatus(eq(1L), any(AtualizarStatusOSRequestDTO.class))).thenReturn(osEmExecucao);

        mockMvc.perform(patch("/api/ordens-servico/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    // ─── PATCH /api/ordens-servico/{id}/mecanico ─────────────────────────────────

    @Test
    @DisplayName("PATCH /api/ordens-servico/{id}/mecanico - ADMIN deve atribuir mecânico e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtribuirMecanicoNaOs() throws Exception {
        AtribuirMecanicoRequestDTO mecanicoRequest = new AtribuirMecanicoRequestDTO(2L);

        when(service.atribuirMecanico(eq(1L), any(AtribuirMecanicoRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/ordens-servico/1/mecanico")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mecanicoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ─── DELETE /api/ordens-servico/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/ordens-servico/{id} - ADMIN deve excluir OS e retornar 204")
    @WithMockUser(roles = "ADMIN")
    void deveDeletarOs() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/api/ordens-servico/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
