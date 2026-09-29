-- Entidade: Pagamento. Relacao 1:1 com a OS, garantida por uk_pagamento_os (o indice unico ja
-- atende buscas por os_id). O desconto pertence a OS, nao ao pagamento.
-- chk_pagamento_status deve refletir com.oficinapro.enums.StatusPagamento.
CREATE TABLE pagamento (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    os_id BIGINT NOT NULL,
    valor_pago NUMERIC(12, 2) NOT NULL DEFAULT 0,
    obs TEXT,
    data_pagamento_total TIMESTAMP,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT uk_pagamento_os UNIQUE (os_id),
    CONSTRAINT chk_pagamento_valor_pago_nao_negativo CHECK (valor_pago >= 0),
    CONSTRAINT chk_pagamento_status CHECK (status IN (
        'PAGAMENTO_PENDENTE',
        'PAGO_PARCIALMENTE',
        'PAGA'
    )),
    CONSTRAINT fk_pagamento_os
        FOREIGN KEY (os_id) REFERENCES ordem_servico (id) ON DELETE CASCADE
);
