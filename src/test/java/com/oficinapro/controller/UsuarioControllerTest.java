package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.responseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.service.usuario.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UsuarioController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class UsuarioControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UsuarioService usuarioService;

  private responseDTO responseDTO;
  private UsuarioRequestDTO requestDTO;
  private UsuarioUpdateRequestDTO updateRequestDTO;

  @BeforeEach
  void setUp() {
    responseDTO =
        new responseDTO(
            1L, "Ana Admin", "83944445555", "11122233344", 1L, "ana.admin", Role.ADMIN);
    requestDTO =
        new UsuarioRequestDTO(
            "Ana Admin", "83944445555", "11122233344", 1L, "ana.admin", "senha1234", Role.ADMIN);
    updateRequestDTO =
        new UsuarioUpdateRequestDTO(
            "Ana Admin", "83944445555", "11122233344", 1L, "ana.admin", "senha1234", Role.ADMIN);
  }

  // ─── GET /api/usuarios ────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/usuarios - ADMIN deve retornar 200 com página de usuários")
  @WithMockUser(roles = "ADMIN")
  void deveListarUsuariosComoAdmin() throws Exception {
    when(usuarioService.listar(any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

    mockMvc
        .perform(get("/api/usuarios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].nome").value("Ana Admin"))
        .andExpect(jsonPath("$.content[0].username").value("ana.admin"));
  }

  @Test
  @DisplayName("GET /api/usuarios - GERENTE deve retornar 403 (somente ADMIN pode listar todos)")
  @WithMockUser(roles = "GERENTE")
  void deveNegarAcessoParaGerente() throws Exception {
    mockMvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
  }

  // ─── GET /api/usuarios/{id} ───────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/usuarios/{id} - ADMIN deve retornar 200 com o usuário correto")
  @WithMockUser(roles = "ADMIN")
  void deveBuscarUsuarioPorId() throws Exception {
    when(usuarioService.buscarPorId(1L)).thenReturn(responseDTO);

    mockMvc
        .perform(get("/api/usuarios/1"))
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

    mockMvc
        .perform(
            post("/api/usuarios")
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
    when(usuarioService.atualizar(eq(1L), any(UsuarioUpdateRequestDTO.class)))
        .thenReturn(responseDTO);

    mockMvc
        .perform(
            put("/api/usuarios/1")
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

    mockMvc.perform(delete("/api/usuarios/1").with(csrf())).andExpect(status().isNoContent());
  }

  // ─────────── busca global do ADMIN (rotas /admin/**) ───────────
  //
  // As rotas antigas /nome e /documento escopavam pela oficina do usuário logado
  // e por isso falhavam sempre para o ADMIN, que não pertence a oficina alguma.
  // Passaram a ser de GERENTE, e o ADMIN ganhou rotas próprias, sem escopo.

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/usuarios/admin/nome/{nome} - ADMIN busca em todas as oficinas")
  void buscarPorNomeAdmin_admin_retorna200() throws Exception {
    when(usuarioService.buscarPorNomeAdmin("Ana")).thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/usuarios/admin/nome/Ana"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/usuarios/admin/nome/{nome} - GERENTE recebe 403")
  void buscarPorNomeAdmin_gerente_retorna403() throws Exception {
    mockMvc.perform(get("/api/usuarios/admin/nome/Ana")).andExpect(status().isForbidden());

    verify(usuarioService, never()).buscarPorNomeAdmin(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/usuarios/admin/documento/{doc} - ADMIN busca em todas as oficinas")
  void buscarPorDocumentoAdmin_admin_retorna200() throws Exception {
    when(usuarioService.buscarPorDocumentoAdmin("12345678901"))
        .thenReturn(List.of(responseDTO));

    mockMvc
        .perform(get("/api/usuarios/admin/documento/12345678901"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  @WithMockUser(roles = "MECANICO")
  @DisplayName("GET /api/usuarios/admin/documento/{doc} - MECANICO recebe 403")
  void buscarPorDocumentoAdmin_mecanico_retorna403() throws Exception {
    mockMvc
        .perform(get("/api/usuarios/admin/documento/12345678901"))
        .andExpect(status().isForbidden());

    verify(usuarioService, never()).buscarPorDocumentoAdmin(any());
  }

  @Test
  @WithMockUser(roles = "GERENTE")
  @DisplayName("GET /api/usuarios/nome/{nome} - a rota escopada agora é do GERENTE")
  void buscarPorNome_gerente_retorna200() throws Exception {
    when(usuarioService.buscarPorNome("Ana")).thenReturn(List.of(responseDTO));

    mockMvc.perform(get("/api/usuarios/nome/Ana")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/usuarios/nome/{nome} - ADMIN recebe 403 na rota escopada por oficina")
  void buscarPorNome_admin_retorna403() throws Exception {
    mockMvc
        .perform(get("/api/usuarios/nome/Ana"))
        .andExpect(status().isForbidden());

    verify(usuarioService, never()).buscarPorNome(any());
  }
}