package com.oficinapro.controller;

import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.service.pagamento.PagamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PagamentoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PagamentoService pagamentoService;

    private PagamentoResponseDTO responseDTO() {
        return new PagamentoResponseDTO(
                10L, 1L, new BigDecimal("100.00"), "obs",
                LocalDateTime.now(), StatusPagamento.PAGAMENTO_PENDENTE);
    }

    // ---------------------------------------------------------
    // POST /api/pagamentos
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void criar_admin_retorna201() throws Exception {
        PagamentoRequestDTO request = new PagamentoRequestDTO(1L, BigDecimal.ZERO, "obs");
        when(pagamentoService.criar(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/api/pagamentos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.osId").value(1));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void criar_mecanico_retorna403() throws Exception {
        PagamentoRequestDTO request = new PagamentoRequestDTO(1L, BigDecimal.ZERO, "obs");

        mockMvc.perform(post("/api/pagamentos")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // GET /api/pagamentos/{id}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(pagamentoService.buscarPorId(10L)).thenReturn(responseDTO());

        mockMvc.perform(get("/api/pagamentos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(pagamentoService.buscarPorId(999L)).thenThrow(new PagamentoNotFoundException(999L));

        mockMvc.perform(get("/api/pagamentos/999"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // GET /api/pagamentos/os/{osId}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPorOsId_retorna200() throws Exception {
        when(pagamentoService.buscarPorOsId(1L)).thenReturn(responseDTO());

        mockMvc.perform(get("/api/pagamentos/os/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.osId").value(1));
    }

    // ---------------------------------------------------------
    // PUT /api/pagamentos/{id}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void atualizar_retorna200() throws Exception {
        PagamentoRequestDTO request = new PagamentoRequestDTO(1L, new BigDecimal("50.00"), "novo obs");
        when(pagamentoService.atualizar(eq(10L), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/api/pagamentos/10")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------
    // PATCH /api/pagamentos/{id}/desconto
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void aplicarDesconto_retorna200() throws Exception {
        BigDecimal desconto = new BigDecimal("20.00");
        when(pagamentoService.aplicarDesconto(eq(10L), any())).thenReturn(responseDTO());

        mockMvc.perform(patch("/api/pagamentos/10/desconto")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(desconto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void aplicarDesconto_mecanico_retorna403() throws Exception {
        BigDecimal desconto = new BigDecimal("20.00");

        mockMvc.perform(patch("/api/pagamentos/10/desconto")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(desconto)))
                .andExpect(status().isForbidden());
    }
}