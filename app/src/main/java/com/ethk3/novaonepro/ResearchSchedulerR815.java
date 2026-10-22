package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** Pressure-aware bounded research queue that remains strictly below settlement and prediction. */
public final class ResearchSchedulerR815 {
    public static final class SettledOosEvent {
        public final String targetIssue;
        public final String settlementHash;
        public final String canonicalHash;
        public final int actualNumber;
        public final int hitLabel;

        public SettledOosEvent(
                String targetIssue,
                String settlementHash,
                String canonicalHash,
                int actualNumber,
                int hitLabel) {
            this.targetIssue = safe(targetIssue);
            this.settlementHash = safe(settlementHash);
            this.canonicalHash = safe(canonicalHash);
            this.actualNumber = actualNumber;
            this.hitLabel = hitLabel;
            if (!TargetIssueResolver.isValid(this.targetIssue)
                    || !hash(this.settlementHash)
                    || !hash(this.canonicalHash)
                    || !AppConfig.isValidNumber(actualNumber)
                    || (hitLabel != 0 && hitLabel != 1))
                throw new IllegalArgumentException("R815_RESEARCH_EVENT_INVALID");
        }
    }

    public static final class Status {
        public final boolean paused;
        public final String reason;
        public final int queueDepth;
        public final long lastProgressAt;

        Status(boolean paused, String reason, int queueDepth, long lastProgressAt) {
            this.paused = paused;
            this.reason = safe(reason);
            this.queueDepth = queueDepth;
            this.lastProgressAt = lastProgressAt;
        }
    }

    private final DbStore store;
    private final ResearchRepositoryR815 repository;
    private volatile String pauseReason = "";
    private volatile long lastProgressAt;

    public ResearchSchedulerR815(DbStore store) {
        if (store == null) throw new IllegalArgumentException("R815_RESEARCH_SCHEDULER_STORE_NULL");
        this.store = store;
        repository = new ResearchRepositoryR815(store);
    }

