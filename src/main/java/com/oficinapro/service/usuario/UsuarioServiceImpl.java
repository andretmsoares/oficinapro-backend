package com.oficinapro.service.usuario;

import com.oficinapro.dto.usuario.UsuarioRequestDTO;
import com.oficinapro.dto.usuario.UsuarioResponseDTO;
import com.oficinapro.dto.usuario.UsuarioUpdateRequestDTO;
import com.oficinapro.exception.UsernameAlreadyExistsException;
import com.oficinapro.exception.UsuarioAlreadyExistsException;
import com.oficinapro.exception.UsuarioNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UsuarioRepository;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl
        extends AbstractPessoaServiceImpl<Usuario, UsuarioRequestDTO, UsuarioUpdateRequestDTO, UsuarioResponseDTO>
        implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              OficinaServiceImpl oficinaService,
                              PessoaService pessoaService,
                              PasswordEncoder passwordEncoder) {
        super(usuarioRepository, oficinaService, pessoaService);
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void validateBeforeCreate(UsuarioRequestDTO request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Override
    protected void validateBeforeUpdate(Long id, UsuarioUpdateRequestDTO request) {
        if (usuarioRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Override
    protected UsuarioResponseDTO toResponse(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(), u.getNome(), u.getTelefone(), u.getDocumento(),
                u.getOficina().getId(), u.getUsername(), u.getRole());
    }

    @Override
    protected Usuario toEntity(UsuarioRequestDTO request, Oficina oficina) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setDocumento(request.documento());
        usuario.setTelefone(request.telefone());
        usuario.setOficina(oficina);
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