package com.oficinapro.controller;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.service.oficina.OficinaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Oficinas",
        description = "Operações de gerenciamento de oficinas"
)
@RestController
@RequestMapping("/api/oficinas")
public class OficinaController {

    private final OficinaService oficinaService;

    public OficinaController(OficinaService oficinaService) {
        this.oficinaService = oficinaService;
    }

    @Operation(
            summary = "Listar oficinas",
            description = "Retorna todas as oficinas cadastradas"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de oficinas retornada com sucesso"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OficinaResponseDTO>> listar() {
        return ResponseEntity.ok(oficinaService.listar());
    }

    @Operation(
            summary = "Buscar oficina",
            description = "Busca uma oficina pelo ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Oficina encontrada"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Oficina não encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OficinaResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                oficinaService.buscarPorId(id)
        );
    }

    @Operation(
            summary = "Criar oficina",
            description = "Cria uma nova oficina no sistema"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Oficina criada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "CNPJ já cadastrado"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OficinaResponseDTO> criar(
            @Valid @RequestBody OficinaRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(oficinaService.criar(request));
    }

    @Operation(
            summary = "Atualizar oficina",
            description = "Atualiza os dados de uma oficina"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Oficina atualizada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Oficina não encontrada"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "CNPJ já cadastrado"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OficinaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OficinaRequestDTO request) {

        return ResponseEntity.ok(
                oficinaService.atualizar(id, request)
        );
    }

    @Operation(
            summary = "Excluir oficina",
            description = "Exclui uma oficina pelo ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Oficina excluída com sucesso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Oficina não encontrada"
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        oficinaService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}