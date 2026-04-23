CREATE UNIQUE INDEX uk_usuarios_single_super_admin
    ON usuarios ((CASE WHEN role = 'SUPER_ADMIN' THEN 1 ELSE NULL END));
