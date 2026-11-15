package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable denominator for controlled-action frequency.
 *
 * <p>One row is written for every R922 canonical prediction, including HOLD/SHADOW.  Fifteen
 * percent is an observation target only.  It is never a quota, debt, chase instruction, or input
 * to {@link DecisionGateEngine}.  Gate changes require settled future-OOS non-inferiority evidence
 * and are deliberately outside this writer.</p>
 */
public final class ControlledCandidateOpportunityAuditR922 {
    public static final String VERSION = "R922_CONTROLLED_OPPORTUNITY_AUDIT_V1";
    public static final String TABLE = "v57_controlled_opportunity_r922";
    public static final double OBSERVATION_TARGET = 0.15d;
    public static final int DEFAULT_WINDOW = 120;

    private ControlledCandidateOpportunityAuditR922() {}

    public static void ensureSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R922_OPPORTUNITY_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE + "("
                        + "target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,runtime_version TEXT NOT NULL,"
                        + "gate_eligible INTEGER NOT NULL,gate_code TEXT NOT NULL,gate_detail TEXT NOT NULL,"
                        + "governance_lane TEXT NOT NULL,governance_candidate TEXT NOT NULL,"
                        + "governance_status TEXT NOT NULL,governance_weight REAL NOT NULL,"
                        + "governance_applied INTEGER NOT NULL,controlled_influence_eligible INTEGER NOT NULL,"
                        + "authorized_action TEXT NOT NULL,authorized_reject TEXT NOT NULL,"
                        + "position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,"
                        + "raw_action_p REAL NOT NULL,third_margin REAL NOT NULL,action_stability REAL NOT NULL,"
                        + "entropy REAL NOT NULL,assignment_support REAL NOT NULL,falsifier REAL NOT NULL,"
                        + "rank_retention REAL NOT NULL,third_robustness REAL NOT NULL,margin_robustness REAL NOT NULL,"
                        + "observation_target REAL NOT NULL CHECK(observation_target=0.15),"
                        + "record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)" );
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r922_opportunity_window ON " + TABLE
                        + "(runtime_version,created_at DESC,target_issue DESC)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r922_opportunity_reject ON " + TABLE
                        + "(runtime_version,gate_code,authorized_action)");
    }

    public static Opportunity from(
            String baseIssue,
            String targetIssue,
            V51Models.SelectedActionEnvelope selected,
            V51RankStabilityEngine.Result stability,
            V51CandidateRealGateR915.Evaluation gate,
            V51Models.AuthorizedDecision authorized,
            String governanceLane,
            String governanceCandidate,
            String governanceStatus,
            double governanceWeight,
            boolean governanceApplied,
            boolean controlledInfluenceEligible,
            long createdAt) {
        V51Models.PositionAction action = selected == null ? null : selected.action;
        double assignment = action != null && action.metaFeatures != null
                && action.metaFeatures.length > 15 ? finite(action.metaFeatures[15], -1.0d) : -1.0d;
        Opportunity out = new Opportunity(
                safe(baseIssue), safe(targetIssue), AppConfig.R36_RUNTIME_REPAIR_VERSION,
                gate != null && gate.eligible, gate == null ? "NOT_EVALUATED" : gate.code,
                gate == null ? "NO_GATE_DETAIL" : gate.detail,
                safe(governanceLane), safe(governanceCandidate), safe(governanceStatus),
                finite(governanceWeight, 0.0d), governanceApplied, controlledInfluenceEligible,
                authorized == null ? "HOLD" : authorized.action.name(),
                authorized == null ? "AUTHORIZATION_NULL" : authorized.rejectCode.name(),
                action == null ? 0 : action.position, action == null ? 0 : action.n1,
                action == null ? 0 : action.n2, action == null ? 0 : action.n3,
                action == null ? -1.0d : action.rawActionP,
                action == null ? -1.0d : action.thirdMargin,
                action == null ? -1.0d : action.stability,
                action == null ? -1.0d : action.entropy,
                assignment,
                selected == null ? -1.0d : selected.falsifierScore,
                stability == null ? -1.0d : stability.retention,
                stability == null ? -1.0d : stability.thirdRobustness,
                stability == null ? -1.0d : stability.rankMarginRobustness,
                Math.max(1L, createdAt), "");
        return out.withHash(V51ChecksumEngine.sha256(out.canonicalWithoutHash()));
    }

    static void insertWithinTransaction(SQLiteDatabase db, Opportunity opportunity) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R922_OPPORTUNITY_CANONICAL_TRANSACTION_REQUIRED");
        if (opportunity == null || !opportunity.valid())
            throw new IllegalArgumentException("R922_OPPORTUNITY_INVALID");
        Cursor existing = null;
        try {
            existing = db.rawQuery(
                    "SELECT record_hash FROM " + TABLE + " WHERE target_issue=? LIMIT 1",
                    new String[] {opportunity.targetIssue});
            if (existing.moveToFirst()) {
                if (!opportunity.recordHash.equals(safe(existing.getString(0))))
                    throw new IllegalStateException("R922_OPPORTUNITY_FIRST_WRITE_CONFLICT");
                return;
            }
        } finally {
            if (existing != null) existing.close();
        }
        ContentValues v = new ContentValues();
        v.put("target_issue", opportunity.targetIssue);
        v.put("base_issue", opportunity.baseIssue);
        v.put("runtime_version", opportunity.runtimeVersion);
        v.put("gate_eligible", opportunity.gateEligible ? 1 : 0);
        v.put("gate_code", opportunity.gateCode);
        v.put("gate_detail", opportunity.gateDetail);
        v.put("governance_lane", opportunity.governanceLane);
        v.put("governance_candidate", opportunity.governanceCandidate);
        v.put("governance_status", opportunity.governanceStatus);
        v.put("governance_weight", opportunity.governanceWeight);
        v.put("governance_applied", opportunity.governanceApplied ? 1 : 0);
        v.put("controlled_influence_eligible", opportunity.controlledInfluenceEligible ? 1 : 0);
        v.put("authorized_action", opportunity.authorizedAction);
        v.put("authorized_reject", opportunity.authorizedReject);
        v.put("position", opportunity.position);
        v.put("n1", opportunity.n1);
        v.put("n2", opportunity.n2);
        v.put("n3", opportunity.n3);
        v.put("raw_action_p", opportunity.rawActionP);
        v.put("third_margin", opportunity.thirdMargin);
        v.put("action_stability", opportunity.actionStability);
        v.put("entropy", opportunity.entropy);
        v.put("assignment_support", opportunity.assignmentSupport);
        v.put("falsifier", opportunity.falsifier);
        v.put("rank_retention", opportunity.rankRetention);
        v.put("third_robustness", opportunity.thirdRobustness);
        v.put("margin_robustness", opportunity.marginRobustness);
        v.put("observation_target", OBSERVATION_TARGET);
        v.put("record_hash", opportunity.recordHash);
        v.put("created_at", opportunity.createdAt);
        db.insertOrThrow(TABLE, null, v);
    }

    public static Report capture(DbStore store, int requestedWindow) {
        if (store == null) return Report.readError(requestedWindow, "STORE_NULL");
        Cursor cursor = null;
        try {
            int window = Math.max(1, Math.min(1200, requestedWindow));
            cursor = store.readable().rawQuery(
                    "SELECT target_issue,base_issue,runtime_version,gate_eligible,gate_code,gate_detail,"
                            + "governance_lane,governance_candidate,governance_status,governance_weight,"
                            + "governance_applied,controlled_influence_eligible,authorized_action,authorized_reject,"
                            + "position,n1,n2,n3,raw_action_p,third_margin,action_stability,entropy,"
                            + "assignment_support,falsifier,rank_retention,third_robustness,margin_robustness,"
                            + "created_at,record_hash FROM " + TABLE
                            + " WHERE runtime_version=? ORDER BY created_at DESC,target_issue DESC LIMIT ?",
                    new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION, String.valueOf(window)});
            ArrayList<Opportunity> rows = new ArrayList<Opportunity>();
            while (cursor.moveToNext()) {
                rows.add(new Opportunity(
                        cursor.getString(1), cursor.getString(0), cursor.getString(2),
                        cursor.getInt(3) == 1, cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getDouble(9),
                        cursor.getInt(10) == 1, cursor.getInt(11) == 1,
                        cursor.getString(12), cursor.getString(13), cursor.getInt(14), cursor.getInt(15),
                        cursor.getInt(16), cursor.getInt(17), cursor.getDouble(18), cursor.getDouble(19),
                        cursor.getDouble(20), cursor.getDouble(21), cursor.getDouble(22), cursor.getDouble(23),
                        cursor.getDouble(24), cursor.getDouble(25), cursor.getDouble(26), cursor.getLong(27),
                        cursor.getString(28)));
            }
            return evaluate(rows.toArray(new Opportunity[rows.size()]), window);
        } catch (Throwable failure) {
            return Report.readError(requestedWindow,
                    failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Pure host-testable funnel aggregation. */
    public static Report evaluate(Opportunity[] opportunities, int requestedWindow) {
        Opportunity[] rows = opportunities == null ? new Opportunity[0] : opportunities;
        int total = 0, eligible = 0, bets = 0, governed = 0, certifiedInfluence = 0, invalid = 0;
        LinkedHashMap<String, Integer> rejects = new LinkedHashMap<String, Integer>();
        String latestTarget = "";
        for (int i = 0; i < rows.length; i++) {
            Opportunity row = rows[i];
            if (row == null || !row.valid()) {
                invalid++;
                increment(rejects, "AUDIT_ROW_INVALID");
                continue;
            }
            total++;
            if (latestTarget.length() == 0) latestTarget = row.targetIssue;
            if (row.gateEligible) eligible++;
            if (row.governanceApplied) governed++;
            if (row.controlledInfluenceEligible) certifiedInfluence++;
            if ("BET".equals(row.authorizedAction)) bets++;
            else increment(rejects, rejectionBucket(row));
        }
        double frequency = total <= 0 ? -1.0d : ((double) bets) / total;
        String targetState = total <= 0 ? "NO_SAMPLE"
                : frequency >= OBSERVATION_TARGET ? "TARGET_OBSERVED"
                : "BELOW_TARGET_OBSERVED_DO_NOT_FORCE";
        return new Report("OK", Math.max(1, requestedWindow), total, eligible, bets, governed,
                certifiedInfluence, invalid, frequency, targetState, latestTarget, rejects, "");
    }

    private static String rejectionBucket(Opportunity row) {
        if (row.gateCode.length() > 0 && !row.gateEligible) return "GATE_" + row.gateCode;
        if (row.governanceApplied && !row.controlledInfluenceEligible)
            return "RESEARCH_GOVERNED_SHADOW_ONLY";
        if (row.authorizedReject.length() > 0 && !"NONE".equals(row.authorizedReject))
            return "DECISION_" + row.authorizedReject;
        return "SHADOW_OR_HOLD";
    }

    private static void increment(Map<String, Integer> map, String key) {
        Integer old = map.get(key);
        map.put(key, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
    }

    private static double finite(double value, double fallback) {
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Opportunity {
        public final String baseIssue, targetIssue, runtimeVersion, gateCode, gateDetail;
        public final String governanceLane, governanceCandidate, governanceStatus;
        public final String authorizedAction, authorizedReject, recordHash;
        public final boolean gateEligible, governanceApplied, controlledInfluenceEligible;
        public final int position, n1, n2, n3;
        public final double governanceWeight, rawActionP, thirdMargin, actionStability, entropy;
        public final double assignmentSupport, falsifier, rankRetention, thirdRobustness, marginRobustness;
        public final long createdAt;

        public Opportunity(
                String baseIssue, String targetIssue, String runtimeVersion, boolean gateEligible,
                String gateCode, String gateDetail, String governanceLane, String governanceCandidate,
                String governanceStatus, double governanceWeight, boolean governanceApplied,
                boolean controlledInfluenceEligible, String authorizedAction, String authorizedReject,
                int position, int n1, int n2, int n3, double rawActionP, double thirdMargin,
                double actionStability, double entropy, double assignmentSupport, double falsifier,
                double rankRetention, double thirdRobustness, double marginRobustness,
                long createdAt, String recordHash) {
            this.baseIssue = safe(baseIssue);
            this.targetIssue = safe(targetIssue);
            this.runtimeVersion = safe(runtimeVersion);
            this.gateEligible = gateEligible;
            this.gateCode = safe(gateCode);
            this.gateDetail = safe(gateDetail);
            this.governanceLane = safe(governanceLane);
            this.governanceCandidate = safe(governanceCandidate);
            this.governanceStatus = safe(governanceStatus);
            this.governanceWeight = finite(governanceWeight, 0.0d);
            this.governanceApplied = governanceApplied;
            this.controlledInfluenceEligible = controlledInfluenceEligible;
            this.authorizedAction = safe(authorizedAction);
            this.authorizedReject = safe(authorizedReject);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.rawActionP = finite(rawActionP, -1.0d);
            this.thirdMargin = finite(thirdMargin, -1.0d);
            this.actionStability = finite(actionStability, -1.0d);
            this.entropy = finite(entropy, -1.0d);
            this.assignmentSupport = finite(assignmentSupport, -1.0d);
            this.falsifier = finite(falsifier, -1.0d);
            this.rankRetention = finite(rankRetention, -1.0d);
            this.thirdRobustness = finite(thirdRobustness, -1.0d);
            this.marginRobustness = finite(marginRobustness, -1.0d);
            this.createdAt = createdAt;
            this.recordHash = safe(recordHash);
        }

        Opportunity withHash(String hash) {
            return new Opportunity(baseIssue, targetIssue, runtimeVersion, gateEligible, gateCode,
                    gateDetail, governanceLane, governanceCandidate, governanceStatus,
                    governanceWeight, governanceApplied, controlledInfluenceEligible,
                    authorizedAction, authorizedReject, position, n1, n2, n3, rawActionP,
                    thirdMargin, actionStability, entropy, assignmentSupport, falsifier,
                    rankRetention, thirdRobustness, marginRobustness, createdAt, hash);
        }

        String canonicalWithoutHash() {
            return VERSION + '|' + baseIssue + '|' + targetIssue + '|' + runtimeVersion + '|'
                    + (gateEligible ? 1 : 0) + '|' + gateCode + '|' + gateDetail + '|'
                    + governanceLane + '|' + governanceCandidate + '|' + governanceStatus + '|'
                    + governanceWeight + '|' + (governanceApplied ? 1 : 0) + '|'
                    + (controlledInfluenceEligible ? 1 : 0) + '|' + authorizedAction + '|'
                    + authorizedReject + '|' + position + '|' + n1 + '|' + n2 + '|' + n3 + '|'
                    + rawActionP + '|' + thirdMargin + '|' + actionStability + '|' + entropy + '|'
                    + assignmentSupport + '|' + falsifier + '|' + rankRetention + '|'
                    + thirdRobustness + '|' + marginRobustness + '|' + OBSERVATION_TARGET + '|'
                    + createdAt;
        }

        public boolean valid() {
            return TargetIssueResolver.exact(baseIssue, targetIssue).valid
                    && runtimeVersion.length() > 0
                    && gateCode.length() > 0
                    && governanceLane.length() > 0
                    && ("BET".equals(authorizedAction) || "SHADOW".equals(authorizedAction)
                            || "HOLD".equals(authorizedAction))
                    && position >= 1 && position <= 10
                    && DecisionGateEngine.b12K3(n1, n2, n3)
                    && createdAt > 0L
                    && recordHash.length() == 64;
        }
    }

    public static final class Report {
        public final String readStatus, targetState, latestTarget, error;
        public final int requestedWindow, opportunities, eligible, bets, governed;
        public final int certifiedInfluence, invalidRows;
        public final double frequency;
        public final Map<String, Integer> rejectDistribution;

        Report(String readStatus, int requestedWindow, int opportunities, int eligible, int bets,
                int governed, int certifiedInfluence, int invalidRows, double frequency,
                String targetState, String latestTarget, Map<String, Integer> rejects, String error) {
            this.readStatus = safe(readStatus);
            this.requestedWindow = Math.max(1, requestedWindow);
            this.opportunities = Math.max(0, opportunities);
            this.eligible = Math.max(0, eligible);
            this.bets = Math.max(0, bets);
            this.governed = Math.max(0, governed);
            this.certifiedInfluence = Math.max(0, certifiedInfluence);
            this.invalidRows = Math.max(0, invalidRows);
            this.frequency = frequency;
            this.targetState = safe(targetState);
            this.latestTarget = safe(latestTarget);
            this.rejectDistribution = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Integer>(rejects == null
                            ? Collections.<String, Integer>emptyMap() : rejects));
            this.error = safe(error);
        }

        static Report readError(int window, String error) {
            return new Report("READ_ERROR", window, 0, 0, 0, 0, 0, 0, -1.0d,
                    "READ_ERROR", "", new LinkedHashMap<String, Integer>(), error);
        }

        public String frequencyText() {
            return frequency < 0.0d ? "N/A"
                    : String.format(Locale.US, "%.2f%%", frequency * 100.0d);
        }

        public String summary() {
            return VERSION + "|window=" + requestedWindow + "|opportunities=" + opportunities
                    + "|gateEligible=" + eligible + "|BET=" + bets + "|frequency="
                    + frequencyText() + "|observationTarget=15.00%|targetState=" + targetState
                    + "|governed=" + governed + "|certifiedInfluence=" + certifiedInfluence
                    + "|invalidRows=" + invalidRows + "|rejects=" + rejectDistribution
                    + "|quota=FORBIDDEN|frequencyDebt=FORBIDDEN|gateOwner=DecisionGateEngine_ONLY"
                    + (error.length() == 0 ? "" : "|error=" + error);
        }
    }

public static Report captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int requestedWindow) {
        if (store == null) return Report.readError(requestedWindow, "STORE_NULL");
        Cursor cursor = null;
        try {
            int window = Math.max(1, Math.min(1200, requestedWindow));
            cursor = report.select(
                    "SELECT target_issue,base_issue,runtime_version,gate_eligible,gate_code,gate_detail,"
                            + "governance_lane,governance_candidate,governance_status,governance_weight,"
                            + "governance_applied,controlled_influence_eligible,authorized_action,authorized_reject,"
                            + "position,n1,n2,n3,raw_action_p,third_margin,action_stability,entropy,"
                            + "assignment_support,falsifier,rank_retention,third_robustness,margin_robustness,"
                            + "created_at,record_hash FROM " + TABLE
                            + " WHERE runtime_version=? ORDER BY created_at DESC,target_issue DESC LIMIT ?",
                    new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION, String.valueOf(window)});
            ArrayList<Opportunity> rows = new ArrayList<Opportunity>();
            while (cursor.moveToNext()) {
                rows.add(new Opportunity(
                        cursor.getString(1), cursor.getString(0), cursor.getString(2),
                        cursor.getInt(3) == 1, cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getDouble(9),
                        cursor.getInt(10) == 1, cursor.getInt(11) == 1,
                        cursor.getString(12), cursor.getString(13), cursor.getInt(14), cursor.getInt(15),
                        cursor.getInt(16), cursor.getInt(17), cursor.getDouble(18), cursor.getDouble(19),
                        cursor.getDouble(20), cursor.getDouble(21), cursor.getDouble(22), cursor.getDouble(23),
                        cursor.getDouble(24), cursor.getDouble(25), cursor.getDouble(26), cursor.getLong(27),
                        cursor.getString(28)));
            }
            return evaluateSnapshotCP4(report, rows.toArray(new Opportunity[rows.size()]), window);
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);} finally {
            if (cursor != null) cursor.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.trim();
    }

public static Report evaluateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Opportunity[] opportunities, int requestedWindow) {
        Opportunity[] rows = opportunities == null ? new Opportunity[0] : opportunities;
        int total = 0, eligible = 0, bets = 0, governed = 0, certifiedInfluence = 0, invalid = 0;
        LinkedHashMap<String, Integer> rejects = new LinkedHashMap<String, Integer>();
        String latestTarget = "";
        for (int i = 0; i < rows.length; i++) {
            Opportunity row = rows[i];
            if (row == null || !row.valid()) {
                invalid++;
                incrementSnapshotCP4(report, rejects, "AUDIT_ROW_INVALID");
                continue;
            }
            total++;
            if (latestTarget.length() == 0) latestTarget = row.targetIssue;
            if (row.gateEligible) eligible++;
            if (row.governanceApplied) governed++;
            if (row.controlledInfluenceEligible) certifiedInfluence++;
            if ("BET".equals(row.authorizedAction)) bets++;
            else incrementSnapshotCP4(report, rejects, rejectionBucketSnapshotCP4(report, row));
        }
        double frequency = total <= 0 ? -1.0d : ((double) bets) / total;
        String targetState = total <= 0 ? "NO_SAMPLE"
                : frequency >= OBSERVATION_TARGET ? "TARGET_OBSERVED"
                : "BELOW_TARGET_OBSERVED_DO_NOT_FORCE";
        return new Report("OK", Math.max(1, requestedWindow), total, eligible, bets, governed,
                certifiedInfluence, invalid, frequency, targetState, latestTarget, rejects, "");
    }

public static String rejectionBucketSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Opportunity row) {
        if (row.gateCode.length() > 0 && !row.gateEligible) return "GATE_" + row.gateCode;
        if (row.governanceApplied && !row.controlledInfluenceEligible)
            return "RESEARCH_GOVERNED_SHADOW_ONLY";
        if (row.authorizedReject.length() > 0 && !"NONE".equals(row.authorizedReject))
            return "DECISION_" + row.authorizedReject;
        return "SHADOW_OR_HOLD";
    }

public static void incrementSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Map<String, Integer> map, String key) {
        Integer old = map.get(key);
        map.put(key, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
    }
}
