package com.oficinapro.controller;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.service.veiculo.VeiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Veículos",
        description = "Operações de gerenciamento de veículos"
)
@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<Page<VeiculoResponseDTO>> listar(
            @PageableDefault(size = 20, sort = "modelo") Pageable pageable) {
        return ResponseEntity.ok(veiculoService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
    }

    @GetMapping("/oficina/{oficinaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<Page<VeiculoResponseDTO>> listarPorOficina(
            @PathVariable Long oficinaId,
            @PageableDefault(size = 20, sort = "modelo") Pageable pageable) {
        return ResponseEntity.ok(veiculoService.listarPorOficinaId(oficinaId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<VeiculoResponseDTO> criar(@Valid @RequestBody VeiculoRequestDTO request) {
        VeiculoResponseDTO response = veiculoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<VeiculoResponseDTO> atualizar(@PathVariable Long id,
                                                        @Valid @RequestBody VeiculoRequestDTO request) {
        return ResponseEntity.ok(veiculoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        veiculoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}