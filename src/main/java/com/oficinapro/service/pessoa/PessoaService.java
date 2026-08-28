package com.oficinapro.service.pessoa;

public interface PessoaService {

    boolean existsByOficinaIdAndDocumento(Long oficinaId, String documento);

    boolean existsByOficinaIdAndDocumentoExcluindoId(Long oficinaId, String documento, Long id);
}