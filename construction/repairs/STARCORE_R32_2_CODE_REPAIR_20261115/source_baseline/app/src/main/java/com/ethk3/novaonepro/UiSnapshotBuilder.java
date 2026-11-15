package com.ethk3.novaonepro;

import java.util.ArrayList;

/** Builds one immutable R811 UI truth snapshot off the UI thread. */
public final class UiSnapshotBuilder {
    public static final String MODULE_VERSION = "R811_UI_TRUTH_SNAPSHOT_V1";

    public Snapshot build(DbStore store) {
        Snapshot out = new Snapshot();
        out.createdAt = TimeSourceR811.wallNowMs();
        if (store == null) {
            out.runtime = "BusinessTruth=FAIL｜rootCause=READ_ERROR｜STORE_NULL";
            finalizeSnapshot(out);
            return out;
        }
        try {
            RuntimeHealthLedgerR36.BusinessTruth truth =
                    RuntimeHealthLedgerR36.businessTruth(store);
            DiagnosticSnapshotR811 diagnostic = DiagnosticSnapshotR811.capture(store);
            ApiTransportGovernanceEngine.TelemetrySnapshot transport =
                    ApiClient.tianshuTransportSnapshot();
            out.targetIssue = diagnostic.targetIssue;
            out.latestDrawIssue = diagnostic.latestDraw;
            out.latestCanonicalIssue = diagnostic.latestCanonical;
            out.latestSettledIssue = diagnostic.latestSettled;
            out.latestOosIssue = diagnostic.latestOos;
            out.rootCause = truth.rootCause;
            Models.Decision current =
                    CanonicalDecisionViewAdapterR36.read(store, diagnostic.targetIssue);

            out.runtime =
                    "版本="
                            + V51BuildManifest.VERSION_NAME
                            + "｜VC="
                            + V51BuildManifest.VERSION_CODE
                            + "｜DB36/R811="
                            + diagnostic.schemaRevision
                            + "\nBusinessTruth="
                            + truth.status
                            + "｜rootCause="
                            + truth.rootCause
                            + "｜serviceAlive="
                            + (truth.serviceAlive ? 1 : 0)
                            + "｜businessAlive="
                            + (truth.businessAlive ? 1 : 0)
                            + "\nIssue draw/canonical/settled/oos="
                            + issue(diagnostic.latestDraw)
                            + '/'
                            + issue(diagnostic.latestCanonical)
                            + '/'
                            + issue(diagnostic.latestSettled)
                            + '/'
                            + issue(diagnostic.latestOos)
                            + "｜lag="
                            + truth.drawToCanonicalLag
                            + '/'
                            + truth.drawToSettledLag
                            + '/'
                            + truth.settledToOosLag
                            + "\nCORE="
                            + diagnostic.coreStatus
                            + "｜FULL="
                            + diagnostic.fullStatus
                            + "｜Deferred="
                            + diagnostic.deferredPending
                            + "｜P95="
                            + truth.analysisP95Ms
                            + "ms";
            out.decision = decision(store, current, diagnostic);
            out.learning =
                    "PRODUCTION：MODEL_PROVEN="
                            + V51BuildManifest.MODEL_PROVEN
                            + "｜REAL="
                            + (V51BuildManifest.mayAuthorizeRealBet() ? "AUTHORIZED" : "LOCKED")
                            + "\nRESEARCH：SHADOW_ONLY｜state="
                            + state(store, "r811_research_state", "IDLE")
                            + "｜candidate="
                            + state(store, "r811_research_candidate", "NONE")
                            + "\n边界：Research artifact不能写Production/REAL";
            out.diagnosis =
                    "SHADOW predicted/settled/pending="
                            + diagnostic.shadowPredicted
                            + '/'
                            + diagnostic.shadowSettled
                            + '/'
                            + Math.max(0, diagnostic.shadowPredicted - diagnostic.shadowSettled)
                            + "｜hit/miss="
                            + diagnostic.shadowHit
                            + '/'
                            + diagnostic.shadowMiss
                            + "\nRecent30="
                            + window(diagnostic.recent30)
                            + "｜Recent60="
                            + window(diagnostic.recent60)
                            + "｜Recent120="
                            + window(diagnostic.recent120)
                            + "\nDBwait="
                            + diagnostic.dbLockWaitMs
                            + "ms｜RealtimePressure="
                            + (diagnostic.realtimePressure ? "YES" : "NO");
            out.model =
                    "SelectedHitP证明="
                            + (V51BuildManifest.MODEL_PROVEN ? "PROVEN" : "NOT_PROVEN")
                            + "｜Wilson30="
                            + UiFormatter.formatOptionalProbability(
                                    diagnostic.recent30.available(),
                                    diagnostic.recent30.wilsonLower,
                                    "NO_OOS")
                            + "｜Brier30="
                            + optionalMetric(
                                    diagnostic.recent30.available(), diagnostic.recent30.brier)
                            + "｜ECE=N/A（需校准分桶证据）";
            out.transport =
                    "Transport="
                            + safe(transport.activeSourceLine)
                            + "｜latestIssue="
                            + issue(transport.lastSuccessfulIssue)
                            + "｜staleAgeMs="
                            + transport.staleAgeMs
                            + "｜switches="
                            + transport.primaryBackupSwitchCount
                            + "｜primary="
                            + transport.primary.state
                            + "｜backup="
                            + transport.backup.state;
            finalizeSnapshot(out);
            return out;
        } catch (Throwable e) {
            out.runtime =
                    "BusinessTruth=FAIL｜rootCause=READ_ERROR｜"
                            + e.getClass().getSimpleName();
            out.rootCause = Models.ReasonCode.UNKNOWN_READ_FAILED.name();
            finalizeSnapshot(out);
            return out;
        }
    }

