package com.oficinapro.controller;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Autenticação",
        description = "Login e consulta do usuário autenticado"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Login",
            description = "Autentica por username e password e devolve o access token JWT"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @SecurityRequirements // rota pública: não exige o cadeado no Swagger
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Usuário autenticado",
            description = "Retorna os dados do usuário dono do token enviado"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(authService.usuarioLogado());
    }
}
