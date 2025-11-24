package com.example.demo.controller;

import com.example.demo.model.Newsletter;
import com.example.demo.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @GetMapping("/newsletter")
    public String newsletter(Model model) {

        List<Newsletter> newsletters = newsletterService.getDummyNewsletters();
        model.addAttribute("newsletters", newsletters);

        return "newsletter";
    }
}

