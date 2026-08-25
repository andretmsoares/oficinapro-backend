CREATE TABLE oficina (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    telefone VARCHAR(20),

    CONSTRAINT uq_oficina_cnpj
        UNIQUE (cnpj)
);

CREATE TABLE unidade (
     id BIGSERIAL PRIMARY KEY,
     oficina_id BIGINT NOT NULL,
     nome VARCHAR(255) NOT NULL,
     endereco VARCHAR(255) NOT NULL UNIQUE,
     telefone VARCHAR(20),

    CONSTRAINT fk_unidade_oficina FOREIGN KEY (oficina_id) REFERENCES oficina(id) ON DELETE CASCADE
)
