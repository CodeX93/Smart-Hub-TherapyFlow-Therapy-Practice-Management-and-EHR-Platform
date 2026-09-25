package com.smart.therapy.flow.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class TotpService {
    static final int SECRET_BYTES = 20;
    static final long STEP_SECONDS = 30;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public Long verify(String secret, String suppliedCode, Instant now, int window, Long lastAcceptedStep) {
        if (suppliedCode == null || !suppliedCode.matches("\\d{6}")) {
            return null;
        }
        long currentStep = now.getEpochSecond() / STEP_SECONDS;
        for (long step = currentStep - window; step <= currentStep + window; step++) {
            String expected = generateCode(secret, step, 6);
            if (constantTimeEquals(expected, suppliedCode)
                    && (lastAcceptedStep == null || step > lastAcceptedStep)) {
                return step;
            }
        }
        return null;
    }

    public String generateCode(String base32Secret, long timeStep, int digits) {
        try {
            byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(base32Secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int modulus = (int) Math.pow(10, digits);
            return String.format(Locale.ROOT, "%0" + digits + "d", binary % modulus);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TOTP calculation failed", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                supplied.getBytes(StandardCharsets.US_ASCII));
    }

    static String encodeBase32(byte[] input) {
        StringBuilder out = new StringBuilder((input.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] decodeBase32(String input) {
        String normalized = input.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        byte[] output = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (char c : normalized.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 secret");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                output[index++] = (byte) ((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return output;
    }
}
