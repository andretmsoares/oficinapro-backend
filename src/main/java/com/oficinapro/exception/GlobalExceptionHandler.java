package com.oficinapro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // EXCEPTIONS DE RECURSOS NÃO ENCONTRADOS (404)
    // ==========================================

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

    @ExceptionHandler(OrdemDeServicoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOSNotFound(
            OrdemDeServicoNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    // ==========================================
    // EXCEPTIONS DE CONFLITOS / DADOS DUPLICADOS (409)
    // ==========================================

    @ExceptionHandler(CnpjAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCnpjAlreadyExists(
            CnpjAlreadyExistsException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
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

    // ==========================================
    // EXCEPTIONS DE VALIDAÇÃO DE CAMPOS (400)
    // ==========================================

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

    // ==========================================
    // MÉTODO AUXILIAR
    // ==========================================

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