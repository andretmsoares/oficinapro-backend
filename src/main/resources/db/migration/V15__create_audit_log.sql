-- Trilha de auditoria de eventos de seguranca e financeiros (login, usuarios, estornos...).
-- Sem FK para usuario/oficina de proposito: o registro precisa sobreviver a exclusao do ator
-- ou do alvo. Nao guarda dado pessoal alem de ids; o detalhe e texto curto gerado pelo sistema.
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    ocorrido_em TIMESTAMP NOT NULL,
    acao VARCHAR(50) NOT NULL,
    ator_id BIGINT,
    ator_role VARCHAR(50),
    oficina_id BIGINT,
    alvo_tipo VARCHAR(50),
    alvo_id BIGINT,
    detalhe VARCHAR(500),
    ip VARCHAR(45)
);

CREATE INDEX idx_audit_log_ocorrido_em ON audit_log (ocorrido_em);
CREATE INDEX idx_audit_log_oficina ON audit_log (oficina_id, ocorrido_em);
CREATE INDEX idx_audit_log_ator ON audit_log (ator_id, ocorrido_em);
