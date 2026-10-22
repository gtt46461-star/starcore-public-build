package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * B07 exact OOS audit. It distinguishes an absent surface, a partial surface, contract-invalid rows
 * and an actual SQLite write failure. Diagnostic fallback only writes immutable OOS observations;
 * it never enters DecisionGate and never creates BET.
 */
public final class OosWriteAuditEngine {
    public static final String MODULE_VERSION = "STARCORE_TIANSHU_B07_OOS_WRITE_AUDIT_20260806";
    public static final String ROOT_NO_SURFACE = "NO_SURFACE";
    public static final String ROOT_PARTIAL_SURFACE = "PARTIAL_SURFACE";
    public static final String ROOT_CONTRACT_INVALID = "CONTRACT_INVALID";
    public static final String ROOT_SQLITE_WRITE_FAILURE = "SQLITE_WRITE_FAILURE";
    public static OosActionLogEngine.BatchWriteResult LAST =
            new OosActionLogEngine.BatchWriteResult();

    private OosWriteAuditEngine() {}

    public static int persist(DbStore store, List<Models.ComboCandidate> formal) {
        return persist(store, formal, null, "");
    }

    public static int persist(
            DbStore store,
            List<Models.ComboCandidate> formal,
            List<Models.ComboCandidate> diagnosticSurface,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> selected = prepare(formal);
        String root = "OK";
        String mode = "V48_FINAL_RANK_PRE_LANE";

        if (selected.size() == 0) {
            root = ROOT_NO_SURFACE;
        } else if (selected.size() < AppConfig.V48_FORMAL_CANDIDATE_COUNT) {
            root = ROOT_PARTIAL_SURFACE;
        }

        if (selected.size() != AppConfig.V48_FORMAL_CANDIDATE_COUNT) {
            ArrayList<Models.ComboCandidate> fallback = prepare(diagnosticSurface);
            if (fallback.size() > selected.size()) {
                selected = fallback;
                mode = "B07_SHADOW_DIAGNOSTIC_FALLBACK_NO_BET";
            }
        }

        int cap = Math.min(AppConfig.V48_FORMAL_CANDIDATE_COUNT, selected.size());
        ArrayList<Models.ComboCandidate> write = new ArrayList<Models.ComboCandidate>();
        int invalid = 0;
        for (int i = 0; i < cap; i++) {
            Models.ComboCandidate c = selected.get(i);
            if (c == null) {
                invalid++;
                continue;
            }
            if (empty(c.baseIssue)) c.baseIssue = c.snapshotMaxIssue;
            if (empty(c.targetIssue)) c.targetIssue = targetIssue;
            if (empty(c.snapshotMaxIssue)) c.snapshotMaxIssue = c.baseIssue;
            if (empty(c.evidenceAsOfIssue)) c.evidenceAsOfIssue = c.baseIssue;
            if (empty(c.trainedUntilIssue)) c.trainedUntilIssue = c.baseIssue;
            PredictionContractEngine.stampCandidate(c, null);
            if (c.contractValid == 1) write.add(c);
            else invalid++;
        }

        if (invalid > 0) root = ROOT_CONTRACT_INVALID;
        if (write.size() > 0) {
            LAST = OosActionLogEngine.recordCandidatesDetailed(store, write, write.size(), mode);
            if (LAST.failed > 0 || (LAST.error != null && LAST.error.length() > 0))
                root = ROOT_SQLITE_WRITE_FAILURE;
        } else {
            LAST = new OosActionLogEngine.BatchWriteResult();
            LAST.requested = 0;
            LAST.valid = 0;
            LAST.error = "";
            if (invalid == 0) root = ROOT_NO_SURFACE;
        }

        try {
            if (store != null) {
                store.setState(
                        "v48_oos_write_audit",
                        root
                                + "|formal="
                                + size(formal)
                                + "|diagnostic="
                                + size(diagnosticSurface)
                                + "|selected="
                                + selected.size()
                                + "|valid="
                                + write.size()
                                + "|"
                                + LAST.summary());
                store.setState("v48_oos_write_root", root);
                store.setState("v48_oos_write_owner", "OosWriteAudit/OosActionLog");
                store.setState("v48_oos_write_mode", mode);
                store.setState("v48_oos_write_version", MODULE_VERSION);
                store.setState("v48_oos_write_policy", "DIAGNOSTIC_ONLY_NOT_DECISION_GATE_NOT_BET");
            }
        } catch (Throwable ignore) {
        }
        try {
            V48PipelineLedger.oos(store, LAST.inserted, LAST.duplicateExisting, LAST.failed);
        } catch (Throwable ignore) {
        }
        return LAST.inserted + LAST.duplicateExisting;
    }

    private static ArrayList<Models.ComboCandidate> prepare(List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        for (int i = 0; i < input.size(); i++) if (input.get(i) != null) out.add(input.get(i));
        return out;
    }

    private static int size(List<?> x) {
        return x == null ? 0 : x.size();
    }

    private static boolean empty(String x) {
        return x == null || x.length() == 0;
    }
}
