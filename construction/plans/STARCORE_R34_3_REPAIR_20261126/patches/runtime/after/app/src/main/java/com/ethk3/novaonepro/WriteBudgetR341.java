package com.ethk3.novaonepro;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admission and measurement for the existing single writer. The owner alone ends its transaction. */
public final class WriteBudgetR341 {
    public static final int MAX_ROWS = 64;
    public static final long MAX_BIND_BYTES = 64 * 1024L;
    static final long TARGET_HOLD_MS = 100L;
    static final long HARD_HOLD_MS = 200L;
    private static final Map<DatabaseCoordinator.WriteClass, Map<String, Object>> LAST =
            new EnumMap<>(DatabaseCoordinator.WriteClass.class);

    private final DatabaseCoordinator.WriteContext context;
    private final long queuedAt;
    private final long deadline;
    private long acquiredAt = -1L;
    private long releasedAt = -1L;
    private long maxHoldMs;
    private long beginNs = -1L;
    private long sqlNs = -1L;
    private long commitNs = -1L;
    private long diagnosticNs;
    private String admission = "WAITING";

    private WriteBudgetR341(DatabaseCoordinator.WriteContext context, long deadline, long now) {
        this.context = context;
        this.deadline = deadline;
        this.queuedAt = now;
    }

    static WriteBudgetR341 forLane(DatabaseCoordinator.WriteContext context, long deadline, long now) {
        return new WriteBudgetR341(context, deadline, now);
    }

    boolean canAdmit(long now) {
        if (now >= deadline) admission = "DEADLINE";
        else if (context.boundedSlice && overSize()) admission = "SLICE_TOO_LARGE";
        else admission = "ADMITTED";
        return "ADMITTED".equals(admission);
    }

    private boolean overSize() {
        return context.rows > MAX_ROWS || context.bindBytes > MAX_BIND_BYTES || context.ddlCount > 1;
    }

    void beforeStatement(String phase, long now) {
        if (now >= deadline) {
            admission = "DEADLINE";
            throw new DatabaseCoordinator.WriteRejectedR31("TEMP_TIMEOUT", context.operation, phase,
                    DatabaseCoordinator.CommitOutcome.ROLLED_BACK,
                    new IllegalStateException("CP3_DB_BUSY_DEADLINE"));
        }
    }

    DatabaseCoordinator.WriteRejectedR31 rejected() {
        String category = "DEADLINE".equals(admission) ? "TEMP_TIMEOUT" : "RESOURCE_QUEUE";
        return new DatabaseCoordinator.WriteRejectedR31(category, context.operation, "ADMISSION",
                DatabaseCoordinator.CommitOutcome.ROLLED_BACK, new IllegalStateException(admission));
    }

    void acquired(long now) { acquiredAt = now; }

    void releasing(long now) {
        releasedAt = now;
        maxHoldMs = Math.max(maxHoldMs, Math.max(0L, now - acquiredAt));
    }

    void transaction(long begin, long sql, long commit) {
        beginNs = begin;
        sqlNs = sql;
        commitNs = commit;
    }

    void diagnostic(long elapsedNs) { diagnosticNs += elapsedNs; }

