package com.oficinapro.controller;

import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.service.registro_pagamento.RegistroPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Registros de pagamento",
    description = "Histórico de recebimentos de um pagamento (parcelas)")
@RestController
@RequestMapping("/api/registros-pagamento")
@RequiredArgsConstructor
public class RegistroPagamentoController {

  private final RegistroPagamentoService registroPagamentoService;

  @Operation(
      summary = "Registrar recebimento",
      description =
          "Registra o recebimento de um valor para o pagamento informado (uma parcela)."
              + " O valor é somado ao valorPago do pagamento e o status é recalculado"
              + " automaticamente (PAGAMENTO_PENDENTE, PAGO_PARCIALMENTE ou PAGA)."
              + " Receber acima do valor da OS é recusado — não existe pagamento a maior.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Recebimento registrado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Pagamento não encontrado, ou pertence a outra oficina"),
    @ApiResponse(
        responseCode = "409",
        description =
            "O valor recebido ultrapassa o valor da OS, ou o pagamento foi alterado por"
                + " outro usuário (conflito de versão)")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<RegistroPagamentoResponseDTO> criar(
      @Valid @RequestBody RegistroPagamentoRequestDTO request) {
    RegistroPagamentoResponseDTO response = registroPagamentoService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Buscar registro de pagamento por ID",
      description = "Busca um registro de recebimento pelo ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Registro encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Registro não encontrado, ou pertence a pagamento de outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<RegistroPagamentoResponseDTO> buscarPorId(
      @Parameter(description = "ID do registro") @PathVariable Long id) {
    return ResponseEntity.ok(registroPagamentoService.buscarPorId(id));
  }

  @Operation(
      summary = "Listar histórico de recebimentos de um pagamento",
      description = "Retorna todos os registros de recebimento do pagamento informado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de registros retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Pagamento não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/pagamento/{pagamentoId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<RegistroPagamentoResponseDTO>> listarPorPagamento(
      @Parameter(description = "ID do pagamento") @PathVariable Long pagamentoId) {
    return ResponseEntity.ok(registroPagamentoService.listarPorPagamento(pagamentoId));
  }

  @Operation(
      summary = "Excluir registro de pagamento (estornar)",
      description =
          "Remove um registro de recebimento e estorna o valor correspondente no"
              + " pagamento, recalculando o status. Estornar mais do que já foi pago"
              + " nunca acontece: o valor estornado é exatamente o do registro excluído.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Registro excluído e valor estornado com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Registro não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "O pagamento foi alterado por outro usuário (conflito de versão)")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do registro") @PathVariable Long id) {
    registroPagamentoService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
