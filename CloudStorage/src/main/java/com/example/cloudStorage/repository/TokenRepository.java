package com.example.cloudStorage.repository;

import com.example.cloudStorage.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByAuthToken(String authToken);
    void deleteByAuthToken(String authToken);
}