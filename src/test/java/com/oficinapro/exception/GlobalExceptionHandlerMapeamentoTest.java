package com.oficinapro.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oficinapro.exception.auth.ContaBloqueadaException;
import com.oficinapro.exception.auth.LoginTemporariamenteBloqueadoException;
import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaJaVinculadoException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.logo.LogoInvalidaException;
import com.oficinapro.exception.logo.LogoNotFoundException;
import com.oficinapro.exception.logo.LogoStorageIndisponivelException;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaAlreadyActivatedException;
import com.oficinapro.exception.oficina.OficinaAlreadyDisabledException;
import com.oficinapro.exception.oficina.OficinaDisabledException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoImpossibleDeleteException;
import com.oficinapro.exception.ordem_servico.OrdemDeServicoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundForThisOsException;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioCannotDeleteSelfException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Complementa {@link GlobalExceptionHandlerTest}: verifica o mapeamento exceção → status HTTP dos
 * handlers que ainda não tinham cobertura (login, logo, upload e as demais exceções de domínio).
 */
class GlobalExceptionHandlerMapeamentoTest {

  private static final Map<String, Supplier<RuntimeException>> EXCECOES = new HashMap<>();

  private static void reg(String cenario, Supplier<RuntimeException> excecao) {
    EXCECOES.put(cenario, excecao);
  }

  static {
              reg("cliente-nf", ClienteNotFoundException::new);
              reg("mecanico-nf", MecanicoNotFoundException::new);
              reg("usuario-nf", UsuarioNotFoundException::new);
              reg("unidade-nf", () -> new UnidadeNotFoundException(1L));
              reg("veiculo-nf", () -> new VeiculoNotFoundException(1L));
              reg("os-nf", () -> new OrdemDeServicoNotFoundException(1L));
              reg("item-nf", ItemOsPecaNotFoundException::new);
              reg("pagamento-nf", () -> new PagamentoNotFoundException(1L));
              reg("pagamento-os-nf", () -> new PagamentoNotFoundForThisOsException(1L));
              reg("registro-nf", () -> new RegistroPagamentoNotFoundException(1L));
              reg("logo-nf", LogoNotFoundException::new);
              reg("cnpj-dup", () -> new CnpjAlreadyExistsException("123"));
              reg("endereco-dup", () -> new EnderecoAlreadyExistsException("Rua A"));
              reg("cliente-dup", ClienteAlreadyExistsException::new);
              reg("mecanico-dup", MecanicoAlreadyExistsException::new);
              reg("username-dup", UsernameAlreadyExistsException::new);
              reg("usuario-dup", UsuarioAlreadyExistsException::new);
              reg("item-vinculado", ItemOsPecaJaVinculadoException::new);
              reg("os-delete", OrdemDeServicoImpossibleDeleteException::new);
              reg("oficina-ativa", OficinaAlreadyActivatedException::new);
              reg("oficina-desativada-ja", OficinaAlreadyDisabledException::new);
              reg("usuario-sem-oficina", UsuarioAcessDeniedException::new);
              reg("oficina-disabled", OficinaDisabledException::new);
              reg("auto-exclusao", UsuarioCannotDeleteSelfException::new);
              reg("conta-bloqueada", ContaBloqueadaException::new);
              reg("logo-invalida", () -> new LogoInvalidaException("Formato inválido"));
              reg("logo-storage", LogoStorageIndisponivelException::new);
              reg("credenciais", () -> new BadCredentialsException("Bad credentials"));
              reg("upload-grande", () -> new MaxUploadSizeExceededException(1L));
  }

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  static Stream<Arguments> mapeamentos() {
    return Stream.of(
        Arguments.of("cliente-nf", 404),
        Arguments.of("mecanico-nf", 404),
        Arguments.of("usuario-nf", 404),
        Arguments.of("unidade-nf", 404),
        Arguments.of("veiculo-nf", 404),
        Arguments.of("os-nf", 404),
        Arguments.of("item-nf", 404),
        Arguments.of("pagamento-nf", 404),
        Arguments.of("pagamento-os-nf", 404),
        Arguments.of("registro-nf", 404),
        Arguments.of("logo-nf", 404),
        Arguments.of("cnpj-dup", 409),
        Arguments.of("endereco-dup", 409),
        Arguments.of("cliente-dup", 409),
        Arguments.of("mecanico-dup", 409),
        Arguments.of("username-dup", 409),
        Arguments.of("usuario-dup", 409),
        Arguments.of("item-vinculado", 409),
        Arguments.of("os-delete", 409),
        Arguments.of("oficina-ativa", 409),
        Arguments.of("oficina-desativada-ja", 409),
        Arguments.of("usuario-sem-oficina", 403),
        Arguments.of("oficina-disabled", 403),
        Arguments.of("auto-exclusao", 403),
        Arguments.of("conta-bloqueada", 423),
        Arguments.of("logo-invalida", 400),
        Arguments.of("logo-storage", 503),
        Arguments.of("credenciais", 401),
        Arguments.of("upload-grande", 413));
  }

  @ParameterizedTest(name = "{0} deve virar HTTP {1}")
  @MethodSource("mapeamentos")
  @DisplayName("cada exceção de domínio deve ser traduzida para o status HTTP correspondente")
  void deveTraduzirExcecaoParaStatus(String cenario, int statusEsperado) throws Exception {
    mockMvc
        .perform(get("/teste/" + cenario))
        .andExpect(status().is(statusEsperado))
        .andExpect(jsonPath("$.status").value(statusEsperado))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  @DisplayName("credenciais inválidas devem devolver mensagem genérica, sem revelar a causa")
  void credenciaisInvalidasNaoRevelamCausa() throws Exception {
    mockMvc
        .perform(get("/teste/credenciais"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
  }

  @Test
  @DisplayName("upload grande demais deve devolver 413 com mensagem de tamanho máximo")
  void uploadGrandeDevolveMensagemDeTamanhoMaximo() throws Exception {
    mockMvc
        .perform(get("/teste/upload-grande"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.message").value("O arquivo enviado excede o tamanho máximo."));
  }

  @Test
  @DisplayName("login temporariamente bloqueado deve virar 429 com Retry-After e retryAfterSeconds")
  void loginTemporariamenteBloqueadoVira429ComRetryAfter() throws Exception {
    mockMvc
        .perform(get("/teste/login-bloqueado"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "42"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(42))
        .andExpect(jsonPath("$.status").value(429));
  }

  @RestController
  @RequestMapping("/teste")
  static class ControllerDeTeste {

    @GetMapping("/login-bloqueado")
    public void loginBloqueado() {
      throw new LoginTemporariamenteBloqueadoException(42);
    }

    @GetMapping("/{cenario}")
    public void lanca(@PathVariable String cenario) {
      throw EXCECOES.get(cenario).get();
    }
  }
}
