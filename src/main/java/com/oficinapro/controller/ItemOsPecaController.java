package com.oficinapro.controller;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.service.item_os_peca.ItemOsPecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itens-os-peca")
@RequiredArgsConstructor
public class ItemOsPecaController {

    private final ItemOsPecaService itemOsPecaService;

    @GetMapping("/os/{osId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<ItemOsPecaResponseDTO>> listarPorOrdemServico(@PathVariable Long osId) {
        return ResponseEntity.ok(itemOsPecaService.listarPorOrdemServico(osId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<ItemOsPecaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(itemOsPecaService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<ItemOsPecaResponseDTO> criar(@Valid @RequestBody ItemOsPecaRequestDTO request) {
        ItemOsPecaResponseDTO response = itemOsPecaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<ItemOsPecaResponseDTO> atualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ItemOsPecaUpdateRequestDTO request) {
        return ResponseEntity.ok(itemOsPecaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        itemOsPecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}