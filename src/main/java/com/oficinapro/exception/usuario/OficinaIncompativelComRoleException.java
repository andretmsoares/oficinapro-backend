package com.oficinapro.exception.usuario;

/**
 * O vínculo com oficina precisa ser coerente com o cargo:
 * ADMIN é o administrador do SaaS e não pertence a nenhuma oficina,
 * enquanto ADMINISTRATIVO e MECANICO pertencem obrigatoriamente a uma.
 */
public class OficinaIncompativelComRoleException extends RuntimeException {
    public OficinaIncompativelComRoleException(String message) {
        super(message);
    }
}
