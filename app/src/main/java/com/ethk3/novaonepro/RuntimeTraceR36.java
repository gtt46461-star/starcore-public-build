package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory monotonic stage trace; it performs no SQLite I/O. */
public final class RuntimeTraceR36 {
    public enum StageKind {
        COMPUTE,
        DB_WAIT,
        COMMIT,
        OTHER
    }

    private final String attemptId, targetIssue;
    private final long requestAt;
    private final ArrayList<Span> spans = new ArrayList<Span>();

    public RuntimeTraceR36(String attemptId, String targetIssue, long requestAt) {
        this.attemptId = safe(attemptId);
        this.targetIssue = safe(targetIssue);
        this.requestAt = Math.max(0L, requestAt);
    }

    public Mark begin(String stage) {
        RuntimeResourceAuditR927.stageBegin(safe(stage));
        return new Mark(safe(stage), System.nanoTime(), System.currentTimeMillis());
    }

    public void end(Mark mark, boolean success, String detail) {
        if (mark == null) return;
        long ended = System.nanoTime();
        RuntimeResourceAuditR927.stageEnd(mark.stage, Math.max(0L,(ended-mark.nanoStartedAt)/1000000L), success);
        synchronized (spans) {
            spans.add(
                    new Span(
                            mark.stage,
                            mark.wallStartedAt,
                            Math.max(0L, (ended - mark.nanoStartedAt) / 1000000L),
                            success,
                            safe(detail),
                            classify(mark.stage)));
        }
    }

    public List<Span> spans() {
        synchronized (spans) {
            return Collections.unmodifiableList(new ArrayList<Span>(spans));
        }
    }

    public String attemptId() {
        return attemptId;
    }

    public String targetIssue() {
        return targetIssue;
    }

    public long requestAt() {
        return requestAt;
    }

    /** Completed, possibly nested spans; this is not an additive request latency estimate. */
    public String compactStagesR923() {
        StringBuilder out = new StringBuilder(2048);
        out.append("target=").append(targetIssue)
                .append("|scope=COMPLETED_SPANS_BEFORE_COMMIT|nested=INCLUSIVE_NOT_ADDITIVE");
        List<Span> copy = spans();
        for (int i = 0; i < copy.size() && i < 40; i++) {
            Span span = copy.get(i);
            out.append('|').append(span.stage).append('=').append(span.elapsedMs)
                    .append("ms:").append(span.success ? "OK" : "FAIL");
        }
        if (copy.size() > 40) out.append("|additionalSpans=").append(copy.size() - 40);
        return out.toString();
    }

    public Summary summarize() {
        List<Span> copy = spans();
        ArrayList<Long> elapsed = new ArrayList<Long>();
        long compute = 0L, dbWait = 0L, commit = 0L, slowestMs = 0L;
        String slowest = "";
        for (int i = 0; i < copy.size(); i++) {
            Span span = copy.get(i);
            elapsed.add(Long.valueOf(span.elapsedMs));
            if (span.kind == StageKind.COMPUTE) compute += span.elapsedMs;
            else if (span.kind == StageKind.DB_WAIT) dbWait += span.elapsedMs;
            else if (span.kind == StageKind.COMMIT) commit += span.elapsedMs;
            if (span.elapsedMs > slowestMs) {
                slowestMs = span.elapsedMs;
                slowest = span.stage;
            }
        }
        Collections.sort(elapsed);
        return new Summary(
                compute,
                dbWait,
                commit,
                percentile(elapsed, 0.50d),
                percentile(elapsed, 0.95d),
                percentile(elapsed, 0.99d),
                slowest,
                slowestMs);
    }

    public static final class Mark {
        final String stage;
        final long nanoStartedAt, wallStartedAt;

        Mark(String stage, long nano, long wall) {
            this.stage = stage;
            nanoStartedAt = nano;
            wallStartedAt = wall;
        }
    }

    public static final class Span {
        public final String stage, detail;
        public final long startedAt, elapsedMs;
        public final boolean success;
        public final StageKind kind;

        Span(
                String stage,
                long started,
                long elapsed,
                boolean success,
                String detail,
                StageKind kind) {
            this.stage = safe(stage);
            startedAt = started;
            elapsedMs = elapsed;
            this.success = success;
            this.detail = safe(detail);
            this.kind = kind == null ? StageKind.OTHER : kind;
        }

        Span(String stage, long started, long elapsed, boolean success, String detail) {
            this(stage, started, elapsed, success, detail, classify(stage));
        }
    }

    public static final class Summary {
        public final long computeMs,
                dbWaitMs,
                commitMs,
                p50Ms,
                p95Ms,
                p99Ms,
                slowestStageMs;
        public final String slowestStage;

        Summary(
                long compute,
                long dbWait,
                long commit,
                long p50,
                long p95,
                long p99,
                String slowest,
                long slowestMs) {
            computeMs = compute;
            dbWaitMs = dbWait;
            commitMs = commit;
            p50Ms = p50;
            p95Ms = p95;
            p99Ms = p99;
            slowestStage = safe(slowest);
            slowestStageMs = slowestMs;
        }

        public String canonical() {
            return "compute_ms="
                    + computeMs
                    + "|db_wait_ms="
                    + dbWaitMs
                    + "|commit_ms="
                    + commitMs
                    + "|p50="
                    + p50Ms
                    + "|p95="
                    + p95Ms
                    + "|p99="
                    + p99Ms
                    + "|slowest="
                    + slowestStage
                    + '@'
                    + slowestStageMs;
        }
    }

    private static StageKind classify(String stage) {
        String x = safe(stage).toUpperCase(java.util.Locale.US);
        if (x.indexOf("DB_") >= 0 || x.indexOf("LOCK") >= 0) return StageKind.DB_WAIT;
        if (x.indexOf("COMMIT") >= 0 || x.indexOf("PERSIST") >= 0) return StageKind.COMMIT;
        if (x.indexOf("FEATURE") >= 0
                || x.indexOf("STATE") >= 0
                || x.indexOf("NUMBER") >= 0
                || x.indexOf("ACTION") >= 0
                || x.indexOf("FUNNEL") >= 0
                || x.indexOf("META") >= 0
                || x.indexOf("RANK") >= 0
                || x.indexOf("SELECT") >= 0) return StageKind.COMPUTE;
        return StageKind.OTHER;
    }

    private static long percentile(List<Long> sorted, double q) {
        if (sorted == null || sorted.size() == 0) return 0L;
        int index = (int) Math.ceil(q * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index))).longValue();
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }
}
