package com.oficinapro.service.oficina;

import com.oficinapro.exception.logo.LogoInvalidaException;
import com.oficinapro.exception.logo.LogoNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.OficinaRepository;
import com.oficinapro.security.OficinaAccessValidator;
import com.oficinapro.storage.LogoStorage;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OficinaLogoServiceImpl implements OficinaLogoService {

  static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;

  private final OficinaRepository oficinaRepository;
  private final OficinaService oficinaService;
  private final OficinaAccessValidator oficinaAccessValidator;
  private final LogoStorage logoStorage;

  public OficinaLogoServiceImpl(
      OficinaRepository oficinaRepository,
      OficinaService oficinaService,
      OficinaAccessValidator oficinaAccessValidator,
      LogoStorage logoStorage) {
    this.oficinaRepository = oficinaRepository;
    this.oficinaService = oficinaService;
    this.oficinaAccessValidator = oficinaAccessValidator;
    this.logoStorage = logoStorage;
  }

  @Override
  @Transactional
  public void atualizar(Long oficinaId, MultipartFile arquivo) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    byte[] conteudo = lerEValidar(arquivo);
    String contentType = detectarTipo(conteudo);

    Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);
    String antigo = oficina.getLogoPath();

    oficina.setLogoPath(logoStorage.salvar(oficinaId, conteudo, contentType));
    oficinaRepository.save(oficina);

    if (antigo != null) logoStorage.remover(antigo);
  }

  @Override
  @Transactional
  public void remover(Long oficinaId) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    Oficina oficina = oficinaService.buscarPorEntidadeId(oficinaId);
    String antigo = oficina.getLogoPath();
    if (antigo == null) return;

    oficina.setLogoPath(null);
    oficinaRepository.save(oficina);
    logoStorage.remover(antigo);
  }

  @Override
  @Transactional(readOnly = true)
  public Logo buscar(Long oficinaId) {
    oficinaAccessValidator.validarAcessoOficina(oficinaId);

    String caminho = oficinaService.buscarPorEntidadeId(oficinaId).getLogoPath();
    if (caminho == null) throw new LogoNotFoundException();

    byte[] conteudo = logoStorage.ler(caminho).orElseThrow(LogoNotFoundException::new);
    return new Logo(conteudo, detectarTipo(conteudo));
  }

  private byte[] lerEValidar(MultipartFile arquivo) {
    if (arquivo == null || arquivo.isEmpty()) {
      throw new LogoInvalidaException("Envie um arquivo de imagem.");
    }
    if (arquivo.getSize() > TAMANHO_MAXIMO) {
      throw new LogoInvalidaException("A logo deve ter no máximo 2 MB.");
    }
    try {
      return arquivo.getBytes();
    } catch (IOException e) {
      throw new LogoInvalidaException("Não foi possível ler o arquivo enviado.");
    }
  }

  /**
   * O tipo é decidido pelos bytes do arquivo (assinatura), não pelo content-type nem pela extensão
   * informados pelo cliente, que são triviais de forjar. Só PNG e JPEG; SVG fica de fora de
   * propósito (pode carregar scripts e o OpenPDF não o renderiza).
   */
  static String detectarTipo(byte[] b) {
    if (b.length > 8
        && (b[0] & 0xFF) == 0x89
        && b[1] == 'P'
        && b[2] == 'N'
        && b[3] == 'G'
        && b[4] == 0x0D
        && b[5] == 0x0A
        && b[6] == 0x1A
        && b[7] == 0x0A) {
      return "image/png";
    }
    if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
      return "image/jpeg";
    }
    throw new LogoInvalidaException("Formato inválido. Envie uma imagem PNG ou JPEG.");
  }
}
