package com.oficinapro.repository;

import com.oficinapro.model.Pessoa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PessoaCrudRepository<T extends Pessoa> extends JpaRepository<T, Long> {
  Page<T> findByOficinaId(Long oficinaId, Pageable pageable);

  List<T> findByOficinaIdAndNome(Long oficinaId, String nome);

  List<T> findByNome( String nome);

  List<T> findByDocumento( String documento);

  Optional<T> findByOficinaIdAndDocumento(Long oficinaId, String documento);
}
