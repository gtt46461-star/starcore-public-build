package com.ethk3.novaonepro;

/** Checks issue lag, fetch/analysis wall-clock age, generation and target consistency. */
public final class RuntimeFreshnessGuard {
    public static final String MODULE_VERSION = "V48_RUNTIME_FRESHNESS_20260721_48072148";

    public static final class Result {
        public boolean fresh;
        public int issueLag;
        public long fetchAgeMs;
        public long analysisAgeMs;
        public long currentGeneration;
        public long issueGeneration;
        public String latestFetch = "";
        public String target = "";
        public String code = "";

        public String summary() {
            return "fresh="
                    + (fresh ? 1 : 0)
                    + "|lag="
                    + issueLag
                    + "|fetchAge="
                    + fetchAgeMs
                    + "|analysisAge="
                    + analysisAgeMs
                    + "|generation="
                    + issueGeneration
                    + '/'
                    + currentGeneration
                    + "|fetch="
                    + latestFetch
                    + "|target="
                    + target
                    + "|code="
                    + code;
        }
    }

    private RuntimeFreshnessGuard() {}

    public static Result evaluate(DbStore store, String target) {
        Result result = new Result();
        result.target = safe(target);
        long now = System.currentTimeMillis();
        result.latestFetch =
                get(store, "last_fetch_issue", get(store, "v47_latest_fetch_issue", ""));
        long fetchAt =
                parse(get(store, "last_fetch_success_at", get(store, "last_fetch_time", "0")));
        long analysisAt =
                parse(
                        get(
                                store,
                                "last_analysis_success_at",
                                get(store, "last_analysis_finish_at", "0")));
        result.fetchAgeMs = fetchAt <= 0L ? Long.MAX_VALUE : Math.max(0L, now - fetchAt);
        result.analysisAgeMs = analysisAt <= 0L ? Long.MAX_VALUE : Math.max(0L, now - analysisAt);
        result.issueLag = distance(result.target, result.latestFetch);
        result.currentGeneration = parse(get(store, "service_generation", "0"));
        result.issueGeneration = V48IssueContext.generation(store);

        boolean issueOk =
                result.latestFetch.length() == 0
                        || result.issueLag <= AppConfig.V48_MAX_ANALYSIS_LAG_ISSUES;
        boolean wallOk =
                result.fetchAgeMs <= AppConfig.V48_MAX_FETCH_AGE_MS
                        && result.analysisAgeMs <= AppConfig.V48_MAX_ANALYSIS_AGE_MS;
        boolean generationOk =
                result.issueGeneration == 0L
                        || result.currentGeneration == 0L
                        || result.issueGeneration == result.currentGeneration;
        boolean targetOk = result.target.equals(V48IssueContext.target(store));

        result.fresh = issueOk && wallOk && generationOk && targetOk;
        result.code = result.fresh ? "OK" : AppConfig.V48_ROOT_RUNTIME;
        try {
            if (store != null) store.setState("v48_runtime_freshness", result.summary());
        } catch (Throwable ignore) {
        }
        return result;
    }

    private static int distance(String target, String latest) {
        try {
            long a = Long.parseLong(digits(target));
            long b = Long.parseLong(digits(latest));
            long delta = b - a;
            return delta <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, delta);
        } catch (Throwable error) {
            return 0;
        }
    }

    private static String digits(String value) {
        StringBuilder out = new StringBuilder();
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (Character.isDigit(c)) out.append(c);
            }
        }
        return out.toString();
    }

    private static String get(DbStore store, String key, String fallback) {
        try {
            return store == null ? fallback : store.getState(key, fallback);
        } catch (Throwable error) {
            return fallback;
        }
    }

    private static long parse(String value) {
        try {
            return Long.parseLong(value);
        } catch (Throwable error) {
            return 0L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
