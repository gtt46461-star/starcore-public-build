package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * V40 strict action-level Top-K micro walk-forward publisher.
 *
 * <p>Invariants: 1. One historical origin contributes up to a diversified Top-K qualified action
 * surface. 2. Settled pending rows are published before any expensive historical scan. 3. A newly
 * generated origin is published immediately before the next yield check. 4. Yield never rolls back
 * published sample count or READY state. 5. Target draw is used only after the immutable pre-target
 * action row is inserted.
 */
public final class MicroWalkForwardEngine {
    public static final String V46_ROLE = "STRICT_TIME_ORDERED_OOS_PUBLISHER";

    public static final String MODULE_VERSION = AppConfig.MICRO_WALK_FORWARD_VERSION;
    public static final String STATE_STATUS = "v28_micro_walkforward_status";
    public static final String STATE_VERSION = "v28_micro_walkforward_version";
    public static final String STATE_SAMPLE = "v28_micro_walkforward_sample";
    public static final String STATE_READY = "v28_micro_walkforward_ready";
    public static final String STATE_LAST_TARGET = "v28_micro_walkforward_last_target";
    public static final String STATE_LAST_COST = "v28_micro_walkforward_last_cost_ms";
    public static final String STATE_LAST_SUMMARY = "v28_micro_walkforward_summary";
    public static final String STATE_HISTORY_CURSOR = "v28_micro_walkforward_history_cursor";
    public static final String STATE_HISTORY_GENERATED = "v28_micro_walkforward_history_generated";
    public static final String STATE_LAST_ERROR = "v28_micro_walkforward_error";
    public static final String STATE_PENDING = "v28_micro_walkforward_pending";
    public static final String STATE_PROGRESS_AT = "v28_micro_walkforward_progress_at";

    public static final String STATUS_READY = "MICRO_WALK_FORWARD_READY";
    public static final String STATUS_WATCH = "MICRO_WALK_FORWARD_WATCH";
    public static final String STATUS_PROBE = "MICRO_WALK_FORWARD_PROBE";
    public static final String STATUS_WARM = "MICRO_WALK_FORWARD_WARM";
    public static final String STATUS_IDLE = "MICRO_WALK_FORWARD_IDLE";
    public static final String STATUS_YIELD = "MICRO_WALK_FORWARD_YIELD_REALTIME";
    public static final String STATUS_ERROR = "MICRO_WALK_FORWARD_ERROR";

    public interface YieldSignal {
        boolean shouldYield();
    }

    public static final class Report {
        public String status = STATUS_IDLE;
        public String trainedUntilIssue = "";
        public String testTargetIssue = "";
        public String historyCursor = "";
        public int generatedHistoricalOrigins;
        public int generatedHistoricalPredictions;
        public int processedOrigins;
        public int processedPredictions;
        public int historicalPredictions;
        public int livePredictions;
        public int oosPredictions;
        public int hits;
        public int familiesUpdated;
        public int bucketsUpdated;
        public int totalPublishedSample;
        public int sampleBefore;
        public int progressSample;
        public int pendingBefore;
        public int pendingAfter;
        public long costMs;
        public String publishedVersion = MODULE_VERSION;
        public int nextReadSuccess;
        public int yieldedToRealtime;
        public String error = "";

        public String summary() {
            return "status="
                    + status
                    + "｜trainedUntil="
                    + trainedUntilIssue
                    + "｜testTarget="
                    + testTargetIssue
                    + "｜origin="
                    + processedOrigins
                    + "｜prediction="
                    + processedPredictions
                    + "(历史="
                    + historicalPredictions
                    + ",实时="
                    + livePredictions
                    + ")"
                    + "｜本轮历史TopK="
                    + generatedHistoricalOrigins
                    + "/"
                    + generatedHistoricalPredictions
                    + "｜cursor="
                    + historyCursor
                    + "｜OOS="
                    + oosPredictions
                    + "｜hit="
                    + hits
                    + "｜family="
                    + familiesUpdated
                    + "｜bucket="
                    + bucketsUpdated
                    + "｜sample="
                    + sampleBefore
                    + "+"
                    + progressSample
                    + "="
                    + totalPublishedSample
                    + "｜pending="
                    + pendingBefore
                    + "->"
                    + pendingAfter
                    + "｜cost="
                    + costMs
                    + "ms"
                    + "｜publish="
                    + publishedVersion
                    + "｜nextRead="
                    + nextReadSuccess
                    + "｜yield="
                    + yieldedToRealtime
                    + (safe(error).length() == 0 ? "" : "｜error=" + error);
        }
    }

