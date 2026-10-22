package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;

/**
 * Runtime persistence adapter for Batch-3/4.
 *
 * <p>This class never creates a BET and never changes the existing prediction/settlement decision.
 * It consumes the already-frozen canonical observation and writes restart-safe research evidence
 * inside the caller's SQLite transaction. A SAVEPOINT isolates research failures so the restored
 * Batch-2 output/shadow chain cannot be broken by Batch-4 evidence collection.</p>
 */
public final class Batch34RuntimeRepositoryR930 {
    public static final String VERSION = "R930_BATCH34_RUNTIME_REPOSITORY_1";
    private static final String CAPTURE_SOURCE = "Batch34RuntimeRepositoryR930";

    private Batch34RuntimeRepositoryR930() {}

    public static final class CaptureResult {
        public final boolean success;
        public final boolean replay;
        public final String targetIssue;
        public final String state;
        public final int sequence;
        public final String reason;
        public final String evidenceSha256;

        CaptureResult(
                boolean success,
                boolean replay,
                String targetIssue,
                String state,
                int sequence,
                String reason,
                String evidenceSha256) {
            this.success = success;
            this.replay = replay;
            this.targetIssue = safe(targetIssue);
            this.state = safe(state);
            this.sequence = sequence;
            this.reason = safe(reason);
            this.evidenceSha256 = safe(evidenceSha256);
        }

        public String canonical() {
            return success + "|" + replay + "|" + targetIssue + "|" + state + "|" + sequence
                    + "|" + reason + "|" + evidenceSha256;
        }
    }

    public static final class CaptureSummary {
        public final int systemAvailable;
        public final int eligible;
        public final int selected;
        public final int executionStarted;
        public final int frozen;
        public final int failed;

        CaptureSummary(int available, int eligible, int selected, int started, int frozen, int failed) {
            systemAvailable = available;
            this.eligible = eligible;
            this.selected = selected;
            executionStarted = started;
            this.frozen = frozen;
            this.failed = failed;
        }

        public boolean monotonic() {
            return systemAvailable >= eligible
                    && eligible >= selected
                    && selected >= executionStarted
                    && executionStarted >= frozen
                    && failed >= 0
                    && failed <= systemAvailable;
        }

        public String canonical() {
            return "available=" + systemAvailable + "|eligible=" + eligible + "|selected=" + selected
                    + "|started=" + executionStarted + "|frozen=" + frozen + "|failed=" + failed;
        }
    }

    public static final class PolicyMetrics {
        public final String policySha256;
        public final String startIssue;
        public final String endIssue;
        public final int availableN;
        public final int selectedN;
        public final int settledSelectedN;
        public final int calibratedN;
        public final int hits;
        public final double coverage;
        public final double hitRate;
        public final double brier;
        public final double risk;
        public final int frequencyDebt;
        public final String denominator;

        PolicyMetrics(
                String policySha256,
                String startIssue,
                String endIssue,
                int availableN,
                int selectedN,
                int settledSelectedN,
                int calibratedN,
                int hits,
                double coverage,
                double hitRate,
                double brier,
                double risk,
                int frequencyDebt,
                String denominator) {
            this.policySha256 = safe(policySha256);
            this.startIssue = safe(startIssue);
            this.endIssue = safe(endIssue);
            this.availableN = availableN;
            this.selectedN = selectedN;
            this.settledSelectedN = settledSelectedN;
            this.calibratedN = calibratedN;
            this.hits = hits;
            this.coverage = coverage;
            this.hitRate = hitRate;
            this.brier = brier;
            this.risk = risk;
            this.frequencyDebt = frequencyDebt;
            this.denominator = safe(denominator);
        }

        public String canonical() {
            return policySha256 + "|" + startIssue + "|" + endIssue + "|" + availableN + "|"
                    + selectedN + "|" + settledSelectedN + "|" + calibratedN + "|" + hits + "|" + finite(coverage)
                    + "|" + finite(hitRate) + "|" + finite(brier) + "|" + finite(risk) + "|"
                    + frequencyDebt + "|" + denominator;
        }
    }

    private static final class SelectedObservation {
        final boolean found;
        final String lane;
        final String action;
        final String rejectCode;
        final double selectedHitP;
        final double predictability;
        final String recordHash;
        final int preDrawFrozen;

