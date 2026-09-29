package com.oficinapro.service.ordem_servico;

import com.oficinapro.model.OrdemDeServico;

public interface OrdemDeServicoPdfService {
    byte[] gerar(OrdemDeServico os);
}