package com.example.mustacheapp.web;

import com.example.mustacheapp.model.Credential;
import com.example.mustacheapp.repository.CredentialRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final CredentialRepository credentialRepository;

    public HomeController(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal User user, Model model) {
        List<Credential> creds = credentialRepository.findByOwnerUsername(user.getUsername());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("credentials", creds);
        return "home";
    }
}