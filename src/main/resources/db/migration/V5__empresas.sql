CREATE TABLE empresas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome_fantasia VARCHAR(200) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO empresas (id, nome_fantasia, ativo) VALUES (1, 'Tenant padrão', TRUE)
    ON DUPLICATE KEY UPDATE nome_fantasia = VALUES(nome_fantasia);