    private static final class Row {
        long id;
        String baseIssue = "";
        String targetIssue = "";
        String coarse = "";
        String mid = "";
        String fine = "";
        String position = "";
        String source = "";
        int hit;
        double rank;
        double probability;
        double modelProbability;
        OnlineActionModelEngine.FeatureVector features;
    }

    public Report runBatch(DbStore store, int maxOrigins, long budgetMs) {
        return runBatch(store, maxOrigins, budgetMs, null);
    }

    public Report runBatch(DbStore store, int maxOrigins, long budgetMs, YieldSignal signal) {
        Report r = new Report();
        long started = System.currentTimeMillis();
        int originLimit =
                Math.max(
                        1,
                        Math.min(
                                AppConfig.MICRO_WALK_FORWARD_MAX_ORIGINS,
                                maxOrigins <= 0
                                        ? AppConfig.MICRO_WALK_FORWARD_BATCH_ORIGINS
                                        : maxOrigins));
        long budget =
                Math.max(
                        1200L,
                        Math.min(
                                AppConfig.MICRO_WALK_FORWARD_HARD_BUDGET_MS,
                                budgetMs <= 0L
                                        ? AppConfig.MICRO_WALK_FORWARD_TARGET_BUDGET_MS
                                        : budgetMs));
        long deadline = started + budget;
        if (store == null) return finish(store, r, started);

        try {
            r.sampleBefore = countProcessed(store);
            r.pendingBefore = OosActionLogEngine.countSettledUnprocessedForCurrentBuild(store);

            /* Publish-first: this work is small and must not be starved by another scan. */
            publishPending(
                    store,
                    Math.max(8, AppConfig.MICRO_WALK_FORWARD_MAX_PREDICTIONS_PER_BATCH),
                    deadline,
                    r);

            if (AppConfig.MICRO_WALK_FORWARD_HISTORICAL_BOOTSTRAP_ENABLED
                    && System.currentTimeMillis() < deadline
                    && r.totalPublishedSample < AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE * 3) {
                bootstrapHistoricalTopKActions(store, originLimit, deadline, signal, r);
            }

            /* Always flush rows created by the last origin, even when realtime requested yield. */
            publishPending(
                    store,
                    Math.max(8, AppConfig.MICRO_WALK_FORWARD_MAX_PREDICTIONS_PER_BATCH),
                    deadline + 500L,
                    r);

            r.totalPublishedSample = countProcessed(store);
            r.progressSample = Math.max(0, r.totalPublishedSample - r.sampleBefore);
            r.pendingAfter = OosActionLogEngine.countSettledUnprocessedForCurrentBuild(store);
            r.historyCursor = store.getState(STATE_HISTORY_CURSOR, "");
            r.nextReadSuccess = verifyPublishedRead(store) ? 1 : 0;

            if (r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE) {
                r.status = STATUS_READY;
            } else if (r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_WARM_SAMPLE) {
                r.status = STATUS_WARM;
            } else if (r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_PROBE_SAMPLE) {
                r.status = STATUS_PROBE;
            } else if (r.progressSample > 0 || r.processedPredictions > 0) {
                r.status = STATUS_WATCH;
            } else if (r.yieldedToRealtime == 1) {
                r.status = STATUS_YIELD;
            } else {
                r.status = STATUS_IDLE;
            }
        } catch (Throwable e) {
            r.status = STATUS_ERROR;
            r.error = e.getClass().getSimpleName() + ":" + safe(e.getMessage());
            try {
                store.setState(STATE_LAST_ERROR, r.error);
            } catch (Throwable ignore) {
            }
        }
        return finish(store, r, started);
    }

    public static boolean isReady(DbStore store) {
        if (store == null) return false;
        return MODULE_VERSION.equals(store.getState(STATE_VERSION, ""))
                && "1".equals(store.getState(STATE_READY, "0"))
                && parseInt(store.getState(STATE_SAMPLE, "0"), 0)
                        >= AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE;
    }

