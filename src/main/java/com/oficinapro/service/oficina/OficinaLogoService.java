package com.oficinapro.service.oficina;

import org.springframework.web.multipart.MultipartFile;

public interface OficinaLogoService {

  /** Logo já validada: bytes e content-type detectado pelos bytes (não pelo cliente). */
  record Logo(byte[] conteudo, String contentType) {}

  void atualizar(Long oficinaId, MultipartFile arquivo);

  void remover(Long oficinaId);

  Logo buscar(Long oficinaId);
}
