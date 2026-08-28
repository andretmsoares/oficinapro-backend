package com.oficinapro.controller;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.service.mecanico.MecanicoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(
        name = "Mecânicos",
        description = "Operações de gerenciamento de mecânicos"
)
@RequestMapping("/api/mecanicos")
@RequiredArgsConstructor
public class MecanicoController {

    private final MecanicoService mecanicoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<MecanicoResponseDTO>> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable)
     {
        return ResponseEntity.ok(mecanicoService.listar(pageable));
    }

    @GetMapping("/oficina/{oficinaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Page<MecanicoResponseDTO>> listarPorOficina(
            @PathVariable Long oficinaId,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(mecanicoService.listarPorOficinaId(oficinaId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<MecanicoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mecanicoService.buscarPorId(id));
    }

    @GetMapping("/nome/{nome}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<MecanicoResponseDTO> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(mecanicoService.buscarPorNome(nome));
    }

    @GetMapping("/documento/{documento}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<MecanicoResponseDTO> buscarPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(mecanicoService.buscarPorDocumento(documento));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<MecanicoResponseDTO> criar(@Valid @RequestBody MecanicoRequestDTO request) {
        MecanicoResponseDTO response = mecanicoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<MecanicoResponseDTO> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody MecanicoRequestDTO request) {
        return ResponseEntity.ok(mecanicoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        mecanicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}