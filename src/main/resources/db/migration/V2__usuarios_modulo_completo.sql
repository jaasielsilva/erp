ALTER TABLE usuarios
    ADD COLUMN created_by VARCHAR(160) NULL AFTER ativo,
    ADD COLUMN updated_by VARCHAR(160) NULL AFTER created_by;

UPDATE usuarios
SET created_by = COALESCE(created_by, 'system'),
    updated_by = COALESCE(updated_by, 'system');

CREATE TABLE usuario_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(160) NOT NULL,
    details VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_audit_logs_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_usuario_audit_logs_usuario_id ON usuario_audit_logs (usuario_id);
CREATE INDEX idx_usuario_audit_logs_created_at ON usuario_audit_logs (created_at);

CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_password_reset_tokens_usuario_id ON password_reset_tokens (usuario_id);
