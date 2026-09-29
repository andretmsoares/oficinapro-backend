-- Entidade: Mecanico (subtipo de Pessoa).
CREATE TABLE mecanico (
    pessoa_id BIGINT PRIMARY KEY,
    salario NUMERIC(12, 2),
    obs TEXT,

    CONSTRAINT fk_mecanico_pessoa
        FOREIGN KEY (pessoa_id) REFERENCES pessoa (id) ON DELETE CASCADE
);
