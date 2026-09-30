-- Caminho do objeto (no bucket) da logo da oficina, impressa nos PDFs. Nulo = usa a logo do sistema.
ALTER TABLE oficina ADD COLUMN logo_path VARCHAR(255);
