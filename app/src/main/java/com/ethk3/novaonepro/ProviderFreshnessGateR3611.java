package com.ethk3.novaonepro;

/**
 * R3.6.11 provider freshness truth. Data-only; it owns no prediction, settlement or BET path.
 * A one-minute game must not accept an hours-old HTTP-200 payload or an untrusted wall clock as a
 * successful live fetch.
 */
public final class ProviderFreshnessGateR3611 {
    public static final long STALE_MS = 125000L;
    private static volatile Candidate primary = Candidate.empty();
    private static volatile Candidate backup = Candidate.empty();
    private static volatile Candidate selected = Candidate.empty();

    private ProviderFreshnessGateR3611() {}

    /**
     * A live poll must try the alternate provider when the known next issue is already due.
     * The broad freshness gate deliberately permits the previous draw for settlement/history;
     * using it alone for transport acceptance masked a faster backup for up to 125 seconds.
     * This changes neither the stored draw nor its immutable prediction deadline.
     */
    public static Decision evaluateLatestPollR75(
            Models.Draw draw, String expectedTargetIssue, long nowMs) {
        Decision freshness = evaluate(draw, expectedTargetIssue, nowMs);
        if (!freshness.accepted) return freshness;
        String target = safe(expectedTargetIssue);
        if (!TargetIssueResolver.isValid(draw.issue)) return freshness;
        // At startup there may be no durable local issue. The source draw's own deadline
        // still tells us when to ask the alternate provider; it does not create a draw.
        if (!TargetIssueResolver.isValid(target)) target = TargetIssueResolver.next(draw.issue).targetIssue;
        if (TargetIssueResolver.distance(draw.issue, target) == 1) {
            Models.Draw clockDraw = draw;
            long normalizedOpen = normalizeEpochMs(draw.openTime);
            if (normalizedOpen != draw.openTime) {
                // Preserve the existing gate's seconds-or-milliseconds input contract.
                // Never mutate the caller's response or durable evidence.
                clockDraw = new Models.Draw();
                clockDraw.issue = draw.issue;
                clockDraw.numbers = draw.numbers;
                clockDraw.openTime = normalizedOpen;
            }
            long nextOpen = TargetIssueResolver.predrawDeadlineWallR20261008(clockDraw);
            if (nowMs >= nextOpen)
                return new Decision(false, "AWAITING_NEXT_ISSUE", freshness.ageMs, draw.issue);
        }
        return freshness;
    }

    public static Decision evaluate(Models.Draw draw, String expectedTargetIssue, long nowMs) {
        if (draw == null || !draw.isValid()) {
            return new Decision(false, "DRAW_INVALID", -1L, "");
        }
        TimeSourceR811.Skew skew = TimeSourceR811.detectSkew();
        if (skew.excessive) {
            return new Decision(false, "CLOCK_SKEW_QUARANTINED", Math.abs(skew.deltaMs), draw.issue);
        }

        long openMs = normalizeEpochMs(draw.openTime);
        if (openMs <= 0L) {
            return new Decision(false, "OPEN_TIME_MISSING", Long.MAX_VALUE, draw.issue);
        }
        long ageMs = nowMs >= openMs ? nowMs - openMs : 0L;
        if (ageMs > STALE_MS) {
            return new Decision(false, "OPEN_TIME_STALE", ageMs, draw.issue);
        }

        String target = safe(expectedTargetIssue);
        if (TargetIssueResolver.isValid(target) && TargetIssueResolver.isValid(draw.issue)) {
            try {
                int distance = TargetIssueResolver.distance(draw.issue, target);
                if (distance >= 2) {
                    return new Decision(false, "ISSUE_BEHIND_" + distance, ageMs, draw.issue);
                }
            } catch (Throwable ignore) {
            }
        }
        return new Decision(true, "FRESH", ageMs, draw.issue);
    }

    public static void record(String sourceLine, Decision decision, boolean wasSelected, long nowMs) {
        Candidate c =
                new Candidate(
                        sourceLine,
                        decision == null ? "" : decision.issue,
                        decision == null ? Long.MAX_VALUE : decision.ageMs,
                        decision == null ? "DECISION_NULL" : decision.code,
                        nowMs);
        if (AppConfig.API_LINE_BACKUP.equals(sourceLine)) backup = c;
        else primary = c;
        if (wasSelected && decision != null && decision.accepted) selected = c;
    }

    public static Snapshot snapshot() {
        return new Snapshot(primary, backup, selected);
    }

    public static long normalizeEpochMs(long value) {
        if (value <= 0L) return 0L;
        return value < 100000000000L ? value * 1000L : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Decision {
        public final boolean accepted;
        public final String code;
        public final long ageMs;
        public final String issue;

        Decision(boolean accepted, String code, long ageMs, String issue) {
            this.accepted = accepted;
            this.code = code == null ? "" : code;
            this.ageMs = ageMs;
            this.issue = issue == null ? "" : issue;
        }

        public String compact() {
            return code + "|issue=" + issue + "|ageMs=" + ageMs;
        }
    }

    public static final class Candidate {
        public final String sourceLine, issue, code;
        public final long ageMs, observedAt;

        Candidate(String sourceLine, String issue, long ageMs, String code, long observedAt) {
            this.sourceLine = sourceLine == null ? "" : sourceLine;
            this.issue = issue == null ? "" : issue;
            this.ageMs = ageMs;
            this.code = code == null ? "" : code;
            this.observedAt = observedAt;
        }

        static Candidate empty() {
            return new Candidate("", "", -1L, "NO_SAMPLE", 0L);
        }
    }

    public static final class Snapshot {
        public final Candidate primary, backup, selected;

        Snapshot(Candidate primary, Candidate backup, Candidate selected) {
            this.primary = primary == null ? Candidate.empty() : primary;
            this.backup = backup == null ? Candidate.empty() : backup;
            this.selected = selected == null ? Candidate.empty() : selected;
        }
    }
}
