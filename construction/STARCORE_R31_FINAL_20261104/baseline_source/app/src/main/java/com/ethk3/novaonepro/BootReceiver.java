package com.ethk3.novaonepro;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserManager;

import java.lang.reflect.Method;

/**
 * 轻量进程外恢复入口。
 *
 * <p>只读写 ServiceHeartbeatStore，不打开主 SQLite，不运行抓取/概率/策略， 不创建 Decision、Trade 或 BET。Alarm/Job
 * 只负责在主前台服务死亡后请求恢复。
 */
public final class BootReceiver extends BroadcastReceiver {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION =
            AppConfig.R36_RUNTIME_REPAIR_VERSION + "|START_SERVICE_AND_RECONCILE_ONLY";
    private static final int REQUEST_WATCHDOG = 7429;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!allowedRuntimeTriggerCP12(action)) return;
        final Context app=appContext(context);
        final Intent copy=intent==null?null:new Intent(intent);
        final PendingResult pending=goAsync();
        RecoveryDispatchR965.submit(()->receiveOffMain(app,copy),()->{if(pending!=null)pending.finish();});
    }

    private static boolean allowedRuntimeTriggerCP12(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                        || Intent.ACTION_USER_UNLOCKED.equals(action)))
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)
                || AppConfig.ACTION_SERVICE_WATCHDOG.equals(action);
    }

    private static void receiveOffMain(Context context, Intent intent) {
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        String action = intent == null ? "" : safe(intent.getAction());
        if (!isUserUnlocked(app)) {
            state.noteLockedBootOnly(action);
            return; // ACTION_USER_UNLOCKED re-enters the original owner after unlock.
        }
        if(Intent.ACTION_USER_UNLOCKED.equals(action)||Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))KeyProvider.reloadAsync(app);
        state.markReceiver(action);

        long age = state.getHeartbeatAgeMs();
        if (AppConfig.ACTION_SERVICE_WATCHDOG.equals(action)) {
            state.markAlarmTriggered(action, age);
            state.markRecoveryTrigger("alarm:" + action);
        }

        if (!state.isUserEnabled()) {
            cancelAllRecovery(app, "receiver:user_disabled");
            return;
        }

        boolean lockedBoot = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);
        boolean bootLike =
                Intent.ACTION_BOOT_COMPLETED.equals(action)
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            && (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                                || Intent.ACTION_USER_UNLOCKED.equals(action)))
                        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                        || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
                                .equals(action);
        boolean userUnlocked = isUserUnlocked(app);
        boolean businessStale = state.isBusinessStale();
        ServiceHeartbeatStore.V57RuntimeTruth v57Truth = state.v57RuntimeTruth();
        state.markBusinessStage("V57_B13_BOOT_TRUTH", v57Truth.summary());
        state.markBusinessStage(
                "RECEIVER_CHECK",
                "action="
                        + action
                        + ",heartbeatAge="
                        + age
                        + ",businessStale="
                        + businessStale
                        + ",userUnlocked="
                        + userUnlocked);
        boolean shouldStart = userUnlocked && (bootLike || !v57Truth.healthy() || businessStale);
        if (lockedBoot && !userUnlocked) {
            state.markRecoveryResult("receiver", true, "LOCKED_BOOT_DEFER_UNTIL_USER_UNLOCKED");
            scheduleAllRecovery(app, "locked_boot_deferred");
            return;
        }

        if (shouldStart) {
            String reason =
                    "receiver:" + action + ":age=" + age + ":businessStale=" + businessStale;
            boolean ok = startAutoFetchService(app, reason);
            state.markRecoveryEvaluation(
                    "receiver", ok, ok ? "start_evaluated" : "start_failed_or_deferred");
            if (ok) state.setLastReason("RECOVERY_REQUESTED:" + reason);
        } else {
            state.markRecoveryEvaluation("receiver", true, "heartbeat_alive");
        }

        scheduleAllRecovery(app, "receiver:" + action);
    }

    public static boolean startAutoFetchService(Context context, String reason) {
        return startAutoFetchService(context, reason, false);
    }

    /**
     * R915 durable reconcile entry.
     *
     * <p>A healthy process heartbeat only proves that the last pulse happened; it does not prove
     * that fetch/analysis/settlement loops are still armed.  Alarm, JobScheduler and task-removal
     * recovery therefore use this entry to deliver an idempotent START command even while the
     * process looks healthy.  User-disabled and locked-user boundaries remain fail-closed and the
     * recovery lease still prevents a start storm.</p>
     */
    public static boolean reconcileAutoFetchService(Context context, String reason) {
        return startAutoFetchService(context, "R915_RECONCILE:" + safe(reason), true);
    }

    private static boolean startAutoFetchService(
            Context context, String reason, boolean forceBusinessReconcile) {
        if (context == null) return false;
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        String safeReason = safe(reason);
        if (safeReason.startsWith("manual_") || safeReason.startsWith("ui_manual_")) {
            state.setUserEnabled(true);
            state.markUiStartRequested(safeReason);
        }
        if (!state.isUserEnabled()) return false;
        if (!isUserUnlocked(app)) {
            state.markRecoveryResult("startAutoFetchService", true, "DEFER_UNTIL_USER_UNLOCKED");
            scheduleAllRecovery(app, "defer_until_unlocked:" + safe(reason));
            return false;
        }
        if (!forceBusinessReconcile
                && state.isServiceHealthyForRecovery()
                && !state.isBusinessStale()) {
            state.markRecoveryResult("startAutoFetchService", true, "SKIP_HEALTHY_SERVICE");
            return true;
        }
        if (!state.tryAcquireRecoveryLease(reason)) {
            state.markRecoveryResult("startAutoFetchService", true, "SKIP_RECOVERY_LEASE_ACTIVE");
            return true;
        }
        long age = state.getHeartbeatAgeMs();
        try {
            Intent i = new Intent(app, AutoFetchService.class);
            i.setAction(AppConfig.ACTION_SERVICE_START);
            i.setPackage(AppConfig.PACKAGE_NAME);
            i.putExtra("start_reason", safe(reason));
            i.putExtra("force_business_reconcile", forceBusinessReconcile);
            state.markRecoveryRequest(reason, age);
            state.markStartIntentRequested(reason);
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(i);
            else app.startService(i);
            state.markRecoveryResult(
                    "startAutoFetchService",
                    true,
                    (forceBusinessReconcile ? "RECONCILE_REQUESTED:" : "START_REQUESTED:")
                            + safe(reason));
            return true;
        } catch (Throwable e) {
            String err = safeError(e);
            state.releaseRecoveryLease("start_failed");
            state.setLastReason("START_ERROR:" + err);
            state.markRecoveryResult("startAutoFetchService", false, err);
            return false;
        }
    }

    public static boolean forceRearmAutoFetchService(Context context, String reason) {
        if (context == null) return false;
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        String safeReason = safe(reason);
        if (safeReason.startsWith("manual_")
                || safeReason.startsWith("ui_manual_")
                || safeReason.startsWith("FORCE_REARM")) {
            state.setUserEnabled(true);
            state.markUiStartRequested(safeReason);
        }
        if (!state.isUserEnabled()) return false;
        if (!isUserUnlocked(app)) {
            scheduleAllRecovery(app, "force_rearm_locked:" + safe(reason));
            return false;
        }
        boolean ok = reconcileAutoFetchService(app, "FORCE_REARM:" + safe(reason));
        state.markRecoveryEvaluation(
                "force_rearm",
                ok,
                ok
                        ? "LEASED_START_EVALUATED:" + safe(reason)
                        : "START_DEFERRED_OR_FAILED:" + safe(reason));
        scheduleAllRecovery(app, (ok ? "force_rearm:" : "force_rearm_fallback:") + safe(reason));
        return ok;
    }

    public static boolean stopAutoFetchService(Context context) {
        if (context == null) return false;
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        state.setUserEnabled(false);
        state.markStop("user_stop");
        cancelAllRecovery(app, "user_stop");
        try {
            Intent i = new Intent(app, AutoFetchService.class);
            i.setAction(AppConfig.ACTION_SERVICE_STOP);
            i.setPackage(AppConfig.PACKAGE_NAME);
            try {
                app.startService(i);
            } catch (Throwable e) {
                app.stopService(new Intent(app, AutoFetchService.class));
            }
            return true;
        } catch (Throwable e) {
            state.markRecoveryResult("user_stop", false, safeError(e));
            return false;
        }
    }

    public static void persistServiceUserEnabled(Context context, boolean enabled) {
        if (context != null) new ServiceHeartbeatStore(appContext(context)).setUserEnabled(enabled);
    }

    public static boolean isServiceUserEnabled(Context context) {
        return context != null && new ServiceHeartbeatStore(appContext(context)).isUserEnabled();
    }

    public static void scheduleAllRecovery(Context context, String reason) {
        if (context == null) return;
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(appContext(context));
        if (!state.isUserEnabled()) return;
        scheduleWatchdog(
                context,
                reason,
                Math.min(
                        AppConfig.SERVICE_WATCHDOG_ALARM_DELAY_MS,
                        AppConfig.B07R2_RUNTIME_GUARD_INTERVAL_MS));
        scheduleJobWatchdog(context, reason);
        com.ethk3.novaonepro.strategyv2.ReplayBurstSchedulerSU1.schedule(appContext(context));
    }

    public static void scheduleWatchdog(Context context, String reason, long delayMs) {
        if (context == null) return;
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        if (!state.isUserEnabled()) return;
        try {
            AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            if (am == null) throw new IllegalStateException("AlarmManager unavailable");
            Intent i = new Intent(app, InternalWatchdogReceiverR815.class);
            i.setAction(InternalWatchdogReceiverR815.ACTION);
            i.setPackage(AppConfig.PACKAGE_NAME);
            i.putExtra(InternalWatchdogReceiverR815.EXTRA_REASON, safe(reason));
            i.putExtra(
                    InternalWatchdogReceiverR815.EXTRA_GENERATION,
                    RecoveryCoordinatorR815.currentGeneration(app) + 1L);
            PendingIntent pi = PendingIntent.getBroadcast(app, REQUEST_WATCHDOG, i, pendingFlags());
            long realDelay = Math.max(5000L, delayMs);
            long atElapsed = SystemClock.elapsedRealtime() + realDelay;
            long atWall = System.currentTimeMillis() + realDelay;
            String mode;
            if (Build.VERSION.SDK_INT >= 23) {
                boolean exactAllowed = canScheduleExactAlarmsCompat(am);
                try {
                    if (exactAllowed) {
                        am.setExactAndAllowWhileIdle(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi);
                        mode = "EXACT_AND_ALLOW_WHILE_IDLE";
                    } else {
                        am.setAndAllowWhileIdle(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi);
                        mode = "INEXACT_ALLOW_WHILE_IDLE_PERMISSION_MISSING";
                    }
                } catch (Throwable e) {
                    am.setWindow(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            atElapsed,
                            AppConfig.SERVICE_WATCHDOG_WINDOW_MS,
                            pi);
                    mode = "FALLBACK_SET_WINDOW:" + safeError(e);
                }
                state.markBusinessStage(
                        "ALARM_CAPABILITY", exactAllowed ? "EXACT_ALLOWED" : "EXACT_NOT_ALLOWED");
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setWindow(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        atElapsed,
                        AppConfig.SERVICE_WATCHDOG_WINDOW_MS,
                        pi);
                mode = "SET_WINDOW";
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi);
                mode = "SET";
            }
            state.markAlarmScheduled(mode, atWall, reason);
            state.setLastReason(reason);
        } catch (Throwable e) {
            state.setAlarmStatus("ERROR:" + safeError(e));
            state.markRecoveryResult("schedule_alarm", false, safeError(e));
        }
    }

    public static boolean scheduleJobWatchdog(Context context, String reason) {
        if (context == null || Build.VERSION.SDK_INT < 21) return false;
        Context app = appContext(context);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        if (!state.isUserEnabled()) return false;
        try {
            JobScheduler scheduler =
                    (JobScheduler) app.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) throw new IllegalStateException("JobScheduler unavailable");
            JobInfo.Builder b =
                    new JobInfo.Builder(
                            AppConfig.SERVICE_WATCHDOG_JOB_ID,
                            new ComponentName(app, ServiceWatchdogJobService.class));
            b.setMinimumLatency(AppConfig.SERVICE_JOB_MIN_LATENCY_MS);
            b.setOverrideDeadline(AppConfig.SERVICE_JOB_DEADLINE_MS);
            b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
            b.setPersisted(true);
            int result = scheduler.schedule(b.build());
            String status =
                    result == JobScheduler.RESULT_SUCCESS
                            ? "SCHEDULED"
                            : "SCHEDULE_FAILED:" + result;
            state.markJobScheduled(status, reason);
            state.setLastReason(reason);
            return result == JobScheduler.RESULT_SUCCESS;
        } catch (Throwable e) {
            String err = safeError(e);
            state.setJobStatus("ERROR:" + err);
            state.markRecoveryResult("schedule_job", false, err);
            return false;
        }
    }

    public static void cancelAllRecovery(Context context, String reason) {
        if (context == null) return;
        com.ethk3.novaonepro.strategyv2.ReplayBurstSchedulerSU1.cancel(appContext(context));
        Context app = appContext(context);
        try {
            AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(app, InternalWatchdogReceiverR815.class);
            i.setAction(InternalWatchdogReceiverR815.ACTION);
            i.setPackage(AppConfig.PACKAGE_NAME);
            PendingIntent pi = PendingIntent.getBroadcast(app, REQUEST_WATCHDOG, i, pendingFlags());
            if (am != null) am.cancel(pi);
            pi.cancel();
        } catch (Throwable ignore) {
        }
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                JobScheduler js =
                        (JobScheduler) app.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (js != null) js.cancel(AppConfig.SERVICE_WATCHDOG_JOB_ID);
            } catch (Throwable ignore) {
            }
        }
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        state.setAlarmStatus("CANCELLED:" + safe(reason));
        state.setJobStatus("CANCELLED:" + safe(reason));
    }

    public static void cancelWatchdog(Context context) {
        cancelAllRecovery(context, "cancel_watchdog");
    }

    private static int pendingFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    private static boolean canScheduleExactAlarmsCompat(AlarmManager am) {
        if (am == null) return false;
        if (Build.VERSION.SDK_INT < 31) return true;
        try {
            Method m = AlarmManager.class.getMethod("canScheduleExactAlarms");
            Object v = m.invoke(am);
            return v instanceof Boolean && ((Boolean) v).booleanValue();
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean isUserUnlocked(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 24) return true;
        try {
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            return um == null || um.isUserUnlocked();
        } catch (Throwable e) {
            return true;
        }
    }

    private static Context appContext(Context c) {
        try {
            Context a = c.getApplicationContext();
            return a == null ? c : a;
        } catch (Throwable e) {
            return c;
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 260 ? s.substring(0, 260) : s;
    }

    private static String safeError(Throwable e) {
        if (e == null) return "unknown";
        String s = e.getClass().getSimpleName() + ":" + String.valueOf(e.getMessage());
        return s.length() > 220 ? s.substring(0, 220) : s;
    }
}
