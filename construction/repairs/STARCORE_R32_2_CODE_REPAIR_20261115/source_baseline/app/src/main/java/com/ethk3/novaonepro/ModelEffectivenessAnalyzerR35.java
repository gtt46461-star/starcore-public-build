package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * R3.6 evidence-only runtime/model diagnosis. The historical class name is retained for binary
 * compatibility; no R3.5 writer or model method is used.
 */
public final class ModelEffectivenessAnalyzerR35 {
    public static final String ACTIVE_SCHEMA = "R36_TYPED_DIAGNOSIS_V1";

    public Analysis analyze(DbStore store) {
        RuntimeHealthLedgerR36.BusinessTruth business =
                RuntimeHealthLedgerR36.businessTruth(store);
        RuntimeSummary runtime = readRuntime(store);
        Metrics metrics = readMetrics(store);
        Coverage coverage = readCoverage(store, 30);
        CountResult pending =
                readCount(
                        store,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                        null);
        boolean artifactsProven =
                V51NumberModelArtifact.PROVEN
                        && V51ExpertGateArtifact.PROVEN
                        && V51ActionRankerArtifact.PROVEN
                        && V51ActionRankerArtifact.RANK_ORDER_PASSED
                        && V51SelectedCalibrationArtifact.PROVEN
                        && V51PolicySurfaceArtifact.MODEL_GATE_PASSED;
        ArrayList<Finding> findings = new ArrayList<Finding>();

        if (!"OK".equals(runtime.readStatus)) {
            findings.add(
                    finding(
                            "P0",
                            "RUNTIME_CHAIN",
                            "RuntimeHealthLedgerR36",
                            "readRuntime",
                            runtime.readStatus + ':' + runtime.error,
                            "A read error cannot be rendered as zero/PASS",
                            1.0d,
                            "RUNTIME_LEDGER_READ_ERROR",
                            "Repair the database/read path.",
                            "Do not substitute zero metrics.",
                            "T23 injected read error",
                            "READ_ERROR/P0",
                            "Keep SHADOW_ONLY and export raw DB evidence."));
        } else {
            if (runtime.maxConcurrency > 1) {
                findings.add(
                        finding(
                                "P0",
                                "RUNTIME_CHAIN",
                                "AnalysisSingleFlightR36",
                                "submit/releaseAndTakeNext",
                                "maxConcurrency=" + runtime.maxConcurrency,
                                "required<=1",
                                1.0d,
                                "ANALYSIS_CONCURRENCY_ESCAPE",
                                "Keep one process-wide owner and latest-pending successor.",
                                "Do not create a rescue model thread.",
                                "T01/T04 stress",
                                "maxConcurrency=1",
                                "Stop active runtime and preserve canonical rows."));
            }
            if (runtime.oomCount > 0) {
                findings.add(
                        finding(
                                "P0",
                                "RUNTIME_CHAIN",
                                "SettlementEngine/DeferredEvidenceWriterR36",
                                "bounded owner paths",
                                "oomCount=" + runtime.oomCount,
                                "OOM must remain explicit",
                                1.0d,
                                "OOM_OBSERVED",
                                "Keep settlement exact and deferred chunks bounded.",
                                "Do not swallow OOM into a success status.",
                                "T29-T34 soak",
                                "OOM=0",
                                "Keep durable snapshots; pause deferred enrichment."));
            }
            if (runtime.eventCount > 0
                    && runtime.analysisP95Ms > AppConfig.R36_ANALYSIS_TARGET_MS) {
                findings.add(
                        finding(
                                "P0",
                                "RUNTIME_CHAIN",
                                "V51RuntimeOrchestrator",
                                "runR36",
                                "requestToCommitP95Ms=" + runtime.analysisP95Ms,
                                "device target=" + AppConfig.R36_ANALYSIS_TARGET_MS,
                                0.95d,
                                "REALTIME_BUDGET_EXCEEDED",
                                "Profile typed stage spans.",
                                "Do not reduce the formal 1200-action universe.",
                                "T19/T29",
                                "P95<=5000ms",
                                "Remain SHADOW_ONLY; retain full action space."));
            }
            if ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                    && runtime.lastAttemptAt >= runtime.lastAt) {
                findings.add(
                        finding(
                                "P0",
                                "RUNTIME_CHAIN",
                                "V51RuntimeOrchestrator",
                                "runR36",
                                "lastAttempt="
                                        + runtime.lastAttemptStatus
                                        + '|'
                                        + runtime.lastAttemptDetail,
                                "A failed attempt must never be hidden as NEVER_COMMITTED",
                                1.0d,
                                "ANALYSIS_ATTEMPT_FAILED",
                                "Repair the exact failing stage, then replay the same PIT input.",
                                "Do not infer insufficient sample while the producer is failing.",
                                "T39 history-hash parity plus physical replay",
                                "ANALYSIS_CANONICAL_COMMITTED",
                                "Keep SHADOW_ONLY and preserve the failed-attempt row."));
            }
        }

