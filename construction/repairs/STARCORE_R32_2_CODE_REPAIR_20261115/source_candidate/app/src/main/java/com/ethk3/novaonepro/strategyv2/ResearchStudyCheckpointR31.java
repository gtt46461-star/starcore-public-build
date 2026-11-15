package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B08-C03: migration of research study checkpoints from the legacy chart cache to a typed store.
 *
 * <p>While migrating, every put writes both stores in the same transaction (the caller's batch) and verify compares
 * them byte for byte. Only after every legacy key verified does the typed store become the single authority; from then
 * on the legacy cache is a rebuildable projection and may be cleared. Clearing the legacy cache before the switch is
 * refused, so a cache TTL can never delete the only copy of a numerical state.</p>
 */
public final class ResearchStudyCheckpointR31 {
    public static final String VERSION = "R31_RESEARCH_STUDY_CHECKPOINT_V1";
    public interface Store { String get(String key); void put(String key, String value); Set<String> keys(); void remove(String key); }
    public enum Authority { LEGACY_CACHE, DUAL_WRITE_VERIFYING, TYPED_STORE }

    private final Store legacy, typed; private Authority authority = Authority.LEGACY_CACHE;
    public ResearchStudyCheckpointR31(Store legacy, Store typed) { this.legacy = legacy; this.typed = typed; }
    public Authority authority() { return authority; }

    static String envelope(String key, String state) { return VERSION + "|" + StatsR27.sha256(key + "|" + state) + "|" + state; }
    static String unwrap(String key, String stored) {
        if (stored == null) return null;
        String[] f = stored.split("\\|", 3);
        if (f.length != 3 || !VERSION.equals(f[0]) || !StatsR27.sha256(key + "|" + f[2]).equals(f[1])) throw new IllegalStateException("R31_STUDY_CHECKPOINT_INTEGRITY:" + key);
        return f[2];
    }

    public void put(String key, String state) {
        if (state == null || state.isEmpty()) throw new IllegalArgumentException("R31_STUDY_CHECKPOINT_EMPTY");
        switch (authority) {
            case LEGACY_CACHE: legacy.put(key, state); break;
            case DUAL_WRITE_VERIFYING: legacy.put(key, state); typed.put(key, envelope(key, state)); break;
            case TYPED_STORE: typed.put(key, envelope(key, state)); legacy.put(key, state); break; // legacy = projection
        }
    }
    public String get(String key) { return authority == Authority.TYPED_STORE ? unwrap(key, typed.get(key)) : legacy.get(key); }
    /** True when the typed copy of `key` equals the legacy value. */
    public boolean verify(String key) { String l = legacy.get(key), t = unwrap(key, typed.get(key)); return l != null && l.equals(t); }
    /** Copies every legacy state, verifies each, and switches authority only when all verified. Returns mismatched keys. */
    public List<String> migrate() {
        if (authority == Authority.TYPED_STORE) return Collections.emptyList();
        authority = Authority.DUAL_WRITE_VERIFYING;
        for (String key : new TreeSet<>(legacy.keys())) { String v = legacy.get(key); if (v != null) typed.put(key, envelope(key, v)); }
        List<String> bad = new ArrayList<>(); for (String key : new TreeSet<>(legacy.keys())) if (!verify(key)) bad.add(key);
        if (bad.isEmpty()) authority = Authority.TYPED_STORE;
        return bad;
    }
    /** The legacy cache may be dropped (TTL) only after the typed store is the authority. */
    public void clearLegacy() {
        if (authority != Authority.TYPED_STORE) throw new IllegalStateException("R31_STUDY_CHECKPOINT_CACHE_BEFORE_MIGRATION");
        for (String key : new ArrayList<>(legacy.keys())) legacy.remove(key);
    }
    /** Rebuilds the legacy projection from the authority. */
    public void rebuildLegacy() { if (authority != Authority.TYPED_STORE) throw new IllegalStateException("R31_STUDY_CHECKPOINT_NOT_AUTHORITATIVE"); for (String key : typed.keys()) legacy.put(key, unwrap(key, typed.get(key))); }
}
