package com.oficinapro.service.cliente;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.model.Cliente;
import com.oficinapro.service.pessoaCrud.PessoaCrudService;

public interface ClienteService
        extends PessoaCrudService<ClienteRequestDTO, ClienteRequestDTO, ClienteResponseDTO, Cliente> {}