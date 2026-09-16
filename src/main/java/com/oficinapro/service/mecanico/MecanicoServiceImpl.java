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

  @Transactional
  @Override
  protected Mecanico toEntity(MecanicoRequestDTO request, Oficina oficina) {
    Mecanico m = new Mecanico();
    m.setNome(request.nome());
    m.setDocumento(request.documento());
    m.setTelefone(request.telefone());
    m.setOficina(oficina);
    m.setSalario(request.salario());
    m.setObs(request.obs());
    return m;
  }

  @Override
  @Transactional
  protected void applyUpdate(Mecanico m, MecanicoRequestDTO request) {
    m.setNome(request.nome());
    m.setDocumento(request.documento());
    m.setTelefone(request.telefone());
    m.setSalario(request.salario());
    m.setObs(request.obs());
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoCreate(MecanicoRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdCreate(MecanicoRequestDTO r) {
    return r.oficinaId();
  }

  @Transactional(readOnly = true)
  @Override
  protected String extractDocumentoUpdate(MecanicoRequestDTO r) {
    return r.documento();
  }

  @Transactional(readOnly = true)
  @Override
  protected Long extractOficinaIdUpdate(MecanicoRequestDTO r) {
    return r.oficinaId();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException notFoundException() {
    return new MecanicoNotFoundException();
  }

  @Transactional(readOnly = true)
  @Override
  protected RuntimeException alreadyExistsException() {
    return new MecanicoAlreadyExistsException();
  }
}
