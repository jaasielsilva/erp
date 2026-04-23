package com.erpcorporativo.web.controller.auth;

import com.erpcorporativo.api.service.usuario.UsuarioService;
import com.erpcorporativo.web.viewmodel.auth.ChangePasswordForm;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/minha-conta/alterar-senha")
@RequiredArgsConstructor
public class ChangePasswordController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String form(Model model, Principal principal) {
        model.addAttribute("email", principal.getName());
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        return "auth/change-password";
    }

    @PostMapping
    public String submit(
            @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        model.addAttribute("email", principal.getName());
        if (bindingResult.hasErrors()) {
            return "auth/change-password";
        }
        try {
            usuarioService.changeOwnPassword(
                    principal.getName(),
                    form.getCurrentPassword(),
                    form.getNewPassword(),
                    form.getConfirmPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Senha alterada com sucesso.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            bindingResult.reject("password.error", ex.getMessage());
            return "auth/change-password";
        }
    }
}
