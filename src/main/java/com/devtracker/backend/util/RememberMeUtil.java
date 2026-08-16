package com.devtracker.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class RememberMeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RememberMeUtil() {
    }


    // Generate a secure random token
    public static String generateToken() {

        byte[] bytes = new byte[32];

        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }


    // SHA-256 hash
    public static String hashToken(String token) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder sb = new StringBuilder();

            for (byte b : hash) {

                sb.append(
                        String.format("%02x", b)
                );
            }

            return sb.toString();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }


    // Create remember-me cookie
    public static void createCookie(
            HttpServletResponse response,
            String token) {

        Cookie cookie =
                new Cookie("remember_me", token);

        cookie.setHttpOnly(true);
        cookie.setPath("/");

        // 30 days
        cookie.setMaxAge(
                60 * 60 * 24 * 30
        );

        response.addCookie(cookie);
    }


    // Delete remember-me cookie
    public static void deleteCookie(
            HttpServletResponse response) {

        Cookie cookie =
                new Cookie("remember_me", "");

        cookie.setHttpOnly(true);
        cookie.setPath("/");

        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}