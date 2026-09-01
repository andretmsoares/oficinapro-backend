package com.oficinapro.controller;

import com.oficinapro.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import tools.jackson.databind.ObjectMapper;
import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    private UsuarioResponseDTO responseDTO;
    private UsuarioRequestDTO requestDTO;
    private UsuarioUpdateRequestDTO updateRequestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new UsuarioResponseDTO(
                1L, "Ana Admin", "83944445555", "11122233344",
                1L, "ana.admin", Role.ADMIN
        );
        requestDTO = new UsuarioRequestDTO(
                "Ana Admin", "83944445555", "11122233344",
                1L, "ana.admin", "senha1234", Role.ADMIN
        );
        updateRequestDTO = new UsuarioUpdateRequestDTO(
                "Ana Admin", "83944445555", "11122233344",
                1L, "ana.admin", "senha1234", Role.ADMIN
        );
    }

    // ─── GET /api/usuarios ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/usuarios - ADMIN deve retornar 200 com página de usuários")
    @WithMockUser(roles = "ADMIN")
    void deveListarUsuariosComoAdmin() throws Exception {
        when(usuarioService.listar(any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Ana Admin"))
                .andExpect(jsonPath("$.content[0].username").value("ana.admin"));
    }

    @Test
    @DisplayName("GET /api/usuarios - ADMINISTRATIVO deve retornar 403 (somente ADMIN pode listar todos)")
    @WithMockUser(roles = "ADMINISTRATIVO")
    void deveNegarAcessoParaAdministrativo() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/usuarios/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/usuarios/{id} - ADMIN deve retornar 200 com o usuário correto")
    @WithMockUser(roles = "ADMIN")
    void deveBuscarUsuarioPorId() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Admin"))
                .andExpect(jsonPath("$.username").value("ana.admin"));
    }

    // ─── POST /api/usuarios ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/usuarios - ADMIN deve criar usuário e retornar 201")
    @WithMockUser(roles = "ADMIN")
    void deveCriarUsuario() throws Exception {
        when(usuarioService.criar(any(UsuarioRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ana.admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ─── PUT /api/usuarios/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/usuarios/{id} - ADMIN deve atualizar usuário e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarUsuario() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioUpdateRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/usuarios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Admin"));
    }

    // ─── DELETE /api/usuarios/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/usuarios/{id} - ADMIN deve excluir usuário e retornar 204")
    @WithMockUser(roles = "ADMIN")
    void deveDeletarUsuario() throws Exception {
        doNothing().when(usuarioService).deletar(1L);

        mockMvc.perform(delete("/api/usuarios/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
