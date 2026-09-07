package com.shigoto.backend.service;

import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.exception.CvTooLargeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
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
        assertThrows(CvTooLargeException.class,
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

    @Test
    void loadsWhitelistedFixedDemoCvThroughNormalStorageFlow() throws Exception {
        Path demoCv = temporaryDirectory.resolve("demo-eren-yeager-cv.pdf");
        Files.writeString(demoCv, "%PDF-demo");

        assertArrayEquals("%PDF-demo".getBytes(),
                service().load("demo-eren-yeager-cv.pdf").getContentAsByteArray());
        assertThrows(ResourceNotFoundException.class,
                () -> service().load("demo-unknown-candidate-cv.pdf"));
    }

    @Test
    void deletingDemoReferenceKeepsSharedSeededAsset() throws Exception {
        Path demoCv = temporaryDirectory.resolve("demo-mikasa-ackerman-cv.pdf");
        Files.writeString(demoCv, "%PDF-demo");

        service().delete("demo-mikasa-ackerman-cv.pdf");

        assertTrue(Files.exists(demoCv));
    }

    @Test
    void projectDemoCvsArePresentAndLoadableThroughNormalStorageFlow() throws Exception {
        CvStorageService projectStorage = new CvStorageService(
                Path.of(System.getProperty("user.dir"), "data", "cvs").toString());

        for (String storageKey : new String[]{
                "demo-eren-yeager-cv.pdf",
                "demo-mikasa-ackerman-cv.pdf",
                "demo-armin-arlert-cv.pdf"
        }) {
            byte[] content = projectStorage.load(storageKey).getContentAsByteArray();
            assertTrue(content.length > 5);
            assertArrayEquals("%PDF-".getBytes(), java.util.Arrays.copyOf(content, 5));
        }
    }

    private CvStorageService service() {
        return new CvStorageService(temporaryDirectory.toString());
    }

    private MockMultipartFile pdf(String filename, byte[] content) {
        return new MockMultipartFile("cv", filename, "application/pdf", content);
    }
}
