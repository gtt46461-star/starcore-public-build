package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure-Java Batch-2 contracts shared by Android runtime code and host verification.
 * It owns no prediction, settlement or REAL/BET decision. It only classifies evidence and validates
 * invariants that must be true before the existing owners may persist their results.
 */
public final class Batch2ContractCoreR926 {
    public static final long REQUEST_TO_DURABLE_BUDGET_MS = 5000L;
    public static final long COMPUTE_BUDGET_MS = 3000L;
    public static final long COLD_START_TARGET_MS = 8000L;
    public static final long RESEARCH_CANCEL_TARGET_MS = 100L;
    public static final long DB_SLICE_TARGET_MS = 50L;

    private Batch2ContractCoreR926() {}

    public static String classifyFetch(
            boolean transportOk, int httpCode, boolean payloadAccepted, String reason) {
        String r = normalize(reason);
        if (!transportOk) {
            if (containsAny(r, "timeout", "timed_out", "timed out", "sockettimeout"))
                return "TRANSPORT_TIMEOUT";
            if (containsAny(
                    r,
                    "airplane",
                    "network_unavailable",
                    "no_network",
                    "offline",
                    "unable to resolve host",
                    "no address associated with hostname",
                    "network is unreachable"))
                return "TRANSPORT_NETWORK_UNAVAILABLE";
            if (httpCode == 503 || containsAny(r, "http_503", "503")) return "TRANSPORT_HTTP_503";
            return "TRANSPORT_FAILED";
        }
        if (httpCode == 503) return "TRANSPORT_HTTP_503";
        if (httpCode < 200 || httpCode >= 300) return "HTTP_FAILED_" + httpCode;
        if (containsAny(r, "clock_skew", "clock skew", "time_jump", "wall_clock_jump"))
            return "CLOCK_SKEW_QUARANTINED";
        if (payloadAccepted) {
            if (containsAny(r, "duplicate", "already_exists", "idempotent")) return "ACCEPTED_DUPLICATE";
            return "BUSINESS_ACCEPTED";
        }
        if (containsAny(r, "conflict", "same_issue_different", "draw_conflict"))
            return "CONFLICT_QUARANTINED";
        if (containsAny(r, "out_of_order", "out-of-order", "stale_issue"))
            return "OUT_OF_ORDER_REJECTED";
        if (containsAny(r, "dirty_json", "invalid_json", "json", "parse"))
            return "PAYLOAD_REJECTED_DIRTY_JSON";
        if (containsAny(r, "wrong_game", "game_mismatch")) return "PAYLOAD_REJECTED_WRONG_GAME";
        if (containsAny(r, "number_pattern", "invalid_number", "pattern"))
            return "PAYLOAD_REJECTED_NUMBER_PATTERN";
        if (containsAny(r, "duplicate")) return "PAYLOAD_REJECTED_DUPLICATE_CONFLICT";
        return "PAYLOAD_REJECTED_BUSINESS";
    }

    public static String learningKey(
            String game,
            String issue,
            String updateContract,
            String labelVersion,
            String inputHash) {
        return sha256(
                "LEARNING|"
                        + safe(game)
                        + '|'
                        + safe(issue)
                        + '|'
                        + safe(updateContract)
                        + '|'
                        + safe(labelVersion)
                        + '|'
                        + safe(inputHash));
    }

    /** B11 immutable provenance package presented to the existing DecisionGate transaction. */
    public static final class GateInput {
        public final String action;
        public final boolean gateEligible;
        public final boolean controlledInfluenceEligible;
        public final int governanceApplied;
        public final int position, n1, n2, n3;
        public final String opportunityHash;
        public final String canonicalHash;
        public final String snapshotHash;
        public final String historyHash;
        public final String actionSpaceHash;
        public final String bundleHash;
        public final String contractHash;
        public final String snapshotRecordHash;
        public final String dataHash;
        public final String featureHash;
        public final String modelHash;
        public final String stateHash;
        public final String selectionHash;
        public final String calibrationHash;
        public final String certificateHash;
        public final String budgetHash;
        public final String actualConfig;

        /** Compatibility constructor: safe for HOLD/SHADOW; a BET using it fail-closes on missing E02D evidence. */
        public GateInput(
                String action,
                boolean gateEligible,
                boolean controlledInfluenceEligible,
                int governanceApplied,
                int position,
                int n1,
                int n2,
                int n3,
                String opportunityHash,
                String canonicalHash,
                String snapshotHash,
                String historyHash,
                String actionSpaceHash,
                String bundleHash,
                String contractHash,
                String snapshotRecordHash) {
            this(action, gateEligible, controlledInfluenceEligible, governanceApplied,
                    position, n1, n2, n3, opportunityHash, canonicalHash, snapshotHash,
                    historyHash, actionSpaceHash, bundleHash, contractHash, snapshotRecordHash,
                    "", "", "", "", "", "", "", "", "");
        }

