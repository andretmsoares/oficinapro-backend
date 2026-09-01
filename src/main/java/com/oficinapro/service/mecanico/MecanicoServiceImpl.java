package com.oficinapro.service.mecanico;

import com.oficinapro.dto.mecanico.MecanicoRequestDTO;
import com.oficinapro.dto.mecanico.MecanicoResponseDTO;
import com.oficinapro.exception.mecanico.MecanicoAlreadyExistsException;
import com.oficinapro.exception.mecanico.MecanicoNotFoundException;
import com.oficinapro.model.Mecanico;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.MecanicoRepository;
import com.oficinapro.security.AuthenticatedUserProvider;
import com.oficinapro.service.oficina.OficinaServiceImpl;
import com.oficinapro.service.pessoaCrud.AbstractPessoaServiceImpl;
import com.oficinapro.service.pessoa.PessoaService;
import org.springframework.stereotype.Service;

@Service
public class MecanicoServiceImpl
        extends AbstractPessoaServiceImpl<Mecanico, MecanicoRequestDTO, MecanicoRequestDTO, MecanicoResponseDTO>
        implements MecanicoService {

    public MecanicoServiceImpl(MecanicoRepository repository,
                               OficinaServiceImpl oficinaService,
                               PessoaService pessoaService,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        super(repository, oficinaService, pessoaService, authenticatedUserProvider);
    }

    @Override
    protected MecanicoResponseDTO toResponse(Mecanico m) {
        return new MecanicoResponseDTO(
                m.getId(), m.getNome(), m.getTelefone(), m.getDocumento(),
                m.getOficina().getId(), m.getSalario(), m.getObs());
    }

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
    protected void applyUpdate(Mecanico m, MecanicoRequestDTO request) {
        m.setNome(request.nome());
        m.setDocumento(request.documento());
        m.setTelefone(request.telefone());
        m.setSalario(request.salario());
        m.setObs(request.obs());
    }

    @Override protected String extractDocumentoCreate(MecanicoRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdCreate(MecanicoRequestDTO r) { return r.oficinaId(); }
    @Override protected String extractDocumentoUpdate(MecanicoRequestDTO r) { return r.documento(); }
    @Override protected Long extractOficinaIdUpdate(MecanicoRequestDTO r) { return r.oficinaId(); }

    @Override protected RuntimeException notFoundException() { return new MecanicoNotFoundException(); }
    @Override protected RuntimeException alreadyExistsException() { return new MecanicoAlreadyExistsException(); }
}