        SelectedObservation(
                boolean found,
                String lane,
                String action,
                String rejectCode,
                double selectedHitP,
                double predictability,
                String recordHash,
                int preDrawFrozen) {
            this.found = found;
            this.lane = safe(lane);
            this.action = safe(action);
            this.rejectCode = safe(rejectCode);
            this.selectedHitP = selectedHitP;
            this.predictability = predictability;
            this.recordHash = safe(recordHash);
            this.preDrawFrozen = preDrawFrozen;
        }
    }

    private static final class ExistingCapture {
        final String state;
        final int sequence;
        final String canonicalHash;
        final String evidenceHash;

        ExistingCapture(String state, int sequence, String canonicalHash, String evidenceHash) {
            this.state = safe(state);
            this.sequence = sequence;
            this.canonicalHash = safe(canonicalHash);
            this.evidenceHash = safe(evidenceHash);
        }
    }

    /**
     * C04 + D09 canonical hook. Every durable canonical issue receives one capture funnel and one
     * immutable policy decision, regardless of REAL/SHADOW lane.
     */
    public static CaptureResult captureCanonicalWithinTransaction(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,
            String canonicalHash) {
        requireTransaction(db, "R930_CAPTURE_TRANSACTION_REQUIRED");
        String issue = request == null ? "" : safe(request.targetIssue);
        if (issue.length() == 0) {
            return failure(db, request, "R930_CAPTURE_ISSUE_MISSING", "");
        }
        if (!fastSchemaReady(db)) {
            return failure(db, request, "R930_CAPTURE_SCHEMA_NOT_READY", "");
        }

        String canonical = normalizeHash("CANONICAL", canonicalHash);
        String frozenSnapshot = normalizeHash(
                "SNAPSHOT",
                safe(snapshotHash) + "|" + (snapshot == null ? "" : safe(snapshot.recordHash)));
        ExistingCapture existing = readCapture(db, issue);
        if (existing != null) {
            if ("FROZEN".equals(existing.state) && existing.canonicalHash.equals(canonical)) {
                appendRuntimeEvent(
                        db,
                        request,
                        "BATCH34_CAPTURE",
                        "CAPTURE_REPLAY_IDEMPOTENT",
                        "state=FROZEN|sequence=" + existing.sequence
                                + "|evidence_hash=" + existing.evidenceHash,
                        "BATCH34_CAPTURE|" + issue + "|" + canonical);
                return new CaptureResult(
                        true,
                        true,
                        issue,
                        existing.state,
                        existing.sequence,
                        "C04_REPLAY_IDEMPOTENT",
                        existing.evidenceHash);
            }
            return failure(
                    db,
                    request,
                    "R930_CAPTURE_IMMUTABLE_HASH_CONFLICT",
                    "stored=" + existing.canonicalHash + "|incoming=" + canonical);
        }

        db.execSQL("SAVEPOINT r930_capture");
        try {
            SelectedObservation observation = readSelectedObservation(db, issue);
            long now = System.currentTimeMillis();
            String opportunity = V51ChecksumEngine.sha256(
                    "C04_OPPORTUNITY|" + ReleaseManifestR926.GAME_CODE + "|" + issue + "|" + canonical);

            ContentValues initial = new ContentValues();
            initial.put("game_code", ReleaseManifestR926.GAME_CODE);
            initial.put("target_issue", issue);
            initial.put("opportunity_sha256", opportunity);
            initial.put("current_state", "SYSTEM_AVAILABLE");
            initial.put("sequence", 0);
            initial.put("system_available", 1);
            initial.put("eligible", 0);
            initial.put("selected", 0);
            initial.put("execution_started", 0);
            initial.put("frozen", 0);
            initial.put("failed", 0);
            initial.put("lane", observation.lane);
            initial.put("decision_action", observation.action);
            initial.put("reject_code", observation.rejectCode);
            initial.put("selected_hit_p", finiteProbability(observation.selectedHitP)
                    ? observation.selectedHitP : -1.0d);
            initial.put("predictability", finiteProbability(observation.predictability)
                    ? observation.predictability : -1.0d);
            initial.put("canonical_hash", canonical);
            initial.put("snapshot_hash", frozenSnapshot);
            initial.put("frozen_payload_hash", "");
            initial.put("last_failure", "");
            initial.put("created_at", now);
            initial.put("updated_at", now);
            long inserted = db.insertWithOnConflict(
                    Batch34SchemaR930.TABLE_CAPTURE,
                    null,
                    initial,
                    SQLiteDatabase.CONFLICT_IGNORE);
            if (inserted < 0L) {
                ExistingCapture raced = readCapture(db, issue);
                if (raced != null && "FROZEN".equals(raced.state)
                        && raced.canonicalHash.equals(canonical)) {
                    db.execSQL("RELEASE r930_capture");
                    return new CaptureResult(
                            true,
                            true,
                            issue,
                            raced.state,
                            raced.sequence,
                            "C04_REPLAY_IDEMPOTENT",
                            raced.evidenceHash);
                }
                throw new IllegalStateException("R930_CAPTURE_INITIAL_INSERT_FAILED");
            }

            String selectedPayload = observation.found
                    + "|" + observation.preDrawFrozen + "|" + observation.lane + "|"
                    + observation.action + "|" + observation.rejectCode + "|"
                    + finite(observation.selectedHitP) + "|" + finite(observation.predictability)
                    + "|" + observation.recordHash;
            if (!observation.found || observation.preDrawFrozen != 1
                    || observation.recordHash.length() == 0) {
                String failedHash = V51ChecksumEngine.sha256(
                        "C04_FAILED|" + issue + "|" + canonical + "|" + selectedPayload);
                transition(
                        db,
                        issue,
                        "SYSTEM_AVAILABLE",
                        "FAILED",
                        0,
                        1,
                        "C04_CANONICAL_SHADOW_OBSERVATION_MISSING",
                        failedHash);
                appendEvidence(
                        db,
                        issue,
                        "CAPTURE_FAILED",
                        failedHash,
                        "missing_or_unfrozen_shadow_observation");
                appendRuntimeEvent(
                        db,
                        request,
                        "BATCH34_CAPTURE",
                        "CAPTURE_FAILED",
                        "reason=C04_CANONICAL_SHADOW_OBSERVATION_MISSING|evidence_hash=" + failedHash,
                        "BATCH34_CAPTURE_FAILED|" + issue + "|" + canonical);
                db.execSQL("RELEASE r930_capture");
                return new CaptureResult(
                        false,
                        false,
                        issue,
                        "FAILED",
                        1,
                        "C04_CANONICAL_SHADOW_OBSERVATION_MISSING",
                        failedHash);
            }

            String eligibleHash = V51ChecksumEngine.sha256("C04_ELIGIBLE|" + selectedPayload);
            transition(db, issue, "SYSTEM_AVAILABLE", "ELIGIBLE", 0, 1,
                    "C04_PRE_DRAW_OBSERVATION_VALID", eligibleHash);
            String selectedHash = V51ChecksumEngine.sha256(
                    "C04_SELECTED|" + selectedPayload + "|" + eligibleHash);
            transition(db, issue, "ELIGIBLE", "SELECTED", 1, 2,
                    "C04_EXISTING_OWNER_SELECTED_ACTION", selectedHash);
            String startedHash = V51ChecksumEngine.sha256(
                    "C04_EXECUTION_STARTED|" + selectedPayload + "|" + selectedHash);
            transition(db, issue, "SELECTED", "EXECUTION_STARTED", 2, 3,
                    "C04_CANONICAL_TRANSACTION_STARTED", startedHash);
            String frozenPayload = V51ChecksumEngine.sha256(
                    "C04_FROZEN|" + issue + "|" + opportunity + "|" + canonical + "|"
                            + frozenSnapshot + "|" + selectedPayload + "|" + startedHash);
            transition(db, issue, "EXECUTION_STARTED", "FROZEN", 3, 4,
                    "C04_CANONICAL_AND_SHADOW_ATOMICALLY_FROZEN", frozenPayload);

            ContentValues frozen = new ContentValues();
            frozen.put("frozen_payload_hash", frozenPayload);
            frozen.put("updated_at", System.currentTimeMillis());
            int frozenRows = db.update(
                    Batch34SchemaR930.TABLE_CAPTURE,
                    frozen,
                    "game_code=? AND target_issue=? AND current_state='FROZEN' AND sequence=4",
                    new String[] {ReleaseManifestR926.GAME_CODE, issue});
            if (frozenRows != 1) {
                throw new IllegalStateException("R930_CAPTURE_FROZEN_HASH_UPDATE_FAILED");
            }

            appendEvidence(
                    db,
                    issue,
                    "CAPTURE_FROZEN",
                    frozenPayload,
                    "lane=" + observation.lane + "|action=" + observation.action
                            + "|reject=" + observation.rejectCode);
            persistPolicyDecision(db, issue, observation, canonical);
            appendRuntimeEvent(
                    db,
                    request,
                    "BATCH34_CAPTURE",
                    "CAPTURE_FROZEN",
                    "state=FROZEN|sequence=4|lane=" + observation.lane
                            + "|decision_action=" + observation.action
                            + "|reject_code=" + observation.rejectCode
                            + "|selected_hit_p=" + finite(observation.selectedHitP)
                            + "|predictability=" + finite(observation.predictability)
                            + "|evidence_hash=" + frozenPayload,
                    "BATCH34_CAPTURE|" + issue + "|" + canonical);

            db.execSQL("RELEASE r930_capture");
            return new CaptureResult(
                    true,
                    false,
                    issue,
                    "FROZEN",
                    4,
                    "C04_CAPTURE_FROZEN",
                    frozenPayload);
        } catch (Throwable error) {
            rollbackSavepoint(db, "r930_capture");
            return failure(
                    db,
                    request,
                    "R930_CAPTURE_WRITE_FAILED",
                    error.getClass().getSimpleName() + ":" + safe(error.getMessage()));
        }
    }

