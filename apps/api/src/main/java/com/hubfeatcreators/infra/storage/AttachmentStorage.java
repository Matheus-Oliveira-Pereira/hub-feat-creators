package com.hubfeatcreators.infra.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface AttachmentStorage {

    record StoredFile(String storagePath, long sizeBytes) {}

    StoredFile store(String assessoriaId, String filename, String contentType, InputStream data, long contentLength);

    InputStream load(String storagePath);

    void delete(String storagePath);
}