        if (!"OK".equals(metrics.readStatus)) {
            findings.add(
                    finding(
                            "P0",
                            "OOS",
                            "ModelEffectivenessAnalyzerR35",
                            "readMetrics",
                            metrics.readStatus + ':' + metrics.error,
                            "No numeric fallback is permitted",
                            1.0d,
                            "OOS_READ_ERROR",
                            "Repair the typed OOS read.",
                            "Do not display 0/PASS.",
                            "T23",
                            "READ_ERROR/P0",
                            "Keep model unproven."));
        } else if (metrics.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE) {
            findings.add(
                    finding(
                            "P1",
                            "SAMPLE",
                            "ModelEffectivenessAnalyzerR35",
                            "analyze",
                            "strictOosSample=" + metrics.strictOosSample,
                            "promotionMin=" + AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE,
                            1.0d,
                            "INSUFFICIENT_STRICT_OOS_SAMPLE",
                            "Continue one frozen SHADOW per issue.",
                            "Do not infer effectiveness from a tiny sample.",
                            "Accumulate pre-draw then post-draw labels",
                            "N>=120",
                            "No code rollback required."));
        }
        if (!"OK".equals(coverage.readStatus)) {
            findings.add(
                    finding(
                            "P0",
                            "COVERAGE",
                            "ModelEffectivenessAnalyzerR35",
                            "readCoverage",
                            coverage.readStatus + ':' + coverage.error,
                            "Expected targets must be enumerable",
                            1.0d,
                            "COVERAGE_READ_ERROR",
                            "Repair the expected-target query.",
                            "Do not use table row count as coverage.",
                            "T20/T21",
                            "explicit missing targets",
                            "Keep canonical rows immutable."));
        }
        if (!artifactsProven) {
            findings.add(
                    finding(
                            "P1",
                            "MODEL",
                            "V51NumberModelArtifact/V51SelectedCalibrationArtifact",
                            "PROVEN",
                            "artifactProof=false",
                            "REAL requires every frozen artifact proof",
                            1.0d,
                            "MODEL_DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY",
                            "Produce offline frozen artifact hashes and strict OOS proof.",
                            "Do not auto-unlock REAL or loosen DecisionGate.",
                            "Artifact + OOS certificate",
                            "all artifacts proven",
                            "Remain SHADOW_ONLY."));
        }
        if ("OK".equals(pending.readStatus) && pending.value > 0) {
            findings.add(
                    finding(
                            "P2",
                            "EVIDENCE",
                            "DeferredEvidenceWriterR36",
                            "drainAvailable",
                            "pending=" + pending.value,
                            "eventual exact 100/1200",
                            0.95d,
                            "DEFERRED_EVIDENCE_PENDING",
                            "Resume from the durable snapshot/lease.",
                            "Do not rerun a model after draw.",
                            "T08-T10/T28",
                            "pending=0 and exact1200",
                            "Keep canonical; retry snapshot materialization only."));
        }

