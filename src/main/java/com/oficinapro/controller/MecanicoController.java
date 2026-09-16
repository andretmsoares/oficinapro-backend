package com.oficinapro.controller;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.service.mecanico.MecanicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Mecânicos", description = "Cadastro de mecânicos das oficinas")
@RequestMapping("/api/mecanicos")
@RequiredArgsConstructor
public class MecanicoController {

  private final MecanicoService mecanicoService;

  @Operation(
      summary = "Listar mecânicos da própria oficina",
      description =
          "Retorna, paginado e ordenado por nome, os mecânicos da oficina do usuário"
              + " autenticado. A oficina vem do token — não é possível informá-la na"
              + " requisição.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de mecânicos retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Page<MecanicoResponseDTO>> listar(
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(mecanicoService.listar(pageable));
  }

  @Operation(
      summary = "Buscar mecânico por ID",
      description =
          "Busca um mecânico pelo ID, restrito à oficina do GERENTE autenticado. Mecânico"
              + " de outra oficina responde 404, não 403.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mecânico encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Mecânico não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<MecanicoResponseDTO> buscarPorId(
      @Parameter(description = "ID do mecânico") @PathVariable Long id) {
    return ResponseEntity.ok(mecanicoService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar mecânicos por nome",
      description =
          "Busca, na oficina do GERENTE autenticado, mecânicos cujo nome contenha o termo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de mecânicos encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping("/nome/{nome}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<MecanicoResponseDTO>> buscarPorNome(
      @Parameter(description = "Termo de busca pelo nome") @PathVariable String nome) {
    return ResponseEntity.ok(mecanicoService.buscarPorNome(nome));
  }

  @Operation(
      summary = "Buscar mecânico por documento",
      description =
          "Busca, na oficina do GERENTE autenticado, o mecânico com o CPF exato informado"
              + " (sem máscara).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mecânico encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Nenhum mecânico com este documento na oficina")
  })
  @GetMapping("/documento/{documento}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<MecanicoResponseDTO> buscarPorDocumento(
      @Parameter(description = "CPF, apenas dígitos") @PathVariable String documento) {
    return ResponseEntity.ok(mecanicoService.buscarPorDocumento(documento));
  }

  @Operation(
      summary = "Cadastrar mecânico",
      description =
          "Cria um mecânico vinculado à oficina informada no corpo, com salário e"
              + " observações opcionais.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Mecânico criado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de criar em oficina de outro tenant"),
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado nesta oficina")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<MecanicoResponseDTO> criar(@Valid @RequestBody MecanicoRequestDTO request) {
    MecanicoResponseDTO response = mecanicoService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar mecânico",
      description = "Atualiza os dados cadastrais de um mecânico da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mecânico atualizado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Mecânico não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Documento já usado por outro mecânico da oficina")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<MecanicoResponseDTO> atualizar(
      @Parameter(description = "ID do mecânico") @PathVariable Long id,
      @Valid @RequestBody MecanicoRequestDTO request) {
    return ResponseEntity.ok(mecanicoService.atualizar(id, request));
  }

  @Operation(summary = "Excluir mecânico", description = "Remove um mecânico da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Mecânico excluído com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Mecânico não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Mecânico possui vínculos (ex.: ordens de serviço) que impedem a exclusão")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do mecânico") @PathVariable Long id) {
    mecanicoService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
