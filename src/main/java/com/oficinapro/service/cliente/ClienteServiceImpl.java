package com.oficinapro.service.cliente;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.ClienteRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.stereotype.Service;

@Service
public class ClienteServiceImpl
        extends AbstractPessoaServiceImpl<Cliente, ClienteRequestDTO, ClienteRequestDTO, ClienteResponseDTO>
        implements ClienteService {

    public ClienteServiceImpl(ClienteRepository repository,
                              OficinaServiceImpl oficinaService,
                              PessoaService pessoaService,
                              AuthenticatedUserProvider authenticatedUserProvider) {
        super(repository, oficinaService, pessoaService, authenticatedUserProvider);
    }

    @Override
    protected ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                cliente.getDocumento(), cliente.getOficina().getId());
    }

    @Override
    protected Cliente toEntity(ClienteRequestDTO request, Oficina oficina) {
        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setDocumento(request.documento());
        cliente.setTelefone(request.telefone());
        cliente.setOficina(oficina);
        return cliente;
    }

    @Override
    protected void applyUpdate(Cliente cliente, ClienteRequestDTO request) {
        cliente.setNome(request.nome());
        cliente.setDocumento(request.documento());
        cliente.setTelefone(request.telefone());
    }

    @Override protected String extractDocumentoCreate(ClienteRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdCreate(ClienteRequestDTO r) { return r.oficinaId(); }
    @Override protected String extractDocumentoUpdate(ClienteRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdUpdate(ClienteRequestDTO r) { return r.oficinaId(); }

    @Override protected RuntimeException notFoundException() { return new ClienteNotFoundException(); }
    @Override protected RuntimeException alreadyExistsException() { return new ClienteAlreadyExistsException(); }
}