package com.ethk3.novaonepro;

/**
 * R926 production recovery/diagnostic phase extracted from the service.
 *
 * <p>This class never decides a prediction, never creates a BET and never changes a settlement
 * result.  Its only job is to make secondary failures observable without letting a failed
 * diagnostic write hide the original failure.  The in-memory return value is therefore always
 * populated even when SQLite itself is unavailable.</p>
 */
public final class RuntimeRecoveryCoordinatorR926 {
    public static final class Record {
        public final String code;
        public final String issue;
        public final String detail;
        public final long at;
        public final boolean stateWritten;
        public final boolean eventWritten;
        public final String secondaryError;

        Record(String code, String issue, String detail, long at,
               boolean stateWritten, boolean eventWritten, String secondaryError) {
            this.code = safe(code);
            this.issue = safe(issue);
            this.detail = safe(detail);
            this.at = Math.max(0L, at);
            this.stateWritten = stateWritten;
            this.eventWritten = eventWritten;
            this.secondaryError = safe(secondaryError);
        }

        public String canonical() {
            return code + "|issue=" + issue + "|at=" + at
                    + "|state=" + (stateWritten ? 1 : 0)
                    + "|event=" + (eventWritten ? 1 : 0)
                    + "|detail=" + detail
                    + (secondaryError.length() == 0 ? "" : "|secondary=" + secondaryError);
        }
    }

    private RuntimeRecoveryCoordinatorR926() {}

    public static Record record(DbStore store, String stage, String issue, Throwable failure) {
        String code = code(stage);
        String detail = bounded(DatabaseCoordinator.describeCauseChain(failure, 480), 480);
        long now = System.currentTimeMillis();
        boolean stateWritten = false;
        boolean eventWritten = false;
        StringBuilder secondary = new StringBuilder();

        if (store != null) {
            try {
                store.setState("r926_last_critical_catch", code);
                store.setState("r926_last_critical_catch_issue", safe(issue));
                store.setState("r926_last_critical_catch_detail", detail);
                store.setState("r926_last_critical_catch_at", String.valueOf(now));
                stateWritten = true;
            } catch (Throwable stateFailure) {
                appendSecondary(secondary, "STATE", stateFailure);
            }
            try {
                eventWritten = RuntimeEventHooksR926.recovery(store, safe(issue), code, detail);
            } catch (Throwable eventFailure) {
                appendSecondary(secondary, "EVENT", eventFailure);
            }
        } else {
            secondary.append("STORE_NULL");
        }
        return new Record(code, issue, detail, now, stateWritten, eventWritten, secondary.toString());
    }

    public static Record recordMessage(DbStore store, String stage, String issue, String detail) {
        return record(store, stage, issue, new IllegalStateException(bounded(detail, 480)));
    }

    private static String code(String stage) {
        String x = safe(stage).toUpperCase(java.util.Locale.US);
        StringBuilder b = new StringBuilder("R926_");
        for (int i = 0; i < x.length(); i++) {
            char ch = x.charAt(i);
            b.append((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_' ? ch : '_');
        }
        return b.toString();
    }

    private static void appendSecondary(StringBuilder b, String source, Throwable failure) {
        if (b.length() > 0) b.append(';');
        b.append(source).append('=')
                .append(bounded(DatabaseCoordinator.describeCauseChain(failure, 160), 160));
    }

    private static String bounded(String s, int max) {
        String x = safe(s);
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
