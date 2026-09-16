package com.oficinapro.exception.oficina;

public class OficinaDisabledException extends RuntimeException {
    
    public OficinaDisabledException() {
        super ("Oficina está desativada");
    }
}
