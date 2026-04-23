CREATE TABLE configuracao_email_empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    provedor VARCHAR(40) NOT NULL DEFAULT 'GMAIL_OAUTH2',
    endereco_remetente VARCHAR(160) NULL,
    oauth_client_id TEXT NULL,
    oauth_client_secret_ciphertext TEXT NULL,
    oauth_refresh_token_ciphertext TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_configuracao_email_empresa_empresa UNIQUE (empresa_id),
    CONSTRAINT fk_configuracao_email_empresa_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
);

CREATE INDEX idx_configuracao_email_empresa_empresa_id ON configuracao_email_empresa (empresa_id);