    /** Settlement hook freezes D09 outcome and, on a real miss, an immutable C09 failure case. */
    public static void recordSettlementWithinTransaction(
            SQLiteDatabase db,
            String issue,
            String settlementHash,
            String status,
            String drawHash) {
        requireTransaction(db, "R930_SETTLEMENT_TRANSACTION_REQUIRED");
        String target = safe(issue);
        if (target.length() == 0 || !fastSchemaReady(db)) return;
        db.execSQL("SAVEPOINT r930_settlement");
        try {
            String settlement = normalizeHash("SETTLEMENT", settlementHash);
            String draw = normalizeHash("DRAW", drawHash);
            int hit = hitLabel(db, target, status);
            ContentValues outcome = new ContentValues();
            outcome.put("game_code", ReleaseManifestR926.GAME_CODE);
            outcome.put("target_issue", target);
            outcome.put("hit_label", hit);
            outcome.put("settlement_sha256", settlement);
            outcome.put("draw_sha256", draw);
            outcome.put("status", safe(status));
            outcome.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict(
                    Batch34SchemaR930.TABLE_POLICY_OUTCOME,
                    null,
                    outcome,
                    SQLiteDatabase.CONFLICT_IGNORE);

            String evidenceHash = V51ChecksumEngine.sha256(
                    "D09_OUTCOME|" + target + "|" + hit + "|" + settlement + "|" + draw + "|"
                            + safe(status));
            appendEvidence(db, target, "POLICY_OUTCOME", evidenceHash,
                    "hit_label=" + hit + "|status=" + safe(status));

            if (hit == 0) persistFailureCase(db, target, settlement, draw, status);
            appendRuntimeEvent(
                    db,
                    target,
                    "",
                    "",
                    "BATCH34_OUTCOME",
                    "POLICY_OUTCOME_FROZEN",
                    RuntimeEventLedgerR926.RANK_SETTLED - 1,
                    "hit_label=" + hit + "|settlement_hash=" + settlement
                            + "|draw_hash=" + draw + "|evidence_hash=" + evidenceHash,
                    "BATCH34_OUTCOME|" + target + "|" + settlement);
            db.execSQL("RELEASE r930_settlement");
        } catch (Throwable error) {
            rollbackSavepoint(db, "r930_settlement");
            appendRuntimeEvent(
                    db,
                    target,
                    "",
                    "",
                    "BATCH34_OUTCOME",
                    "OUTCOME_CAPTURE_FAILED",
                    RuntimeEventLedgerR926.RANK_SETTLED - 1,
                    "error=" + error.getClass().getSimpleName() + ":" + safe(error.getMessage()),
                    "BATCH34_OUTCOME_FAILED|" + target + "|"
                            + V51ChecksumEngine.sha256(String.valueOf(error)));
        }
    }