    private static String decision(
            DbStore store, Models.Decision value, DiagnosticSnapshotR811 diagnostic) {
        if (value == null) return "WAITING_CANONICAL｜target=" + issue(diagnostic.targetIssue);
        boolean probabilityAvailable =
                !Double.isNaN(value.finalP)
                        && !Double.isInfinite(value.finalP)
                        && value.finalP >= 0.0d
                        && value.finalP <= 1.0d;
        boolean canonical = AppConfig.isValidPosition(value.position);
        if (!canonical) {
            return "目标期="
                    + issue(value.targetIssue)
                    + "｜"
                    + CanonicalReasonCP12.displayDecision(value)
                    + "\nSelectedHitP="
                    + UiFormatter.formatOptionalProbability(false, value.finalP, value.blockCode)
                    + "\n"
                    + bounded(value.reasonSummary, 360)
                    + "\nReason=" + CanonicalReasonCP12.displayDecision(value)
                    + "|receipt=" + CanonicalReasonCP12.persistedCode(value.reasonText).name();
        }
        return "目标期="
                + issue(value.targetIssue)
                + "｜"
                + safe(value.action)
                + "｜P"
                + value.position
                + ' '
                + UiFormatter.numberGroup(value.n1, value.n2, value.n3)
                + "\nSelectedHitP="
                + UiFormatter.formatOptionalProbability(
                        probabilityAvailable, value.finalP, "CALIBRATION_UNAVAILABLE")
                + "｜Evidence="
                + diagnostic.fullStatus
                + "｜Settlement="
                + CanonicalDecisionViewAdapterR36.settlementStatus(
                        store, value.targetIssue, value.settled)
                + "\n"
                + bounded(value.reasonSummary, 360)
                    + "\nReason=" + CanonicalReasonCP12.displayDecision(value)
                    + "|receipt=" + CanonicalReasonCP12.persistedCode(value.reasonText).name();
    }

    private static String window(DiagnosticSnapshotR811.WindowStats stats) {
        if (stats == null || !stats.available()) return "N/A";
        return stats.hit
                + "/"
                + stats.sample
                + "="
                + UiFormatter.formatOptionalProbability(true, stats.hitRate, "NO_OOS");
    }

    private static String optionalMetric(boolean available, double value) {
        if (!available || Double.isNaN(value) || Double.isInfinite(value)) return "N/A";
        return String.valueOf(Math.round(value * 10000.0d) / 10000.0d);
    }

    private static String state(DbStore store, String key, String fallback) {
        String value = store.getState(key, fallback);
        return value == null || value.length() == 0 ? fallback : value;
    }

    private static void finalizeSnapshot(Snapshot out) {
        out.hash =
                V51ChecksumEngine.sha256(
                        out.runtime
                                + '|'
                                + out.decision
                                + '|'
                                + out.learning
                                + '|'
                                + out.diagnosis
                                + '|'
                                + out.model
                                + '|'
                                + out.transport);
        out.sections.add(out.runtime);
        out.sections.add(out.decision);
        out.sections.add(out.learning);
        out.sections.add(out.diagnosis);
        out.sections.add(out.model);
        out.sections.add(out.transport);
    }

    private static String issue(String value) {
        return safe(value).length() == 0 ? "--" : safe(value);
    }

    private static String bounded(String value, int max) {
        String out = safe(value);
        return out.length() <= max ? out : out.substring(0, max) + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Snapshot {
        public long createdAt;
        public String runtime = "";
        public String decision = "";
        public String learning = "";
        public String diagnosis = "";
        public String model = "";
        public String transport = "";
        public String targetIssue = "";
        public String latestDrawIssue = "";
        public String latestCanonicalIssue = "";
        public String latestSettledIssue = "";
        public String latestOosIssue = "";
        public String rootCause = "UNKNOWN";
        public String hash = "";
        public final ArrayList<String> sections = new ArrayList<String>();
    }
}