    public static boolean isWarm(DbStore store) {
        return store != null
                && MODULE_VERSION.equals(store.getState(STATE_VERSION, ""))
                && parseInt(store.getState(STATE_SAMPLE, "0"), 0)
                        >= AppConfig.MICRO_WALK_FORWARD_WARM_SAMPLE;
    }

    public static boolean isProbeReady(DbStore store) {
        return store != null
                && MODULE_VERSION.equals(store.getState(STATE_VERSION, ""))
                && parseInt(store.getState(STATE_SAMPLE, "0"), 0)
                        >= AppConfig.MICRO_WALK_FORWARD_PROBE_SAMPLE;
    }

    private void bootstrapHistoricalTopKActions(
            DbStore store, int originLimit, long deadline, YieldSignal signal, Report report) {
        List<Models.Draw> draws;
        try {
            draws = store.getRecentDraws(AppConfig.MICRO_WALK_FORWARD_HISTORY_LOAD);
        } catch (Throwable e) {
            report.error = append(report.error, "LOAD_HISTORY:" + e.getClass().getSimpleName());
            return;
        }
        if (draws == null || draws.size() <= AppConfig.MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY) return;

        String cursor = store.getState(STATE_HISTORY_CURSOR, "");
        int minTrain =
                Math.max(
                        AppConfig.MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY,
                        Math.min(AppConfig.MICRO_WALK_FORWARD_VISIBLE_HISTORY, draws.size() / 4));
        int first = findFirstOrigin(draws, minTrain, cursor);
        if (first < 0) return;

        FullCandidateScanEngine scanner = new FullCandidateScanEngine();
        for (int idx = first;
                idx < draws.size() && report.generatedHistoricalOrigins < originLimit;
                idx++) {
            if (System.currentTimeMillis() >= deadline) break;
            if (report.generatedHistoricalOrigins
                            >= AppConfig.MICRO_WALK_FORWARD_MIN_PROGRESS_PER_BATCH
                    && shouldYield(signal)) {
                report.yieldedToRealtime = 1;
                break;
            }

            Models.Draw actual = draws.get(idx);
            if (actual == null || !actual.isValid()) continue;
            int from = Math.max(0, idx - AppConfig.MICRO_WALK_FORWARD_VISIBLE_HISTORY);
            if (idx - from < AppConfig.MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY) continue;
            ArrayList<Models.Draw> visible = new ArrayList<Models.Draw>();
            for (int j = from; j < idx; j++) {
                Models.Draw d = draws.get(j);
                if (d != null && d.isValid()) visible.add(d);
            }
            if (visible.size() < AppConfig.MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY) continue;

            String base = visible.get(visible.size() - 1).issue;
            ArrayList<Models.ComboCandidate> top = new ArrayList<Models.ComboCandidate>();
            try {
                top =
                        scanner.scanTopLight(
                                base,
                                actual.issue,
                                visible,
                                AppConfig.MICRO_WALK_FORWARD_LIGHT_TOP_NUMBERS,
                                AppConfig.MICRO_WALK_FORWARD_CANDIDATES_PER_ORIGIN);
            } catch (Throwable e) {
                report.error = append(report.error, "SCAN_TOPK:" + e.getClass().getSimpleName());
            }

            int insertedCount = 0;
            OnlineActionModelEngine.Snapshot online = OnlineActionModelEngine.load(store);
            for (int slot = 0; slot < top.size(); slot++) {
                Models.ComboCandidate candidate = top.get(slot);
                if (candidate == null) continue;
                candidate.baseIssue = base;
                candidate.targetIssue = actual.issue;
                candidate.rawRankScore =
                        RealtimeCausalProbabilityEngine.computeRawRankScore(candidate);
                candidate.modelP = OnlineActionModelEngine.predict(online, candidate);
                candidate.calibratedHitProbability = candidate.modelP;
                candidate.calibratedP = candidate.calibratedHitProbability;
                candidate.finalP = candidate.calibratedHitProbability;
                candidate.honestProbability = candidate.finalP;
                if (candidate.dynamicGate <= 0.0d)
                    candidate.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
                candidate.evidenceAsOfIssue = base;
                candidate.snapshotAsOfIssue = base;
                candidate.teacherAsOfIssue = base;
                candidate.probabilityMode = "V40_HISTORICAL_DISTRIBUTION_TOPK_ACTION";
                candidate.oosRankSlot = slot + 1;
                candidate.oosCandidateCount = top.size();
                PredictionContractEngine.stampCandidate(candidate, visible);
                ActionUtilityEngine.evaluate(null, candidate);
                long id =
                        OosActionLogEngine.recordHistoricalCandidate(
                                store, candidate, actual, slot + 1, top.size());
                if (id >= 0L) insertedCount++;
            }

            /* Cursor advances only after the origin and all immutable Top-K predictions
             * are deterministically evaluated. */
            cursor = actual.issue;
            report.historyCursor = cursor;
            try {
                store.setState(STATE_HISTORY_CURSOR, cursor);
            } catch (Throwable ignore) {
            }
            if (insertedCount > 0) {
                report.generatedHistoricalOrigins++;
                report.generatedHistoricalPredictions += insertedCount;
                publishPending(store, insertedCount, deadline + 500L, report);
            }
        }

        if (report.generatedHistoricalOrigins > 0) {
            int total =
                    parseInt(store.getState(STATE_HISTORY_GENERATED, "0"), 0)
                            + report.generatedHistoricalOrigins;
            try {
                store.setState(STATE_HISTORY_GENERATED, String.valueOf(total));
            } catch (Throwable ignore) {
            }
        }
    }

