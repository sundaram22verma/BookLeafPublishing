package com.bookleaf.support.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttachmentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesSupportedFilesWithGeneratedStorageKeys() throws Exception {
        AttachmentStorageService storage = new AttachmentStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "files", "../../report.pdf", "application/pdf", "report".getBytes());

        storage.validate(file);
        String key = storage.store(file);

        assertEquals("report", Files.readString(storage.resolve(key)));
        assertEquals(36, key.length());
    }

    @Test
    void rejectsUnsupportedExtensionsAndMimeTypes() {
        AttachmentStorageService storage = new AttachmentStorageService(tempDir.toString());
        MockMultipartFile executable = new MockMultipartFile(
                "files", "payload.exe", "application/octet-stream", new byte[]{1});
        MockMultipartFile mismatched = new MockMultipartFile(
                "files", "image.png", "text/html", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> storage.validate(executable));
        assertThrows(IllegalArgumentException.class, () -> storage.validate(mismatched));
    }
}
