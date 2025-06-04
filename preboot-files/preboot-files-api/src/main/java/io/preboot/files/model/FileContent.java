package io.preboot.files.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public record FileContent(FileMetadata metadata, InputStream contentStream) implements AutoCloseable {

    public byte[] toByteArray() throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            contentStream.transferTo(baos);
            return baos.toByteArray();
        }
    }

    @Override
    public void close() throws Exception {
        if (contentStream != null) {
            contentStream.close();
        }
    }
}