        public GateInput(
                String action,
                boolean gateEligible,
                boolean controlledInfluenceEligible,
                int governanceApplied,
                int position,
                int n1,
                int n2,
                int n3,
                String opportunityHash,
                String canonicalHash,
                String snapshotHash,
                String historyHash,
                String actionSpaceHash,
                String bundleHash,
                String contractHash,
                String snapshotRecordHash,
                String dataHash,
                String featureHash,
                String modelHash,
                String stateHash,
                String selectionHash,
                String calibrationHash,
                String certificateHash,
                String budgetHash,
                String actualConfig) {
            this.action = safe(action).toUpperCase(Locale.US);
            this.gateEligible = gateEligible;
            this.controlledInfluenceEligible = controlledInfluenceEligible;
            this.governanceApplied = governanceApplied;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.opportunityHash = safe(opportunityHash);
            this.canonicalHash = safe(canonicalHash);
            this.snapshotHash = safe(snapshotHash);
            this.historyHash = safe(historyHash);
            this.actionSpaceHash = safe(actionSpaceHash);
            this.bundleHash = safe(bundleHash);
            this.contractHash = safe(contractHash);
            this.snapshotRecordHash = safe(snapshotRecordHash);
            this.dataHash = safe(dataHash);
            this.featureHash = safe(featureHash);
            this.modelHash = safe(modelHash);
            this.stateHash = safe(stateHash);
            this.selectionHash = safe(selectionHash);
            this.calibrationHash = safe(calibrationHash);
            this.certificateHash = safe(certificateHash);
            this.budgetHash = safe(budgetHash);
            this.actualConfig = safe(actualConfig);
        }
    }

    public static final class GateCheck {
        public final boolean valid;
        public final boolean isBet;
        public final String code;
        public final String provenanceHash;

        GateCheck(boolean valid, boolean isBet, String code, String provenanceHash) {
            this.valid = valid;
            this.isBet = isBet;
            this.code = safe(code);
            this.provenanceHash = safe(provenanceHash);
        }
    }

    public static GateCheck validateGate(GateInput in) {
        if (in == null) return new GateCheck(false, false, "GATE_INPUT_NULL", "");
        boolean bet = "BET".equals(in.action) || "REAL".equals(in.action);
        if (!bet && !"HOLD".equals(in.action) && !"SHADOW".equals(in.action))
            return new GateCheck(false, false, "ACTION_UNKNOWN", "");
        String evidence =
                in.action + '|' + in.gateEligible + '|' + in.controlledInfluenceEligible + '|'
                        + in.governanceApplied + '|' + in.position + '|' + in.n1 + '|' + in.n2 + '|'
                        + in.n3 + '|' + in.opportunityHash + '|' + in.canonicalHash + '|'
                        + in.snapshotHash + '|' + in.historyHash + '|' + in.actionSpaceHash + '|'
                        + in.bundleHash + '|' + in.contractHash + '|' + in.snapshotRecordHash + '|'
                        + in.dataHash + '|' + in.featureHash + '|' + in.modelHash + '|' + in.stateHash + '|'
                        + in.selectionHash + '|' + in.calibrationHash + '|' + in.certificateHash + '|'
                        + in.budgetHash + '|' + in.actualConfig;
        String provenance = sha256("B11_PROVENANCE_V2|" + evidence);
        if (!bet) return new GateCheck(true, false, "HOLD_AUDITED", provenance);
        if (!in.gateEligible) return new GateCheck(false, true, "BET_GATE_NOT_ELIGIBLE", provenance);
        if (in.position < 1 || in.position > 10)
            return new GateCheck(false, true, "BET_POSITION_INVALID", provenance);
        if (!validNumber(in.n1) || !validNumber(in.n2) || !validNumber(in.n3))
            return new GateCheck(false, true, "BET_NUMBER_INVALID", provenance);
        if (in.n1 == in.n2 || in.n1 == in.n3 || in.n2 == in.n3)
            return new GateCheck(false, true, "BET_NUMBERS_NOT_DISTINCT", provenance);
        if (!isSha256(in.opportunityHash)) return invalid("BET_OPPORTUNITY_HASH_MISSING", provenance);
        if (!isSha256(in.canonicalHash)) return invalid("BET_CANONICAL_HASH_MISSING", provenance);
        if (!isSha256(in.snapshotHash)) return invalid("BET_SNAPSHOT_HASH_MISSING", provenance);
        if (!isSha256(in.historyHash)) return invalid("BET_HISTORY_HASH_MISSING", provenance);
        if (!isSha256(in.actionSpaceHash)) return invalid("BET_ACTION_SPACE_HASH_MISSING", provenance);
        if (!isSha256(in.bundleHash)) return invalid("BET_BUNDLE_HASH_MISSING", provenance);
        if (!isSha256(in.contractHash)) return invalid("BET_CONTRACT_HASH_MISSING", provenance);
        if (!isSha256(in.snapshotRecordHash)) return invalid("BET_SNAPSHOT_RECORD_HASH_MISSING", provenance);
        if (!isSha256(in.dataHash)) return invalid("BET_DATA_HASH_MISSING", provenance);
        if (!isSha256(in.featureHash)) return invalid("BET_FEATURE_HASH_MISSING", provenance);
        if (!isSha256(in.modelHash)) return invalid("BET_MODEL_HASH_MISSING", provenance);
        if (!isSha256(in.stateHash)) return invalid("BET_STATE_HASH_MISSING", provenance);
        if (!isSha256(in.selectionHash)) return invalid("BET_SELECTION_HASH_MISSING", provenance);
        if (!isSha256(in.calibrationHash)) return invalid("BET_CALIBRATION_HASH_MISSING", provenance);
        if (!isSha256(in.certificateHash)) return invalid("BET_CERTIFICATE_HASH_MISSING", provenance);
        if (!isSha256(in.budgetHash)) return invalid("BET_BUDGET_HASH_MISSING", provenance);
        if (in.actualConfig.length() == 0) return invalid("BET_ACTUAL_CONFIG_MISSING", provenance);
        return new GateCheck(true, true, "BET_PROVENANCE_VALID_V2", provenance);
    }

