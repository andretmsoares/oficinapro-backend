package com.oficinapro.controller;

import com.oficinapro.dto.ordemDeServico.*;
import com.oficinapro.enums.StatusOrdemDeServico;
import com.oficinapro.service.ordem_servico.OrdemDeServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
public class OrdemDeServicoController {

    private final OrdemDeServicoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> criar(
            @RequestBody @Valid OrdemDeServicoRequestDTO request) {
        OrdemDeServicoResponseDTO response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid OrdemDeServicoRequestDTO request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusOSRequestDTO request) {
        return ResponseEntity.ok(service.atualizarStatus(id, request));
    }

    @PatchMapping("/{id}/mecanico")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> atribuirMecanico(
            @PathVariable Long id,
            @RequestBody @Valid AtribuirMecanicoRequestDTO request) {
        return ResponseEntity.ok(service.atribuirMecanico(id, request));
    }

    @PatchMapping("/{id}/cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO')")
    public ResponseEntity<OrdemDeServicoResponseDTO> atribuirCliente(
            @PathVariable Long id,
            @RequestBody @Valid AtribuirClienteRequestDTO request) {
        return ResponseEntity.ok(service.atribuirCliente(id, request));
    }

    @GetMapping("/veiculo/{veiculoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(service.listarPorVeiculo(veiculoId));
    }

    @GetMapping("/mecanico/{mecanicoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorMecanico(@PathVariable Long mecanicoId) {
        return ResponseEntity.ok(service.listarPorMecanico(mecanicoId));
    }

    @GetMapping("/unidade/{unidadeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorUnidade(@PathVariable Long unidadeId) {
        return ResponseEntity.ok(service.listarPorUnidade(unidadeId));
    }

    @GetMapping("/oficina/{oficinaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorOficina(@PathVariable Long oficinaId) {
        return ResponseEntity.ok(service.listarPorOficina(oficinaId));
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATIVO', 'MECANICO')")
    public ResponseEntity<List<OrdemDeServicoResponseDTO>> listarPorStatus(@PathVariable StatusOrdemDeServico status) {
        return ResponseEntity.ok(service.listarPorStatus(status));
    }
}