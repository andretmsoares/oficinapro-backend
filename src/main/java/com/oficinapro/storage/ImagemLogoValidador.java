package com.oficinapro.storage;

import com.oficinapro.exception.logo.LogoInvalidaException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Confere as dimensões de uma imagem lendo só o cabeçalho, SEM decodificar os pixels.
 *
 * <p>O tamanho em bytes não limita o tamanho decodificado: um PNG de poucos KB pode declarar
 * dezenas de milhares de pixels por lado (a compressão de dados repetidos chega a 1000:1) e, ao ser
 * decodificado para entrar no PDF, ocupar gigabytes de memória e derrubar a JVM compartilhada por
 * todas as oficinas. Por isso a checagem acontece antes de qualquer decodificação.
 */
public final class ImagemLogoValidador {

  /** Maior lado aceito, em pixels. Uma logo de cabeçalho de PDF não precisa de mais. */
  public static final int LADO_MAXIMO = 2000;

  /** Limite de pixels totais (4 MP ≈ 16 MB decodificados em ARGB). */
  public static final long PIXELS_MAXIMOS = 4_000_000L;

  private ImagemLogoValidador() {}

  /**
   * @throws LogoInvalidaException se a imagem não puder ser lida ou exceder os limites
   */
  public static void validarDimensoes(byte[] conteudo) {
    try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(conteudo))) {
      Iterator<ImageReader> leitores = in == null ? null : ImageIO.getImageReaders(in);

      if (leitores == null || !leitores.hasNext()) {
        throw new LogoInvalidaException("Não foi possível ler a imagem enviada.");
      }

      ImageReader leitor = leitores.next();
      try {
        leitor.setInput(in, true, true);
        long largura = leitor.getWidth(0);
        long altura = leitor.getHeight(0);

        if (largura <= 0
            || altura <= 0
            || largura > LADO_MAXIMO
            || altura > LADO_MAXIMO
            || largura * altura > PIXELS_MAXIMOS) {
          throw new LogoInvalidaException(
              "A imagem deve ter no máximo %dx%d pixels.".formatted(LADO_MAXIMO, LADO_MAXIMO));
        }
      } finally {
        leitor.dispose();
      }
    } catch (IOException | RuntimeException e) {
      if (e instanceof LogoInvalidaException invalida) {
        throw invalida;
      }
      throw new LogoInvalidaException("Não foi possível ler a imagem enviada.");
    }
  }
}
