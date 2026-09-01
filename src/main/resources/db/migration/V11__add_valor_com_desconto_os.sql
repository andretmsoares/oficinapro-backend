ALTER TABLE ordem_servico
    ADD COLUMN valor_com_desconto NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE ordem_servico
    ADD COLUMN desconto NUMERIC(12, 2) NOT NULL DEFAULT 0;