    public boolean enqueue(final SettledOosEvent event) {
        if (event == null) return false;
        Boolean result =
                store.coordinatorR36()
                        .runWrite(
                                "R815_RESEARCH_ENQUEUE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues values = new ContentValues();
                                        values.put("target_issue", event.targetIssue);
                                        values.put("settlement_hash", event.settlementHash);
                                        values.put("state", "PENDING");
                                        values.put("attempts", 0);
                                        values.put("next_attempt_at", 0L);
                                        values.put("created_at", System.currentTimeMillis());
                                        values.put("updated_at", System.currentTimeMillis());
                                        values.put("last_error", "");
                                        long inserted =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R815_RESEARCH_QUEUE,
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        if (inserted >= 0L) return Boolean.TRUE;
                                        Cursor cursor = null;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT settlement_hash FROM "
                                                                    + DbHelper.TABLE_R815_RESEARCH_QUEUE
                                                                    + " WHERE target_issue=? LIMIT 1",
                                                            new String[] {event.targetIssue});
                                            return Boolean.valueOf(
                                                    cursor.moveToFirst()
                                                            && event.settlementHash.equals(safe(cursor.getString(0))));
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    public int drain(int budget) {
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return 0;
        if (pauseReason.length() > 0) return 0;
        int bound = Math.max(1, Math.min(4, budget));
        int completed = 0;
        for (int i = 0; i < bound; i++) {
            QueueRow row = claim();
            if (row == null) break;
            if (process(row)) completed++;
        }
        return completed;
    }

    public void pause(String reason) {
        pauseReason = safe(reason);
        store.setState("r815_research_pause_reason", pauseReason);
    }

    public Status status() {
        return new Status(pauseReason.length() > 0, pauseReason, queueDepth(), lastProgressAt);
    }

    public static boolean enqueueFromProduction(DbStore store, ShadowObservationSidecarR815.SettledEvent event) {
        if (store == null || event == null) return false;
        return new ResearchSchedulerR815(store)
                .enqueue(
                        new SettledOosEvent(
                                event.targetIssue,
                                event.settlementHash,
                                event.canonicalHash,
                                event.actualNumber,
                                event.hitLabel));
    }

    private boolean process(QueueRow row) {
        try {
            ResearchRepositoryR815.Candidate candidate = repository.firstRunnable(row.targetIssue);
            if (candidate == null) {
                retry(row, "NO_EXECUTABLE_CANDIDATE", 60000L);
                return false;
            }
            if (candidate.state == ResearchRepositoryR815.State.NEW
                    && !repository.transition(candidate.candidateId, ResearchRepositoryR815.State.QUEUED, "QUEUE_CLAIM")) {
                retry(row, "CANDIDATE_QUEUE_TRANSITION_FAILED", 30000L);
                return false;
            }
            if (!repository.transition(candidate.candidateId, ResearchRepositoryR815.State.RUNNING, "EXECUTE")) {
                retry(row, "CANDIDATE_RUNNING_TRANSITION_FAILED", 30000L);
                return false;
            }
            List<Models.Draw> history = store.getDrawsBeforeIssue(row.targetIssue, 720);
            HistoryIntegrityVerifierR815.Result integrity = HistoryIntegrityVerifierR815.verifyAscending(history);
            if (!integrity.minimalReady()) {
                repository.transition(candidate.candidateId, ResearchRepositoryR815.State.REJECTED, integrity.summary());
                finish(row, "REJECT_HISTORY_NOT_READY");
                return true;
            }
            double[][] q = frequencyQ(integrity.cleanAscending());
            CrossPositionPrimitiveCacheR815.Snapshot cache =
                    CrossPositionPrimitiveCacheR815.build(
                            new CrossPositionPrimitiveCacheR815.Binding(
                                    row.targetIssue,
                                    integrity.historyHash,
                                    ResearchFeatureSchemaR815.schemaSha256(),
                                    integrity.contiguousSuffix),
                            q);
            int[] top = top3(cache, 1);
            ResearchFeatureVectorR815.Binding binding =
                    new ResearchFeatureVectorR815.Binding(
                            ReleaseIdentityR815.GAME_CODE,
                            DatasetIdentityGateR815.Scope.SEALED_OOS,
                            row.targetIssue,
                            integrity.historyHash,
                            candidate.datasetSha256,
                            ResearchFeatureSchemaR815.schemaSha256());
            ResearchFeatureVectorR815 vector =
                    ResearchFeatureMaterializerR815.materialize(
                            binding,
                            cache,
                            new ResearchFeatureMaterializerR815.ActionIdentity(
                                    row.targetIssue, 1, top[0], top[1], top[2], 1));
            ResearchCandidateExecutorRegistryR815.Execution execution =
                    ResearchCandidateExecutorRegistryR815.execute(candidate, vector);
            Models.Draw actualDraw = store.getDrawByIssue(row.targetIssue);
            if (actualDraw == null || !actualDraw.isValid()) throw new IllegalStateException("R815_RESEARCH_ACTUAL_DRAW_MISSING");
            int actual = actualDraw.getNumberAtPosition(execution.position);
            boolean hit = execution.selected && (actual == execution.n1 || actual == execution.n2 || actual == execution.n3);
            double brier = (execution.probability - (hit ? 1.0d : 0.0d));
            brier *= brier;
            double logLoss = -Math.log(Math.max(1.0e-9d, hit ? execution.probability : 1.0d - execution.probability));
            if (!repository.saveEval(
                    new ResearchRepositoryR815.Evaluation(
                            candidate.candidateId,
                            row.targetIssue,
                            execution.artifactHash,
                            execution.selected,
                            hit,
                            execution.probability,
                            brier,
                            logLoss,
                            "R815",
                            "UNKNOWN",
                            System.currentTimeMillis())))
                throw new IllegalStateException("R815_RESEARCH_EVAL_WRITE_FAILED");
            repository.transition(candidate.candidateId, ResearchRepositoryR815.State.EVALUATED, execution.detail);
            new ResearchPromotionVerifierR815(store, repository).transitionIfEligible(
                    new ResearchPromotionVerifierR815(store, repository).evaluate(candidate.candidateId));
            finish(row, "COMPLETED");
            lastProgressAt = System.currentTimeMillis();
            return true;
        } catch (RuntimeException failure) {
            CriticalPathErrorLedgerR815.record(
                    store,
                    new CriticalPathErrorLedgerR815.Event(
                            "RESEARCH", "PROCESS_FAILED", row.targetIssue, 0L, true, failure, failure.getMessage()));
            retry(row, failure.getClass().getSimpleName(), 30000L);
            return false;
        }
    }

    private QueueRow claim() {
        long now = System.currentTimeMillis();
        return store.coordinatorR36()
                .runWrite(
                        "R815_RESEARCH_CLAIM",
                        new DatabaseCoordinator.WriteCallable<QueueRow>() {
                            @Override
                            public QueueRow call(SQLiteDatabase db) {
                                Cursor cursor = null;
                                try {
                                    cursor =
                                            db.rawQuery(
                                                    "SELECT target_issue,settlement_hash,attempts FROM "
                                                            + DbHelper.TABLE_R815_RESEARCH_QUEUE
                                                            + " WHERE state IN ('PENDING','RETRY_WAIT') AND next_attempt_at<=?"
                                                            + " ORDER BY created_at ASC LIMIT 1",
                                                    new String[] {String.valueOf(now)});
                                    if (!cursor.moveToFirst()) return null;
                                    QueueRow row = new QueueRow(cursor.getString(0), cursor.getString(1), cursor.getInt(2) + 1);
                                    ContentValues values = new ContentValues();
                                    values.put("state", "RUNNING");
                                    values.put("attempts", row.attempts);
                                    values.put("updated_at", now);
                                    if (db.update(
                                                    DbHelper.TABLE_R815_RESEARCH_QUEUE,
                                                    values,
                                                    "target_issue=? AND state IN ('PENDING','RETRY_WAIT')",
                                                    new String[] {row.targetIssue})
                                            != 1) return null;
                                    return row;
                                } finally {
                                    if (cursor != null) cursor.close();
                                }
                            }
                        },
                        null);
    }

    private void finish(final QueueRow row, final String state) {
        store.coordinatorR36()
                .runWrite(
                        "R815_RESEARCH_FINISH",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                ContentValues values = new ContentValues();
                                values.put("state", state);
                                values.put("updated_at", System.currentTimeMillis());
                                values.put("next_attempt_at", 0L);
                                values.put("last_error", "");
                                db.update(
                                        DbHelper.TABLE_R815_RESEARCH_QUEUE,
                                        values,
                                        "target_issue=? AND state='RUNNING'",
                                        new String[] {row.targetIssue});
                            }
                        });
    }

    private void retry(final QueueRow row, final String error, long delay) {
        store.coordinatorR36()
                .runWrite(
                        "R815_RESEARCH_RETRY",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                ContentValues values = new ContentValues();
                                boolean terminal = row.attempts >= 6;
                                values.put("state", terminal ? "FAILED" : "RETRY_WAIT");
                                values.put("updated_at", System.currentTimeMillis());
                                values.put("next_attempt_at", terminal ? 0L : System.currentTimeMillis() + delay);
                                values.put("last_error", bounded(error, 256));
                                db.update(
                                        DbHelper.TABLE_R815_RESEARCH_QUEUE,
                                        values,
                                        "target_issue=? AND state='RUNNING'",
                                        new String[] {row.targetIssue});
                            }
                        });
    }

    private int queueDepth() {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + DbHelper.TABLE_R815_RESEARCH_QUEUE
                                            + " WHERE state IN ('PENDING','RETRY_WAIT','RUNNING')",
                                    null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static double[][] frequencyQ(List<Models.Draw> history) {
        double[][] q = new double[10][10];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) q[p][n] = 1.0d;
        for (int i = 0; i < history.size(); i++) {
            Models.Draw draw = history.get(i);
            for (int p = 0; p < 10; p++) q[p][draw.numbers[p] - 1] += 1.0d;
        }
        return q;
    }

    private static int[] top3(CrossPositionPrimitiveCacheR815.Snapshot cache, int position) {
        int[] best = new int[] {1, 2, 3};
        for (int number = 1; number <= 10; number++) {
            for (int k = 0; k < 3; k++) {
                if (cache.atom(position, number).q > cache.atom(position, best[k]).q) {
                    for (int move = 2; move > k; move--) best[move] = best[move - 1];
                    best[k] = number;
                    break;
                }
            }
        }
        return best;
    }

    private static final class QueueRow {
        final String targetIssue;
        final String settlementHash;
        final int attempts;

        QueueRow(String targetIssue, String settlementHash, int attempts) {
            this.targetIssue = safe(targetIssue);
            this.settlementHash = safe(settlementHash);
            this.attempts = attempts;
        }
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String bounded(String value, int max) {
        String clean = safe(value);
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
