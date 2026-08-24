package com.oficinapro.service.impl;

import com.oficinapro.dto.oficina.OficinaRequest;
import com.oficinapro.dto.oficina.OficinaResponse;
import com.oficinapro.exception.CnpjAlreadyExistsException;
import com.oficinapro.exception.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.service.OficinaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OficinaServiceImpl implements OficinaService {

    private final OficinaRepository oficinaRepository;

    public OficinaServiceImpl(OficinaRepository oficinaRepository) {
        this.oficinaRepository = oficinaRepository;
    }

    @Override
    public List<OficinaResponse> listar() {
        return oficinaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OficinaResponse buscarPorId(Long id) {

        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() ->
                        new OficinaNotFoundException(id));

        return toResponse(oficina);
    }

    @Override
    public OficinaResponse criar(OficinaRequest request) {

        if (oficinaRepository.existsByCnpj(request.cnpj())) {
            throw new CnpjAlreadyExistsException(request.cnpj());
        }

        Oficina oficina = new Oficina();

        oficina.setNome(request.nome());
        oficina.setCnpj(request.cnpj());
        oficina.setTelefone(request.telefone());

        Oficina saved = oficinaRepository.save(oficina);

        return toResponse(saved);
    }

    @Override
    public OficinaResponse atualizar(
            Long id,
            OficinaRequest request) {

        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() ->
                        new OficinaNotFoundException(id));

        if (!oficina.getCnpj().equals(request.cnpj())
                && oficinaRepository.existsByCnpj(request.cnpj())) {

            throw new CnpjAlreadyExistsException(request.cnpj());
        }

        oficina.setNome(request.nome());
        oficina.setCnpj(request.cnpj());
        oficina.setTelefone(request.telefone());

        Oficina updated = oficinaRepository.save(oficina);

        return toResponse(updated);
    }

    @Override
    public void deletar(Long id) {

        if (!oficinaRepository.existsById(id)) {
            throw new OficinaNotFoundException(id);
        }

        oficinaRepository.deleteById(id);
    }

    private OficinaResponse toResponse(Oficina oficina) {

        return new OficinaResponse(
                oficina.getId(),
                oficina.getNome(),
                oficina.getCnpj(),
                oficina.getTelefone()
        );
    }
}