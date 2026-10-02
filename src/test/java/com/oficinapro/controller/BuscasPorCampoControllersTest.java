package com.oficinapro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.dto.usuario.UsuarioMeUpdateRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.veiculo.VeiculoResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import com.oficinapro.service.cliente.ClienteService;
import com.oficinapro.service.mecanico.MecanicoService;
import com.oficinapro.service.oficina.OficinaService;
import com.oficinapro.service.usuario.UsuarioService;
import com.oficinapro.service.veiculo.VeiculoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Endpoints de busca por nome/documento/placa e demais rotas de Cliente, Mecânico, Usuário, Veículo
 * e Oficina que os testes de controller existentes não cobriam.
 */
@WebMvcTest({
  ClienteController.class,
  MecanicoController.class,
  UsuarioController.class,
  VeiculoController.class,
  OficinaController.class
})
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class BuscasPorCampoControllersTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ClienteService clienteService;
  @MockitoBean private MecanicoService mecanicoService;
  @MockitoBean private UsuarioService usuarioService;
  @MockitoBean private VeiculoService veiculoService;
  @MockitoBean private OficinaService oficinaService;

  private final ClienteResponseDTO cliente =
      new ClienteResponseDTO(1L, "JOAO SILVA", "83988887777", "12345678901", 1L);
  private final MecanicoResponseDTO mecanico =
      new MecanicoResponseDTO(
          1L, "CARLOS", "83988887777", "98765432100", 1L, new BigDecimal("2500"), "obs");
  private final UsuarioResponseDTO usuario =
      new UsuarioResponseDTO(
          1L, "ANA", "83988887777", "12345678901", 1L, "ana", Role.GERENTE, false);
  private final VeiculoResponseDTO veiculo =
      new VeiculoResponseDTO(1L, 1L, "CIVIC", 2020, "HONDA", "AZUL", "ABC1234");

  // ─── Cliente ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/clientes/nome/{nome} - MECANICO consulta por nome e recebe 200")
  @WithMockUser(roles = "MECANICO")
  void clientePorNome() throws Exception {
    when(clienteService.buscarPorNome("joao")).thenReturn(List.of(cliente));

    mockMvc
        .perform(get("/api/clientes/nome/joao"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("JOAO SILVA"));
  }

  @Test
  @DisplayName("GET /api/clientes/nome/{nome} - ADMIN do SaaS recebe 403")
  @WithMockUser(roles = "ADMIN")
  void clientePorNomeNegadoParaAdmin() throws Exception {
    mockMvc.perform(get("/api/clientes/nome/joao")).andExpect(status().isForbidden());

    verifyNoInteractions(clienteService);
  }

  @Test
  @DisplayName("POST /api/clientes/documento/buscar - encontrado retorna 200")
  @WithMockUser(roles = "GERENTE")
  void clientePorDocumento() throws Exception {
    when(clienteService.buscarPorDocumento("12345678901")).thenReturn(cliente);

    mockMvc
        .perform(
            post("/api/clientes/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documento").value("12345678901"));
  }

  @Test
  @DisplayName("POST /api/clientes/documento/buscar - inexistente retorna 404")
  @WithMockUser(roles = "GERENTE")
  void clientePorDocumentoInexistente() throws Exception {
    when(clienteService.buscarPorDocumento("000")).thenThrow(new ClienteNotFoundException());

    mockMvc
        .perform(
            post("/api/clientes/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"000\"}"))
        .andExpect(status().isNotFound());
  }

  // ─── Mecânico ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/mecanicos/nome/{nome} - GERENTE consulta por nome e recebe 200")
  @WithMockUser(roles = "GERENTE")
  void mecanicoPorNome() throws Exception {
    when(mecanicoService.buscarPorNome("carlos")).thenReturn(List.of(mecanico));

    mockMvc
        .perform(get("/api/mecanicos/nome/carlos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("CARLOS"));
  }

  @Test
  @DisplayName("GET /api/mecanicos/nome/{nome} - MECANICO recebe 403")
  @WithMockUser(roles = "MECANICO")
  void mecanicoPorNomeNegadoParaMecanico() throws Exception {
    mockMvc.perform(get("/api/mecanicos/nome/carlos")).andExpect(status().isForbidden());

    verifyNoInteractions(mecanicoService);
  }

  @Test
  @DisplayName("POST /api/mecanicos/documento/buscar - encontrado retorna 200")
  @WithMockUser(roles = "GERENTE")
  void mecanicoPorDocumento() throws Exception {
    when(mecanicoService.buscarPorDocumento("98765432100")).thenReturn(mecanico);

    mockMvc
        .perform(
            post("/api/mecanicos/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"98765432100\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.salario").value(2500));
  }

  @Test
  @DisplayName("POST /api/mecanicos/documento/buscar - inexistente retorna 404")
  @WithMockUser(roles = "GERENTE")
  void mecanicoPorDocumentoInexistente() throws Exception {
    when(mecanicoService.buscarPorDocumento("000")).thenThrow(new MecanicoNotFoundException());

    mockMvc
        .perform(
            post("/api/mecanicos/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"000\"}"))
        .andExpect(status().isNotFound());
  }

  // ─── Veículo ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/veiculos/placa/{placa} - encontrado retorna 200")
  @WithMockUser(roles = "MECANICO")
  void veiculoPorPlaca() throws Exception {
    when(veiculoService.buscarPorPlaca("ABC-1234")).thenReturn(veiculo);

    mockMvc
        .perform(get("/api/veiculos/placa/ABC-1234"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.placa").value("ABC1234"));
  }

  @Test
  @DisplayName("GET /api/veiculos/placa/{placa} - inexistente retorna 404")
  @WithMockUser(roles = "GERENTE")
  void veiculoPorPlacaInexistente() throws Exception {
    when(veiculoService.buscarPorPlaca("ZZZ0000")).thenThrow(new VeiculoNotFoundException(null));

    mockMvc.perform(get("/api/veiculos/placa/ZZZ0000")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/veiculos/placa/{placa} - ADMIN do SaaS recebe 403")
  @WithMockUser(roles = "ADMIN")
  void veiculoPorPlacaNegadoParaAdmin() throws Exception {
    mockMvc.perform(get("/api/veiculos/placa/ABC1234")).andExpect(status().isForbidden());

    verifyNoInteractions(veiculoService);
  }

  // ─── Usuário ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/usuarios/documento/buscar - GERENTE encontra o usuário")
  @WithMockUser(roles = "GERENTE")
  void usuarioPorDocumento() throws Exception {
    when(usuarioService.buscarPorDocumento("12345678901")).thenReturn(usuario);

    mockMvc
        .perform(
            post("/api/usuarios/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("ana"));
  }

  @Test
  @DisplayName("POST /api/usuarios/documento/buscar - ADMIN recebe 403 (rota é de GERENTE)")
  @WithMockUser(roles = "ADMIN")
  void usuarioPorDocumentoNegadoParaAdmin() throws Exception {
    mockMvc
        .perform(
            post("/api/usuarios/documento/buscar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(usuarioService);
  }

  @Test
  @DisplayName("PUT /api/usuarios/me - qualquer perfil autenticado atualiza a própria conta")
  @WithMockUser(roles = "MECANICO")
  void atualizarMe() throws Exception {
    when(usuarioService.atualizarMe(any(UsuarioMeUpdateRequestDTO.class))).thenReturn(usuario);

    mockMvc
        .perform(
            put("/api/usuarios/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UsuarioMeUpdateRequestDTO(
                            "Ana", "12345678901", "83988887777", "ana", null, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("ana"));
  }

  @Test
  @DisplayName("PUT /api/usuarios/me - campos obrigatórios em branco retornam 400")
  @WithMockUser(roles = "GERENTE")
  void atualizarMeComPayloadInvalido() throws Exception {
    mockMvc
        .perform(
            put("/api/usuarios/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UsuarioMeUpdateRequestDTO("", "", "", "", null, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.nome").exists())
        .andExpect(jsonPath("$.fields.username").exists());

    verifyNoInteractions(usuarioService);
  }

  @Test
  @DisplayName("PUT /api/usuarios/me - username já usado vira 409")
  @WithMockUser(roles = "GERENTE")
  void atualizarMeComUsernameEmUso() throws Exception {
    when(usuarioService.atualizarMe(any(UsuarioMeUpdateRequestDTO.class)))
        .thenThrow(new UsernameAlreadyExistsException());

    mockMvc
        .perform(
            put("/api/usuarios/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UsuarioMeUpdateRequestDTO(
                            "Ana", "12345678901", "83988887777", "ocupado", null, "senhaAtual1"))))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("PATCH /api/usuarios/{id}/desbloquear - GERENTE desbloqueia e recebe 204")
  @WithMockUser(roles = "GERENTE")
  void desbloquearUsuario() throws Exception {
    mockMvc
        .perform(patch("/api/usuarios/1/desbloquear").with(csrf()))
        .andExpect(status().isNoContent());

    verify(usuarioService).desbloquear(1L);
  }

  @Test
  @DisplayName("PATCH /api/usuarios/{id}/desbloquear - usuário inexistente vira 404")
  @WithMockUser(roles = "ADMIN")
  void desbloquearUsuarioInexistente() throws Exception {
    org.mockito.Mockito.doThrow(new UsuarioNotFoundException())
        .when(usuarioService)
        .desbloquear(99L);

    mockMvc
        .perform(patch("/api/usuarios/99/desbloquear").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("PATCH /api/usuarios/{id}/desbloquear - MECANICO recebe 403")
  @WithMockUser(roles = "MECANICO")
  void desbloquearNegadoParaMecanico() throws Exception {
    mockMvc
        .perform(patch("/api/usuarios/1/desbloquear").with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(usuarioService);
  }

  // ─── Oficina ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /api/oficinas/buscar - ADMIN busca paginado repassando o termo")
  @WithMockUser(roles = "ADMIN")
  void oficinaBuscar() throws Exception {
    OficinaResponseDTO oficina =
        new OficinaResponseDTO(1L, "OFICINA CENTRAL", "12345678000195", "83999998888", true);
    when(oficinaService.buscar(org.mockito.ArgumentMatchers.eq("central"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(oficina)));

    mockMvc
        .perform(get("/api/oficinas/buscar").param("search", "central"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nome").value("OFICINA CENTRAL"));
  }

  @Test
  @DisplayName("GET /api/oficinas/buscar - sem termo usa string vazia")
  @WithMockUser(roles = "ADMIN")
  void oficinaBuscarSemTermo() throws Exception {
    when(oficinaService.buscar(org.mockito.ArgumentMatchers.eq(""), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc
        .perform(get("/api/oficinas/buscar"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  @DisplayName("GET /api/oficinas/buscar - GERENTE recebe 403")
  @WithMockUser(roles = "GERENTE")
  void oficinaBuscarNegadoParaGerente() throws Exception {
    mockMvc.perform(get("/api/oficinas/buscar")).andExpect(status().isForbidden());

    verifyNoInteractions(oficinaService);
  }
}
