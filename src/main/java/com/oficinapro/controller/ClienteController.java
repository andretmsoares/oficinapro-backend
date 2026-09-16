package com.oficinapro.controller;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.service.cliente.ClienteService;
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
@Tag(name = "Clientes", description = "Cadastro de clientes das oficinas")
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

  private final ClienteService clienteService;

  @Operation(
      summary = "Listar clientes da própria oficina",
      description =
          "Retorna, paginado e ordenado por nome, os clientes da oficina do usuário"
              + " autenticado. A oficina vem do token — não é possível informá-la na"
              + " requisição.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de clientes retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<Page<ClienteResponseDTO>> listar(
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(clienteService.listar(pageable));
  }

  @Operation(
      summary = "Buscar cliente por ID",
      description =
          "Busca um cliente pelo ID, restrito à oficina do GERENTE autenticado. Cliente"
              + " de outra oficina responde 404, não 403, para não confirmar que o"
              + " registro existe.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Cliente não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<ClienteResponseDTO> buscarPorId(
      @Parameter(description = "ID do cliente") @PathVariable Long id) {
    return ResponseEntity.ok(clienteService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar clientes por nome",
      description =
          "Busca, na oficina do GERENTE autenticado, clientes cujo nome contenha o termo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de clientes encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping("/nome/{nome}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<List<ClienteResponseDTO>> buscarPorNome(
      @Parameter(description = "Termo de busca pelo nome") @PathVariable String nome) {
    return ResponseEntity.ok(clienteService.buscarPorNome(nome));
  }

  @Operation(
      summary = "Buscar cliente por documento",
      description =
          "Busca, na oficina do GERENTE autenticado, o cliente com o CPF/CNPJ exato"
              + " informado (sem máscara).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Nenhum cliente com este documento na oficina")
  })
  @GetMapping("/documento/{documento}")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(
      @Parameter(description = "CPF ou CNPJ, apenas dígitos") @PathVariable String documento) {
    return ResponseEntity.ok(clienteService.buscarPorDocumento(documento));
  }

  @Operation(
      summary = "Cadastrar cliente",
      description =
          "Cria um cliente vinculado à oficina informada no corpo. O documento é único"
              + " por oficina — o mesmo CPF pode ser cliente de oficinas diferentes.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de criar em oficina de outro tenant"),
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado nesta oficina")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<ClienteResponseDTO> criar(@Valid @RequestBody ClienteRequestDTO request) {
    ClienteResponseDTO response = clienteService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar cliente",
      description = "Atualiza os dados cadastrais de um cliente da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ver campo 'fields')"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Documento já usado por outro cliente da oficina")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<ClienteResponseDTO> atualizar(
      @Parameter(description = "ID do cliente") @PathVariable Long id,
      @Valid @RequestBody ClienteRequestDTO request) {
    return ResponseEntity.ok(clienteService.atualizar(id, request));
  }

  @Operation(summary = "Excluir cliente", description = "Remove um cliente da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Cliente excluído com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
    @ApiResponse(
        responseCode = "409",
        description = "Cliente possui vínculos (ex.: ordens de serviço) que impedem a exclusão")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do cliente") @PathVariable Long id) {
    clienteService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
