package com.erpcorporativo.api.controller.usuario;

import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.dto.usuario.UsuarioAuditResponse;
import com.erpcorporativo.api.dto.usuario.UsuarioCreateRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioRequest;
import com.erpcorporativo.api.dto.usuario.UsuarioResetPasswordResponse;
import com.erpcorporativo.api.dto.usuario.UsuarioResponse;
import com.erpcorporativo.api.service.usuario.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class UsuarioApiController {

    private final UsuarioService usuarioService;

    @Value("${app.security.expose-password-reset-credentials:false}")
    private boolean exposePasswordResetCredentials;

    @GetMapping
    public Page<UsuarioResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return usuarioService.search(q, role, ativo, pageable, authentication.getName());
    }

    @GetMapping("/{id}")
    public UsuarioResponse get(@PathVariable Long id, Authentication authentication) {
        return usuarioService.getById(id, authentication.getName());
    }

    @PostMapping
    public UsuarioResponse create(@RequestBody @Valid UsuarioCreateRequest request, Authentication authentication) {
        return usuarioService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@PathVariable Long id, @RequestBody @Valid UsuarioRequest request, Authentication authentication) {
        return usuarioService.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/toggle-status")
    public UsuarioResponse toggleStatus(@PathVariable Long id, Authentication authentication) {
        return usuarioService.toggleStatus(id, authentication.getName());
    }

    @PostMapping("/{id}/reset-password")
    public UsuarioResetPasswordResponse resetPassword(@PathVariable Long id, Authentication authentication) {
        UsuarioResetPasswordResponse result = usuarioService.resetPasswordByAdmin(id, authentication.getName());
        if (!exposePasswordResetCredentials) {
            return UsuarioResetPasswordResponse.concealed(
                    result.usuarioId(),
                    "Solicitacao concluida. A senha temporaria nao e retornada por API neste ambiente. "
                            + "Repasse a senha ao usuario por um canal seguro, ou defina EXPOSE_PASSWORD_RESET_CREDENTIALS=true "
                            + "apenas em desenvolvimento. Token de reset nunca e retornado na API. "
                            + "No proximo login, o usuario trocara a senha.");
        }
        // Desenvolvimento: expoe apenas senha temporaria; token nunca sai na resposta (fluxo seguro depois: e-mail/link).
        return new UsuarioResetPasswordResponse(
                result.usuarioId(),
                result.temporaryPassword(),
                null,
                "Modo desenvolvimento: senha temporaria no campo indicado. Token nao e exposto na API."
        );
    }

    @GetMapping("/{id}/audit")
    public Page<UsuarioAuditResponse> audit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return usuarioService.getAuditLog(id, pageable, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        usuarioService.delete(id, authentication.getName());
    }
}
