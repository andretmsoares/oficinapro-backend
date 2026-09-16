package com.oficinapro.controller;

import com.oficinapro.dto.veiculo.VeiculoRequestDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.service.veiculo.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Veículos", description = "Cadastro de veículos das oficinas")
@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

  private final VeiculoService veiculoService;

  public VeiculoController(VeiculoService veiculoService) {
    this.veiculoService = veiculoService;
  }

  @Operation(
      summary = "Listar veículos da própria oficina",
      description =
          "Retorna, paginado e ordenado por nome, os veículos da oficina do usuário"
              + " autenticado. A oficina vem do token — não é possível informá-la na"
              + " requisição.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de veículos retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Page<VeiculoResponseDTO>> listar(
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(veiculoService.listar(pageable));
  }

  @Operation(
      summary = "Buscar veículo por ID",
      description =
          "Busca um veículo pelo ID, restrito à oficina do usuário autenticado. Veículo"
              + " de outra oficina responde 404, não 403.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Veículo não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<VeiculoResponseDTO> buscarPorId(
      @Parameter(description = "ID do veículo") @PathVariable Long id) {
    return ResponseEntity.ok(veiculoService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar veículo por placa",
      description =
          "Busca o veículo com a placa informada, na oficina do usuário autenticado. A"
              + " placa é normalizada antes da busca (aceita com ou sem hífen, e"
              + " maiúsculas/minúsculas).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Nenhum veículo com esta placa na oficina")
  })
  @GetMapping("/placa/{placa}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(
      @Parameter(description = "Placa, com ou sem hífen (ex.: ABC1D23 ou ABC-1D23)") @PathVariable
          String placa) {
    return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
  }

  @Operation(
      summary = "Cadastrar veículo",
      description =
          "Cria um veículo vinculado à oficina informada no corpo. A placa é única por"
              + " oficina. Também pode ser feito pelo MECANICO, para cadastrar o veículo"
              + " assim que ele chega, antes de identificar o proprietário.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Veículo criado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de criar em oficina de outro tenant"),
    @ApiResponse(responseCode = "409", description = "Placa já cadastrada nesta oficina")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<VeiculoResponseDTO> criar(@Valid @RequestBody VeiculoRequestDTO request) {
    VeiculoResponseDTO response = veiculoService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar veículo",
      description =
          "Atualiza os dados de um veículo da própria oficina. Exclusivo do GERENTE: o"
              + " MECANICO pode cadastrar mas não alterar.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Veículo atualizado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
    @ApiResponse(responseCode = "409", description = "Placa já usada por outro veículo da oficina")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<VeiculoResponseDTO> atualizar(
      @Parameter(description = "ID do veículo") @PathVariable Long id,
      @Valid @RequestBody VeiculoRequestDTO request) {
    return ResponseEntity.ok(veiculoService.atualizar(id, request));
  }

  @Operation(
      summary = "Excluir veículo",
      description = "Remove um veículo da própria oficina. Exclusivo do GERENTE.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Veículo excluído com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Veículo possui ordens de serviço vinculadas que impedem a exclusão")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do veículo") @PathVariable Long id) {
    veiculoService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