    private int findFirstOrigin(List<Models.Draw> draws, int minTrain, String cursor) {
        int first = Math.max(1, minTrain);
        if (safe(cursor).length() == 0) return first < draws.size() ? first : -1;
        for (int i = first; i < draws.size(); i++) {
            Models.Draw d = draws.get(i);
            if (d != null
                    && d.isValid()
                    && PredictionContractEngine.compareIssue(d.issue, cursor) > 0) return i;
        }
        return -1;
    }

    /** Legacy compatibility selector; V40 bootstrap uses scanTopLight. */
    private Models.ComboCandidate selectPrimaryAction(List<Models.ComboCandidate> all) {
        Models.ComboCandidate bestQualified = null;
        Models.ComboCandidate bestAny = null;
        double bestQualifiedScore = -1.0d;
        double bestAnyScore = -1.0d;
        if (all == null) return null;
        for (int i = 0; i < all.size(); i++) {
            Models.ComboCandidate c = all.get(i);
            if (c == null || !c.isValidK3()) continue;
            double score = RealtimeCausalProbabilityEngine.computeRawRankScore(c);
            c.rawRankScore = score;
            if (score > bestAnyScore
                    || (Math.abs(score - bestAnyScore) < 0.0000001d && tieBefore(c, bestAny))) {
                bestAny = c;
                bestAnyScore = score;
            }
            double coverage = Math.max(c.coverageQuality, c.coverageQualityScore);
            double third = Math.max(c.thirdQuality, c.thirdNumberQualityScore);
            boolean qualified =
                    c.hardBlockFlag != 1
                            && safe(c.hardBlockSource).length() == 0
                            && coverage >= AppConfig.AUTH_MIN_COVERAGE
                            && third >= AppConfig.AUTH_MIN_THIRD_QUALITY;
            if (qualified
                    && (score > bestQualifiedScore
                            || (Math.abs(score - bestQualifiedScore) < 0.0000001d
                                    && tieBefore(c, bestQualified)))) {
                bestQualified = c;
                bestQualifiedScore = score;
            }
        }
        return bestQualified != null ? bestQualified : bestAny;
    }

    private boolean tieBefore(Models.ComboCandidate a, Models.ComboCandidate b) {
        if (a == null) return false;
        if (b == null) return true;
        if (a.position != b.position) return a.position < b.position;
        return a.k3Mask < b.k3Mask;
    }

