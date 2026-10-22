package com.ethk3.novaonepro;

/** Binary/source compatibility facade; R817 is the sole active release identity. */
public final class ReleaseIdentityR815 {
    public static final String APPLICATION_ID = ReleaseIdentityR816.APPLICATION_ID;
    public static final String VERSION_NAME = ReleaseIdentityR816.VERSION_NAME;
    public static final int VERSION_CODE = ReleaseIdentityR816.VERSION_CODE;
    public static final int COMPILE_SDK = ReleaseIdentityR816.COMPILE_SDK;
    public static final int MIN_SDK = ReleaseIdentityR816.MIN_SDK;
    public static final int TARGET_SDK = ReleaseIdentityR816.TARGET_SDK;
    public static final int DB_VERSION = ReleaseIdentityR816.DB_VERSION;
    public static final int SCHEMA_REVISION = ReleaseIdentityR816.SCHEMA_REVISION;
    public static final String GAME_CODE = ReleaseIdentityR816.GAME_CODE;
    public static final String RELEASE_MODE = ReleaseIdentityR816.RELEASE_MODE;

    private ReleaseIdentityR815() {}

    public static String canonical() {
        return ReleaseIdentityR816.canonical();
    }

    public static String sha256() {
        return ReleaseIdentityR816.sha256();
    }

    public static boolean isConsistent() {
        return ReleaseIdentityR816.isConsistent();
    }

    public static void assertBuildConfig() {
        ReleaseIdentityR816.assertBuildConfig();
    }
}
