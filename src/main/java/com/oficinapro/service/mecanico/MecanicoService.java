package com.oficinapro.service.mecanico;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.model.Mecanico;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;


public interface MecanicoService extends PessoaCrudService<MecanicoRequestDTO, MecanicoRequestDTO, MecanicoResponseDTO, Mecanico> {}
