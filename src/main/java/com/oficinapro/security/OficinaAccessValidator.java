package com.oficinapro.security;

import com.oficinapro.model.Usuario;
import com.oficinapro.security.role.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OficinaAccessValidator {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public OficinaAccessValidator(
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public Usuario getUsuarioAutenticado() {
        return authenticatedUserProvider.getUsuarioAutenticado();
    }

    public void validarRole(Role... rolesPermitidas) {
        Usuario logado = getUsuarioAutenticado();

        for (Role role : rolesPermitidas) {
            if (logado.getRole() == role) {
                return;
            }
        }

        throw new AccessDeniedException(
                "Usuário não possui permissão para realizar esta operação"
        );
    }

    /**
     * Retorna a oficina do usuário logado.
     * <p>
     * Usuários ADMIN do SaaS não possuem oficina.
     */
    public Long getOficinaIdUsuarioLogado() {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        Long oficinaId = logado.getOficina() != null
                ? logado.getOficina().getId()
                : null;

        if (oficinaId == null) {
            throw new AccessDeniedException(
                    "Usuário não está vinculado a nenhuma oficina"
            );
        }

        return oficinaId;
    }

    /**
     * Valida se o usuário logado pode operar sobre a oficina informada.
     * <p>
     * ADMIN do SaaS pode operar sobre qualquer oficina.
     */
    public void validarAcessoOficina(Long oficinaId) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        if (logado.getRole() == Role.ADMIN) {
            return;
        }

        Long oficinaDoLogado = getOficinaIdUsuarioLogado();

        if (!oficinaDoLogado.equals(oficinaId)) {
            throw new AccessDeniedException(
                    "Você só pode acessar dados da sua própria oficina"
            );
        }
    }

    /**
     * Valida acesso a um registro através da oficina à qual ele pertence.
     * <p>
     * Para evitar vazamento de informação, retorna uma exceção de "não encontrado"
     * fornecida pelo service chamador.
     */
    public void validarAcessoAoRegistro(
            Long oficinaDoRegistro,
            RuntimeException notFoundException
    ) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        if (logado.getRole() == Role.ADMIN) {
            return;
        }

        Long oficinaDoLogado = getOficinaIdUsuarioLogado();

        if (!oficinaDoLogado.equals(oficinaDoRegistro)) {
            throw notFoundException;
        }
    }

    /**
     * Versão para registros que necessariamente possuem oficina.
     */
    public void validarAcessoAoRegistro(
            Long oficinaDoRegistro,
            Long id,
            java.util.function.Function<Long, RuntimeException> notFoundException
    ) {
        validarAcessoAoRegistro(
                oficinaDoRegistro,
                notFoundException.apply(id)
        );
    }
}