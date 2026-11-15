package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicInteger;

/** Existing production diagnostic injection; only the callback fails after the real transaction commits. */
public final class HistoryDiagnosticFaultR31Host {
    private HistoryDiagnosticFaultR31Host() {}
    public static final AtomicInteger count = new AtomicInteger();
    public static void install() {
        DatabaseCoordinator.diagnosticFaultR31.set(phase -> {
            if ("AFTER_COMMIT".equals(phase)) {
                count.incrementAndGet();
                throw new IllegalStateException("HOST_INJECTED_HISTORY_POSTCOMMIT_DIAGNOSTIC");
            }
        });
    }
    public static void clear() { DatabaseCoordinator.diagnosticFaultR31.remove(); }
}
