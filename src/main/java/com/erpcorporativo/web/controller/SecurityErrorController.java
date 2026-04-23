package com.erpcorporativo.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecurityErrorController {

    @GetMapping("/acesso-negado")
    public String accessDenied() {
        return "error/access-denied";
    }
}
