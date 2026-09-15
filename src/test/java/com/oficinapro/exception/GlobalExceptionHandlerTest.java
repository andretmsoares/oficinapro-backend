package com.oficinapro.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.exception.mao_obra.MaoObraNotFoundException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.exception.ordem_servico.DescontoInvalidoException;
import com.oficinapro.exception.ordem_servico.OSCanceledException;
import com.oficinapro.exception.ordem_servico.OSFinishedException;
import com.oficinapro.exception.ordem_servico.OSIsNotPossibleSwapWorkshopException;
import com.oficinapro.exception.pagamento.PagamentoAlreadyExistsException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.exception.pagamento.PagamentoValorInvalidoException;
import com.oficinapro.exception.usuario.OficinaIncompativelComRoleException;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tradução de exceções para status HTTP.
 *
 * <p>Usa um controller sintético e {@code standaloneSetup} com o advice real: assim a verificação
 * cobre exceções que nenhum controller de produção consegue disparar em teste (falhas de acesso a
 * dados, corpo ilegível, tipo de path variable incorreto) sem depender de contexto Spring.
 *
 * <p>Além do status, os testes verificam que mensagens de infraestrutura NÃO vazam detalhes de
 * banco para o cliente.
 */
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private void esperaStatus(String cenario, int statusEsperado) throws Exception {
    mockMvc.perform(get("/teste/" + cenario)).andExpect(status().is(statusEsperado));
  }

  @Nested
  @DisplayName("404 - recurso não encontrado")
  class NaoEncontrado {

    @Test
    @DisplayName("exceções de domínio '...NotFound' devem virar 404 preservando a mensagem")
    void notFoundDeDominioViram404() throws Exception {
      mockMvc
          .perform(get("/teste/oficina-not-found"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("mão de obra inexistente deve virar 404")
    void maoObraNaoEncontradaVira404() throws Exception {
      esperaStatus("mao-obra-not-found", 404);
    }
  }

  @Nested
  @DisplayName("409 - conflito")
  class Conflito {

    @Test
    @DisplayName("placa já cadastrada deve virar 409, não 500")
    void placaDuplicadaVira409() throws Exception {
      esperaStatus("placa-duplicada", 409);
    }

    @Test
    @DisplayName("segundo pagamento para a mesma OS deve virar 409")
    void pagamentoDuplicadoVira409() throws Exception {
      esperaStatus("pagamento-duplicado", 409);
    }

    @Test
    @DisplayName("valor de pagamento excedido deve virar 409")
    void valorExcedidoVira409() throws Exception {
      esperaStatus("valor-excedido", 409);
    }

    @Test
    @DisplayName("violação de integridade deve virar 409 SEM expor detalhes do banco")
    void violacaoDeIntegridadeVira409SemVazarDetalhes() throws Exception {
      mockMvc
          .perform(get("/teste/integridade"))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.message")
                  .value(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("uk_pagamento_os"))))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsStringIgnoringCase("constraint"))));
    }

    @Test
    @DisplayName("resultado múltiplo inesperado no banco deve virar 409 sem expor a query")
    void resultadoMultiploVira409SemVazarQuery() throws Exception {
      mockMvc
          .perform(get("/teste/resultado-multiplo"))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.message")
                  .value(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsStringIgnoringCase("select"))));
    }
  }

  @Nested
  @DisplayName("400 - requisição inválida")
  class RequisicaoInvalida {

    @Test
    @DisplayName("desconto inválido deve virar 400")
    void descontoInvalidoVira400() throws Exception {
      esperaStatus("desconto-invalido", 400);
    }

    @Test
    @DisplayName("valor de pagamento inválido deve virar 400")
    void valorInvalidoVira400() throws Exception {
      esperaStatus("valor-invalido", 400);
    }

    @Test
    @DisplayName("role incompatível com oficina deve virar 400")
    void roleIncompativelVira400() throws Exception {
      esperaStatus("role-incompativel", 400);
    }

    @Test
    @DisplayName("estado ilegal (ex.: transição de status proibida) deve virar 400")
    void estadoIlegalVira400() throws Exception {
      esperaStatus("estado-ilegal", 400);
    }

    @Test
    @DisplayName("uso incorreto da API de acesso a dados deve virar 400 sem expor detalhes")
    void usoIncorretoDeDataAccessVira400() throws Exception {
      mockMvc
          .perform(get("/teste/data-access-invalido"))
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsStringIgnoringCase("hibernate"))));
    }

    @Test
    @DisplayName("data/hora inválida (ex.: mês 13 no fluxo mensal) deve virar 400")
    void dataInvalidaVira400() throws Exception {
      esperaStatus("data-invalida", 400);
    }

    @Test
    @DisplayName("path variable com tipo incorreto deve virar 400, não 500")
    void pathVariableComTipoErradoVira400() throws Exception {
      mockMvc.perform(get("/teste/id/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("corpo JSON malformado deve virar 400, não 500")
    void corpoMalformadoVira400() throws Exception {
      mockMvc
          .perform(
              post("/teste/corpo")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{ nao e json valido "))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("422 - regra de negócio da OS")
  class RegraDeNegocioOs {

    @Test
    @DisplayName("OS cancelada deve virar 422")
    void osCanceladaVira422() throws Exception {
      esperaStatus("os-cancelada", 422);
    }

    @Test
    @DisplayName("OS fechada deve virar 422")
    void osFechadaVira422() throws Exception {
      esperaStatus("os-fechada", 422);
    }

    @Test
    @DisplayName("troca de oficina da OS deve virar 422")
    void trocaDeOficinaVira422() throws Exception {
      esperaStatus("troca-oficina", 422);
    }
  }

  @Nested
  @DisplayName("403 - autorização")
  class Autorizacao {

    @Test
    @DisplayName("acesso negado deve virar 403 com mensagem genérica")
    void acessoNegadoVira403() throws Exception {
      mockMvc
          .perform(get("/teste/acesso-negado"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.message").exists());
    }
  }

  @Nested
  @DisplayName("formato do corpo de erro")
  class FormatoDoCorpo {

    @Test
    @DisplayName("todo erro tratado deve responder com status, error, message e timestamp")
    void corpoDeErroPossuiCamposPadrao() throws Exception {
      mockMvc
          .perform(get("/teste/oficina-not-found"))
          .andExpect(jsonPath("$.status").exists())
          .andExpect(jsonPath("$.error").exists())
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("o corpo de erro não deve expor stacktrace nem nome de classe de exceção")
    void corpoDeErroNaoExpoeStacktrace() throws Exception {
      mockMvc
          .perform(get("/teste/integridade"))
          .andExpect(jsonPath("$.stackTrace").doesNotExist())
          .andExpect(jsonPath("$.exception").doesNotExist())
          .andExpect(jsonPath("$.trace").doesNotExist());
    }
  }

  /** Controller sintético: cada rota dispara uma exceção específica. */
  @RestController
  @RequestMapping("/teste")
  static class ControllerDeTeste {

    @GetMapping("/oficina-not-found")
    public void oficinaNotFound() {
      throw new OficinaNotFoundException(1L);
    }

    @GetMapping("/mao-obra-not-found")
    public void maoObraNotFound() {
      throw new MaoObraNotFoundException(1L);
    }

    @GetMapping("/placa-duplicada")
    public void placaDuplicada() {
      throw new PlacaAlreadyExistsException("ABC1234");
    }

    @GetMapping("/pagamento-duplicado")
    public void pagamentoDuplicado() {
      throw new PagamentoAlreadyExistsException();
    }

    @GetMapping("/valor-excedido")
    public void valorExcedido() {
      throw new PagamentoValorExcedidoException(new BigDecimal("200.00"), new BigDecimal("100.00"));
    }

    @GetMapping("/valor-invalido")
    public void valorInvalido() {
      throw new PagamentoValorInvalidoException("O estorno excede o valor já pago");
    }

    @GetMapping("/desconto-invalido")
    public void descontoInvalido() {
      throw new DescontoInvalidoException("Desconto não pode ser negativo");
    }

    @GetMapping("/role-incompativel")
    public void roleIncompativel() {
      throw new OficinaIncompativelComRoleException("ADMIN não pode ter oficina");
    }

    @GetMapping("/os-cancelada")
    public void osCancelada() {
      throw new OSCanceledException();
    }

    @GetMapping("/os-fechada")
    public void osFechada() {
      throw new OSFinishedException();
    }

    @GetMapping("/troca-oficina")
    public void trocaOficina() {
      throw new OSIsNotPossibleSwapWorkshopException();
    }

    @GetMapping("/estado-ilegal")
    public void estadoIlegal() {
      throw new IllegalStateException("Transição de status não permitida: ABERTA → FECHADA");
    }

    @GetMapping("/integridade")
    public void integridade() {
      throw new DataIntegrityViolationException(
          "could not execute statement; constraint [uk_pagamento_os]");
    }

    @GetMapping("/resultado-multiplo")
    public void resultadoMultiplo() {
      throw new IncorrectResultSizeDataAccessException(
          "select v from Veiculo v where v.placa = ?", 1, 2);
    }

    @GetMapping("/data-access-invalido")
    public void dataAccessInvalido() {
      throw new InvalidDataAccessApiUsageException(
          "org.hibernate.TransientObjectException: id nulo");
    }

    @GetMapping("/data-invalida")
    public void dataInvalida() {
      throw new DateTimeException("Invalid value for MonthOfYear: 13");
    }

    @GetMapping("/acesso-negado")
    public void acessoNegado() {
      throw new AccessDeniedException("Você só pode acessar dados da sua própria oficina");
    }

    @GetMapping("/id/{id}")
    public void comId(@PathVariable Long id) {
      // nunca alcançado quando o id não é numérico
    }

    @PostMapping("/corpo")
    public void comCorpo(@RequestBody CorpoDeTeste corpo) {
      // nunca alcançado quando o JSON é malformado
    }

    record CorpoDeTeste(Long id, String nome) {}
  }
}
