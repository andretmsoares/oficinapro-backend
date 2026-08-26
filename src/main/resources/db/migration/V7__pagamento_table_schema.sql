CREATE TABLE pagamento (
   id BIGSERIAL PRIMARY KEY,
   os_id BIGINT NOT NULL,
   valor_pago NUMERIC(12, 2) NOT NULL,
   obs TEXT,
   desconto NUMERIC(12, 2) NOT NULL DEFAULT 0,
   data_pagamento_total TIMESTAMP,

   CONSTRAINT fk_pagamento_os
       FOREIGN KEY (os_id)
           REFERENCES ordem_servico(id)
           ON DELETE CASCADE
);

CREATE TABLE registro_pagamento (
    id BIGSERIAL PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    meio_pagamento VARCHAR(30) NOT NULL,
    data TIMESTAMP NOT NULL,

    CONSTRAINT fk_registro_pagamento
        FOREIGN KEY (pagamento_id)
            REFERENCES pagamento(id)
            ON DELETE CASCADE
);
