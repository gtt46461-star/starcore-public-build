package com.ethk3.novaonepro;

/** Source-compatible R816 facade; R817 is the sole current release identity. */
public final class ReleaseIdentityR816 {
    public static final String APPLICATION_ID = ReleaseIdentityR817.APPLICATION_ID;
    public static final String VERSION_NAME = ReleaseIdentityR817.VERSION_NAME;
    public static final int VERSION_CODE = ReleaseIdentityR817.VERSION_CODE;
    public static final int COMPILE_SDK = ReleaseIdentityR817.COMPILE_SDK;
    public static final int MIN_SDK = ReleaseIdentityR817.MIN_SDK;
    public static final int TARGET_SDK = ReleaseIdentityR817.TARGET_SDK;
    public static final int DB_VERSION = ReleaseIdentityR817.DB_VERSION;
    public static final int SCHEMA_REVISION = ReleaseIdentityR817.SCHEMA_REVISION;
    public static final String GAME_CODE = ReleaseIdentityR817.GAME_CODE;
    public static final String RELEASE_MODE = ReleaseIdentityR817.RELEASE_MODE;

    private ReleaseIdentityR816() {}

    public static String canonical() {
        return ReleaseIdentityR817.canonical();
    }

    public static String sha256() {
        return ReleaseIdentityR817.sha256();
    }

    public static boolean isConsistent() {
        return ReleaseIdentityR817.isConsistent();
    }

    public static void assertBuildConfig() {
        ReleaseIdentityR817.assertBuildConfig();
    }
}
