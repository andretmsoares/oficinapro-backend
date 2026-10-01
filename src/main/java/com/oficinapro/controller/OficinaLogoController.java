package com.oficinapro.controller;

import com.oficinapro.service.oficina.OficinaLogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Logo da oficina", description = "Logo impressa no cabeçalho dos PDFs da oficina")
@RestController
@RequestMapping("/api/oficinas/{oficinaId}/logo")
public class OficinaLogoController {

  private final OficinaLogoService logoService;

  public OficinaLogoController(OficinaLogoService logoService) {
    this.logoService = logoService;
  }

  @Operation(
      summary = "Enviar/substituir a logo",
      description =
          "Aceita PNG ou JPEG de até 2 MB (campo multipart `arquivo`). O tipo é verificado pelo"
              + " conteúdo do arquivo. GERENTE só altera a logo da própria oficina.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Logo atualizada"),
    @ApiResponse(responseCode = "400", description = "Arquivo ausente, grande demais ou inválido"),
    @ApiResponse(responseCode = "403", description = "Sem permissão ou oficina de outro tenant"),
    @ApiResponse(responseCode = "503", description = "Armazenamento de logos não configurado")
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> atualizar(
      @PathVariable Long oficinaId, @RequestParam("arquivo") MultipartFile arquivo) {
    logoService.atualizar(oficinaId, arquivo);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Remover a logo",
      description = "Volta a usar a logo padrão do sistema nos PDFs. Idempotente.")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
  @DeleteMapping
  public ResponseEntity<Void> remover(@PathVariable Long oficinaId) {
    logoService.remover(oficinaId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Obter a logo", description = "Devolve a imagem; 404 se não houver logo.")
  @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'MECANICO')")
  @GetMapping
  public ResponseEntity<byte[]> buscar(@PathVariable Long oficinaId) {
    OficinaLogoService.Logo logo = logoService.buscar(oficinaId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(logo.contentType()))
        .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
        .body(logo.conteudo());
  }
}
