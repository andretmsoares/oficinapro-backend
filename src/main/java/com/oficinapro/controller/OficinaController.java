package com.oficinapro.controller;

import com.oficinapro.dto.oficina.OficinaRequest;
import com.oficinapro.dto.oficina.OficinaResponse;
import com.oficinapro.service.oficina.OficinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oficinas")
public class OficinaController {

    private final OficinaService oficinaService;

    public OficinaController(OficinaService oficinaService) {
        this.oficinaService = oficinaService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OficinaResponse>> listar() {
        return ResponseEntity.ok(oficinaService.listar());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OficinaResponse> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                oficinaService.buscarPorId(id)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OficinaResponse> criar(
            @Valid @RequestBody OficinaRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(oficinaService.criar(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OficinaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OficinaRequest request) {

        return ResponseEntity.ok(
                oficinaService.atualizar(id, request)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        oficinaService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}