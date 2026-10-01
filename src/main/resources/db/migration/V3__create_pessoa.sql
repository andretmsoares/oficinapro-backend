-- Entidade: Pessoa (raiz da heranca JOINED de Cliente, Usuario e Mecanico).
-- oficina_id e nulo apenas para o ADMIN do SaaS. No PostgreSQL NULL nao colide com NULL em
-- indice unico, entao esses registros nao disputam unicidade de documento.
-- O indice de uq_pessoa__oficina_doc ja atende buscas por oficina_id.
CREATE TABLE pessoa (
    id BIGSERIAL PRIMARY KEY,
    oficina_id BIGINT,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    documento VARCHAR(14),

    CONSTRAINT uq_pessoa__oficina_doc UNIQUE (oficina_id, documento),
    CONSTRAINT fk_pessoa_oficina
        FOREIGN KEY (oficina_id) REFERENCES oficina (id) ON DELETE CASCADE
);
