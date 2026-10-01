-- Entidade: Unidade. O endereco e unico por oficina (nao globalmente), para que uma
-- oficina nao descubra unidades de outro tenant por conflito de unicidade.
-- O indice de uq_unidade_oficina_endereco ja atende buscas por oficina_id.
CREATE TABLE unidade (
    id BIGSERIAL PRIMARY KEY,
    oficina_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),

    CONSTRAINT uq_unidade_oficina_endereco UNIQUE (oficina_id, endereco),
    CONSTRAINT fk_unidade_oficina
        FOREIGN KEY (oficina_id) REFERENCES oficina (id) ON DELETE CASCADE
);
