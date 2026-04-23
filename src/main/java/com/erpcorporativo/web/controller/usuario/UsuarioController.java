package com.erpcorporativo.web.controller.usuario;

import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.dto.usuario.UsuarioCreateRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioResponse;
import com.erpcorporativo.api.service.usuario.UsuarioService;
import com.erpcorporativo.web.viewmodel.usuario.UsuarioForm;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Value("${app.security.expose-password-reset-credentials:false}")
    private boolean exposePasswordResetCredentials;

    private static final List<UserRole> ASSIGNABLE_ROLES = Arrays.stream(UserRole.values())
            .filter(role -> role != UserRole.SUPER_ADMIN)
            .toList();

    @ModelAttribute("canManageUsers")
    boolean canManageUsers() {
        return true;
    }

    @ModelAttribute("canManageCompanies")
    boolean canManageCompanies(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<UsuarioResponse> usuarios = usuarioService.search(q, role, ativo, pageable, principal.getName());

        model.addAttribute("email", principal.getName());
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("ativo", ativo);
        model.addAttribute("roleValues", ASSIGNABLE_ROLES);
        return "usuarios/index";
    }

    @GetMapping("/novo")
    public String newForm(Model model, Principal principal) {
        UsuarioForm form = new UsuarioForm();
        form.setAtivo(true);
        model.addAttribute("email", principal.getName());
        model.addAttribute("isEdit", false);
        model.addAttribute("usuarioId", null);
        model.addAttribute("usuarioForm", form);
        model.addAttribute("roleValues", ASSIGNABLE_ROLES);
        return "usuarios/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.getSenha() == null || form.getSenha().isBlank()) {
            bindingResult.rejectValue("senha", "senha.required", "Informe uma senha inicial.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", false);
            model.addAttribute("usuarioId", null);
            model.addAttribute("roleValues", ASSIGNABLE_ROLES);
            return "usuarios/form";
        }

        usuarioService.create(new UsuarioCreateRequest(
                form.getNome(),
                form.getEmail(),
                form.getRole(),
                form.isAtivo(),
                form.getSenha()
        ), principal.getName());

        redirectAttributes.addFlashAttribute("successMessage", "Usuário criado com sucesso.");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        UsuarioResponse usuario = usuarioService.getById(id, principal.getName());
        UsuarioForm form = new UsuarioForm();
        form.setNome(usuario.nome());
        form.setEmail(usuario.email());
        form.setRole(usuario.role());
        form.setAtivo(usuario.ativo());

        model.addAttribute("email", principal.getName());
        model.addAttribute("isEdit", true);
        model.addAttribute("usuarioId", id);
        model.addAttribute("usuarioForm", form);
        model.addAttribute("roleValues", ASSIGNABLE_ROLES);
        return "usuarios/form";
    }

    @PostMapping("/{id}/editar")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", true);
            model.addAttribute("usuarioId", id);
            model.addAttribute("roleValues", ASSIGNABLE_ROLES);
            return "usuarios/form";
        }

        usuarioService.update(id, new UsuarioRequest(
                form.getNome(),
                form.getEmail(),
                form.getRole(),
                form.isAtivo()
        ), principal.getName());

        redirectAttributes.addFlashAttribute("successMessage", "Usuário atualizado com sucesso.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        UsuarioResponse response = usuarioService.toggleStatus(id, principal.getName());
        String status = response.ativo() ? "ativado" : "inativado";
        redirectAttributes.addFlashAttribute("successMessage", "Usuário " + status + " com sucesso.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (exposePasswordResetCredentials) {
            var resetResult = usuarioService.resetPasswordByAdmin(id, principal.getName());
            redirectAttributes.addFlashAttribute(
                    "warningMessage",
                    "Senha temporária (somente dev): " + resetResult.temporaryPassword()
                            + ". No próximo login será exigida nova senha."
            );
        } else {
            usuarioService.resetPasswordByAdmin(id, principal.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Senha redefinida. No próximo login o usuário definirá uma nova senha."
            );
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/excluir")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        usuarioService.delete(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Usuário removido com sucesso.");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/auditoria")
    public String audit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UsuarioResponse usuario = usuarioService.getById(id, principal.getName());
        model.addAttribute("email", principal.getName());
        model.addAttribute("usuario", usuario);
        model.addAttribute("auditsPage", usuarioService.getAuditLog(id, pageable, principal.getName()));
        model.addAttribute("pageSize", size);
        return "usuarios/auditoria";
    }
}