    void finish(long now, String phase, DatabaseCoordinator.CommitOutcome outcome, String failure) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("operation", context.operation);
        receipt.put("lane", context.writeClass.name());
        receipt.put("deadlineElapsedMs", deadline);
        receipt.put("remainingAtAcquireMs", acquiredAt < 0 ? null : deadline - acquiredAt);
        receipt.put("remainingAtReturnMs", deadline - now);
        receipt.put("queuedElapsedMs", queuedAt);
        receipt.put("waitMs", (acquiredAt < 0 ? now : acquiredAt) - queuedAt);
        receipt.put("maxHoldMs", maxHoldMs);
        receipt.put("beginNs", beginNs);
        receipt.put("sqlNs", sqlNs);
        receipt.put("commitNs", commitNs);
        receipt.put("diagnosticNs", diagnosticNs);
        receipt.put("afterReleaseMs", releasedAt < 0 ? null : now - releasedAt);
        receipt.put("preparedRows", context.rows);
        receipt.put("preparedBindBytes", context.bindBytes);
        receipt.put("preparedStatements", context.statements);
        receipt.put("preparedDdlCount", context.ddlCount);
        receipt.put("boundedSlice", context.boundedSlice);
        receipt.put("admission", admission);
        receipt.put("overSize", overSize());
        receipt.put("overTargetHold", maxHoldMs > TARGET_HOLD_MS);
        receipt.put("overHardHold", maxHoldMs > HARD_HOLD_MS);
        receipt.put("phase", phase);
        receipt.put("outcome", outcome.name());
        receipt.put("failure", failure);
        if (!failure.isEmpty() || !"ADMITTED".equals(admission)) {
            receipt.put("nextAttemptElapsedMs", now + ("SLICE_TOO_LARGE".equals(admission) ? 1000L : 250L));
        }
        synchronized (LAST) { LAST.put(context.writeClass, java.util.Collections.unmodifiableMap(receipt)); }
    }

    static Map<String, Map<String, Object>> snapshot() {
        Map<String, Map<String, Object>> byLane = new LinkedHashMap<>();
        synchronized (LAST) { LAST.forEach((lane, receipt) -> byLane.put(lane.name(), receipt)); }
        return java.util.Collections.unmodifiableMap(byLane);
    }

    static final class DatasetService {
        private String visibleHead = "";
        private String preparedInput = "";
        private String lastCommit = "";
        private long readySince = -1L;
        private long preparedUntil;
        private long lastAttempt = -1L;
        private long lastCommitted = -1L;
        private long lastFreezeObserved = -1L;

        synchronized void ready(String head, long now) {
            if (head == null || head.isEmpty()) return;
            visibleHead = head;
            if (readySince < 0L) readySince = now;
        }

        synchronized boolean due(long now, int waitingFreeze) {
            if (waitingFreeze > 0 && readySince >= 0) lastFreezeObserved = now;
            return waitingFreeze == 0 && readySince >= 0L && now - readySince >= 5000L;
        }

        synchronized void prepared(String input, long now) {
            if (input == null || input.isEmpty()) throw new IllegalArgumentException("DATASET_INPUT_REQUIRED");
            if (readySince < 0L) readySince = now;
            preparedInput = input;
            lastAttempt = now;
            preparedUntil = now + 200L;
        }

        synchronized boolean hasPreparedTurn(long now) {
            return !preparedInput.isEmpty() && now < preparedUntil && due(now, 0);
        }

        synchronized boolean isTurn(String input, long now) {
            return !input.isEmpty() && input.equals(preparedInput) && hasPreparedTurn(now);
        }

        synchronized void release(String input) {
            if (input.equals(preparedInput)) {
                preparedInput = "";
                preparedUntil = 0L;
            }
        }

        synchronized void unavailable(String head) {
            if (head != null && head.equals(visibleHead) && preparedInput.isEmpty()) readySince = -1L;
        }

        synchronized void committed(String input, String receipt, long now) {
            if (!input.equals(preparedInput)) return;
            lastCommit = receipt;
            lastCommitted = now;
            readySince = -1L;
            release(input);
        }

        synchronized Map<String, Object> snapshot(long now, int waitingFreeze) {
            long age = readySince < 0 ? 0L : now - readySince;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("visibleHead", visibleHead);
            result.put("oldestReadyElapsedMs", readySince);
            result.put("oldestReadyAgeMs", age);
            result.put("preparedInput", preparedInput);
            result.put("lastAttemptElapsedMs", lastAttempt);
            result.put("lastCommittedElapsedMs", lastCommitted);
            result.put("lastCommit", lastCommit);
            result.put("lastFreezeObservedElapsedMs", lastFreezeObserved);
            result.put("freezeWaiters", waitingFreeze);
            result.put("nextEligibleElapsedMs", readySince < 0 ? -1L : readySince + 5000L);
            result.put("status", readySince < 0 ? "NO_READY_INPUT" : waitingFreeze > 0 ? "SATURATED_CURRENT_FREEZE"
                    : age >= 30000 ? "STARVATION_OVER_30S" : age >= 10000 ? "WAIT_OVER_10S" : "READY_WAITING");
            return java.util.Collections.unmodifiableMap(result);
        }
    }
}
