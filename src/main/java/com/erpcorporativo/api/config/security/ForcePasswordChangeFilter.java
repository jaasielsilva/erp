package com.erpcorporativo.api.config.security;

import com.erpcorporativo.api.service.usuario.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        if (!usuarioService.mustChangePassword(authentication.getName()) || isAllowedPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (requestUri.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Troca de senha obrigatoria pendente.\",\"timestamp\":\""
                            + java.time.Instant.now()
                            + "\"}");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/minha-conta/alterar-senha");
    }

    private boolean isAllowedPath(String requestUri) {
        return "/minha-conta/alterar-senha".equals(requestUri)
                || "/logout".equals(requestUri)
                || requestUri.startsWith("/css/")
                || requestUri.startsWith("/js/")
                || requestUri.startsWith("/actuator/");
    }
}
