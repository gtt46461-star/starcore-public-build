package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.Settings;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;

/**
 * Coalesces recovery requests by generation and persists the complete B07 recovery proof.
 *
 * <p>This class does not fetch, analyze, settle, or create BET. It only requests the existing
 * AutoFetchService owner and records: request -> accepted start/reconcile -> service observation ->
 * independently observed healthy business truth. User-disabled state is fail-closed and can never
 * be promoted by Alarm/Job recovery.
 */
public final class RecoveryCoordinatorR815 {
    public static final String PREFS = "r815_recovery";
    private static final String KEY_GENERATION = "generation";
    private static final String KEY_STATE = "state";
    private static final String KEY_WINDOW_START = "window_start";
    private static final String KEY_BOOT_SCOPE = "r341_boot_scope";
    private static final String KEY_REQUEST_MONO = "r341_request_elapsed";
    private static final String KEY_NEXT_DUE = "r341_next_due_elapsed";
    private static final String KEY_ACCEPTED_MONO = "r341_accepted_elapsed";
    private static final String KEY_STARTED_MONO = "r341_started_elapsed";
    private static final String KEY_PROGRESS_MONO = "r341_progress_elapsed";
    private static final String KEY_ATTEMPTS = "attempts";
    private static final String KEY_LAST_DETAIL = "last_detail";
    private static final String KEY_REQUEST_AT = "request_at";
    private static final String KEY_ACCEPTED_AT = "accepted_at";
    private static final String KEY_SERVICE_STARTED_AT = "service_started_at";
    private static final String KEY_BUSINESS_RESTORED_AT = "business_restored_at";
    private static final String KEY_REQUEST_REASON = "request_reason";
    private static final String KEY_BUSINESS_DETAIL = "business_detail";
    private static final String KEY_REJECT_REASON = "reject_reason";
    private static final long WINDOW_MS = 10L * 60L * 1000L;
    private static final long COOLDOWN_MS = 30_000L;
    private static final int MAX_ATTEMPTS = 3;
    private static final Object LOCK = new Object();

    public enum State {
        IDLE,
        REQUESTED,
        RUNNING,
        FETCH_SETTLEMENT_ONLY,
        COMPLETED
    }

    public static final class Result {
        public final boolean accepted;
        public final long generation;
        public final State state;
        public final int attempts;
        public final String status;
        public final long nextDueElapsedMs;

        Result(boolean accepted, long generation, State state, int attempts, String status) {
            this(accepted, generation, state, attempts, status, 0L);
        }

        Result(boolean accepted, long generation, State state, int attempts, String status, long nextDue) {
            this.accepted = accepted;
            this.generation = Math.max(0L, generation);
            this.state = state == null ? State.IDLE : state;
            this.attempts = Math.max(0, attempts);
            this.status = safe(status);
            nextDueElapsedMs = Math.max(0L, nextDue);
        }

        public String summary() {
            return status
                    + "|generation="
                    + generation
                    + "|state="
                    + state.name()
                    + "|attempts="
                    + attempts
                    + "|accepted="
                    + (accepted ? 1 : 0) + "|next_due_elapsed=" + nextDueElapsedMs;
        }
    }

    public static final class Evidence {
        public final long generation;
        public final State state;
        public final int attempts;
        public final long requestAt, acceptedAt, serviceStartedAt, businessRestoredAt;
        public final String requestReason, businessDetail, rejectReason;
        public final String clockScope;
        private final long requestedElapsed, acceptedElapsed, startedElapsed, progressElapsed;

        Evidence(
                long generation,
                State state,
                int attempts,
                long requestAt,
                long acceptedAt,
                long serviceStartedAt,
                long businessRestoredAt,
                String requestReason,
                String businessDetail,
                String rejectReason, String clockScope, long requestedElapsed, long acceptedElapsed,
                long startedElapsed, long progressElapsed) {
            this.generation = Math.max(0L, generation);
            this.state = state == null ? State.IDLE : state;
            this.attempts = Math.max(0, attempts);
            this.requestAt = Math.max(0L, requestAt);
            this.acceptedAt = Math.max(0L, acceptedAt);
            this.serviceStartedAt = Math.max(0L, serviceStartedAt);
            this.businessRestoredAt = Math.max(0L, businessRestoredAt);
            this.requestReason = safe(requestReason);
            this.businessDetail = safe(businessDetail);
            this.rejectReason = safe(rejectReason);
            this.clockScope = safe(clockScope);
            this.requestedElapsed = requestedElapsed;
            this.acceptedElapsed = acceptedElapsed;
            this.startedElapsed = startedElapsed;
            this.progressElapsed = progressElapsed;
        }

