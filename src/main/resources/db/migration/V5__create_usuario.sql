-- Entidade: Usuario (subtipo de Pessoa). username e credencial de login, por isso e unico
-- globalmente. chk_usuario_role deve refletir com.oficinapro.enums.Role.
CREATE TABLE usuario (
    pessoa_id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,

    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT chk_usuario_role CHECK (role IN ('ADMIN', 'GERENTE', 'MECANICO')),
    CONSTRAINT fk_usuario_pessoa
        FOREIGN KEY (pessoa_id) REFERENCES pessoa (id) ON DELETE CASCADE
);
