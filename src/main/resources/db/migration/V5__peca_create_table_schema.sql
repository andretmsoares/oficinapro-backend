CREATE TABLE item_os_peca (
      id BIGSERIAL PRIMARY KEY,
      os_id BIGINT NOT NULL,
      nome VARCHAR(255) NOT NULL,
      quantidade NUMERIC(12, 3) NOT NULL,
      valor_unitario NUMERIC(12, 2) NOT NULL,
      valor_total NUMERIC(12, 2) NOT NULL,

      CONSTRAINT fk_item_os_peca_os
          FOREIGN KEY (os_id)
              REFERENCES ordem_servico(id)
              ON DELETE CASCADE,
);