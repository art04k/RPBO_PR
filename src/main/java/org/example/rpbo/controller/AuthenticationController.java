package org.example.rpbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.rpbo.configuration.JwtTokenProvider;
import org.example.rpbo.model.ApplicationUser;
import org.example.rpbo.model.AuthenticationRequest;
import org.example.rpbo.model.AuthenticationResponse;
import org.example.rpbo.repository.ApplicationUserRepository;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final ApplicationUserRepository ApplicationUserRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            String email = request.getEmail();

            ApplicationUser user = ApplicationUserRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

            authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email, request.getPassword())
                    );

            String token = jwtTokenProvider
                    .createToken(email, user.getRole().getGrantedAuthorities());

            return ResponseEntity.ok(new AuthenticationResponse(email, token));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }
}
