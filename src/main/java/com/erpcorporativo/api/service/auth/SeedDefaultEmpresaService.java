package com.erpcorporativo.api.service.auth;

import com.erpcorporativo.api.domain.empresa.Empresa;
import com.erpcorporativo.api.repository.empresa.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garante ao menos uma empresa quando o banco está vazio (ex.: migração V5 não aplicada ainda).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class SeedDefaultEmpresaService implements CommandLineRunner {

    private final EmpresaRepository empresaRepository;

    @Override
    public void run(String... args) {
        if (empresaRepository.count() > 0) {
            return;
        }
        Empresa e = new Empresa();
        e.setNomeFantasia("Tenant padrão");
        e.setAtivo(true);
        empresaRepository.save(e);
    }
}
