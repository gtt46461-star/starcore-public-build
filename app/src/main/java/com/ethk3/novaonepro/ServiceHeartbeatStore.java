package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;

/**
 * 轻量进程外服务状态存储。
 *
 * <p>只使用 SharedPreferences，不打开主 SQLite，不检查 Schema， 不计算概率，不生成 BET，不修改真实账本。
 *
 * <p>本版重点： 1. 一次心跳只做一次原子批量写，不再逐字段多次 commit； 2. 同时记录进程、抓取、分析、Alarm、Job、恢复结果； 3. 诊断直接读取本存储，不再误读旧
 * SQLite 心跳； 4. 普通区与 Device Protected Storage 双写，兼容开机恢复。
 */
public final class ServiceHeartbeatStore {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;

    private static final String PREF = "v20_service_runtime_light";
    private static final Object LOCK = new Object();
    // Constructor rejects Activity/null fallback; only process-lifetime Application context is retained.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static volatile Context LAST_APP_CONTEXT;

    private final Context app;

    public ServiceHeartbeatStore(Context context) {
        Context c = context == null ? null : context.getApplicationContext();
        if (c == null && !(context instanceof android.app.Application))
            throw new IllegalArgumentException("APPLICATION_CONTEXT_REQUIRED");
        app = c == null ? context : c;
        if (app != null) LAST_APP_CONTEXT = app;
    }

    public void setUserEnabled(boolean enabled) {
        synchronized(DatabaseCoordinator.RUNTIME_TRANSITION_R82) {
            boolean refused=enabled&&DatabaseCoordinator.isExplicitMaintenanceActiveR82();
            long now=System.currentTimeMillis();RuntimePatch p=new RuntimePatch();
            p.putBoolean("user_enabled",enabled&&!refused);
            p.putLong(enabled&&!refused?"last_enable_at":"last_disable_at",now);
            p.putString("runtime_version",AppConfig.FINAL_INTEGRATION_FIX_VERSION);
            if(refused)p.putString("last_reason","MAINTENANCE_ACTIVE_START_REFUSED");
            commitPatch(p);
        }
    }

    /** Minimal direct-boot record: no credential-protected preferences, no SQLite. */
    public void noteLockedBootOnly(String action) {
        Context dc = deviceContext();
        if (dc == null) return;
        SharedPreferences minimal = prefs(dc);
        boolean enabled = minimal.getBoolean("user_enabled", false);
        minimal.edit().putBoolean("locked_restore_pending", enabled)
                .putString("locked_restore_action", action == null ? "" : action).commit();
    }

    public boolean isUserEnabled() {
        return getBoolean("user_enabled", true);
    }

    public void markUiStartRequested(String reason) {
        synchronized(DatabaseCoordinator.RUNTIME_TRANSITION_R82) {
            RuntimePatch p=new RuntimePatch();boolean refused=DatabaseCoordinator.isExplicitMaintenanceActiveR82();
            p.putBoolean("user_enabled",!refused);p.putLong("ui_start_requested_at",System.currentTimeMillis());
            p.putString("ui_start_requested_reason",safe(reason));
            p.putString("last_reason",refused?"MAINTENANCE_ACTIVE_START_REFUSED":"UI_START_REQUEST:"+safe(reason));
            commitPatch(p);
        }
    }

