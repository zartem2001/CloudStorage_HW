package com.example.cloudStorage.controller;


import com.example.cloudStorage.dto.ErrorResponse;
import com.example.cloudStorage.dto.LoginRequest;
import com.example.cloudStorage.dto.LoginResponse;
import com.example.cloudStorage.entity.Token;
import com.example.cloudStorage.exception.BadCredentialsException;
import com.example.cloudStorage.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.debug("Попытка входа пользователя {}", request.getLogin());
        try {
            Token token = authService.login(request.getLogin(), request.getPassword());
            return ResponseEntity.ok(new LoginResponse(token.getAuthToken()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}