-- A V2 criou chk_usuario_role permitindo ('ADMIN', 'ADMINISTRATIVO', 'MECANICO').
-- A V14 renomeou o cargo para GERENTE via UPDATE, mas nao ajustou a constraint.
--
-- Consequencia em producao:
--   * se existirem usuarios ADMINISTRATIVO, a propria V14 falha e a aplicacao nao sobe;
--   * se nao existirem, a V14 passa (0 linhas) mas a constraint continua proibindo
--     'GERENTE' -- ou seja, nenhum gerente pode ser cadastrado. O erro chega ao cliente
--     como 409 (DataIntegrityViolationException), sem indicar a causa real.
--
-- O problema nao aparece nos testes porque o perfil de teste usa
-- spring.flyway.enabled=false + ddl-auto=create-drop: o schema vem das entidades JPA,
-- que nao declaram esse CHECK.
--
-- Aqui a constraint e recriada alinhada ao enum com.oficinapro.enums.Role.
-- O UPDATE e repetido de forma idempotente para cobrir bases em que a V14 tenha sido
-- aplicada antes de alguem inserir dados legados.

ALTER TABLE usuario
    DROP CONSTRAINT IF EXISTS chk_usuario_role;

UPDATE usuario SET role = 'GERENTE' WHERE role = 'ADMINISTRATIVO';

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_role
        CHECK (role IN (
                        'ADMIN',
                        'GERENTE',
                        'MECANICO'
            ));
