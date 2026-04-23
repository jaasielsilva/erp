package com.erpcorporativo.web.viewmodel.empresa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpresaUpdateForm {

    @NotBlank(message = "Informe o nome fantasia da empresa.")
    @Size(max = 200, message = "Nome fantasia deve ter no maximo 200 caracteres.")
    private String nomeFantasia;

    private boolean ativo = true;
}
