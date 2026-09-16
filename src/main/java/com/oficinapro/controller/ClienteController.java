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
      summary = "Listar todos os clientes (ADMIN)",
      description =
          "Retorna, paginado, o cadastro de clientes de TODAS as oficinas da plataforma."
              + " Exclusivo do ADMIN do SaaS. Endpoint sensível: dado que o ADMIN não"
              + " deveria acessar informação operacional das oficinas, o uso dele deve"
              + " ser evitado — ver docs/permissions.md.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de clientes retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Apenas o ADMIN do SaaS pode listar todos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<Page<ClienteResponseDTO>> listar(
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(clienteService.listar(pageable));
  }

  @Operation(
      summary = "Listar clientes de uma oficina",
      description =
          "Retorna, paginado e ordenado por nome, os clientes cadastrados na oficina"
              + " informada. O GERENTE só pode consultar a própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de clientes retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentativa de acessar oficina de outro tenant")
  })
  @GetMapping("/oficina/{oficinaId}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<Page<ClienteResponseDTO>> listarPorOficina(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId,
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(clienteService.listarPorOficinaId(oficinaId, pageable));
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
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<ClienteResponseDTO> buscarPorId(
      @Parameter(description = "ID do cliente") @PathVariable Long id) {
    return ResponseEntity.ok(clienteService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar clientes por nome",
      description = "Busca, na oficina do GERENTE autenticado, clientes cujo nome contenha o termo.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de clientes encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping("/nome/{nome}")
  @PreAuthorize("hasAnyRole('GERENTE')")
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
  @PreAuthorize("hasAnyRole('GERENTE')")
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
    @ApiResponse(responseCode = "409", description = "Documento já usado por outro cliente da oficina")
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
