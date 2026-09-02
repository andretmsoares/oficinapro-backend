package com.oficinapro.controller;

import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.service.registro_pagamento.RegistroPagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registros-pagamento")
@RequiredArgsConstructor
public class RegistroPagamentoController {

    private final RegistroPagamentoService registroPagamentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<RegistroPagamentoResponseDTO> criar(@Valid @RequestBody RegistroPagamentoRequestDTO request) {
        RegistroPagamentoResponseDTO response = registroPagamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<RegistroPagamentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(registroPagamentoService.buscarPorId(id));
    }

    @GetMapping("/pagamento/{pagamentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<List<RegistroPagamentoResponseDTO>> listarPorPagamento(@PathVariable Long pagamentoId) {
        return ResponseEntity.ok(registroPagamentoService.listarPorPagamento(pagamentoId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        registroPagamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}