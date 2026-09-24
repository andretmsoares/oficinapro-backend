package com.oficinapro.service.mecanico;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.MecanicoRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MecanicoServiceImpl
    extends AbstractPessoaServiceImpl<
        Mecanico, MecanicoRequestDTO, MecanicoRequestDTO, MecanicoResponseDTO>
    implements MecanicoService {

  public MecanicoServiceImpl(
      MecanicoRepository repository,
      OficinaServiceImpl oficinaService,
      PessoaService pessoaService,
      OficinaAccessValidator oficinaAccessValidator) {

    super(repository, oficinaService, pessoaService, oficinaAccessValidator);
  }

  @Override
  protected MecanicoResponseDTO toResponse(Mecanico m) {
    return new MecanicoResponseDTO(
        m.getId(),
        m.getNome(),
        m.getTelefone(),
        m.getDocumento(),
        m.getOficina().getId(),
        m.getSalario(),
        m.getObs());
  }

  @Override
  @Transactional
  protected Mecanico toEntity(MecanicoRequestDTO request, Oficina oficina) {
    Mecanico mecanico = new Mecanico();

    mecanico.setNome(request.nome().toUpperCase());
    mecanico.setDocumento(request.documento());
    mecanico.setTelefone(request.telefone());
    mecanico.setOficina(oficina);
    mecanico.setSalario(request.salario());
    mecanico.setObs(request.obs());

    return mecanico;
  }

  @Override
  @Transactional
  protected void applyUpdate(Mecanico mecanico, MecanicoRequestDTO request) {

    mecanico.setNome(request.nome().toUpperCase());
    mecanico.setDocumento(request.documento());
    mecanico.setTelefone(request.telefone());
    mecanico.setSalario(request.salario());
    mecanico.setObs(request.obs());
  }

  @Override
  @Transactional(readOnly = true)
  protected String extractDocumentoCreate(MecanicoRequestDTO request) {
    return request.documento();
  }

  @Override
  @Transactional(readOnly = true)
  protected Long extractOficinaIdCreate(MecanicoRequestDTO request) {
    return oficinaAccessValidator.getOficinaIdUsuarioLogado();
  }

  @Override
  @Transactional(readOnly = true)
  protected String extractDocumentoUpdate(MecanicoRequestDTO request) {
    return request.documento();
  }

  @Override
  @Transactional(readOnly = true)
  protected Long extractOficinaIdUpdate(MecanicoRequestDTO request) {
    return oficinaAccessValidator.getOficinaIdUsuarioLogado();
  }

  @Override
  @Transactional(readOnly = true)
  protected RuntimeException notFoundException() {
    return new MecanicoNotFoundException();
  }

  @Override
  @Transactional(readOnly = true)
  protected RuntimeException alreadyExistsException() {
    return new MecanicoAlreadyExistsException();
  }
}
