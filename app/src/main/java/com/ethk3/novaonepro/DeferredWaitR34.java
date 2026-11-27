package com.ethk3.novaonepro;

/** Narrow backpressure recognition for persisted pre-R34 materialization receipts. */
public final class DeferredWaitR34 {
    private DeferredWaitR34() {}
    public static final String WRITER_BACKLOG =
            "R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN";

    /** Only a declared, unentered, rolled-back writer admission is a wait. */
    public static boolean isWriterBackpressure(String error) {
        return error != null
                && error.contains(WRITER_BACKLOG)
                && (error.contains("category=DEPENDENCY|") || error.contains("category=RESOURCE_QUEUE|"))
                && error.contains("phase=ADMISSION")
                && error.contains("commitOutcome=ROLLED_BACK");
    }
}
