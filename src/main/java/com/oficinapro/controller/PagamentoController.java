package com.oficinapro.controller;

import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoUpdateRequestDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.service.pagamento.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pagamentos", description = "Pagamento de uma ordem de serviço (relação 1:1 com a OS)")
@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

  private final PagamentoService pagamentoService;

  @Operation(summary = "Buscar pagamento por ID", description = "Busca um pagamento pelo ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Pagamento não encontrado, ou pertence a OS de outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> buscarPorId(
      @Parameter(description = "ID do pagamento") @PathVariable Long id) {
    return ResponseEntity.ok(pagamentoService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar pagamento de uma OS",
      description = "Busca o pagamento vinculado à ordem de serviço informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "A OS não possui pagamento, ou não foi encontrada")
  })
  @GetMapping("/os/{osId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> buscarPorOsId(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long osId) {
    return ResponseEntity.ok(pagamentoService.buscarPorOsId(osId));
  }

  @Operation(
      summary = "Listar pagamentos de uma oficina",
      description = "Retorna todos os pagamentos das ordens de serviço da oficina informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @GetMapping("/oficina/{oficinaId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<PagamentoResponseDTO>> buscarPorOficina(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId) {
    return ResponseEntity.ok(pagamentoService.buscarPorOficina(oficinaId));
  }

  @Operation(
      summary = "Listar pagamentos de uma oficina por status",
      description =
          "Filtra os pagamentos da oficina pelo status informado"
              + " (PAGAMENTO_PENDENTE, PAGO_PARCIALMENTE ou PAGA). Único endpoint"
              + " financeiro liberado também ao MECANICO — informa se a OS está paga,"
              + " sem expor valores agregados da oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Status inválido"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @PreAuthorize("hasAnyRole('GERENTE')")
  @GetMapping("/oficina/{oficinaId}/status/{status}")
  public ResponseEntity<List<PagamentoResponseDTO>> buscarPorStatus(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @Parameter(description = "Status do pagamento") @PathVariable StatusPagamento status) {
    return ResponseEntity.ok(pagamentoService.buscarPorStatus(oficinaId, status));
  }

  @Operation(
      summary = "Calcular valor a receber da oficina",
      description =
          "Soma o saldo em aberto de todos os pagamentos PAGAMENTO_PENDENTE e"
              + " PAGO_PARCIALMENTE da oficina informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Valor a receber calculado com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @PreAuthorize("hasAnyRole('GERENTE')")
  @GetMapping("/oficina/{oficinaId}/a-receber")
  public ResponseEntity<BigDecimal> calcularValorParaReceber(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId) {
    return ResponseEntity.ok(pagamentoService.calcularValorParaReceber(oficinaId));
  }

  @Operation(
      summary = "Atualizar observação do pagamento",
      description =
          "Atualiza apenas a observação do pagamento. Valor pago e status não são"
              + " editáveis por este endpoint — mudam somente por recebimento, estorno ou"
              + " recálculo automático do valor da OS.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento atualizado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Conflito de versão: o pagamento foi alterado por outro usuário")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> atualizar(
      @Parameter(description = "ID do pagamento") @PathVariable Long id,
      @Valid @RequestBody PagamentoUpdateRequestDTO request) {
    return ResponseEntity.ok(pagamentoService.atualizar(id, request));
  }
}
