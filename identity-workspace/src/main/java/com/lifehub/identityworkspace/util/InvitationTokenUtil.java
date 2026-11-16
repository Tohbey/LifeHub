package com.lifehub.identityworkspace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Invitation tokens: 256 random bits, URL safe. Only the SHA-256 hash is stored. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvitationTokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a new random, URL-safe invite token (256 bits). */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Returns the SHA-256 hex hash of a token, which is what gets stored and looked up. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is always available", ex);
        }
    }
}
