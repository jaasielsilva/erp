package com.erpcorporativo.api.service.usuario;

import com.erpcorporativo.api.domain.usuario.PasswordResetToken;
import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditAction;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditLog;
import com.erpcorporativo.api.dto.usuario.UsuarioAuditResponse;
import com.erpcorporativo.api.dto.usuario.UsuarioCreateRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioResetPasswordResponse;
import com.erpcorporativo.api.dto.usuario.UsuarioResponse;
import com.erpcorporativo.api.repository.usuario.PasswordResetTokenRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioAuditLogRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import com.erpcorporativo.api.service.mail.event.PasswordResetMailMessage;
import com.erpcorporativo.api.service.mail.event.PasswordResetMailPublisher;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@$!%*?";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuditLogRepository usuarioAuditLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailPublisher passwordResetMailPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> search(String q, UserRole role, Boolean ativo, Pageable pageable, String actor) {
        Usuario actorUsuario = findActor(actor);
        Specification<Usuario> spec = (root, query, cb) -> cb.conjunction();

        if (actorUsuario.getRole() != UserRole.SUPER_ADMIN) {
            Long empresaId = requireTenantId(actorUsuario);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
        }

        if (StringUtils.hasText(q)) {
            String normalized = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("nome")), normalized),
                            cb.like(cb.lower(root.get("email")), normalized)
                    )
            );
        }

        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }

        if (ativo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
        }

        return usuarioRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse getById(Long id, String actor) {
        Usuario actorUsuario = findActor(actor);
        return toResponse(findEntityForActor(id, actorUsuario));
    }

    @Transactional
    public UsuarioResponse create(UsuarioCreateRequest request, String actor) {
        Usuario actorUsuario = findActor(actor);
        ensureActorCanManageUsers(actorUsuario);
        ensureReservedSuperAdminRoleForCreation(request.role());
        ensureActorCanAssignRole(actorUsuario, request.role());
        ensureSingleSuperAdminOnCreate(request.role());

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Ja existe usuario com esse e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(normalizedEmail);
        usuario.setRole(request.role());
        usuario.setAtivo(request.ativo());
        usuario.setEmpresaId(resolveTenantForManagedUser(actorUsuario, request.role()));
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setMustChangePassword(false);
        usuario.setCreatedBy(actor);
        usuario.setUpdatedBy(actor);

        Usuario saved = usuarioRepository.save(usuario);
        audit(saved.getId(), UsuarioAuditAction.CREATE, actor, "Usuario criado com perfil " + saved.getRole().name());
        return toResponse(saved);
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest request, String actor) {
        Usuario actorUsuario = findActor(actor);
        ensureActorCanManageUsers(actorUsuario);

        Usuario usuario = findEntityForActor(id, actorUsuario);
        ensureActorCanManageTarget(actorUsuario, usuario);
        ensureActorCanAssignRole(actorUsuario, request.role());
        ensureSingleSuperAdminOnUpdate(usuario, request.role());
        ensureSingleSuperAdminNotInactivated(usuario, request.ativo());

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (!usuario.getEmail().equalsIgnoreCase(normalizedEmail) && usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Ja existe usuario com esse e-mail.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(normalizedEmail);
        usuario.setRole(request.role());
        usuario.setAtivo(request.ativo());
        usuario.setUpdatedBy(actor);

        Usuario saved = usuarioRepository.save(usuario);
        audit(saved.getId(), UsuarioAuditAction.UPDATE, actor, "Dados principais atualizados.");
        return toResponse(saved);
    }

    @Transactional
    public UsuarioResponse toggleStatus(Long id, String actor) {
        Usuario actorUsuario = findActor(actor);
        ensureActorCanManageUsers(actorUsuario);

        Usuario usuario = findEntityForActor(id, actorUsuario);
        ensureActorCanManageTarget(actorUsuario, usuario);
        if (actorUsuario.getId().equals(usuario.getId()) && usuario.isAtivo()) {
            throw new AccessDeniedException("Nao e permitido inativar a propria conta.");
        }
        ensureSingleSuperAdminNotInactivated(usuario, !usuario.isAtivo());

        usuario.setAtivo(!usuario.isAtivo());
        usuario.setUpdatedBy(actor);
        Usuario saved = usuarioRepository.save(usuario);
        audit(saved.getId(), UsuarioAuditAction.TOGGLE_STATUS, actor, "Status alterado para " + (saved.isAtivo() ? "ATIVO" : "INATIVO"));
        return toResponse(saved);
    }

    @Transactional
    public UsuarioResetPasswordResponse resetPasswordByAdmin(Long id, String actor) {
        Usuario actorUsuario = findActor(actor);
        ensureActorCanManageUsers(actorUsuario);

        Usuario usuario = findEntityForActor(id, actorUsuario);
        ensureActorCanManageTarget(actorUsuario, usuario);

        String temporaryPassword = generatePassword(12);
        usuario.setSenhaHash(passwordEncoder.encode(temporaryPassword));
        usuario.setMustChangePassword(true);
        usuario.setUpdatedBy(actor);
        usuarioRepository.save(usuario);

        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuarioId(usuario.getId());
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        token.setCreatedBy(actor);
        passwordResetTokenRepository.save(token);

        audit(usuario.getId(), UsuarioAuditAction.RESET_PASSWORD, actor, "Senha resetada por administrador.");
        final Long mailEmpresaId = usuario.getEmpresaId();
        final String mailTo = usuario.getEmail();
        final String mailName = usuario.getNome();
        final String mailTemp = temporaryPassword;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    passwordResetMailPublisher.publish(PasswordResetMailMessage.forAdminTempPassword(
                            mailEmpresaId, mailTo, mailName, mailTemp));
                } catch (Exception ex) {
                    log.warn("Falha ao publicar evento de e-mail de reset para fila: {}", ex.getMessage());
                }
            }
        });
        return new UsuarioResetPasswordResponse(usuario.getId(), temporaryPassword, token.getToken(), null);
    }

    @Transactional
    public void changeOwnPassword(String actor, String currentPassword, String newPassword, String confirmPassword) {
        Usuario usuario = findActor(actor);
        if (!StringUtils.hasText(currentPassword)) {
            throw new IllegalArgumentException("Informe a senha atual.");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter no minimo 6 caracteres.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("A confirmacao da senha nao confere.");
        }
        if (!passwordEncoder.matches(currentPassword, usuario.getSenhaHash())) {
            throw new AccessDeniedException("Senha atual invalida.");
        }
        if (passwordEncoder.matches(newPassword, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(newPassword));
        usuario.setMustChangePassword(false);
        usuario.setUpdatedBy(actor);
        usuarioRepository.save(usuario);
        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());
        audit(usuario.getId(), UsuarioAuditAction.CHANGE_OWN_PASSWORD, actor, "Senha alterada pelo proprio usuario.");
    }

    @Transactional(readOnly = true)
    public boolean mustChangePassword(String actor) {
        return findActor(actor).isMustChangePassword();
    }

    @Transactional
    public void delete(Long id, String actor) {
        Usuario actorUsuario = findActor(actor);
        ensureActorCanManageUsers(actorUsuario);

        Usuario usuario = findEntityForActor(id, actorUsuario);
        ensureActorCanManageTarget(actorUsuario, usuario);
        if (actorUsuario.getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Nao e permitido excluir a propria conta.");
        }
        ensureSingleSuperAdminNotDeleted(usuario);

        usuarioRepository.delete(usuario);
        passwordResetTokenRepository.deleteByUsuarioId(id);
        audit(id, UsuarioAuditAction.DELETE, actor, "Usuario removido.");
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAuditResponse> getAuditLog(Long usuarioId, Pageable pageable, String actor) {
        Usuario actorUsuario = findActor(actor);
        findEntityForActor(usuarioId, actorUsuario);
        return usuarioAuditLogRepository.findByUsuarioId(usuarioId, pageable)
                .map(log -> new UsuarioAuditResponse(log.getAction(), log.getPerformedBy(), log.getDetails(), log.getCreatedAt()));
    }

    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    private Usuario findEntityForActor(Long id, Usuario actorUsuario) {
        if (actorUsuario.getRole() == UserRole.SUPER_ADMIN) {
            return findEntity(id);
        }
        Long empresaId = requireTenantId(actorUsuario);
        return usuarioRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    private Usuario findActor(String actorEmail) {
        return usuarioRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado nao encontrado."));
    }

    private void ensureActorCanManageUsers(Usuario actor) {
        if (actor.getRole() != UserRole.SUPER_ADMIN && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Perfil sem permissao para gerenciar usuarios.");
        }
    }

    private void ensureActorCanAssignRole(Usuario actor, UserRole targetRole) {
        if (actor.getRole() == UserRole.ADMIN && targetRole == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Apenas SUPER_ADMIN pode atribuir perfil SUPER_ADMIN.");
        }
    }

    private void ensureActorCanManageTarget(Usuario actor, Usuario target) {
        if (actor.getRole() != UserRole.SUPER_ADMIN && target.getRole() == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Apenas SUPER_ADMIN pode gerenciar contas SUPER_ADMIN.");
        }
    }

    private void ensureSingleSuperAdminOnCreate(UserRole targetRole) {
        if (targetRole == UserRole.SUPER_ADMIN && usuarioRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            throw new IllegalArgumentException("Ja existe um SUPER_ADMIN unico na plataforma.");
        }
    }

    private void ensureSingleSuperAdminOnUpdate(Usuario currentUser, UserRole targetRole) {
        if (currentUser.getRole() != UserRole.SUPER_ADMIN
                && targetRole == UserRole.SUPER_ADMIN
                && usuarioRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            throw new IllegalArgumentException("Nao e permitido promover outro SUPER_ADMIN. Apenas um SUPER_ADMIN e permitido.");
        }
        if (currentUser.getRole() == UserRole.SUPER_ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Nao e permitido alterar o papel do SUPER_ADMIN unico da plataforma.");
        }
    }

    private void ensureSingleSuperAdminNotInactivated(Usuario target, boolean nextActiveStatus) {
        if (target.getRole() == UserRole.SUPER_ADMIN && !nextActiveStatus) {
            throw new AccessDeniedException("Nao e permitido inativar o SUPER_ADMIN unico da plataforma.");
        }
    }

    private void ensureSingleSuperAdminNotDeleted(Usuario target) {
        if (target.getRole() == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Nao e permitido excluir o SUPER_ADMIN unico da plataforma.");
        }
    }

    private void ensureReservedSuperAdminRoleForCreation(UserRole targetRole) {
        if (targetRole == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Perfil SUPER_ADMIN e reservado ao dono da plataforma e nao pode ser criado manualmente.");
        }
    }

    private Long requireTenantId(Usuario actorUsuario) {
        if (actorUsuario.getEmpresaId() == null) {
            throw new AccessDeniedException("Usuario sem empresa vinculada. Operacao bloqueada por seguranca de tenancy.");
        }
        return actorUsuario.getEmpresaId();
    }

    private Long resolveTenantForManagedUser(Usuario actorUsuario, UserRole targetRole) {
        if (targetRole == UserRole.SUPER_ADMIN) {
            return null;
        }
        if (actorUsuario.getRole() == UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Provisionamento de ADMIN/USER por SUPER_ADMIN deve usar fluxo com empresa_id.");
        }
        return requireTenantId(actorUsuario);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getCreatedBy(),
                usuario.getUpdatedBy(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }

    private void audit(Long usuarioId, UsuarioAuditAction action, String actor, String details) {
        UsuarioAuditLog log = new UsuarioAuditLog();
        log.setUsuarioId(usuarioId);
        log.setAction(action);
        log.setPerformedBy(actor);
        log.setDetails(details);
        usuarioAuditLogRepository.save(log);
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
