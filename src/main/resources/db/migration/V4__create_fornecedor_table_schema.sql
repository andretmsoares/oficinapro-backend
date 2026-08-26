CREATE TABLE fornecedor (
    id BIGSERIAL PRIMARY KEY,
    oficina_id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    cnpj VARCHAR(14),
    endereco VARCHAR(255),

    CONSTRAINT fk_fornecedor_oficina
        FOREIGN KEY (oficina_id)
            REFERENCES oficina(id)
            ON DELETE CASCADE
);