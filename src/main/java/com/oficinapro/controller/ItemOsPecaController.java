package com.oficinapro.controller;

import com.oficinapro.dto.itemOsPeca.ItemOsPecaRequestDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaResponseDTO;
import com.oficinapro.dto.itemOsPeca.ItemOsPecaUpdateRequestDTO;
import com.oficinapro.service.item_os_peca.ItemOsPecaService;
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

@Tag(name = "Peças da OS", description = "Peças lançadas em uma ordem de serviço")
@RestController
@RequestMapping("/api/itens-os-peca")
@RequiredArgsConstructor
public class ItemOsPecaController {

  private final ItemOsPecaService itemOsPecaService;

  @Operation(
      summary = "Listar peças de uma OS",
      description = "Retorna todas as peças lançadas na ordem de serviço informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de peças retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "OS não encontrada, ou pertence a outra oficina")
  })
  @GetMapping("/os/{osId}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<List<ItemOsPecaResponseDTO>> listarPorOrdemServico(
      @Parameter(description = "ID da ordem de serviço") @PathVariable Long osId) {
    return ResponseEntity.ok(itemOsPecaService.listarPorOrdemServico(osId));
  }

  @Operation(summary = "Buscar peça por ID", description = "Busca uma peça pelo ID.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Peça encontrada"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Peça não encontrada, ou pertence a OS de outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<ItemOsPecaResponseDTO> buscarPorId(
      @Parameter(description = "ID da peça") @PathVariable Long id) {
    return ResponseEntity.ok(itemOsPecaService.buscarPorId(id));
  }

  @Operation(
      summary = "Lançar peça na OS",
      description =
          "Lança uma peça na ordem de serviço informada no corpo. A quantidade é sempre"
              + " inteira (2 pastilhas, 1 correia). O valor total é recalculado como"
              + " quantidade × valor unitário, e o valor total da OS é atualizado"
              + " automaticamente somando peças e mão de obra. Não é permitido lançar em"
              + " OS cancelada ou fechada.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Peça lançada com sucesso"),
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
  public ResponseEntity<ItemOsPecaResponseDTO> criar(
      @Valid @RequestBody ItemOsPecaRequestDTO request) {
    ItemOsPecaResponseDTO response = itemOsPecaService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar peça",
      description =
          "Atualiza nome, quantidade e valor unitário de uma peça já lançada. O valor"
              + " total da OS é recalculado. Não é permitido editar peça de OS cancelada"
              + " ou fechada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Peça atualizada com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Peça não encontrada"),
    @ApiResponse(
        responseCode = "422",
        description = "OS cancelada ou fechada não aceita mais edição")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<ItemOsPecaResponseDTO> atualizar(
      @Parameter(description = "ID da peça") @PathVariable Long id,
      @Valid @RequestBody ItemOsPecaUpdateRequestDTO request) {
    return ResponseEntity.ok(itemOsPecaService.atualizar(id, request));
  }

  @Operation(
      summary = "Excluir peça",
      description =
          "Remove uma peça da OS e recalcula o valor total. A exclusão é recusada se"
              + " deixasse o valor da OS abaixo do que já foi pago — nesse caso, estorne o"
              + " pagamento correspondente antes de excluir.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Peça excluída com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Peça não encontrada"),
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
      @Parameter(description = "ID da peça") @PathVariable Long id) {
    itemOsPecaService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
