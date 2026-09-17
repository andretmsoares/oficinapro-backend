-- A V10 já tentava criar esta coluna como NOT NULL sem DEFAULT. Em banco que rodou a V10
-- (instalação nova), "ADD COLUMN status" aqui falhava com "column already exists". Em
-- banco antigo que pulou a V10 via baseline-on-migrate, um "DROP COLUMN status" sem
-- IF EXISTS falharia com "column does not exist". Os dois IF EXISTS/IF NOT EXISTS abaixo
-- tornam esta migration idempotente nos dois cenários, sem depender de qual deles
-- aconteceu antes.
ALTER TABLE pagamento
    DROP COLUMN IF EXISTS status;

ALTER TABLE pagamento
    ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE pagamento
SET status = 'PAGAMENTO_PENDENTE'
WHERE status IS NULL;

ALTER TABLE pagamento
    ALTER COLUMN status SET NOT NULL;