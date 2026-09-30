-- Entidade: Cliente (subtipo de Pessoa).
CREATE TABLE cliente (
    pessoa_id BIGINT PRIMARY KEY,

    CONSTRAINT fk_cliente_pessoa
        FOREIGN KEY (pessoa_id) REFERENCES pessoa (id) ON DELETE CASCADE
);
