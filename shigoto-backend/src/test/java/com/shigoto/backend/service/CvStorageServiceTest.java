package com.shigoto.backend.service;

import com.shigoto.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CvStorageServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndLoadsValidPdf() throws Exception {
        CvStorageService service = service();

        String key = service.store(pdf("resume.pdf", "%PDF-1.4\ncontent".getBytes()));

        assertTrue(key.matches("[0-9a-f-]{36}\\.pdf"));
        assertArrayEquals("%PDF-1.4\ncontent".getBytes(), service.load(key).getContentAsByteArray());
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(IllegalArgumentException.class,
                () -> service().store(pdf("resume.pdf", new byte[0])));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
        System.arraycopy("%PDF-".getBytes(), 0, oversized, 0, 5);
        assertThrows(IllegalArgumentException.class,
                () -> service().store(pdf("resume.pdf", oversized)));
    }

    @Test
    void rejectsWrongExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "cv", "resume.txt", "application/pdf", "%PDF-test".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service().store(file));
    }

    @Test
    void rejectsWrongMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "cv", "resume.pdf", "text/plain", "%PDF-test".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service().store(file));
    }

    @Test
    void rejectsInvalidPdfSignature() {
        assertThrows(IllegalArgumentException.class,
                () -> service().store(pdf("resume.pdf", "not a pdf".getBytes())));
    }

    @Test
    void createsUniqueStorageKeys() {
        CvStorageService service = service();
        String first = service.store(pdf("same.pdf", "%PDF-first".getBytes()));
        String second = service.store(pdf("same.pdf", "%PDF-second".getBytes()));

        assertNotEquals(first, second);
        assertTrue(service.load(first).exists());
        assertTrue(service.load(second).exists());
    }

    @Test
    void missingStoredFileReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service().load(
                "123e4567-e89b-12d3-a456-426614174000.pdf"));
    }

    private CvStorageService service() {
        return new CvStorageService(temporaryDirectory.toString());
    }

    private MockMultipartFile pdf(String filename, byte[] content) {
        return new MockMultipartFile("cv", filename, "application/pdf", content);
    }
}
