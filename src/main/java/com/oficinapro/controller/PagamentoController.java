package com.oficinapro.controller;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.enums.StatusPagamento;
import com.oficinapro.service.pagamento.PagamentoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

  private final PagamentoService pagamentoService;

  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> criar(
      @Valid @RequestBody PagamentoRequestDTO request) {
    PagamentoResponseDTO response = pagamentoService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> buscarPorId(@PathVariable Long id) {
    return ResponseEntity.ok(pagamentoService.buscarPorId(id));
  }

  @GetMapping("/os/{osId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> buscarPorOsId(@PathVariable Long osId) {
    return ResponseEntity.ok(pagamentoService.buscarPorOsId(osId));
  }

  @GetMapping("/oficina/{oficinaId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<PagamentoResponseDTO>> buscarPorOficina(@PathVariable Long oficinaId) {
    return ResponseEntity.ok(pagamentoService.buscarPorOficina(oficinaId));
  }

  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  @GetMapping("/oficina/{oficinaId}/status/{status}")
  public ResponseEntity<List<PagamentoResponseDTO>> buscarPorStatus(
      @PathVariable Long oficinaId, @PathVariable StatusPagamento status) {
    return ResponseEntity.ok(pagamentoService.buscarPorStatus(oficinaId, status));
  }

  @PreAuthorize("hasAnyRole('GERENTE')")
  @GetMapping("/oficina/{oficinaId}/a-receber")
  public ResponseEntity<BigDecimal> calcularValorParaReceber(@PathVariable Long oficinaId) {
    return ResponseEntity.ok(pagamentoService.calcularValorParaReceber(oficinaId));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<PagamentoResponseDTO> atualizar(
      @PathVariable Long id, @Valid @RequestBody PagamentoRequestDTO request) {
    return ResponseEntity.ok(pagamentoService.atualizar(id, request));
  }
}
