package com.oficinapro.controller;

import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.dto.pagamento.PagamentoResumoDTO;
import com.oficinapro.dto.pagamento.PagamentoUpdateRequestDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.service.pagamento.PagamentoService;
import com.oficinapro.util.Paginacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
  public ResponseEntity<Page<PagamentoResponseDTO>> buscarPorOficina(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @Parameter(description = "Parte do número da OS ou do pagamento")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Filtrar por status") @RequestParam(required = false)
          StatusPagamento status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(pagamentoService.buscarPorOficina(oficinaId, q, status, pageable));
  }

  @Operation(
      summary = "Resumo financeiro da oficina",
      description =
          "Total recebido, valor a receber e quantidade de pagamentos com saldo, somados no"
              + " banco sobre todos os pagamentos (não dependem da página exibida).")
  @PreAuthorize("hasAnyRole('GERENTE')")
  @GetMapping("/oficina/{oficinaId}/resumo")
  public ResponseEntity<PagamentoResumoDTO> resumo(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId) {
    return ResponseEntity.ok(pagamentoService.resumo(oficinaId));
  }

  @Operation(
      summary = "Pagamentos de um conjunto de OS",
      description =
          "Devolve os pagamentos das OS informadas (até 100 ids), restritos à oficina. Serve"
              + " para a tela de OS mostrar o valor pendente só das OS da página atual.")
  @PreAuthorize("hasAnyRole('GERENTE')")
  @GetMapping("/oficina/{oficinaId}/por-os")
  public ResponseEntity<List<PagamentoResponseDTO>> buscarPorOsIds(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @Parameter(description = "IDs das OS, separados por vírgula (máx. 100)")
          @RequestParam(name = "osIds")
          List<Long> osIds) {
    if (osIds.size() > Paginacao.TAMANHO_MAXIMO) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe no máximo " + Paginacao.TAMANHO_MAXIMO + " OS");
    }
    return ResponseEntity.ok(pagamentoService.buscarPorOsIds(oficinaId, osIds));
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
  public ResponseEntity<Page<PagamentoResponseDTO>> buscarPorStatus(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @Parameter(description = "Status do pagamento") @PathVariable StatusPagamento status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(pagamentoService.buscarPorStatus(oficinaId, status, pageable));
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
