package com.erpcorporativo.api.repository.usuario;

import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.domain.usuario.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByEmailIgnoreCaseAndAtivoTrue(String email);

    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);

    long countByRoleAndAtivo(UserRole role, boolean ativo);

    long countByAtivo(boolean ativo);

    long countByAtivoAndEmpresaId(boolean ativo, Long empresaId);

    long countByRole(UserRole role);

    long countByRoleAndEmpresaId(UserRole role, Long empresaId);

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndAtivo(Long empresaId, boolean ativo);
}
