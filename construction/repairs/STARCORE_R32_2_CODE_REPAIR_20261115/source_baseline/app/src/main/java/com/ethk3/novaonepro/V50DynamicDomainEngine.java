package com.ethk3.novaonepro;

/**
 * Deterministic dynamic-domain identity; statistics and promotion live in the certificate engine.
 */
public final class V50DynamicDomainEngine {
    public static final String MODULE_VERSION = "V50_DYNAMIC_DOMAIN_50072401";

    private V50DynamicDomainEngine() {}

    public static String domainId(
            String prototype,
            V50Enums.Lifecycle lifecycle,
            int position,
            double hazard,
            double confidence) {
        String positionBand = position <= 3 ? "FRONT" : (position <= 7 ? "MIDDLE" : "BACK");
        String hazardBand = hazard >= 0.70d ? "HZ_HIGH" : (hazard >= 0.42d ? "HZ_MID" : "HZ_LOW");
        String confidenceBand =
                confidence >= 0.70d ? "CF_HIGH" : (confidence >= 0.45d ? "CF_MID" : "CF_LOW");
        String raw =
                safe(prototype)
                        + '|'
                        + (lifecycle == null ? V50Enums.Lifecycle.UNKNOWN.name() : lifecycle.name())
                        + '|'
                        + positionBand
                        + '|'
                        + hazardBand
                        + '|'
                        + confidenceBand;
        return "D50_" + V50ChecksumEngine.sha256(raw).substring(0, 16).toUpperCase(java.util.Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.length() == 0 ? "UNKNOWN" : value;
    }
}
