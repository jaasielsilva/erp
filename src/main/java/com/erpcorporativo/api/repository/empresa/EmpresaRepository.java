package com.erpcorporativo.api.repository.empresa;

import com.erpcorporativo.api.domain.empresa.Empresa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findByAtivoTrueOrderByNomeFantasiaAsc();

    List<Empresa> findAllByOrderByNomeFantasiaAsc();

    boolean existsByNomeFantasiaIgnoreCase(String nomeFantasia);
}
