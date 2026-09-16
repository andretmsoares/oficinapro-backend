package com.oficinapro.service.estatisticas;

import com.oficinapro.dto.estatisticas.EstatisticasOficinaResponseDTO;
import com.oficinapro.dto.estatisticas.EstatisticasSistemaResponseDTO;

public interface EstatisticasService {

  public EstatisticasSistemaResponseDTO resumoDoSistema();

  public EstatisticasOficinaResponseDTO resumoDaOficina(Long oficinaId);
}
