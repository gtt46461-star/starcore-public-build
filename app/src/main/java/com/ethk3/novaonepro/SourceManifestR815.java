package com.ethk3.novaonepro;

/**
 * Deterministic source/resource/toolchain provenance generated before compilation by
 * {@code scripts/r815_generate_provenance.sh}.
 *
 * <p>APK and DEX digests are deliberately not embedded as expected values in this class: an APK
 * cannot contain a truthful hash of its own final bytes without creating a self-reference. The
 * release script writes those final artifact digests to the detached, delivery-side
 * {@code RELEASE_MANIFEST.json}; {@link BuildProvenanceR815} fingerprints the installed APK and
 * all DEX entries at runtime. This keeps the provenance claim mathematically sound.</p>
 */
public final class SourceManifestR815 {
    // Do not export compile-time constants through a second class: incremental
    // compilation may otherwise retain yesterday's inlined values in report consumers.
    // The generated identity still supplies every byte; all consumers read this build's fields.
    public static final String SOURCE_TREE_SHA256 = generated(BuildSourceIdentityCP12.SOURCE_TREE_SHA256);
    public static final String RESOURCE_MANIFEST_SHA256 = generated(BuildSourceIdentityCP12.RESOURCE_MANIFEST_SHA256);
    public static final String TOOLCHAIN_SHA256 = generated(BuildSourceIdentityCP12.TOOLCHAIN_SHA256);
    public static final String GENERATED_AT_UTC = generated(BuildSourceIdentityCP12.GENERATED_AT_UTC);

    private static String generated(String value) { return value; }

    private SourceManifestR815() {}

    public static boolean valid() {
        return hash(SOURCE_TREE_SHA256)
                && hash(RESOURCE_MANIFEST_SHA256)
                && hash(TOOLCHAIN_SHA256)
                && !allZero(SOURCE_TREE_SHA256)
                && !allZero(RESOURCE_MANIFEST_SHA256)
                && !allZero(TOOLCHAIN_SHA256)
                && GENERATED_AT_UTC.length() >= 20
                && GENERATED_AT_UTC.endsWith("Z");
    }

    public static String canonical() {
        return SOURCE_TREE_SHA256
                + '|'
                + RESOURCE_MANIFEST_SHA256
                + '|'
                + TOOLCHAIN_SHA256
                + '|'
                + GENERATED_AT_UTC
                + "|artifact-fingerprints=runtime|detached-release-manifest=true";
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static boolean allZero(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) != '0') return false;
        return true;
    }
}
