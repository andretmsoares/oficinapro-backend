package com.oficinapro.service.estatisticas;

import com.oficinapro.dto.estatisticas.ContagemPorOficinaDTO;
import com.oficinapro.dto.estatisticas.EstatisticasOficinaResponseDTO;
import com.oficinapro.dto.estatisticas.EstatisticasSistemaResponseDTO;
import com.oficinapro.enums.Role;
import com.oficinapro.exception.oficina.OficinaNotFoundException;
import com.oficinapro.model.Oficina;
import com.oficinapro.repository.*;
import com.oficinapro.security.OficinaAccessValidator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstatisticasServiceImpl implements EstatisticasService {

  private final OficinaRepository oficinaRepository;
  private final ClienteRepository clienteRepository;
  private final MecanicoRepository mecanicoRepository;
  private final VeiculoRepository veiculoRepository;
  private final OrdemDeServicoRepository ordemDeServicoRepository;
  private final OficinaAccessValidator oficinaAccessValidator;

  @Override
  @Transactional(readOnly = true)
  public EstatisticasSistemaResponseDTO resumoDoSistema() {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    // Quatro queries agregadas, e não uma por oficina: o custo não cresce com o
    // número de tenants.
    Map<Long, Long> clientes = indexar(clienteRepository.contarPorOficina());
    Map<Long, Long> mecanicos = indexar(mecanicoRepository.contarPorOficina());
    Map<Long, Long> veiculos = indexar(veiculoRepository.contarPorOficina());
    Map<Long, Long> ordens = indexar(ordemDeServicoRepository.contarPorOficina());

    List<EstatisticasOficinaResponseDTO> porOficina =
        oficinaRepository.findAll(Sort.by("nome")).stream()
            .map(
                oficina ->
                    new EstatisticasOficinaResponseDTO(
                        oficina.getId(),
                        oficina.getNome(),
                        oficina.getCnpj(),
                        oficina.getTelefone(),
                        oficina.getAtivo(),
                        clientes.getOrDefault(oficina.getId(), 0L),
                        mecanicos.getOrDefault(oficina.getId(), 0L),
                        veiculos.getOrDefault(oficina.getId(), 0L),
                        ordens.getOrDefault(oficina.getId(), 0L)))
            .toList();

    return new EstatisticasSistemaResponseDTO(
        porOficina.size(),
        somar(porOficina, EstatisticasOficinaResponseDTO::clientes),
        somar(porOficina, EstatisticasOficinaResponseDTO::mecanicos),
        somar(porOficina, EstatisticasOficinaResponseDTO::veiculos),
        somar(porOficina, EstatisticasOficinaResponseDTO::ordensDeServico),
        porOficina);
  }

  @Override
  @Transactional(readOnly = true)
  public EstatisticasOficinaResponseDTO resumoDaOficina(Long oficinaId) {
    oficinaAccessValidator.validarRole(Role.ADMIN);

    Oficina oficina =
        oficinaRepository
            .findById(oficinaId)
            .orElseThrow(() -> new OficinaNotFoundException(oficinaId));

    return new EstatisticasOficinaResponseDTO(
        oficina.getId(),
        oficina.getNome(),
        oficina.getCnpj(),
        oficina.getTelefone(),
        oficina.getAtivo(),
        clienteRepository.countByOficinaId(oficinaId),
        mecanicoRepository.countByOficinaId(oficinaId),
        veiculoRepository.countByOficinaId(oficinaId),
        ordemDeServicoRepository.countByOficinaId(oficinaId));
  }

  private Map<Long, Long> indexar(List<ContagemPorOficinaDTO> contagens) {
    return contagens.stream()
        .collect(
            Collectors.toMap(ContagemPorOficinaDTO::oficinaId, ContagemPorOficinaDTO::quantidade));
  }

  private long somar(
      List<EstatisticasOficinaResponseDTO> lista,
      Function<EstatisticasOficinaResponseDTO, Long> campo) {
    return lista.stream().mapToLong(campo::apply).sum();
  }
}
