package com.oficinapro.repository;

import com.oficinapro.model.Cliente;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends PessoaCrudRepository<Cliente> {}