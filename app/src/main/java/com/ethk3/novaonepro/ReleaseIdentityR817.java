package com.ethk3.novaonepro;

/**
 * Runtime identity verifier.  Class name is retained for compatibility; R926 values are generated
 * from release/release_manifest.json and every comparison reports expected/actual/source/severity.
 */
public final class ReleaseIdentityR817 {
    public static final String APPLICATION_ID = ReleaseManifestR926.APPLICATION_ID;
    public static final String VERSION_NAME = ReleaseManifestR926.VERSION_NAME;
    public static final int VERSION_CODE = ReleaseManifestR926.VERSION_CODE;
    public static final int COMPILE_SDK = ReleaseManifestR926.COMPILE_SDK;
    public static final int MIN_SDK = ReleaseManifestR926.MIN_SDK;
    public static final int TARGET_SDK = ReleaseManifestR926.TARGET_SDK;
    public static final int DB_VERSION = ReleaseManifestR926.DB_USER_VERSION;
    /** Compatibility name: this is specifically the legacy R906 same-user_version revision. */
    public static final int SCHEMA_REVISION = ReleaseManifestR926.LEGACY_R906_REVISION;
    public static final String GAME_CODE = ReleaseManifestR926.GAME_CODE;
    public static final String RELEASE_MODE = ReleaseManifestR926.RELEASE_MODE;

    private ReleaseIdentityR817() {}

    public static String canonical() {
        return APPLICATION_ID + '|' + VERSION_NAME + '|' + VERSION_CODE + '|' + COMPILE_SDK + '|'
                + MIN_SDK + '|' + TARGET_SDK + '|' + DB_VERSION + '|' + SCHEMA_REVISION + '|'
                + GAME_CODE + '|' + RELEASE_MODE + '|' + ReleaseManifestR926.schemaIdentity() + '|'
                + ReleaseManifestR926.MANIFEST_SHA256;
    }

    public static String sha256() { return V51ChecksumEngine.sha256(canonical()); }
    public static boolean isConsistent() { return mismatchDetail().length() == 0; }

    /** Compact hard-error codes retained for existing callers. */
    public static String mismatchDetail() {
        StringBuilder out = new StringBuilder();
        hard(out, "BUILD_APP_ID", APPLICATION_ID, BuildConfig.APPLICATION_ID);
        hard(out, "BUILD_VERSION_NAME", VERSION_NAME, BuildConfig.VERSION_NAME);
        hard(out, "BUILD_VERSION_CODE", VERSION_CODE, BuildConfig.VERSION_CODE);
        hard(out, "BUILD_MANIFEST_SHA", ReleaseManifestR926.MANIFEST_SHA256, BuildConfig.RELEASE_MANIFEST_SHA256);
        hard(out, "MANIFEST_VERSION_NAME", VERSION_NAME, V51BuildManifest.VERSION_NAME);
        hard(out, "MANIFEST_VERSION_CODE", VERSION_CODE, V51BuildManifest.VERSION_CODE);
        hard(out, "REPAIR_VERSION", VERSION_NAME, AppConfig.R36_RUNTIME_REPAIR_VERSION);
        hard(out, "GAME_MANIFEST", GAME_CODE, V51BuildManifest.GAME_CODE);
        hard(out, "GAME_CONFIG", GAME_CODE, AppConfig.LOTTERY_CODE);
        hard(out, "DB_VERSION", DB_VERSION, AppConfig.DB_VERSION);
        hard(out, "R906_REVISION", SCHEMA_REVISION, AppConfig.R906_SCHEMA_REVISION);
        hard(out, "BUILD_DB_VERSION", DB_VERSION, BuildConfig.DB_USER_VERSION);
        hard(out, "BUILD_R906_REVISION", SCHEMA_REVISION, BuildConfig.LEGACY_R906_SCHEMA_REVISION);
        hard(out, "BUILD_R925_REVISION", ReleaseManifestR926.R925_REVISION, BuildConfig.R925_SCHEMA_REVISION);
        hard(out, "BUILD_CRITICAL_REVISION", ReleaseManifestR926.CRITICAL_DOMAIN_REVISION, BuildConfig.CRITICAL_DOMAIN_REVISION);
        hard(out, "BUILD_RESEARCH_REVISION", ReleaseManifestR926.RESEARCH_DOMAIN_REVISION, BuildConfig.RESEARCH_DOMAIN_REVISION);
        hard(out, "BUILD_LEGACY_REVISION", ReleaseManifestR926.LEGACY_DOMAIN_REVISION, BuildConfig.LEGACY_DOMAIN_REVISION);
        hard(out, "BUILD_EVENT_REVISION", ReleaseManifestR926.EVENT_LEDGER_REVISION, BuildConfig.EVENT_LEDGER_REVISION);
        if (AppConfig.R815_SHADOW_ONLY || AppConfig.R815_MODEL_PROVEN || AppConfig.R815_REAL_BET_HARD_DISABLED)
            append(out, "CONFIG_REAL_BOUNDARY");
        if (V51BuildManifest.MODEL_PROVEN || V51BuildManifest.REALTIME_PROVEN || V51BuildManifest.SHADOW_ONLY
                || V51BuildManifest.REAL_BET_HARD_DISABLED || !V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED)
            append(out, "MANIFEST_CONTROLLED_BOUNDARY");
        return out.toString();
    }

