package com.oficinapro.controller;

import com.oficinapro.dto.mao_obra.MaoObraRequestDTO;
import com.oficinapro.dto.mao_obra.MaoObraResponseDTO;
import com.oficinapro.service.mao_obra.MaoObraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mão de obra", description = "Serviços de mão de obra lançados em uma ordem de serviço")
@RestController
@RequestMapping("/api/mao-obra")
@RequiredArgsConstructor
public class MaoObraController {

  private final MaoObraService maoObraService;

  @Operation(
      summary = "Listar mão de obra de uma OS",
      description = "Retorna todos os lançamentos de mão de obra da ordem de serviço informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS não encontrada, ou pertence a outra oficina")
  })
  @GetMapping("/os/{osId}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<List<MaoObraResponseDTO>> listarPorOrdemServico(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long osId) {
    return ResponseEntity.ok(maoObraService.listarPorOrdemServico(osId));
  }

  @Operation(
      summary = "Buscar lançamento de mão de obra por ID",
      description = "Busca um lançamento de mão de obra pelo ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lançamento encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Lançamento não encontrado, ou pertence a OS de outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<MaoObraResponseDTO> buscarPorId(
      @Parameter(description = "ID do lançamento") @PathVariable Long id) {
    return ResponseEntity.ok(maoObraService.buscarPorId(id));
  }

  @Operation(
      summary = "Lançar mão de obra na OS",
      description =
          "Lança um valor de mão de obra (ex.: revisão, diagnóstico) na ordem de serviço"
              + " informada. O valor precisa ser positivo. O valor total da OS é"
              + " recalculado automaticamente somando peças e mão de obra. Não é"
              + " permitido lançar em OS cancelada ou fechada.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Mão de obra lançada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS não encontrada, ou pertence a outra oficina"),
    @ApiResponse(
        responseCode = "422",
        description = "OS cancelada ou fechada não aceita novos lançamentos")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<MaoObraResponseDTO> criar(@Valid @RequestBody MaoObraRequestDTO request) {
    MaoObraResponseDTO response = maoObraService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar lançamento de mão de obra",
      description =
          "Atualiza valor e descrição de um lançamento existente. O campo osId do corpo"
              + " é ignorado: não é permitido mover o lançamento para outra OS por este"
              + " endpoint. O valor total da OS é recalculado.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lançamento atualizado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado"),
    @ApiResponse(
        responseCode = "422",
        description = "OS cancelada ou fechada não aceita mais edição")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<MaoObraResponseDTO> atualizar(
      @Parameter(description = "ID do lançamento") @PathVariable Long id,
      @Valid @RequestBody MaoObraRequestDTO request) {
    return ResponseEntity.ok(maoObraService.atualizar(id, request));
  }

  @Operation(
      summary = "Excluir lançamento de mão de obra",
      description =
          "Remove um lançamento e recalcula o valor total da OS. A exclusão é recusada"
              + " se deixasse o valor da OS abaixo do que já foi pago.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Lançamento excluído com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "A exclusão deixaria o valor da OS abaixo do que já foi pago"),
    @ApiResponse(
        responseCode = "422",
        description = "OS cancelada ou fechada não aceita mais edição")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do lançamento") @PathVariable Long id) {
    maoObraService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
