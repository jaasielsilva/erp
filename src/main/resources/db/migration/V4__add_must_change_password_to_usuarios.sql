ALTER TABLE usuarios
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER senha_hash;
