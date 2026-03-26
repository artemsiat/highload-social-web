package ru.artem.highload.social.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.artem.highload.social.web.dto.RegisterRequest;
import ru.artem.highload.social.web.dto.UserProfileResponse;
import ru.artem.highload.social.web.entity.UserEntity;
import ru.artem.highload.social.web.exception.LoginAlreadyExistsException;
import ru.artem.highload.social.web.exception.UserNotFoundException;
import ru.artem.highload.social.web.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Long register(RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        try {
            return userRepository.createUser(
                    request.login(), hash,
                    request.firstName(), request.lastName(),
                    request.birthDate(), request.gender(),
                    request.interests(), request.city()
            );
        } catch (DuplicateKeyException e) {
            throw new LoginAlreadyExistsException(request.login());
        }
    }

    public List<UserProfileResponse> search(String firstNamePrefix, String lastNamePrefix) {
        return userRepository.searchByFirstNameAndLastNamePrefix(firstNamePrefix, lastNamePrefix)
                .stream()
                .map(user -> new UserProfileResponse(
                        user.getId(), user.getFirstName(), user.getLastName(),
                        user.getBirthDate(), user.getGender(),
                        user.getInterests(), user.getCity()
                ))
                .toList();
    }

    public UserProfileResponse getProfileById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return new UserProfileResponse(
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getBirthDate(), user.getGender(),
                user.getInterests(), user.getCity()
        );
    }
}
