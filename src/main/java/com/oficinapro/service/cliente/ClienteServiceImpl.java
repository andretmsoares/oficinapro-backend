package com.oficinapro.service.cliente;

import com.oficinapro.dto.cliente.ClienteRequestDTO;
import com.oficinapro.dto.cliente.ClienteResponseDTO;
import com.oficinapro.exception.cliente.ClienteAlreadyExistsException;
import com.oficinapro.exception.cliente.ClienteNotFoundException;
import com.oficinapro.model.Cliente;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.ClienteRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl
    extends AbstractPessoaServiceImpl<
        Cliente, ClienteRequestDTO, ClienteRequestDTO, ClienteResponseDTO>
    implements ClienteService {

  public ClienteServiceImpl(
      ClienteRepository repository,
      OficinaServiceImpl oficinaService,
      PessoaService pessoaService,
      OficinaAccessValidator oficinaAccessValidator) {
    super(repository, oficinaService, pessoaService, oficinaAccessValidator);
  }

  @Override
  protected ClienteResponseDTO toResponse(Cliente cliente) {
    return new ClienteResponseDTO(
        cliente.getId(),
        cliente.getNome(),
        cliente.getTelefone(),
        cliente.getDocumento(),
        cliente.getOficina().getId());
  }

  @Override
  @Transactional
  protected Cliente toEntity(ClienteRequestDTO request, Oficina oficina) {
    Cliente cliente = new Cliente();
    cliente.setNome(request.nome());
    cliente.setDocumento(request.documento());
    cliente.setTelefone(request.telefone());
    cliente.setOficina(oficina);
    return cliente;
  }

  @Override
  @Transactional
  protected void applyUpdate(Cliente cliente, ClienteRequestDTO request) {
    cliente.setNome(request.nome());
    cliente.setDocumento(request.documento());
    cliente.setTelefone(request.telefone());
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoCreate(ClienteRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdCreate(ClienteRequestDTO r) {
    return oficinaAccessValidator.getOficinaIdUsuarioLogado();
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoUpdate(ClienteRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdUpdate(ClienteRequestDTO r) {
    return oficinaAccessValidator.getOficinaIdUsuarioLogado();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException notFoundException() {
    return new ClienteNotFoundException();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException alreadyExistsException() {
    return new ClienteAlreadyExistsException();
  }
}
