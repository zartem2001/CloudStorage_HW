package com.example.cloudStorage.repository;

import com.example.cloudStorage.entity.FileEntity;
import com.example.cloudStorage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUserAndFilename(User user, String filename);
    List<FileEntity> findAllByUser(User user);
    void deleteByUserAndFilename(User user, String filename);
}
