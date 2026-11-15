package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Immediate, non-pausing failure analysis after two consecutive proven REAL misses.
 *
 * <p>It observes only settled REAL_TRADE rows whose realtime provenance survived the canonical
 * contract.  The resulting vector is research input for later generations; it cannot authorize,
 * suppress or retroactively alter a BET.</p>
 */
public final class MissStreakEscalationR923 {
    public static final String VERSION = "R923_TWO_MISS_RESEARCH_ESCALATION_V1";
    public static final int ESCALATE_AT = 2;

    private MissStreakEscalationR923() {}

    public static Result analyze(DbStore store) {
        if (store == null) return Result.error("STORE_NULL");
        Cursor c = null;
        try {
            ArrayList<Observation> newestFirst = new ArrayList<Observation>();
            c = store.readable().rawQuery(
                    "SELECT t.target_issue,t.hit,t.position,t.n1,t.n2,t.n3,t.actual_number,"
                            + "t.authorization_id,t.created_at,COALESCE(o.gate_code,''),"
                            + "COALESCE(o.gate_detail,''),COALESCE(o.raw_action_p,-1),"
                            + "COALESCE(o.third_margin,-1),COALESCE(o.action_stability,-1),"
                            + "COALESCE(o.entropy,-1),COALESCE(o.assignment_support,-1),"
                            + "COALESCE(o.falsifier,-1),COALESCE(p.attribution_summary,''),"
                            + "COALESCE(p.feedback_summary,''),t.source_type,t.is_realtime_real,"
                            + "t.contract_valid,t.authorization_valid,t.authorized_mode,t.contract_status,"
                            + "EXISTS(SELECT 1 FROM " + V51Repository.AUTH + " a JOIN "
                            + V51Repository.SELECTED + " s ON s.decision_id=a.decision_id AND s.target_issue=a.target_issue"
                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " f ON f.target_issue=a.target_issue"
                            + " WHERE a.target_issue=t.target_issue AND a.authorization_hash=t.authorization_id"
                            + " AND s.authorization_hash=t.authorization_id AND f.authorization_hash=t.authorization_id"
                            + " AND a.contract_hash=s.contract_hash AND a.contract_hash=f.contract_hash"
                            + " AND s.decision_action='BET' AND f.decision_action='BET' AND f.pre_draw_frozen=1"
                            + " AND s.position=t.position AND s.n1=t.n1 AND s.n2=t.n2 AND s.n3=t.n3"
                            + " AND f.position=t.position AND f.n1=t.n1 AND f.n2=t.n2 AND f.n3=t.n3)"
                            + " FROM " + DbHelper.TABLE_TRADES
                            + " t LEFT JOIN " + ControlledCandidateOpportunityAuditR922.TABLE
                            + " o ON o.target_issue=t.target_issue LEFT JOIN "
                            + LegacyPredrawContextBridgeR915.POST_TABLE
                            + " p ON p.target_issue=t.target_issue"
                            + " WHERE t.settled=1 AND t.action='BET' AND t.stat_scope='REAL_TRADE'"
                            + " ORDER BY t.target_issue DESC,t._id DESC LIMIT 24", null);
            while (c.moveToNext()) {
                boolean proven = Models.SOURCE_TYPE_REALTIME.equals(c.getString(19))
                        && c.getInt(20) == 1 && c.getInt(21) == 1 && c.getInt(22) == 1
                        && "BET".equals(c.getString(23))
                        && ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS.equals(c.getString(24)) && c.getInt(25)==1;
                if (!proven) { newestFirst.add(null); continue; }
                newestFirst.add(new Observation(
                        c.getString(0), c.isNull(1) ? -1 : c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4),
                        c.getInt(5), c.getInt(6), c.getString(7), c.getLong(8), c.getString(9),
                        c.getString(10), c.getDouble(11), c.getDouble(12), c.getDouble(13),
                        c.getDouble(14), c.getDouble(15), c.getDouble(16), c.getString(17),
                        c.getString(18)));
            }
            Result result = evaluate(newestFirst.toArray(new Observation[newestFirst.size()]));
            publish(store, result);
            return result;
        } catch (Throwable failure) {
            Result result = Result.error(
                    failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
            publish(store, result);
            return result;
        } finally {
            if (c != null) c.close();
        }
    }

