package ru.artem.highload.social.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.artem.highload.social.web.dto.RegisterRequest;
import ru.artem.highload.social.web.dto.UserProfileResponse;
import ru.artem.highload.social.web.dto.UserSearchRequest;
import ru.artem.highload.social.web.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> register(@Valid @RequestBody RegisterRequest request) {
        Long id = userService.register(request);
        return Map.of("id", id);
    }

    @GetMapping("/get/{id}")
    public UserProfileResponse getProfile(@PathVariable Long id) {
        return userService.getProfileById(id);
    }

    @GetMapping("/search")
    public List<UserProfileResponse> search(@Valid UserSearchRequest request) {
        log.info("search request [{}]", request);
        return userService.search(request.firstName(), request.lastName());
    }
}
