package org.example.rpbo.controller;

import lombok.RequiredArgsConstructor;
import org.example.rpbo.model.ApplicationUser;
import org.example.rpbo.model.ApplicationRole;
import org.example.rpbo.model.RegistrationRequest;
import org.example.rpbo.repository.ApplicationUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final ApplicationUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        // Проверка, существует ли уже такой email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email уже используется");
        }

        // Создание нового пользователя и установка свойств
        ApplicationUser newUser = new ApplicationUser();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(ApplicationRole.USER); // Устанавливаем роль по умолчанию

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body("Пользователь успешно зарегистрирован");
    }
}