        String rootCause;
        if ("READ_ERROR".equals(business.rootCause)
                || !"OK".equals(runtime.readStatus)
                || !"OK".equals(metrics.readStatus)
                || !"OK".equals(coverage.readStatus)
                || !"OK".equals(pending.readStatus)) rootCause = "READ_ERROR_FIRST";
        else if ("RUNTIME_CHAIN".equals(business.rootCause)
                || ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                        && runtime.lastAttemptAt >= runtime.lastAt)
                || runtime.maxConcurrency > 1
                || runtime.oomCount > 0
                || (runtime.eventCount > 0
                        && runtime.analysisP95Ms > AppConfig.R36_ANALYSIS_TARGET_MS))
            rootCause = "RUNTIME_CHAIN_FIRST";
        else if ("SETTLEMENT_OOS".equals(business.rootCause))
            rootCause = "SETTLEMENT_OOS_FIRST";
        else if (metrics.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE)
            rootCause = "INSUFFICIENT_SAMPLE";
        else if (metrics.calibrationSample < metrics.strictOosSample)
            rootCause = "CALIBRATION_UNPROVEN";
        else if (!artifactsProven) rootCause = "MODEL_ARTIFACT_UNPROVEN";
        else rootCause = "NO_FAILURE_PROVEN_CONTINUE_MONITORING";

        String modelStatus =
                artifactsProven
                                && "OK".equals(metrics.readStatus)
                                && metrics.strictOosSample >= AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE
                        ? "PROOF_INPUTS_AVAILABLE_REQUIRES_FROZEN_THRESHOLD_REVIEW"
                        : "DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY";
        return new Analysis(
                runtime,
                metrics,
                coverage,
                "OK".equals(pending.readStatus) ? pending.value : -1,
                pending.readStatus,
                artifactsProven,
                modelStatus,
                rootCause,
                findings);
    }

    private static RuntimeSummary readRuntime(DbStore store) {
        if (store == null) return RuntimeSummary.readError("STORE_NULL");
        long epochAt = RuntimeReleaseEpochR817.readAt(store);
        if (epochAt <= 0L) return RuntimeSummary.readError("R816_RELEASE_EPOCH_MISSING");
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.rawQuery(
                            "SELECT"
                                + " elapsed_ms,max_concurrency,oom_count,status,target_issue,event_at,detail"
                                + " FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type='" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "'"
                                    + " AND event_at>=?"
                                    + " ORDER BY event_at DESC,_id DESC LIMIT 200",
                            new String[] {String.valueOf(epochAt)});
            ArrayList<Long> elapsed = new ArrayList<Long>();
            int maximum = 0, oom = 0;
            String last = "NEVER_COMMITTED", target = "";
            long lastAt = 0L;
            int observed = 0;
            while (c.moveToNext()) {
                if (observed++ == 0) {
                    last = safe(c.getString(3));
                    target = safe(c.getString(4));
                    lastAt = c.getLong(5);
                }
                if (RuntimeHealthLedgerR36.measuredDurableR80(c.getLong(0), c.getString(3), c.getString(6)))
                    elapsed.add(Long.valueOf(c.getLong(0)));
                maximum = Math.max(maximum, c.getInt(1));
                oom += Math.max(0, c.getInt(2));
            }
            c.close();
            c = null;
            c =
                    db.rawQuery(
                            "SELECT COALESCE(SUM(CASE WHEN oom_count>0 OR status='OOM'"
                                    + " OR detail LIKE '%OutOfMemoryError%' THEN 1 ELSE 0 END),0),"
                                    + "COALESCE(MAX(max_concurrency),0)"
                                    + " FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_at>=?",
                            new String[] {String.valueOf(epochAt)});
            if (c.moveToFirst()) {
                oom = Math.max(oom, c.getInt(0));
                maximum = Math.max(maximum, c.getInt(1));
            }
            c.close();
            c = null;
            String lastAttemptType = "NEVER", lastAttemptStatus = "NEVER", lastAttemptTarget = "";
            String lastAttemptDetail = "";
            long lastAttemptAt = 0L;
            c =
                    db.rawQuery(
                            "SELECT event_type,status,target_issue,event_at,detail FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type IN"
                                    + " ('" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "','ANALYSIS_ATTEMPT_FAILED')"
                                    + " AND event_at>=?"
                                    + " ORDER BY event_at DESC LIMIT 1",
                            new String[] {String.valueOf(epochAt)});
            if (c.moveToFirst()) {
                lastAttemptType = safe(c.getString(0));
                lastAttemptStatus = safe(c.getString(1));
                lastAttemptTarget = safe(c.getString(2));
                lastAttemptAt = c.getLong(3);
                lastAttemptDetail = safe(c.getString(4));
            }
            Collections.sort(elapsed);
            long p95 =
                    elapsed.size() == 0
                            ? -1L
                            : elapsed.get(Math.max(0, (int) Math.ceil(elapsed.size() * 0.95d) - 1))
                                    .longValue();
            LifetimeRuntime lifetime = readLifetimeRuntime(db);
            return new RuntimeSummary(
                    elapsed.isEmpty() ? "NO_MEASURED_DATA" : "OK",
                    elapsed.size(),
                    p95,
                    maximum,
                    oom,
                    last,
                    target,
                    lastAt,
                    lastAttemptType,
                    lastAttemptStatus,
                    lastAttemptTarget,
                    lastAttemptAt,
                    lastAttemptDetail,
                    epochAt,
                    lifetime.eventCount,
                    lifetime.p95Ms,
                    lifetime.maxConcurrency,
                    lifetime.oomCount,
                    "");
        } catch (Throwable e) {
            return RuntimeSummary.readError(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static LifetimeRuntime readLifetimeRuntime(SQLiteDatabase db) {
        Cursor c = null;
        try {
            ArrayList<Long> elapsed = new ArrayList<Long>();
            c =
                    db.rawQuery(
                            "SELECT elapsed_ms,status,detail FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type='" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "'"
                                    + " ORDER BY event_at DESC,_id DESC LIMIT 200",
                            null);
            while (c.moveToNext()) if (RuntimeHealthLedgerR36.measuredDurableR80(c.getLong(0), c.getString(1), c.getString(2)))
                elapsed.add(Long.valueOf(c.getLong(0)));
            c.close();
            c = null;
            Collections.sort(elapsed);
            long p95 =
                    elapsed.size() == 0
                            ? -1L
                            : elapsed.get(Math.max(0, (int) Math.ceil(elapsed.size() * 0.95d) - 1))
                                    .longValue();
            c =
                    db.rawQuery(
                            "SELECT COALESCE(SUM(CASE WHEN oom_count>0 OR status='OOM'"
                                    + " OR detail LIKE '%OutOfMemoryError%' THEN 1 ELSE 0 END),0),"
                                    + "COALESCE(MAX(max_concurrency),0) FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH,
                            null);
            int oom = 0, maximum = 0;
            if (c.moveToFirst()) {
                oom = c.getInt(0);
                maximum = c.getInt(1);
            }
            return new LifetimeRuntime(elapsed.size(), p95, maximum, oom);
        } finally {
            if (c != null) c.close();
        }
    }

    private static final class LifetimeRuntime {
        final int eventCount, maxConcurrency, oomCount;
        final long p95Ms;

        LifetimeRuntime(int events, long p95, int maximum, int oom) {
            eventCount = events;
            p95Ms = p95;
            maxConcurrency = maximum;
            oomCount = oom;
        }
    }

    private static Metrics readMetrics(DbStore store) {
        if (store == null) return Metrics.readError("STORE_NULL");
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT selected_hit_p,hit_label,brier FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 ORDER BY settled_at ASC",
                                    null);
            int n = 0, hit = 0, validP = 0;
            double brierSum = 0.0d;
            int[] binN = new int[10], binHit = new int[10];
            double[] binP = new double[10];
            while (c.moveToNext()) {
                n++;
                int y = c.getInt(1);
                if (y == 1) hit++;
                double p = c.getDouble(0), brier = c.getDouble(2);
                if (p >= 0.0d && p <= 1.0d && !Double.isNaN(p) && !Double.isInfinite(p)) {
                    validP++;
                    brierSum += brier >= 0.0d ? brier : (p - y) * (p - y);
                    int bin = Math.min(9, Math.max(0, (int) Math.floor(p * 10.0d)));
                    binN[bin]++;
                    binHit[bin] += y;
                    binP[bin] += p;
                }
            }
            double ece = -1.0d;
            if (validP > 0) {
                ece = 0.0d;
                for (int i = 0; i < 10; i++)
                    if (binN[i] > 0) {
                        double avgP = binP[i] / binN[i], avgY = ((double) binHit[i]) / binN[i];
                        ece += ((double) binN[i] / validP) * Math.abs(avgP - avgY);
                    }
            }
            double rate = n > 0 ? ((double) hit) / n : -1.0d;
            double brier = validP > 0 ? brierSum / validP : -1.0d;
            return new Metrics("OK", n, hit, validP, rate, ece, brier, wilsonLower(hit, n), "");
        } catch (Throwable e) {
            return Metrics.readError(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static Coverage readCoverage(DbStore store, int requestedWindow) {
        if (store == null) return Coverage.readError(requestedWindow, "STORE_NULL");
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.rawQuery(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                            + " WHERE state_key='r922_activation_base_issue' LIMIT 1",
                            null);
            String activationBase = c.moveToFirst() ? safe(c.getString(0)) : "";
            c.close();
            c = null;
            if (!TargetIssueResolver.isValid(activationBase))
                return new Coverage(
                        "OK",
                        requestedWindow,
                        0,
                        0,
                        Collections.<String>emptyList(),
                        "",
                        "R922",
                        "");
            c =
                    db.rawQuery(
                            "SELECT issue FROM "
                                    + DbHelper.TABLE_DRAWS
                                    + " WHERE dirty=0 AND issue>? ORDER BY issue DESC LIMIT ?",
                            new String[] {activationBase, String.valueOf(requestedWindow)});
            ArrayList<String> expected = new ArrayList<String>();
            while (c.moveToNext()) {
                String issue = safe(c.getString(0));
                if (TargetIssueResolver.isValid(issue)) expected.add(issue);
            }
            c.close();
            c = null;
            ArrayList<String> missing = new ArrayList<String>();
            int covered = 0;
            for (int i = 0; i < expected.size(); i++) {
                c =
                        db.rawQuery(
                                "SELECT 1 FROM "
                                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                        + " WHERE target_issue=? AND pre_draw_frozen=1"
                                        + " AND runtime_version=? LIMIT 1",
                                new String[] {
                                    expected.get(i), AppConfig.R36_RUNTIME_REPAIR_VERSION
                                });
                if (c.moveToFirst()) covered++;
                else missing.add(expected.get(i));
                c.close();
                c = null;
            }
            return new Coverage(
                    "OK",
                    requestedWindow,
                    expected.size(),
                    covered,
                    missing,
                    activationBase,
                    "R922",
                    "");
        } catch (Throwable e) {
            return Coverage.readError(requestedWindow, e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static CountResult readCount(DbStore store, String sql, String[] args) {
        if (store == null) return new CountResult("READ_ERROR", -1, "STORE_NULL");
        Cursor c = null;
        try {
            c = store.readable().rawQuery(sql, args);
            if (!c.moveToFirst()) return new CountResult("READ_ERROR", -1, "NO_ROW");
            return new CountResult("OK", c.getInt(0), "");
        } catch (Throwable e) {
            return new CountResult("READ_ERROR", -1, e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.959963984540054d,
                p = ((double) hit) / n,
                z2 = z * z,
                den = 1.0d + z2 / n,
                centre = p + z2 / (2.0d * n),
                margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return Math.max(0.0d, (centre - margin) / den);
    }

    private static Finding finding(
            String severity,
            String category,
            String file,
            String method,
            String evidence,
            String counterEvidence,
            double confidence,
            String cause,
            String change,
            String doNotChange,
            String validation,
            String expected,
            String rollback) {
        return new Finding(
                severity,
                category,
                file,
                method,
                evidence,
                counterEvidence,
                confidence,
                cause,
                change,
                doNotChange,
                validation,
                expected,
                rollback);
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    private static final class CountResult {
        final String readStatus, error;
        final int value;

        CountResult(String r, int v, String e) {
            readStatus = r;
            value = v;
            error = e;
        }
    }

    public static final class RuntimeSummary {
        public final String readStatus,
                lastStatus,
                lastTarget,
                lastAttemptType,
                lastAttemptStatus,
                lastAttemptTarget,
                lastAttemptDetail,
                error;
        public final int eventCount, maxConcurrency, oomCount;
        public final int lifetimeEventCount, lifetimeMaxConcurrency, lifetimeOomCount;
        public final long analysisP95Ms, lastAt, lastAttemptAt, releaseEpochAt;
        public final long lifetimeAnalysisP95Ms;

        RuntimeSummary(
                String read,
                int events,
                long p95,
                int max,
                int oom,
                String last,
                String target,
                long at,
                String attemptType,
                String attemptStatus,
                String attemptTarget,
                long attemptAt,
                String attemptDetail,
                long epochAt,
                int lifetimeEvents,
                long lifetimeP95,
                int lifetimeMax,
                int lifetimeOom,
                String error) {
            readStatus = read;
            eventCount = events;
            analysisP95Ms = p95;
            maxConcurrency = max;
            oomCount = oom;
            lastStatus = last;
            lastTarget = target;
            lastAt = at;
            lastAttemptType = attemptType;
            lastAttemptStatus = attemptStatus;
            lastAttemptTarget = attemptTarget;
            lastAttemptAt = attemptAt;
            lastAttemptDetail = attemptDetail;
            releaseEpochAt = epochAt;
            lifetimeEventCount = lifetimeEvents;
            lifetimeAnalysisP95Ms = lifetimeP95;
            lifetimeMaxConcurrency = lifetimeMax;
            lifetimeOomCount = lifetimeOom;
            this.error = error;
        }

        static RuntimeSummary readError(String e) {
            return new RuntimeSummary(
                    "READ_ERROR",
                    -1,
                    -1L,
                    -1,
                    -1,
                    "READ_ERROR",
                    "",
                    0L,
                    "READ_ERROR",
                    "READ_ERROR",
                    "",
                    0L,
                    "",
                    -1L,
                    -1,
                    -1L,
                    -1,
                    -1,
                    safe(e));
        }

        public String summary() {
            return "read="
                    + readStatus
                    + "|scope=R900_CURRENT_RELEASE"
                    + "|epochAt="
                    + releaseEpochAt
                    + "|events="
                    + eventCount
                    + "|requestToCommitP95Ms="
                    + analysisP95Ms
                    + "|maxConcurrency="
                    + maxConcurrency
                    + "|oom="
                    + oomCount
                    + "|last="
                    + lastStatus
                    + "|target="
                    + lastTarget
                    + "|at="
                    + lastAt
                    + "|lastAttempt="
                    + lastAttemptType
                    + ':'
                    + lastAttemptStatus
                    + "|attemptTarget="
                    + lastAttemptTarget
                    + "|attemptAt="
                    + lastAttemptAt
                    + (lastAttemptDetail.length() > 0
                            ? "|attemptDetail=" + lastAttemptDetail
                            : "")
                    + "|lifetimeEvents="
                    + lifetimeEventCount
                    + "|lifetimeRequestToCommitP95Ms="
                    + lifetimeAnalysisP95Ms
                    + "|lifetimeMaxConcurrency="
                    + lifetimeMaxConcurrency
                    + "|lifetimeOom="
                    + lifetimeOomCount
                    + (error.length() > 0 ? "|error=" + error : "");
        }
    }

    public static final class Metrics {
        public final String readStatus, error;
        public final int strictOosSample, hitCount, calibrationSample;
        public final double hitRate, ece, brier, wilsonLower95;

        Metrics(
                String read,
                int n,
                int hit,
                int calibration,
                double rate,
                double ece,
                double brier,
                double wilson,
                String error) {
            readStatus = read;
            strictOosSample = n;
            hitCount = hit;
            calibrationSample = calibration;
            hitRate = rate;
            this.ece = ece;
            this.brier = brier;
            wilsonLower95 = wilson;
            this.error = error;
        }

        static Metrics readError(String e) {
            return new Metrics("READ_ERROR", -1, -1, -1, -1.0d, -1.0d, -1.0d, -1.0d, safe(e));
        }
    }

    public static final class Coverage {
        public final String readStatus, error, activationBaseIssue, activationEpoch;
        public final int requestedWindow, eligible, covered;
        public final List<String> missingTargets;

        Coverage(
                String read,
                int requested,
                int eligible,
                int covered,
                List<String> missing,
                String activationBase,
                String activationEpoch,
                String error) {
            readStatus = read;
            requestedWindow = requested;
            this.eligible = eligible;
            this.covered = covered;
            missingTargets = Collections.unmodifiableList(new ArrayList<String>(missing));
            activationBaseIssue = safe(activationBase);
            this.activationEpoch = safe(activationEpoch);
            this.error = error;
        }

        static Coverage readError(int requested, String e) {
            return new Coverage(
                    "READ_ERROR",
                    requested,
                    -1,
                    -1,
                    Collections.<String>emptyList(),
                    "",
                    "R811",
                    safe(e));
        }

        public String missingCsv() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < missingTargets.size(); i++) {
                if (i > 0) b.append(',');
                b.append(missingTargets.get(i));
            }
            return b.toString();
        }
    }

    public static final class Finding {
        public final String severity,
                category,
                file,
                method,
                evidence,
                counterEvidence,
                cause,
                codeChange,
                doNotChange,
                validation,
                expected,
                rollback;
        public final double confidence;

        Finding(
                String severity,
                String category,
                String file,
                String method,
                String evidence,
                String counterEvidence,
                double confidence,
                String cause,
                String change,
                String doNotChange,
                String validation,
                String expected,
                String rollback) {
            this.severity = safe(severity);
            this.category = safe(category);
            this.file = safe(file);
            this.method = safe(method);
            this.evidence = safe(evidence);
            this.counterEvidence = safe(counterEvidence);
            this.confidence = confidence;
            this.cause = safe(cause);
            codeChange = safe(change);
            this.doNotChange = safe(doNotChange);
            this.validation = safe(validation);
            this.expected = safe(expected);
            this.rollback = safe(rollback);
        }

        public String render() {
            return severity
                    + "|category="
                    + category
                    + "|file="
                    + file
                    + "|method="
                    + method
                    + "|evidence="
                    + evidence
                    + "|counterEvidence="
                    + counterEvidence
                    + "|confidence="
                    + confidence
                    + "|cause="
                    + cause
                    + "|change="
                    + codeChange
                    + "|doNotChange="
                    + doNotChange
                    + "|validation="
                    + validation
                    + "|expected="
                    + expected
                    + "|rollback="
                    + rollback;
        }
    }

    public static final class Analysis {
        public final RuntimeSummary runtime;
        public final Metrics metrics;
        public final Coverage coverage;
        public final int recentCanonical30, deferredPending;
        public final String deferredReadStatus;
        public final boolean artifactsProven;
        public final String modelStatus, rootCause;
        public final List<Finding> findings;

        Analysis(
                RuntimeSummary runtime,
                Metrics metrics,
                Coverage coverage,
                int pending,
                String pendingRead,
                boolean proven,
                String modelStatus,
                String rootCause,
                List<Finding> findings) {
            this.runtime = runtime;
            this.metrics = metrics;
            this.coverage = coverage;
            recentCanonical30 = coverage.covered;
            deferredPending = pending;
            deferredReadStatus = safe(pendingRead);
            artifactsProven = proven;
            this.modelStatus = safe(modelStatus);
            this.rootCause = safe(rootCause);
            this.findings = Collections.unmodifiableList(new ArrayList<Finding>(findings));
        }

        public String render() {
            StringBuilder b = new StringBuilder();
            b.append("rootCause=").append(rootCause).append('\n');
            b.append("modelStatus=").append(modelStatus).append('\n');
            b.append("strictOosRead=")
                    .append(metrics.readStatus)
                    .append("|N=")
                    .append(metrics.strictOosSample)
                    .append("|hit=")
                    .append(metrics.hitCount)
                    .append("|hitRate=")
                    .append(
                            metrics.strictOosSample <= 0
                                    ? "NOT_AVAILABLE"
                                    : String.valueOf(metrics.hitRate))
                    .append("|wilsonLower95=")
                    .append(metrics.wilsonLower95)
                    .append("|ece=")
                    .append(metrics.ece)
                    .append("|brier=")
                    .append(metrics.brier)
                    .append('\n');
            b.append("coverageRead=")
                    .append(coverage.readStatus)
                    .append("|sinceBase=")
                    .append(coverage.activationBaseIssue)
                    .append("|activationEpoch=")
                    .append(coverage.activationEpoch)
                    .append("|covered=")
                    .append(coverage.covered)
                    .append("|eligible=")
                    .append(coverage.eligible)
                    .append("|window=")
                    .append(coverage.requestedWindow)
                    .append("|missing=")
                    .append(coverage.missingCsv())
                    .append('\n');
            b.append("runtime=").append(runtime.summary()).append('\n');
            b.append("deferredRead=")
                    .append(deferredReadStatus)
                    .append("|pending=")
                    .append(deferredPending)
                    .append('\n');
            for (int i = 0; i < findings.size(); i++)
                b.append(findings.get(i).render()).append('\n');
            return b.toString();
        }
    }

public Analysis analyzeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        RuntimeHealthLedgerR36.BusinessTruth business =
                RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report, store);
        RuntimeSummary runtime = readRuntimeSnapshotCP4(report, store);
        Metrics metrics = readMetricsSnapshotCP4(report, store);
        Coverage coverage = readCoverageSnapshotCP4(report, store, 30);
        CountResult pending =
                readCountSnapshotCP4(report, 
                        store,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                        null);
        boolean artifactsProven =
                V51NumberModelArtifact.PROVEN
                        && V51ExpertGateArtifact.PROVEN
                        && V51ActionRankerArtifact.PROVEN
                        && V51ActionRankerArtifact.RANK_ORDER_PASSED
                        && V51SelectedCalibrationArtifact.PROVEN
                        && V51PolicySurfaceArtifact.MODEL_GATE_PASSED;
        ArrayList<Finding> findings = new ArrayList<Finding>();

        if (!"OK".equals(runtime.readStatus)) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P0",
                            "RUNTIME_CHAIN",
                            "RuntimeHealthLedgerR36",
                            "readRuntime",
                            runtime.readStatus + ':' + runtime.error,
                            "A read error cannot be rendered as zero/PASS",
                            1.0d,
                            "RUNTIME_LEDGER_READ_ERROR",
                            "Repair the database/read path.",
                            "Do not substitute zero metrics.",
                            "T23 injected read error",
                            "READ_ERROR/P0",
                            "Keep SHADOW_ONLY and export raw DB evidence."));
        } else {
            if (runtime.maxConcurrency > 1) {
                findings.add(
                        findingSnapshotCP4(report, 
                                "P0",
                                "RUNTIME_CHAIN",
                                "AnalysisSingleFlightR36",
                                "submit/releaseAndTakeNext",
                                "maxConcurrency=" + runtime.maxConcurrency,
                                "required<=1",
                                1.0d,
                                "ANALYSIS_CONCURRENCY_ESCAPE",
                                "Keep one process-wide owner and latest-pending successor.",
                                "Do not create a rescue model thread.",
                                "T01/T04 stress",
                                "maxConcurrency=1",
                                "Stop active runtime and preserve canonical rows."));
            }
            if (runtime.oomCount > 0) {
                findings.add(
                        findingSnapshotCP4(report, 
                                "P0",
                                "RUNTIME_CHAIN",
                                "SettlementEngine/DeferredEvidenceWriterR36",
                                "bounded owner paths",
                                "oomCount=" + runtime.oomCount,
                                "OOM must remain explicit",
                                1.0d,
                                "OOM_OBSERVED",
                                "Keep settlement exact and deferred chunks bounded.",
                                "Do not swallow OOM into a success status.",
                                "T29-T34 soak",
                                "OOM=0",
                                "Keep durable snapshots; pause deferred enrichment."));
            }
            if (runtime.eventCount > 0
                    && runtime.analysisP95Ms > AppConfig.R36_ANALYSIS_TARGET_MS) {
                findings.add(
                        findingSnapshotCP4(report, 
                                "P0",
                                "RUNTIME_CHAIN",
                                "V51RuntimeOrchestrator",
                                "runR36",
                                "requestToCommitP95Ms=" + runtime.analysisP95Ms,
                                "device target=" + AppConfig.R36_ANALYSIS_TARGET_MS,
                                0.95d,
                                "REALTIME_BUDGET_EXCEEDED",
                                "Profile typed stage spans.",
                                "Do not reduce the formal 1200-action universe.",
                                "T19/T29",
                                "P95<=5000ms",
                                "Remain SHADOW_ONLY; retain full action space."));
            }
            if ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                    && runtime.lastAttemptAt >= runtime.lastAt) {
                findings.add(
                        findingSnapshotCP4(report, 
                                "P0",
                                "RUNTIME_CHAIN",
                                "V51RuntimeOrchestrator",
                                "runR36",
                                "lastAttempt="
                                        + runtime.lastAttemptStatus
                                        + '|'
                                        + runtime.lastAttemptDetail,
                                "A failed attempt must never be hidden as NEVER_COMMITTED",
                                1.0d,
                                "ANALYSIS_ATTEMPT_FAILED",
                                "Repair the exact failing stage, then replay the same PIT input.",
                                "Do not infer insufficient sample while the producer is failing.",
                                "T39 history-hash parity plus physical replay",
                                "ANALYSIS_CANONICAL_COMMITTED",
                                "Keep SHADOW_ONLY and preserve the failed-attempt row."));
            }
        }

        if (!"OK".equals(metrics.readStatus)) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P0",
                            "OOS",
                            "ModelEffectivenessAnalyzerR35",
                            "readMetrics",
                            metrics.readStatus + ':' + metrics.error,
                            "No numeric fallback is permitted",
                            1.0d,
                            "OOS_READ_ERROR",
                            "Repair the typed OOS read.",
                            "Do not display 0/PASS.",
                            "T23",
                            "READ_ERROR/P0",
                            "Keep model unproven."));
        } else if (metrics.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P1",
                            "SAMPLE",
                            "ModelEffectivenessAnalyzerR35",
                            "analyze",
                            "strictOosSample=" + metrics.strictOosSample,
                            "promotionMin=" + AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE,
                            1.0d,
                            "INSUFFICIENT_STRICT_OOS_SAMPLE",
                            "Continue one frozen SHADOW per issue.",
                            "Do not infer effectiveness from a tiny sample.",
                            "Accumulate pre-draw then post-draw labels",
                            "N>=120",
                            "No code rollback required."));
        }
        if (!"OK".equals(coverage.readStatus)) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P0",
                            "COVERAGE",
                            "ModelEffectivenessAnalyzerR35",
                            "readCoverage",
                            coverage.readStatus + ':' + coverage.error,
                            "Expected targets must be enumerable",
                            1.0d,
                            "COVERAGE_READ_ERROR",
                            "Repair the expected-target query.",
                            "Do not use table row count as coverage.",
                            "T20/T21",
                            "explicit missing targets",
                            "Keep canonical rows immutable."));
        }
        if (!artifactsProven) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P1",
                            "MODEL",
                            "V51NumberModelArtifact/V51SelectedCalibrationArtifact",
                            "PROVEN",
                            "artifactProof=false",
                            "REAL requires every frozen artifact proof",
                            1.0d,
                            "MODEL_DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY",
                            "Produce offline frozen artifact hashes and strict OOS proof.",
                            "Do not auto-unlock REAL or loosen DecisionGate.",
                            "Artifact + OOS certificate",
                            "all artifacts proven",
                            "Remain SHADOW_ONLY."));
        }
        if ("OK".equals(pending.readStatus) && pending.value > 0) {
            findings.add(
                    findingSnapshotCP4(report, 
                            "P2",
                            "EVIDENCE",
                            "DeferredEvidenceWriterR36",
                            "drainAvailable",
                            "pending=" + pending.value,
                            "eventual exact 100/1200",
                            0.95d,
                            "DEFERRED_EVIDENCE_PENDING",
                            "Resume from the durable snapshot/lease.",
                            "Do not rerun a model after draw.",
                            "T08-T10/T28",
                            "pending=0 and exact1200",
                            "Keep canonical; retry snapshot materialization only."));
        }

        String rootCause;
        if ("READ_ERROR".equals(business.rootCause)
                || !"OK".equals(runtime.readStatus)
                || !"OK".equals(metrics.readStatus)
                || !"OK".equals(coverage.readStatus)
                || !"OK".equals(pending.readStatus)) rootCause = "READ_ERROR_FIRST";
        else if ("RUNTIME_CHAIN".equals(business.rootCause)
                || ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                        && runtime.lastAttemptAt >= runtime.lastAt)
                || runtime.maxConcurrency > 1
                || runtime.oomCount > 0
                || (runtime.eventCount > 0
                        && runtime.analysisP95Ms > AppConfig.R36_ANALYSIS_TARGET_MS))
            rootCause = "RUNTIME_CHAIN_FIRST";
        else if ("SETTLEMENT_OOS".equals(business.rootCause))
            rootCause = "SETTLEMENT_OOS_FIRST";
        else if (metrics.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE)
            rootCause = "INSUFFICIENT_SAMPLE";
        else if (metrics.calibrationSample < metrics.strictOosSample)
            rootCause = "CALIBRATION_UNPROVEN";
        else if (!artifactsProven) rootCause = "MODEL_ARTIFACT_UNPROVEN";
        else rootCause = "NO_FAILURE_PROVEN_CONTINUE_MONITORING";

        String modelStatus =
                artifactsProven
                                && "OK".equals(metrics.readStatus)
                                && metrics.strictOosSample >= AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE
                        ? "PROOF_INPUTS_AVAILABLE_REQUIRES_FROZEN_THRESHOLD_REVIEW"
                        : "DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY";
        return new Analysis(
                runtime,
                metrics,
                coverage,
                "OK".equals(pending.readStatus) ? pending.value : -1,
                pending.readStatus,
                artifactsProven,
                modelStatus,
                rootCause,
                findings);
    }

