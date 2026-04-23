package com.erpcorporativo.api.repository.configuracao;

import com.erpcorporativo.api.domain.configuracao.ConfiguracaoEmailEmpresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoEmailEmpresaRepository extends JpaRepository<ConfiguracaoEmailEmpresa, Long> {

    Optional<ConfiguracaoEmailEmpresa> findByEmpresa_Id(Long empresaId);
}
