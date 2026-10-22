package com.ethk3.novaonepro;

/** GENERATED from release/release_manifest.json by scripts/r926_release_manifest.py. */
public final class ReleaseManifestR926 {
    public static final String MANIFEST_SHA256 = "a47985154d718ef91c691648089f72db069826074f2fd69988259581a4817de8";
    public static final String APPLICATION_ID = "com.ethk3.novaonepro";
    public static final String VERSION_NAME = "V57.21.2.0.95-R5.4.2.95-1021-C024";
    public static final int VERSION_CODE = 577251054;
    public static final int COMPILE_SDK = 36;
    public static final int MIN_SDK = 21;
    public static final int TARGET_SDK = 28;
    public static final String DATABASE_NAME = "binance_k3_nova_one_pro_v57.db";
    public static final int DB_USER_VERSION = 36;
    public static final int LEGACY_R811_REVISION = 1;
    public static final int LEGACY_R815_REVISION = 3;
    public static final int LEGACY_R906_REVISION = 4;
    public static final String R925_REVISION = "R925_SCHEMA_7";
    public static final int CRITICAL_DOMAIN_REVISION = 5;
    public static final int RESEARCH_DOMAIN_REVISION = 16;
    public static final int LEGACY_DOMAIN_REVISION = 1;
    public static final int EVENT_LEDGER_REVISION = 1;
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String RELEASE_MODE = "CONTROLLED_CANDIDATE_MODEL_NOT_PROVEN";
    public static final String DECISION_OWNER = "DecisionGateEngine_ONLY";
    public static final String SETTLEMENT_OWNER = "SettlementEngine_ONLY";
    private ReleaseManifestR926() {}
    public static String schemaIdentity() {
        return "user=" + DB_USER_VERSION + "|r811=" + LEGACY_R811_REVISION
                + "|r815=" + LEGACY_R815_REVISION + "|r906=" + LEGACY_R906_REVISION
                + "|r925=" + R925_REVISION + "|critical=" + CRITICAL_DOMAIN_REVISION
                + "|research=" + RESEARCH_DOMAIN_REVISION + "|legacy=" + LEGACY_DOMAIN_REVISION
                + "|event=" + EVENT_LEDGER_REVISION;
    }
}
