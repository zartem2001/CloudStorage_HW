package com.example.cloudStorage.service;

import com.example.cloudStorage.entity.Token;
import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.exception.BadCredentialsException;
import com.example.cloudStorage.exception.UnauthorizedException;
import com.example.cloudStorage.repository.TokenRepository;
import com.example.cloudStorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Token login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }
        String tokenValue = UUID.randomUUID().toString();
        Token token = Token.builder()
                .authToken(tokenValue)
                .user(user)
                .build();
        tokenRepository.save(token);
        log.info("Пользователь {} авторизован, выдан токен", login);
        return token;
    }

    public void logout(String authToken) {
        tokenRepository.deleteByAuthToken(authToken);
        log.info("Токен {} удалён", authToken);
    }

    public User getUserByToken(String authToken) {
        Token token = tokenRepository.findByAuthToken(authToken)
                .orElseThrow(() -> new UnauthorizedException("Токен не найден"));
        return token.getUser();
    }
}