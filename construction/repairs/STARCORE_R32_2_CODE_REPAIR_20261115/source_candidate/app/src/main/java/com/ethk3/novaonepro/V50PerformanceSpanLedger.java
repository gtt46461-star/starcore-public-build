package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/** Inclusive/exclusive timing ledger with parent-child reconciliation. */
public final class V50PerformanceSpanLedger {
    public static final class Span {
        public final String traceId;
        public final String spanId;
        public final String parentSpanId;
        public final String targetIssue;
        public final String owner;
        public final long startedAtNanos;
        private long childNanos;
        private long inclusiveNanos;
        private boolean finished;
        private boolean success;
        private String errorCode = "";

        Span(
                String traceId,
                String spanId,
                String parentSpanId,
                String targetIssue,
                String owner,
                long startedAtNanos) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.targetIssue = targetIssue;
            this.owner = owner;
            this.startedAtNanos = startedAtNanos;
        }

        public long inclusiveNanos() {
            return inclusiveNanos;
        }

        public long exclusiveNanos() {
            return Math.max(0L, inclusiveNanos - childNanos);
        }
    }

    private final String traceId;
    private final String targetIssue;
    private final ArrayList<Span> finished = new ArrayList<Span>();
    private int sequence;

    public V50PerformanceSpanLedger(String targetIssue) {
        this.targetIssue = safe(targetIssue);
        this.traceId =
                V50ChecksumEngine.sha256(
                        this.targetIssue
                                + '|'
                                + System.currentTimeMillis()
                                + '|'
                                + System.nanoTime());
    }

    public Span start(String owner, Span parent) {
        sequence++;
        return new Span(
                traceId,
                String.valueOf(sequence),
                parent == null ? "" : parent.spanId,
                targetIssue,
                safe(owner),
                System.nanoTime());
    }

    public void finish(Span span, Span parent, boolean success, String errorCode) {
        if (span == null || span.finished) return;
        span.inclusiveNanos = Math.max(0L, System.nanoTime() - span.startedAtNanos);
        span.success = success;
        span.errorCode = safe(errorCode);
        span.finished = true;
        if (parent != null) parent.childNanos += span.inclusiveNanos;
        finished.add(span);
    }

    public void persist(DbStore store) {
        if (store == null) return;
        SQLiteDatabase db = store.writable();
        db.beginTransaction();
        try {
            for (int i = 0; i < finished.size(); i++) {
                Span span = finished.get(i);
                ContentValues cv = new ContentValues();
                cv.put("trace_id", span.traceId);
                cv.put("span_id", span.spanId);
                cv.put("parent_span_id", span.parentSpanId);
                cv.put("target_issue", span.targetIssue);
                cv.put("owner", span.owner);
                cv.put("started_at_nanos", span.startedAtNanos);
                cv.put("inclusive_nanos", span.inclusiveNanos());
                cv.put("exclusive_nanos", span.exclusiveNanos());
                cv.put("success", span.success ? 1 : 0);
                cv.put("error_code", span.errorCode);
                cv.put("created_at", System.currentTimeMillis());
                long row = db.insert(SchemaContractRegistry.TABLE_PERFORMANCE, null, cv);
                if (row < 0L) {
                    throw new IllegalStateException("V50_PERFORMANCE_INSERT_FAILED");
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String traceId() {
        return traceId;
    }

    public long exclusiveSumNanos() {
        long sum = 0L;
        for (int i = 0; i < finished.size(); i++) {
            sum += finished.get(i).exclusiveNanos();
        }
        return sum;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
