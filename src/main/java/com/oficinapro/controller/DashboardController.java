package com.oficinapro.controller;

import com.oficinapro.dto.dashboard.*;
import com.oficinapro.service.dashboard.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard", description = "Dados para o dashboard")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService service;

  @GetMapping("/data")
  @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO')")
  public ResponseEntity<DashboardResponseDTO> getData() {
    return ResponseEntity.ok(service.getData());
  }
}
