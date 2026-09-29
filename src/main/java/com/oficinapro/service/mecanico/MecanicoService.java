package com.oficinapro.service.mecanico;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.model.Mecanico;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MecanicoService
    extends PessoaCrudService<
        MecanicoRequestDTO, MecanicoRequestDTO, MecanicoResponseDTO, Mecanico> {

  /** Busca paginada, na oficina do usuário, por nome, documento ou telefone (parcial). */
  Page<MecanicoResponseDTO> buscar(String termo, Pageable pageable);
}
