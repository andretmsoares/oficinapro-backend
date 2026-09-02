package com.oficinapro.service.registro_pagamento;

import com.oficinapro.dto.registro_pagamento.RegistroPagamentoRequestDTO;
import com.oficinapro.dto.registro_pagamento.RegistroPagamentoResponseDTO;
import com.oficinapro.exception.registro_pagamento.RegistroPagamentoNotFoundException;
import com.oficinapro.model.Pagamento;
import com.oficinapro.model.RegistroPagamento;
import com.oficinapro.repository.RegistroPagamentoRepository;
import com.oficinapro.service.pagamento.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistroPagamentoServiceImpl implements RegistroPagamentoService {

    private final RegistroPagamentoRepository registroPagamentoRepository;
    private final PagamentoService pagamentoService;

    @Override
    @Transactional(readOnly = true)
    public List<RegistroPagamentoResponseDTO> listarPorPagamento(Long pagamentoId) {

        pagamentoService.buscarEntidadePorId(pagamentoId);

        return registroPagamentoRepository.findByPagamentoId(pagamentoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegistroPagamentoResponseDTO buscarPorId(Long id) {
        return toResponse(buscarPorEntidadeId(id));
    }

    private RegistroPagamento buscarPorEntidadeId(Long id) {
        RegistroPagamento registro = registroPagamentoRepository.findById(id)
                .orElseThrow(() -> new RegistroPagamentoNotFoundException(id));

        // Isolamento por oficina: buscarEntidadePorId do pagamento valida o acesso
        // (pagamento -> ordem de serviço -> oficina). Sem isto, um ADMINISTRATIVO
        // conseguiria ler o registro de pagamento de outra oficina por ID.
        pagamentoService.buscarEntidadePorId(registro.getPagamento().getId());

        return registro;
    }

    @Override
    @Transactional
    public RegistroPagamentoResponseDTO criar(RegistroPagamentoRequestDTO request) {
        Pagamento pagamento = pagamentoService.buscarEntidadePorId(request.pagamentoId());

        pagamentoService.atualizarValorPago(pagamento.getId(), request.valor());

        RegistroPagamento registro = new RegistroPagamento();
        registro.setPagamento(pagamento);
        registro.setValor(request.valor());
        registro.setMeioPagamento(request.meioPagamento());
        registro.setData(LocalDateTime.now());

        registro = registroPagamentoRepository.save(registro);

        return toResponse(registro);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        RegistroPagamento registro = buscarPorEntidadeId(id);

        pagamentoService.estornarValorPago(registro.getPagamento().getId(), registro.getValor());

        registroPagamentoRepository.delete(registro);
    }

    private RegistroPagamentoResponseDTO toResponse(RegistroPagamento registro) {
        return new RegistroPagamentoResponseDTO(
                registro.getId(),
                registro.getPagamento().getId(),
                registro.getValor(),
                registro.getMeioPagamento(),
                registro.getData()
        );
    }
}