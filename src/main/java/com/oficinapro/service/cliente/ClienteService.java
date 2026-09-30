package com.oficinapro.service.cliente;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.model.Cliente;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService
    extends PessoaCrudService<ClienteRequestDTO, ClienteRequestDTO, ClienteResponseDTO, Cliente> {
  Integer count();

  /** Busca paginada, na oficina do usuário, por nome, documento ou telefone (parcial). */
  Page<ClienteResponseDTO> buscar(String termo, Pageable pageable);
}
