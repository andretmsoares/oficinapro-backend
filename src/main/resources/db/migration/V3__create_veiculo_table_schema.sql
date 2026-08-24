CREATE TABLE veiculo (
     id BIGSERIAL PRIMARY KEY,
     oficina_id BIGINT NOT NULL,
     modelo VARCHAR(100) NOT NULL,
     ano INTEGER,
     marca VARCHAR(100),
     placa VARCHAR(10) NOT NULL,

     CONSTRAINT uq_veiculo_placa_oficina
         UNIQUE (oficina_id,placa),

     CONSTRAINT fk_veiculo_oficina
         FOREIGN KEY (oficina_id)
             REFERENCES oficina(id)
             ON DELETE CASCADE
);