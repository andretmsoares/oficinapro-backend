package com.oficinapro.controller;

import com.oficinapro.dto.auth.LoginRequestDTO;
import com.oficinapro.dto.auth.LoginResponseDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private LoginRequestDTO loginRequest;
    private UsuarioResponseDTO usuarioAdmin;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO("admin.saas", "senha1234");

        // ADMIN do SaaS: oficinaId nulo
        usuarioAdmin = new UsuarioResponseDTO(
                1L, "Administrador do SaaS", null, null,
                null, "admin.saas", Role.ADMIN);
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login - credenciais válidas devem retornar 200 com o token")
    @WithMockUser
    void deveAutenticarERetornarToken() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(LoginResponseDTO.bearer("token.jwt.aqui", 28800, usuarioAdmin));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token.jwt.aqui"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(28800))
                .andExpect(jsonPath("$.usuario.username").value("admin.saas"))
                .andExpect(jsonPath("$.usuario.role").value("ADMIN"))
                // ADMIN do SaaS não tem filiação com nenhuma oficina
                .andExpect(jsonPath("$.usuario.oficinaId").value(nullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/login - credenciais inválidas devem retornar 401 sem revelar a causa")
    @WithMockUser
    void deveRetornar401ParaCredenciaisInvalidas() throws Exception {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /api/auth/login - username em branco deve retornar 400")
    @WithMockUser
    void deveRetornar400ParaPayloadInvalido() throws Exception {
        LoginRequestDTO invalido = new LoginRequestDTO("", "");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists());
    }

    // ─── GET /api/auth/me ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/auth/me - deve retornar os dados do usuário autenticado")
    @WithMockUser
    void deveRetornarUsuarioAutenticado() throws Exception {
        when(authService.usuarioLogado()).thenReturn(usuarioAdmin);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin.saas"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
