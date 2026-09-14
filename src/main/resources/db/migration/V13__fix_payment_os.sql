DROP INDEX IF EXISTS idx_pagamento_os_id;

ALTER TABLE pagamento
    ADD CONSTRAINT uk_pagamento_os
        UNIQUE (os_id);