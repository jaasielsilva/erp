package com.erpcorporativo.web.controller.configuracao;

import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import com.erpcorporativo.api.service.configuracao.ConfiguracaoEmailService;
import com.erpcorporativo.api.service.mail.SmtpConnectionTestService;
import com.erpcorporativo.api.repository.empresa.EmpresaRepository;
import com.erpcorporativo.web.viewmodel.configuracao.EmailConfigForm;
import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class ConfiguracoesController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoEmailService configuracaoEmailService;
    private final SmtpConnectionTestService smtpConnectionTestService;

    @GetMapping("/email")
    public String email(
            @RequestParam(required = false) Long empresaId,
            Principal principal,
            Authentication authentication,
            Model model) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado."));

        model.addAttribute("email", principal.getName());
        model.addAttribute("canManageUsers", canManageUsers(authentication));
        model.addAttribute("canManageCompanies", canManageCompanies(authentication));

        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            var empresas = empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc();
            if (empresas.isEmpty()) {
                model.addAttribute("noEmpresas", true);
                model.addAttribute("emailConfigForm", new EmailConfigForm());
                return "configuracoes/email";
            }
            model.addAttribute("empresas", empresas);
        }

        Long targetEmpresaId = resolveTargetEmpresaIdForGet(actor, empresaId, model);
        model.addAttribute("empresaSelecionadaId", targetEmpresaId);
        model.addAttribute("empresaNome",
                empresaRepository.findById(targetEmpresaId).map(e -> e.getNomeFantasia()).orElse(""));
        model.addAttribute("emailConfigForm", configuracaoEmailService.loadForm(targetEmpresaId));
        model.addAttribute("hasStoredSmtpPassword", configuracaoEmailService.hasStoredSmtpPassword(targetEmpresaId));
        return "configuracoes/email";
    }

    @PostMapping("/email")
    public String saveEmail(
            @RequestParam(required = false) Long empresaId,
            @ModelAttribute("emailConfigForm") EmailConfigForm form,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado."));

        if (actor.getRole() == UserRole.SUPER_ADMIN
                && empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nenhuma empresa ativa. Cadastre ou restaure empresas antes de salvar.");
            return "redirect:/configuracoes/email";
        }

        try {
            Long targetEmpresaId = resolveTargetEmpresaId(actor, empresaId);
            configuracaoEmailService.save(targetEmpresaId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Configuracao de e-mail salva.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return redirectToEmailConfig(actor, empresaId);
    }

    @PostMapping("/email/teste")
    public String testarEmail(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String testRecipientEmail,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        Usuario actor = usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado."));

        if (actor.getRole() == UserRole.SUPER_ADMIN
                && empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nenhuma empresa ativa. Impossivel testar.");
            return "redirect:/configuracoes/email";
        }

        String to = (testRecipientEmail != null && !testRecipientEmail.isBlank()) ? testRecipientEmail.trim() : principal.getName();

        try {
            Long target = resolveTargetEmpresaId(actor, empresaId);
            smtpConnectionTestService.sendTestEmail(target, to);
            redirectAttributes.addFlashAttribute("successMessage", "Teste enviado para " + to + ". Verifique a caixa de entrada (e o spam).");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Falha no teste: " + ex.getMessage());
        }

        return redirectToEmailConfig(actor, empresaId);
    }

    private String redirectToEmailConfig(Usuario actor, Long empresaId) {
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            var list = empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc();
            if (list.isEmpty()) {
                return "redirect:/configuracoes/email";
            }
            long id;
            if (empresaId != null && empresaRepository.existsById(empresaId)) {
                id = empresaId;
            } else {
                id = list.get(0).getId();
            }
            return "redirect:/configuracoes/email?empresaId=" + id;
        }
        return "redirect:/configuracoes/email";
    }

    private Long resolveTargetEmpresaIdForGet(Usuario actor, Long empresaIdParam, Model model) {
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            var list = empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc();
            if (empresaIdParam != null) {
                if (empresaRepository.existsById(empresaIdParam)) {
                    return empresaIdParam;
                }
                model.addAttribute("warningMessage", "Empresa nao encontrada. Exibindo a primeira da lista.");
            }
            return list.get(0).getId();
        }
        Long tenant = actor.getEmpresaId();
        if (tenant == null) {
            throw new AccessDeniedException("Usuario sem empresa vinculada.");
        }
        if (empresaIdParam != null && !empresaIdParam.equals(tenant)) {
            throw new AccessDeniedException("Acesso negado a outro tenant.");
        }
        return tenant;
    }

    private Long resolveTargetEmpresaId(Usuario actor, Long empresaIdParam) {
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            if (empresaIdParam != null) {
                if (empresaRepository.existsById(empresaIdParam)) {
                    return empresaIdParam;
                }
                throw new IllegalArgumentException("Empresa invalida.");
            }
            return empresaRepository.findByAtivoTrueOrderByNomeFantasiaAsc()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada."))
                    .getId();
        }
        Long tenant = actor.getEmpresaId();
        if (tenant == null) {
            throw new AccessDeniedException("Usuario sem empresa vinculada.");
        }
        if (empresaIdParam != null && !empresaIdParam.equals(tenant)) {
            throw new AccessDeniedException("Acesso negado a outro tenant.");
        }
        return tenant;
    }

    private static boolean canManageUsers(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static boolean canManageCompanies(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
