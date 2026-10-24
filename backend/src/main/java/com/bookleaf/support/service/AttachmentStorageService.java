package com.bookleaf.support.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentStorageService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "csv");

    private final Path directory;

    public AttachmentStorageService(@Value("${bookleaf.attachments.directory:./uploads}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public void validate(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Each attachment must be between 1 byte and 10 MB.");
        }

        String originalName = file.getOriginalFilename();
        String extension = extension(originalName);
        String contentType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !isAllowedContentType(extension, contentType)) {
            throw new IllegalArgumentException("Unsupported attachment type. Use an image, PDF, Word, Excel, or CSV file.");
        }
    }

    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(directory);
        String key = UUID.randomUUID().toString();
        Path destination = directory.resolve(key).normalize();
        if (!destination.getParent().equals(directory)) {
            throw new IllegalArgumentException("Invalid attachment name.");
        }
        try (var input = file.getInputStream()) {
            Files.copy(input, destination);
        }
        return key;
    }

    public Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid attachment key.");
        }
        return directory.resolve(key).normalize();
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean isAllowedContentType(String extension, String contentType) {
        if (contentType == null) return false;
        String type = contentType.toLowerCase(java.util.Locale.ROOT);
        if (Set.of("png", "jpg", "jpeg", "gif", "webp").contains(extension)) {
            return type.startsWith("image/");
        }
        return switch (extension) {
            case "pdf" -> type.equals("application/pdf");
            case "doc" -> type.equals("application/msword") || type.equals("application/octet-stream");
            case "docx" -> type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    || type.equals("application/octet-stream");
            case "xls" -> type.equals("application/vnd.ms-excel") || type.equals("application/octet-stream");
            case "xlsx" -> type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || type.equals("application/octet-stream");
            case "csv" -> type.equals("text/csv") || type.equals("application/vnd.ms-excel")
                    || type.equals("application/octet-stream");
            default -> false;
        };
    }
}
