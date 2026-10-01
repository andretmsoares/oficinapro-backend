package com.oficinapro.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class R2LogoStorageTest {

  private static final byte[] CONTEUDO = {1, 2, 3};

  private S3Client s3;
  private R2LogoStorage storage;

  @BeforeEach
  void setUp() {
    s3 = mock(S3Client.class);
    storage = new R2LogoStorage("bucket-teste", s3);
  }

  @Test
  @DisplayName("salvar: grava no bucket com caminho gerado pelo servidor e extensão do tipo")
  void salvarDeveGravarNoBucket() {
    String caminho = storage.salvar(7L, CONTEUDO, "image/png");

    assertThat(caminho).startsWith("logos/oficina-7-").endsWith(".png");
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().bucket()).isEqualTo("bucket-teste");
    assertThat(captor.getValue().key()).isEqualTo(caminho);
    assertThat(captor.getValue().contentType()).isEqualTo("image/png");
  }

  @Test
  @DisplayName("salvar: tipos que não são PNG usam extensão .jpg")
  void salvarJpegUsaExtensaoJpg() {
    assertThat(storage.salvar(1L, CONTEUDO, "image/jpeg")).endsWith(".jpg");
  }

  @Test
  @DisplayName("ler: logo recém-salva vem do cache, sem nova ida ao bucket")
  void lerUsaCacheAposSalvar() {
    String caminho = storage.salvar(1L, CONTEUDO, "image/png");

    assertThat(storage.ler(caminho)).contains(CONTEUDO);
    verify(s3, never()).getObjectAsBytes(any(GetObjectRequest.class));
  }

  @Test
  @DisplayName("ler: busca no bucket quando não está em cache e guarda o resultado")
  void lerBuscaNoBucket() {
    when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), CONTEUDO));

    assertThat(storage.ler("logos/x.png")).contains(CONTEUDO);
    assertThat(storage.ler("logos/x.png")).contains(CONTEUDO);
    verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
  }

  @Test
  @DisplayName("ler: objeto inexistente resulta em vazio")
  void lerInexistenteRetornaVazio() {
    when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().build());

    assertThat(storage.ler("logos/nao-existe.png")).isEmpty();
  }

  @Test
  @DisplayName("remover: apaga o objeto do bucket")
  void removerApagaObjeto() {
    storage.remover("logos/x.png");

    ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3).deleteObject(captor.capture());
    assertThat(captor.getValue().key()).isEqualTo("logos/x.png");
  }

  @Test
  @DisplayName("remover: falha no bucket não pode impedir a troca da logo")
  void removerEngoleFalhas() {
    doThrow(new RuntimeException("falha")).when(s3).deleteObject(any(DeleteObjectRequest.class));

    assertThatCode(() -> storage.remover("logos/x.png")).doesNotThrowAnyException();
  }
}