    private void publishPending(DbStore store, int limit, long deadline, Report report) {
        if (store == null || limit <= 0) return;
        ArrayList<Row> rows = loadRows(store, limit);
        if (rows.size() == 0) {
            report.totalPublishedSample = countProcessed(store);
            return;
        }

        Report committed = new Report();
        try (DatabaseCoordinator.WriterGuardCP3 cp3WriterGuard1 = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SQLiteDatabase db = store.writable();
            db.beginTransaction();
            try {
                OnlineActionModelEngine.Snapshot onlineModel = OnlineActionModelEngine.loadWithinTransactionCP3(db);
                String lastOrigin = "";
                for (int i = 0; i < rows.size(); i++) {
                    if (i > 0 && System.currentTimeMillis() >= deadline) break;
                    Row x = rows.get(i);
                    try(Cursor live=db.rawQuery("SELECT settled,processed,hit,target_issue FROM "+DbHelper.TABLE_OOS_ACTION_LOG+" WHERE _id=?",new String[]{String.valueOf(x.id)})){
                        if(!live.moveToFirst()||live.getInt(1)!=0)continue;
                        if(live.getInt(0)!=1||live.getInt(2)!=x.hit||!x.targetIssue.equals(live.getString(3)))throw new IllegalStateException("OOS_ROW_CHANGED");}

                    committed.testTargetIssue = x.targetIssue;
                    committed.trainedUntilIssue = x.baseIssue;
                    committed.processedPredictions++;
                    committed.oosPredictions++;
                    if (!safe(x.targetIssue).equals(lastOrigin)) {
                        committed.processedOrigins++;
                        lastOrigin = safe(x.targetIssue);
                    }
                    if (OosActionLogEngine.SOURCE_HISTORICAL_WALK_FORWARD.equals(x.source)) {
                        committed.historicalPredictions++;
                    } else {
                        committed.livePredictions++;
                    }
                    if (x.hit == 1) committed.hits++;
                    committed.familiesUpdated +=
                            updateFamily(db, "GLOBAL", "GLOBAL", x.hit, x.targetIssue);
                    committed.familiesUpdated +=
                            updateFamily(db, "POSITION", x.position, x.hit, x.targetIssue);
                    committed.familiesUpdated +=
                            updateFamily(db, "COARSE", x.coarse, x.hit, x.targetIssue);
                    committed.familiesUpdated += updateFamily(db, "MID", x.mid, x.hit, x.targetIssue);
                    committed.familiesUpdated +=
                            updateFamily(db, "FINE", x.fine, x.hit, x.targetIssue);
                    OnlineActionModelEngine.update(onlineModel, x.features, x.hit);
                    committed.bucketsUpdated +=
                            updateBucket(
                                    db,
                                    MonotonicCalibrationEngine.bucket(x.modelProbability),
                                    x.hit,
                                    x.modelProbability);
                    ContentValues done = new ContentValues();
                    done.put("processed", 1);
                    done.put("processed_at", System.currentTimeMillis());
                    done.put("published_version", MODULE_VERSION);
                    int marked = db.update(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            done,
                            "_id=? AND settled=1 AND processed=0",
                            new String[] {String.valueOf(x.id)});
                    if(marked!=1)throw new IllegalStateException("OOS_PROCESSED_CAS");
                }
                OnlineActionModelEngine.persistWithinTransactionCP3(db,onlineModel,System.currentTimeMillis());
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        report.testTargetIssue=committed.testTargetIssue;
        report.trainedUntilIssue=committed.trainedUntilIssue;
        report.processedPredictions+=committed.processedPredictions;
        report.oosPredictions+=committed.oosPredictions;
        report.processedOrigins+=committed.processedOrigins;
        report.historicalPredictions+=committed.historicalPredictions;
        report.livePredictions+=committed.livePredictions;
        report.hits+=committed.hits;
        report.familiesUpdated+=committed.familiesUpdated;
        report.bucketsUpdated+=committed.bucketsUpdated;
        report.totalPublishedSample = countProcessed(store);
    }

    private ArrayList<Row> loadRows(DbStore store, int limit) {
        ArrayList<Row> out = new ArrayList<Row>();
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    DbHelper.TABLE_OOS_ACTION_LOG,
                                    new String[] {
                                        "_id",
                                        "base_issue",
                                        "target_issue",
                                        "coarse_family_key",
                                        "mid_family_key",
                                        "fine_family_key",
                                        "position_family_key",
                                        "hit",
                                        "raw_rank_score",
                                        "calibrated_probability",
                                        "sample_source",
                                        "model_probability",
                                        "feature_0",
                                        "feature_1",
                                        "feature_2",
                                        "feature_3",
                                        "feature_4",
                                        "feature_5",
                                        "feature_6",
                                        "feature_7"
                                    },
                                    "settled=1 AND processed=0 AND snapshot_max_issue<=base_issue"
                                        + " AND trained_until_issue<=base_issue AND build_id=? AND"
                                        + " cohort_id=? AND schema_version=? AND pipeline_version=?"
                                        + " AND calibration_version=? AND version=?",
                                    new String[] {
                                        AppConfig.BUILD_ID,
                                        AppConfig.COHORT_ID,
                                        AppConfig.SCHEMA_VERSION,
                                        AppConfig.PIPELINE_VERSION,
                                        AppConfig.CALIBRATION_VERSION,
                                        OosActionLogEngine.MODULE_VERSION
                                    },
                                    null,
                                    null,
                                    "target_issue ASC,_id ASC",
                                    String.valueOf(limit));
            while (c != null && c.moveToNext()) {
                Row x = new Row();
                x.id = c.getLong(0);
                x.baseIssue = safe(c.getString(1));
                x.targetIssue = safe(c.getString(2));
                x.coarse = safe(c.getString(3));
                x.mid = safe(c.getString(4));
                x.fine = safe(c.getString(5));
                x.position = safe(c.getString(6));
                x.hit = c.getInt(7);
                x.rank = c.getDouble(8);
                x.probability = c.getDouble(9);
                x.source = safe(c.getString(10));
                x.modelProbability = c.getDouble(11);
                x.features = OnlineActionModelEngine.readFeatureVector(c, 12);
                if (PredictionContractEngine.compareIssue(x.baseIssue, x.targetIssue) < 0)
                    out.add(x);
            }
        } catch (Throwable e) {
            throw new IllegalStateException("OOS_PENDING_READ_FAILED",e);
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return out;
    }

    private int updateFamily(SQLiteDatabase db, String level, String key, int hit, String target) {
        if (db == null || safe(key).length() == 0) return 0;
        Cursor c = null;
        int n = 0, h = 0, rs = 0, rh = 0;
        try {
            c =
                    db.query(
                            DbHelper.TABLE_HIERARCHICAL_POSTERIOR,
                            new String[] {
                                "sample_count", "hit_count", "recent_sample", "recent_hit"
                            },
                            "family_level=? AND family_key=? AND version=?",
                            new String[] {level, key, AppConfig.HIERARCHICAL_POSTERIOR_VERSION},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) {
                n = c.getInt(0);
                h = c.getInt(1);
                rs = c.getInt(2);
                rh = c.getInt(3);
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        n++;
        if (hit == 1) h++;
        if (rs < AppConfig.POSTERIOR_RECENT_WINDOW) {
            rs++;
            if (hit == 1) rh++;
        } else {
            double old = rs <= 0 ? 0.0d : ((double) rh) / rs;
            double next =
                    old
                                    * (AppConfig.POSTERIOR_RECENT_WINDOW - 1.0d)
                                    / AppConfig.POSTERIOR_RECENT_WINDOW
                            + (hit == 1 ? 1.0d : 0.0d) / AppConfig.POSTERIOR_RECENT_WINDOW;
            rs = AppConfig.POSTERIOR_RECENT_WINDOW;
            rh = (int) Math.round(next * rs);
        }
        double mean = HierarchicalPosteriorEngine.betaMean(h, n);
        double lcb = HierarchicalPosteriorEngine.wilsonLower(h, n);
        double recent = rs > 0 ? ((double) rh) / rs : mean;
        double drift = Math.abs(recent - mean);
        ContentValues v = new ContentValues();
        v.put("family_level", level);
        v.put("family_key", key);
        v.put("sample_count", n);
        v.put("hit_count", h);
        v.put("recent_sample", rs);
        v.put("recent_hit", rh);
        v.put("posterior_mean", mean);
        v.put("wilson_lower", lcb);
        v.put("recent_rate", recent);
        v.put("drift_score", drift);
        v.put("last_target_issue", safe(target));
        v.put("updated_at", System.currentTimeMillis());
        v.put("version", AppConfig.HIERARCHICAL_POSTERIOR_VERSION);
        long id =
                db.insertWithOnConflict(
                        DbHelper.TABLE_HIERARCHICAL_POSTERIOR,
                        null,
                        v,
                        SQLiteDatabase.CONFLICT_REPLACE);
        return id >= 0L ? 1 : 0;
    }

    private int updateBucket(SQLiteDatabase db, int bucket, int hit, double predicted) {
        Cursor c = null;
        int n = 0, h = 0;
        double bs = 0.0d;
        try {
            c =
                    db.query(
                            DbHelper.TABLE_MONOTONIC_CALIBRATION,
                            new String[] {"sample_count", "hit_count", "brier_sum"},
                            "calibration_version=? AND bucket_no=?",
                            new String[] {AppConfig.CALIBRATION_VERSION, String.valueOf(bucket)},
                            null,
                            null,
                            null,
                            "1");
            if (c != null && c.moveToFirst()) {
                n = c.getInt(0);
                h = c.getInt(1);
                bs = c.getDouble(2);
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        n++;
        if (hit == 1) h++;
        double y = hit == 1 ? 1.0d : 0.0d;
        double p = clamp(predicted, 0.0d, 1.0d);
        bs += (p - y) * (p - y);
        ContentValues v = new ContentValues();
        v.put("calibration_version", AppConfig.CALIBRATION_VERSION);
        v.put("bucket_no", bucket);
        v.put("sample_count", n);
        v.put("hit_count", h);
        v.put("brier_sum", bs);
        v.put("updated_at", System.currentTimeMillis());
        v.put("version", MODULE_VERSION);
        long id =
                db.insertWithOnConflict(
                        DbHelper.TABLE_MONOTONIC_CALIBRATION,
                        null,
                        v,
                        SQLiteDatabase.CONFLICT_REPLACE);
        return id >= 0L ? 1 : 0;
    }

    private Report finish(DbStore store, Report r, long start) {
        r.costMs = System.currentTimeMillis() - start;
        if (store != null) {
            try {
                int actual = countProcessed(store);
                r.totalPublishedSample = Math.max(r.totalPublishedSample, actual);
                r.progressSample =
                        Math.max(r.progressSample, r.totalPublishedSample - r.sampleBefore);
                r.pendingAfter = OosActionLogEngine.countSettledUnprocessedForCurrentBuild(store);
                boolean ready = r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE;
                if (ready) r.status = STATUS_READY;
                else if (r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_WARM_SAMPLE)
                    r.status = STATUS_WARM;
                else if (r.totalPublishedSample >= AppConfig.MICRO_WALK_FORWARD_PROBE_SAMPLE)
                    r.status = STATUS_PROBE;
                store.setState(STATE_STATUS, r.status);
                store.setState(STATE_VERSION, MODULE_VERSION);
                store.setState(STATE_SAMPLE, String.valueOf(r.totalPublishedSample));
                store.setState(STATE_READY, ready ? "1" : "0");
                store.setState(STATE_PENDING, String.valueOf(r.pendingAfter));
                if (r.progressSample > 0) {
                    store.setState(STATE_PROGRESS_AT, String.valueOf(System.currentTimeMillis()));
                }
                if (safe(r.testTargetIssue).length() > 0) {
                    store.setState(STATE_LAST_TARGET, r.testTargetIssue);
                }
                store.setState(STATE_LAST_COST, String.valueOf(r.costMs));
                store.setState(STATE_LAST_SUMMARY, r.summary());
            } catch (Throwable ignore) {
            }
        }
        return r;
    }

    private int countProcessed(DbStore store) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + DbHelper.TABLE_OOS_ACTION_LOG
                                            + " WHERE processed=1 AND published_version=? AND"
                                            + " build_id=? AND cohort_id=? AND schema_version=? AND"
                                            + " pipeline_version=? AND calibration_version=? AND"
                                            + " version=?",
                                    new String[] {
                                        MODULE_VERSION,
                                        AppConfig.BUILD_ID,
                                        AppConfig.COHORT_ID,
                                        AppConfig.SCHEMA_VERSION,
                                        AppConfig.PIPELINE_VERSION,
                                        AppConfig.CALIBRATION_VERSION,
                                        OosActionLogEngine.MODULE_VERSION
                                    });
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private boolean verifyPublishedRead(DbStore store) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    DbHelper.TABLE_HIERARCHICAL_POSTERIOR,
                                    new String[] {"sample_count"},
                                    "family_level='GLOBAL' AND family_key='GLOBAL' AND version=?",
                                    new String[] {AppConfig.HIERARCHICAL_POSTERIOR_VERSION},
                                    null,
                                    null,
                                    null,
                                    "1");
            return c != null && c.moveToFirst() && c.getInt(0) > 0;
        } catch (Throwable e) {
            return false;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private static boolean shouldYield(YieldSignal signal) {
        try {
            return signal != null && signal.shouldYield();
        } catch (Throwable e) {
            return true;
        }
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0) return safe(a);
        return a + ";" + b;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
