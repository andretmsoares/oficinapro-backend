package com.oficinapro.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Google Cloud Storage. Só é criado quando {@code oficinapro.storage.gcs.bucket} está definido. As
 * credenciais vêm do ambiente (Application Default Credentials): a service account anexada ao
 * serviço no GCP, ou {@code GOOGLE_APPLICATION_CREDENTIALS} apontando para o JSON da chave.
 *
 * <p>Cada upload gera um caminho novo (com UUID), então o conteúdo de um caminho nunca muda e o
 * cache em memória não precisa ser invalidado.
 */
@Component
@ConditionalOnExpression(
    "!'${oficinapro.storage.gcs.bucket:}'.isEmpty() && '${oficinapro.storage.r2.bucket:}'.isEmpty()")
public class GcsLogoStorage implements LogoStorage {

  private static final Logger log = LoggerFactory.getLogger(GcsLogoStorage.class);
  private static final int CACHE_MAX = 50;

  private final Storage storage;
  private final String bucket;

  private final Map<String, byte[]> cache =
      Collections.synchronizedMap(
          new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
              return size() > CACHE_MAX;
            }
          });

  public GcsLogoStorage(@Value("${oficinapro.storage.gcs.bucket}") String bucket) {
    this.bucket = bucket;
    this.storage = StorageOptions.getDefaultInstance().getService();
  }

  @Override
  public String salvar(Long oficinaId, byte[] conteudo, String contentType) {
    String caminho = "logos/oficina-" + oficinaId + "-" + UUID.randomUUID() + extensao(contentType);
    BlobInfo info =
        BlobInfo.newBuilder(BlobId.of(bucket, caminho)).setContentType(contentType).build();
    storage.create(info, conteudo);
    cache.put(caminho, conteudo);
    return caminho;
  }

  @Override
  public Optional<byte[]> ler(String caminho) {
    byte[] emCache = cache.get(caminho);
    if (emCache != null) return Optional.of(emCache);

    try {
      byte[] bytes = storage.readAllBytes(BlobId.of(bucket, caminho));
      if (bytes != null) cache.put(caminho, bytes);
      return Optional.ofNullable(bytes);
    } catch (StorageException e) {
      if (e.getCode() == 404) return Optional.empty();
      throw e;
    }
  }

  @Override
  public void remover(String caminho) {
    cache.remove(caminho);
    try {
      storage.delete(BlobId.of(bucket, caminho));
    } catch (Exception e) {
      log.warn("Não foi possível remover a logo antiga {}: {}", caminho, e.getMessage());
    }
  }

  private String extensao(String contentType) {
    return "image/png".equals(contentType) ? ".png" : ".jpg";
  }
}
