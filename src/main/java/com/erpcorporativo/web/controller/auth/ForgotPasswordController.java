package com.erpcorporativo.web.controller.auth;

import com.erpcorporativo.api.service.auth.PasswordRecoveryService;
import com.erpcorporativo.web.viewmodel.auth.ForgotPasswordForm;
import com.erpcorporativo.web.viewmodel.auth.ResetPasswordByTokenForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final PasswordRecoveryService passwordRecoveryService;

    @GetMapping("/esqueci-senha")
    public String forgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/esqueci-senha")
    public String requestReset(
            @Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        passwordRecoveryService.requestPasswordReset(form.getEmail());
        redirectAttributes.addFlashAttribute(
                "infoMessage",
                "Se o e-mail existir e estiver ativo, enviaremos instrucoes de recuperacao.");
        return "redirect:/esqueci-senha";
    }

    @GetMapping("/redefinir-senha")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        if (!passwordRecoveryService.isTokenValid(token)) {
            model.addAttribute("tokenInvalid", true);
            return "auth/reset-password";
        }
        if (!model.containsAttribute("resetForm")) {
            ResetPasswordByTokenForm form = new ResetPasswordByTokenForm();
            form.setToken(token);
            model.addAttribute("resetForm", form);
        }
        return "auth/reset-password";
    }

    @PostMapping("/redefinir-senha")
    public String resetSubmit(
            @Valid @ModelAttribute("resetForm") ResetPasswordByTokenForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tokenInvalid", !passwordRecoveryService.isTokenValid(form.getToken()));
            return "auth/reset-password";
        }
        try {
            passwordRecoveryService.resetPasswordWithToken(
                    form.getToken(),
                    form.getNewPassword(),
                    form.getConfirmPassword());
            redirectAttributes.addFlashAttribute(
                    "loginSuccess",
                    "Senha redefinida com sucesso. Entre com sua nova senha.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("tokenInvalid", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
