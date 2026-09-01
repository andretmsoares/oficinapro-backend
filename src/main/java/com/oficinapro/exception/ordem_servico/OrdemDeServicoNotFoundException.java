package com.oficinapro.exception.ordem_servico;

public class OrdemDeServicoNotFoundException extends RuntimeException {
    public OrdemDeServicoNotFoundException(Long id) {
        super("OS não encontrada com o id: " + id);
    }
}
