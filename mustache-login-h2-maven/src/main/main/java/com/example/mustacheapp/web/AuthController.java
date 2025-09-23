package com.example.mustacheapp.web;

import com.example.mustacheapp.model.User;
import com.example.mustacheapp.repository.UserRepository;
import com.example.mustacheapp.web.dto.RegisterDto;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "register";
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            model.addAttribute("error", "El usuario ya existe");
            return "register";
        }
        String hash = encoder.encode(dto.getPassword());
        User user = new User(dto.getUsername(), hash, "USER");
        userRepository.save(user);
        model.addAttribute("success", "Usuario registrado. Inicia sesión.");
        return "login";
    }
}