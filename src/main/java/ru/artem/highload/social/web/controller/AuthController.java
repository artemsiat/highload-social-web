package ru.artem.highload.social.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.artem.highload.social.web.dto.LoginRequest;
import ru.artem.highload.social.web.dto.TokenResponse;
import ru.artem.highload.social.web.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.login(), request.password());
        return new TokenResponse(token);
    }
}
