package com.erpcorporativo.api.service.auth;

import com.erpcorporativo.api.config.security.SeedSuperAdminProperties;
import com.erpcorporativo.api.domain.usuario.UserRole;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SeedSuperAdminService implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final SeedSuperAdminProperties properties;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }

        usuarioRepository.findByEmailIgnoreCase(properties.email()).ifPresentOrElse(
                existing -> {
                    // Existing account is kept to avoid overwriting credentials.
                },
                () -> {
                    Usuario superAdmin = new Usuario();
                    superAdmin.setNome(properties.name());
                    superAdmin.setEmail(properties.email());
                    superAdmin.setSenhaHash(passwordEncoder.encode(properties.password()));
                    superAdmin.setMustChangePassword(false);
                    superAdmin.setRole(UserRole.SUPER_ADMIN);
                    superAdmin.setAtivo(true);
                    superAdmin.setCreatedBy("seed");
                    superAdmin.setUpdatedBy("seed");
                    usuarioRepository.save(superAdmin);
                }
        );
    }
}
