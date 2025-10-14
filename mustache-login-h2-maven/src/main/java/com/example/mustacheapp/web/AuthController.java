package com.example.mustacheapp.web;

import com.example.mustacheapp.model.User;
import com.example.mustacheapp.repository.UserRepository;
import com.example.mustacheapp.web.dto.RegisterDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {
        if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            model.addAttribute("error", "Todos los campos son obligatorios");
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "register";
        }
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "El usuario ya existe");
            return "register";
        }
        String hash = encoder.encode(password);
        User user = new User(username, hash, "USER");
        userRepository.save(user);
        return "redirect:/login?registered";
    }

}
