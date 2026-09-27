package com.example.cloudStorage;

import com.example.cloudStorage.dto.LoginRequest;
import com.example.cloudStorage.dto.LoginResponse;
import com.example.cloudStorage.entity.Token;
import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.exception.BadCredentialsException;
import com.example.cloudStorage.repository.TokenRepository;
import com.example.cloudStorage.repository.UserRepository;
import com.example.cloudStorage.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccess() {
        User user = User.builder().id(1L).login("user").password("hash").build();
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token token = authService.login("user", "password");
        assertNotNull(token.getAuthToken());
        assertFalse(token.getAuthToken().isEmpty());
        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    void loginWrongPassword() {
        User user = User.builder().id(1L).login("user").password("hash").build();
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.login("user", "wrong"));
    }
}
