package com.erpcorporativo.web.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String loginErrorParam,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired,
            HttpServletRequest request,
            Model model
    ) {
        Object authError = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (authError != null || loginErrorParam != null) {
            model.addAttribute("loginError", "Credenciais invalidas.");
            request.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
        if (logout != null) {
            model.addAttribute("loginSuccess", "Sessao encerrada com sucesso.");
        }
        if (expired != null) {
            model.addAttribute("loginWarning", "Sua sessao expirou. Entre novamente.");
        }
        return "auth/login";
    }

}
