package com.ethk3.novaonepro;

/** Compatibility facade for old UI field name. */
public final class DecisionFunnelDiagnosisEngine {
    public static final String MODULE_VERSION = V48BuildManifest.DIAGNOSIS_VERSION;

    private DecisionFunnelDiagnosisEngine() {}

    public static String buildAndPersist(DbStore store) {
        return V48DecisionFunnelDiagnosisEngine.buildAndPersist(store);
    }
}