    /** Pure newest-first aggregation used by the host contract harness. */
    public static Result evaluate(Observation[] observations) {
        Observation[] rows = observations == null ? new Observation[0] : observations;
        ArrayList<Observation> misses = new ArrayList<Observation>();
        int invalid = 0;
        for (int i = 0; i < rows.length; i++) {
            Observation row = rows[i];
            if (row == null || !row.valid()) {
                invalid++;
                // Unknown/corrupt evidence breaks the contiguous run; never join two misses
                // across an unverified intervening observation.
                break;
            }
            if (row.hit == 1) break;
            misses.add(row);
        }
        if (rows.length == 0)
            return new Result(true, "NO_PROVEN_REAL_SAMPLE", 0, invalid, "", "", "");
        if (misses.isEmpty() && invalid > 0)
            return new Result(true, "UNVERIFIED_LATEST_REAL_BOUNDARY", 0, invalid, "", "",
                    "最新REAL证据未通过，不能跨过它拼接连续未中。NO_PAUSE=TRUE");

        int streak = misses.size();
        if (streak < ESCALATE_AT) {
            String latest = streak == 0 ? firstValidIssue(rows) : misses.get(0).targetIssue;
            return new Result(true, "WATCH", streak, invalid, latest,
                    "REAL_MISS_STREAK=" + streak + "|escalateAt=" + ESCALATE_AT,
                    "未达到两次连续真实未中；保持逐期观察。NO_PAUSE=TRUE");
        }

        double raw = 0.0d, margin = 0.0d, stability = 0.0d, entropy = 0.0d;
        double assignment = 0.0d, falsifier = 0.0d;
        int rawN = 0, marginN = 0, stabilityN = 0, entropyN = 0, assignmentN = 0, falsifierN = 0;
        StringBuilder detail = new StringBuilder(2048);
        for (int i = 0; i < misses.size(); i++) {
            Observation row = misses.get(i);
            if (unit(row.rawActionP)) { raw += row.rawActionP; rawN++; }
            if (unit(row.thirdMargin)) { margin += row.thirdMargin; marginN++; }
            if (unit(row.actionStability)) { stability += row.actionStability; stabilityN++; }
            if (unit(row.entropy)) { entropy += row.entropy; entropyN++; }
            if (unit(row.assignmentSupport)) { assignment += row.assignmentSupport; assignmentN++; }
            if (unit(row.falsifier)) { falsifier += row.falsifier; falsifierN++; }
            detail.append(i + 1).append("|").append(row.targetIssue)
                    .append("|K3=").append(row.position).append(':').append(row.n1).append('/')
                    .append(row.n2).append('/').append(row.n3)
                    .append("|actual=").append(row.actualNumber)
                    .append("|gate=").append(explicit(row.gateCode, "N/A"))
                    .append("|raw/stability/entropy/falsifier=")
                    .append(number(row.rawActionP)).append('/').append(number(row.actionStability))
                    .append('/').append(number(row.entropy)).append('/').append(number(row.falsifier))
                    .append("|why=").append(clip(explicit(row.attribution, row.feedback), 360))
                    .append('\n');
        }
        String vector = "REAL_MISS_STREAK_2PLUS|observedWindow=24|n=" + streak
                + "|latest=" + misses.get(0).targetIssue
                + "|avgRaw=" + average(raw, rawN)
                + "|avgThirdMargin=" + average(margin, marginN)
                + "|avgStability=" + average(stability, stabilityN)
                + "|avgEntropy=" + average(entropy, entropyN)
                + "|avgAssignment=" + average(assignment, assignmentN)
                + "|avgFalsifier=" + average(falsifier, falsifierN)
                + "|latestGate=" + explicit(misses.get(0).gateCode, "N/A")
                + "|vectorHash=" + V51ChecksumEngine.sha256(detail.toString());
        detail.append("结论=已在第").append(ESCALATE_AT)
                .append("次连续真实未中时升级研究失败向量；继续运行，不暂停、不追单、不补频率。\n")
                .append("边界=该分析进入下一代结构创造，不能保证下一期命中，也不能改写REAL门。\n")
                .append("FailureVector=").append(vector);
        return new Result(true, "ESCALATED_RESEARCH_NO_PAUSE", streak, invalid,
                misses.get(0).targetIssue, vector, detail.toString());
    }

