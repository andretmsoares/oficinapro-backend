package com.oficinapro.service.pessoa;

import com.oficinapro.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PessoaServiceImpl implements PessoaService {

    private final PessoaRepository pessoaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOficinaIdAndDocumento(Long oficinaId, String documento) {
        if (documento == null || documento.isBlank()) {
            return false;
        }
        return pessoaRepository.existsByOficinaIdAndDocumento(oficinaId, documento);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOficinaIdAndDocumentoExcluindoId(Long oficinaId, String documento, Long id) {
        if (documento == null || documento.isBlank()) {
            return false;
        }
        return pessoaRepository.existsByOficinaIdAndDocumentoAndIdNot(oficinaId, documento, id);
    }
}