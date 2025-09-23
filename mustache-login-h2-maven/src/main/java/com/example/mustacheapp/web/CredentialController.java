package com.example.mustacheapp.web;

import com.example.mustacheapp.model.Credential;
import com.example.mustacheapp.repository.CredentialRepository;
import com.example.mustacheapp.util.EncryptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/credentials")
public class CredentialController {

    private final CredentialRepository credentialRepository;
    private final EncryptionService encryptionService;

    public CredentialController(CredentialRepository credentialRepository, EncryptionService encryptionService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("credential", new Credential());
        return "credential_form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Credential credential, @AuthenticationPrincipal User user) {
        // encrypt password before saving
        String encrypted = encryptionService.encrypt(credential.getPasswordEncrypted());
        credential.setPasswordEncrypted(encrypted);
        credential.setOwnerUsername(user.getUsername());
        credentialRepository.save(credential);
        return "redirect:/";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model, @AuthenticationPrincipal User user) {
        Credential cred = credentialRepository.findById(id).orElse(null);
        if (cred == null || !cred.getOwnerUsername().equals(user.getUsername())) {
            return "redirect:/";
        }
        String decrypted = encryptionService.decrypt(cred.getPasswordEncrypted());
        model.addAttribute("credential", cred);
        model.addAttribute("decrypted", decrypted);
        return "credential_view";
    }
}