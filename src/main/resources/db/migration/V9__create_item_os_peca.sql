-- Entidade: ItemOsPeca. os_id e opcional: a peca pode existir no catalogo da oficina sem estar
-- vinculada a uma OS (vincular/desvincular em ItemOsPecaService). Apagar a OS apaga os itens
-- vinculados a ela.
CREATE TABLE item_os_peca (
    id BIGSERIAL PRIMARY KEY,
    oficina_id BIGINT NOT NULL,
    os_id BIGINT,
    nome VARCHAR(255) NOT NULL,
    quantidade NUMERIC(12, 3) NOT NULL,
    valor_unitario NUMERIC(12, 0) NOT NULL,
    valor_total NUMERIC(12, 0) NOT NULL,

    CONSTRAINT fk_item_os_oficina FOREIGN KEY (oficina_id) REFERENCES oficina (id),
    CONSTRAINT fk_item_os_peca_os
        FOREIGN KEY (os_id) REFERENCES ordem_servico (id) ON DELETE CASCADE
);

CREATE INDEX idx_item_os_peca_os ON item_os_peca (os_id);
CREATE INDEX idx_item_os_peca_oficina ON item_os_peca (oficina_id);
