CREATE INDEX idx_unidade_oficina
    ON unidade(oficina_id);

CREATE INDEX idx_pessoa_oficina
    ON pessoa(oficina_id);

CREATE INDEX idx_veiculo_oficina
    ON veiculo(oficina_id);

CREATE INDEX idx_fornecedor_oficina
    ON fornecedor(oficina_id);

CREATE INDEX idx_nota_compra_fornecedor
    ON nota_compra(fornecedor_id);

CREATE INDEX idx_item_nota_compra_nota
    ON item_nota_compra(nota_compra_id);

CREATE INDEX idx_os_oficina
    ON ordem_servico(oficina_id);

CREATE INDEX idx_os_cliente
    ON ordem_servico(cliente_id);

CREATE INDEX idx_os_veiculo
    ON ordem_servico(veiculo_id);

CREATE INDEX idx_os_unidade
    ON ordem_servico(unidade_id);

CREATE INDEX idx_pagamento_os
    ON pagamento(os_id);

CREATE INDEX idx_registro_pagamento
    ON registro_pagamento(pagamento_id);

CREATE INDEX idx_item_os_peca_os
    ON item_os_peca(os_id);

CREATE INDEX idx_item_os_peca_nota
    ON item_os_peca(item_nota_compra_id);