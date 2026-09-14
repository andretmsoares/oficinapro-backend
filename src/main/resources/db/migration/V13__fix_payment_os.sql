ALTER TABLE pagamento
    ADD CONSTRAINT uk_pagamento_os
        UNIQUE (os_id);