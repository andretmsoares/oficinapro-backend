package com.oficinapro.controller;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import com.oficinapro.service.mao_obra.MaoObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mao-obra")
@RequiredArgsConstructor
public class MaoObraController {

    private final MaoObraService maoObraService;

    @GetMapping("/os/{osId}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
    public ResponseEntity<List<MaoObraResponseDTO>> listarPorOrdemServico(@PathVariable Long osId) {
        return ResponseEntity.ok(maoObraService.listarPorOrdemServico(osId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
    public ResponseEntity<MaoObraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(maoObraService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
    public ResponseEntity<MaoObraResponseDTO> criar(@Valid @RequestBody MaoObraRequestDTO request) {
        MaoObraResponseDTO response = maoObraService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
    public ResponseEntity<MaoObraResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody MaoObraRequestDTO request
    ) {
        return ResponseEntity.ok(maoObraService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        maoObraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}