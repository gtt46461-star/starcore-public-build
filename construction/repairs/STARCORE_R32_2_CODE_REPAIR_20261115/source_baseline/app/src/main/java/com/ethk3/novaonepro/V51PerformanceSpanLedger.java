package com.ethk3.novaonepro;

/** Compatibility facade over the R3.6 in-memory trace. It performs no DB I/O. */
public final class V51PerformanceSpanLedger {
    public static final class Span {
        final RuntimeTraceR36.Mark mark;

        Span(RuntimeTraceR36.Mark mark) {
            this.mark = mark;
        }
    }

    private final RuntimeTraceR36 trace;

    public V51PerformanceSpanLedger(DbStore ignored) {
        this(new RuntimeTraceR36("LEGACY_FACADE", "", System.currentTimeMillis()));
    }

    public V51PerformanceSpanLedger(RuntimeTraceR36 trace) {
        if (trace == null) throw new IllegalArgumentException("R36_TRACE_NULL");
        this.trace = trace;
    }

    public Span begin(String targetIssue, String traceId, String name) {
        return new Span(trace.begin(name));
    }

    public void end(Span span, boolean success, String failureOwner) {
        if (span != null) trace.end(span.mark, success, failureOwner);
    }
}
