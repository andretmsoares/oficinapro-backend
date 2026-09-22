package com.oficinapro.controller;

import com.oficinapro.dto.usuario.*;
import com.oficinapro.service.usuario.UsuarioService;
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
@Tag(name = "Usuários", description = "Contas de acesso ao sistema (ADMIN, GERENTE, MECANICO)")
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  @Operation(
      summary = "Listar usuários",
      description =
          "Para o ADMIN do SaaS, retorna os usuários de todas as oficinas da plataforma."
              + " Para o GERENTE, apenas os da própria oficina — a oficina vem do token,"
              + " não da requisição.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  public ResponseEntity<Page<UsuarioResponseDTO>> listar(
      @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(usuarioService.listar(pageable));
  }

  @Operation(
    summary = "Atualizar meus dados",
    description =
        "Atualiza os dados do usuário autenticado. O usuário pode alterar nome, "
            + "documento, telefone, username e senha. A role e a oficina não podem "
            + "ser alteradas por este endpoint.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "409", description = "Username ou documento já em uso")
  })
  @PutMapping("/me")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'MECANICO')")
  public ResponseEntity<UsuarioResponseDTO> atualizarMe(
      @Valid @RequestBody UsuarioMeUpdateRequestDTO request) {

    return ResponseEntity.ok(usuarioService.atualizarMe(request));
  }

  @Operation(
      summary = "Buscar usuário por ID",
      description =
          "Busca um usuário pelo ID. O ADMIN pode buscar qualquer usuário; o GERENTE só"
              + " os da própria oficina — usuário de outra oficina responde 404, não 403.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(
        responseCode = "404",
        description = "Usuário não encontrado, ou pertence a outra oficina")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  public ResponseEntity<UsuarioResponseDTO> buscarPorId(
      @Parameter(description = "ID do usuário") @PathVariable Long id) {
    return ResponseEntity.ok(usuarioService.buscarPorId(id));
  }

  @Operation(
      summary = "Buscar usuários por nome (dentro da própria oficina)",
      description =
          "Busca, na oficina do GERENTE autenticado, usuários cujo nome contenha o termo."
              + " Restrito a GERENTE: o ADMIN não pertence a nenhuma oficina e deve usar"
              + " o endpoint /admin/nome/{nome}.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de usuários encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão")
  })
  @GetMapping("/nome/{nome}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<List<UsuarioResponseDTO>> buscarPorNome(
      @Parameter(description = "Termo de busca pelo nome") @PathVariable String nome) {
    return ResponseEntity.ok(usuarioService.buscarPorNome(nome));
  }

  @Operation(
      summary = "Buscar usuário por documento (dentro da própria oficina)",
      description =
          "Busca, na oficina do GERENTE autenticado, o usuário com o documento exato"
              + " informado. Restrito a GERENTE, pelo mesmo motivo do endpoint por nome.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Nenhum usuário com este documento na oficina")
  })
  @GetMapping("/documento/{documento}")
  @PreAuthorize("hasAnyRole('GERENTE')")
  public ResponseEntity<UsuarioResponseDTO> buscarPorDocumento(
      @Parameter(description = "CPF, apenas dígitos") @PathVariable String documento) {
    return ResponseEntity.ok(usuarioService.buscarPorDocumento(documento));
  }

  @Operation(
      summary = "Buscar usuários por nome em todas as oficinas (ADMIN)",
      description =
          "Busca usuários cujo nome contenha o termo, sem escopo de oficina. Exclusivo"
              + " do ADMIN — as rotas /nome/{nome} não funcionam para ele, pois exigem"
              + " oficina, e o ADMIN do SaaS não pertence a nenhuma.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de usuários encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Apenas o ADMIN do SaaS")
  })
  @GetMapping("/admin/nome/{nome}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UsuarioResponseDTO>> buscarPorNomeAdmin(
      @Parameter(description = "Termo de busca pelo nome") @PathVariable String nome) {
    return ResponseEntity.ok(usuarioService.buscarPorNomeAdmin(nome));
  }

  @Operation(
      summary = "Buscar usuários por documento em todas as oficinas (ADMIN)",
      description =
          "Busca usuários pelo documento, sem escopo de oficina. Exclusivo do ADMIN, pelo"
              + " mesmo motivo do endpoint de busca por nome.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de usuários encontrados (pode ser vazia)"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Apenas o ADMIN do SaaS")
  })
  @GetMapping("/admin/documento/{documento}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<List<UsuarioResponseDTO>> buscarPorDocumentoAdmin(
      @Parameter(description = "Documento, apenas dígitos") @PathVariable String documento) {
    return ResponseEntity.ok(usuarioService.buscarPorDocumentoAdmin(documento));
  }

  @Operation(
      summary = "Criar usuário",
      description =
          "Cria uma conta de acesso. O ADMIN pode criar qualquer papel, inclusive outro"
              + " ADMIN; o GERENTE só pode criar GERENTE ou MECANICO na própria oficina."
              + " ADMIN não pode ter oficinaId; GERENTE e MECANICO são obrigados a ter.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
    @ApiResponse(
        responseCode = "400",
        description = "Dados inválidos, ou papel incompatível com o vínculo de oficina"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description =
            "Sem permissão para gerenciar usuários, ou tentando atribuir um papel que não"
                + " pode conceder (ex.: GERENTE tentando criar ADMIN)"),
    @ApiResponse(responseCode = "409", description = "Username ou documento já cadastrado")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO request) {
    UsuarioResponseDTO response = usuarioService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Atualizar usuário",
      description =
          "Atualiza os dados de uma conta, incluindo papel e senha (opcional — se"
              + " omitida, a senha atual é mantida). Uma conta com papel ADMIN só pode"
              + " ser editada por outro ADMIN.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
    @ApiResponse(
        responseCode = "400",
        description = "Dados inválidos, ou papel incompatível com o vínculo de oficina"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(
        responseCode = "403",
        description = "Sem permissão, ou tentando editar um ADMIN sem ser ADMIN"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
    @ApiResponse(responseCode = "409", description = "Username ou documento já em uso")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  public ResponseEntity<UsuarioResponseDTO> atualizar(
      @Parameter(description = "ID do usuário") @PathVariable Long id,
      @Valid @RequestBody UsuarioUpdateRequestDTO request) {
    return ResponseEntity.ok(usuarioService.atualizar(id, request));
  }

  @Operation(summary = "Excluir usuário", description = "Remove uma conta de acesso.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Sem permissão"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  public ResponseEntity<Void> deletar(
      @Parameter(description = "ID do usuário") @PathVariable Long id) {
    usuarioService.deletar(id);
    return ResponseEntity.noContent().build();
  }
}
