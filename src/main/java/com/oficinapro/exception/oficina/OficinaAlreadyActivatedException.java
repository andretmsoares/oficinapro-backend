package com.oficinapro.exception.oficina;

public class OficinaAlreadyActivatedException extends RuntimeException{
    public OficinaAlreadyActivatedException() {
        super("Oficina já ativada");
    }
    
}
