package ru.artem.highload.social.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.artem.highload.social.web.entity.UserEntity;
import ru.artem.highload.social.web.exception.InvalidCredentialsException;
import ru.artem.highload.social.web.repository.UserRepository;
import ru.artem.highload.social.web.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public String login(String login, String password) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtUtil.generateToken(user.getId());
    }
}
