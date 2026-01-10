package com.banda.barbershop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Redirect root URL to admin dashboard
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/admin-dashboard.html";
    }
}
