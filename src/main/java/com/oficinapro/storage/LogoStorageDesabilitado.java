package com.oficinapro.storage;

import com.oficinapro.exception.logo.LogoStorageIndisponivelException;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Usado quando nenhum bucket está configurado (dev/testes): o upload é recusado e os PDFs usam a
 * logo padrão do sistema.
 */
@Component
@ConditionalOnExpression("'${oficinapro.storage.gcs.bucket:}'.isEmpty()")
public class LogoStorageDesabilitado implements LogoStorage {

  @Override
  public String salvar(Long oficinaId, byte[] conteudo, String contentType) {
    throw new LogoStorageIndisponivelException();
  }

  @Override
  public Optional<byte[]> ler(String caminho) {
    return Optional.empty();
  }

  @Override
  public void remover(String caminho) {}
}
