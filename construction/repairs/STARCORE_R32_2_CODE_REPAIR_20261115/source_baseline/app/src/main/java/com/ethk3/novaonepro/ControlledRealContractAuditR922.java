package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only audit of every row that claims to be a real controlled action.
 *
 * <p>The old report incorrectly required the number of BET observations to be zero.  That made a
 * healthy controlled ledger fail as soon as it contained a genuine action.  R922 instead verifies
 * the exact contract of every claimed REAL row: realtime source, one position, fixed K3, one row
 * per target, R915 authorization, controlled lane, and formal exact calibration; legacy engineering rows are separately labelled.</p>
 */
public final class ControlledRealContractAuditR922 {
    public static final String VERSION = "R922_CONTROLLED_REAL_CONTRACT_AUDIT_V1";
    public static final String EXACT_CONTRACT_STATUS = "R915_CONTROLLED_V51_CONTRACT_VALID";
    public static final String FORMAL_EXACT_STATUS = "R974_CERTIFIED_EXACT_CALIBRATED_V1";

    private ControlledRealContractAuditR922() {}

    public static Report capture(DbStore store) {
        if (store == null) return Report.readError("STORE_NULL");
        Cursor cursor = null;
        try {
            SQLiteDatabase db = store.readable();
            cursor = db.rawQuery(
                    "SELECT target_issue,settled,action,stat_scope,source_type,is_realtime_real,"
                            + "authorization_valid,authorized_mode,authorization_id,contract_valid,"
                            + "contract_status,lane_candidate,position,n1,n2,n3,k3_mask,raw_p,"
                            + "calibrated_p,final_p,authorization_probability,"
                            + "EXISTS(SELECT 1 FROM " + V51Repository.AUTH + " a JOIN "
                            + V51Repository.SELECTED + " s ON s.decision_id=a.decision_id AND s.target_issue=a.target_issue"
                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o ON o.target_issue=a.target_issue"
                            + " WHERE a.target_issue=t.target_issue AND a.authorization_hash=t.authorization_id"
                            + " AND s.authorization_hash=t.authorization_id AND o.authorization_hash=t.authorization_id"
                            + " AND a.contract_hash=s.contract_hash AND a.contract_hash=o.contract_hash"
                            + " AND s.decision_action='BET' AND o.decision_action='BET' AND o.pre_draw_frozen=1"
                            + " AND o.position=t.position AND o.n1=t.n1 AND o.n2=t.n2 AND o.n3=t.n3"
                            + " AND s.position=t.position AND s.n1=t.n1 AND s.n2=t.n2 AND s.n3=t.n3),"
                            + "EXISTS(SELECT 1 FROM research_exact_probability_f05 q JOIN r974_exact_real_receipt e"
                            + " ON e.target_issue=q.target_issue AND e.trade_id=q.trade_id JOIN research_certificate_r929 c"
                            + " ON c.certificate_hash=q.certificate_hash JOIN research_candidate_cal_model_r932 m"
                            + " ON m.model_hash=q.calibration_hash JOIN research_calibration_quality_cp12 z"
                            + " ON z.study_hash=m.study_hash AND z.model_hash=m.model_hash WHERE q.game_code='"+V51Repository.GAME_CODE+"'"
                            + " AND q.target_issue=t.target_issue AND q.trade_id=t._id AND q.authorization_hash=t.authorization_id"
                            + " AND e.authorization_hash=q.authorization_hash AND e.candidate_id=q.candidate_id AND e.model_hash=q.model_hash"
                            + " AND e.certificate_hash=q.certificate_hash AND e.proof_hash=q.proof_hash AND e.action_hash=q.action_hash"
                            + " AND c.candidate_id=q.candidate_id AND c.model_hash=q.model_hash AND c.calibration_hash=q.calibration_hash"
                            + " AND c.selection_hash=q.selection_hash AND EXISTS(SELECT 1 FROM research_certificate_gate_r932 g WHERE g.certificate_hash=q.certificate_hash AND g.gate_id='EXACT_SELECTION_SCOPE' AND g.source='F05_EXACT_MODEL_SELECTION_POLICY' AND g.source_hash=q.scope_policy_hash AND g.pass=1) AND m.candidate_key='algorithm:'||q.candidate_id"
                            + " AND m.validation_until<q.target_issue AND m.frozen_at<q.captured_at AND q.captured_at<=q.committed_at"
                            + " AND z.state='CALIBRATION_QUALIFIED' AND q.selected_hit_p>0 AND q.selected_hit_p<1"
                            + " AND q.selected_hit_p=t.calibrated_p AND q.selected_hit_p=t.final_p"
                            + " AND q.selected_hit_p=t.authorization_probability) FROM "
                            + DbHelper.TABLE_TRADES
                            + " t WHERE action='BET' OR stat_scope='REAL_TRADE' OR is_realtime_real=1"
                            + " ORDER BY target_issue ASC,_id ASC",
                    null);
            java.util.ArrayList<Row> rows = new java.util.ArrayList<Row>();
            while (cursor.moveToNext()) {
                Row row = new Row(
                        safe(cursor.getString(0)), cursor.getInt(1), safe(cursor.getString(2)),
                        safe(cursor.getString(3)), safe(cursor.getString(4)), cursor.getInt(5),
                        cursor.getInt(6), safe(cursor.getString(7)), safe(cursor.getString(8)),
                        cursor.getInt(9), safe(cursor.getString(10)), safe(cursor.getString(11)),
                        cursor.getInt(12), cursor.getInt(13), cursor.getInt(14), cursor.getInt(15),
                        cursor.getInt(16), cursor.getDouble(17), cursor.getDouble(18),
                        cursor.getDouble(19), cursor.getDouble(20));
                row.durableEvidenceMatches = cursor.getInt(21) == 1;
                row.exactCertificateEvidenceMatches = cursor.getInt(22) == 1;
                rows.add(row);
            }
            return evaluate(rows.toArray(new Row[rows.size()]));
        } catch (Throwable failure) {
            return Report.readError(failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Pure host-testable contract evaluator. */
    public static Report evaluate(Row[] rows) {
        Row[] input = rows == null ? new Row[0] : rows;
        int total = 0, settled = 0, open = 0, invalid = 0, duplicates = 0;
        LinkedHashMap<String, Integer> reasons = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, Integer> targets = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < input.length; i++) {
            Row row = input[i];
            if (row == null) {
                invalid++;
                increment(reasons, "NULL_ROW");
                continue;
            }
            total++;
            if (row.settled == 1) settled++;
            else open++;
            Integer count = targets.get(row.targetIssue);
            targets.put(row.targetIssue, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            String reason = invalidReason(row);
            if (reason.length() > 0) {
                invalid++;
                increment(reasons, reason);
            }
        }
        for (Integer count : targets.values())
            if (count != null && count.intValue() > 1) duplicates += count.intValue() - 1;
        if (duplicates > 0) incrementBy(reasons, "DUPLICATE_TARGET", duplicates);
        boolean passed = invalid == 0 && duplicates == 0;
        // open>1 is reported by settlement backlog diagnostics, not a duplicate-game-target violation.
        String status = passed ? "PASS_EXACT_CONTROLLED_LEDGER" : "FAIL_CONTRACT";
        return new Report("OK", passed, total, settled, open, invalid, duplicates, reasons, status, "");
    }

    private static String invalidReason(Row r) {
        if (!TargetIssueResolver.isValid(r.targetIssue)) return "TARGET_INVALID";
        if (!"BET".equals(r.action)) return "ACTION_NOT_BET";
        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(r.statScope)) return "SCOPE_NOT_REAL_TRADE";
        if (!Models.SOURCE_TYPE_REALTIME.equals(r.sourceType)) return "SOURCE_NOT_REALTIME";
        if (r.isRealtimeReal != 1) return "REALTIME_FLAG_INVALID";
        if (r.authorizationValid != 1 || !"BET".equals(r.authorizedMode))
            return "AUTHORIZATION_MODE_INVALID";
        if (!r.authorizationId.matches("[0-9a-f]{64}")
                && !V51AuthorizationCore.isBetEnvelope(r.authorizationId)) return "AUTHORIZATION_ID_INVALID";
        if (!r.durableEvidenceMatches) return "AUTHORIZATION_EVIDENCE_MISMATCH";
        if (r.contractValid != 1 || (!EXACT_CONTRACT_STATUS.equals(r.contractStatus)&&!FORMAL_EXACT_STATUS.equals(r.contractStatus)))
            return "CONTRACT_STATUS_INVALID";
        if (!V51Enums.Lane.CONTROLLED_PROBE.name().equals(r.lane))
            return "LANE_NOT_CONTROLLED_PROBE";
        if (r.position < 1 || r.position > 10 || !DecisionGateEngine.b12K3(r.n1, r.n2, r.n3))
            return "FIXED_K3_INVALID";
        if (r.k3Mask != 0 && r.k3Mask != AppConfig.buildK3Mask(r.n1, r.n2, r.n3))
            return "K3_MASK_INVALID";
        if (!finite(r.rawP)) return "RAW_SCORE_INVALID";
        if (FORMAL_EXACT_STATUS.equals(r.contractStatus)) {
            if(!r.exactCertificateEvidenceMatches)return "EXACT_CALIBRATION_RECEIPT_MISMATCH";
            if(!CertifiedPredictionR974.validSelectedProbability(r.finalP)
                    ||Double.compare(r.calibratedP,r.finalP)!=0
                    ||Double.compare(r.authorizationProbability,r.finalP)!=0)
                return "EXACT_CALIBRATION_PROBABILITY_MISMATCH";
        } else {
            if(r.rawP<0||r.rawP>1)return "LEGACY_RAW_SCORE_INVALID";
            if (!zeroOrUnavailable(r.calibratedP)||!zeroOrUnavailable(r.finalP)||!zeroOrUnavailable(r.authorizationProbability))
                return "LEGACY_UNPROVEN_CALIBRATION_MUST_BE_ZERO";
        }
        if (r.settled != 0 && r.settled != 1) return "SETTLED_FLAG_INVALID";
        return "";
    }

    private static boolean zeroOrUnavailable(double value) {
        return Double.isNaN(value) || Math.abs(value) <= 1.0e-12d;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static void increment(Map<String, Integer> map, String key) {
        incrementBy(map, key, 1);
    }

    private static void incrementBy(Map<String, Integer> map, String key, int amount) {
        Integer old = map.get(key);
        map.put(key, Integer.valueOf((old == null ? 0 : old.intValue()) + Math.max(0, amount)));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Row {
        // evaluate(Row[]) is a pure field-contract test. capture() also checks persisted witnesses.
        private boolean durableEvidenceMatches = true;
        private boolean exactCertificateEvidenceMatches = false;
        public final String targetIssue, action, statScope, sourceType, authorizedMode;
        public final String authorizationId, contractStatus, lane;
        public final int settled, isRealtimeReal, authorizationValid, contractValid;
        public final int position, n1, n2, n3, k3Mask;
        public final double rawP, calibratedP, finalP, authorizationProbability;

        public Row(
                String targetIssue, int settled, String action, String statScope,
                String sourceType, int isRealtimeReal, int authorizationValid,
                String authorizedMode, String authorizationId, int contractValid,
                String contractStatus, String lane, int position, int n1, int n2, int n3,
                int k3Mask, double rawP, double calibratedP, double finalP,
                double authorizationProbability) {
            this.targetIssue = safe(targetIssue);
            this.settled = settled;
            this.action = safe(action);
            this.statScope = safe(statScope);
            this.sourceType = safe(sourceType);
            this.isRealtimeReal = isRealtimeReal;
            this.authorizationValid = authorizationValid;
            this.authorizedMode = safe(authorizedMode);
            this.authorizationId = safe(authorizationId);
            this.contractValid = contractValid;
            this.contractStatus = safe(contractStatus);
            this.lane = safe(lane);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.k3Mask = k3Mask;
            this.rawP = rawP;
            this.calibratedP = calibratedP;
            this.finalP = finalP;
            this.authorizationProbability = authorizationProbability;
        }
    }

    public static final class Report {
        public final String readStatus, status, error;
        public final boolean passed;
        public final int total, settled, open, invalid, duplicateTargets;
        public final Map<String, Integer> rejectReasons;

        Report(String readStatus, boolean passed, int total, int settled, int open,
                int invalid, int duplicateTargets, Map<String, Integer> reasons,
                String status, String error) {
            this.readStatus = safe(readStatus);
            this.passed = passed;
            this.total = Math.max(0, total);
            this.settled = Math.max(0, settled);
            this.open = Math.max(0, open);
            this.invalid = Math.max(0, invalid);
            this.duplicateTargets = Math.max(0, duplicateTargets);
            this.rejectReasons = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<String, Integer>(reasons == null
                            ? java.util.Collections.<String, Integer>emptyMap() : reasons));
            this.status = safe(status);
            this.error = safe(error);
        }

        static Report readError(String error) {
            return new Report("READ_ERROR", false, 0, 0, 0, 0, 0,
                    new LinkedHashMap<String, Integer>(), "READ_ERROR", error);
        }

        public String summary() {
            return VERSION + "|read=" + readStatus + "|status=" + status + "|total=" + total
                    + "|settled=" + settled + "|open=" + open + "|invalid=" + invalid
                    + "|duplicate=" + duplicateTargets + "|reasons=" + rejectReasons
                    + (error.length() == 0 ? "" : "|error=" + error)
                    + "|rawScoreIsCalibratedProbability=FALSE";
        }

        public String frequencyText(int opportunities) {
            if (opportunities <= 0) return "N/A";
            return String.format(Locale.US, "%.2f%%", 100.0d * total / opportunities);
        }
    }

public static Report captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return Report.readError("STORE_NULL");
        Cursor cursor = null;
        try {
            SQLiteDatabase db = store.readable();
            cursor = report.select(
                    "SELECT target_issue,settled,action,stat_scope,source_type,is_realtime_real,"
                            + "authorization_valid,authorized_mode,authorization_id,contract_valid,"
                            + "contract_status,lane_candidate,position,n1,n2,n3,k3_mask,raw_p,"
                            + "calibrated_p,final_p,authorization_probability,"
                            + "EXISTS(SELECT 1 FROM " + V51Repository.AUTH + " a JOIN "
                            + V51Repository.SELECTED + " s ON s.decision_id=a.decision_id AND s.target_issue=a.target_issue"
                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o ON o.target_issue=a.target_issue"
                            + " WHERE a.target_issue=t.target_issue AND a.authorization_hash=t.authorization_id"
                            + " AND s.authorization_hash=t.authorization_id AND o.authorization_hash=t.authorization_id"
                            + " AND a.contract_hash=s.contract_hash AND a.contract_hash=o.contract_hash"
                            + " AND s.decision_action='BET' AND o.decision_action='BET' AND o.pre_draw_frozen=1"
                            + " AND o.position=t.position AND o.n1=t.n1 AND o.n2=t.n2 AND o.n3=t.n3"
                            + " AND s.position=t.position AND s.n1=t.n1 AND s.n2=t.n2 AND s.n3=t.n3),"
                            + "EXISTS(SELECT 1 FROM research_exact_probability_f05 q JOIN r974_exact_real_receipt e"
                            + " ON e.target_issue=q.target_issue AND e.trade_id=q.trade_id JOIN research_certificate_r929 c"
                            + " ON c.certificate_hash=q.certificate_hash JOIN research_candidate_cal_model_r932 m"
                            + " ON m.model_hash=q.calibration_hash JOIN research_calibration_quality_cp12 z"
                            + " ON z.study_hash=m.study_hash AND z.model_hash=m.model_hash WHERE q.game_code='"+V51Repository.GAME_CODE+"'"
                            + " AND q.target_issue=t.target_issue AND q.trade_id=t._id AND q.authorization_hash=t.authorization_id"
                            + " AND e.authorization_hash=q.authorization_hash AND e.candidate_id=q.candidate_id AND e.model_hash=q.model_hash"
                            + " AND e.certificate_hash=q.certificate_hash AND e.proof_hash=q.proof_hash AND e.action_hash=q.action_hash"
                            + " AND c.candidate_id=q.candidate_id AND c.model_hash=q.model_hash AND c.calibration_hash=q.calibration_hash"
                            + " AND c.selection_hash=q.selection_hash AND EXISTS(SELECT 1 FROM research_certificate_gate_r932 g WHERE g.certificate_hash=q.certificate_hash AND g.gate_id='EXACT_SELECTION_SCOPE' AND g.source='F05_EXACT_MODEL_SELECTION_POLICY' AND g.source_hash=q.scope_policy_hash AND g.pass=1) AND m.candidate_key='algorithm:'||q.candidate_id"
                            + " AND m.validation_until<q.target_issue AND m.frozen_at<q.captured_at AND q.captured_at<=q.committed_at"
                            + " AND z.state='CALIBRATION_QUALIFIED' AND q.selected_hit_p>0 AND q.selected_hit_p<1"
                            + " AND q.selected_hit_p=t.calibrated_p AND q.selected_hit_p=t.final_p"
                            + " AND q.selected_hit_p=t.authorization_probability) FROM "
                            + DbHelper.TABLE_TRADES
                            + " t WHERE action='BET' OR stat_scope='REAL_TRADE' OR is_realtime_real=1"
                            + " ORDER BY target_issue ASC,_id ASC",
                    null);
            java.util.ArrayList<Row> rows = new java.util.ArrayList<Row>();
            while (cursor.moveToNext()) {
                Row row = new Row(
                        safeSnapshotCP4(report, cursor.getString(0)), cursor.getInt(1), safeSnapshotCP4(report, cursor.getString(2)),
                        safeSnapshotCP4(report, cursor.getString(3)), safeSnapshotCP4(report, cursor.getString(4)), cursor.getInt(5),
                        cursor.getInt(6), safeSnapshotCP4(report, cursor.getString(7)), safeSnapshotCP4(report, cursor.getString(8)),
                        cursor.getInt(9), safeSnapshotCP4(report, cursor.getString(10)), safeSnapshotCP4(report, cursor.getString(11)),
                        cursor.getInt(12), cursor.getInt(13), cursor.getInt(14), cursor.getInt(15),
                        cursor.getInt(16), cursor.getDouble(17), cursor.getDouble(18),
                        cursor.getDouble(19), cursor.getDouble(20));
                row.durableEvidenceMatches = cursor.getInt(21) == 1;
                row.exactCertificateEvidenceMatches = cursor.getInt(22) == 1;
                rows.add(row);
            }
            return evaluateSnapshotCP4(report, rows.toArray(new Row[rows.size()]));
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);} finally {
            if (cursor != null) cursor.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.trim();
    }

public static Report evaluateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Row[] rows) {
        Row[] input = rows == null ? new Row[0] : rows;
        int total = 0, settled = 0, open = 0, invalid = 0, duplicates = 0;
        LinkedHashMap<String, Integer> reasons = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, Integer> targets = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < input.length; i++) {
            Row row = input[i];
            if (row == null) {
                invalid++;
                incrementSnapshotCP4(report, reasons, "NULL_ROW");
                continue;
            }
            total++;
            if (row.settled == 1) settled++;
            else open++;
            Integer count = targets.get(row.targetIssue);
            targets.put(row.targetIssue, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            String reason = invalidReasonSnapshotCP4(report, row);
            if (reason.length() > 0) {
                invalid++;
                incrementSnapshotCP4(report, reasons, reason);
            }
        }
        for (Integer count : targets.values())
            if (count != null && count.intValue() > 1) duplicates += count.intValue() - 1;
        if (duplicates > 0) incrementBySnapshotCP4(report, reasons, "DUPLICATE_TARGET", duplicates);
        boolean passed = invalid == 0 && duplicates == 0;
        // open>1 is reported by settlement backlog diagnostics, not a duplicate-game-target violation.
        String status = passed ? "PASS_EXACT_CONTROLLED_LEDGER" : "FAIL_CONTRACT";
        return new Report("OK", passed, total, settled, open, invalid, duplicates, reasons, status, "");
    }

public static void incrementBySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Map<String, Integer> map, String key, int amount) {
        Integer old = map.get(key);
        map.put(key, Integer.valueOf((old == null ? 0 : old.intValue()) + Math.max(0, amount)));
    }

public static void incrementSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Map<String, Integer> map, String key) {
        incrementBySnapshotCP4(report, map, key, 1);
    }

