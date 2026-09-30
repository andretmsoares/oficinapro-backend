package com.oficinapro.storage;

import java.util.Optional;

/**
 * Armazenamento das logos das oficinas. O caminho devolvido por {@link #salvar} é gerado pelo
 * servidor (nunca vem do cliente) e é o que fica em {@code oficina.logo_path}.
 */
public interface LogoStorage {

  /** Grava a logo e devolve o caminho do objeto. */
  String salvar(Long oficinaId, byte[] conteudo, String contentType);

  /** Bytes da logo, ou vazio se o objeto não existir. */
  Optional<byte[]> ler(String caminho);

  /** Remove o objeto; falha silenciosa (não pode impedir a troca da logo). */
  void remover(String caminho);
}
