package com.example.recipeapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // ルートパスから home にリダイレクト
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }
}