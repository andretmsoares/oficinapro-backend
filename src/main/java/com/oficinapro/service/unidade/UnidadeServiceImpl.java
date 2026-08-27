package com.oficinapro.service.unidade;

import com.oficinapro.dto.unidade.UnidadeRequestDTO;
import com.oficinapro.dto.unidade.UnidadeResponseDTO;
import com.oficinapro.exception.EnderecoAlreadyExistsException;
import com.oficinapro.exception.OficinaNotFoundException;
import com.oficinapro.exception.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnidadeServiceImpl implements UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final OficinaRepository oficinaRepository;

    public UnidadeServiceImpl(
            UnidadeRepository unidadeRepository,
            OficinaRepository oficinaRepository) {

        this.unidadeRepository = unidadeRepository;
        this.oficinaRepository = oficinaRepository;
    }

    @Override
    public List<UnidadeResponseDTO> listar() {
        return unidadeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UnidadeResponseDTO buscarPorId(Long id) {

        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() ->
                        new UnidadeNotFoundException(id));

        return toResponse(unidade);
    }

    @Override
    public List<UnidadeResponseDTO> listarPorOficina(Long oficinaId) {

        if (!oficinaRepository.existsById(oficinaId)) {
            throw new OficinaNotFoundException(oficinaId);
        }

        return unidadeRepository.findByOficinaId(oficinaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UnidadeResponseDTO criar(
            Long oficinaId,
            UnidadeRequestDTO request) {

        Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() ->
                        new OficinaNotFoundException(oficinaId));

        if (unidadeRepository.existsByEndereco(request.endereco())) {
            throw new EnderecoAlreadyExistsException(
                    request.endereco()
            );
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
    public UnidadeResponseDTO atualizar(
            Long id,
            UnidadeRequestDTO request) {

        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() ->
                        new UnidadeNotFoundException(id));

        if (!unidade.getEndereco().equals(request.endereco())
                && unidadeRepository.existsByEndereco(request.endereco())) {

            throw new EnderecoAlreadyExistsException(
                    request.endereco()
            );
        }

        unidade.setNome(request.nome());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        Unidade updated = unidadeRepository.save(unidade);

        return toResponse(updated);
    }

    @Override
    public void deletar(Long id) {

        if (!unidadeRepository.existsById(id)) {
            throw new UnidadeNotFoundException(id);
        }

        unidadeRepository.deleteById(id);
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