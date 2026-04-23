package com.erpcorporativo.web.controller;

import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Set;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, Principal principal, Authentication authentication) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado."));

        model.addAttribute("email", principal.getName());
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            model.addAttribute("activeUsers", usuarioRepository.countByAtivo(true));
            model.addAttribute("inactiveUsers", usuarioRepository.countByAtivo(false));
            model.addAttribute("adminUsers", usuarioRepository.countByRole(UserRole.ADMIN));
            model.addAttribute("superAdminUsers", usuarioRepository.countByRole(UserRole.SUPER_ADMIN));
        } else {
            Long empresaId = actor.getEmpresaId();
            if (empresaId == null) {
                throw new AccessDeniedException("Usuario sem empresa vinculada. Operacao bloqueada por seguranca de tenancy.");
            }
            model.addAttribute("activeUsers", usuarioRepository.countByAtivoAndEmpresaId(true, empresaId));
            model.addAttribute("inactiveUsers", usuarioRepository.countByAtivoAndEmpresaId(false, empresaId));
            model.addAttribute("adminUsers", usuarioRepository.countByRoleAndEmpresaId(UserRole.ADMIN, empresaId));
            model.addAttribute("superAdminUsers", 0L);
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
        boolean canManageUsers = authorities.contains("ROLE_SUPER_ADMIN") || authorities.contains("ROLE_ADMIN");
        boolean canManageCompanies = authorities.contains("ROLE_SUPER_ADMIN");
        model.addAttribute("canManageUsers", canManageUsers);
        model.addAttribute("canManageCompanies", canManageCompanies);
        return "dashboard/index";
    }
}
