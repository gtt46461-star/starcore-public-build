package com.ethk3.novaonepro;

/** V45 read-only lifecycle trace for candidate identity, probability lock and contract. */
public final class PredictionLifecycleTraceEngine {
    public static final String MODULE_VERSION = "V45_PREDICTION_LIFECYCLE_TRACE_20260720";

    private PredictionLifecycleTraceEngine() {}

    public static void record(DbStore store, Models.ComboCandidate c, String stage, String status) {
        if (c == null) return;
        PredictionEnvelope envelope = PredictionEnvelope.readOnly(c);
        String key = PredictionEnvelope.candidateKey(c);
        String detail =
                "stage="
                        + safe(stage)
                        + "|status="
                        + safe(status)
                        + "|candidate="
                        + key
                        + "|object="
                        + System.identityHashCode(c)
                        + "|contract="
                        + safe(c.contractDecisionId)
                        + "|candidateHash="
                        + safe(c.candidateHash)
                        + "|lock="
                        + shortHash(c.v43ProbabilityLockHash)
                        + "|P="
                        + (envelope == null ? "--" : round(envelope.canonicalProbability))
                        + "|gate="
                        + (envelope == null ? "--" : round(envelope.gate));
        c.reasonSummary = append(c.reasonSummary, "V45生命周期=" + detail);
        if (store == null) return;
        try {
            store.setState("v45_lifecycle_version", MODULE_VERSION);
            store.setState("v45_lifecycle_last_stage", safe(stage));
            store.setState("v45_lifecycle_last_status", safe(status));
            store.setState("v45_lifecycle_last_candidate", key);
            store.setState("v45_lifecycle_last_detail", detail);
        } catch (Throwable ignore) {
        }
    }

    public static void recordContract(
            DbStore store, Models.ComboCandidate c, CompiledActionContract contract) {
        record(
                store,
                c,
                "COMPILED_ACTION_CONTRACT",
                contract == null ? "CONTRACT_NULL" : contract.summary());
        if (store != null && contract != null) {
            try {
                store.setState("v45_compiled_contract_hash", contract.contractHash);
                store.setState("v45_compiled_contract_summary", contract.summary());
                store.setState(
                        "v45_compiled_contract_compile_count",
                        String.valueOf(contract.compileCount));
            } catch (Throwable ignore) {
            }
        }
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "｜" + b;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String shortHash(String s) {
        return safe(s).length() <= 12 ? safe(s) : s.substring(0, 12);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
