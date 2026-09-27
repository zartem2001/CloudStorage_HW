package com.example.cloudStorage.controller;

import com.example.cloudStorage.dto.ErrorResponse;
import com.example.cloudStorage.dto.FileDto;
import com.example.cloudStorage.dto.RenameRequest;
import com.example.cloudStorage.entity.FileEntity;
import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.exception.FileNotFoundException;
import com.example.cloudStorage.exception.UnauthorizedException;
import com.example.cloudStorage.exception.FileAlreadyExistsException;
import com.example.cloudStorage.service.AuthService;
import com.example.cloudStorage.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    private User getCurrentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("user");
        if (user == null) {
            throw new UnauthorizedException("Пользователь не авторизован");
        }
        return user;
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            fileService.uploadFile(user, filename, file);
            return ResponseEntity.ok().build();
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка при сохранении файла", 500));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            fileService.deleteFile(user, filename);
            return ResponseEntity.ok().build();
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), 404));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка при удалении файла", 500));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Resource resource = fileService.downloadFile(user, filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), 404));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка при скачивании файла", 500));
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestBody RenameRequest renameRequest,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            fileService.renameFile(user, filename, renameRequest.getName());
            return ResponseEntity.ok().build();
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), 404));
        } catch (FileAlreadyExistsException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка при переименовании", 500));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {
        User user = getCurrentUser(request);
        List<FileEntity> files = fileService.getFiles(user, limit);
        List<FileDto> dtos = files.stream()
                .map(f -> new FileDto(f.getFilename(), f.getSize()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}