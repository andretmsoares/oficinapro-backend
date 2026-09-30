package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.ordemDeServico.AtribuirClienteRequestDTO;
import com.oficinapro.dto.ordemDeServico.AtualizarStatusOSRequestDTO;
import com.oficinapro.dto.ordemDeServico.FluxoMensalOSResponseDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoRequestDTO;
import com.oficinapro.dto.ordemDeServico.OrdemDeServicoResponseDTO;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.ordem_servico.DescontoInvalidoException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

/** Endpoints de {@link OrdemDeServicoController} que não eram exercitados por nenhum teste. */
@WebMvcTest(OrdemDeServicoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class OrdemDeServicoControllerComplementarTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private OrdemDeServicoService service;

  private OrdemDeServicoResponseDTO responseDTO;

  @BeforeEach
  void setUp() {
    responseDTO =
        new OrdemDeServicoResponseDTO(
            1L,
            1L,
            1L,
            1L,
            1L,
            1L,
            LocalDateTime.of(2025, 1, 15, 10, 0, 0),
            null,
            StatusOrdemDeServico.ABERTA,
            "Revisao geral",
            "Cliente",
            "ABC1A12",
            "Oficina",
            "Unidade",
            "Mecanico",
            new BigDecimal("500"),
            BigDecimal.ZERO,
            new BigDecimal("500"));
  }

  // ─── GET /fluxo-mensal ────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /fluxo-mensal - deve repassar mês e ano e devolver uma linha por dia")
  @WithMockUser(roles = "GERENTE")
  void deveRetornarFluxoMensal() throws Exception {
    when(service.fluxoMensal(2, 2024))
        .thenReturn(
            List.of(
                new FluxoMensalOSResponseDTO(1, 2L, 0L), new FluxoMensalOSResponseDTO(2, 0L, 1L)));

    mockMvc
        .perform(get("/api/ordens-servico/fluxo-mensal").param("mes", "2").param("ano", "2024"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].day").value(1))
        .andExpect(jsonPath("$[0].abertas").value(2))
        .andExpect(jsonPath("$[1].finalizadas").value(1));
  }

  @Test
  @DisplayName("GET /fluxo-mensal - sem os parâmetros obrigatórios deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400SemParametros() throws Exception {
    mockMvc.perform(get("/api/ordens-servico/fluxo-mensal")).andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("GET /fluxo-mensal - mês inválido (DateTimeException do service) vira 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ParaMesInvalido() throws Exception {
    when(service.fluxoMensal(13, 2024))
        .thenThrow(new java.time.DateTimeException("Invalid value for MonthOfYear"));

    mockMvc
        .perform(get("/api/ordens-servico/fluxo-mensal").param("mes", "13").param("ano", "2024"))
        .andExpect(status().isBadRequest());
  }

  // ─── PUT /{id} ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("PUT /{id} - deve atualizar a OS e retornar 200")
  @WithMockUser(roles = "GERENTE")
  void deveAtualizarOs() throws Exception {
    OrdemDeServicoRequestDTO request = new OrdemDeServicoRequestDTO(1L, 1L, 1L, 1L, "Nova obs");
    when(service.atualizar(eq(1L), any(OrdemDeServicoRequestDTO.class))).thenReturn(responseDTO);

    mockMvc
        .perform(
            put("/api/ordens-servico/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @DisplayName("PUT /{id} - sem unidade e veículo obrigatórios deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400AoAtualizarComPayloadInvalido() throws Exception {
    OrdemDeServicoRequestDTO invalido = new OrdemDeServicoRequestDTO(null, null, null, null, null);

    mockMvc
        .perform(
            put("/api/ordens-servico/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.unidadeId").exists())
        .andExpect(jsonPath("$.fields.veiculoId").exists());

    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("PUT /{id} - troca de oficina vira 422")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar422AoTentarTrocarOficina() throws Exception {
    when(service.atualizar(eq(1L), any(OrdemDeServicoRequestDTO.class)))
        .thenThrow(new OSIsNotPossibleSwapWorkshopException());

    mockMvc
        .perform(
            put("/api/ordens-servico/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new OrdemDeServicoRequestDTO(1L, 1L, null, null, null))))
        .andExpect(status().is(422));
  }

  // ─── PATCH /{id}/cliente ──────────────────────────────────────────────────────

  @Test
  @DisplayName("PATCH /{id}/cliente - deve atribuir cliente e retornar 200")
  @WithMockUser(roles = "MECANICO")
  void deveAtribuirCliente() throws Exception {
    when(service.atribuirCliente(eq(1L), any(AtribuirClienteRequestDTO.class)))
        .thenReturn(responseDTO);

    mockMvc
        .perform(
            patch("/api/ordens-servico/1/cliente")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AtribuirClienteRequestDTO(1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clienteId").value(1));
  }

  @Test
  @DisplayName("PATCH /{id}/cliente - sem clienteId deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400AoAtribuirClienteSemId() throws Exception {
    mockMvc
        .perform(
            patch("/api/ordens-servico/1/cliente")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.clienteId").exists());
  }

  // ─── PATCH /{id}/desconto ─────────────────────────────────────────────────────

  @Test
  @DisplayName("PATCH /{id}/desconto - GERENTE aplica desconto e recebe 200")
  @WithMockUser(roles = "GERENTE")
  void deveAplicarDesconto() throws Exception {
    when(service.aplicarDesconto(1L, new BigDecimal("50"))).thenReturn(responseDTO);

    mockMvc
        .perform(
            patch("/api/ordens-servico/1/desconto")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("50"))
        .andExpect(status().isOk());

    verify(service).aplicarDesconto(1L, new BigDecimal("50"));
  }

  @Test
  @DisplayName("PATCH /{id}/desconto - desconto inválido vira 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ParaDescontoInvalido() throws Exception {
    when(service.aplicarDesconto(eq(1L), any(BigDecimal.class)))
        .thenThrow(new DescontoInvalidoException("Desconto nao pode ser negativo"));

    mockMvc
        .perform(
            patch("/api/ordens-servico/1/desconto")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Desconto nao pode ser negativo"));
  }

  @Test
  @DisplayName("PATCH /{id}/desconto - MECANICO não pode aplicar desconto (403)")
  @WithMockUser(roles = "MECANICO")
  void mecanicoNaoPodeAplicarDesconto() throws Exception {
    mockMvc
        .perform(
            patch("/api/ordens-servico/1/desconto")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("10"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  // ─── PATCH /{id}/status ───────────────────────────────────────────────────────

  @Test
  @DisplayName("PATCH /{id}/status - OS cancelada vira 422")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar422ParaOsCancelada() throws Exception {
    when(service.atualizarStatus(eq(1L), any(AtualizarStatusOSRequestDTO.class)))
        .thenThrow(new OSCanceledException());

    mockMvc
        .perform(
            patch("/api/ordens-servico/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new AtualizarStatusOSRequestDTO(StatusOrdemDeServico.DIAGNOSTICO))))
        .andExpect(status().is(422));
  }

  @Test
  @DisplayName("PATCH /{id}/status - sem status no corpo deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400SemStatus() throws Exception {
    mockMvc
        .perform(
            patch("/api/ordens-servico/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.status").exists());
  }

  @Test
  @DisplayName("PATCH /{id}/status - status inexistente no corpo deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ParaStatusInexistente() throws Exception {
    mockMvc
        .perform(
            patch("/api/ordens-servico/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INEXISTENTE\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  // ─── listagens por relação ────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /veiculo/{id} - deve listar as OS do veículo")
  @WithMockUser(roles = "MECANICO")
  void deveListarPorVeiculo() throws Exception {
    when(service.listarPorVeiculo(1L)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/ordens-servico/veiculo/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].veiculoId").value(1));
  }

  @Test
  @DisplayName("GET /veiculo/{id} - veículo inexistente vira 404")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar404ParaVeiculoInexistente() throws Exception {
    when(service.listarPorVeiculo(99L)).thenThrow(new VeiculoNotFoundException(99L));

    mockMvc.perform(get("/api/ordens-servico/veiculo/99")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /mecanico/{id} - deve listar as OS do mecânico")
  @WithMockUser(roles = "GERENTE")
  void deveListarPorMecanico() throws Exception {
    when(service.listarPorMecanico(1L)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/ordens-servico/mecanico/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].mecanicoId").value(1));
  }

  @Test
  @DisplayName("GET /unidade/{id} - deve listar as OS da unidade")
  @WithMockUser(roles = "GERENTE")
  void deveListarPorUnidade() throws Exception {
    when(service.listarPorUnidade(1L)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/ordens-servico/unidade/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].unidadeId").value(1));
  }

  @Test
  @DisplayName("GET /unidade/{id} - unidade inexistente vira 404")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar404ParaUnidadeInexistente() throws Exception {
    when(service.listarPorUnidade(99L)).thenThrow(new UnidadeNotFoundException(99L));

    mockMvc.perform(get("/api/ordens-servico/unidade/99")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /cliente/{id} - deve listar as OS do cliente")
  @WithMockUser(roles = "GERENTE")
  void deveListarPorCliente() throws Exception {
    when(service.listarPorCliente(1L)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/ordens-servico/cliente/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].clienteId").value(1));
  }

  @Test
  @DisplayName("GET /status/{status} - deve converter o enum e listar por status")
  @WithMockUser(roles = "GERENTE")
  void deveListarPorStatus() throws Exception {
    when(service.listarPorStatus(StatusOrdemDeServico.ABERTA)).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/ordens-servico/status/ABERTA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("ABERTA"));
  }

  @Test
  @DisplayName("GET /status/{status} - status inexistente deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ParaStatusInvalidoNoPath() throws Exception {
    mockMvc
        .perform(get("/api/ordens-servico/status/NAO_EXISTE"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("GET /cliente/{id} - ADMIN do SaaS não acessa OS de oficina (403)")
  @WithMockUser(roles = "ADMIN")
  void adminNaoListaOsPorCliente() throws Exception {
    mockMvc.perform(get("/api/ordens-servico/cliente/1")).andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }
}
