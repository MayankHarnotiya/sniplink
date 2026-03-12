package com.snip.link.service;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HashService {


    private static final String BASE62 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    public String generateShortCode(String originalUrl) {
        // Step 1: Hash the URL using MD5
        String hash = getMD5Hash(originalUrl + System.nanoTime());

        // Step 2: Take first 6 chars and map to Base62
        return toBase62(hash.substring(0, 8));
    }

    private String getMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private String toBase62(String hexString) {
        // Convert hex → BigInteger → Base62 string
        BigInteger number = new BigInteger(hexString, 16);
        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(62);

        for (int i = 0; i < CODE_LENGTH; i++) {
            result.append(BASE62.charAt(number.mod(base).intValue()));
            number = number.divide(base);
        }
        return result.toString();
    }


}
