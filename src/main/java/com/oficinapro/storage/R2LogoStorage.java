package com.oficinapro.storage;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cloudflare R2 (API compatível com S3). Só é criado quando {@code oficinapro.storage.r2.bucket}
 * está definido. As credenciais são um par access key / secret gerado no painel do R2.
 *
 * <p>Cada upload gera um caminho novo (com UUID), então o conteúdo de um caminho nunca muda e o
 * cache em memória não precisa ser invalidado.
 */
@Component
@ConditionalOnExpression("!'${oficinapro.storage.r2.bucket:}'.isEmpty()")
public class R2LogoStorage implements LogoStorage {

  private static final Logger log = LoggerFactory.getLogger(R2LogoStorage.class);
  private static final int CACHE_MAX = 50;

  private final S3Client s3;
  private final String bucket;

  private final Map<String, byte[]> cache =
      Collections.synchronizedMap(
          new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
              return size() > CACHE_MAX;
            }
          });

  @Autowired
  public R2LogoStorage(
      @Value("${oficinapro.storage.r2.bucket}") String bucket,
      @Value("${oficinapro.storage.r2.endpoint}") String endpoint,
      @Value("${oficinapro.storage.r2.access-key}") String accessKey,
      @Value("${oficinapro.storage.r2.secret-key}") String secretKey) {
    this(
        bucket,
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build());
  }

  R2LogoStorage(String bucket, S3Client s3) {
    this.bucket = bucket;
    this.s3 = s3;
  }

  @Override
  public String salvar(Long oficinaId, byte[] conteudo, String contentType) {
    String caminho = "logos/oficina-" + oficinaId + "-" + UUID.randomUUID() + extensao(contentType);
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(caminho).contentType(contentType).build(),
        RequestBody.fromBytes(conteudo));
    cache.put(caminho, conteudo);
    return caminho;
  }

  @Override
  public Optional<byte[]> ler(String caminho) {
    byte[] emCache = cache.get(caminho);
    if (emCache != null) return Optional.of(emCache);

    try {
      byte[] bytes =
          s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(caminho).build())
              .asByteArray();
      cache.put(caminho, bytes);
      return Optional.of(bytes);
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public void remover(String caminho) {
    cache.remove(caminho);
    try {
      s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(caminho).build());
    } catch (Exception e) {
      log.warn("Não foi possível remover a logo antiga {}: {}", caminho, e.getMessage());
    }
  }

  private String extensao(String contentType) {
    return "image/png".equals(contentType) ? ".png" : ".jpg";
  }
}
