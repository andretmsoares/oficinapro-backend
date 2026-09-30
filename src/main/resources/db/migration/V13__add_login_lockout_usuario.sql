-- Protecao contra forca bruta no login: contador de falhas consecutivas, bloqueio temporario
-- (bloqueado_ate) e bloqueio permanente, que so um ADMIN/GERENTE desfaz.
ALTER TABLE usuario ADD COLUMN falhas_login INT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN bloqueado_ate TIMESTAMP;
ALTER TABLE usuario ADD COLUMN bloqueio_permanente BOOLEAN NOT NULL DEFAULT FALSE;