        public boolean requestAccepted() {
            return requestedElapsed >= 0L && acceptedElapsed >= requestedElapsed;
        }

        public boolean serviceActuallyStarted() {
            return requestAccepted() && startedElapsed >= acceptedElapsed;
        }

        public boolean businessActuallyRestored() {
            return serviceActuallyStarted() && progressElapsed >= startedElapsed;
        }

        public String summary() {
            return "generation="
                    + generation
                    + "|state="
                    + state.name()
                    + "|attempts="
                    + attempts
                    + "|request_at="
                    + requestAt
                    + "|accepted_at="
                    + acceptedAt
                    + "|service_started_at="
                    + serviceStartedAt
                    + "|business_restored_at="
                    + businessRestoredAt
                    + "|request_accepted="
                    + (requestAccepted() ? 1 : 0)
                    + "|service_started="
                    + (serviceActuallyStarted() ? 1 : 0)
                    + "|business_restored="
                    + (businessActuallyRestored() ? 1 : 0)
                    + "|clock_scope=" + clockScope
                    + "|reason="
                    + bounded(requestReason, 96)
                    + "|business="
                    + bounded(businessDetail, 192)
                    + "|reject="
                    + bounded(rejectReason, 96);
        }
    }

    private RecoveryCoordinatorR815() {}

    interface Delivery { boolean request(Context context, String reason); }

    public static Result request(Context context, String reason, long requestedGeneration) {
        if (context == null) return new Result(false, 0L, State.IDLE, 0, "CONTEXT_NULL");
        return request(context, reason, requestedGeneration, TimeSourceR811.monoNowMs(),
                bootScope(context), BootReceiver::reconcileAutoFetchService);
    }

