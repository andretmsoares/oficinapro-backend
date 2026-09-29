-- Entidade: OrdemDeServico.
-- As FKs para cliente, veiculo, unidade e mecanico ficam sem ON DELETE (NO ACTION):
-- nao se apaga cadastro que possui historico de OS.
-- cliente_id e mecanico_id sao opcionais; a OS pode nascer sem eles.
-- valor_com_desconto = valor_total - desconto (ver OrdemDeServicoValorRecalculator).
-- chk_os_status deve refletir com.oficinapro.enums.StatusOrdemDeServico.
CREATE TABLE ordem_servico (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    oficina_id BIGINT NOT NULL,
    cliente_id BIGINT,
    veiculo_id BIGINT NOT NULL,
    unidade_id BIGINT NOT NULL,
    mecanico_id BIGINT,
    data_abertura TIMESTAMP NOT NULL,
    data_fechamento TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    obs TEXT,
    valor_total NUMERIC(12, 0) NOT NULL DEFAULT 0,
    desconto NUMERIC(12, 0) NOT NULL DEFAULT 0,
    valor_com_desconto NUMERIC(12, 0) NOT NULL DEFAULT 0,

    CONSTRAINT chk_os_status CHECK (status IN (
        'ABERTA',
        'DIAGNOSTICO',
        'AGUARDANDO_APROVACAO',
        'AGUARDANDO_PECAS',
        'EM_EXECUCAO',
        'FINALIZADA',
        'ENTREGUE',
        'FECHADA',
        'CANCELADA'
    )),
    CONSTRAINT chk_os_valores_nao_negativos CHECK (
        valor_total >= 0 AND desconto >= 0 AND valor_com_desconto >= 0
    ),
    CONSTRAINT fk_os_oficina FOREIGN KEY (oficina_id) REFERENCES oficina (id),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (pessoa_id),
    CONSTRAINT fk_os_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo (id),
    CONSTRAINT fk_os_unidade FOREIGN KEY (unidade_id) REFERENCES unidade (id),
    CONSTRAINT fk_os_mecanico FOREIGN KEY (mecanico_id) REFERENCES mecanico (pessoa_id)
);

CREATE INDEX idx_os_oficina ON ordem_servico (oficina_id);
CREATE INDEX idx_os_cliente ON ordem_servico (cliente_id);
CREATE INDEX idx_os_veiculo ON ordem_servico (veiculo_id);
CREATE INDEX idx_os_unidade ON ordem_servico (unidade_id);
CREATE INDEX idx_os_mecanico ON ordem_servico (mecanico_id);
