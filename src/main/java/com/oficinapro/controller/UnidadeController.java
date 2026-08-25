package com.oficinapro.controller;

import com.oficinapro.dto.unidade.UnidadeRequest;
import com.oficinapro.dto.unidade.UnidadeResponse;
import com.oficinapro.service.unidade.UnidadeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<UnidadeResponse>> listar() {
        return ResponseEntity.ok(
                unidadeService.listar()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponse> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                unidadeService.buscarPorId(id)
        );
    }

    @GetMapping("/oficina/{oficinaId}")
    public ResponseEntity<List<UnidadeResponse>> listarPorOficina(
            @PathVariable Long oficinaId) {

        return ResponseEntity.ok(
                unidadeService.listarPorOficina(oficinaId)
        );
    }

    @PostMapping("/oficina/{oficinaId}")
    public ResponseEntity<UnidadeResponse> criar(
            @PathVariable Long oficinaId,
            @Valid @RequestBody UnidadeRequest request) {

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
    public ResponseEntity<UnidadeResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UnidadeRequest request) {

        return ResponseEntity.ok(
                unidadeService.atualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        unidadeService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}