package com.ethk3.novaonepro;

import java.util.List;

/** Compatibility facade. V48IssueContext and V48PipelineLedger are the only live owners. */
public final class V47CandidateSurfaceContext {
    public static final String MODULE_VERSION = "V48_COMPAT_SURFACE_FACADE_20260721_48072148";
    public static final String STATE_VISIBLE_HISTORY = "v48_issue_visible_history";

    private V47CandidateSurfaceContext() {}

    public static int begin(
            DbStore store, List<Models.Draw> history, String base, String target, int scan) {
        V48IssueContext c = V48IssueContext.begin(store, history, base, target);
        V48PipelineLedger.begin(store, target);
        V48PipelineLedger.scan(store, scan);
        return c.visibleHistory;
    }

    public static void probability(DbStore store, int n) {
        V48PipelineLedger.probability(store, n);
    }

    public static void preFinal(DbStore store, int n) {
        V48PipelineLedger.preFinal(store, n);
    }

    public static void formal(DbStore store, int n) {
        V48PipelineLedger.formal(store, n);
    }

    public static void oos(DbStore store, int n) {
        V48PipelineLedger.oos(store, n, 0, 0);
    }

    public static void ui(DbStore store, int n) {
        V48PipelineLedger.ui(store, n);
    }

    public static int visibleHistory(DbStore store) {
        return V48IssueContext.visibleHistory(store);
    }

    public static String summary(DbStore store) {
        return store == null ? "" : store.getState("v48_ledger_summary", "");
    }
}
