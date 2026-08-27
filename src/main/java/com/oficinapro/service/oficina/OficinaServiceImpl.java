package com.oficinapro.service.oficina;

import com.oficinapro.dto.oficina.OficinaRequestDTO;
import com.oficinapro.dto.oficina.OficinaResponseDTO;
import com.oficinapro.exception.CnpjAlreadyExistsException;
import com.oficinapro.exception.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OficinaServiceImpl implements OficinaService {

    private final OficinaRepository oficinaRepository;

    public OficinaServiceImpl(OficinaRepository oficinaRepository) {
        this.oficinaRepository = oficinaRepository;
    }

    @Override
    public List<OficinaResponseDTO> listar() {
        return oficinaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OficinaResponseDTO buscarPorId(Long id) {

        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() ->
                        new OficinaNotFoundException(id));

        return toResponse(oficina);
    }

    @Override
    public OficinaResponseDTO criar(OficinaRequestDTO request) {

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
    public OficinaResponseDTO atualizar(
            Long id,
            OficinaRequestDTO request) {

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

    private OficinaResponseDTO toResponse(Oficina oficina) {

        return new OficinaResponseDTO(
                oficina.getId(),
                oficina.getNome(),
                oficina.getCnpj(),
                oficina.getTelefone()
        );
    }
}