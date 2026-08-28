package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.exception.EnderecoAlreadyExistsException;
import com.oficinapro.exception.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UnidadeRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.role.Role;
import com.oficinapro.service.oficina.OficinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadeServiceImpl implements UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final OficinaService oficinaService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    private Long oficinaObrigatoriaDoLogado(Usuario logado) {
        Long oficinaId = logado.getOficina() != null ? logado.getOficina().getId() : null;
        if (oficinaId == null) {
            throw new AccessDeniedException("Usuário não está vinculado a nenhuma oficina");
        }
        return oficinaId;
    }

    /** Usado quando o oficinaId vem explícito na requisição (criar, listarPorOficina). */
    private void validarAcessoOficina(Long oficinaId) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        if (!oficinaDoLogado.equals(oficinaId)) {
            throw new AccessDeniedException("Você só pode acessar dados da sua própria oficina");
        }
    }

    /** Usado quando se acessa um registro específico (buscarPorId, atualizar, deletar). */
    private void validarAcessoAoRegistro(Unidade unidade) {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();
        if (logado.getRole() == Role.ADMIN) {
            return;
        }
        Long oficinaDoLogado = oficinaObrigatoriaDoLogado(logado);
        Long oficinaDaUnidade = unidade.getOficina() != null ? unidade.getOficina().getId() : null;
        if (!oficinaDoLogado.equals(oficinaDaUnidade)) {
            throw new UnidadeNotFoundException(unidade.getId());
        }
    }

    @Override
    public List<UnidadeResponseDTO> listar() {
        Usuario logado = authenticatedUserProvider.getUsuarioAutenticado();

        List<Unidade> unidades = logado.getRole() == Role.ADMIN
                ? unidadeRepository.findAll()
                : unidadeRepository.findByOficinaId(oficinaObrigatoriaDoLogado(logado));

        return unidades.stream().map(this::toResponse).toList();
    }

    @Override
    public UnidadeResponseDTO buscarPorId(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        validarAcessoAoRegistro(unidade);

        return toResponse(unidade);
    }

    @Override
    public List<UnidadeResponseDTO> listarPorOficina(Long oficinaId) {
        validarAcessoOficina(oficinaId);

        oficinaService.buscarPorEntidadeId(oficinaId);

        return unidadeRepository.findByOficinaId(oficinaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UnidadeResponseDTO criar(Long oficinaId, UnidadeRequestDTO request) {
        validarAcessoOficina(oficinaId);

        Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);

        if (unidadeRepository.existsByEndereco(request.endereco())) {
            throw new EnderecoAlreadyExistsException(request.endereco());
        }

        Unidade unidade = new Unidade();
        unidade.setOficina(oficina);
        unidade.setNome(request.nome());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        Unidade saved = unidadeRepository.save(unidade);

        return toResponse(saved);
    }

    @Override
    public UnidadeResponseDTO atualizar(Long id, UnidadeRequestDTO request) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        validarAcessoAoRegistro(unidade);

        if (!unidade.getEndereco().equals(request.endereco())
                && unidadeRepository.existsByEndereco(request.endereco())) {
            throw new EnderecoAlreadyExistsException(request.endereco());
        }

        unidade.setNome(request.nome());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        Unidade updated = unidadeRepository.save(unidade);

        return toResponse(updated);
    }

    @Override
    public void deletar(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        validarAcessoAoRegistro(unidade);

        unidadeRepository.delete(unidade);
    }

    private UnidadeResponseDTO toResponse(Unidade unidade) {
        return new UnidadeResponseDTO(
                unidade.getId(),
                unidade.getOficina().getId(),
                unidade.getNome(),
                unidade.getEndereco(),
                unidade.getTelefone()
        );
    }
}