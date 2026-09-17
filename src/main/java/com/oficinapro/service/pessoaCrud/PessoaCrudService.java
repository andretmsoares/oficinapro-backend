package com.oficinapro.service.pessoaCrud;

import com.oficinapro.model.Pessoa;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PessoaCrudService<C, U, RES, T extends Pessoa> {
  Page<RES> listar(Pageable pageable);

  T buscarPorEntidadeId(Long id);

  RES buscarPorId(Long id);

  List<RES> buscarPorNome(String nome);

  RES buscarPorDocumento(String documento);

  RES criar(C request);

  RES atualizar(Long id, U request);

  void deletar(Long id);

  List<RES> buscarPorNomeAdmin(String nome);

  List<RES> buscarPorDocumentoAdmin(String documento);

  Page<RES> listarTodos(Pageable pageable);
}
