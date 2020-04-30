package com.face.nd.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HelloController {
    @RequestMapping("/")
    public String Hello(Model model) {
        model.addAttribute("config", EgciController.configEntity);
        return "alarm";
    }
}
