package com.example.mustacheapp;

import com.example.mustacheapp.model.User;
import com.example.mustacheapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MustacheLoginH2Application {
    public static void main(String[] args) {
        SpringApplication.run(MustacheLoginH2Application.class, args);
    }

    @Bean
    public CommandLineRunner createDefaultUser(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            String username = "paco";
            if (!userRepository.existsByUsername(username)) {
                String hash = encoder.encode("paco");
                User user = new User(username, hash, "USER");
                userRepository.save(user);
                System.out.println("Created default user 'paco'");
            } else {
                System.out.println("User 'paco' already exists");
            }
        };
    }
}