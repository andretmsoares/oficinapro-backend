package com.oficinapro.controller;

import com.oficinapro.dto.pagamento.PagamentoRequestDTO;
import com.oficinapro.dto.pagamento.PagamentoResponseDTO;
import com.oficinapro.service.pagamento.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<PagamentoResponseDTO> criar(@Valid @RequestBody PagamentoRequestDTO request) {
        PagamentoResponseDTO response = pagamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<PagamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(id));
    }

    @GetMapping("/os/{osId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<PagamentoResponseDTO> buscarPorOsId(@PathVariable Long osId) {
        return ResponseEntity.ok(pagamentoService.buscarPorOsId(osId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<PagamentoResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody PagamentoRequestDTO request) {
        return ResponseEntity.ok(pagamentoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/desconto")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<PagamentoResponseDTO> aplicarDesconto(@PathVariable Long id,
                                                                @RequestBody BigDecimal desconto) {
        return ResponseEntity.ok(pagamentoService.aplicarDesconto(id, desconto));
    }
}