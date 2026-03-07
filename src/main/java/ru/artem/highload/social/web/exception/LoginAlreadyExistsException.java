package ru.artem.highload.social.web.exception;

public class LoginAlreadyExistsException extends RuntimeException {
    public LoginAlreadyExistsException(String login) {
        super("Login already exists: " + login);
    }
}
