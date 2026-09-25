package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.document.service.LocalStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalStorageService Unit Tests")
class LocalStorageServiceTest {

    @InjectMocks
    private LocalStorageService localStorageService;

    @TempDir
    Path tempDir;

    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(localStorageService, "storageBasePath", tempDir.toString());
        testFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test file content".getBytes()
        );
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test-document.pdf";

        // Act
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Assert
        assertThat(storagePath).isNotNull();
        assertThat(storagePath).contains("clients");
        assertThat(storagePath).contains(clientId);
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test-document.pdf";
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Act
        InputStream inputStream = localStorageService.downloadFile(storagePath);

        // Assert
        assertThat(inputStream).isNotNull();
        inputStream.close();
    }

    @Test
    @DisplayName("Should throw exception when downloading non-existent file")
    void shouldThrowExceptionWhenDownloadingNonExistentFile() {
        // Arrange
        String nonExistentPath = tempDir.resolve("non-existent-file.pdf").toString();

        // Act & Assert
        assertThatThrownBy(() -> localStorageService.downloadFile(nonExistentPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test-document.pdf";
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Act
        localStorageService.deleteFile(storagePath);

        // Assert
        assertThat(localStorageService.fileExists(storagePath)).isFalse();
    }

    @Test
    @DisplayName("Should check file exists correctly")
    void shouldCheckFileExistsCorrectly() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test-document.pdf";
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Act & Assert
        assertThat(localStorageService.fileExists(storagePath)).isTrue();
        assertThat(localStorageService.fileExists("non-existent-path")).isFalse();
    }

    @Test
    @DisplayName("Should get file URL successfully")
    void shouldGetFileUrlSuccessfully() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test-document.pdf";
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Act
        String fileUrl = localStorageService.getFileUrl(storagePath);

        // Assert
        assertThat(fileUrl).isNotNull();
        assertThat(fileUrl).contains("/api/documents/file/");
    }

    @Test
    @DisplayName("Should sanitize file names with special characters")
    void shouldSanitizeFileNamesWithSpecialCharacters() throws Exception {
        // Arrange
        String clientId = "123";
        String fileName = "test@file#name$with%special&chars.pdf";

        // Act
        String storagePath = localStorageService.uploadFile(testFile, clientId, fileName);

        // Assert
        assertThat(storagePath).isNotNull();
        assertThat(storagePath).doesNotContain("@");
        assertThat(storagePath).doesNotContain("#");
    }
}

