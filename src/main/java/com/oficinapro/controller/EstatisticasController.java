package com.oficinapro.controller;

import com.oficinapro.dto.estatisticas.*;
import com.oficinapro.service.estatisticas.EstatisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Estatísticas (ADMIN)", description = "Contagens agregadas da plataforma")
@RequestMapping("/api/admin/estatisticas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EstatisticasController {

  private final EstatisticasService estatisticasService;

  @Operation(
      summary = "Resumo da plataforma",
      description =
          "Contagem de clientes, mecânicos, veículos e ordens de serviço, quebrada por"
              + " oficina e totalizada. Substitui os antigos endpoints de listagem global:"
              + " o ADMIN do SaaS vê volume, não dado operacional dos tenants.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resumo calculado com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Apenas o ADMIN do SaaS")
  })
  @GetMapping
  public ResponseEntity<EstatisticasSistemaResponseDTO> resumo() {
    return ResponseEntity.ok(estatisticasService.resumoDoSistema());
  }

  @Operation(
      summary = "Resumo de uma oficina",
      description = "Mesmas contagens, restritas à oficina informada.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resumo calculado com sucesso"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "403", description = "Apenas o ADMIN do SaaS"),
    @ApiResponse(responseCode = "404", description = "Oficina não encontrada")
  })
  @GetMapping("/oficina/{oficinaId}")
  public ResponseEntity<EstatisticasOficinaResponseDTO> resumoPorOficina(
      @Parameter(description = "ID da oficina") @PathVariable Long oficinaId) {
    return ResponseEntity.ok(estatisticasService.resumoDaOficina(oficinaId));
  }
}
