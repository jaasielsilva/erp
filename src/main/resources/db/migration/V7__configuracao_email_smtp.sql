ALTER TABLE configuracao_email_empresa
    ADD COLUMN smtp_host VARCHAR(160) NULL AFTER endereco_remetente,
    ADD COLUMN smtp_port INT NULL AFTER smtp_host,
    ADD COLUMN smtp_auth BOOLEAN NOT NULL DEFAULT TRUE AFTER smtp_port,
    ADD COLUMN smtp_starttls BOOLEAN NOT NULL DEFAULT TRUE AFTER smtp_auth,
    ADD COLUMN smtp_username VARCHAR(160) NULL AFTER smtp_starttls,
    ADD COLUMN smtp_password_ciphertext TEXT NULL AFTER smtp_username;

UPDATE configuracao_email_empresa
SET
    provedor = 'SMTP_GMAIL',
    smtp_host = COALESCE(smtp_host, 'smtp.gmail.com'),
    smtp_port = COALESCE(smtp_port, 587),
    smtp_auth = COALESCE(smtp_auth, TRUE),
    smtp_starttls = COALESCE(smtp_starttls, TRUE),
    smtp_username = COALESCE(smtp_username, endereco_remetente);