    /** Human/machine readable field-by-field evidence required by B01. */
    public static String fieldReport() {
        StringBuilder b = new StringBuilder(2048);
        row(b,"applicationId",APPLICATION_ID,BuildConfig.APPLICATION_ID,"release_manifest.json->BuildConfig","HARD","R926_ID_APP");
        row(b,"versionName",VERSION_NAME,BuildConfig.VERSION_NAME,"release_manifest.json->BuildConfig","HARD","R926_ID_VERSION_NAME");
        row(b,"versionCode",VERSION_CODE,BuildConfig.VERSION_CODE,"release_manifest.json->BuildConfig","HARD","R926_ID_VERSION_CODE");
        row(b,"manifestSha256",ReleaseManifestR926.MANIFEST_SHA256,BuildConfig.RELEASE_MANIFEST_SHA256,"release_manifest.json(raw SHA-256)","HARD","R926_ID_MANIFEST_SHA");
        row(b,"dbUserVersion",DB_VERSION,AppConfig.DB_VERSION,"release_manifest.json->AppConfig","HARD","R926_ID_DB_VERSION");
        row(b,"legacyR906Revision",SCHEMA_REVISION,AppConfig.R906_SCHEMA_REVISION,"release_manifest.json->AppConfig","HARD","R926_ID_R906_REVISION");
        row(b,"r925Revision",ReleaseManifestR926.R925_REVISION,SchemaRevisionR925.REVISION,"release_manifest.json->SchemaRevisionR925","HARD","R926_ID_R925_REVISION");
        row(b,"criticalDomainRevision",ReleaseManifestR926.CRITICAL_DOMAIN_REVISION,BuildConfig.CRITICAL_DOMAIN_REVISION,"release_manifest.json->BuildConfig","HARD","R926_ID_CRITICAL_REVISION");
        row(b,"researchDomainRevision",ReleaseManifestR926.RESEARCH_DOMAIN_REVISION,BuildConfig.RESEARCH_DOMAIN_REVISION,"release_manifest.json->BuildConfig","HARD","R926_ID_RESEARCH_REVISION");
        row(b,"legacyDomainRevision",ReleaseManifestR926.LEGACY_DOMAIN_REVISION,BuildConfig.LEGACY_DOMAIN_REVISION,"release_manifest.json->BuildConfig","HARD","R926_ID_LEGACY_REVISION");
        row(b,"eventLedgerRevision",ReleaseManifestR926.EVENT_LEDGER_REVISION,BuildConfig.EVENT_LEDGER_REVISION,"release_manifest.json->BuildConfig","HARD","R926_ID_EVENT_REVISION");
        row(b,"gameCode",GAME_CODE,AppConfig.LOTTERY_CODE,"release_manifest.json->AppConfig","HARD","R926_ID_GAME");
        return b.toString();
    }

    private static void row(StringBuilder b,String field,Object expected,Object actual,String source,String severity,String code) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (b.length()>0) b.append('\n');
        b.append("FIELD=").append(field).append("|EXPECTED=").append(String.valueOf(expected))
                .append("|ACTUAL=").append(String.valueOf(actual)).append("|SOURCE=").append(source)
                .append("|SEVERITY=").append(severity).append("|CODE=").append(ok?"OK":code);
    }
    private static void hard(StringBuilder out,String name,Object expected,Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) append(out,name);
    }
    private static void hard(StringBuilder out,String name,int expected,int actual) { if (expected!=actual) append(out,name); }
    private static void append(StringBuilder out,String name) { if(out.length()>0)out.append(','); out.append(name); }
    public static void assertBuildConfig() {
        String detail=mismatchDetail();
        if(detail.length()>0) throw new IllegalStateException("R926_BUILD_IDENTITY_MISMATCH|"+detail);
    }
}
