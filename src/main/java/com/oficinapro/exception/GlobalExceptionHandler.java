package com.oficinapro.exception;

import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.exception.item_os_peca.ItemOsPecaNotFoundException;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.exception.oficina.CnpjAlreadyExistsException;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.exception.ordem_servico.*;
import com.oficinapro.exception.pagamento.PagamentoNotFoundException;
import com.oficinapro.exception.pagamento.PagamentoNotFoundForThisOsException;
import com.oficinapro.exception.pagamento.PagamentoValorExcedidoException;
import com.oficinapro.exception.pagamento.PagamentoValorInvalidoException;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.exception.veiculo.VeiculoNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception exception) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso negado: Você não tem permissão para acessar este recurso.");
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

    @ExceptionHandler(ItemOsPecaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleItemOSNotFound(
            ItemOsPecaNotFoundException  exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PagamentoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePagamentoNotFound(
            PagamentoNotFoundException  exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PagamentoNotFoundForThisOsException.class)
    public ResponseEntity<Map<String, Object>> handlePagamentoNotFoundForThisOs(
            PagamentoNotFoundForThisOsException  exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
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

    @ExceptionHandler(UsuarioAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioAlreadyExists(
            UsuarioAlreadyExistsException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DescontoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleDescontoValueInvalid(
            DescontoInvalidoException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(OSCanceledException.class)
    public ResponseEntity<Map<String, Object>> handleOSCanceled(
            OSCanceledException exception) {
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(OSFinishedException.class)
    public ResponseEntity<Map<String, Object>> handleOSFinished(
            OSFinishedException exception) {
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(OSIsNotPossibleSwapWorkshopException.class)
    public ResponseEntity<Map<String, Object>> handleOSIsNotPossibleSwapWorkshop(
            OSIsNotPossibleSwapWorkshopException exception) {
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> body = new HashMap<>();

        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("message", "Dados inválidos");
        body.put("timestamp", LocalDateTime.now());
        body.put("fields", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message) {

        Map<String, Object> body = new HashMap<>();

        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(status)
                .body(body);
    }
}