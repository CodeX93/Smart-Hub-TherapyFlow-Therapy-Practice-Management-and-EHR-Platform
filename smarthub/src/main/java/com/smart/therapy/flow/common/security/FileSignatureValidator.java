package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Rejects uploads whose bytes do not match their declared media type.
 *
 * This is a file-type control, not a replacement for asynchronous malware
 * scanning. Unknown binary formats fail closed.
 */
public final class FileSignatureValidator {
    private static final int HEADER_BYTES = 32;
    private static final int TEXT_SAMPLE_BYTES = 8192;
    private static final int MAX_ZIP_ENTRIES_TO_INSPECT = 256;

    private static final Set<String> TEXT_TYPES = Set.of(
            "text/plain",
            "text/csv",
            "application/json",
            "application/xml",
            "text/xml"
    );

    private FileSignatureValidator() {
    }

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = normalizeContentType(file.getContentType());
        try {
            byte[] header = readPrefix(file, HEADER_BYTES);
            boolean valid = switch (contentType) {
                case "application/pdf" -> startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII));
                case "image/jpeg", "image/jpg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "image/gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                        || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
                case "image/webp" -> matchesRiffType(header, "WEBP");
                case "audio/wav", "audio/wave", "audio/x-wav" -> matchesRiffType(header, "WAVE");
                case "audio/mpeg", "audio/mp3" -> startsWith(header, "ID3".getBytes(StandardCharsets.US_ASCII))
                        || (header.length >= 2
                        && unsigned(header[0]) == 0xFF
                        && (unsigned(header[1]) & 0xE0) == 0xE0);
                case "audio/ogg", "application/ogg" ->
                        startsWith(header, "OggS".getBytes(StandardCharsets.US_ASCII));
                case "audio/mp4", "audio/x-m4a", "video/mp4" -> hasIsoBaseMediaSignature(header);
                case "video/webm", "audio/webm" -> startsWith(header, 0x1A, 0x45, 0xDF, 0xA3);
                case "application/msword", "application/vnd.ms-excel",
                        "application/vnd.ms-powerpoint" -> startsWith(
                        header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        isOfficeArchive(file, "word/");
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel.sheet.macroenabled.12" ->
                        isOfficeArchive(file, "xl/");
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                        isOfficeArchive(file, "ppt/");
                case "application/zip" -> isZip(header);
                case "application/octet-stream" -> hasKnownBinarySignature(header);
                default -> TEXT_TYPES.contains(contentType) && isSafeText(file);
            };

            if (!valid) {
                throw new BadRequestException("File content does not match an allowed file type");
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("File content could not be validated");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        int parameter = contentType.indexOf(';');
        String normalized = parameter >= 0 ? contentType.substring(0, parameter) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] readPrefix(MultipartFile file, int limit) throws IOException {
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            return input.readNBytes(limit);
        }
    }

    private static boolean isSafeText(MultipartFile file) throws IOException {
        byte[] sample = readPrefix(file, TEXT_SAMPLE_BYTES);
        if (sample.length == 0) {
            return false;
        }
        for (byte value : sample) {
            int unsigned = unsigned(value);
            if (unsigned == 0 || (unsigned < 0x09) || (unsigned > 0x0D && unsigned < 0x20)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOfficeArchive(MultipartFile file, String requiredPrefix) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(file.getInputStream()))) {
            boolean hasContentTypes = false;
            boolean hasRequiredDirectory = false;
            ZipEntry entry;
            int inspected = 0;
            while ((entry = zip.getNextEntry()) != null && inspected++ < MAX_ZIP_ENTRIES_TO_INSPECT) {
                String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                if ("[content_types].xml".equals(name)) {
                    hasContentTypes = true;
                }
                if (name.startsWith(requiredPrefix)) {
                    hasRequiredDirectory = true;
                }
                if (hasContentTypes && hasRequiredDirectory) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean hasKnownBinarySignature(byte[] header) {
        return startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII))
                || startsWith(header, 0xFF, 0xD8, 0xFF)
                || startsWith(header, 0x89, 0x50, 0x4E, 0x47)
                || startsWith(header, "GIF8".getBytes(StandardCharsets.US_ASCII))
                || isZip(header)
                || startsWith(header, 0xD0, 0xCF, 0x11, 0xE0)
                || startsWith(header, "ID3".getBytes(StandardCharsets.US_ASCII))
                || startsWith(header, "OggS".getBytes(StandardCharsets.US_ASCII))
                || startsWith(header, 0x1A, 0x45, 0xDF, 0xA3)
                || matchesRiffType(header, "WEBP")
                || matchesRiffType(header, "WAVE")
                || hasIsoBaseMediaSignature(header);
    }

    private static boolean isZip(byte[] header) {
        return startsWith(header, 0x50, 0x4B, 0x03, 0x04)
                || startsWith(header, 0x50, 0x4B, 0x05, 0x06)
                || startsWith(header, 0x50, 0x4B, 0x07, 0x08);
    }

    private static boolean matchesRiffType(byte[] header, String type) {
        return header.length >= 12
                && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(
                Arrays.copyOfRange(header, 8, 12),
                type.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean hasIsoBaseMediaSignature(byte[] header) {
        return header.length >= 12
                && header[4] == 'f'
                && header[5] == 't'
                && header[6] == 'y'
                && header[7] == 'p';
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] value, int... prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (unsigned(value[index]) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }
}
