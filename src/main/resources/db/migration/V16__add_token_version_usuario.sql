-- Revogacao de JWT: o token carrega a versao (claim "tv"); trocar a senha ou fazer logout
-- incrementa esta coluna e invalida todos os tokens emitidos antes.
ALTER TABLE usuario ADD COLUMN token_version INT NOT NULL DEFAULT 0;