    private static GateCheck invalid(String code, String provenance) {
        return new GateCheck(false, true, code, provenance);
    }

    public static final class Continuity {
        public final int expected;
        public final int full;
        public final int holes;
        public final int unrecoverable;
        public final String firstHole;
        public final String continuousWatermark;
        public final String latestFull;
        public final boolean closure;

        Continuity(
                int expected,
                int full,
                int holes,
                int unrecoverable,
                String firstHole,
                String continuousWatermark,
                String latestFull,
                boolean closure) {
            this.expected = expected;
            this.full = full;
            this.holes = holes;
            this.unrecoverable = unrecoverable;
            this.firstHole = safe(firstHole);
            this.continuousWatermark = safe(continuousWatermark);
            this.latestFull = safe(latestFull);
            this.closure = closure;
        }
    }

    /** Expected issues must be oldest-to-newest. A later FULL can never hide an earlier hole. */
    public static Continuity evaluateContinuity(
            List<String> expectedOldestToNewest, Set<String> fullIssues, int unrecoverable) {
        List<String> expected = expectedOldestToNewest == null
                ? Collections.<String>emptyList() : expectedOldestToNewest;
        Set<String> fullSet = fullIssues == null ? Collections.<String>emptySet() : fullIssues;
        int full = 0;
        int holes = 0;
        String firstHole = "";
        String watermark = "";
        String latestFull = "";
        boolean blocked = false;
        for (String issueRaw : expected) {
            String issue = safe(issueRaw);
            if (issue.length() == 0) continue;
            if (fullSet.contains(issue)) {
                full++;
                latestFull = issue;
                if (!blocked) watermark = issue;
            } else {
                holes++;
                if (firstHole.length() == 0) firstHole = issue;
                blocked = true;
            }
        }
        int bad = Math.max(0, unrecoverable);
        return new Continuity(
                expected.size(), full, holes, bad, firstHole, watermark, latestFull,
                holes == 0 && bad == 0);
    }

    public static final class LatencyStats {
        public final int n;
        public final long p50, p95, p99, max;
        public final int overBudget;

        LatencyStats(int n, long p50, long p95, long p99, long max, int overBudget) {
            this.n = n;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.max = max;
            this.overBudget = overBudget;
        }
    }

    public static LatencyStats latencyStats(List<Long> samples, long budget) {
        ArrayList<Long> x = new ArrayList<Long>();
        if (samples != null) {
            for (Long v : samples) if (v != null) x.add(Long.valueOf(Math.max(0L, v.longValue())));
        }
        Collections.sort(x);
        int n = x.size();
        if (n == 0) return new LatencyStats(0, 0L, 0L, 0L, 0L, 0);
        int over = 0;
        for (Long v : x) if (v.longValue() > Math.max(0L, budget)) over++;
        return new LatencyStats(
                n, percentile(x, .50d), percentile(x, .95d), percentile(x, .99d),
                x.get(n - 1).longValue(), over);
    }

    public static boolean isSha256(String value) {
        String x = safe(value);
        if (x.length() != 64) return false;
        for (int i = 0; i < x.length(); i++) {
            char c = x.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
                return false;
        }
        return true;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safe(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", e);
        }
    }

    private static long percentile(List<Long> sorted, double p) {
        int n = sorted.size();
        int index = Math.max(0, Math.min(n - 1, (int) Math.ceil(n * p) - 1));
        return sorted.get(index).longValue();
    }

    private static boolean validNumber(int n) { return n >= 1 && n <= 10; }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) if (value.contains(token)) return true;
        return false;
    }

    private static String normalize(String s) { return safe(s).toLowerCase(Locale.US); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
