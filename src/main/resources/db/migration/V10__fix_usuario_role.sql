ALTER TABLE usuario DROP CONSTRAINT chk_usuario_role;

ALTER TABLE usuario ADD CONSTRAINT chk_usuario_role
    CHECK (role IN ('ADMIN', 'ADMINISTRATIVO', 'MECANICO'));