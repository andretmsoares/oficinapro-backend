package com.oficinapro.exception;

import com.oficinapro.exception.auth.ContaBloqueadaException;
import com.oficinapro.exception.auth.LoginTemporariamenteBloqueadoException;
import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaJaVinculadoException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.logo.LogoInvalidaException;
import com.oficinapro.exception.logo.LogoNotFoundException;
import com.oficinapro.exception.logo.LogoStorageIndisponivelException;
import com.oficinapro.exception.mao_obra.MaoObraNotFoundException;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaAlreadyActivatedException;
import com.oficinapro.exception.oficina.OficinaAlreadyDisabledException;
import com.oficinapro.exception.oficina.OficinaDisabledException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.exception.ordem_servico.*;
import com.oficinapro.exception.pagamento.PagamentoAlreadyExistsException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundForThisOsException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.exception.pagamento.PagamentoValorInvalidoException;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.exception.usuario.OficinaIncompativelComRoleException;
import com.oficinapro.exception.usuario.SenhaAtualInvalidaException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAcessDeniedException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioCannotDeleteSelfException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.exception.veiculo.PlacaAlreadyExistsException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception exception) {
    return buildResponse(
        HttpStatus.FORBIDDEN, "Acesso negado: Você não tem permissão para acessar este recurso.");
  }

  /**
   * Falha de login. A mensagem é sempre genérica, mesmo quando o username não existe, para não
   * permitir descobrir quais usuários estão cadastrados.
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthenticationFailure(
      AuthenticationException exception) {
    return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
  }

  @ExceptionHandler(LoginTemporariamenteBloqueadoException.class)
  public ResponseEntity<Map<String, Object>> handleLoginTemporariamenteBloqueado(
      LoginTemporariamenteBloqueadoException exception) {
    Map<String, Object> body =
        buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage()).getBody();
    body.put("retryAfterSeconds", exception.getSegundosRestantes());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(exception.getSegundosRestantes()))
        .body(body);
  }

  @ExceptionHandler(LogoInvalidaException.class)
  public ResponseEntity<Map<String, Object>> handleLogoInvalida(LogoInvalidaException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(LogoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleLogoNotFound(LogoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(LogoStorageIndisponivelException.class)
  public ResponseEntity<Map<String, Object>> handleLogoStorageIndisponivel(
      LogoStorageIndisponivelException exception) {
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
  }

  @SuppressWarnings("deprecation")
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleUploadGrande(
      MaxUploadSizeExceededException exception) {
    return buildResponse(
        HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo enviado excede o tamanho máximo.");
  }

  @ExceptionHandler(ContaBloqueadaException.class)
  public ResponseEntity<Map<String, Object>> handleContaBloqueada(
      ContaBloqueadaException exception) {
    return buildResponse(HttpStatus.LOCKED, exception.getMessage());
  }

  @ExceptionHandler(OficinaNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleOficinaNotFound(
      OficinaNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(UnidadeNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUnidadeNotFound(
      UnidadeNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(ClienteNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleClienteNotFound(
      ClienteNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MecanicoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleMecanicoNotFound(
      MecanicoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(UsuarioNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUsuarioNotFound(
      UsuarioNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(VeiculoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleVeiculoNotFound(
      VeiculoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(OrdemDeServicoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleOSNotFound(
      OrdemDeServicoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(MaoObraNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleMaoObraNotFound(
      MaoObraNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(ItemOsPecaNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleItemOSNotFound(
      ItemOsPecaNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PagamentoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handlePagamentoNotFound(
      PagamentoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PagamentoNotFoundForThisOsException.class)
  public ResponseEntity<Map<String, Object>> handlePagamentoNotFoundForThisOs(
      PagamentoNotFoundForThisOsException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  /**
   * A relação OS↔pagamento é 1:1, garantida também pela constraint uk_pagamento_os. Sem este
   * handler a violação da regra escapava como 500, escondendo um erro de uso da API atrás de um
   * erro de servidor.
   */
  @ExceptionHandler(PagamentoAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handlePagamentoAlreadyExists(
      PagamentoAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(RegistroPagamentoNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleRegistroPagamentoNotFoundForThisOs(
      RegistroPagamentoNotFoundException exception) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(PagamentoValorExcedidoException.class)
  public ResponseEntity<Map<String, Object>> handleValorPagamentoExcedido(
      PagamentoValorExcedidoException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(CnpjAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleCnpjAlreadyExists(
      CnpjAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(PlacaAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handlePlacaAlreadyExists(
      PlacaAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(PagamentoValorInvalidoException.class)
  public ResponseEntity<Map<String, Object>> handlePagamentoValorInvalid(
      PagamentoValorInvalidoException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(EnderecoAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleEnderecoAlreadyExists(
      EnderecoAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(ClienteAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleClienteAlreadyExists(
      ClienteAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(MecanicoAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleMecanicoAlreadyExists(
      MecanicoAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(UsernameAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleUsernameAlreadyExists(
      UsernameAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(OficinaIncompativelComRoleException.class)
  public ResponseEntity<Map<String, Object>> handleOficinaIncompativelComRole(
      OficinaIncompativelComRoleException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(UsuarioAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleUsuarioAlreadyExists(
      UsuarioAlreadyExistsException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(OrdemDeServicoImpossibleDeleteException.class)
  public ResponseEntity<Map<String, Object>> handleOrdemDeServicoImpossibleDelete(
      OrdemDeServicoImpossibleDeleteException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(OficinaAlreadyActivatedException.class)
  public ResponseEntity<Map<String, Object>> handleOficinaAlreadyActivated(
      OficinaAlreadyActivatedException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(ItemOsPecaJaVinculadoException.class)
  public ResponseEntity<Map<String, Object>> handleItemOsPecaJaVinculado(
      ItemOsPecaJaVinculadoException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(OficinaAlreadyDisabledException.class)
  public ResponseEntity<Map<String, Object>> handleOficinaAlreadyDisabled(
      OficinaAlreadyDisabledException exception) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(DescontoInvalidoException.class)
  public ResponseEntity<Map<String, Object>> handleDescontoValueInvalid(
      DescontoInvalidoException exception) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(OSCanceledException.class)
  public ResponseEntity<Map<String, Object>> handleOSCanceled(OSCanceledException exception) {
    return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
  }

  @ExceptionHandler(OSFinishedException.class)
  public ResponseEntity<Map<String, Object>> handleOSFinished(OSFinishedException exception) {
    return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
  }

  @ExceptionHandler(OSIsNotPossibleSwapWorkshopException.class)
  public ResponseEntity<Map<String, Object>> handleOSIsNotPossibleSwapWorkshop(
      OSIsNotPossibleSwapWorkshopException exception) {
    return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
  }

  @ExceptionHandler(UsuarioAcessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleUsuarioAcessDenied(
      UsuarioAcessDeniedException exception) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(OficinaDisabledException.class)
  public ResponseEntity<Map<String, Object>> handleOficinaDisabled(
      OficinaDisabledException exception) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(UsuarioCannotDeleteSelfException.class)
  public ResponseEntity<Map<String, Object>> handleUsuarioCannotDeleteSelf(
      UsuarioCannotDeleteSelfException exception) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {

    return buildResponse(
        HttpStatus.CONFLICT,
        "Conflito de dados. Não foi possível realizar a operação porque os dados violam uma regra de integridade.");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(
      OptimisticLockingFailureException ex) {

    return buildResponse(
        HttpStatus.CONFLICT,
        "O registro foi alterado por outro usuário. Atualize os dados e tente novamente.");
  }

  @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
  public ResponseEntity<Map<String, Object>> handleIncorrectResultSize(
      IncorrectResultSizeDataAccessException ex) {

    return buildResponse(
        HttpStatus.CONFLICT,
        "Dados inconsistentes. A consulta retornou mais registros do que o esperado.");
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {

    // A mensagem de regra de negócio não vai ao cliente, mas precisa ficar no servidor: sem log,
    // um bug de verdade se disfarçaria de "operação inválida".
    log.warn("Operação recusada por regra de negócio: {}", ex.getMessage());

    return buildResponse(HttpStatus.BAD_REQUEST, "Operação inválida");
  }

  @ExceptionHandler(SenhaAtualInvalidaException.class)
  public ResponseEntity<Map<String, Object>> handleSenhaAtualInvalida(
      SenhaAtualInvalidaException exception) {
    // 400 e não 401: o frontend trata 401 como sessão expirada e derrubaria o usuário logado.
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  /**
   * Rede de segurança: qualquer exceção inesperada vira um 500 genérico (sem stack trace nem
   * detalhe interno) e é registrada no servidor. Exceções do próprio Spring MVC (404, 405, 415...)
   * implementam {@link ErrorResponse} e mantêm o status original.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {

    if (ex instanceof ErrorResponse errorResponse) {
      HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
      return buildResponse(
          status != null ? status : HttpStatus.BAD_REQUEST, "Requisição não pôde ser atendida");
    }

    log.error("Erro inesperado", ex);

    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");
  }

  @ExceptionHandler(InvalidDataAccessApiUsageException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidDataAccessApiUsage(
      InvalidDataAccessApiUsageException ex) {

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "Requisição inválida. Os dados informados não podem ser utilizados nesta operação.");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {

    return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro inválido");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "Corpo da requisição inválido. Não foi possível interpretar o corpo da requisição.");
  }

  @ExceptionHandler(DateTimeException.class)
  public ResponseEntity<Map<String, Object>> handleDateTimeException(DateTimeException ex) {

    return buildResponse(
        HttpStatus.BAD_REQUEST, "Data ou hora inválida. A data ou hora informada é inválida.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException exception) {

    Map<String, String> errors = new HashMap<>();

    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    Map<String, Object> body = new HashMap<>();

    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation Error");
    body.put("message", "Dados inválidos");
    body.put("timestamp", LocalDateTime.now());
    body.put("fields", errors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {

    Map<String, Object> body = new HashMap<>();

    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    body.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(status).body(body);
  }
}
