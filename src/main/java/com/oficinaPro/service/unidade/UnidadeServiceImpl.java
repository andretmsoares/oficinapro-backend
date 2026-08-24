package com.oficinapro.service.impl;

import com.oficinapro.dto.unidade.UnidadeRequest;
import com.oficinapro.dto.unidade.UnidadeResponse;
import com.oficinapro.exception.EnderecoAlreadyExistsException;
import com.oficinapro.exception.OficinaNotFoundException;
import com.oficinapro.exception.UnidadeNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.model.Unidade;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.repository.UnidadeRepository;
import com.oficinapro.service.UnidadeService;
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
    public List<UnidadeResponse> listar() {
        return unidadeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UnidadeResponse buscarPorId(Long id) {

        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() ->
                        new UnidadeNotFoundException(id));

        return toResponse(unidade);
    }

    @Override
    public List<UnidadeResponse> listarPorOficina(Long oficinaId) {

        if (!oficinaRepository.existsById(oficinaId)) {
            throw new OficinaNotFoundException(oficinaId);
        }

        return unidadeRepository.findByOficinaId(oficinaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UnidadeResponse criar(
            Long oficinaId,
            UnidadeRequest request) {

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
    public UnidadeResponse atualizar(
            Long id,
            UnidadeRequest request) {

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

    private UnidadeResponse toResponse(Unidade unidade) {

        return new UnidadeResponse(
                unidade.getId(),
                unidade.getOficina().getId(),
                unidade.getNome(),
                unidade.getEndereco(),
                unidade.getTelefone()
        );
    }
}