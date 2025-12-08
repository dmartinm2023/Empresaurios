package com.example.mustacheapp.web;

import com.example.mustacheapp.model.Credential;
import com.example.mustacheapp.repository.CredentialRepository;
import com.example.mustacheapp.util.BlockchainService;
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
    private final BlockchainService blockchainService;

    public CredentialController(CredentialRepository credentialRepository,
                                EncryptionService encryptionService,
                                BlockchainService blockchainService) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.blockchainService = blockchainService;
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("credential", new Credential());
        return "credential_form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Credential credential, @AuthenticationPrincipal User user) {
        // Plain-text password before encrypting
        String clearPassword = credential.getPasswordEncrypted();

        // 1) Encrypt for local storage
        String encrypted = encryptionService.encrypt(clearPassword);
        credential.setPasswordEncrypted(encrypted);
        credential.setOwnerUsername(user.getUsername());

        // 2) Send commitment on-chain (best-effort; if it fails we log and continue)
        try {
            BlockchainService.CommitResult result =
                    blockchainService.sendPasswordCommitment(credential.getTitle(), clearPassword);
            credential.setOnChainCommitment(result.getCommitmentHex());
            credential.setOnChainTxHash(result.getTxHash());
        } catch (RuntimeException ex) {
            // In a real app you'd use a logger; here we just ignore for UX simplicity.
            System.err.println("Blockchain error (ignored for UX): " + ex.getMessage());
        }

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
