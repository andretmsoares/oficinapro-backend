package com.oficinapro.exception;

public class CnpjAlreadyExistsException extends RuntimeException {

    public CnpjAlreadyExistsException(String cnpj) {
        super("Já existe uma oficina cadastrada com o CNPJ: " + cnpj);
    }
}