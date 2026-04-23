package com.erpcorporativo.web.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "com.erpcorporativo.web.controller")
public class WebExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : "Você não possui permissão para executar esta ação.");
        return "error/access-denied";
    }

    @ExceptionHandler({EntityNotFoundException.class, IllegalArgumentException.class})
    public String handleBusinessErrors(Exception ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/generic";
    }
}
