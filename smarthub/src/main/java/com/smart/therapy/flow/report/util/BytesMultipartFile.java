package com.smart.therapy.flow.report.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** In-memory MultipartFile for base64 uploads routed through StorageService. */
public class BytesMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public BytesMultipartFile(byte[] content, String originalFilename, String contentType) {
        this.content = content != null ? content : new byte[0];
        this.name = originalFilename != null ? originalFilename : "file";
        this.originalFilename = this.name;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        throw new UnsupportedOperationException("transferTo not supported");
    }
}
