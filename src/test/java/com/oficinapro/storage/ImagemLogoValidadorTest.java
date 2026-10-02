package com.oficinapro.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oficinapro.exception.logo.LogoInvalidaException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImagemLogoValidadorTest {

  private static byte[] imagem(String formato, int largura, int altura, int tipo) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(largura, altura, tipo), formato, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  @DisplayName("PNG e JPEG pequenos são aceitos")
  void aceitaImagensPequenas() {
    assertThatCode(
            () ->
                ImagemLogoValidador.validarDimensoes(
                    imagem("png", 300, 100, BufferedImage.TYPE_INT_RGB)))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                ImagemLogoValidador.validarDimensoes(
                    imagem("jpg", 300, 100, BufferedImage.TYPE_INT_RGB)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("imagem no limite exato de 2000x2000 é aceita")
  void aceitaNoLimite() {
    assertThatCode(
            () ->
                ImagemLogoValidador.validarDimensoes(
                    imagem("png", 2000, 2000, BufferedImage.TYPE_BYTE_BINARY)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("lado acima de 2000 px é recusado")
  void recusaLadoGrande() {
    byte[] larga = imagem("png", 2001, 10, BufferedImage.TYPE_BYTE_BINARY);

    assertThatThrownBy(() -> ImagemLogoValidador.validarDimensoes(larga))
        .isInstanceOf(LogoInvalidaException.class)
        .hasMessageContaining("2000x2000");
  }

  @Test
  @DisplayName("pixel flood (16 milhões de pixels) é recusado sem decodificar a imagem")
  void recusaPixelFlood() {
    byte[] flood = imagem("png", 4000, 4000, BufferedImage.TYPE_BYTE_BINARY);

    assertThatThrownBy(() -> ImagemLogoValidador.validarDimensoes(flood))
        .isInstanceOf(LogoInvalidaException.class);
  }

  @Test
  @DisplayName("bytes que não são uma imagem são recusados")
  void recusaLixo() {
    assertThatThrownBy(() -> ImagemLogoValidador.validarDimensoes("<svg/>".getBytes()))
        .isInstanceOf(LogoInvalidaException.class);
    assertThatThrownBy(() -> ImagemLogoValidador.validarDimensoes(new byte[0]))
        .isInstanceOf(LogoInvalidaException.class);
  }
}
