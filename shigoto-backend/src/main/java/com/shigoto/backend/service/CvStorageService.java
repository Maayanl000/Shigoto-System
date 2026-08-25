package com.shigoto.backend.service;

import com.shigoto.backend.exception.CvStorageException;
import com.shigoto.backend.exception.CvTooLargeException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CvStorageService {
    static final long MAX_CV_SIZE = 5L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final Pattern STORAGE_KEY_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.pdf$",
            Pattern.CASE_INSENSITIVE
    );

    private final Path storageRoot;

    public CvStorageService(@Value("${shigoto.storage.cv-directory}") String storageDirectory) {
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new CvStorageException("CV storage is unavailable", ex);
        }
    }

    public String store(MultipartFile file) {
        validate(file);
        String storageKey = UUID.randomUUID() + ".pdf";
        Path destination = resolveStorageKey(storageKey);
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(storageRoot, "cv-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, destination);
            }
            return storageKey;
        } catch (IOException ex) {
            throw new CvStorageException("CV could not be stored", ex);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // The primary storage result is more useful than a temporary-file cleanup failure.
                }
            }
        }
    }

    public Resource load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResourceNotFoundException("CV not found");
        }
        Path storedFile = resolveStorageKey(storageKey);
        if (!Files.isRegularFile(storedFile) || !Files.isReadable(storedFile)) {
            throw new ResourceNotFoundException("CV file not found");
        }
        return new FileSystemResource(storedFile);
    }

    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        Path storedFile = resolveStorageKey(storageKey);
        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException ex) {
            throw new CvStorageException("CV could not be deleted", ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null) throw new IllegalArgumentException("CV file is required");
        if (file.isEmpty() || file.getSize() == 0) throw new IllegalArgumentException("CV file must not be empty");
        if (file.getSize() > MAX_CV_SIZE) throw new CvTooLargeException("CV file must not exceed 5 MB");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("CV filename must end with .pdf");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("CV must have application/pdf content type");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(PDF_SIGNATURE.length);
            if (!java.util.Arrays.equals(signature, PDF_SIGNATURE)) {
                throw new IllegalArgumentException("CV is not a valid PDF file");
            }
        } catch (IOException ex) {
            throw new CvStorageException("CV could not be read", ex);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (!STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new ResourceNotFoundException("CV file not found");
        }
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new ResourceNotFoundException("CV file not found");
        }
        return resolved;
    }
}