    public void markServiceStarting(String reason) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        // Starting a process is not permission to undo a user stop.
        p.putLong("service_starting_at", now);
        p.putLong("service_ready_at", 0L);
        p.putLong("heartbeat_at", now);
        p.putString("heartbeat_source", "starting:" + safe(reason));
        p.putString("init_status", "STARTING");
        p.putString("init_stage", "SERVICE_ON_CREATE");
        p.putLong("init_stage_at", now);
        p.putString("init_stage_detail", safe(reason));
        p.putString("init_last_error", "");
        p.putInt("init_retry_count", 0);
        p.putInt("process_id", safePid());
        p.putInt("process_alive", 1);
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        p.putString("last_reason", "SERVICE_STARTING:" + safe(reason));
        commitPatch(p);
    }

    public void markInitRetry(int attempt, String reason) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong("init_retry_at", now);
        p.putInt("init_retry_count", Math.max(0, attempt));
        p.putString("init_status", "RETRYING");
        p.putString("init_stage", "RETRY_DELAY");
        p.putLong("init_stage_at", now);
        p.putString("init_retry_reason", safe(reason));
        commitPatch(p);
    }

    public void markInitStage(String stage, String detail) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putString("init_status", "STARTING");
        p.putString("init_stage", safe(stage));
        p.putLong("init_stage_at", now);
        p.putString("init_stage_detail", safe(detail));
        p.putLong("heartbeat_at", now);
        p.putString("heartbeat_source", "init:" + safe(stage));
        p.putInt("process_alive", 1);
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        commitPatch(p);
    }

    public void markInitFailure(String error) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong("init_failure_at", now);
        p.putString("init_status", "FAILED");
        if (getString("init_stage", "").length() == 0) p.putString("init_stage", "UNKNOWN");
        p.putString("init_last_error", safe(error));
        p.putLong("heartbeat_at", now);
        p.putString("heartbeat_source", "init_failed_guard");
        p.putInt("process_alive", 1);
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        commitPatch(p);
    }

    /** 单次原子心跳。 */
    public void writeHeartbeat(String source) {
        long now = System.currentTimeMillis();
        RuntimePatch p = new RuntimePatch();
        p.putLong("heartbeat_at", now);
        p.putString("heartbeat_source", safe(source));
        p.putString("runtime_version", AppConfig.FINAL_INTEGRATION_FIX_VERSION);
        p.putString("fix_cohort", AppConfig.ACTIVE_FIX_COHORT);
        p.putString("probability_pipeline", AppConfig.ACTIVE_PROBABILITY_PIPELINE_VERSION);
        p.putInt("process_id", safePid());
        p.putInt("process_alive", 1);
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        if (getLong("process_started_at", 0L) <= 0L) p.putLong("process_started_at", now);
        commitPatch(p);
    }

    public void markForegroundNotification() {
        RuntimePatch p = new RuntimePatch();
        p.putLong("foreground_notification_at", System.currentTimeMillis());
        p.putString("foreground_status", "POSTED");
        commitPatch(p);
    }

    public void markOnStartCommand(String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("on_start_command_at", System.currentTimeMillis());
        p.putString("on_start_command_reason", safe(reason));
        commitPatch(p);
    }

    public void markStart(String reason) {
        long now = System.currentTimeMillis();
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_start_at", now);
        p.putLong("heartbeat_at", now);
        p.putLong("process_started_at", now);
        p.putString("heartbeat_source", "start:" + safe(reason));
        p.putString("last_reason", safe(reason));
        p.putString("runtime_version", AppConfig.FINAL_INTEGRATION_FIX_VERSION);
        p.putString("fix_cohort", AppConfig.ACTIVE_FIX_COHORT);
        p.putString("probability_pipeline", AppConfig.ACTIVE_PROBABILITY_PIPELINE_VERSION);
        p.putInt("process_id", safePid());
        p.putInt("process_alive", 1);
        commitPatch(p);
    }

    public void markStop(String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_stop_at", System.currentTimeMillis());
        p.putString("last_reason", safe(reason));
        p.putInt("process_alive", 0);
        p.putLong("runtime_lease_until", 0L);
        commitPatch(p);
    }

    public void markTaskRemoved() {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_task_removed_at", System.currentTimeMillis());
        p.putString("last_reason", "task_removed");
        commitPatch(p);
    }

    public void markDestroy(String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_destroy_at", System.currentTimeMillis());
        p.putString("last_destroy_reason", safe(reason));
        p.putString("last_reason", safe(reason));
        p.putInt("process_alive", 0);
        p.putLong("runtime_lease_until", 0L);
        commitPatch(p);
    }

    public void markReceiver(String action) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_receiver_at", System.currentTimeMillis());
        p.putString("last_receiver_action", safe(action));
        p.putString("last_reason", "receiver:" + safe(action));
        commitPatch(p);
    }

    public void markAlarmScheduled(String mode, long triggerAtWallMs, String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("alarm_scheduled_at", System.currentTimeMillis());
        p.putLong("alarm_trigger_at", triggerAtWallMs);
        p.putString("alarm_mode", safe(mode));
        p.putString("alarm_status", "SCHEDULED:" + safe(mode));
        p.putString("alarm_schedule_reason", safe(reason));
        commitPatch(p);
    }

    public void markAlarmTriggered(String action, long heartbeatAgeMs) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("alarm_last_triggered_at", System.currentTimeMillis());
        p.putLong("alarm_trigger_heartbeat_age", heartbeatAgeMs);
        p.putString("alarm_last_action", safe(action));
        p.putString("alarm_status", "TRIGGERED");
        commitPatch(p);
    }

    public void markJobScheduled(String status, String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("job_scheduled_at", System.currentTimeMillis());
        p.putString("job_status", safe(status));
        p.putString("job_schedule_reason", safe(reason));
        commitPatch(p);
    }

    public void markJobTriggered(long heartbeatAgeMs) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("job_last_triggered_at", System.currentTimeMillis());
        p.putLong("job_trigger_heartbeat_age", heartbeatAgeMs);
        p.putString("job_status", "TRIGGERED");
        commitPatch(p);
    }

    public void markJobFinished(String result) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("job_last_finished_at", System.currentTimeMillis());
        p.putString("job_last_result", safe(result));
        p.putString("job_status", "FINISHED:" + safe(result));
        commitPatch(p);
    }

    public void markRecoveryRequest(String source, long heartbeatAgeMs) {
        synchronized (LOCK) {
            int n = getInt("restart_request_count", 0) + 1;
            RuntimePatch p = new RuntimePatch();
            p.putInt("restart_request_count", n);
            p.putLong("last_restart_request_at", System.currentTimeMillis());
            p.putLong("last_restart_heartbeat_age", heartbeatAgeMs);
            p.putString("last_restart_reason", safe(source));
            p.putString("last_reason", safe(source));
            commitPatchLocked(p);
        }
    }

    public void markRecoveryResult(String source, boolean ok, String detail) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_recovery_result_at", System.currentTimeMillis());
        p.putString("last_recovery_source", safe(source));
        p.putString("last_recovery_result", ok ? "OK" : "FAIL");
        p.putString("last_recovery_detail", safe(detail));
        commitPatch(p);
    }

    /** Records a receiver/watchdog decision without replacing the last actual start outcome. */
    public void markRecoveryEvaluation(String source, boolean accepted, String detail) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_recovery_evaluation_at", System.currentTimeMillis());
        p.putString("last_recovery_evaluation_source", safe(source));
        p.putString("last_recovery_evaluation", accepted ? "ACCEPTED" : "REJECTED");
        p.putString("last_recovery_evaluation_detail", safe(detail));
        commitPatch(p);
    }

    public void markFetchRunning(String issue) {
        markStage("fetch", AppConfig.BUSINESS_FETCH_RUNNING, issue, "");
    }

    public void markFetchFailed(String issue, String error) {
        markStage("fetch", AppConfig.BUSINESS_FETCH_FAILED, issue, error);
    }

    public void markAnalysisRunning(String issue) {
        markStage("analysis", AppConfig.BUSINESS_ANALYSIS_RUNNING, issue, "");
    }

    public void markAnalysisFailed(String issue, String error) {
        markStage("analysis", AppConfig.BUSINESS_ANALYSIS_FAILED, issue, error);
    }

    public void markSettlementRunning(String issue, int pending) {
        setPendingSettlementCount(pending);
        markStage("settlement", AppConfig.BUSINESS_SETTLEMENT_RUNNING, issue, "");
    }

    public void markSettlementIdle(String issue) {
        markStage("settlement", AppConfig.BUSINESS_SETTLEMENT_IDLE, issue, "");
    }

    public void markSettlementFailed(String issue, String error) {
        markStage("settlement", AppConfig.BUSINESS_SETTLEMENT_FAILED, issue, error);
    }

    public void setPendingSettlementCount(int count) {
        RuntimePatch p = new RuntimePatch();
        p.putInt("pending_settlement_count", Math.max(0, count));
        p.putLong("pending_settlement_updated_at", System.currentTimeMillis());
        applyTelemetryPatchR76(p);
    }

    private void markStage(String prefix, String state, String issue, String detail) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong(prefix + "_stage_at", now);
        p.putString(prefix + "_stage_state", safe(state));
        p.putString(prefix + "_stage_issue", safe(issue));
        p.putString(prefix + "_stage_detail", safe(detail));
        applyTelemetryPatchR76(p);
    }

    public void markFetchSuccess(String issue) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_fetch_success_at", System.currentTimeMillis());
        p.putString("last_fetch_issue", safe(issue));
        p.putString("last_reason", "fetch_success:" + safe(issue));
        p.putString("fetch_stage_state", AppConfig.BUSINESS_FETCH_SUCCEEDED);
        p.putString("fetch_stage_issue", safe(issue));
        p.putString("fetch_stage_detail", "");
        p.putLong("fetch_stage_at", System.currentTimeMillis());
        applyTelemetryPatchR76(p);
    }

    public void markAnalysisSuccess(String issue) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_analysis_success_at", System.currentTimeMillis());
        p.putString("last_analysis_issue", safe(issue));
        p.putString("analysis_stage_state", AppConfig.BUSINESS_ANALYSIS_SUCCEEDED);
        p.putLong("analysis_stage_at", System.currentTimeMillis());
        applyTelemetryPatchR76(p);
    }

    public void markSettlementSuccess(String issue) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_settlement_success_at", System.currentTimeMillis());
        p.putString("last_settlement_issue", safe(issue));
        p.putString("settlement_stage_state", AppConfig.BUSINESS_SETTLEMENT_SUCCEEDED);
        p.putLong("settlement_stage_at", System.currentTimeMillis());
        applyTelemetryPatchR76(p);
    }

    public void markBusinessStage(String stage, String detail) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("business_stage_at", System.currentTimeMillis());
        p.putString("business_stage", safe(stage));
        p.putString("business_detail", safe(detail));
        applyTelemetryPatchR76(p);
    }

    /** Persists business closure independently from process/service liveness. */
    public void writeBusinessTruth(
            RuntimeHealthLedgerR36.BusinessTruth truth,
            String teacherState,
            String learningState) {
        if (truth == null) return;
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong("business_truth_at", now);
        p.putString("business_truth_status", safe(truth.status));
        p.putString("business_truth_root_cause", safe(truth.rootCause));
        p.putString("business_latest_draw_issue", safe(truth.latestDrawIssue));
        p.putString("business_latest_canonical_issue", safe(truth.latestCanonicalIssue));
        p.putString("business_latest_settled_issue", safe(truth.latestSettledIssue));
        p.putString("business_latest_oos_issue", safe(truth.latestOosIssue));
        p.putInt("business_draw_canonical_lag", truth.drawToCanonicalLag);
        p.putInt("business_draw_settled_lag", truth.drawToSettledLag);
        p.putInt("business_settled_oos_lag", truth.settledToOosLag);
        p.putInt("business_deferred_pending", truth.deferredPending);
        p.putLong("business_analysis_p95_ms", truth.analysisP95Ms);
        p.putString("teacher_module_state", normalizeModuleState(teacherState));
        p.putString("learning_module_state", normalizeModuleState(learningState));
        applyTelemetryPatchR76(p);
    }

    public BusinessTruthSnapshot readBusinessTruth() {
        return new BusinessTruthSnapshot(
                getString("business_truth_status", "UNKNOWN"),
                getString("business_truth_root_cause", "READ_ERROR"),
                getString("business_latest_draw_issue", ""),
                getString("business_latest_canonical_issue", ""),
                getString("business_latest_settled_issue", ""),
                getString("business_latest_oos_issue", ""),
                getInt("business_draw_canonical_lag", -1),
                getInt("business_draw_settled_lag", -1),
                getInt("business_settled_oos_lag", -1),
                getInt("business_deferred_pending", -1),
                getLong("business_analysis_p95_ms", -1L),
                getString("teacher_module_state", "UNKNOWN"),
                getString("learning_module_state", "UNKNOWN"),
                getLong("business_truth_at", 0L));
    }

    private static String normalizeModuleState(String state) {
        String value = safe(state).toUpperCase(java.util.Locale.US);
        if ("RUNNING".equals(value)
                || "IDLE".equals(value)
                || "FAILED".equals(value)) return value;
        return "UNKNOWN";
    }

    public static final class BusinessTruthSnapshot {
        public final String status, rootCause, latestDraw, latestCanonical, latestSettled,
                latestOos, teacher, learning;
        public final int drawCanonicalLag, drawSettledLag, settledOosLag, deferredPending;
        public final long analysisP95Ms, updatedAt;

        public boolean currentClosureR71(String current,String target,long now) {
            return "HEALTHY".equals(status)&&TargetIssueResolver.exact(current,target).valid
                    &&latestDraw.equals(current)&&latestCanonical.equals(target)
                    &&latestSettled.equals(current)&&latestOos.equals(current)
                    &&updatedAt>0&&now>=updatedAt&&now-updatedAt<=15000L;
        }

        BusinessTruthSnapshot(
                String status,
                String rootCause,
                String latestDraw,
                String latestCanonical,
                String latestSettled,
                String latestOos,
                int drawCanonicalLag,
                int drawSettledLag,
                int settledOosLag,
                int deferredPending,
                long analysisP95Ms,
                String teacher,
                String learning,
                long updatedAt) {
            this.status = safe(status);
            this.rootCause = safe(rootCause);
            this.latestDraw = safe(latestDraw);
            this.latestCanonical = safe(latestCanonical);
            this.latestSettled = safe(latestSettled);
            this.latestOos = safe(latestOos);
            this.drawCanonicalLag = drawCanonicalLag;
            this.drawSettledLag = drawSettledLag;
            this.settledOosLag = settledOosLag;
            this.deferredPending = deferredPending;
            this.analysisP95Ms = analysisP95Ms;
            this.teacher = safe(teacher);
            this.learning = safe(learning);
            this.updatedAt = updatedAt;
        }
    }

    /** SUCCESS means the canonical transaction committed, never merely requested. */
    public void markAnalysisCanonicalCommitted(String target, long commitAt) {
        RuntimePatch p = new RuntimePatch();
        long at = commitAt > 0L ? commitAt : System.currentTimeMillis();
        p.putLong("last_analysis_success_at", at);
        p.putString("last_analysis_issue", safe(target));
        p.putString("analysis_stage_state", "CANONICAL_COMMITTED");
        p.putString("analysis_stage_issue", safe(target));
        p.putLong("analysis_stage_at", at);
        p.putString("analysis_progress_status", "OK");
        p.putInt("analysis_progress_percent", 100);
        p.putLong("analysis_progress_at", at);
        applyTelemetryPatchR76(p);
    }

    public void markSettlementTerminal(
            String target, String terminalStatus, String detail, long completedAt) {
        RuntimePatch p = new RuntimePatch();
        long at = completedAt > 0L ? completedAt : System.currentTimeMillis();
        p.putString("settlement_stage_state", safe(terminalStatus));
        p.putString("settlement_stage_issue", safe(target));
        p.putString("settlement_stage_detail", safe(detail));
        p.putLong("settlement_stage_at", at);
        p.putInt("pending_settlement_count", 0);
        p.putLong("pending_settlement_updated_at", at);
        if ("COMPLETED".equals(terminalStatus)) {
            p.putLong("last_settlement_success_at", at);
            p.putString("last_settlement_issue", safe(target));
        }
        applyTelemetryPatchR76(p);
    }

    public void markRecoveryTrigger(String source) {
        synchronized (LOCK) {
            RuntimePatch p = new RuntimePatch();
            p.putLong("recovery_trigger_at", System.currentTimeMillis());
            p.putString("recovery_trigger_source", safe(source));
            p.putInt("recovery_trigger_count", getInt("recovery_trigger_count", 0) + 1);
            commitPatchLocked(p);
        }
    }

    public void markStartIntentRequested(String source) {
        synchronized (LOCK) {
            RuntimePatch p = new RuntimePatch();
            p.putLong("start_intent_requested_at", System.currentTimeMillis());
            p.putString("start_intent_requested_source", safe(source));
            p.putInt("start_intent_request_count", getInt("start_intent_request_count", 0) + 1);
            commitPatchLocked(p);
        }
    }

    public int getRecoveryTriggerCount() {
        return getInt("recovery_trigger_count", 0);
    }

    public int getStartIntentRequestCount() {
        return getInt("start_intent_request_count", 0);
    }

    public void writeGuardPulse(String source) {
        long now = System.currentTimeMillis();
        RuntimePatch p = new RuntimePatch();
        p.putLong("heartbeat_at", now);
        p.putString("heartbeat_source", safe(source));
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        p.putString("runtime_guard_reason", safe(source));
        p.putInt("process_alive", 1);
        commitPatch(p);
    }

    public void markTeacherSlice(String status, int processed, String cursor, long costMs) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("last_teacher_slice_at", System.currentTimeMillis());
        p.putString("last_teacher_slice_status", safe(status));
        p.putInt("last_teacher_slice_processed", processed);
        p.putString("last_teacher_slice_cursor", safe(cursor));
        p.putLong("last_teacher_slice_cost_ms", costMs);
        applyTelemetryPatchR76(p);
    }

    public int incrementRestart(String reason) {
        markRecoveryRequest(reason, getHeartbeatAgeMs());
        return getInt("restart_request_count", 0);
    }

    public void markActualProcessStart(String reason) {
        synchronized (LOCK) {
            long generation = getLong("service_generation", 0L) + 1L;
            int actual = getInt("actual_process_start_count", 0) + 1;
            RuntimePatch p = new RuntimePatch();
            p.putLong("service_generation", generation);
            p.putLong("process_start_id", System.currentTimeMillis());
            p.putInt("actual_process_start_count", actual);
            p.putLong("actual_process_start_at", System.currentTimeMillis());
            p.putString("actual_process_start_reason", safe(reason));
            p.putString("runtime_version", VERSION);
            commitPatchLocked(p);
        }
    }

    public void markAnalysisProgress(String status, int percent, String stage, String issue) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong("analysis_progress_at", now);
        p.putInt("analysis_progress_percent", Math.max(0, Math.min(100, percent)));
        p.putString("analysis_progress_status", safe(status));
        p.putString("analysis_progress_stage", safe(stage));
        p.putString("analysis_progress_issue", safe(issue));
        applyTelemetryPatchR76(p);
    }

    public boolean isAnalysisProgressFresh() {
        long at = getLong("analysis_progress_at", 0L);
        String st = getString("analysis_progress_status", "");
        return "RUNNING".equals(st)
                && at > 0L
                && System.currentTimeMillis() - at <= AppConfig.ANALYSIS_PROGRESS_STALE_MS;
    }

    /** WAITING/REQUESTED is legitimate bounded work, not an immediate stale failure. */
    public boolean isAnalysisWorkFresh() {
        long at = getLong("analysis_progress_at", 0L);
        String st = getString("analysis_progress_status", "");
        boolean active =
                "RUNNING".equals(st) || "WAITING".equals(st) || "REQUESTED".equals(st);
        return active
                && at > 0L
                && System.currentTimeMillis() - at <= AppConfig.ANALYSIS_PROGRESS_STALE_MS;
    }

    public boolean tryAcquireRecoveryLease(String reason) {
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            long until = getLong("recovery_lease_until", 0L);
            long acquired = getLong("recovery_lease_acquired_at", 0L);
            long ready = getLong("service_ready_at", 0L);
            // Recovery and service run in the app's main process. A new main PID
            // proves that a lease from the previous PID cannot still own a start.
            // Fall back to the heartbeat owner for pre-R13 leases without this field.
            boolean previousProcess = isPreviousMainProcessR13(
                    getInt("recovery_lease_process_id", getInt("process_id", 0)));
            boolean abandoned =
                    until > now && (previousProcess || (acquired > 0L
                            && ready < acquired
                            && now - acquired > AppConfig.B07_RECOVERY_LEASE_STEAL_MS
                            && isHeartbeatExpired()));
            if (until > now && !abandoned) return false;
            RuntimePatch p = new RuntimePatch();
            p.putLong("recovery_lease_until", now + AppConfig.RECOVERY_LEASE_MS);
            p.putString("recovery_lease_reason", safe(reason));
            p.putLong("recovery_lease_acquired_at", now);
            p.putInt("recovery_lease_process_id", safePid());
            p.putString("recovery_lease_mode", abandoned && previousProcess
                    ? "STEAL_PREVIOUS_PROCESS" : abandoned ? "STEAL_ABANDONED" : "NORMAL");
            commitPatchLocked(p);
            return true;
        }
    }

    public void releaseRecoveryLease(String reason) {
        RuntimePatch p = new RuntimePatch();
        p.putLong("recovery_lease_until", 0L);
        p.putString("recovery_lease_release_reason", safe(reason));
        commitPatch(p);
    }

    public void markServiceReady(String reason) {
        long now = System.currentTimeMillis();
        RuntimePatch p = new RuntimePatch();
        p.putLong("service_ready_at", now);
        p.putString("service_ready_reason", safe(reason));
        p.putLong("recovery_lease_until", 0L);
        p.putString("recovery_lease_release_reason", "SERVICE_READY:" + safe(reason));
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        p.putInt("process_alive", 1);
        p.putString("init_status", "READY");
        p.putString("init_stage", "BUSINESS_READY");
        p.putLong("init_stage_at", now);
        p.putString("init_stage_detail", safe(reason));
        p.putString("init_last_error", "");
        p.putLong("last_recovery_result_at", now);
        p.putString("last_recovery_source", "service_ready");
        p.putString("last_recovery_result", "OK");
        p.putString("last_recovery_detail", "SERVICE_READY:" + safe(reason));
        commitPatch(p);
    }

    public void renewRuntimeLease(String reason) {
        long now = System.currentTimeMillis();
        RuntimePatch p = new RuntimePatch();
        p.putLong("runtime_lease_until", now + AppConfig.B07_RUNTIME_LEASE_MS);
        p.putLong("runtime_guard_at", now);
        p.putString("runtime_guard_reason", safe(reason));
        p.putInt("process_alive", 1);
        commitPatch(p);
    }

    public boolean isRuntimeLeaseFresh() {
        return getLong("runtime_lease_until", 0L) > System.currentTimeMillis();
    }

    public void markRuntimeGuardPulse(String reason) {
        renewRuntimeLease(reason);
    }

    private boolean isPreviousMainProcessR13(int recordedPid) {
        // Diagnostic exports use a secondary process and must observe the main
        // service's evidence rather than compare it with their own diagnostic PID.
        return recordedPid > 0 && recordedPid != safePid()
                && StarcoreApplication.isMainRuntimeProcess(app);
    }

    public boolean isServiceHealthyForRecovery() {
        if (isPreviousMainProcessR13(getInt("process_id", 0))) return false;
        if (isHeartbeatExpired()) return false;
        if (!isRuntimeLeaseFresh()) return false;
        return !isBusinessStale() || isAnalysisWorkFresh();
    }

    public long getHeartbeatAt() {
        return getLong("heartbeat_at", 0L);
    }

    public long getHeartbeatAgeMs() {
        long at = getHeartbeatAt();
        if (at <= 0L) return Long.MAX_VALUE;
        long age = System.currentTimeMillis() - at;
        return age < 0L ? 0L : age;
    }

    public boolean isHeartbeatExpired() {
        return getHeartbeatAgeMs() > AppConfig.SERVICE_RUNTIME_STALE_MS;
    }

    public int getRestartCount() {
        return getInt("restart_count", 0);
    }

    public void setAlarmStatus(String status) {
        putStringAtomic("alarm_status", safe(status));
    }

    public String getAlarmStatus() {
        return getString("alarm_status", "UNKNOWN");
    }

    public void setJobStatus(String status) {
        putStringAtomic("job_status", safe(status));
    }

    public String getJobStatus() {
        return getString("job_status", "UNKNOWN");
    }

    public void setLastReason(String reason) {
        putStringAtomic("last_reason", safe(reason));
    }

    public String getLastReason() {
        return getString("last_reason", "");
    }

    public String getLastReceiverAction() {
        return getString("last_receiver_action", "");
    }

    public long getLastStartAt() {
        return getLong("last_start_at", 0L);
    }

    public long getLastTaskRemovedAt() {
        return getLong("last_task_removed_at", 0L);
    }

    public long getLastDestroyAt() {
        return getLong("last_destroy_at", 0L);
    }

    public long getLastFetchSuccessAt() {
        return getLong("last_fetch_success_at", 0L);
    }

    public String getLastFetchIssue() {
        return getString("last_fetch_issue", "");
    }

    public long getLastAnalysisSuccessAt() {
        return getLong("last_analysis_success_at", 0L);
    }

    public String getLastAnalysisIssue() {
        return getString("last_analysis_issue", "");
    }

    public long getLastSettlementSuccessAt() {
        return getLong("last_settlement_success_at", 0L);
    }

    public String getLastSettlementIssue() {
        return getString("last_settlement_issue", "");
    }

    public String getBusinessHealth() {
        return derivedBusinessHealth();
    }

    public long getBusinessActivityAt() {
        long f = getLastFetchSuccessAt();
        long a = getLastAnalysisSuccessAt();
        return f > a ? f : a;
    }

    public boolean isBusinessStale() {
        return getBusinessStaleReason().length() > 0;
    }

    public String getBusinessStaleReason() {
        long now = System.currentTimeMillis();
        ServiceStartupStateR363.Evidence startup = startupEvidence(now);
        ServiceStartupStateR363.Phase phase =
                ServiceStartupStateR363.classify(
                        startup, AppConfig.SERVICE_BUSINESS_START_GRACE_MS);
        if (phase != ServiceStartupStateR363.Phase.READY)
            return ServiceStartupStateR363.preReadyStaleReason(
                    startup, AppConfig.SERVICE_BUSINESS_START_GRACE_MS);
        if (AppConfig.BUSINESS_ANALYSIS_FAILED.equals(
                getString("analysis_stage_state", "UNKNOWN"))) return "ANALYSIS_FAILED";
        long started = Math.max(getLastStartAt(), getServiceReadyAt());
        if (now - started < AppConfig.SERVICE_BUSINESS_START_GRACE_MS) return "";
        long fetch = getLastFetchSuccessAt(),
                analysis = getLastAnalysisSuccessAt(),
                taskRemoved = getLastTaskRemovedAt();
        if (taskRemoved > 0L
                && fetch < taskRemoved
                && now - taskRemoved > AppConfig.SERVICE_FAST_RECOVERY_ALARM_MS)
            return "TASK_REMOVED_NO_FETCH";
        if (fetch <= 0L || now - fetch > AppConfig.SERVICE_FETCH_STALE_MS) return "FETCH_STALE";
        if (getHeartbeatAgeMs() <= AppConfig.SERVICE_RUNTIME_STALE_MS
                && issueLag(getLastFetchIssue(), getLastAnalysisIssue()) > 2)
            return "BUSINESS_STALE_ISSUE_LAG_GT_2";
        if ((analysis <= 0L
                        || analysis + AppConfig.SERVICE_ANALYSIS_STALE_MS < fetch
                        || now - analysis > AppConfig.SERVICE_ANALYSIS_STALE_MS)
                && !isAnalysisWorkFresh()) return "ANALYSIS_STALE_NO_PROGRESS";
        int pending = getInt("pending_settlement_count", 0);
        long settlement = getLastSettlementSuccessAt(),
                settlementStage = getLong("settlement_stage_at", 0L);
        if (pending > 0
                && (settlement <= 0L
                        || now - Math.max(settlement, settlementStage)
                                > AppConfig.SERVICE_SETTLEMENT_STALE_MS)) return "SETTLEMENT_STALE";
        String researchStale = getResearchStaleReason();
        if (researchStale.length() > 0) return researchStale;
        return "";
    }

    private static int issueLag(String latestDrawIssue, String lastAnalyzedTarget) {
        int distance = TargetIssueResolver.distance(lastAnalyzedTarget, latestDrawIssue);
        return distance == Integer.MIN_VALUE || distance <= 0 ? 0 : distance;
    }

    private String derivedBusinessHealth() {
        if (getProcessAliveFlag() != 1) return "PROCESS_DOWN";
        if (isHeartbeatExpired()) return "SERVICE_STALE";
        String truth = runtimeTruthState();
        if ("STARTING".equals(truth)) return "STARTING";
        if ("INIT_FAILED".equals(truth)) return "INIT_FAILED";
        if ("INIT_STALLED".equals(truth)) return "INIT_STALLED";
        if ("NOT_REQUESTED".equals(truth)) return "NOT_REQUESTED";
        String stale = getBusinessStaleReason();
        if ("ANALYSIS_FAILED".equals(stale)) return "ANALYSIS_FAILED";
        if (stale.indexOf("SERVICE_") >= 0) return "SERVICE_STALE";
        if (stale.indexOf("FETCH") >= 0 || stale.indexOf("TASK_REMOVED") >= 0) return "FETCH_STALE";
        if (stale.indexOf("ANALYSIS") >= 0 || stale.indexOf("ISSUE_LAG") >= 0)
            return "ANALYSIS_STALE";
        if (stale.indexOf("SETTLEMENT") >= 0) return "SETTLEMENT_STALE";
        if (stale.indexOf("RESEARCH") >= 0) return "RESEARCH_STALE";
        String progress = getString("analysis_progress_status", "");
        if ("WAITING".equals(progress) || "REQUESTED".equals(progress)) return "WAITING_CANONICAL";
        return "HEALTHY";
    }

    private static String digits(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
        }
        return b.toString();
    }

    public long getLastAlarmTriggeredAt() {
        return getLong("alarm_last_triggered_at", 0L);
    }

    public long getLastJobTriggeredAt() {
        return getLong("job_last_triggered_at", 0L);
    }

    public long getLastRecoveryResultAt() {
        return getLong("last_recovery_result_at", 0L);
    }

    public String getLastRecoveryResult() {
        return getString("last_recovery_result", "");
    }

    public String getLastRecoveryDetail() {
        return getString("last_recovery_detail", "");
    }

    public long getLastTeacherSliceAt() {
        return getLong("last_teacher_slice_at", 0L);
    }

    public String getLastTeacherSliceStatus() {
        return getString("last_teacher_slice_status", "");
    }

    public int getLastTeacherSliceProcessed() {
        return getInt("last_teacher_slice_processed", 0);
    }

    public String getLastTeacherSliceCursor() {
        return getString("last_teacher_slice_cursor", "");
    }

    public long getLastTeacherSliceCostMs() {
        return getLong("last_teacher_slice_cost_ms", 0L);
    }

    public void markTeacherPump(String state, String detail) {
        markLoopPulse("teacher_pump", state, detail);
    }

    /** R955: completed useful autonomous-research progress, not mere dispatch/yield. */
    public void markLearningPump(String state, String detail) {
        markLoopPulse("learning_pump", state, detail);
    }

    /** R955: dispatcher attempt/yield/error heartbeat; never refreshes the progress clock. */
    public void markLearningAttempt(String state, String detail) {
        markLoopPulse("learning_attempt", state, detail);
    }

    public void markLoopRepair(String reason) {
        markLoopPulse("loop_repair", "REPAIRED", reason);
    }

    private void markLoopPulse(String prefix, String state, String detail) {
        RuntimePatch p = new RuntimePatch();
        long now = System.currentTimeMillis();
        p.putLong(prefix + "_at", now);
        p.putString(prefix + "_state", safe(state));
        p.putString(prefix + "_detail", safe(detail));
        applyTelemetryPatchR76(p);
    }

    public long getTeacherPumpAt() {
        return getLong("teacher_pump_at", 0L);
    }

    public long getLearningPumpAt() {
        return getLong("learning_pump_at", 0L);
    }

    public long getLearningAttemptAt() {
        return getLong("learning_attempt_at", 0L);
    }

    public String getLearningAttemptState() {
        return getString("learning_attempt_state", "UNKNOWN");
    }

    public String getLearningAttemptDetail() {
        return getString("learning_attempt_detail", "");
    }

    public BackgroundResearchProgressContractR955.Result researchProgressTruth() {
        return BackgroundResearchProgressContractR955.evaluate(
                System.currentTimeMillis(),
                getLastStartAt(),
                getServiceReadyAt(),
                getLearningPumpAt(),
                getLearningAttemptAt(),
                getLearningAttemptState(),
                AppConfig.BACKGROUND_LEARNING_PUMP_STALE_MS,
                AppConfig.SERVICE_BUSINESS_START_GRACE_MS);
    }

    public String getResearchStaleReason() {
        BackgroundResearchProgressContractR955.Result truth = researchProgressTruth();
        return truth.stale() ? truth.reason : "";
    }

    public long getLoopRepairAt() {
        return getLong("loop_repair_at", 0L);
    }

    public boolean isTeacherPumpStale() {
        long at = getTeacherPumpAt();
        return at <= 0L
                || System.currentTimeMillis() - at > AppConfig.BACKGROUND_TEACHER_PUMP_STALE_MS;
    }

    public boolean isLearningPumpStale() {
        return researchProgressTruth().stale();
    }

    public long getServiceStartingAt() {
        return getLong("service_starting_at", 0L);
    }

    public long getServiceReadyAt() {
        return getLong("service_ready_at", 0L);
    }

    public long getForegroundNotificationAt() {
        return getLong("foreground_notification_at", 0L);
    }

    public long getOnStartCommandAt() {
        return getLong("on_start_command_at", 0L);
    }

    public int getProcessAliveFlag() {
        return getInt("process_alive", 0);
    }

    public String getInitStatus() {
        return getString("init_status", "UNKNOWN");
    }

    public String getInitLastError() {
        return getString("init_last_error", "");
    }

    public int getInitRetryCount() {
        return getInt("init_retry_count", 0);
    }

    public String runtimeTruthState() {
        if (!isUserEnabled()) return "STOPPED_BY_USER";
        if (isPreviousMainProcessR13(getInt("process_id", 0))) return "STALE_WAIT_RECOVERY";
        long now = System.currentTimeMillis();
        long age = getHeartbeatAgeMs();
        long foreground = getForegroundNotificationAt();
        int alive = getProcessAliveFlag();
        String classified =
                ServiceStartupStateR363.truthState(
                        startupEvidence(now), AppConfig.SERVICE_BUSINESS_START_GRACE_MS);
        if (alive == 1 && age <= 5000L) return classified;
        if ("INIT_FAILED".equals(classified) || "INIT_STALLED".equals(classified))
            return classified;
        if (foreground > 0L && now - foreground <= 15000L) return "STARTING";
        return "STALE_WAIT_RECOVERY";
    }

    private ServiceStartupStateR363.Evidence startupEvidence(long now) {
        return new ServiceStartupStateR363.Evidence(
                isUserEnabled(),
                now,
                getServiceStartingAt(),
                getLastStartAt(),
                getForegroundNotificationAt(),
                getOnStartCommandAt(),
                getServiceReadyAt(),
                getInitStatus(),
                getString("init_stage", "UNKNOWN"),
                getInitLastError());
    }

    public String runtimeTruthSummary() {
        return "truth="
                + runtimeTruthState()
                + "|enabled="
                + (isUserEnabled() ? 1 : 0)
                + "|alive="
                + getProcessAliveFlag()
                + "|heartbeatAge="
                + ageText(getHeartbeatAgeMs())
                + "|startingAt="
                + getServiceStartingAt()
                + "|readyAt="
                + getServiceReadyAt()
                + "|foregroundAt="
                + getForegroundNotificationAt()
                + "|onStartAt="
                + getOnStartCommandAt()
                + "|init="
                + getInitStatus()
                + "|initStage="
                + getString("init_stage", "UNKNOWN")
                + "@"
                + getLong("init_stage_at", 0L)
                + "|retry="
                + getInitRetryCount()
                + "|initError="
                + getInitLastError()
                + "|business="
                + getBusinessHealth()
                + "|staleReason="
                + getBusinessStaleReason();
    }

    public Models.ServiceRuntimeState snapshot() {
        Models.ServiceRuntimeState x = new Models.ServiceRuntimeState();
        x.userEnabled = isUserEnabled() ? 1 : 0;
        x.heartbeatAt = getHeartbeatAt();
        x.heartbeatAgeMs = getHeartbeatAgeMs();
        x.startAt = getLastStartAt();
        x.taskRemovedAt = getLastTaskRemovedAt();
        x.destroyAt = getLastDestroyAt();
        x.restartCount = getRestartCount();
        x.alarmStatus = getAlarmStatus();
        x.jobStatus = getJobStatus();
        x.lastReason = getLastReason();
        x.lastReceiverAction = getLastReceiverAction();
        x.lastFetchSuccessAt = getLastFetchSuccessAt();
        x.lastAnalysisSuccessAt = getLastAnalysisSuccessAt();
        x.lastSettlementSuccessAt = getLastSettlementSuccessAt();
        x.lastAlarmTriggeredAt = getLastAlarmTriggeredAt();
        x.lastJobTriggeredAt = getLastJobTriggeredAt();
        x.lastTeacherPumpAt = getTeacherPumpAt();
        x.lastLearningPumpAt = getLearningPumpAt();
        x.lastLoopRepairAt = getLoopRepairAt();
        x.teacherPumpState = getString("teacher_pump_state", "UNKNOWN");
        x.learningPumpState = getString("learning_pump_state", "UNKNOWN");
        x.loopRepairReason = getString("loop_repair_detail", "");
        x.backgroundHealthState = getBusinessHealth();
        x.fetchStageState = getString("fetch_stage_state", "UNKNOWN");
        x.analysisStageState = getString("analysis_stage_state", "UNKNOWN");
        x.settlementStageState = getString("settlement_stage_state", "UNKNOWN");
        x.pendingSettlementCount = getInt("pending_settlement_count", 0);
        x.businessStaleReason = getBusinessStaleReason();
        x.serviceGeneration = getLong("service_generation", 0L);
        x.processStartId = getLong("process_start_id", 0L);
        x.analysisProgressAt = getLong("analysis_progress_at", 0L);
        x.analysisProgressPercent = getInt("analysis_progress_percent", 0);
        x.analysisProgressStage = getString("analysis_progress_stage", "");
        x.recoveryLeaseUntil = getLong("recovery_lease_until", 0L);
        x.restartRequestCount = getInt("restart_request_count", 0);
        x.actualProcessStartCount = getInt("actual_process_start_count", 0);
        x.updatedAt = System.currentTimeMillis();
        return x;
    }

    public String detailedSummary() {
        return "enabled="
                + (isUserEnabled() ? 1 : 0)
                + "|truth="
                + runtimeTruthState()
                + "|heartbeatAge="
                + ageText(getHeartbeatAgeMs())
                + "|pid="
                + getInt("process_id", 0)
                + "|alive="
                + getInt("process_alive", 0)
                + "|startAt="
                + getLastStartAt()
                + "|startingAt="
                + getServiceStartingAt()
                + "|readyAt="
                + getServiceReadyAt()
                + "|fg="
                + getLong("foreground_notification_at", 0L)
                + "|cmd="
                + getLong("on_start_command_at", 0L)
                + "|init="
                + getInitStatus()
                + "/stage="
                + getString("init_stage", "UNKNOWN")
                + "@"
                + getLong("init_stage_at", 0L)
                + "/detail="
                + getString("init_stage_detail", "")
                + "/retry="
                + getInitRetryCount()
                + "/error="
                + getInitLastError()
                + "|fetch="
                + getLastFetchIssue()
                + "@"
                + getLastFetchSuccessAt()
                + "|analysis="
                + getLastAnalysisIssue()
                + "@"
                + getLastAnalysisSuccessAt()
                + "|settlement="
                + getLastSettlementIssue()
                + "@"
                + getLastSettlementSuccessAt()
                + "|business="
                + getBusinessHealth()
                + "/stale="
                + (isBusinessStale() ? 1 : 0)
                + "/reason="
                + getBusinessStaleReason()
                + "/fetchStage="
                + getString("fetch_stage_state", "UNKNOWN")
                + "/fetchDetail="
                + getString("fetch_stage_detail", "")
                + "/analysisStage="
                + getString("analysis_stage_state", "UNKNOWN")
                + "/analysisDetail="
                + getString("analysis_stage_detail", "")
                + "/settleStage="
                + getString("settlement_stage_state", "UNKNOWN")
                + "/settleDetail="
                + getString("settlement_stage_detail", "")
                + "|apiCredential="
                + (KeyProvider.isKeyReady()?"READY":KeyProvider.unavailableReasonCP12())
                + "/pending="
                + getInt("pending_settlement_count", 0)
                + "|alarm="
                + getAlarmStatus()
                + "/trigger="
                + getLastAlarmTriggeredAt()
                + "|job="
                + getJobStatus()
                + "/trigger="
                + getLastJobTriggeredAt()
                + "|teacherPump="
                + getString("teacher_pump_state", "UNKNOWN")
                + "@"
                + getTeacherPumpAt()
                + "|learningProgress="
                + getString("learning_pump_state", "UNKNOWN")
                + "@"
                + getLearningPumpAt()
                + "|learningAttempt="
                + getLearningAttemptState()
                + "@"
                + getLearningAttemptAt()
                + "|researchTruth="
                + researchProgressTruth().canonical()
                + "|loopRepair="
                + getString("loop_repair_detail", "")
                + "@"
                + getLoopRepairAt()
                + "|receiver="
                + getLastReceiverAction()
                + "|recovery="
                + getLastRecoveryResult()
                + ":"
                + getLastRecoveryDetail()
                + "/evaluation="
                + getString("last_recovery_evaluation", "")
                + ":"
                + getString("last_recovery_evaluation_detail", "")
                + "|restartRequest="
                + getInt("restart_request_count", 0)
                + "/actualStart="
                + getInt("actual_process_start_count", 0)
                + "/generation="
                + getLong("service_generation", 0L)
                + "/progress="
                + getInt("analysis_progress_percent", 0)
                + "@"
                + getLong("analysis_progress_at", 0L)
                + "/leaseUntil="
                + getLong("recovery_lease_until", 0L)
                + "/runtimeLease="
                + getLong("runtime_lease_until", 0L)
                + "/guard@"
                + getLong("runtime_guard_at", 0L)
                + "|teacher="
                + getLastTeacherSliceStatus()
                + ":"
                + getLastTeacherSliceProcessed()
                + "/"
                + getLastTeacherSliceCursor()
                + "/"
                + getLastTeacherSliceCostMs()
                + "ms"
                + "|cohort="
                + getString("fix_cohort", AppConfig.ACTIVE_FIX_COHORT)
                + "|pipeline="
                + getString("probability_pipeline", AppConfig.ACTIVE_PROBABILITY_PIPELINE_VERSION);
    }

    public static Models.ServiceRuntimeState peekLastSnapshot() {
        Context c = LAST_APP_CONTEXT;
        return c == null ? emptySnapshot() : new ServiceHeartbeatStore(c).snapshot();
    }

    public static String peekLastDetailedSummary() {
        Context c = LAST_APP_CONTEXT;
        return c == null ? "NO_CONTEXT" : new ServiceHeartbeatStore(c).detailedSummary();
    }

    private static Models.ServiceRuntimeState emptySnapshot() {
        Models.ServiceRuntimeState x = new Models.ServiceRuntimeState();
        x.heartbeatAgeMs = Long.MAX_VALUE;
        x.alarmStatus = "NO_CONTEXT";
        x.jobStatus = "NO_CONTEXT";
        return x;
    }

    private SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private Context deviceContext() {
        if (app == null) return null;
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                return app.createDeviceProtectedStorageContext();
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    private void putStringAtomic(String key, String value) {
        RuntimePatch p = new RuntimePatch();
        p.putString(key, value);
        commitPatch(p);
    }

    /** Observational progress is immediately visible in-process and flushed by Android's
     * ordered preference writer. Never synchronously fsync both preference stores on the
     * fetch/analysis/settlement/research owner. Permission, stop/start and recovery leases
     * continue through commitPatch/commitPatchLocked below. This is not a durable business
     * receipt: canonical/settlement/learning truth remains in its original SQLite Owner. */
    private void applyTelemetryPatchR76(RuntimePatch patch) {
        if (app == null || patch == null) return;
        synchronized (LOCK) {
            try { applyPatch(prefs(app).edit(), patch).apply(); }
            catch (Throwable ignored) {}
            Context dc = deviceContext();
            if (dc != null) {
                try { applyPatch(prefs(dc).edit(), patch).apply(); }
                catch (Throwable ignored) {}
            }
        }
    }

    private void commitPatch(RuntimePatch patch) {
        synchronized (LOCK) {
            commitPatchLocked(patch);
        }
    }

    private void commitPatchLocked(RuntimePatch patch) {
        if (app == null || patch == null) return;
        try {
            applyPatch(prefs(app).edit(), patch).commit();
        } catch (Throwable ignore) {
        }
        Context dc = deviceContext();
        if (dc != null) {
            try {
                applyPatch(prefs(dc).edit(), patch).commit();
            } catch (Throwable ignore) {
            }
        }
    }

    private SharedPreferences.Editor applyPatch(SharedPreferences.Editor e, RuntimePatch p) {
        for (int i = 0; i < p.size; i++) {
            String k = p.keys[i];
            Object v = p.values[i];
            if (v instanceof String) e.putString(k, (String) v);
            else if (v instanceof Long) e.putLong(k, ((Long) v).longValue());
            else if (v instanceof Integer) e.putInt(k, ((Integer) v).intValue());
            else if (v instanceof Boolean) e.putBoolean(k, ((Boolean) v).booleanValue());
        }
        return e;
    }

    private String getString(String key, String fallback) {
        if (app == null) return fallback;
        try {
            SharedPreferences p = prefs(app);
            if (p.contains(key)) return p.getString(key, fallback);
        } catch (Throwable ignore) {
        }
        Context dc = deviceContext();
        if (dc != null)
            try {
                SharedPreferences p = prefs(dc);
                if (p.contains(key)) return p.getString(key, fallback);
            } catch (Throwable ignore) {
            }
        return fallback;
    }

    private long getLong(String key, long fallback) {
        if (app == null) return fallback;
        try {
            SharedPreferences p = prefs(app);
            if (p.contains(key)) return p.getLong(key, fallback);
        } catch (Throwable ignore) {
        }
        Context dc = deviceContext();
        if (dc != null)
            try {
                SharedPreferences p = prefs(dc);
                if (p.contains(key)) return p.getLong(key, fallback);
            } catch (Throwable ignore) {
            }
        return fallback;
    }

    private int getInt(String key, int fallback) {
        if (app == null) return fallback;
        try {
            SharedPreferences p = prefs(app);
            if (p.contains(key)) return p.getInt(key, fallback);
        } catch (Throwable ignore) {
        }
        Context dc = deviceContext();
        if (dc != null)
            try {
                SharedPreferences p = prefs(dc);
                if (p.contains(key)) return p.getInt(key, fallback);
            } catch (Throwable ignore) {
            }
        return fallback;
    }

    private boolean getBoolean(String key, boolean fallback) {
        if (app == null) return fallback;
        try {
            SharedPreferences p = prefs(app);
            if (p.contains(key)) return p.getBoolean(key, fallback);
        } catch (Throwable ignore) {
        }
        Context dc = deviceContext();
        if (dc != null)
            try {
                SharedPreferences p = prefs(dc);
                if (p.contains(key)) return p.getBoolean(key, fallback);
            } catch (Throwable ignore) {
            }
        return fallback;
    }

    private static int safePid() {
        try {
            return Process.myPid();
        } catch (Throwable e) {
            return 0;
        }
    }

    private static String ageText(long age) {
        return age == Long.MAX_VALUE ? "NO_HEARTBEAT" : String.valueOf(age) + "ms";
    }

    private static String safe(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    private static final class RuntimePatch {
        final String[] keys = new String[32];
        final Object[] values = new Object[32];
        int size;

        void putString(String k, String v) {
            put(k, v == null ? "" : v);
        }

        void putLong(String k, long v) {
            put(k, Long.valueOf(v));
        }

        void putInt(String k, int v) {
            put(k, Integer.valueOf(v));
        }

        void putBoolean(String k, boolean v) {
            put(k, Boolean.valueOf(v));
        }

        private void put(String k, Object v) {
            if (k == null || size >= keys.length) return;
            keys[size] = k;
            values[size] = v;
            size++;
        }
    }

    // V57_B13_RUNTIME_TRUTH
    public static final class V57RuntimeTruth {
        public final String process,
                service,
                foreground,
                business,
                fetch,
                analysis,
                settlement,
                research,
                overall,
                reason;
        public final long heartbeatAge, fetchAge, analysisAge, settlementAge, researchProgressAge, researchAttemptAge;

        V57RuntimeTruth(
                String process,
                String service,
                String foreground,
                String business,
                String fetch,
                String analysis,
                String settlement,
                String research,
                String overall,
                String reason,
                long heartbeatAge,
                long fetchAge,
                long analysisAge,
                long settlementAge,
                long researchProgressAge,
                long researchAttemptAge) {
            this.process = process;
            this.service = service;
            this.foreground = foreground;
            this.business = business;
            this.fetch = fetch;
            this.analysis = analysis;
            this.settlement = settlement;
            this.research = research;
            this.overall = overall;
            this.reason = reason;
            this.heartbeatAge = heartbeatAge;
            this.fetchAge = fetchAge;
            this.analysisAge = analysisAge;
            this.settlementAge = settlementAge;
            this.researchProgressAge = researchProgressAge;
            this.researchAttemptAge = researchAttemptAge;
        }

        public boolean healthy() {
            return "HEALTHY".equals(overall);
        }

        public String summary() {
            return "overall="
                    + overall
                    + "|process="
                    + process
                    + "|service="
                    + service
                    + "|foreground="
                    + foreground
                    + "|business="
                    + business
                    + "|fetch="
                    + fetch
                    + "|analysis="
                    + analysis
                    + "|settlement="
                    + settlement
                    + "|research="
                    + research
                    + "|hbAge="
                    + heartbeatAge
                    + "|fetchAge="
                    + fetchAge
                    + "|analysisAge="
                    + analysisAge
                    + "|settlementAge="
                    + settlementAge
                    + "|researchProgressAge="
                    + researchProgressAge
                    + "|researchAttemptAge="
                    + researchAttemptAge
                    + "|reason="
                    + reason;
        }
    }

    public V57RuntimeTruth v57RuntimeTruth() {
        long now = System.currentTimeMillis();
        long hb = getHeartbeatAgeMs();
        long fa = v57Age(now, getLastFetchSuccessAt());
        long aa = v57Age(now, getLastAnalysisSuccessAt());
        long sa = v57Age(now, getLastSettlementSuccessAt());
        BackgroundResearchProgressContractR955.Result researchTruth = researchProgressTruth();
        long rpa = researchTruth.progressAgeMs;
        long raa = researchTruth.attemptAgeMs;
        String process = getProcessAliveFlag() == 1 ? "ALIVE" : "DOWN";
        String service = runtimeTruthState();
        String foreground = getForegroundNotificationAt() > 0L ? "SEEN" : "MISSING";
        String stale = getBusinessStaleReason();
        boolean starting = "STARTING".equals(service);
        boolean initFailed = "INIT_FAILED".equals(service) || "INIT_STALLED".equals(service);
        String business =
                starting
                        ? "STARTING"
                        : (initFailed ? "BLOCKED" : (stale.length() == 0 ? "HEALTHY" : "STALE"));
        String fetch =
                starting
                        ? "WAITING"
                        : initFailed
                                ? "BLOCKED"
                        : stale.indexOf("FETCH") >= 0
                                        || stale.indexOf("TASK_REMOVED_NO_FETCH") >= 0
                        ? "STALE"
                        : "OK";
        String analysis =
                starting
                        ? "WAITING"
                        : (initFailed
                                ? "BLOCKED"
                                : (stale.indexOf("ANALYSIS") >= 0 ? "STALE" : "OK"));
        String settlement =
                starting
                        ? "WAITING"
                        : (initFailed
                                ? "BLOCKED"
                                : (stale.indexOf("SETTLEMENT") >= 0 ? "STALE" : "OK"));
        String research =
                starting
                        ? "WAITING"
                        : (initFailed ? "BLOCKED" : researchTruth.state);
        String overall = "HEALTHY", reason = "";
        if (!isUserEnabled()) {
            overall = "USER_STOPPED";
            reason = "EXPLICIT_USER_STOP";
        } else if ("DOWN".equals(process)) {
            overall = "PROCESS_DOWN";
            reason = "PROCESS_FLAG";
        } else if (isHeartbeatExpired()) {
            overall = "SERVICE_STALE";
            reason = "HEARTBEAT_EXPIRED";
        } else if (getForegroundNotificationAt() <= 0L) {
            overall = "FOREGROUND_MISSING";
            reason = "NO_FOREGROUND_MARK";
        } else if (starting) {
            overall = "SERVICE_STARTING";
            reason = getString("init_stage", "UNKNOWN");
        } else if (initFailed) {
            overall = service;
            reason = stale.length() == 0 ? getInitLastError() : stale;
        } else if (stale.length() > 0) {
            overall = "BUSINESS_LOOP_STALE";
            reason = stale;
        }
        return new V57RuntimeTruth(
                process,
                service,
                foreground,
                business,
                fetch,
                analysis,
                settlement,
                research,
                overall,
                reason,
                hb,
                fa,
                aa,
                sa,
                rpa,
                raa);
    }

    public String v57RuntimeTruthSummary() {
        return v57RuntimeTruth().summary();
    }

    private static long v57Age(long now, long at) {
        return at <= 0L ? Long.MAX_VALUE : Math.max(0L, now - at);
    }
}
