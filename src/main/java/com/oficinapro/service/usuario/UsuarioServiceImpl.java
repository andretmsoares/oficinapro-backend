package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.exception.usuario.OficinaIncompativelComRoleException;
import com.oficinapro.exception.usuario.UsernameAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioAlreadyExistsException;
import com.oficinapro.exception.usuario.UsuarioNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl
        extends AbstractPessoaServiceImpl<Usuario, UsuarioRequestDTO, UsuarioUpdateRequestDTO, UsuarioResponseDTO>
        implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              OficinaServiceImpl oficinaService,
                              PessoaService pessoaService,
                              PasswordEncoder passwordEncoder,
                              AuthenticatedUserProvider authenticatedUserProvider) {
        super(usuarioRepository, oficinaService, pessoaService, authenticatedUserProvider);
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Override
    protected void validateBeforeCreate(UsuarioRequestDTO request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }
        // Permissão antes de coerência: quem não pode atribuir a role recebe 403,
        // sem pistas sobre o formato correto do payload.
        validarPermissaoParaAtribuirRole(request.role());
        validarCoerenciaRoleOficina(request.role(), request.oficinaId());
    }

    @Override
    protected void validateBeforeUpdate(Long id, UsuarioUpdateRequestDTO request) {
        if (usuarioRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new UsernameAlreadyExistsException();
        }

        Usuario alvo = buscarPorEntidadeId(id);
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        if (alvo.getRole() == Role.ADMIN && logado.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas ADMIN pode editar uma conta com role ADMIN");
        }

        validarPermissaoParaAtribuirRole(request.role());
        validarCoerenciaRoleOficina(request.role(), request.oficinaId());
    }

    /**
     * ADMIN é o administrador do SaaS: não tem filiação com nenhuma oficina.
     * ADMINISTRATIVO e MECANICO existem sempre dentro de uma oficina.
     */
    private void validarCoerenciaRoleOficina(Role role, Long oficinaId) {
        if (role == Role.ADMIN && oficinaId != null) {
            throw new OficinaIncompativelComRoleException(
                    "ADMIN é o administrador do SaaS e não pode ser vinculado a uma oficina");
        }
        if (role != Role.ADMIN && oficinaId == null) {
            throw new OficinaIncompativelComRoleException(
                    "O ID da oficina é obrigatório para o cargo " + role);
        }
    }

    /**
     * Hierarquia de criação/promoção de usuários:
     * - ADMIN (do SaaS) pode atribuir qualquer role, inclusive ADMIN.
     * - ADMINISTRATIVO (da oficina) pode criar ADMINISTRATIVO e MECANICO, nunca ADMIN.
     *   O isolamento por oficina é garantido em AbstractPessoaServiceImpl.resolverOficina.
     * - MECANICO não gerencia usuários (já bloqueado no @PreAuthorize do controller,
     *   replicado aqui como defesa em profundidade).
     */
    private void validarPermissaoParaAtribuirRole(Role roleAlvo) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        Role roleLogado = logado.getRole();

        boolean podeGerenciarUsuarios = roleLogado == Role.ADMIN || roleLogado == Role.ADMINISTRATIVO;
        if (!podeGerenciarUsuarios) {
            throw new AccessDeniedException("Seu perfil não tem permissão para gerenciar usuários");
        }

        if (roleAlvo == Role.ADMIN && roleLogado != Role.ADMIN) {
            throw new AccessDeniedException("Apenas um usuário ADMIN pode criar ou promover outro ADMIN");
        }
    }

    @Override
    protected UsuarioResponseDTO toResponse(Usuario u) {
        return UsuarioResponseDTO.de(u);
    }

    @Override
    protected Usuario toEntity(UsuarioRequestDTO request, Oficina oficina) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setDocumento(request.documento());
        usuario.setTelefone(request.telefone());
        usuario.setOficina(oficina); // nulo quando role == ADMIN
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRole(request.role());
        return usuario;
    }

    @Override
    protected void applyUpdate(Usuario usuario, UsuarioUpdateRequestDTO request) {
        usuario.setNome(request.nome());
        usuario.setDocumento(request.documento());
        usuario.setTelefone(request.telefone());
        usuario.setUsername(request.username());
        usuario.setRole(request.role());

        // Mantém o vínculo coerente com a role: promover para ADMIN desliga a oficina,
        // rebaixar para ADMINISTRATIVO/MECANICO exige (e aplica) uma oficina.
        usuario.setOficina(request.oficinaId() == null
                ? null
                : oficinaService.buscarPorEntidadeId(request.oficinaId()));

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }
    }

    @Override protected String extractDocumentoCreate(UsuarioRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdCreate(UsuarioRequestDTO r) { return r.oficinaId(); }
    @Override protected String extractDocumentoUpdate(UsuarioUpdateRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdUpdate(UsuarioUpdateRequestDTO r) { return r.oficinaId(); }

    @Override protected RuntimeException notFoundException() { return new UsuarioNotFoundException(); }
    @Override protected RuntimeException alreadyExistsException() { return new UsuarioAlreadyExistsException(); }
}
