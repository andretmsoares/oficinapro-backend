package com.oficinapro.controller;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.service.unidade.UnidadeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
            name = "Unidades",
        description = "Operações de gerenciamento das Unidades de cada Oficina"
)
@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<List<UnidadeResponseDTO>> listar() {
        return ResponseEntity.ok(
                unidadeService.listar()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<UnidadeResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                unidadeService.buscarPorId(id)
        );
    }

    @GetMapping("/oficina/{oficinaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<List<UnidadeResponseDTO>> listarPorOficina(
            @PathVariable Long oficinaId) {

        return ResponseEntity.ok(
                unidadeService.listarPorOficina(oficinaId)
        );
    }

    @PostMapping("/oficina/{oficinaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<UnidadeResponseDTO> criar(
            @PathVariable Long oficinaId,
            @Valid @RequestBody UnidadeRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        unidadeService.criar(
                                oficinaId,
                                request
                        )
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<UnidadeResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UnidadeRequestDTO request) {

        return ResponseEntity.ok(
                unidadeService.atualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        unidadeService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}