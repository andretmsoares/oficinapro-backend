-- Entidade: RegistroPagamento (historico de recebimentos de um pagamento).
-- chk_registro_pagamento_meio deve refletir com.oficinapro.enums.MeioPagamento.
CREATE TABLE registro_pagamento (
    id BIGSERIAL PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    valor NUMERIC(12, 0) NOT NULL,
    meio_pagamento VARCHAR(30) NOT NULL,
    data TIMESTAMP NOT NULL,

    CONSTRAINT chk_registro_pagamento_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_registro_pagamento_meio CHECK (meio_pagamento IN (
        'PIX',
        'DINHEIRO',
        'CARTAO_CREDITO',
        'CARTAO_DEBITO',
        'CHEQUE'
    )),
    CONSTRAINT fk_registro_pagamento
        FOREIGN KEY (pagamento_id) REFERENCES pagamento (id) ON DELETE CASCADE
);

CREATE INDEX idx_registro_pagamento ON registro_pagamento (pagamento_id);