public static String invalidReasonSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Row r) {
        if (!TargetIssueResolver.isValid(r.targetIssue)) return "TARGET_INVALID";
        if (!"BET".equals(r.action)) return "ACTION_NOT_BET";
        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(r.statScope)) return "SCOPE_NOT_REAL_TRADE";
        if (!Models.SOURCE_TYPE_REALTIME.equals(r.sourceType)) return "SOURCE_NOT_REALTIME";
        if (r.isRealtimeReal != 1) return "REALTIME_FLAG_INVALID";
        if (r.authorizationValid != 1 || !"BET".equals(r.authorizedMode))
            return "AUTHORIZATION_MODE_INVALID";
        if (!r.authorizationId.matches("[0-9a-f]{64}")
                && !V51AuthorizationCore.isBetEnvelope(r.authorizationId)) return "AUTHORIZATION_ID_INVALID";
        if (!r.durableEvidenceMatches) return "AUTHORIZATION_EVIDENCE_MISMATCH";
        if (r.contractValid != 1 || (!EXACT_CONTRACT_STATUS.equals(r.contractStatus)&&!FORMAL_EXACT_STATUS.equals(r.contractStatus)))
            return "CONTRACT_STATUS_INVALID";
        if (!V51Enums.Lane.CONTROLLED_PROBE.name().equals(r.lane))
            return "LANE_NOT_CONTROLLED_PROBE";
        if (r.position < 1 || r.position > 10 || !DecisionGateEngine.b12K3(r.n1, r.n2, r.n3))
            return "FIXED_K3_INVALID";
        if (r.k3Mask != 0 && r.k3Mask != AppConfig.buildK3Mask(r.n1, r.n2, r.n3))
            return "K3_MASK_INVALID";
        if (!finite(r.rawP)) return "RAW_SCORE_INVALID";
        if (FORMAL_EXACT_STATUS.equals(r.contractStatus)) {
            if(!r.exactCertificateEvidenceMatches)return "EXACT_CALIBRATION_RECEIPT_MISMATCH";
            if(!CertifiedPredictionR974.validSelectedProbability(r.finalP)
                    ||Double.compare(r.calibratedP,r.finalP)!=0
                    ||Double.compare(r.authorizationProbability,r.finalP)!=0)
                return "EXACT_CALIBRATION_PROBABILITY_MISMATCH";
        } else {
            if(r.rawP<0||r.rawP>1)return "LEGACY_RAW_SCORE_INVALID";
            if (!zeroOrUnavailable(r.calibratedP)||!zeroOrUnavailable(r.finalP)||!zeroOrUnavailable(r.authorizationProbability))
                return "LEGACY_UNPROVEN_CALIBRATION_MUST_BE_ZERO";
        }
        if (r.settled != 0 && r.settled != 1) return "SETTLED_FLAG_INVALID";
        return "";
    }

public static boolean zeroOrUnavailableSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double value) {
        return Double.isNaN(value) || Math.abs(value) <= 1.0e-12d;
    }

public static boolean finiteSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
