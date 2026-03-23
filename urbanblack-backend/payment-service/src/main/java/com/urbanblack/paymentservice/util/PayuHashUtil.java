package com.urbanblack.paymentservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PayuHashUtil {
    private PayuHashUtil() {}

    public static String sha512Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute SHA-512", e);
        }
    }
}