    public static void recordFullWithinTransaction(
            SQLiteDatabase db, String issue, String snapshotHash, String actionSpaceHash) {
        requireTransaction(db, "R930_FULL_TRANSACTION_REQUIRED");
        if (!fastSchemaReady(db) || safe(issue).length() == 0) return;
        String payload = V51ChecksumEngine.sha256(
                "R930_FULL|" + safe(issue) + "|" + normalizeHash("FULL_SNAPSHOT", snapshotHash)
                        + "|" + normalizeHash("ACTION_SPACE", actionSpaceHash));
        appendEvidence(db, issue, "FULL_1200_ACTION_EVIDENCE", payload,
                "snapshot_hash=" + safe(snapshotHash) + "|action_space_hash=" + safe(actionSpaceHash));
    }

    public static void recordLearnedWithinTransaction(
            SQLiteDatabase db,
            String issue,
            String source,
            String learningIdentity,
            String preStateHash,
            String postStateHash,
            String trainedUntil) {
        requireTransaction(db, "R930_LEARNED_TRANSACTION_REQUIRED");
        if (!fastSchemaReady(db) || safe(issue).length() == 0) return;
        String payload = V51ChecksumEngine.sha256(
                "R930_LEARNED|" + safe(issue) + "|" + safe(source) + "|"
                        + safe(learningIdentity) + "|" + safe(preStateHash) + "|"
                        + safe(postStateHash) + "|" + safe(trainedUntil));
        appendEvidence(db, issue, "LEARNED_EXACTLY_ONCE_LINK", payload,
                "source=" + safe(source) + "|trained_until=" + safe(trainedUntil));
    }

