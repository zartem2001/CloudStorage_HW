package com.example.cloudStorage;

import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (userRepository.findByLogin("user").isEmpty()) {
            User user = User.builder()
                    .login("user")
                    .password(passwordEncoder.encode("password"))
                    .build();
            userRepository.save(user);
            log.info("Создан тестовый пользователь: user / password");
        }
    }
}