    static Result request(Context context, String reason, long requestedGeneration, long elapsed,
            String boot, Delivery delivery) {
        Context app = app(context);
        synchronized (LOCK) {
            SharedPreferences preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            long current = preferences.getLong(KEY_GENERATION, 0L);
            int attempts = preferences.getInt(KEY_ATTEMPTS, 0);
            State prior = parse(preferences.getString(KEY_STATE, State.IDLE.name()));
            if (!heartbeat.isUserEnabled()) {
                preferences.edit().putString(KEY_STATE, State.IDLE.name())
                        .putString(KEY_REJECT_REASON, "USER_DISABLED_FAIL_CLOSED")
                        .putString(KEY_LAST_DETAIL, "USER_DISABLED_FAIL_CLOSED").commit();
                heartbeat.markRecoveryEvaluation("r815", false, "USER_DISABLED_FAIL_CLOSED_NO_RESTART");
                return new Result(false, current, State.IDLE, attempts, "USER_DISABLED_FAIL_CLOSED");
            }
            if (requestedGeneration > 0L && requestedGeneration < current)
                return new Result(false, current, prior, attempts, "STALE_GENERATION_REJECTED");

            long window = preferences.getLong(KEY_WINDOW_START, -1L);
            boolean sameWindow = boot.equals(preferences.getString(KEY_BOOT_SCOPE, ""))
                    && window >= 0L && elapsed >= window && elapsed - window < WINDOW_MS;
            boolean continuing = sameWindow && (prior == State.REQUESTED || prior == State.RUNNING
                    || prior == State.FETCH_SETTLEMENT_ONLY);
            long due = preferences.getLong(KEY_NEXT_DUE, 0L);
            if (continuing && elapsed < due) {
                boolean accepted = preferences.getLong(KEY_ACCEPTED_MONO, -1L) >= window
                        && !"START_DEFERRED_OR_FAILED".equals(preferences.getString(KEY_REJECT_REASON, ""));
                return new Result(accepted, current, prior, attempts,
                        accepted ? "COALESCED_AWAITING_PROGRESS" : "DELIVERY_RETRY_PENDING", due);
            }
            if (continuing && attempts >= MAX_ATTEMPTS) {
                long nextWindow = window + WINDOW_MS;
                preferences.edit().putString(KEY_STATE, State.FETCH_SETTLEMENT_ONLY.name())
                        .putString(KEY_REJECT_REASON, "RECOVERY_NEEDS_DIAGNOSIS")
                        .putLong(KEY_NEXT_DUE, nextWindow).commit();
                return new Result(false, current, State.FETCH_SETTLEMENT_ONLY, attempts,
                        "RECOVERY_RATE_LIMITED", nextWindow);
            }

            long generation = continuing ? current : Math.max(current + 1L, requestedGeneration);
            long now = System.currentTimeMillis();
            SharedPreferences.Editor update = preferences.edit();
            if (!continuing) {
                attempts = 0;
                window = elapsed;
                update.putLong(KEY_GENERATION, generation).putLong(KEY_WINDOW_START, window)
                        .putString(KEY_BOOT_SCOPE, boot).putLong(KEY_REQUEST_MONO, elapsed)
                        .putLong(KEY_REQUEST_AT, now).putLong(KEY_ACCEPTED_AT, 0L)
                        .putLong(KEY_ACCEPTED_MONO, -1L).putLong(KEY_SERVICE_STARTED_AT, 0L)
                        .putLong(KEY_STARTED_MONO, -1L).putLong(KEY_BUSINESS_RESTORED_AT, 0L)
                        .putLong(KEY_PROGRESS_MONO, -1L).putString(KEY_REQUEST_REASON, bounded(reason, 192))
                        .putString(KEY_BUSINESS_DETAIL, "");
            }
            attempts++;
            State next = attempts >= MAX_ATTEMPTS ? State.FETCH_SETTLEMENT_ONLY : State.REQUESTED;
            due = elapsed + COOLDOWN_MS;
            if (!update.putInt(KEY_ATTEMPTS, attempts).putLong(KEY_NEXT_DUE, due)
                    .putString(KEY_STATE, next.name()).putString(KEY_LAST_DETAIL, bounded(reason, 192))
                    .putString(KEY_REJECT_REASON, "").commit()) {
                return new Result(false, generation, next, attempts, "RECOVERY_LEDGER_UNAVAILABLE", due);
            }

            heartbeat.markRecoveryTrigger("r815:" + bounded(reason, 128));
            heartbeat.markBusinessStage("B07_RECOVERY_REQUESTED", evidence(app).summary());
            boolean delivered = delivery.request(app, "r815:" + bounded(reason, 128));
            if (!delivered) {
                preferences.edit().putString(KEY_REJECT_REASON, "START_DEFERRED_OR_FAILED")
                        .putString(KEY_LAST_DETAIL, "START_DEFERRED_OR_FAILED").commit();
                return new Result(false, generation, next, attempts, "START_DEFERRED_OR_FAILED", due);
            }
            SharedPreferences.Editor accepted = preferences.edit()
                    .putString(KEY_LAST_DETAIL, "START_OR_RECONCILE_REQUEST_ACCEPTED");
            if (preferences.getLong(KEY_ACCEPTED_MONO, -1L) < window) {
                accepted.putLong(KEY_ACCEPTED_MONO, elapsed).putLong(KEY_ACCEPTED_AT, System.currentTimeMillis());
            }
            accepted.commit();
            heartbeat.markBusinessStage("B07_RECOVERY_ACCEPTED", evidence(app).summary());
            return new Result(true, generation, next, attempts, "START_OR_RECONCILE_REQUESTED", due);
        }
    }

    private static String bootScope(Context context) {
        String boot = IssueTimelineWriterR30.bootId();
        if (boot.startsWith("KERNEL_BOOT:") || Build.VERSION.SDK_INT < 24) return boot;
        try {
            int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
            return count >= 0 ? "ANDROID_BOOT_COUNT:" + count : boot;
        } catch (SecurityException unavailable) {
            return boot;
        }
    }

    static void scheduleRetry(Context context, Result result) {
        if (result.nextDueElapsedMs <= 0L || result.accepted) return;
        long delay = Math.max(1000L, result.nextDueElapsedMs - TimeSourceR811.monoNowMs());
        BootReceiver.scheduleWatchdog(context, "r815:" + result.status, delay);
    }

