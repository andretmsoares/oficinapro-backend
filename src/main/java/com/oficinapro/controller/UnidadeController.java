package com.oficinapro.controller;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.service.unidade.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Unidades", description = "Unidades (endereços de atendimento) de cada oficina")
@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

  private final UnidadeService unidadeService;

  public UnidadeController(UnidadeService unidadeService) {
    this.unidadeService = unidadeService;
  }

  @Operation(
      summary = "Listar unidades da própria oficina",
      description = "Retorna todas as unidades da oficina do GERENTE autenticado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de unidades retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<UnidadeResponseDTO>> listar() {
    return ResponseEntity.ok(unidadeService.listar());
  }

  @Operation(
      summary = "Buscar unidade por ID",
      description =
          "Busca uma unidade pelo ID, restrita à oficina do GERENTE autenticado. Unidade"
              + " de outra oficina responde 404, não 403.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Unidade encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Unidade não encontrada, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<UnidadeResponseDTO> buscarPorId(
      @Parameter(description = "ID da unidade") @PathVariable Long id) {

    return ResponseEntity.ok(unidadeService.buscarPorId(id));
  }

  @Operation(
      summary = "Listar unidades de uma oficina",
      description =
          "Retorna as unidades da oficina informada. Restrito à própria oficina do"
              + " GERENTE autenticado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de unidades retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @GetMapping("/oficina/{oficinaId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<UnidadeResponseDTO>> listarPorOficina(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId) {

    return ResponseEntity.ok(unidadeService.listarPorOficina(oficinaId));
  }

  @Operation(
      summary = "Cadastrar unidade",
      description =
          "Cria uma unidade (endereço de atendimento) para a oficina informada na URL. O"
              + " endereço é único por oficina — duas oficinas diferentes podem operar no"
              + " mesmo endereço.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Unidade criada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de criar em oficina de outro tenant"),
    @ApiResponse(responseCode = "409", description = "Endereço já cadastrado nesta oficina")
  })
  @PostMapping("/oficina/{oficinaId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<UnidadeResponseDTO> criar(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @Valid @RequestBody UnidadeRequestDTO request) {

    return ResponseEntity.status(HttpStatus.CREATED).body(unidadeService.criar(oficinaId, request));
  }

  @Operation(
      summary = "Atualizar unidade",
      description = "Atualiza os dados de uma unidade da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Unidade atualizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Unidade não encontrada"),
    @ApiResponse(
        responseCode = "409",
        description = "Endereço já usado por outra unidade da mesma oficina")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<UnidadeResponseDTO> atualizar(
      @Parameter(description = "ID da unidade") @PathVariable Long id,
      @Valid @RequestBody UnidadeRequestDTO request) {

    return ResponseEntity.ok(unidadeService.atualizar(id, request));
  }

  @Operation(summary = "Excluir unidade", description = "Remove uma unidade da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Unidade excluída com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Unidade não encontrada"),
    @ApiResponse(
        responseCode = "409",
        description = "Unidade possui ordens de serviço vinculadas que impedem a exclusão")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID da unidade") @PathVariable Long id) {

    unidadeService.deletar(id);

    return ResponseEntity.noContent().build();
  }
}
