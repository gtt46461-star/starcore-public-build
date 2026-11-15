package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Length-delimited identity fields; callers must include their protocol revision. */
public final class HashingSU1 {
    private HashingSU1() {}
    public static String bytes(byte[] data) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder out=new StringBuilder(64);
            for(byte v:hash) { out.append("0123456789abcdef".charAt((v&255)>>>4)); out.append("0123456789abcdef".charAt(v&15)); }
            return out.toString();
        } catch(java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public static String text(String value) { return bytes(value.getBytes(StandardCharsets.UTF_8)); }
    public static String fields(String... fields) {
        StringBuilder out=new StringBuilder();
        for(String field:fields) { if(field==null)throw new IllegalArgumentException("SU1_NULL_IDENTITY"); out.append(field.getBytes(StandardCharsets.UTF_8).length).append(':').append(field); }
        return text(out.toString());
    }
    public static String sha(String value) { if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("SU1_SHA256");return value; }
}
