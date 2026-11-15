package com.ethk3.novaonepro;

/** Realtime stage budget governor. It may defer background enrichment, never fabricate BET. */
public final class PipelineBudgetGovernor {
    public static final String MODULE_VERSION = "V46_PIPELINE_BUDGET_GOVERNOR_20260721_46072130";
    private final long started = System.currentTimeMillis();

    public long elapsed() {
        return System.currentTimeMillis() - started;
    }

    public boolean deferOptional() {
        return elapsed() >= AppConfig.V46_REALTIME_TARGET_MS - 1800L;
    }

    public boolean overP95() {
        return elapsed() > AppConfig.V46_REALTIME_P95_MS;
    }

    public String summary() {
        return "elapsed="
                + elapsed()
                + "|defer="
                + (deferOptional() ? 1 : 0)
                + "|p95="
                + (overP95() ? 1 : 0);
    }
}
