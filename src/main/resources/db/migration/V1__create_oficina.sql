-- Entidade: Oficina (tenant raiz do SaaS).
CREATE TABLE oficina (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    telefone VARCHAR(20),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_oficina_cnpj UNIQUE (cnpj)
);
