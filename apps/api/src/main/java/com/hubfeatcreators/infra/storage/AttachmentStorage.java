package com.hubfeatcreators.infra.storage;

import java.io.InputStream;

public interface AttachmentStorage {

    record StoredFile(String storagePath, long sizeBytes) {}

    StoredFile store(
            String bucket,
            String filename,
            String contentType,
            InputStream data,
            long contentLength);

    InputStream load(String storagePath);

    void delete(String storagePath);
}
