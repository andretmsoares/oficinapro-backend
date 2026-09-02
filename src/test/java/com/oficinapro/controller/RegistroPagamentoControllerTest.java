package com.oficinapro.controller;

import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.enums.MeioPagamento;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.service.registro_pagamento.RegistroPagamentoService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistroPagamentoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class RegistroPagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistroPagamentoService registroPagamentoService;

    private RegistroPagamentoResponseDTO responseDTO() {
        return new RegistroPagamentoResponseDTO(100L, 10L, new BigDecimal("150.00"), MeioPagamento.PIX, LocalDateTime.now());
    }

    // ---------------------------------------------------------
    // POST /api/registros-pagamento
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void criar_admin_retorna201() throws Exception {
        RegistroPagamentoRequestDTO request = new RegistroPagamentoRequestDTO(10L, new BigDecimal("150.00"), MeioPagamento.PIX);
        when(registroPagamentoService.criar(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/api/registros-pagamento")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.pagamentoId").value(10));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void criar_mecanico_retorna403() throws Exception {
        RegistroPagamentoRequestDTO request = new RegistroPagamentoRequestDTO(10L, new BigDecimal("150.00"), MeioPagamento.PIX);

        mockMvc.perform(post("/api/registros-pagamento")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // GET /api/registros-pagamento/{id}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(registroPagamentoService.buscarPorId(100L)).thenReturn(responseDTO());

        mockMvc.perform(get("/api/registros-pagamento/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meioPagamento").value("PIX"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(registroPagamentoService.buscarPorId(999L)).thenThrow(new RegistroPagamentoNotFoundException(999L));

        mockMvc.perform(get("/api/registros-pagamento/999"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // GET /api/registros-pagamento/pagamento/{pagamentoId}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarPorPagamento_retornaLista() throws Exception {
        when(registroPagamentoService.listarPorPagamento(10L)).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/api/registros-pagamento/pagamento/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    // ---------------------------------------------------------
    // DELETE /api/registros-pagamento/{id}
    // ---------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletar_admin_retorna204() throws Exception {
        doNothing().when(registroPagamentoService).deletar(100L);

        mockMvc.perform(delete("/api/registros-pagamento/100").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void deletar_mecanico_retorna403() throws Exception {
        mockMvc.perform(delete("/api/registros-pagamento/100").with(csrf()))
                .andExpect(status().isForbidden());

        verify(registroPagamentoService, never()).deletar(any());
    }
}