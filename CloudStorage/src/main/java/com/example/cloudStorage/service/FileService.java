package com.example.cloudStorage.service;

import com.example.cloudStorage.entity.FileEntity;
import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.exception.FileAlreadyExistsException;
import com.example.cloudStorage.exception.FileNotFoundException;
import com.example.cloudStorage.repository.FileEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final FileEntityRepository fileRepository;

    @Value("${app.storage.location}")
    private String storagePath;

    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(User user, String filename, MultipartFile file) throws IOException {
        Path userDir = Paths.get(storagePath, user.getId().toString());
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        Path filePath = userDir.resolve(filename);

        file.transferTo(filePath);

        try {
            if (fileRepository.findByUserAndFilename(user, filename).isPresent()) {
                throw new FileAlreadyExistsException("Файл с именем " + filename + " уже существует");
            }
            FileEntity entity = FileEntity.builder()
                    .filename(filename)
                    .size(file.getSize())
                    .filePath(filePath.toString())
                    .user(user)
                    .build();
            fileRepository.save(entity);
            log.info("Файл {} загружен пользователем {}", filename, user.getLogin());
        } catch (Exception e) {
            Files.deleteIfExists(filePath);
            throw e;
        }
    }

    public Resource downloadFile(User user, String filename) throws IOException {
        FileEntity entity = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден"));
        Path path = Paths.get(entity.getFilePath());
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Файл отсутствует на диске");
        }
        InputStream inputStream = Files.newInputStream(path);
        return new InputStreamResource(inputStream);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(User user, String filename) throws IOException {
        FileEntity entity = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден"));
        fileRepository.delete(entity);
        Path path = Paths.get(entity.getFilePath());
        Files.deleteIfExists(path);
        log.info("Файл {} удалён пользователем {}", filename, user.getLogin());
    }

    @Transactional(rollbackFor = Exception.class)
    public void renameFile(User user, String oldName, String newName) throws IOException {
        FileEntity entity = fileRepository.findByUserAndFilename(user, oldName)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден"));
        if (fileRepository.findByUserAndFilename(user, newName).isPresent()) {
            throw new FileAlreadyExistsException("Файл с именем " + newName + " уже существует");
        }
        Path oldPath = Paths.get(entity.getFilePath());
        Path newPath = oldPath.resolveSibling(newName);
        entity.setFilename(newName);
        entity.setFilePath(newPath.toString());
        fileRepository.save(entity);

        Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
        log.info("Файл {} переименован в {} пользователем {}", oldName, newName, user.getLogin());
    }

    public List<FileEntity> getFiles(User user, Integer limit) {
        List<FileEntity> entities = fileRepository.findAllByUser(user);
        if (limit != null && limit > 0) {
            entities = entities.stream().limit(limit).collect(Collectors.toList());
        }
        return entities;
    }
}