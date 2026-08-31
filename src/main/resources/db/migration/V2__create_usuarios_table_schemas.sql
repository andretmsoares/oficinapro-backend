CREATE TABLE pessoa (
    id BIGSERIAL PRIMARY KEY,
    oficina_id BIGINT,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    documento VARCHAR(14),

    CONSTRAINT uq_pessoa__oficina_doc
        UNIQUE (oficina_id, documento),

    CONSTRAINT fk_pessoa_oficina
        FOREIGN KEY (oficina_id)
            REFERENCES oficina(id)
            ON DELETE CASCADE
);

CREATE TABLE cliente (
     pessoa_id BIGINT PRIMARY KEY,

     CONSTRAINT fk_cliente_pessoa
         FOREIGN KEY (pessoa_id)
             REFERENCES pessoa(id)
             ON DELETE CASCADE
);

CREATE TABLE usuario (
     pessoa_id BIGINT PRIMARY KEY,
     username VARCHAR(100) NOT NULL UNIQUE,
     password VARCHAR(255) NOT NULL,
     role VARCHAR(50) NOT NULL,

     CONSTRAINT chk_usuario_role
         CHECK (role IN (
                         'ADMIN',
                         'ADMINISTRATIVO',
                         'MECANICO'
             )),

     CONSTRAINT fk_usuario_pessoa
         FOREIGN KEY (pessoa_id)
             REFERENCES pessoa(id)
             ON DELETE CASCADE
);

CREATE TABLE mecanico (
      pessoa_id BIGINT PRIMARY KEY,
      salario NUMERIC(12, 2),
      obs TEXT,

      CONSTRAINT fk_mecanico_pessoa
          FOREIGN KEY (pessoa_id)
              REFERENCES pessoa(id)
              ON DELETE CASCADE
);