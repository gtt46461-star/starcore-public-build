package com.ethk3.novaonepro;

/** Exact received body identity. No trim, parsing, normalization or hash overwrite. */
public final class RawHttpIdentityR34 {
    private RawHttpIdentityR34() {}

    public static String payloadSha256(String body) {
        return V51ChecksumEngine.sha256(body == null ? "" : body);
    }

    /** A REQUESTED row has an empty hash; a completed row is immutable in identity. */
    public static void requireNoOverwrite(String persistedHash, String receivedHash) {
        if (receivedHash == null || !receivedHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("R34_HTTP_RECEIVED_HASH_REQUIRED");
        if (persistedHash != null && !persistedHash.isEmpty()
                && !persistedHash.equals(receivedHash))
            throw new IllegalStateException("R34_INTEGRITY_CONFLICT_HTTP_REQUEST_PAYLOAD");
    }
}