    /** Called by the existing AutoFetchService onCreate path for a newly-created service instance. */
    public static void markServiceStarted(Context context) {
        if (context == null) return;
        Context app = app(context);
        synchronized (LOCK) {
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!heartbeat.isUserEnabled()) {
                p.edit()
                        .putString(KEY_STATE, State.IDLE.name())
                        .putString(KEY_REJECT_REASON, "SERVICE_STARTED_WHILE_USER_DISABLED")
                        .putString(KEY_LAST_DETAIL, "SERVICE_STARTED_WHILE_USER_DISABLED")
                        .commit();
                BootReceiver.cancelAllRecovery(app, "b07:user_disabled_service_start");
                return;
            }
            long accepted = p.getLong(KEY_ACCEPTED_MONO, -1L);
            long mono = TimeSourceR811.monoNowMs();
            SharedPreferences.Editor e = p.edit();
            if (accepted >= 0L && mono >= accepted && bootScope(app).equals(p.getString(KEY_BOOT_SCOPE, ""))
                    && !State.COMPLETED.name().equals(p.getString(KEY_STATE, ""))) {
                if (!State.FETCH_SETTLEMENT_ONLY.name().equals(p.getString(KEY_STATE, ""))) {
                    e.putString(KEY_STATE, State.RUNNING.name());
                }
                e.putLong(KEY_SERVICE_STARTED_AT, System.currentTimeMillis()).putLong(KEY_STARTED_MONO, mono)
                        .putString(KEY_LAST_DETAIL, "SERVICE_CREATED_AFTER_RECOVERY_ACCEPT");
            } else {
                e.putString(KEY_LAST_DETAIL, "SERVICE_CREATED_WITHOUT_ACTIVE_RECOVERY");
            }
            e.commit();
            heartbeat.markBusinessStage("B07_SERVICE_STARTED", evidence(app).summary());
        }
    }

    /** Reads the existing committed runtime event and its exact database identity. */
    static void observeBusinessCommit(Context context, DbStore store, String issue, boolean settlement) {
        if (context == null || store == null || !TargetIssueResolver.isValid(issue)) return;
        Context app = app(context);
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long recovery = p.getLong(KEY_GENERATION, 0L);
        long accepted = p.getLong(KEY_ACCEPTED_MONO, -1L);
        if (recovery <= 0L || accepted < 0L || p.getLong(KEY_PROGRESS_MONO, -1L) >= 0L
                || !bootScope(app).equals(p.getString(KEY_BOOT_SCOPE, ""))) return;
        String type = settlement ? "SETTLED" : "CANONICAL";
        String stage = settlement ? SettlementTaskStoreR36.COMPLETED : "CANONICAL_COMMITTED";
        SQLiteDatabase db = store.readable();
        String database = RuntimeObservationScopeR341.databaseIdentity(db);
        if (RuntimeObservationScopeR341.UNRESOLVED.equals(database)) return;
        try (Cursor row = db.rawQuery("SELECT e.event_id,s.sample_mono_at FROM " + RuntimeEventLedgerR926.TABLE_EVENT
                + " e JOIN " + RuntimeObservationScopeR341.EVENT_SCOPE + " s ON s.event_id=e.event_id"
                + " WHERE e.target_issue=? AND e.event_type=? AND e.stage=? AND s.database_identity=?"
                + " AND s.release_id=? AND s.source_commit=? AND s.process_epoch=? AND s.boot_id=?"
                + " AND s.sample_mono_at>? ORDER BY s.sample_mono_at LIMIT 1",
                new String[]{issue, type, stage, database, ReleaseManifestR926.MANIFEST_SHA256,
                    SourceManifestR815.observationSourceCommit(), ProcessBootIdentityR906.id(),
                    IssueTimelineWriterR30.bootId(), Long.toString(accepted)})) {
            if (row.moveToFirst()) markBusinessRestored(app, recovery, row.getLong(1),
                    type + "|issue=" + issue + "|event=" + row.getString(0));
        }
    }

    private static void markBusinessRestored(Context app, long recovery, long commitElapsed, String detail) {
        synchronized (LOCK) {
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            if (!heartbeat.isUserEnabled()) return;
            SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long accepted = p.getLong(KEY_ACCEPTED_MONO, -1L);
            long started = p.getLong(KEY_STARTED_MONO, -1L);
            if (p.getLong(KEY_GENERATION, 0L) != recovery || accepted < 0L || commitElapsed <= accepted
                    || commitElapsed < started || p.getLong(KEY_PROGRESS_MONO, -1L) >= 0L
                    || !bootScope(app).equals(p.getString(KEY_BOOT_SCOPE, ""))) return;

            long now = System.currentTimeMillis();
            SharedPreferences.Editor update = p.edit();
            if (started < accepted) {
                update.putLong(KEY_SERVICE_STARTED_AT, now).putLong(KEY_STARTED_MONO, commitElapsed);
            }
            if (update.putString(KEY_STATE, State.COMPLETED.name())
                    .putLong(KEY_BUSINESS_RESTORED_AT, now).putLong(KEY_PROGRESS_MONO, commitElapsed)
                    .putString(KEY_BUSINESS_DETAIL, bounded("recovery=" + recovery + '|' + detail, 320))
                    .putString(KEY_LAST_DETAIL, "BUSINESS_COMMIT_OBSERVED").putString(KEY_REJECT_REASON, "")
                    .commit()) {
                heartbeat.markBusinessStage("B07_BUSINESS_RESTORED", evidence(app).summary());
            }
        }
    }

    /**
     * AutoFetchService historically calls this from onDestroy. A destroy is not completion unless
     * business recovery was independently observed; otherwise the recovery remains requestable.
     */
    public static void markServiceCompleted(Context context) {
        if (context == null) return;
        Context app = app(context);
        synchronized (LOCK) {
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!heartbeat.isUserEnabled()) {
                p.edit()
                        .putString(KEY_STATE, State.IDLE.name())
                        .putString(KEY_LAST_DETAIL, "USER_STOPPED_NO_RECOVERY")
                        .putString(KEY_REJECT_REASON, "USER_STOPPED_NO_RECOVERY")
                        .commit();
                BootReceiver.cancelAllRecovery(app, "b07:user_stop_completed");
                return;
            }
            Evidence x = evidence(app);
            if (x.businessActuallyRestored()) {
                p.edit()
                        .putString(KEY_STATE, State.COMPLETED.name())
                        .putString(KEY_LAST_DETAIL, "SERVICE_DESTROYED_AFTER_BUSINESS_RESTORED")
                        .commit();
            } else if (x.requestAt > 0L) {
                p.edit()
                        .putString(KEY_STATE, State.REQUESTED.name())
                        .putString(KEY_LAST_DETAIL, "SERVICE_DESTROYED_BEFORE_BUSINESS_RESTORED")
                        .commit();
            }
            heartbeat.markBusinessStage("B07_SERVICE_DESTROY", evidence(app).summary());
        }
    }

    public static Evidence evidence(Context context) {
        if (context == null)
            return new Evidence(0L, State.IDLE, 0, 0L, 0L, 0L, 0L, "", "", "CONTEXT_NULL",
                    RuntimeObservationScopeR341.LEGACY_UNSCOPED, -1L, -1L, -1L, -1L);
        SharedPreferences p = app(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Evidence(
                p.getLong(KEY_GENERATION, 0L),
                parse(p.getString(KEY_STATE, State.IDLE.name())),
                p.getInt(KEY_ATTEMPTS, 0),
                p.getLong(KEY_REQUEST_AT, 0L),
                p.getLong(KEY_ACCEPTED_AT, 0L),
                p.getLong(KEY_SERVICE_STARTED_AT, 0L),
                p.getLong(KEY_BUSINESS_RESTORED_AT, 0L),
                p.getString(KEY_REQUEST_REASON, ""),
                p.getString(KEY_BUSINESS_DETAIL, ""),
                p.getString(KEY_REJECT_REASON, ""),
                p.getString(KEY_BOOT_SCOPE, RuntimeObservationScopeR341.LEGACY_UNSCOPED),
                p.getLong(KEY_REQUEST_MONO, -1L), p.getLong(KEY_ACCEPTED_MONO, -1L),
                p.getLong(KEY_STARTED_MONO, -1L), p.getLong(KEY_PROGRESS_MONO, -1L));
    }

    public static long currentGeneration(Context context) {
        if (context == null) return 0L;
        return app(context)
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_GENERATION, 0L);
    }

    public static boolean fetchSettlementOnly(Context context) {
        if (context == null) return false;
        String state =
                app(context)
                        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString(KEY_STATE, "");
        return State.FETCH_SETTLEMENT_ONLY.name().equals(state);
    }

    private static State parse(String value) {
        try {
            return State.valueOf(safe(value));
        } catch (IllegalArgumentException failure) {
            return State.IDLE;
        }
    }

    private static Context app(Context context) {
        Context application = context.getApplicationContext();
        return application == null ? context : application;
    }

    private static String bounded(String value, int max) {
        String clean = safe(value);
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
