package com.oficinapro.controller;

import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.exception.ClienteNotFoundException;
import com.oficinapro.service.cliente.ClienteService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@ActiveProfiles("test")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    private ClienteResponseDTO responseDTO;
    private ClienteRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new ClienteResponseDTO(1L, "João Silva", "83999998888", "12345678901", 1L);
        requestDTO = new ClienteRequestDTO("João Silva", "83999998888", "12345678901", 1L);
    }

    // ─── GET /api/clientes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/clientes - ADMIN deve retornar 200 com página de clientes")
    @WithMockUser(roles = "ADMIN")
    void deveListarClientesComoAdmin() throws Exception {
        when(clienteService.listar(any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("João Silva"));
    }

    @Test
    @DisplayName("GET /api/clientes - MECANICO deve retornar 403")
    @WithMockUser(roles = "MECANICO")
    void deveNegarAcessoParaMecanico() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/clientes/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/clientes/{id} - ADMIN deve retornar 200 com o cliente correto")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarClientePorId() throws Exception {
        when(clienteService.buscarPorId(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.documento").value("12345678901"));
    }

    @Test
    @DisplayName("GET /api/clientes/{id} - Deve retornar 404 quando cliente não existir")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoBuscarClienteInexistente() throws Exception {
        when(clienteService.buscarPorId(99L)).thenThrow(new ClienteNotFoundException());

        mockMvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /api/clientes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/clientes - ADMIN deve criar cliente e retornar 201")
    @WithMockUser(roles = "ADMIN")
    void deveCriarCliente() throws Exception {
        when(clienteService.criar(any(ClienteRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"));
    }

    @Test
    @DisplayName("POST /api/clientes - Deve retornar 400 com nome em branco (validação)")
    @WithMockUser(roles = "ADMIN")
    void deveRetornar400ComNomeEmBranco() throws Exception {
        ClienteRequestDTO requestInvalido = new ClienteRequestDTO("", "83999998888", "12345678901", 1L);

        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/clientes/{id} ──────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/clientes/{id} - ADMIN deve atualizar cliente e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarCliente() throws Exception {
        when(clienteService.atualizar(eq(1L), any(ClienteRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/clientes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"));
    }

    // ─── DELETE /api/clientes/{id} ───────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/clientes/{id} - ADMIN deve excluir cliente e retornar 204")
    @WithMockUser(roles = "ADMIN")
    void deveDeletarCliente() throws Exception {
        doNothing().when(clienteService).deletar(1L);

        mockMvc.perform(delete("/api/clientes/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
