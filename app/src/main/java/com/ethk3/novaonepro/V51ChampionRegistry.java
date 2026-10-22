package com.ethk3.novaonepro;

/** Persistent champion identity. The compiled bundle remains authoritative. */
public final class V51ChampionRegistry {
    private static final String KEY_BUNDLE = "v51_champion_bundle_hash";
    private static final String KEY_STATE = "v51_champion_state";
    private static final String KEY_AT = "v51_champion_promoted_at";

    private V51ChampionRegistry() {}

    public static String currentBundle(DbStore store) {
        if (store == null) return V51ArtifactBundleManifest.BUNDLE_HASH;
        String value = store.getState(KEY_BUNDLE, "");
        return value.length() == 0 ? V51ArtifactBundleManifest.BUNDLE_HASH : value;
    }

    public static boolean isCompiledChampion(DbStore store) {
        return V51ArtifactBundleManifest.BUNDLE_HASH.equals(currentBundle(store));
    }

    static void mark(DbStore store, String bundle, String state) {
        if (store == null) throw new IllegalArgumentException("V51_STORE_NULL");
        store.setState(KEY_BUNDLE, bundle);
        store.setState(KEY_STATE, state);
        store.setState(KEY_AT, String.valueOf(System.currentTimeMillis()));
    }
}
