package com.ethk3.novaonepro;

/** Fail-closed rollback: disable real authorization and retain evidence. */
public final class V51ArtifactRollbackEngine {
    private V51ArtifactRollbackEngine() {}

    public static void rollback(DbStore store, String reason) {
        if (store == null) return;
        String safe = reason == null ? "" : reason;
        store.setState("v51_runtime_forced_shadow", "1");
        store.setState("v51_rollback_reason", safe);
        store.setState("v51_rollback_at", String.valueOf(System.currentTimeMillis()));
        store.setState("r811_production_rollback_point", V51ArtifactBundleManifest.BUNDLE_HASH);
        V51ChampionRegistry.mark(
                store, V51ArtifactBundleManifest.BUNDLE_HASH, "ROLLED_BACK_SHADOW");
    }

    public static boolean forcedShadow(DbStore store) {
        return store != null && "1".equals(store.getState("v51_runtime_forced_shadow", "0"));
    }

    /**
     * Hot-path read-only view of the persisted rollback gate. Known R914 transient health
     * rollbacks are eligible for the same recovery that reconcileTransientRollbackR915 performs,
     * but no SQLite write is allowed while Selection Policy is running. The durable cleanup is
     * performed only after the canonical decision commits.
     */
    public static boolean effectiveForcedShadowR947(DbStore store) {
        if (store == null) return true;
        if (!forcedShadow(store)) return false;
        if (!V51BuildManifest.isIdentityConsistent()) return true;
        if ("1".equals(store.getState("v57_artifact_rollback_all", "0"))) return true;
        String reason = store.getState("v51_rollback_reason", "");
        return !("R811_HARD_GATE:SETTLEMENT_OOS".equals(reason)
                || "R811_HARD_GATE:RUNTIME_CHAIN".equals(reason));
    }

    /**
     * R914 persisted transient runtime-health failures as an indefinite Production rollback.  A
     * cover install therefore stayed SHADOW forever even after the scheduler/settlement repair.
     * R915 clears only those two known transient causes and never clears an atomic/artifact or
     * operator rollback.
     */
    public static boolean reconcileTransientRollbackR915(DbStore store) {
        if (store == null || !V51BuildManifest.isIdentityConsistent()) return false;
        if (!"1".equals(store.getState("v51_runtime_forced_shadow", "0"))) return false;
        if ("1".equals(store.getState("v57_artifact_rollback_all", "0"))) return false;
        String reason = store.getState("v51_rollback_reason", "");
        if (!("R811_HARD_GATE:SETTLEMENT_OOS".equals(reason)
                || "R811_HARD_GATE:RUNTIME_CHAIN".equals(reason))) return false;
        store.setState("r915_cleared_transient_rollback_reason", reason);
        store.setState("r915_cleared_transient_rollback_at",
                String.valueOf(System.currentTimeMillis()));
        store.setState("v51_runtime_forced_shadow", "0");
        store.setState("v51_rollback_reason", "");
        store.setState("v51_rollback_at", "0");
        store.setState("r811_production_rollback_point", "");
        V51ChampionRegistry.mark(
                store, V51ArtifactBundleManifest.BUNDLE_HASH, "ACTIVE_R915_CONTROLLED_CANDIDATE");
        return true;
    }

    public static void rollbackResearch(DbStore store, String candidateId, String reason) {
        if (store == null) return;
        store.setState("r811_research_rollback_candidate", candidateId == null ? "" : candidateId);
        store.setState("r811_research_rollback_reason", reason == null ? "" : reason);
        store.setState("r811_research_shadow_state", "ROLLED_BACK_TO_RESEARCH");
        store.setState("r811_research_rollback_at", String.valueOf(TimeSourceR811.wallNowMs()));
    }

    public static boolean enforceHardGate(DbStore store) {
        if (store == null) return true;
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruth(store);
        if ("READ_ERROR".equals(truth.rootCause)
                || "RUNTIME_CHAIN".equals(truth.rootCause)
                || "SETTLEMENT_OOS".equals(truth.rootCause)) {
            rollback(store, "R811_HARD_GATE:" + truth.rootCause);
            return true;
        }
        return forcedShadow(store);
    }

    // V57_B13_ATOMIC_ROLLBACK
    public static void rollbackAtomicBundle(DbStore store, String reason) {
        if (store == null) return;
        rollback(store, "V57_ATOMIC:" + (reason == null ? "" : reason));
        store.setState("v57_artifact_bundle_state", "ROLLED_BACK_SHADOW");
        store.setState("v57_artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
        store.setState("v57_artifact_rollback_all", "1");
    }

    public static boolean mixedArtifactActivationAllowed() {
        return false;
    }
}
