-- A V15 removeu a unicidade GLOBAL de unidade.endereco, mas nao colocou nada no lugar.
-- Resultado: a mesma oficina passou a poder cadastrar duas unidades no mesmo endereco.
--
-- Aqui a regra e restabelecida no escopo correto: endereco e unico POR OFICINA.
-- Oficinas diferentes podem operar no mesmo endereco (predio compartilhado, troca de
-- ponto comercial), e a verificacao deixa de revelar dados de outros tenants.
--
-- Nao ha limpeza de duplicatas nesta migration de proposito: ate a V15 o endereco era
-- unico globalmente, logo nenhuma base existente pode conter duas unidades com o mesmo
-- endereco na mesma oficina. Se esta migration falhar por violacao de unicidade, ha
-- duplicatas criadas na janela entre V15 e V18 e elas devem ser resolvidas manualmente
-- antes do deploy -- apagar unidade automaticamente destruiria historico de OS.
ALTER TABLE unidade
    ADD CONSTRAINT uq_unidade_oficina_endereco
        UNIQUE (oficina_id, endereco);
