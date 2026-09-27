package com.example.cloudStorage;

import com.example.cloudStorage.entity.FileEntity;
import com.example.cloudStorage.entity.User;
import com.example.cloudStorage.exception.FileAlreadyExistsException;
import com.example.cloudStorage.repository.FileEntityRepository;
import com.example.cloudStorage.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileEntityRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cloud-test");
        ReflectionTestUtils.setField(fileService, "storagePath", tempDir.toString());
    }

    private User createUser() {
        return User.builder().id(1L).login("user").password("pass").build();
    }

    @Test
    void uploadFile_Success() throws IOException {
        User user = createUser();
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        when(fileRepository.findByUserAndFilename(user, "test.txt")).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        fileService.uploadFile(user, "test.txt", file);

        verify(fileRepository).save(any(FileEntity.class));
        Path expectedPath = tempDir.resolve(user.getId().toString()).resolve("test.txt");
        assertTrue(Files.exists(expectedPath));
        // Очистка
        Files.deleteIfExists(expectedPath);
        Files.deleteIfExists(tempDir.resolve(user.getId().toString()));
    }

    @Test
    void uploadFile_DuplicateName_ThrowsException() throws IOException {
        User user = createUser();
        MultipartFile file = new MockMultipartFile("file", "dup.txt", "text/plain", "data".getBytes());
        when(fileRepository.findByUserAndFilename(user, "dup.txt"))
                .thenReturn(Optional.of(new FileEntity()));

        assertThrows(FileAlreadyExistsException.class, () ->
                fileService.uploadFile(user, "dup.txt", file));

        // Файл не должен остаться
        Path filePath = tempDir.resolve(user.getId().toString()).resolve("dup.txt");
        assertFalse(Files.exists(filePath));
        Files.deleteIfExists(filePath.getParent());
    }

    @Test
    void deleteFile_Success() throws IOException {
        User user = createUser();
        FileEntity entity = FileEntity.builder()
                .id(10L).filename("del.txt").filePath(tempDir.resolve("del.txt").toString())
                .user(user).build();
        Files.write(tempDir.resolve("del.txt"), "content".getBytes());

        when(fileRepository.findByUserAndFilename(user, "del.txt")).thenReturn(Optional.of(entity));
        doNothing().when(fileRepository).delete(entity);

        fileService.deleteFile(user, "del.txt");

        verify(fileRepository).delete(entity);
        assertFalse(Files.exists(tempDir.resolve("del.txt")));
    }

    @Test
    void deleteFile_NotFound_Throws() {
        User user = createUser();
        when(fileRepository.findByUserAndFilename(user, "nofile.txt")).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class, () ->
                fileService.deleteFile(user, "nofile.txt"));
    }

    @Test
    void renameFile_Success() throws IOException {
        User user = createUser();
        Path oldPath = tempDir.resolve("old.txt");
        Files.write(oldPath, "data".getBytes());
        FileEntity entity = FileEntity.builder()
                .id(1L).filename("old.txt").filePath(oldPath.toString())
                .user(user).build();

        when(fileRepository.findByUserAndFilename(user, "old.txt")).thenReturn(Optional.of(entity));
        when(fileRepository.findByUserAndFilename(user, "new.txt")).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        fileService.renameFile(user, "old.txt", "new.txt");

        verify(fileRepository).save(any(FileEntity.class));
        assertFalse(Files.exists(oldPath));
        assertTrue(Files.exists(tempDir.resolve("new.txt")));
        Files.deleteIfExists(tempDir.resolve("new.txt"));
    }

    @Test
    void getFiles_WithLimit() {
        User user = createUser();
        List<FileEntity> all = List.of(
                FileEntity.builder().filename("a.txt").size(1L).build(),
                FileEntity.builder().filename("b.txt").size(2L).build(),
                FileEntity.builder().filename("c.txt").size(3L).build()
        );
        when(fileRepository.findAllByUser(user)).thenReturn(all);

        List<FileEntity> result = fileService.getFiles(user, 2);
        assertEquals(2, result.size());
    }

    @Test
    void downloadFile_Success() throws IOException {
        User user = createUser();
        Path filePath = tempDir.resolve("down.txt");
        Files.write(filePath, "content".getBytes());
        FileEntity entity = FileEntity.builder()
                .filename("down.txt").filePath(filePath.toString()).user(user).build();

        when(fileRepository.findByUserAndFilename(user, "down.txt")).thenReturn(Optional.of(entity));

        Resource resource = fileService.downloadFile(user, "down.txt");
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void downloadFile_NotFound_Throws() {
        User user = createUser();
        when(fileRepository.findByUserAndFilename(user, "ghost.txt")).thenReturn(Optional.empty());
        assertThrows(FileNotFoundException.class, () ->
                fileService.downloadFile(user, "ghost.txt"));
    }
}