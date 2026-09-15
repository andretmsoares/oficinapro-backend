package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.exception.unidade.EnderecoAlreadyExistsException;
import com.oficinapro.exception.unidade.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.model.Usuario;
import com.oficinapro.repository.UnidadeRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.enums.Role;
import com.oficinapro.service.oficina.OficinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadeServiceImpl implements UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final OficinaService oficinaService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OficinaAccessValidator oficinaAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public List<UnidadeResponseDTO> listar() {
        Usuario logado =
                authenticatedUserProvider.getUsuarioAutenticado();

        List<Unidade> unidades = logado.getRole() == Role.ADMIN
                ? unidadeRepository.findAll()
                : unidadeRepository.findByOficinaId(
                oficinaAccessValidator.getOficinaIdUsuarioLogado()
        );

        return unidades.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadeResponseDTO buscarPorId(Long id) {
        return toResponse(this.buscarPorEntidadeId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Unidade buscarPorEntidadeId(Long id) {
        Unidade unidade = unidadeRepository
                .findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        oficinaAccessValidator.validarAcessoAoRegistro(
                unidade.getOficina() != null
                        ? unidade.getOficina().getId()
                        : null,
                new UnidadeNotFoundException(id)
        );

        return unidade;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadeResponseDTO> listarPorOficina(Long oficinaId) {
        oficinaAccessValidator.validarAcessoOficina(oficinaId);

        oficinaService.buscarPorEntidadeId(oficinaId);

        return unidadeRepository.findByOficinaId(oficinaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UnidadeResponseDTO criar(Long oficinaId, UnidadeRequestDTO request) {
        oficinaAccessValidator.validarAcessoOficina(oficinaId);

        Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);

        if (unidadeRepository.existsByOficinaIdAndEndereco(oficinaId, request.endereco())) {
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
    @Transactional
    public UnidadeResponseDTO atualizar(Long id, UnidadeRequestDTO request) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        oficinaAccessValidator.validarAcessoAoRegistro(
                unidade.getOficina() != null
                        ? unidade.getOficina().getId()
                        : null,
                new UnidadeNotFoundException(id)
        );

        Long oficinaDaUnidade = unidade.getOficina() != null
                ? unidade.getOficina().getId()
                : null;

        if (oficinaDaUnidade != null
                && unidadeRepository.existsByOficinaIdAndEnderecoAndIdNot(
                        oficinaDaUnidade, request.endereco(), id)) {
            throw new EnderecoAlreadyExistsException(request.endereco());
        }

        unidade.setNome(request.nome());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        Unidade updated = unidadeRepository.save(unidade);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new UnidadeNotFoundException(id));

        oficinaAccessValidator.validarAcessoAoRegistro(
                unidade.getOficina() != null
                        ? unidade.getOficina().getId()
                        : null,
                new UnidadeNotFoundException(id)
        );

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