    public static CaptureSummary captureSummary(SQLiteDatabase db) {
        if (db == null || !fastSchemaReady(db)) return new CaptureSummary(0, 0, 0, 0, 0, 0);
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT COALESCE(SUM(system_available),0),COALESCE(SUM(eligible),0),"
                            + "COALESCE(SUM(selected),0),COALESCE(SUM(execution_started),0),"
                            + "COALESCE(SUM(frozen),0),COALESCE(SUM(failed),0) FROM "
                            + Batch34SchemaR930.TABLE_CAPTURE
                            + " WHERE game_code=?",
                    new String[] {ReleaseManifestR926.GAME_CODE});
            if (!c.moveToFirst()) return new CaptureSummary(0, 0, 0, 0, 0, 0);
            return new CaptureSummary(
                    c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4), c.getInt(5));
        } finally {
            if (c != null) c.close();
        }
    }

    /** D09 descriptive effect surface. No threshold is forced and frequency debt is always zero. */
    public static PolicyMetrics policyMetrics(SQLiteDatabase db, String policySha256) {
        String policy = safe(policySha256);
        if (db == null || !fastSchemaReady(db) || policy.length() == 0) {
            return new PolicyMetrics(policy, "", "", 0, 0, 0, 0,
                    0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0,
                    "AVAILABLE_ISSUES|SELECTED_ISSUES|SETTLED_SELECTED_ISSUES|CALIBRATED_SELECTED_ISSUES");
        }
        Cursor c = null;
        int available = 0;
        int selected = 0;
        int settledSelected = 0;
        int calibrated = 0;
        int hits = 0;
        double squared = 0.0d;
        String start = "";
        String end = "";
        try {
            c = db.rawQuery(
                    "SELECT d.target_issue,d.selected,d.selected_hit_p,o.hit_label"
                            + " FROM " + Batch34SchemaR930.TABLE_POLICY_DECISION + " d"
                            + " LEFT JOIN " + Batch34SchemaR930.TABLE_POLICY_OUTCOME + " o"
                            + " ON o.game_code=d.game_code AND o.target_issue=d.target_issue"
                            + " WHERE d.game_code=? AND d.policy_sha256=? ORDER BY d.target_issue",
                    new String[] {ReleaseManifestR926.GAME_CODE, policy});
            while (c.moveToNext()) {
                String issue = safe(c.getString(0));
                if (start.length() == 0) start = issue;
                end = issue;
                available++;
                boolean chosen = c.getInt(1) == 1;
                if (!chosen) continue;
                selected++;
                if (c.isNull(3)) continue;
                int hit = c.getInt(3);
                if (hit != 0 && hit != 1) continue;
                settledSelected++;
                hits += hit;
                double p = c.getDouble(2);
                if (finiteProbability(p)) {
                    calibrated++;
                    double e = p - hit;
                    squared += e * e;
                }
            }
        } finally {
            if (c != null) c.close();
        }
        double coverage = available == 0 ? Double.NaN : selected / (double) available;
        double hitRate = settledSelected == 0 ? Double.NaN : hits / (double) settledSelected;
        double brier = calibrated == 0 ? Double.NaN : squared / calibrated;
        double risk = settledSelected == 0
                ? Double.NaN
                : (1.0d - hitRate) + (Double.isNaN(brier) ? 0.0d : brier);
        return new PolicyMetrics(
                policy,
                start,
                end,
                available,
                selected,
                settledSelected,
                calibrated,
                hits,
                coverage,
                hitRate,
                brier,
                risk,
                0,
                "coverage=SELECTED/AVAILABLE|hit_rate=HITS/SETTLED_SELECTED|brier=CALIBRATED_SELECTED");
    }

    public static String freezePolicyMetricsWithinTransaction(
            SQLiteDatabase db, String policySha256) {
        requireTransaction(db, "R930_POLICY_METRIC_TRANSACTION_REQUIRED");
        PolicyMetrics m = policyMetrics(db, policySha256);
        String metricId = V51ChecksumEngine.sha256("D09_POLICY_METRIC|" + m.canonical());
        ContentValues cv = new ContentValues();
        cv.put("metric_id", metricId);
        cv.put("policy_sha256", m.policySha256);
        cv.put("start_issue", m.startIssue);
        cv.put("end_issue", m.endIssue);
        cv.put("available_n", m.availableN);
        cv.put("selected_n", m.selectedN);
        cv.put("settled_selected_n", m.settledSelectedN);
        cv.put("calibrated_n", m.calibratedN);
        cv.put("hits", m.hits);
        cv.put("coverage", m.coverage);
        cv.put("hit_rate", m.hitRate);
        cv.put("brier", m.brier);
        cv.put("risk", m.risk);
        cv.put("denominator", m.denominator);
        cv.put("frequency_debt", 0);
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(
                Batch34SchemaR930.TABLE_POLICY_METRIC,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE);
        return metricId;
    }

    private static void persistPolicyDecision(
            SQLiteDatabase db,
            String issue,
            SelectedObservation observation,
            String canonicalHash) {
        String policyHash;
        try {
            policyHash = safe(SelectionEvidenceContractR929.installedPolicy().hash);
        } catch (Throwable ignored) {
            policyHash = V51ChecksumEngine.sha256("R930_POLICY_UNAVAILABLE");
        }
        boolean selected = "REAL".equalsIgnoreCase(observation.lane)
                || "BET".equalsIgnoreCase(observation.action)
                || "REAL".equalsIgnoreCase(observation.action);
        ContentValues cv = new ContentValues();
        cv.put("game_code", ReleaseManifestR926.GAME_CODE);
        cv.put("target_issue", issue);
        cv.put("policy_sha256", policyHash);
        cv.put("predictability", finiteProbability(observation.predictability)
                ? observation.predictability : -1.0d);
        cv.put("selected_hit_p", finiteProbability(observation.selectedHitP)
                ? observation.selectedHitP : -1.0d);
        cv.put("lane", observation.lane);
        cv.put("selected", selected ? 1 : 0);
        cv.put("decision_action", observation.action);
        cv.put("reject_code", observation.rejectCode);
        cv.put("canonical_hash", canonicalHash);
        cv.put("created_at", System.currentTimeMillis());
        long row = db.insertWithOnConflict(
                Batch34SchemaR930.TABLE_POLICY_DECISION,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE);
        if (row < 0L && !policyDecisionMatches(db, issue, policyHash, canonicalHash)) {
            throw new IllegalStateException("R930_POLICY_DECISION_IMMUTABLE_CONFLICT");
        }
    }

    private static boolean policyDecisionMatches(
            SQLiteDatabase db, String issue, String policyHash, String canonicalHash) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT policy_sha256,canonical_hash FROM "
                            + Batch34SchemaR930.TABLE_POLICY_DECISION
                            + " WHERE game_code=? AND target_issue=?",
                    new String[] {ReleaseManifestR926.GAME_CODE, issue});
            return c.moveToFirst()
                    && policyHash.equals(safe(c.getString(0)))
                    && canonicalHash.equals(safe(c.getString(1)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static void persistFailureCase(
            SQLiteDatabase db,
            String issue,
            String settlementHash,
            String drawHash,
            String status) {
        Cursor c = null;
        String selectedAction = "";
        String frozenInput = "";
        try {
            c = db.rawQuery(
                    "SELECT opportunity_sha256,frozen_payload_hash,lane,decision_action FROM "
                            + Batch34SchemaR930.TABLE_CAPTURE
                            + " WHERE game_code=? AND target_issue=? AND frozen=1",
                    new String[] {ReleaseManifestR926.GAME_CODE, issue});
            if (!c.moveToFirst()) return;
            selectedAction = normalizeHash("SELECTED_ACTION", c.getString(0));
            frozenInput = normalizeHash("FROZEN_INPUT", c.getString(1));
            String lane = safe(c.getString(2));
            String action = safe(c.getString(3));
            if (!"REAL".equalsIgnoreCase(lane)
                    && !"BET".equalsIgnoreCase(action)
                    && !"REAL".equalsIgnoreCase(action)) return;
        } finally {
            if (c != null) c.close();
        }
        String actual = V51ChecksumEngine.sha256(
                "ACTUAL|" + drawHash + "|" + settlementHash + "|" + safe(status));
        String canonical = issue + "|" + selectedAction + "|" + frozenInput + "|" + actual
                + "|REAL_SELECTED_MISS";
        String caseHash = V51ChecksumEngine.sha256("C09_FAILURE_CASE|" + canonical);
        ContentValues cv = new ContentValues();
        cv.put("case_sha256", caseHash);
        cv.put("target_issue", issue);
        cv.put("selected_action_sha256", selectedAction);
        cv.put("frozen_input_sha256", frozenInput);
        cv.put("actual_outcome_sha256", actual);
        cv.put("failure_class", "REAL_SELECTED_MISS");
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(
                Batch34SchemaR930.TABLE_FAILURE_CASE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void transition(
            SQLiteDatabase db,
            String issue,
            String from,
            String to,
            int before,
            int after,
            String reason,
            String payloadHash) {
        String transitionId = V51ChecksumEngine.sha256(
                "C04_TRANSITION|" + ReleaseManifestR926.GAME_CODE + "|" + issue + "|"
                        + from + "|" + to + "|" + before + "|" + after + "|" + payloadHash);
        ContentValues event = new ContentValues();
        event.put("transition_id", transitionId);
        event.put("game_code", ReleaseManifestR926.GAME_CODE);
        event.put("target_issue", issue);
        event.put("from_state", from);
        event.put("to_state", to);
        event.put("sequence_before", before);
        event.put("sequence_after", after);
        event.put("reason", safe(reason));
        event.put("payload_hash", normalizeHash("TRANSITION_PAYLOAD", payloadHash));
        event.put("created_at", System.currentTimeMillis());
        long row = db.insertWithOnConflict(
                Batch34SchemaR930.TABLE_CAPTURE_TRANSITION,
                null,
                event,
                SQLiteDatabase.CONFLICT_IGNORE);
        if (row < 0L) throw new IllegalStateException("R930_CAPTURE_TRANSITION_DUPLICATE");

        ContentValues projection = new ContentValues();
        projection.put("current_state", to);
        projection.put("sequence", after);
        projection.put("updated_at", System.currentTimeMillis());
        if ("ELIGIBLE".equals(to)) projection.put("eligible", 1);
        if ("SELECTED".equals(to)) projection.put("selected", 1);
        if ("EXECUTION_STARTED".equals(to)) projection.put("execution_started", 1);
        if ("FROZEN".equals(to)) projection.put("frozen", 1);
        if ("FAILED".equals(to)) {
            projection.put("failed", 1);
            projection.put("last_failure", safe(reason));
        }
        int updated = db.update(
                Batch34SchemaR930.TABLE_CAPTURE,
                projection,
                "game_code=? AND target_issue=? AND current_state=? AND sequence=?",
                new String[] {
                    ReleaseManifestR926.GAME_CODE,
                    issue,
                    from,
                    String.valueOf(before)
                });
        if (updated != 1) throw new IllegalStateException(
                "R930_CAPTURE_CAS_FAILED|" + from + "|" + to + "|" + before);
    }

    private static ExistingCapture readCapture(SQLiteDatabase db, String issue) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT current_state,sequence,canonical_hash,frozen_payload_hash FROM "
                            + Batch34SchemaR930.TABLE_CAPTURE
                            + " WHERE game_code=? AND target_issue=?",
                    new String[] {ReleaseManifestR926.GAME_CODE, issue});
            return c.moveToFirst()
                    ? new ExistingCapture(c.getString(0), c.getInt(1), c.getString(2), c.getString(3))
                    : null;
        } finally {
            if (c != null) c.close();
        }
    }

    private static SelectedObservation readSelectedObservation(SQLiteDatabase db, String issue) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT lane,decision_action,reject_code,selected_hit_p,predictability,"
                            + "record_hash,pre_draw_frozen FROM "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {issue});
            if (!c.moveToFirst()) {
                return new SelectedObservation(false, "", "", "", -1.0d, -1.0d, "", 0);
            }
            return new SelectedObservation(
                    true,
                    c.getString(0),
                    c.getString(1),
                    c.getString(2),
                    c.getDouble(3),
                    c.getDouble(4),
                    c.getString(5),
                    c.getInt(6));
        } finally {
            if (c != null) c.close();
        }
    }

    private static int hitLabel(SQLiteDatabase db, String issue, String status) {
        String upper = safe(status).toUpperCase(Locale.US);
        if (upper.contains("WIN") || upper.contains("HIT")) return 1;
        if (upper.contains("LOSS") || upper.contains("MISS")) return 0;
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT hit_label FROM " + DbHelper.TABLE_R35_OOS_LABEL
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {issue});
            if (c.moveToFirst()) {
                int hit = c.getInt(0);
                return hit == 0 || hit == 1 ? hit : -1;
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        return -1;
    }

    private static void appendEvidence(
            SQLiteDatabase db,
            String issue,
            String type,
            String payloadHash,
            String detail) {
        String normalized = normalizeHash(type, payloadHash);
        String evidenceId = V51ChecksumEngine.sha256(
                "R930_EVIDENCE|" + ReleaseManifestR926.GAME_CODE + "|" + safe(issue)
                        + "|" + safe(type) + "|" + normalized);
        ContentValues cv = new ContentValues();
        cv.put("evidence_id", evidenceId);
        cv.put("game_code", ReleaseManifestR926.GAME_CODE);
        cv.put("target_issue", safe(issue));
        cv.put("evidence_type", safe(type));
        cv.put("owner", CAPTURE_SOURCE);
        cv.put("payload_hash", normalized);
        cv.put("detail", trim(detail, 1200));
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(
                Batch34SchemaR930.TABLE_EVIDENCE_LINK,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static CaptureResult failure(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            String reason,
            String detail) {
        String issue = request == null ? "" : safe(request.targetIssue);
        String hash = V51ChecksumEngine.sha256(
                "R930_CAPTURE_FAILURE|" + issue + "|" + safe(reason) + "|" + safe(detail));
        appendRuntimeEvent(
                db,
                request,
                "BATCH34_CAPTURE",
                "CAPTURE_AUDIT_FAILED",
                "reason=" + safe(reason) + "|detail=" + trim(detail, 700)
                        + "|evidence_hash=" + hash,
                "BATCH34_CAPTURE_AUDIT_FAILED|" + issue + "|" + hash);
        return new CaptureResult(false, false, issue, "FAILED", 0, reason, hash);
    }

    private static void appendRuntimeEvent(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            String type,
            String stage,
            String payload,
            String dedup) {
        appendRuntimeEvent(
                db,
                request == null ? "" : request.targetIssue,
                request == null ? "" : String.valueOf(request.generation),
                request == null ? "" : request.attemptId(),
                type,
                stage,
                RuntimeEventLedgerR926.RANK_FROZEN,
                payload,
                dedup);
    }

    private static void appendRuntimeEvent(
            SQLiteDatabase db,
            String issue,
            String epoch,
            String attempt,
            String type,
            String stage,
            int rank,
            String payload,
            String dedup) {
        if (db == null || safe(issue).length() == 0) return;
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        safe(epoch),
                        safe(attempt),
                        type,
                        stage,
                        rank,
                        trim(payload, 1600),
                        CAPTURE_SOURCE,
                        safe(dedup)));
    }

    private static boolean fastSchemaReady(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT revision,contract_hash,state FROM " + Batch34SchemaR930.TABLE_META
                            + " WHERE singleton=1",
                    null);
            return c.moveToFirst()
                    && Batch34SchemaR930.REVISION.equals(safe(c.getString(0)))
                    && Batch34SchemaR930.CONTRACT_HASH.equals(safe(c.getString(1)))
                    && "READY".equals(safe(c.getString(2)));
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    private static void rollbackSavepoint(SQLiteDatabase db, String name) {
        try {
            db.execSQL("ROLLBACK TO " + name);
        } catch (Throwable ignored) {
        }
        try {
            db.execSQL("RELEASE " + name);
        } catch (Throwable ignored) {
        }
    }

    private static void requireTransaction(SQLiteDatabase db, String code) {
        if (db == null || !db.inTransaction()) throw new IllegalStateException(code);
    }

    private static String normalizeHash(String domain, String value) {
        String x = safe(value);
        if (Batch34EvidenceContractR929.isSha256(x)) return x.toLowerCase(Locale.US);
        return V51ChecksumEngine.sha256(safe(domain) + "|" + x);
    }

    private static boolean finiteProbability(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static String finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value)
                ? "UNKNOWN"
                : String.format(Locale.US, "%.12g", value);
    }

    private static String trim(String value, int max) {
        String x = safe(value);
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
