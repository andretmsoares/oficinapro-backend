CREATE TABLE ordem_servico (
       id BIGSERIAL PRIMARY KEY,
       oficina_id BIGINT NOT NULL,
       cliente_id BIGINT,
       veiculo_id BIGINT NOT NULL,
       unidade_id BIGINT NOT NULL,
       mecanico_id BIGINT,
       data_abertura TIMESTAMP NOT NULL,
       data_fechamento TIMESTAMP,
       status VARCHAR(30) NOT NULL,
       obs TEXT,
       valor_total NUMERIC(12, 2) NOT NULL DEFAULT 0,

       CONSTRAINT fk_os_oficina
           FOREIGN KEY (oficina_id)
               REFERENCES oficina(id),

       CONSTRAINT fk_os_cliente
           FOREIGN KEY (cliente_id)
               REFERENCES cliente(pessoa_id),

       CONSTRAINT fk_os_veiculo
           FOREIGN KEY (veiculo_id)
               REFERENCES veiculo(id),

       CONSTRAINT fk_os_unidade
           FOREIGN KEY (unidade_id)
               REFERENCES unidade(id)

       CONSTRAINT fk_os_mecanico
            FOREIGN KEY (mecanico_id)
               REFERENCES mecanico(pessoa_id)
);