    private static void publish(DbStore store, Result result) {
        if (store == null || result == null) return;
        try {
            if (result.summary().equals(store.getState("r923_miss_streak_analysis", ""))) return;
            String previousEscalated = store.getState(
                    "r923_miss_streak_last_escalated_issue", "");
            LinkedHashMap<String, String> states = new LinkedHashMap<String, String>();
            states.put("r923_miss_streak_version", VERSION);
            states.put("r923_miss_streak_status", result.status);
            states.put("r923_miss_streak_current", String.valueOf(result.streak));
            states.put("r923_miss_streak_issue", result.latestIssue);
            states.put("r923_miss_streak_analysis", result.summary());
            states.put("r923_miss_streak_detail", clip(result.detail, 6000));
            states.put("r923_miss_streak_at", String.valueOf(System.currentTimeMillis()));
            if (result.escalated()) {
                states.put("r923_miss_streak_last_failure_vector", result.failureVector);
                states.put("r923_miss_streak_last_escalated_issue", result.latestIssue);
                if (!result.latestIssue.equals(previousEscalated))
                    states.put("r923_miss_streak_research_step", "ALGORITHM_PENDING");
            }
            store.setStates(states);
        } catch (Throwable ignored) {}
    }

    private static boolean unit(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static String average(double sum, int n) {
        return n <= 0 ? "N/A" : String.format(Locale.US, "%.6f", sum / n);
    }

    private static String number(double value) {
        return Double.isFinite(value) && value >= 0.0d
                ? String.format(Locale.US, "%.6f", value) : "N/A";
    }

    private static String firstValidIssue(Observation[] rows) {
        for (int i = 0; i < rows.length; i++) if (rows[i] != null && rows[i].valid())
            return rows[i].targetIssue;
        return "";
    }

    private static String explicit(String value, String fallback) {
        String out = safe(value);
        return out.length() == 0 ? safe(fallback) : out;
    }

    private static String clip(String value, int max) {
        String out = safe(value).replace('\n', ' ');
        return out.length() <= max ? out : out.substring(0, Math.max(0, max)) + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Observation {
        public final String targetIssue, authorizationId, gateCode, gateDetail, attribution, feedback;
        public final int hit, position, n1, n2, n3, actualNumber;
        public final long createdAt;
        public final double rawActionP, thirdMargin, actionStability, entropy;
        public final double assignmentSupport, falsifier;

        public Observation(
                String targetIssue, int hit, int position, int n1, int n2, int n3, int actual,
                String authorizationId, long createdAt, String gateCode, String gateDetail,
                double rawActionP, double thirdMargin, double actionStability, double entropy,
                double assignmentSupport, double falsifier, String attribution, String feedback) {
            this.targetIssue = safe(targetIssue);
            this.hit = hit;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            actualNumber = actual;
            this.authorizationId = safe(authorizationId);
            this.createdAt = createdAt;
            this.gateCode = safe(gateCode);
            this.gateDetail = safe(gateDetail);
            this.rawActionP = rawActionP;
            this.thirdMargin = thirdMargin;
            this.actionStability = actionStability;
            this.entropy = entropy;
            this.assignmentSupport = assignmentSupport;
            this.falsifier = falsifier;
            this.attribution = safe(attribution);
            this.feedback = safe(feedback);
        }

        boolean valid() {
            return TargetIssueResolver.isValid(targetIssue)
                    && (hit == 0 || hit == 1)
                    && position >= 1 && position <= 10
                    && DecisionGateEngine.b12K3(n1, n2, n3)
                    && actualNumber >= 1 && actualNumber <= 10
                    && hit == (actualNumber == n1 || actualNumber == n2 || actualNumber == n3 ? 1 : 0)
                    && (authorizationId.matches("[0-9a-f]{64}") || V51AuthorizationCore.isBetEnvelope(authorizationId))
                    && createdAt > 0L;
        }
    }

    public static final class Result {
        public final boolean success;
        public final String status, latestIssue, failureVector, detail;
        public final int streak, invalidRows;

        Result(boolean success, String status, int streak, int invalidRows, String latestIssue,
                String failureVector, String detail) {
            this.success = success;
            this.status = safe(status);
            this.streak = Math.max(0, streak);
            this.invalidRows = Math.max(0, invalidRows);
            this.latestIssue = safe(latestIssue);
            this.failureVector = safe(failureVector);
            this.detail = safe(detail);
        }

        static Result error(String reason) {
            return new Result(false, "READ_ERROR", 0, 0, "", "", safe(reason));
        }

        public boolean escalated() {
            return success && streak >= ESCALATE_AT;
        }

        public String summary() {
            return VERSION + "|status=" + status + "|streak=" + streak
                    + "|escalateAt=" + ESCALATE_AT + "|latest=" + latestIssue
                    + "|invalid=" + invalidRows + "|noPause=1|noBetAuthority=1"
                    + (failureVector.length() == 0 ? "" : "|failure=" + failureVector);
        }
    }
}
