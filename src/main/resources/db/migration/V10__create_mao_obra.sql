-- Entidade: MaoObra. Apagar a OS apaga a mao de obra lancada nela.
CREATE TABLE mao_obra (
    id BIGSERIAL PRIMARY KEY,
    os_id BIGINT NOT NULL,
    valor NUMERIC(12, 0) NOT NULL,
    descricao TEXT NOT NULL,

    CONSTRAINT fk_mao_obra_os
        FOREIGN KEY (os_id) REFERENCES ordem_servico (id) ON DELETE CASCADE
);

CREATE INDEX idx_mao_obra_os ON mao_obra (os_id);
