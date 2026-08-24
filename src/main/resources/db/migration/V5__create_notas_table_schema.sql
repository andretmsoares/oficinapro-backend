CREATE TABLE nota_compra (
     id BIGSERIAL PRIMARY KEY,
     tipo_doc VARCHAR(50) NOT NULL,
     fornecedor_id BIGINT NOT NULL,
     numero_doc VARCHAR(50),
     data DATE NOT NULL,
     valor_total NUMERIC(12, 2) NOT NULL,
     arquivo TEXT,

     CONSTRAINT fk_nota_compra_fornecedor
         FOREIGN KEY (fornecedor_id)
             REFERENCES fornecedor(id)
);

CREATE TABLE item_nota_compra (
      id BIGSERIAL PRIMARY KEY,
      nota_compra_id BIGINT NOT NULL,
      nome VARCHAR(255) NOT NULL,
      quantidade NUMERIC(12, 3) NOT NULL,
      valor_unitario NUMERIC(12, 2) NOT NULL,
      valor_total NUMERIC(12, 2) NOT NULL,

      CONSTRAINT fk_item_nota_compra
          FOREIGN KEY (nota_compra_id)
              REFERENCES nota_compra(id)
              ON DELETE CASCADE
);
