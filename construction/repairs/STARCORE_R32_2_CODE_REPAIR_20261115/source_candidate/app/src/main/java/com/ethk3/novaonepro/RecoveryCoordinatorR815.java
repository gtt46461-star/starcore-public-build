package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;

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

        Result(boolean accepted, long generation, State state, int attempts, String status) {
            this.accepted = accepted;
            this.generation = Math.max(0L, generation);
            this.state = state == null ? State.IDLE : state;
            this.attempts = Math.max(0, attempts);
            this.status = safe(status);
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
                    + (accepted ? 1 : 0);
        }
    }

    public static final class Evidence {
        public final long generation;
        public final State state;
        public final int attempts;
        public final long requestAt, acceptedAt, serviceStartedAt, businessRestoredAt;
        public final String requestReason, businessDetail, rejectReason;

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
                String rejectReason) {
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
        }

        public boolean requestAccepted() {
            return requestAt > 0L && acceptedAt >= requestAt;
        }

        public boolean serviceActuallyStarted() {
            return requestAccepted() && serviceStartedAt >= acceptedAt;
        }

        public boolean businessActuallyRestored() {
            return serviceActuallyStarted() && businessRestoredAt >= serviceStartedAt;
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
                    + "|reason="
                    + bounded(requestReason, 96)
                    + "|business="
                    + bounded(businessDetail, 192)
                    + "|reject="
                    + bounded(rejectReason, 96);
        }
    }

    private RecoveryCoordinatorR815() {}

    public static Result request(Context context, String reason, long requestedGeneration) {
        if (context == null) return new Result(false, 0L, State.IDLE, 0, "CONTEXT_NULL");
        Context app = app(context);
        synchronized (LOCK) {
            SharedPreferences preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            long now = System.currentTimeMillis();
            long current = preferences.getLong(KEY_GENERATION, 0L);
            int attempts = preferences.getInt(KEY_ATTEMPTS, 0);
            State prior = parse(preferences.getString(KEY_STATE, State.IDLE.name()));

            if (!heartbeat.isUserEnabled()) {
                preferences.edit()
                        .putString(KEY_STATE, State.IDLE.name())
                        .putString(KEY_REJECT_REASON, "USER_DISABLED_FAIL_CLOSED")
                        .putString(KEY_LAST_DETAIL, "USER_DISABLED_FAIL_CLOSED")
                        .commit();
                heartbeat.markRecoveryEvaluation(
                        "r815", false, "USER_DISABLED_FAIL_CLOSED_NO_RESTART");
                return new Result(false, current, State.IDLE, attempts, "USER_DISABLED_FAIL_CLOSED");
            }

            long generation = requestedGeneration > 0L ? requestedGeneration : current + 1L;
            long window = preferences.getLong(KEY_WINDOW_START, 0L);
            if (generation < current)
                return new Result(false, current, prior, attempts, "STALE_GENERATION_REJECTED");
            if (generation == current
                    && preferences.getLong(KEY_ACCEPTED_AT, 0L) > 0L
                    && now - preferences.getLong(KEY_REQUEST_AT, 0L) >= 0L
                    && now - preferences.getLong(KEY_REQUEST_AT, 0L) < 15000L
                    && (prior == State.REQUESTED
                            || prior == State.RUNNING
                            || prior == State.FETCH_SETTLEMENT_ONLY)) {
                return new Result(true, current, prior, attempts, "COALESCED_SAME_GENERATION");
            }
            if (window <= 0L || now - window > WINDOW_MS) {
                window = now;
                attempts = 0;
            }
            if (attempts >= MAX_ATTEMPTS) {
                preferences.edit().putString(KEY_STATE, State.FETCH_SETTLEMENT_ONLY.name())
                        .putString(KEY_REJECT_REASON, "RECOVERY_RATE_LIMITED").commit();
                return new Result(false, current, State.FETCH_SETTLEMENT_ONLY, attempts, "RECOVERY_RATE_LIMITED");
            }
            attempts++;
            State next = attempts >= MAX_ATTEMPTS ? State.FETCH_SETTLEMENT_ONLY : State.REQUESTED;
            preferences.edit()
                    .putLong(KEY_GENERATION, generation)
                    .putLong(KEY_WINDOW_START, window)
                    .putInt(KEY_ATTEMPTS, attempts)
                    .putString(KEY_STATE, next.name())
                    .putString(KEY_LAST_DETAIL, bounded(reason, 192))
                    .putLong(KEY_REQUEST_AT, now)
                    .putLong(KEY_ACCEPTED_AT, 0L)
                    .putLong(KEY_SERVICE_STARTED_AT, 0L)
                    .putLong(KEY_BUSINESS_RESTORED_AT, 0L)
                    .putString(KEY_REQUEST_REASON, bounded(reason, 192))
                    .putString(KEY_BUSINESS_DETAIL, "")
                    .putString(KEY_REJECT_REASON, "")
                    .commit();
            heartbeat.markRecoveryTrigger("r815:" + bounded(reason, 128));
            heartbeat.markBusinessStage("B07_RECOVERY_REQUESTED", evidence(app).summary());
            if (next == State.FETCH_SETTLEMENT_ONLY)
                heartbeat.markBusinessStage("R815_SAFE_DEGRADED", "FETCH_SETTLEMENT_ONLY");

            boolean started =
                    BootReceiver.reconcileAutoFetchService(app, "r815:" + bounded(reason, 128));
            if (started) {
                preferences.edit()
                        .putLong(KEY_ACCEPTED_AT, System.currentTimeMillis())
                        .putString(KEY_LAST_DETAIL, "START_OR_RECONCILE_REQUEST_ACCEPTED")
                        .commit();
                heartbeat.markBusinessStage("B07_RECOVERY_ACCEPTED", evidence(app).summary());
            } else {
                preferences.edit()
                        .putString(KEY_REJECT_REASON, "START_DEFERRED_OR_FAILED")
                        .putString(KEY_LAST_DETAIL, "START_DEFERRED_OR_FAILED")
                        .commit();
            }
            if (!started && next != State.FETCH_SETTLEMENT_ONLY)
                return new Result(false, generation, next, attempts, "START_DEFERRED_OR_FAILED");
            return new Result(
                    true,
                    generation,
                    next,
                    attempts,
                    started ? "START_OR_RECONCILE_REQUESTED" : "SAFE_DEGRADED");
        }
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
            long accepted = p.getLong(KEY_ACCEPTED_AT, 0L);
            long request = p.getLong(KEY_REQUEST_AT, 0L);
            SharedPreferences.Editor e = p.edit();
            if (request > 0L && accepted >= request) {
                e.putString(KEY_STATE, State.RUNNING.name())
                        .putLong(KEY_SERVICE_STARTED_AT, System.currentTimeMillis())
                        .putString(KEY_LAST_DETAIL, "SERVICE_CREATED_AFTER_RECOVERY_ACCEPT");
            } else {
                e.putString(KEY_LAST_DETAIL, "SERVICE_CREATED_WITHOUT_ACTIVE_RECOVERY");
            }
            e.commit();
            heartbeat.markBusinessStage("B07_SERVICE_STARTED", evidence(app).summary());
        }
    }

    /**
     * Called only from an independent watchdog observation that already proved V57 business truth
     * healthy and the runtime lease fresh. If reconcile targeted an already-running service, Android
     * legitimately skips onCreate; in that case this independent healthy observation is the proof
     * that the accepted reconcile reached a live runtime. startService()/reconcile return alone is
     * never treated as recovery success.
     */
    public static void markBusinessRestored(Context context, String detail) {
        if (context == null) return;
        Context app = app(context);
        synchronized (LOCK) {
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            if (!heartbeat.isUserEnabled()) return;
            SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long request = p.getLong(KEY_REQUEST_AT, 0L);
            long accepted = p.getLong(KEY_ACCEPTED_AT, 0L);
            long started = p.getLong(KEY_SERVICE_STARTED_AT, 0L);
            long restored = p.getLong(KEY_BUSINESS_RESTORED_AT, 0L);
            if (request <= 0L || accepted < request) return;

            long now = System.currentTimeMillis();
            boolean inferredAlreadyRunning = started < accepted;
            if (inferredAlreadyRunning) started = now;
            if (restored >= started) return;

            SharedPreferences.Editor e = p.edit();
            if (inferredAlreadyRunning) {
                e.putLong(KEY_SERVICE_STARTED_AT, started)
                        .putString(KEY_LAST_DETAIL, "LIVE_SERVICE_OBSERVED_BY_HEALTHY_WATCHDOG");
            }
            e.putString(KEY_STATE, State.COMPLETED.name())
                    .putLong(KEY_BUSINESS_RESTORED_AT, now)
                    .putString(
                            KEY_BUSINESS_DETAIL,
                            bounded(
                                    (inferredAlreadyRunning
                                                    ? "ALREADY_RUNNING_RECONCILE|"
                                                    : "NEW_SERVICE_START|")
                                            + safe(detail),
                                    320))
                    .putString(KEY_LAST_DETAIL, "BUSINESS_TRUTH_RESTORED")
                    .putString(KEY_REJECT_REASON, "")
                    .commit();
            heartbeat.markBusinessStage("B07_BUSINESS_RESTORED", evidence(app).summary());
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
            return new Evidence(0L, State.IDLE, 0, 0L, 0L, 0L, 0L, "", "", "CONTEXT_NULL");
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
                p.getString(KEY_REJECT_REASON, ""));
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