public static Finding findingSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            String severity,
            String category,
            String file,
            String method,
            String evidence,
            String counterEvidence,
            double confidence,
            String cause,
            String change,
            String doNotChange,
            String validation,
            String expected,
            String rollback) {
        return new Finding(
                severity,
                category,
                file,
                method,
                evidence,
                counterEvidence,
                confidence,
                cause,
                change,
                doNotChange,
                validation,
                expected,
                rollback);
    }

public static CountResult readCountSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String sql, String[] args) {
        if (store == null) return new CountResult("READ_ERROR", -1, "STORE_NULL");
        Cursor c = null;
        try {
            c = report.select(sql, args);
            if (!c.moveToFirst()) return new CountResult("READ_ERROR", -1, "NO_ROW");
            return new CountResult("OK", c.getInt(0), "");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static Coverage readCoverageSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int requestedWindow) {
        if (store == null) return Coverage.readError(requestedWindow, "STORE_NULL");
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    report.select(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                            + " WHERE state_key='r922_activation_base_issue' LIMIT 1",
                            null);
            String activationBase = c.moveToFirst() ? safeSnapshotCP4(report, c.getString(0)) : "";
            c.close();
            c = null;
            if (!TargetIssueResolver.isValid(activationBase))
                return new Coverage(
                        "OK",
                        requestedWindow,
                        0,
                        0,
                        Collections.<String>emptyList(),
                        "",
                        "R922",
                        "");
            c =
                    report.select(
                            "SELECT issue FROM "
                                    + DbHelper.TABLE_DRAWS
                                    + " WHERE dirty=0 AND issue>? ORDER BY issue DESC LIMIT ?",
                            new String[] {activationBase, String.valueOf(requestedWindow)});
            ArrayList<String> expected = new ArrayList<String>();
            while (c.moveToNext()) {
                String issue = safeSnapshotCP4(report, c.getString(0));
                if (TargetIssueResolver.isValid(issue)) expected.add(issue);
            }
            c.close();
            c = null;
            ArrayList<String> missing = new ArrayList<String>();
            int covered = 0;
            for (int i = 0; i < expected.size(); i++) {
                c =
                        report.select(
                                "SELECT 1 FROM "
                                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                        + " WHERE target_issue=? AND pre_draw_frozen=1"
                                        + " AND runtime_version=? LIMIT 1",
                                new String[] {
                                    expected.get(i), AppConfig.R36_RUNTIME_REPAIR_VERSION
                                });
                if (c.moveToFirst()) covered++;
                else missing.add(expected.get(i));
                c.close();
                c = null;
            }
            return new Coverage(
                    "OK",
                    requestedWindow,
                    expected.size(),
                    covered,
                    missing,
                    activationBase,
                    "R922",
                    "");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        return x == null ? "" : x.trim();
    }

public static Metrics readMetricsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return Metrics.readError("STORE_NULL");
        Cursor c = null;
        try {
            c =
                    report.select(
                                    "SELECT selected_hit_p,hit_label,brier FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 ORDER BY settled_at ASC",
                                    null);
            int n = 0, hit = 0, validP = 0;
            double brierSum = 0.0d;
            int[] binN = new int[10], binHit = new int[10];
            double[] binP = new double[10];
            while (c.moveToNext()) {
                n++;
                int y = c.getInt(1);
                if (y == 1) hit++;
                double p = c.getDouble(0), brier = c.getDouble(2);
                if (p >= 0.0d && p <= 1.0d && !Double.isNaN(p) && !Double.isInfinite(p)) {
                    validP++;
                    brierSum += brier >= 0.0d ? brier : (p - y) * (p - y);
                    int bin = Math.min(9, Math.max(0, (int) Math.floor(p * 10.0d)));
                    binN[bin]++;
                    binHit[bin] += y;
                    binP[bin] += p;
                }
            }
            double ece = -1.0d;
            if (validP > 0) {
                ece = 0.0d;
                for (int i = 0; i < 10; i++)
                    if (binN[i] > 0) {
                        double avgP = binP[i] / binN[i], avgY = ((double) binHit[i]) / binN[i];
                        ece += ((double) binN[i] / validP) * Math.abs(avgP - avgY);
                    }
            }
            double rate = n > 0 ? ((double) hit) / n : -1.0d;
            double brier = validP > 0 ? brierSum / validP : -1.0d;
            return new Metrics("OK", n, hit, validP, rate, ece, brier, wilsonLowerSnapshotCP4(report, hit, n), "");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static double wilsonLowerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int hit, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.959963984540054d,
                p = ((double) hit) / n,
                z2 = z * z,
                den = 1.0d + z2 / n,
                centre = p + z2 / (2.0d * n),
                margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return Math.max(0.0d, (centre - margin) / den);
    }

public static RuntimeSummary readRuntimeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return RuntimeSummary.readError("STORE_NULL");
        long epochAt = RuntimeReleaseEpochR817.readAtSnapshotCP4(report, store);
        if (epochAt <= 0L) return RuntimeSummary.readError("R816_RELEASE_EPOCH_MISSING");
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    report.select(
                            "SELECT"
                                + " elapsed_ms,max_concurrency,oom_count,status,target_issue,event_at,detail"
                                + " FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type='" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "'"
                                    + " AND event_at>=?"
                                    + " ORDER BY event_at DESC,_id DESC LIMIT 200",
                            new String[] {String.valueOf(epochAt)});
            ArrayList<Long> elapsed = new ArrayList<Long>();
            int maximum = 0, oom = 0;
            String last = "NEVER_COMMITTED", target = "";
            long lastAt = 0L;
            int observed = 0;
            while (c.moveToNext()) {
                if (observed++ == 0) {
                    last = safeSnapshotCP4(report, c.getString(3));
                    target = safeSnapshotCP4(report, c.getString(4));
                    lastAt = c.getLong(5);
                }
                if (RuntimeHealthLedgerR36.measuredDurableR80(c.getLong(0), c.getString(3), c.getString(6)))
                    elapsed.add(Long.valueOf(c.getLong(0)));
                maximum = Math.max(maximum, c.getInt(1));
                oom += Math.max(0, c.getInt(2));
            }
            c.close();
            c = null;
            c =
                    report.select(
                            "SELECT COALESCE(SUM(CASE WHEN oom_count>0 OR status='OOM'"
                                    + " OR detail LIKE '%OutOfMemoryError%' THEN 1 ELSE 0 END),0),"
                                    + "COALESCE(MAX(max_concurrency),0)"
                                    + " FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_at>=?",
                            new String[] {String.valueOf(epochAt)});
            if (c.moveToFirst()) {
                oom = Math.max(oom, c.getInt(0));
                maximum = Math.max(maximum, c.getInt(1));
            }
            c.close();
            c = null;
            String lastAttemptType = "NEVER", lastAttemptStatus = "NEVER", lastAttemptTarget = "";
            String lastAttemptDetail = "";
            long lastAttemptAt = 0L;
            c =
                    report.select(
                            "SELECT event_type,status,target_issue,event_at,detail FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type IN"
                                    + " ('" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "','ANALYSIS_ATTEMPT_FAILED')"
                                    + " AND event_at>=?"
                                    + " ORDER BY event_at DESC LIMIT 1",
                            new String[] {String.valueOf(epochAt)});
            if (c.moveToFirst()) {
                lastAttemptType = safeSnapshotCP4(report, c.getString(0));
                lastAttemptStatus = safeSnapshotCP4(report, c.getString(1));
                lastAttemptTarget = safeSnapshotCP4(report, c.getString(2));
                lastAttemptAt = c.getLong(3);
                lastAttemptDetail = safeSnapshotCP4(report, c.getString(4));
            }
            Collections.sort(elapsed);
            long p95 =
                    elapsed.size() == 0
                            ? -1L
                            : elapsed.get(Math.max(0, (int) Math.ceil(elapsed.size() * 0.95d) - 1))
                                    .longValue();
            LifetimeRuntime lifetime = readLifetimeRuntimeSnapshotCP4(report, db);
            return new RuntimeSummary(
                    elapsed.isEmpty() ? "NO_MEASURED_DATA" : "OK",
                    elapsed.size(),
                    p95,
                    maximum,
                    oom,
                    last,
                    target,
                    lastAt,
                    lastAttemptType,
                    lastAttemptStatus,
                    lastAttemptTarget,
                    lastAttemptAt,
                    lastAttemptDetail,
                    epochAt,
                    lifetime.eventCount,
                    lifetime.p95Ms,
                    lifetime.maxConcurrency,
                    lifetime.oomCount,
                    "");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static LifetimeRuntime readLifetimeRuntimeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        Cursor c = null;
        try {
            ArrayList<Long> elapsed = new ArrayList<Long>();
            c =
                    report.select(
                            "SELECT elapsed_ms,status,detail FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE event_type='" + RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923 + "'"
                                    + " ORDER BY event_at DESC,_id DESC LIMIT 200",
                            null);
            while (c.moveToNext()) if (RuntimeHealthLedgerR36.measuredDurableR80(c.getLong(0), c.getString(1), c.getString(2)))
                elapsed.add(Long.valueOf(c.getLong(0)));
            c.close();
            c = null;
            Collections.sort(elapsed);
            long p95 =
                    elapsed.size() == 0
                            ? -1L
                            : elapsed.get(Math.max(0, (int) Math.ceil(elapsed.size() * 0.95d) - 1))
                                    .longValue();
            c =
                    report.select(
                            "SELECT COALESCE(SUM(CASE WHEN oom_count>0 OR status='OOM'"
                                    + " OR detail LIKE '%OutOfMemoryError%' THEN 1 ELSE 0 END),0),"
                                    + "COALESCE(MAX(max_concurrency),0) FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH,
                            null);
            int oom = 0, maximum = 0;
            if (c.moveToFirst()) {
                oom = c.getInt(0);
                maximum = c.getInt(1);
            }
            return new LifetimeRuntime(elapsed.size(), p95, maximum, oom);
        } finally {
            if (c != null) c.close();
        }
    }
}
