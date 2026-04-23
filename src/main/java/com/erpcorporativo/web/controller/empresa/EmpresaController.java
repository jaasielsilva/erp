package com.erpcorporativo.web.controller.empresa;

import com.erpcorporativo.api.service.empresa.EmpresaService;
import com.erpcorporativo.web.viewmodel.empresa.EmpresaForm;
import com.erpcorporativo.web.viewmodel.empresa.EmpresaUpdateForm;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/empresas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmpresaController {

    private final EmpresaService empresaService;

    @ModelAttribute("canManageUsers")
    boolean canManageUsers() {
        return true;
    }

    @ModelAttribute("canManageCompanies")
    boolean canManageCompanies() {
        return true;
    }

    @GetMapping
    public String list(Model model, Principal principal) {
        var empresas = empresaService.listAll();
        Map<Long, Long> adminCounts = new LinkedHashMap<>();
        Map<Long, Long> userCounts = new LinkedHashMap<>();
        Map<Long, Long> activeUserCounts = new LinkedHashMap<>();
        for (var empresa : empresas) {
            adminCounts.put(empresa.getId(), empresaService.countAdmins(empresa.getId()));
            userCounts.put(empresa.getId(), empresaService.countUsers(empresa.getId()));
            activeUserCounts.put(empresa.getId(), empresaService.countActiveUsers(empresa.getId()));
        }
        model.addAttribute("email", principal.getName());
        model.addAttribute("empresas", empresas);
        model.addAttribute("adminCounts", adminCounts);
        model.addAttribute("userCounts", userCounts);
        model.addAttribute("activeUserCounts", activeUserCounts);
        return "empresas/index";
    }

    @GetMapping("/nova")
    public String newForm(Model model, Principal principal) {
        if (!model.containsAttribute("empresaForm")) {
            model.addAttribute("empresaForm", new EmpresaForm());
        }
        model.addAttribute("email", principal.getName());
        model.addAttribute("isEdit", false);
        model.addAttribute("empresaId", null);
        return "empresas/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("empresaForm") EmpresaForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", false);
            model.addAttribute("empresaId", null);
            return "empresas/form";
        }
        try {
            var empresa = empresaService.createWithAdmin(form, principal.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Empresa criada com sucesso. Admin inicial provisionado e obrigado a trocar senha no primeiro login.");
            return "redirect:/empresas/" + empresa.getId() + "/editar";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("empresa.error", ex.getMessage());
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", false);
            model.addAttribute("empresaId", null);
            return "empresas/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        var empresa = empresaService.getById(id);
        EmpresaUpdateForm form = new EmpresaUpdateForm();
        form.setNomeFantasia(empresa.getNomeFantasia());
        form.setAtivo(empresa.isAtivo());
        model.addAttribute("email", principal.getName());
        model.addAttribute("isEdit", true);
        model.addAttribute("empresaId", id);
        model.addAttribute("empresaForm", form);
        return "empresas/form";
    }

    @PostMapping("/{id}/editar")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("empresaForm") EmpresaUpdateForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", true);
            model.addAttribute("empresaId", id);
            return "empresas/form";
        }
        try {
            empresaService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Empresa atualizada com sucesso.");
            return "redirect:/empresas";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("empresa.error", ex.getMessage());
            model.addAttribute("email", principal.getName());
            model.addAttribute("isEdit", true);
            model.addAttribute("empresaId", id);
            return "empresas/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var empresa = empresaService.toggleStatus(id);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Empresa " + (empresa.isAtivo() ? "ativada" : "inativada") + " com sucesso.");
        return "redirect:/empresas";
    }
}
