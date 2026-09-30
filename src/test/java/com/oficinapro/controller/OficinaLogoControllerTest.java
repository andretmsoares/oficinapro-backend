package com.oficinapro.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.exception.GlobalExceptionHandler;
import com.oficinapro.exception.logo.LogoInvalidaException;
import com.oficinapro.exception.logo.LogoNotFoundException;
import com.oficinapro.exception.logo.LogoStorageIndisponivelException;
import com.oficinapro.service.oficina.OficinaLogoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(OficinaLogoController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class OficinaLogoControllerTest {

  private static final byte[] PNG = {
    (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
  };

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OficinaLogoService logoService;

  private MockMultipartHttpServletRequestBuilder put(String url) {
    // multipart() usa POST por padrão; a rota de upload é PUT.
    MockMultipartHttpServletRequestBuilder builder = multipart(HttpMethod.PUT, url);
    builder.with(csrf());
    return builder;
  }

  private static MockMultipartFile arquivo() {
    return new MockMultipartFile("arquivo", "logo.png", "image/png", PNG);
  }

  // ─── PUT /api/oficinas/{id}/logo ──────────────────────────────────────────────

  @Test
  @DisplayName("PUT logo - GERENTE envia arquivo válido e recebe 204")
  @WithMockUser(roles = "GERENTE")
  void deveAtualizarLogo() throws Exception {
    mockMvc.perform(put("/api/oficinas/1/logo").file(arquivo())).andExpect(status().isNoContent());

    verify(logoService).atualizar(eq(1L), any());
  }

  @Test
  @DisplayName("PUT logo - ADMIN também pode atualizar a logo")
  @WithMockUser(roles = "ADMIN")
  void adminPodeAtualizarLogo() throws Exception {
    mockMvc.perform(put("/api/oficinas/2/logo").file(arquivo())).andExpect(status().isNoContent());

    verify(logoService).atualizar(eq(2L), any());
  }

  @Test
  @DisplayName("PUT logo - MECANICO não pode atualizar (403)")
  @WithMockUser(roles = "MECANICO")
  void mecanicoNaoPodeAtualizarLogo() throws Exception {
    mockMvc.perform(put("/api/oficinas/1/logo").file(arquivo())).andExpect(status().isForbidden());

    verifyNoInteractions(logoService);
  }

  @Test
  @DisplayName("PUT logo - sem a parte 'arquivo' deve retornar 400")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400SemArquivo() throws Exception {
    mockMvc.perform(put("/api/oficinas/1/logo")).andExpect(status().isBadRequest());

    verifyNoInteractions(logoService);
  }

  @Test
  @DisplayName("PUT logo - formato inválido detectado pelo service vira 400 com a mensagem")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar400ParaLogoInvalida() throws Exception {
    doThrow(new LogoInvalidaException("Formato inválido. Envie uma imagem PNG ou JPEG."))
        .when(logoService)
        .atualizar(eq(1L), any());

    mockMvc
        .perform(put("/api/oficinas/1/logo").file(arquivo()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Formato inválido. Envie uma imagem PNG ou JPEG."));
  }

  @Test
  @DisplayName("PUT logo - oficina de outro tenant vira 403")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar403ParaOficinaDeOutroTenant() throws Exception {
    doThrow(new AccessDeniedException("outra oficina"))
        .when(logoService)
        .atualizar(eq(2L), any());

    mockMvc.perform(put("/api/oficinas/2/logo").file(arquivo())).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT logo - storage não configurado vira 503")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar503QuandoStorageIndisponivel() throws Exception {
    doThrow(new LogoStorageIndisponivelException()).when(logoService).atualizar(eq(1L), any());

    mockMvc
        .perform(put("/api/oficinas/1/logo").file(arquivo()))
        .andExpect(status().isServiceUnavailable());
  }

  // ─── DELETE /api/oficinas/{id}/logo ───────────────────────────────────────────

  @Test
  @DisplayName("DELETE logo - GERENTE remove e recebe 204")
  @WithMockUser(roles = "GERENTE")
  void deveRemoverLogo() throws Exception {
    mockMvc.perform(delete("/api/oficinas/1/logo").with(csrf())).andExpect(status().isNoContent());

    verify(logoService).remover(1L);
  }

  @Test
  @DisplayName("DELETE logo - MECANICO não pode remover (403)")
  @WithMockUser(roles = "MECANICO")
  void mecanicoNaoPodeRemoverLogo() throws Exception {
    mockMvc.perform(delete("/api/oficinas/1/logo").with(csrf())).andExpect(status().isForbidden());

    verifyNoInteractions(logoService);
  }

  // ─── GET /api/oficinas/{id}/logo ──────────────────────────────────────────────

  @Test
  @DisplayName("GET logo - devolve os bytes com o content-type e cache privado de 5 minutos")
  @WithMockUser(roles = "MECANICO")
  void deveBuscarLogo() throws Exception {
    when(logoService.buscar(1L)).thenReturn(new OficinaLogoService.Logo(PNG, "image/png"));

    mockMvc
        .perform(get("/api/oficinas/1/logo"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(PNG))
        .andExpect(header().string("Cache-Control", containsString("max-age=300")))
        .andExpect(header().string("Cache-Control", containsString("private")));
  }

  @Test
  @DisplayName("GET logo - oficina sem logo vira 404")
  @WithMockUser(roles = "GERENTE")
  void deveRetornar404QuandoNaoHaLogo() throws Exception {
    when(logoService.buscar(1L)).thenThrow(new LogoNotFoundException());

    mockMvc.perform(get("/api/oficinas/1/logo")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET logo - oficina de outro tenant vira 403")
  @WithMockUser(roles = "GERENTE")
  void buscarDeOutroTenantRetorna403() throws Exception {
    when(logoService.buscar(2L)).thenThrow(new AccessDeniedException("outra oficina"));

    mockMvc.perform(get("/api/oficinas/2/logo")).andExpect(status().isForbidden());
  }
}
