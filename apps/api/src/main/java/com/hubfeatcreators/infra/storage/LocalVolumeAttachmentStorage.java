package com.hubfeatcreators.infra.storage;

import com.hubfeatcreators.infra.web.BusinessException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalVolumeAttachmentStorage implements AttachmentStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalVolumeAttachmentStorage.class);

    private final Path root;

    public LocalVolumeAttachmentStorage(
            @Value("${app.storage.upload-dir:${java.io.tmpdir}/hub-attachments}")
                    String uploadDir) {
        this.root = Paths.get(uploadDir);
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            log.warn("storage.init.failed dir={}", uploadDir, e);
        }
    }

    @Override
    public StoredFile store(
            String bucket,
            String filename,
            String contentType,
            InputStream data,
            long contentLength) {
        LocalDate today = LocalDate.now();
        String safeFilename = UUID.randomUUID() + "-" + sanitize(filename);
        Path dir =
                root.resolve(bucket)
                        .resolve(String.valueOf(today.getYear()))
                        .resolve(String.format("%02d", today.getMonthValue()));
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(safeFilename);
            long written = Files.copy(data, dest, StandardCopyOption.REPLACE_EXISTING);
            String storagePath = root.relativize(dest).toString().replace('\\', '/');
            log.info("storage.stored path={} size={}", storagePath, written);
            return new StoredFile(storagePath, written);
        } catch (IOException e) {
            throw BusinessException.internalError(
                    "STORAGE_WRITE_FAILED", "Falha ao gravar arquivo.");
        }
    }

    @Override
    public InputStream load(String storagePath) {
        Path file = root.resolve(storagePath);
        if (!file.startsWith(root)) {
            throw BusinessException.badRequest("STORAGE_TRAVERSAL", "Caminho inválido.");
        }
        try {
            return new FileInputStream(file.toFile());
        } catch (IOException e) {
            throw BusinessException.notFound();
        }
    }

    @Override
    public void delete(String storagePath) {
        Path file = root.resolve(storagePath);
        if (!file.startsWith(root)) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("storage.delete.failed path={}", storagePath, e);
        }
    }

    private String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
