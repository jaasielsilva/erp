package com.erpcorporativo.api.service.empresa;

import com.erpcorporativo.api.domain.empresa.Empresa;
import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditAction;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditLog;
import com.erpcorporativo.api.repository.empresa.EmpresaRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioAuditLogRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import com.erpcorporativo.web.viewmodel.empresa.EmpresaForm;
import com.erpcorporativo.web.viewmodel.empresa.EmpresaUpdateForm;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuditLogRepository usuarioAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Empresa> listAll() {
        return empresaRepository.findAllByOrderByNomeFantasiaAsc();
    }

    @Transactional(readOnly = true)
    public Empresa getById(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa nao encontrada."));
    }

    @Transactional
    public Empresa createWithAdmin(EmpresaForm form, String actorEmail) {
        String nomeFantasia = normalizeText(form.getNomeFantasia());
        if (!StringUtils.hasText(nomeFantasia)) {
            throw new IllegalArgumentException("Nome fantasia e obrigatorio.");
        }
        if (empresaRepository.existsByNomeFantasiaIgnoreCase(nomeFantasia)) {
            throw new IllegalArgumentException("Ja existe uma empresa com este nome fantasia.");
        }
        String adminEmail = normalizeEmail(form.getAdminEmail());
        if (!StringUtils.hasText(adminEmail)) {
            throw new IllegalArgumentException("E-mail do administrador e obrigatorio.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("Ja existe usuario com este e-mail.");
        }
        String adminNome = normalizeText(form.getAdminNome());
        if (!StringUtils.hasText(adminNome)) {
            throw new IllegalArgumentException("Nome do administrador inicial e obrigatorio.");
        }
        if (!StringUtils.hasText(form.getAdminSenha())) {
            throw new IllegalArgumentException("Senha inicial do administrador e obrigatoria.");
        }

        Empresa empresa = new Empresa();
        empresa.setNomeFantasia(nomeFantasia);
        empresa.setAtivo(form.isAtivo());
        Empresa savedEmpresa = empresaRepository.save(empresa);

        Usuario admin = new Usuario();
        admin.setNome(adminNome);
        admin.setEmail(adminEmail);
        admin.setRole(UserRole.ADMIN);
        admin.setAtivo(true);
        admin.setEmpresaId(savedEmpresa.getId());
        admin.setSenhaHash(passwordEncoder.encode(form.getAdminSenha()));
        admin.setMustChangePassword(true);
        admin.setCreatedBy(actorEmail);
        admin.setUpdatedBy(actorEmail);
        Usuario savedAdmin = usuarioRepository.save(admin);

        UsuarioAuditLog audit = new UsuarioAuditLog();
        audit.setUsuarioId(savedAdmin.getId());
        audit.setAction(UsuarioAuditAction.CREATE);
        audit.setPerformedBy(actorEmail);
        audit.setDetails("Usuario administrador provisionado na criacao da empresa " + savedEmpresa.getNomeFantasia() + ".");
        usuarioAuditLogRepository.save(audit);

        return savedEmpresa;
    }

    @Transactional
    public Empresa update(Long id, EmpresaUpdateForm form) {
        Empresa empresa = getById(id);
        String nomeFantasia = normalizeText(form.getNomeFantasia());
        if (!StringUtils.hasText(nomeFantasia)) {
            throw new IllegalArgumentException("Nome fantasia e obrigatorio.");
        }
        if (!empresa.getNomeFantasia().equalsIgnoreCase(nomeFantasia)
                && empresaRepository.existsByNomeFantasiaIgnoreCase(nomeFantasia)) {
            throw new IllegalArgumentException("Ja existe uma empresa com este nome fantasia.");
        }
        empresa.setNomeFantasia(nomeFantasia);
        empresa.setAtivo(form.isAtivo());
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa toggleStatus(Long id) {
        Empresa empresa = getById(id);
        empresa.setAtivo(!empresa.isAtivo());
        return empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public long countAdmins(Long empresaId) {
        return usuarioRepository.countByRoleAndEmpresaId(UserRole.ADMIN, empresaId);
    }

    @Transactional(readOnly = true)
    public long countUsers(Long empresaId) {
        return usuarioRepository.countByEmpresaId(empresaId);
    }

    @Transactional(readOnly = true)
    public long countActiveUsers(Long empresaId) {
        return usuarioRepository.countByEmpresaIdAndAtivo(empresaId, true);
    }

    private static String normalizeText(String input) {
        return input == null ? "" : input.trim();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
