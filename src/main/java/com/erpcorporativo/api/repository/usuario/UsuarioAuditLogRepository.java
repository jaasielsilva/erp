package com.erpcorporativo.api.repository.usuario;

import com.erpcorporativo.api.domain.usuario.UsuarioAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAuditLogRepository extends JpaRepository<UsuarioAuditLog, Long> {
    Page<UsuarioAuditLog> findByUsuarioId(Long usuarioId, Pageable pageable);
}
