ALTER TABLE item_os_peca
    ADD COLUMN oficina_id BIGINT NOT NULL;

ALTER TABLE item_os_peca
    ADD  CONSTRAINT fk_item_os_oficina
           FOREIGN KEY (oficina_id)
               REFERENCES oficina(id);