package com.ethk3.novaonepro;

/**
 * R926 read-only presentation repository.
 *
 * <p>This type deliberately exposes only rendering/read operations used by the client UI.  It has
 * no schema initializer, no writable database handle, no DecisionGate access and no settlement
 * entry point.  Moving these reads out of MainActivity makes the UI independently testable and
 * prevents diagnostic screens from becoming accidental write-path owners.
 */
public final class RuntimeReadOnlyRepositoryR926 {
    private final DbStore store;

    public RuntimeReadOnlyRepositoryR926(DbStore store) {
        if (store == null) throw new IllegalArgumentException("R926_READ_STORE_NULL");
        this.store = store;
    }

    public RuntimeTruthSnapshotR906 runtimeTruth() {
        return RuntimeTruthSnapshotR906.capture(store);
    }

    public String researchDashboardSection(int section) {
        if (section < 0 || section >= ResearchDashboardR924.TITLES.length)
            throw new IllegalArgumentException("R926_RESEARCH_SECTION_OUT_OF_RANGE:" + section);
        return ResearchDashboardR924.captureSection(store, section);
    }

    public String pipeline(int limit, boolean csv) {
        return RuntimeObservabilityR925.pipeline(store, boundedLimit(limit, 5000), csv);
    }

    public String fetches(int limit) {
        return RuntimeObservabilityR925.fetches(store, boundedLimit(limit, 1000));
    }

    public String shadow() {
        return RuntimeObservabilityR925.shadow(store);
    }

    public String researchReadiness() {
        return ResearchReadinessR924.capture(store);
    }

    public String lifecycleAudit() {
        return LifecycleAuditR925.report(store);
    }

    public String combinedRuntimeDetail() {
        RuntimeTruthSnapshotR906 snapshot = runtimeTruth();
        return snapshot.pipelineDetail
                + "\n\n"
                + snapshot.shadowDetail
                + "\n\n"
                + snapshot.realDetail
                + "\n\n"
                + researchReadiness();
    }

    private static int boundedLimit(int value, int max) {
        if (value < 1) return 1;
        return Math.min(value, max);
    }
}
