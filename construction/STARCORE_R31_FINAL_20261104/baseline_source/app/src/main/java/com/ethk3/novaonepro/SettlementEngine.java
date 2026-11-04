package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SettlementEngine {
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int OPEN_TRADE_SCAN_LIMIT = 260;
    private static final int RECENT_DRAW_SCAN_LIMIT = 500;
    private static final int MAX_SETTLE_PER_ROUND = 120;
    private static final int MAX_DETAIL_LEN = 2200;
    private static final int DECISION_REPAIR_SCAN_LIMIT = 260;
    private static final String SETTLEMENT_SPEED_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String REALTIME_VALID_DOMAIN_SETTLEMENT_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String LINK_MODIFICATION_V2_SETTLEMENT_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final String MISS_TREND_BROKEN = "MISS_TREND_BROKEN";
    private static final String MISS_DRIFT_SPIKE = "MISS_DRIFT_SPIKE";
    private static final String MISS_HOT_REVERSE = "MISS_HOT_REVERSE";
    private static final String MISS_FALSE_RECOVERY = "MISS_FALSE_RECOVERY";
    private static final String MISS_SAMPLE_WEAK = "MISS_SAMPLE_WEAK";
    private static final String MISS_THIRD_WEAK = "MISS_THIRD_WEAK";
    private static final String MISS_POSITION_LOST = "MISS_POSITION_LOST";
    private static final String MISS_MARKOV_FAILED = "MISS_MARKOV_FAILED";
    private static final String MISS_K3_RANK_4_6 = "MISS_K3_RANK_4_6";

    private final DataGuard dataGuard;

    /** Capability token: only SettlementEngine can construct one. */
    static final class OwnerToken {
        private final String targetIssue, nonce;
        private final long issuedAt;

        private OwnerToken(String target, long at) {
            targetIssue = safe(target);
            issuedAt = at;
            nonce =
                    V51ChecksumEngine.sha256(
                            "R36_SETTLEMENT_OWNER|"
                                    + targetIssue
                                    + '|'
                                    + at
                                    + '|'
                                    + System.identityHashCode(this));
        }

        private boolean validFor(String target) {
            return issuedAt > 0L && targetIssue.equals(safe(target)) && nonce.length() == 64;
        }
    }

    static boolean isOwnerTokenR36(OwnerToken token, String target) {
        return token != null && token.validFor(target);
    }

    // R30_REVOCATION_BEGIN
    /** Existing draw writers call this only inside their own source-observation transaction. */
    public static void revokeCertificatesForDrawWithinR30(SQLiteDatabase db,String issue,String cause,String eventKey) {
        com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.revokeWithin(db,issue,cause,eventKey);
    }
    // R30_REVOCATION_END

    public SettlementEngine() {
        dataGuard = new DataGuard();
    }

    public SettlementReport settleLatest(DbStore store, Models.Draw latestDraw) {
        return SettlementCoordinatorR815.settleNow(
                store, latestDraw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public SettlementReport settleAfterDrawSaved(DbStore store, Models.Draw latestDraw) {
        return SettlementCoordinatorR815.settleNow(
                store, latestDraw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public SettlementReport settleAfterFetch(DbStore store, Models.Draw latestDraw) {
        return SettlementCoordinatorR815.settleNow(
                store, latestDraw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public SettlementReport fastSettle(DbStore store, Models.Draw latestDraw) {
        return SettlementCoordinatorR815.settleNow(
                store, latestDraw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public SettlementReport settleFastByLatestDraw(DbStore store, Models.Draw latestDraw) {
        return SettlementCoordinatorR815.settleNow(
                store, latestDraw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    /**
     * Exact-target, one-owner R3.6 settlement. OOS, legacy REAL (if any), task terminal and health
     * terminal commit together.
     */
    SettlementReport executeFromCoordinatorR815(final DbStore store, final Models.Draw draw) {
        final long started = System.currentTimeMillis();
        SettlementReport invalid = new SettlementReport();
        invalid.issue = draw == null ? "" : safe(draw.issue);
        invalid.createdAt = started;
        if (!SettlementCoordinatorR815.isCoordinatorExecutionActive()) {
            invalid.dataErrorCount = 1;
            invalid.detail = "R815_SETTLEMENT_COORDINATOR_REQUIRED";
            return invalid;
        }
        if (store == null || draw == null || !draw.isValid()) {
            invalid.ok = 0;
            invalid.dataErrorCount = 1;
            invalid.detail = "R36_SETTLEMENT_INVALID_INPUT";
            return invalid;
        }
        RealtimePressureGateR811.beginSettlement();
        try {
        AutoFetchService.b13RecordBusinessBeat(store, "SETTLEMENT_REQUESTED", draw.issue);
        SettlementTaskStoreR36.Terminal old = SettlementTaskStoreR36.terminal(store, draw.issue);
        if (old != null) return reportFromTerminalR36(old, draw);
        final String owner = "SettlementEngine-R36-" + Thread.currentThread().getId();
        SettlementTaskStoreR36.TaskClaim acquired =
                SettlementTaskStoreR36.normalizeAndClaimCurrentR915(
                        store,
                        draw.issue,
                        owner,
                        started,
                        "EXACT_CURRENT_UNDER_SOLE_GATE");
        if (acquired == null) {
            SettlementTaskStoreR36.Terminal terminal =
                    SettlementTaskStoreR36.terminal(store, draw.issue);
            if (terminal != null) return reportFromTerminalR36(terminal, draw);
            invalid.ok = 1;
            invalid.waitingCount = 1;
            invalid.detail = "R915_SETTLEMENT_ATOMIC_CLAIM_NOT_ACQUIRED";
            return invalid;
        }
        final SettlementTaskStoreR36.TaskClaim claim = acquired;
        try {
            if (!claim.drawHash.equals(DrawHashR36.sha256V2(draw)))
                throw new IllegalStateException("R36_SETTLEMENT_DRAW_HASH_MISMATCH");
            final OwnerToken token = new OwnerToken(draw.issue, System.currentTimeMillis());
            DatabaseCoordinator.TransactionResult<SettlementReport> out =
                    store.coordinatorR36()
                            .runSettlementTransactionR36(
                                    draw.issue,
                                    claim.drawHash,
                                    new DatabaseCoordinator.TransactionCallable<
                                            SettlementReport>() {
                                        @Override
                                        public SettlementReport call(
                                                SQLiteDatabase db,
                                                DatabaseCoordinator.TransactionContext tx) {
                                            OosAfterDrawWriterR36.LabelResult oos =
                                                    OosAfterDrawWriterR36
                                                            .settleFrozenWithinTransaction(
                                                                    db, token, draw);
                                            RealSettlementR36 real =
                                                    settleRealWithinTransactionR36(db, token, draw);
                                            long now = System.currentTimeMillis();
                                            String terminalStatus =
                                                    oos.success
                                                            ? SettlementTaskStoreR36.COMPLETED
                                                            : ("FROZEN_OBSERVATION_MISSING"
                                                                            .equals(oos.status)
                                                                    ? SettlementTaskStoreR36
                                                                            .NO_PREDRAW_CANONICAL_TERMINAL
                                                                    : SettlementTaskStoreR36
                                                                            .FAILED_TERMINAL);
                                            String settlementHash =
                                                    V51ChecksumEngine.sha256(
                                                            "R36_SETTLEMENT|"
                                                                    + claim.drawHash
                                                                    + '|'
                                                                    + oos.recordHash
                                                                    + '|'
                                                                    + real.recordHash
                                                                    + '|'
                                                                    + terminalStatus);
                                            /*
                                             * R909 CORE-ISOLATION:
                                             * The production settlement transaction contains ONLY
                                             * the immutable OOS/REAL result and settlement task terminal.
                                             * IssueClosure, Research and Deferred outbox rows are derivative
                                             * sidecars and are reconciled after the core commit.  They are
                                             * forbidden from rolling back a valid exact settlement.
                                             */
                                            SettlementTaskStoreR36.markTerminalWithinTransaction(
                                                    db,
                                                    claim,
                                                    terminalStatus,
                                                    settlementHash,
                                                    oos.status,
                                                    oos.success ? "" : oos.summary(),
                                                    now);
                                            RuntimeEventHooksR926.settledWithinTransaction(
                                                    db, draw.issue, settlementHash, terminalStatus, claim.drawHash);
                                            com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.feedbackWithinTransaction(db, draw);
                                            SettlementReport report = new SettlementReport();
                                            report.ok = oos.success ? 1 : 0;
                                            report.issue = draw.issue;
                                            report.createdAt = now;
                                            report.shadowSettledCount =
                                                    oos.success && !oos.replay ? 1 : 0;
                                            report.shadowHitCount =
                                                    oos.success && !oos.replay && oos.hitLabel == 1
                                                            ? 1
                                                            : 0;
                                            report.shadowMissCount =
                                                    oos.success && !oos.replay && oos.hitLabel == 0
                                                            ? 1
                                                            : 0;
                                            report.openCount = real.found && !real.replay ? 1 : 0;
                                            report.settledCount =
                                                    real.settled && !real.replay ? 1 : 0;
                                            report.winCount =
                                                    real.settled && real.hit && !real.replay
                                                            ? 1
                                                            : 0;
                                            report.missCount =
                                                    real.settled && !real.hit && !real.replay
                                                            ? 1
                                                            : 0;
                                            report.actualNumber = real.actual;
                                            report.result =
                                                    real.settled
                                                            ? (real.hit
                                                                    ? AppConfig.RESULT_WIN
                                                                    : AppConfig.RESULT_MISS)
                                                            : "";
                                            report.pnl = real.pnl;
                                            report.detail =
                                                    "R36_EXACT_TERMINAL|task="
                                                            + terminalStatus
                                                            + "|oos="
                                                            + oos.status
                                                            + "|real="
                                                            + real.status
                                                            + "|core=COMMITTED_SIDEcars_DEFERRED"
                                                            + "|hash="
                                                            + settlementHash;
                                            return report;
                                        }
                                    },
                                    null);
            if (out.success && out.value != null) {
                recordPostCommitTelemetryR911(store, draw, out.value, started);
                return out.value;
            }
            if (out.success && out.replay) {
                SettlementTaskStoreR36.Terminal terminal =
                        SettlementTaskStoreR36.terminal(store, draw.issue);
                if (terminal != null) return reportFromTerminalR36(terminal, draw);
            }
            SettlementTaskStoreR36.markRetry(store, claim, out.code);
            invalid.ok = 0;
            invalid.detail = "R36_SETTLEMENT_TX_FAILED:" + out.code;
            return invalid;
        } catch (OutOfMemoryError oom) {
            RuntimeMemoryGuardR811.recoverAfterFailure();
            try {
                store.setState(
                        "r811_settlement_memory_recovery",
                        RuntimeMemoryGuardR811.snapshot("SETTLEMENT_OOM_RECOVERED").canonical());
                store.setState(
                        "r811_settlement_memory_recovery_at",
                        String.valueOf(System.currentTimeMillis()));
            } catch (Throwable stateFailure) {
                try { RuntimeEventHooksR926.recovery(store, draw.issue, "SETTLEMENT_OOM_DIAGNOSTIC_FAILED",
                        DatabaseCoordinator.describeCauseChain(stateFailure, 240)); }
                catch (Throwable eventFailure) {
                    invalid.detail = "R36_SETTLEMENT_OOM_DIAGNOSTIC_FAILED:"
                            + eventFailure.getClass().getSimpleName();
                }
            }
            SettlementTaskStoreR36.markRetry(store, claim, "OOM_CAUGHT");
            invalid.ok = 0;
            String secondaryOomDiagnostic = safe(invalid.detail);
            invalid.detail = "R36_SETTLEMENT_OOM_CAUGHT"
                    + (secondaryOomDiagnostic.length() == 0 ? "" : "|" + secondaryOomDiagnostic);
            return invalid;
        } catch (Throwable e) {
            String cause = DatabaseCoordinator.describeCauseChain(e, 1800);
            SettlementTaskStoreR36.markRetry(store, claim, cause);
            invalid.ok = 0;
            invalid.detail = "R36_SETTLEMENT_ERROR:" + cause;
            return invalid;
        }
        } finally {
            RealtimePressureGateR811.endSettlement();
        }
    }

    private static void recordPostCommitTelemetryR911(
            DbStore store, Models.Draw draw, SettlementReport report, long started) {
        if (store == null || draw == null || report == null) return;
        long now = System.currentTimeMillis();
        try {
            SettlementTaskStoreR36.Terminal terminal =
                    SettlementTaskStoreR36.terminal(store, draw.issue);
            String status = terminal == null ? "CORE_COMMITTED" : terminal.status;
            RuntimeHealthLedgerR36.record(
                    store,
                    "SETTLEMENT_TERMINAL",
                    "SET-" + DrawHashR36.sha256V2(draw).substring(0, 16),
                    draw.issue,
                    status,
                    started,
                    now,
                    Math.max(0L, now - started),
                    0,
                    1,
                    true,
                    "R911_CORE_V2|MODEL_CALLS=0");
            java.util.HashMap<String,String> states = new java.util.HashMap<String,String>();
            states.put("r36_settlement_status", status);
            states.put("r36_settlement_target", draw.issue);
            states.put("r36_settlement_model_calls", "0");
            states.put("r911_settlement_core_commit_at", String.valueOf(now));
            store.setStates(states);
        } catch (Throwable telemetryFailure) {
            try {
                store.setState(
                        "r911_settlement_postcommit_telemetry_error",
                        telemetryFailure.getClass().getSimpleName() + ':' + safe(telemetryFailure.getMessage()));
            } catch (Throwable ignore) {}
        }
    }

    public int drainSettlementTasksR36(DbStore store, int maxTasks) {
        return SettlementCoordinatorR815.drainBacklog(store, maxTasks);
    }

    private static RealSettlementR36 settleRealWithinTransactionR36(
            SQLiteDatabase db, OwnerToken token, Models.Draw draw) {
        if (!isOwnerTokenR36(token, draw.issue))
            throw new SecurityException("R36_REAL_SETTLEMENT_OWNER_REQUIRED");
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT"
                                + " _id,action,stat_scope,position,n1,n2,n3,k3_mask,settled,actual_number,hit,result,pnl"
                                + " FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE target_issue=? LIMIT 2",
                            new String[] {draw.issue});
            if (!c.moveToFirst()) return RealSettlementR36.none();
            long id = c.getLong(0);
            String action = AppConfig.normalizeDecision(c.getString(1));
            String scope = AppConfig.normalizeStatScope(c.getString(2));
            int position = c.getInt(3), n1 = c.getInt(4), n2 = c.getInt(5), n3 = c.getInt(6);
            int mask = c.getInt(7),
                    settled = c.getInt(8),
                    oldActual = c.getInt(9),
                    oldHit = c.getInt(10);
            String oldResult = safe(c.getString(11));
            double oldPnl = c.getDouble(12);
            if (c.moveToNext()) throw new IllegalStateException("R36_MULTIPLE_REAL_ROWS_FOR_ISSUE");
            if (!AppConfig.DECISION_BET.equals(action)
                    || !AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope))
                return RealSettlementR36.ignored();
            if (!AppConfig.isValidPosition(position)
                    || !DecisionGateEngine.b12K3(n1, n2, n3)
                    || (mask > 0 && mask != AppConfig.buildK3Mask(n1, n2, n3)))
                throw new IllegalStateException("R36_REAL_TRADE_K3_INVALID");
            int actual = draw.getNumberAtPosition(position);
            boolean hit = actual == n1 || actual == n2 || actual == n3;
            double pnl = hit ? AppConfig.WIN_PNL_K3 : AppConfig.MISS_PNL_K3;
            if (settled == 1) {
                if (oldActual != actual || oldHit != (hit ? 1 : 0))
                    throw new IllegalStateException("R36_REAL_SETTLEMENT_CONFLICT");
                return new RealSettlementR36(
                        true,
                        true,
                        true,
                        hit,
                        actual,
                        oldPnl,
                        "REAL_REPLAY",
                        V51ChecksumEngine.sha256(
                                "R36_REAL_REPLAY|" + id + '|' + oldResult + '|' + oldActual));
            }
            long now = System.currentTimeMillis();
            ContentValues cv = new ContentValues();
            cv.put("settled", 1);
            cv.put("actual_number", actual);
            cv.put("hit", hit ? 1 : 0);
            cv.put("result", hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
            cv.put("pnl", pnl);
            cv.put("settle_time", now);
            cv.put("settled_at", now);
            cv.put("action", AppConfig.DECISION_BET);
            cv.put("stat_scope", AppConfig.STAT_SCOPE_REAL_TRADE);
            cv.put("miss_reason", hit ? "" : "预测位置实际号码未落入K3组合");
            cv.put(
                    "settlement_reason",
                    "R36严格单位置K3精确结算|target="
                            + draw.issue
                            + "|position="
                            + position
                            + "|actual="
                            + actual);
            int n =
                    db.update(
                            DbHelper.TABLE_TRADES,
                            cv,
                            "_id=? AND settled=0",
                            new String[] {String.valueOf(id)});
            if (n != 1) throw new IllegalStateException("R36_REAL_UPDATE_CARDINALITY_" + n);
            ContentValues decision = new ContentValues();
            decision.put("settled", 1);
            decision.put("actual_number", actual);
            decision.put("hit", hit ? 1 : 0);
            decision.put("result", hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
            decision.put("pnl", pnl);
            decision.put("settle_time", now);
            decision.put("settled_at", now);
            db.update(
                    DbHelper.TABLE_DECISIONS,
                    decision,
                    "target_issue=? AND decision=? AND stat_scope=?",
                    new String[] {
                        draw.issue, AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                    });
            String hash =
                    V51ChecksumEngine.sha256(
                            "R36_REAL|"
                                    + id
                                    + '|'
                                    + DrawHashR36.sha256V2(draw)
                                    + '|'
                                    + actual
                                    + '|'
                                    + (hit ? 1 : 0));
            return new RealSettlementR36(true, true, false, hit, actual, pnl, "REAL_SETTLED", hash);
        } finally {
            if (c != null) c.close();
        }
    }

    private static SettlementReport reportFromTerminalR36(
            SettlementTaskStoreR36.Terminal terminal, Models.Draw draw) {
        SettlementReport r = new SettlementReport();
        r.ok = SettlementTaskStoreR36.COMPLETED.equals(terminal.status) ? 1 : 0;
        r.issue = draw == null ? terminal.targetIssue : draw.issue;
        r.createdAt = terminal.completedAt;
        r.detail =
                "R36_SETTLEMENT_REPLAY|task="
                        + terminal.status
                        + "|hash="
                        + terminal.settlementHash
                        + (terminal.error.length() > 0 ? "|error=" + terminal.error : "");
        return r;
    }

    private static final class RealSettlementR36 {
        final boolean found, settled, replay, hit;
        final int actual;
        final double pnl;
        final String status, recordHash;

        RealSettlementR36(
                boolean found,
                boolean settled,
                boolean replay,
                boolean hit,
                int actual,
                double pnl,
                String status,
                String hash) {
            this.found = found;
            this.settled = settled;
            this.replay = replay;
            this.hit = hit;
            this.actual = actual;
            this.pnl = pnl;
            this.status = safe(status);
            recordHash = safe(hash);
        }

        static RealSettlementR36 none() {
            return new RealSettlementR36(false, false, false, false, 0, 0.0d, "NO_REAL_TRADE", "");
        }

        static RealSettlementR36 ignored() {
            return new RealSettlementR36(
                    true, false, true, false, 0, 0.0d, "NON_REAL_ROW_IGNORED", "");
        }
    }

    public SettlementReport settleAllPending(DbStore store) {
        SettlementReport report = new SettlementReport();

        if (store == null) {
            report.ok = 0;
            report.detail = "批量结算失败：DbStore为空";
            return report;
        }

        List<Models.Draw> recent = getRecentDrawsSafe(store, RECENT_DRAW_SCAN_LIMIT);
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(recent);

        if (clean == null || clean.size() == 0) {
            report.ok = 0;
            report.detail = "批量结算失败：没有有效开奖";
            saveSettlementState(store, report);
            return report;
        }

        report = settleRecentDrawsInternal(store, clean, "");
        saveSettlementState(store, report);

        return report;
    }

    public SettlementReport settleRecentDraws(DbStore store, List<Models.Draw> recentDraws) {
        SettlementReport report = new SettlementReport();

        if (store == null) {
            report.ok = 0;
            report.detail = "批量结算失败：DbStore为空";
            return report;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(recentDraws);

        if (clean == null || clean.size() == 0) {
            report.ok = 0;
            report.detail = "批量结算失败：没有有效开奖";
            saveSettlementState(store, report);
            return report;
        }

        report = settleRecentDrawsInternal(store, clean, "");
        saveSettlementState(store, report);

        return report;
    }

    public SettlementReport settleByTargetIssue(
            DbStore store, String targetIssue, Models.Draw actualDraw) {
        SettlementReport report = new SettlementReport();

        if (store == null) {
            report.ok = 0;
            report.detail = "结算失败：DbStore为空";
            return report;
        }

        if (targetIssue == null || targetIssue.length() == 0) {
            report.ok = 0;
            report.detail = "结算失败：目标期为空";
            saveSettlementState(store, report);
            return report;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            report.ok = 0;
            report.detail = "结算失败：实际开奖非法";
            saveSettlementState(store, report);
            return report;
        }

        report.issue = safe(actualDraw.issue);
        report.createdAt = System.currentTimeMillis();

        if (!sameIssue(targetIssue, actualDraw.issue)) {
            report.ok = 1;
            report.waitingCount = 1;
            report.detail = "目标期未开奖，等待：" + safe(targetIssue) + "，当前开奖：" + safe(actualDraw.issue);
            saveSettlementState(store, report);
            return report;
        }

        Models.Trade trade = safeGetTradeByTargetIssue(store, targetIssue);

        if (trade == null) {
            ShadowSettlementSummary shadowSummary =
                    settleShadowCandidatesForDraw(store, actualDraw);
            report.ok = 1;
            report.shadowSettledCount += shadowSummary.settledCount;
            report.shadowHitCount += shadowSummary.hitCount;
            report.shadowMissCount += shadowSummary.missCount;
            report.detail =
                    "目标期没有真实BET交易："
                            + targetIssue
                            + "；真实账本不变；SHADOW反事实结算="
                            + shadowSummary.settledCount
                            + "（中="
                            + shadowSummary.hitCount
                            + "，挂="
                            + shadowSummary.missCount
                            + "）";
            saveSettlementState(store, report);
            return report;
        }

        report.openCount = trade.settled == 0 ? 1 : 0;

        SettlementResult r = settleOneInternal(store, trade, actualDraw, false);

        if (r == null) {
            report.ok = 0;
            report.detail = "目标期交易无法结算";
            saveSettlementState(store, report);
            return report;
        }

        if (!r.canSettle) {
            report.ok = 1;
            report.waitingCount = 1;
            report.detail = safeDefault(r.detail, "暂不能结算");
            saveSettlementState(store, report);
            return report;
        }

        report.ok = 1;
        applyResultToReport(report, r);
        report.detail = r.detail;
        ShadowSettlementSummary shadowSummary = settleShadowCandidatesForDraw(store, actualDraw);
        report.shadowSettledCount += shadowSummary.settledCount;
        report.shadowHitCount += shadowSummary.hitCount;
        report.shadowMissCount += shadowSummary.missCount;
        report.detail =
                appendText(
                        report.detail,
                        "SHADOW反事实="
                                + shadowSummary.settledCount
                                + "（中="
                                + shadowSummary.hitCount
                                + "，挂="
                                + shadowSummary.missCount
                                + "）");
        saveSettlementState(store, report);
        return report;
    }

    private SettlementReport settleRecentDrawsInternal(
            DbStore store, List<Models.Draw> recentDraws, String latestIssue) {
        SettlementReport report = new SettlementReport();
        report.ok = 1;
        report.createdAt = System.currentTimeMillis();
        report.issue = safe(latestIssue);

        if (store == null) {
            report.ok = 0;
            report.detail = "结算失败：DbStore为空";
            return report;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(recentDraws);

        if (clean == null || clean.size() == 0) {
            report.ok = 0;
            report.detail = "结算失败：没有有效开奖";
            return report;
        }

        if (report.issue.length() == 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            if (latest != null) {
                report.issue = safe(latest.issue);
            }
        }

        HashMap<String, Models.Draw> drawMap = buildDrawMap(clean);
        StringBuilder detail = new StringBuilder();
        int repaired = repairMissingTradesFromRecentBetDecisions(store, drawMap, detail);
        report.createdTradeCount += repaired;

        List<Models.Trade> openTrades = getOpenTradesSafe(store);
        report.openCount = openTrades == null ? 0 : openTrades.size();

        int settledThisRound = 0;

        if (openTrades != null && openTrades.size() > 0) {
            for (int i = 0; i < openTrades.size(); i++) {
                if (settledThisRound >= MAX_SETTLE_PER_ROUND) {
                    report.waitingCount += openTrades.size() - i;
                    appendDetail(detail, "本轮已结算" + settledThisRound + "条，剩余下轮继续");
                    break;
                }

                Models.Trade trade = openTrades.get(i);

                if (trade == null) {
                    continue;
                }

                if (trade.settled == 1) {
                    continue;
                }

                if (!isRealBetTradeScopeAndAction(trade)) {
                    continue;
                }

                if (trade.targetIssue == null || trade.targetIssue.length() == 0) {
                    report.waitingCount++;
                    appendDetail(detail, "真实BET交易目标期为空，跳过");
                    continue;
                }

                Models.Draw matched = findDrawByIssueMap(drawMap, trade.targetIssue);

                if (matched == null) {
                    matched = safeGetDrawByIssue(store, trade.targetIssue);
                }

                if (matched == null || !matched.isValid()) {
                    report.waitingCount++;
                    appendDetail(detail, "等待目标期开奖：" + safe(trade.targetIssue));
                    continue;
                }

                SettlementResult r = settleOneInternal(store, trade, matched, false);

                if (r == null) {
                    report.waitingCount++;
                    appendDetail(detail, "无法结算：" + safe(trade.targetIssue));
                    continue;
                }

                if (!r.canSettle) {
                    report.waitingCount++;
                    appendDetail(detail, safeDefault(r.detail, "暂不能结算"));
                    continue;
                }

                settledThisRound++;
                report.issue = safe(matched.issue);
                applyResultToReport(report, r);
                appendDetail(detail, r.detail);
            }
        }

        for (int si = 0; si < clean.size(); si++) {
            Models.Draw shadowDraw = clean.get(si);
            if (shadowDraw == null || !shadowDraw.isValid()) continue;
            ShadowSettlementSummary shadowSummary =
                    settleShadowCandidatesForDraw(store, shadowDraw);
            report.shadowSettledCount += shadowSummary.settledCount;
            report.shadowHitCount += shadowSummary.hitCount;
            report.shadowMissCount += shadowSummary.missCount;
        }

        if (detail.length() == 0) {
            if (report.waitingCount > 0) {
                report.detail = "结算检查完成：还有 " + report.waitingCount + " 条真实BET等待目标期开奖";
            } else if (report.openCount <= 0) {
                report.detail = "结算完成：没有等待结算的真实BET；SHADOW/HOLD不进真实结算";
            } else {
                report.detail = "结算检查完成：没有匹配到可结算的真实BET";
            }
        } else {
            report.detail = trimDetail(detail.toString());
        }

        return report;
    }

    public SettlementResult settleOne(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        if (store == null) {
            SettlementResult r = new SettlementResult();
            r.canSettle = false;
            r.detail = "DbStore为空，无法结算";
            return r;
        }

        return settleOneInternal(store, trade, actualDraw, false);
    }

    private SettlementResult settleOneAndLearn(
            DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        return settleOneInternal(store, trade, actualDraw, false);
    }

    private SettlementResult settleOneInternal(
            DbStore store,
            Models.Trade trade,
            Models.Draw actualDraw,
            boolean allowAlreadySettledRepair) {
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "SettlementEngine", MODULE_BUILD_VERSION, "settleOneInternal");
        } catch (Throwable ignore) {
        }
        SettlementResult r =
                buildStrictSettlementResult(trade, actualDraw, allowAlreadySettledRepair);

        if (r == null || !r.canSettle || r.trade == null) {
            return r;
        }

        int updated = safeSettleTrade(store, r.trade);
        r.dbUpdated = updated;

        if (updated <= 0) {
            appendSettlementResultDetail(r, "数据库写回失败或返回0，学习链路不触发");
        } else {
            syncDecisionSettlementStateSafe(store, r.trade);
            saveCleanLearningSampleState(store, r.trade);

            if (shouldTriggerLearningAfterSettlement(r.trade)) {
                LearningEngine.LearningReport lr =
                        runPostSettlementLearning(store, r.trade, actualDraw);

                if (lr != null) {
                    r.teacherMatrixDrawCount = lr.teacherMatrixDrawCount;
                    r.teacherMatrixCellCount = lr.teacherMatrixCellCount;
                    r.learningSummary = lr.detail == null ? "" : lr.detail;

                    if (r.learningSummary.length() > 0) {
                        appendSettlementResultDetail(r, "学习已触发：" + r.learningSummary);
                    }
                } else {
                    appendSettlementResultDetail(r, "学习触发失败：LearningReport为空");
                }
            } else {
                appendSettlementResultDetail(r, "本结果不是干净真实WIN/MISS样本，不触发真实学习");
            }
        }

        /* REAL BET结算同步更新独立已结算域证据；不改变结算结果和盈亏。 */
        try {
            if (r != null && r.trade != null && r.trade.settled == 1)
                updateRealDomainEvidenceV17(store, r.trade);
        } catch (Throwable ignore) {
        }

        /* SHADOW独立反事实结算，不进入真实盈亏/真实胜率。 */
        settleShadowCandidatesForDraw(store, actualDraw);
        markShadowLearnedSafe(store, r.trade.targetIssue);

        return r;
    }

    private ShadowSettlementSummary settleShadowCandidatesForDraw(DbStore store, Models.Draw draw) {
        ShadowSettlementSummary summary = new ShadowSettlementSummary();
        if (store == null
                || draw == null
                || !draw.isValid()
                || draw.issue == null
                || draw.issue.length() == 0) return summary;
        List<Models.ComboCandidate> shadows;
        try {
            shadows =
                    store.getShadowCandidates(
                            draw.issue, AppConfig.REALTIME_PROBABILITY_AUDIT_LIMIT);
        } catch (Throwable e) {
            shadows = new ArrayList<Models.ComboCandidate>();
        }
        if (shadows == null || shadows.size() == 0) return summary;
        for (int i = 0; i < shadows.size(); i++) {
            Models.ComboCandidate c = shadows.get(i);
            if (c == null || !c.isValidK3() || !sameIssue(c.targetIssue, draw.issue)) continue;
            int actual = draw.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(actual)) continue;
            String eventKey = buildShadowSettlementEventKey(c, draw.issue);
            String doneKey = "shadow_settle_" + Math.abs(eventKey.hashCode());
            try {
                if ("1".equals(store.getState(doneKey, "0"))) continue;
            } catch (Throwable e) {
            }
            boolean hit = actual == c.n1 || actual == c.n2 || actual == c.n3;
            try {
                store.normalizeClosedLoopEvidenceAliases(c);
            } catch (Throwable ignore) {
            }
            if (c.evidenceDomainFrozen != 1
                    || c.promotionFineDomain == null
                    || c.promotionFineDomain.length() == 0) {
                try {
                    store.setState("shadow_domain_missing_at_settlement_issue", draw.issue);
                    store.setState("shadow_domain_missing_at_settlement", "1");
                } catch (Throwable ignore) {
                }
                continue;
            }
            String[] frozenDomains =
                    new String[] {
                        AppConfig.sanitizePromotionDomainKey(c.promotionCoarseDomain),
                        AppConfig.sanitizePromotionDomainKey(c.promotionMidDomain),
                        AppConfig.sanitizePromotionDomainKey(c.promotionFineDomain)
                    };
            String domain = frozenDomains[2];
            if (domain.length() == 0 || AppConfig.isUnsafePromotionDomainKey(domain)) continue;
            c.shadowPromotionDomain = domain;
            c.promotionDomainAvailable = 1;
            updateShadowDomainStat(store, domain, c, draw.issue, hit);
            String parentDomain = frozenDomains[1];
            if (parentDomain.length() > 0 && !parentDomain.equals(domain)) {
                updateShadowDomainStat(store, parentDomain, c, draw.issue, hit);
            }
            String coarseDomain = frozenDomains[0];
            if (coarseDomain.length() > 0
                    && !coarseDomain.equals(domain)
                    && !coarseDomain.equals(parentDomain)) {
                updateShadowDomainStat(store, coarseDomain, c, draw.issue, hit);
            }
            try {
                Models.ShadowDomainStat exact = null;
                List<Models.ShadowDomainStat> exactList =
                        store.getShadowPromotionDomainStats(domain, 1);
                if (exactList != null
                        && exactList.size() > 0
                        && domain.equals(exactList.get(0).domainKey)) exact = exactList.get(0);
                store.updateShadowSettlementEvidence(
                        draw.issue,
                        c.position,
                        c.k3Mask,
                        actual,
                        hit,
                        domain,
                        exact == null ? AppConfig.PROMOTION_WATCH : exact.promotionStatus);
            } catch (Throwable ignore) {
            }
            try {
                store.setState(doneKey, "1");
                store.setState("last_shadow_settlement_event", eventKey);
                store.setState("last_shadow_settlement_issue", draw.issue);
                store.setState("last_shadow_settlement_domain", domain);
                store.setState("last_shadow_settlement_hit", hit ? "1" : "0");
            } catch (Throwable e) {
            }
            summary.settledCount++;
            if (hit) summary.hitCount++;
            else summary.missCount++;
        }
        if (summary.settledCount > 0) {
            try {
                store.markShadowLearned(draw.issue);
            } catch (Throwable e) {
            }
        }
        return summary;
    }

    private String buildShadowSettlementEventKey(Models.ComboCandidate c, String issue) {
        return "SHADOW_SETTLE_" + safe(issue) + "_P" + c.position + "_K" + c.k3Mask;
    }

    private String buildShadowPromotionDomain(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3() || c.evidenceDomainFrozen != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(c.promotionFineDomain);
    }

    private String buildCoarseShadowDomain(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3() || c.evidenceDomainFrozen != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(c.promotionCoarseDomain);
    }

    private String buildMidShadowDomain(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3() || c.evidenceDomainFrozen != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(c.promotionMidDomain);
    }

    private void updateShadowDomainStat(
            DbStore store, String domain, Models.ComboCandidate c, String issue, boolean hit) {
        if (store == null || domain == null || domain.length() == 0) return;
        Models.ShadowDomainStat stat = null;
        try {
            List<Models.ShadowDomainStat> list = store.getShadowPromotionDomainStats(domain, 1);
            if (list != null && list.size() > 0 && domain.equals(list.get(0).domainKey))
                stat = list.get(0);
        } catch (Throwable e) {
            stat = null;
        }
        if (stat == null) stat = new Models.ShadowDomainStat();
        stat.domainKey = domain;
        stat.parentDomainKey = buildParentShadowDomain(domain);
        stat.sampleCount++;
        if (hit) {
            stat.hitCount++;
            stat.misleadingStreak = 0;
        } else {
            stat.missCount++;
            stat.misleadingStreak++;
        }
        stat.recentSampleCount =
                Math.min(AppConfig.FREQ_LOOKBACK_SHORT, stat.recentSampleCount + 1);
        if (hit) stat.recentHitCount = Math.min(stat.recentSampleCount, stat.recentHitCount + 1);
        else if (stat.recentSampleCount >= AppConfig.FREQ_LOOKBACK_SHORT && stat.recentHitCount > 0)
            stat.recentHitCount--;
        stat.hitRate =
                stat.sampleCount <= 0
                        ? 0.0d
                        : ((double) stat.hitCount) / ((double) stat.sampleCount);
        stat.recentHitRate =
                stat.recentSampleCount <= 0
                        ? 0.0d
                        : ((double) stat.recentHitCount) / ((double) stat.recentSampleCount);
        /* K3随机基准0.30作为收缩先验：3中7挂。 */
        stat.posteriorMean = ((double) stat.hitCount + 3.0d) / ((double) stat.sampleCount + 10.0d);
        stat.wilsonLowerBound = Models.calcWilsonLowerBound(stat.hitCount, stat.sampleCount);
        stat.driftScore = Math.abs(stat.hitRate - stat.recentHitRate);
        stat.promotionStatus = shadowPromotionStatus(stat);
        stat.lastSettledIssue = issue == null ? "" : issue;
        stat.updatedAt = System.currentTimeMillis();
        stat.version = LINK_MODIFICATION_V2_SETTLEMENT_VERSION;
        try {
            store.saveShadowDomainStat(stat);
        } catch (Throwable e) {
        }
    }

    private String shadowPromotionStatus(Models.ShadowDomainStat s) {
        if (s == null || s.sampleCount <= 0) return AppConfig.PROMOTION_NO_SAMPLE;
        if (s.sampleCount < 8)
            return s.hitRate >= 0.40d
                    ? AppConfig.PROMOTION_WATCH_POSITIVE
                    : AppConfig.PROMOTION_WATCH;
        if (s.sampleCount >= AppConfig.PROMOTE_BLOCK_MIN_SAMPLE
                && s.posteriorMean <= AppConfig.PROMOTE_BLOCK_MAX_POSTERIOR)
            return AppConfig.PROMOTION_BLOCK;
        if (s.sampleCount >= AppConfig.PROMOTE_STRONG_MIN_SAMPLE
                && s.posteriorMean >= AppConfig.PROMOTE_STRONG_MIN_POSTERIOR
                && s.wilsonLowerBound >= AppConfig.PROMOTE_STRONG_MIN_WILSON
                && s.driftScore <= 0.12d) return AppConfig.PROMOTION_STRONG;
        if (s.sampleCount >= AppConfig.PROMOTE_READY_MIN_SAMPLE
                && s.posteriorMean >= AppConfig.PROMOTE_READY_MIN_POSTERIOR
                && s.wilsonLowerBound >= AppConfig.PROMOTE_READY_MIN_WILSON
                && s.driftScore <= 0.15d) return AppConfig.PROMOTION_READY;
        if (s.sampleCount >= AppConfig.PROMOTE_WATCH_MIN_SAMPLE
                && s.posteriorMean >= AppConfig.PROMOTE_WATCH_MIN_POSTERIOR
                && s.misleadingStreak < 4) return AppConfig.PROMOTION_PROMOTE_WATCH;
        return s.hitRate >= AppConfig.RANDOM_K3_BASELINE
                ? AppConfig.PROMOTION_WATCH_POSITIVE
                : AppConfig.PROMOTION_WATCH;
    }

    private String buildParentShadowDomain(String domain) {
        if (domain == null) return "";
        int idx = domain.lastIndexOf('_');
        return idx > 0 ? domain.substring(0, idx) : domain;
    }

    private double getDoubleReflect(Object o, String field, double def) {
        if (o == null) return def;
        try {
            Field f = o.getClass().getField(field);
            Object v = f.get(o);
            return v instanceof Number ? ((Number) v).doubleValue() : def;
        } catch (Throwable e) {
            return def;
        }
    }

    private String appendText(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "；" + b;
    }

    private boolean shouldTriggerLearningAfterSettlement(Models.Trade trade) {
        if (trade == null) {
            return false;
        }

        String result = normalizeResultText(trade.result);

        if (!AppConfig.RESULT_WIN.equals(result) && !AppConfig.RESULT_MISS.equals(result)) {
            return false;
        }

        if (trade.settled != 1) {
            return false;
        }

        if (!isRealBetTradeScopeAndAction(trade)) {
            return false;
        }

        if (!AppConfig.isValidPosition(trade.position)) {
            return false;
        }

        if (!AppConfig.isValidNumber(trade.actualNumber)) {
            return false;
        }

        if (!isStrictValidTradeK3(trade)) {
            return false;
        }

        if (trade.targetIssue == null || trade.targetIssue.length() == 0) {
            return false;
        }

        return true;
    }

    private LearningEngine.LearningReport runPostSettlementLearning(
            DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        if (store == null || trade == null || actualDraw == null || !actualDraw.isValid()) {
            return null;
        }

        try {
            LearningEngine engine = new LearningEngine();
            return engine.learnTrade(store, trade, actualDraw);
        } catch (Throwable e) {
            try {
                store.setState("settlement_learning_error", "结算后学习异常");
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    public SettlementResult settleTradeObject(Models.Trade trade, Models.Draw actualDraw) {
        return buildStrictSettlementResult(trade, actualDraw, false);
    }

    private SettlementResult buildStrictSettlementResult(
            Models.Trade trade, Models.Draw actualDraw, boolean allowAlreadySettledRepair) {
        SettlementResult r = new SettlementResult();

        if (trade == null) {
            r.canSettle = false;
            r.detail = "交易为空，无法结算";
            return r;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            r.canSettle = false;
            r.detail = "实际开奖为空或非法，等待下一次抓取";
            return r;
        }

        Models.Trade t = copyTrade(trade);

        if (t == null) {
            t = trade;
        }

        r.trade = t;

        if (!isRealBetTradeScopeAndAction(t)) {
            r.canSettle = false;
            r.detail =
                    "非 REAL_TRADE + BET，不进入真实结算：scope="
                            + safe(t.statScope)
                            + " action="
                            + safe(t.action);
            return r;
        }

        normalizeLegacyRealTradeAction(t);

        if (!allowAlreadySettledRepair) {
            if (t.settled == 1) {
                r.canSettle = false;
                r.detail = "交易已经结算过，跳过重复结算：" + safe(t.result);
                return r;
            }

            String oldResult = normalizeResultText(t.result);

            if (oldResult.length() > 0 && !AppConfig.RESULT_WAIT.equals(oldResult)) {
                r.canSettle = false;
                r.detail = "交易已经有结果，跳过重复结算：" + safe(oldResult);
                return r;
            }
        }

        if (t.targetIssue == null || t.targetIssue.length() == 0) {
            r.canSettle = false;
            r.detail = "交易目标期为空，无法结算";
            return r;
        }

        if (actualDraw.issue == null || actualDraw.issue.length() == 0) {
            r.canSettle = false;
            r.detail = "实际开奖缺少期号，无法结算";
            return r;
        }

        if (!sameIssue(t.targetIssue, actualDraw.issue)) {
            r.canSettle = false;
            r.detail = "目标期未开奖，等待：" + safe(t.targetIssue) + "，当前开奖：" + safe(actualDraw.issue);
            return r;
        }

        long now = System.currentTimeMillis();

        normalizeTradeStrictK3(t);

        if (!isStrictValidTradeK3(t)) {
            t.result = AppConfig.RESULT_DATA_ERROR;
            t.actualNumber = 0;
            t.hit = 0;
            t.missReason = AppConfig.MISS_DATA_OFFSET;
            t.pnl = 0.0d;
            t.settleTime = now;
            t.settledAt = now;
            t.settled = 1;
            t.action = AppConfig.ACTION_BET;
            t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

            r.canSettle = true;
            r.hit = false;
            r.pnl = 0.0d;
            r.actualNumber = 0;
            r.detail = "交易位置或K3号码非法，标记DATA_ERROR；不进入学习样本";
            return r;
        }

        int actualNumber = actualDraw.getNumberAtPosition(t.position);

        if (!AppConfig.isValidNumber(actualNumber)) {
            t.result = AppConfig.RESULT_DATA_ERROR;
            t.actualNumber = 0;
            t.hit = 0;
            t.missReason = AppConfig.MISS_DATA_OFFSET;
            t.pnl = 0.0d;
            t.settleTime = now;
            t.settledAt = now;
            t.settled = 1;
            t.action = AppConfig.ACTION_BET;
            t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

            r.canSettle = true;
            r.hit = false;
            r.pnl = 0.0d;
            r.actualNumber = 0;
            r.detail = "实际开奖号码非法，标记DATA_ERROR；不进入学习样本";
            return r;
        }

        boolean hit = strictK3Contains(t, actualNumber);

        t.actualNumber = actualNumber;
        t.settleTime = now;
        t.settledAt = now;
        t.settled = 1;
        t.action = AppConfig.ACTION_BET;
        t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

        if (hit) {
            t.hit = 1;
            t.result = AppConfig.RESULT_WIN;
            t.missReason = "";
            t.pnl = AppConfig.WIN_NET_PROFIT;
        } else {
            t.hit = 0;
            t.result = AppConfig.RESULT_MISS;
            t.missReason = classifyMissReason(t, actualDraw);
            t.pnl = AppConfig.LOSE_NET_PROFIT;
        }

        r.canSettle = true;
        r.hit = hit;
        r.pnl = t.pnl;
        r.actualNumber = actualNumber;
        r.detail = buildSingleSettlementText(t);

        return r;
    }

    private boolean strictK3Contains(Models.Trade t, int actualNumber) {
        if (t == null || !AppConfig.isValidNumber(actualNumber)) {
            return false;
        }

        return actualNumber == t.n1 || actualNumber == t.n2 || actualNumber == t.n3;
    }

    private boolean isStrictValidTradeK3(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (!AppConfig.isValidNumber(t.n1)
                || !AppConfig.isValidNumber(t.n2)
                || !AppConfig.isValidNumber(t.n3)) {
            return false;
        }

        if (t.n1 == t.n2 || t.n1 == t.n3 || t.n2 == t.n3) {
            return false;
        }

        t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);

        return Models.isValidK3Mask(t.k3Mask);
    }

    private void normalizeTradeStrictK3(Models.Trade trade) {
        if (trade == null) {
            return;
        }

        if (AppConfig.isValidNumber(trade.n1)
                && AppConfig.isValidNumber(trade.n2)
                && AppConfig.isValidNumber(trade.n3)
                && trade.n1 != trade.n2
                && trade.n1 != trade.n3
                && trade.n2 != trade.n3) {
            trade.k3Mask = Models.buildK3Mask(trade.n1, trade.n2, trade.n3);
        }

        trade.result = normalizeResultText(trade.result);

        if (trade.result == null || trade.result.length() == 0) {
            trade.result = AppConfig.RESULT_WAIT;
        }

        trade.action = AppConfig.ACTION_BET;
        trade.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
    }

    private String normalizeResultText(String result) {
        if (result == null) {
            return "";
        }

        String r = result.trim();

        if (r.length() == 0) {
            return "";
        }

        String u = r.toUpperCase(java.util.Locale.ROOT);

        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }

        if ("MISS".equals(u)
                || "LOSE".equals(u)
                || "LOSS".equals(u)
                || "未中".equals(r)
                || "不中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }

        if ("WAIT".equals(u) || "WAITING".equals(u) || "等待".equals(r) || "等待结算".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }

        if ("DATA_ERROR".equals(u) || "ERROR".equals(u) || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        if ("VOID".equals(u) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }

        try {
            return AppConfig.normalizeResult(result);
        } catch (Throwable e) {
            return u;
        }
    }

    public Models.Trade createTradeFromDecision(Models.Decision decision) {
        if (!canCreateTradeFromDecision(decision)) {
            return null;
        }

        Models.Trade t = new Models.Trade();
        /* Copy the complete immutable V24 contract and authorization before legacy fields. */
        t.applyDecision(decision);

        t.decisionId = decision.id;
        t.baseIssue = safe(decision.baseIssue);
        t.targetIssue = safe(decision.targetIssue);

        t.createdAt = decision.createdAt > 0L ? decision.createdAt : System.currentTimeMillis();
        t.betTime = t.createdAt;
        t.settleTime = 0L;
        t.settledAt = 0L;

        t.position = decision.position;
        t.setNumbers(decision.n1, decision.n2, decision.n3);
        t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);

        t.rawP = decision.rawP;
        t.calibratedP = decision.calibratedP;
        t.finalP = decision.finalP;
        t.dynamicGate = decision.dynamicGate;
        t.edge = decision.edge;

        if (t.edge == 0.0d && t.finalP > 0.0d && t.dynamicGate > 0.0d) {
            t.edge = t.finalP - t.dynamicGate;
        }

        t.modelP = decision.modelP;
        t.shapeP = decision.shapeP;
        t.comboP = decision.comboP;
        t.recoveryP = decision.recoveryP;
        t.similarP = decision.similarP;
        t.memoryP = decision.memoryP;
        t.calibrationError = decision.calibrationError;

        t.similarHitRate = decision.similarHitRate;
        t.comboHitRate = decision.comboHitRate;
        t.effectiveSample = decision.effectiveSample;
        t.comboSample = decision.comboSample;

        t.trendLife = decision.trendLife;
        t.driftRisk = decision.driftRisk;
        t.breakDepth = decision.breakDepth;
        t.recoveryScore = decision.recoveryScore;
        t.championGap = decision.championGap;
        t.comboGap = decision.comboGap;
        t.hotReverseRisk = decision.hotReverseRisk;
        t.falseStrongRisk = decision.falseStrongRisk;

        t.stateMode = decision.stateMode == null ? AppConfig.MODE_CHAOS : decision.stateMode;
        t.shapeKey = safe(decision.shapeKey);
        t.stateKey = safe(decision.stateKey);
        t.reasonSummary = safe(decision.reasonSummary);

        t.deepScore = decision.deepScore;
        t.deepConfidence = decision.deepConfidence;
        t.deepFeatureP = decision.deepFeatureP;
        t.deepPositionScore = decision.deepPositionScore;
        t.deepNumberScore = decision.deepNumberScore;
        t.deepCycleScore = decision.deepCycleScore;
        t.deepTransitionScore = decision.deepTransitionScore;
        t.deepStateScore = decision.deepStateScore;

        t.aiJudgeScore = decision.aiJudgeScore;
        t.aiJudgeConfidence = decision.aiJudgeConfidence;
        t.evidenceScore = decision.evidenceScore;
        t.evidenceRisk = decision.evidenceRisk;
        t.biasScore = decision.biasScore;
        t.biasConfidence = decision.biasConfidence;
        t.auditPenalty = decision.auditPenalty;
        t.auditBoost = decision.auditBoost;
        t.profileScore = decision.profileScore;
        t.fallbackProfileScore = decision.fallbackProfileScore;
        t.runtimeProfileScore = decision.runtimeProfileScore;
        t.stateNumberProfileScore = decision.stateNumberProfileScore;
        t.teacherWeight = decision.teacherWeight;
        t.aPlusScore = decision.aPlusScore;
        t.promotionScore = decision.promotionScore;
        t.conflictLevel = decision.conflictLevel;
        t.weakestNumberScore = decision.weakestNumberScore;
        t.balanceScore = decision.balanceScore;

        t.deepProfileKey = safe(decision.deepProfileKey);
        t.deepProfileHitRate = decision.deepProfileHitRate;
        t.deepProfileSample = decision.deepProfileSample;
        t.deepFallbackLevel = decision.deepFallbackLevel;
        t.fallbackProfileKey = safe(decision.fallbackProfileKey);
        t.promotionStatus = safe(decision.promotionStatus);
        t.aPlusStatus = safe(decision.aPlusStatus);
        t.candidateEvidenceKey = safe(decision.candidateEvidenceKey);
        t.snapshotId = safe(decision.snapshotId);

        copyOptionalField(decision, t, "teacherMatrixSample");
        copyOptionalField(decision, t, "teacherMatrixHitRate");
        copyOptionalField(decision, t, "teacherMatrixConfidence");
        copyOptionalField(decision, t, "teacherMatrixProbAdjust");
        copyOptionalField(decision, t, "teacherMatrixGateAdjust");
        copyOptionalField(decision, t, "teacherMatrixRiskPenalty");
        copyOptionalField(decision, t, "teacherMatrixApplied");
        copyOptionalField(decision, t, "teacherMatrixStatus");
        copyOptionalField(decision, t, "teacherMatrixProfileKey");
        copyOptionalField(decision, t, "teacherMatrixComboKey");
        copyOptionalField(decision, t, "teacherMatrixSummary");
        copyV20SettlementEvidenceFields(decision, t);

        t.actualNumber = 0;
        t.hit = 0;
        t.result = AppConfig.RESULT_WAIT;
        t.missReason = "";
        t.pnl = 0.0d;
        t.settled = 0;
        t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        t.action = AppConfig.ACTION_BET;

        return t;
    }

    public boolean canCreateTradeFromDecision(Models.Decision decision) {
        if (decision == null) {
            return false;
        }
        /* Settlement may consume a BET but may never create or repair one without the signer. */
        if (!DecisionAuthorizationEngine.isDecisionAuthorized(decision)) {
            return false;
        }

        if (!isBetDecisionOrAction(decision.decision, decision.action)) {
            return false;
        }

        String scope = decision.statScope == null ? "" : decision.statScope;

        if (scope.length() > 0 && !isRealTradeScope(scope)) {
            return false;
        }

        if (!AppConfig.isValidPosition(decision.position)) {
            return false;
        }

        if (!AppConfig.isValidNumber(decision.n1)
                || !AppConfig.isValidNumber(decision.n2)
                || !AppConfig.isValidNumber(decision.n3)) {
            return false;
        }

        if (decision.n1 == decision.n2
                || decision.n1 == decision.n3
                || decision.n2 == decision.n3) {
            return false;
        }

        if (decision.targetIssue == null || decision.targetIssue.length() == 0) {
            return false;
        }

        if (isQuickOrPlaceholderDecision(decision)) {
            return false;
        }

        decision.decision = AppConfig.DECISION_BET;
        decision.action = AppConfig.ACTION_BET;
        decision.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        decision.k3Mask = Models.buildK3Mask(decision.n1, decision.n2, decision.n3);

        return Models.isValidK3Mask(decision.k3Mask);
    }

    public boolean saveBetDecisionAsTrade(DbStore store, Models.Decision decision) {
        if(store==null || decision==null || decision.targetIssue==null)return false;
        Models.Trade existing=store.getTradeByTargetIssue(decision.targetIssue);
        return existing!=null && existing.position==decision.position && existing.n1==decision.n1
                && existing.n2==decision.n2 && existing.n3==decision.n3; // Read-only, never BET creation.
    }

    private boolean isQuickOrPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        String text = buildDecisionSearchText(d);

        /*
         * V20结算修复：真实BET不能因为说明文字里出现“SHADOW/HOLD不计真实统计”
         * 这类审计句子就被误判成占位。只要它是明确BET、REAL_TRADE或空scope、
         * 单位置K3合法、目标期存在、且没有硬占位关键词，就允许创建/补建Trade。
         */
        if (isStrongConfirmedRealBetDecision(d, text)) {
            return false;
        }

        if (hasHardPlaceholderKeyword(text)) {
            return true;
        }

        if (text.indexOf("QUICK_OBSERVE") >= 0
                || text.indexOf("QUICK_") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("只做观察") >= 0
                || text.indexOf("不作为真实 BET") >= 0
                || text.indexOf("不作为真实BET") >= 0
                || text.indexOf("临时") >= 0
                || text.indexOf("正在生成") >= 0
                || text.indexOf("等待实时分析") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        return false;
    }

    private boolean isStrongConfirmedRealBetDecision(Models.Decision d, String text) {
        if (d == null) {
            return false;
        }
        if (!isBetDecisionOrAction(d.decision, d.action)) {
            return false;
        }
        String scope = d.statScope == null ? "" : d.statScope.trim();
        if (scope.length() > 0 && !isRealTradeScope(scope)) {
            return false;
        }
        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }
        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }
        if (d.n1 == d.n2 || d.n1 == d.n3 || d.n2 == d.n3) {
            return false;
        }
        if (d.targetIssue == null || d.targetIssue.length() == 0) {
            return false;
        }
        if (hasHardPlaceholderKeyword(text)) {
            return false;
        }
        if (d.blockCode != null && d.blockCode.length() > 0) {
            return false;
        }
        if (d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            return true;
        }
        return d.k3Mask > 0 || Models.isValidK3Mask(Models.buildK3Mask(d.n1, d.n2, d.n3));
    }

    private boolean hasHardPlaceholderKeyword(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        return text.indexOf("保护性 HOLD") >= 0
                || text.indexOf("正式 HOLD") >= 0
                || text.indexOf("DECISION_HOLD") >= 0
                || text.indexOf("占位") >= 0
                || text.indexOf("未生成有效决策") >= 0
                || text.indexOf("分析线程未生成") >= 0;
    }

    private String buildDecisionSearchText(Models.Decision d) {
        if (d == null) {
            return "";
        }
        String text = "";
        if (d.decision != null) text += d.decision;
        if (d.action != null) text += " " + d.action;
        if (d.reasonSummary != null) text += " " + d.reasonSummary;
        if (d.reasonText != null) text += " " + d.reasonText;
        if (d.deepReason != null) text += " " + d.deepReason;
        if (d.learningSummary != null) text += " " + d.learningSummary;
        if (d.selfDiagnosisText != null) text += " " + d.selfDiagnosisText;
        if (d.blockCode != null) text += " " + d.blockCode;
        if (d.probabilityMode != null) text += " " + d.probabilityMode;
        if (d.backtestMode != null) text += " " + d.backtestMode;
        if (d.shapeKey != null) text += " " + d.shapeKey;
        return text;
    }

    public String classifyMissReason(Models.Trade trade, Models.Draw actualDraw) {
        if (trade == null || actualDraw == null || !actualDraw.isValid()) {
            return AppConfig.MISS_UNKNOWN;
        }

        if (!AppConfig.isValidPosition(trade.position)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        normalizeTradeStrictK3(trade);

        if (!isStrictValidTradeK3(trade)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        int actual = actualDraw.getNumberAtPosition(trade.position);

        if (!AppConfig.isValidNumber(actual)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        if (strictK3Contains(trade, actual)) {
            return "";
        }

        if (AppConfig.MODE_BROKEN.equals(trade.stateMode)
                || AppConfig.MODE_REVERSING.equals(trade.stateMode)) {
            return MISS_TREND_BROKEN;
        }

        if (trade.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return MISS_DRIFT_SPIKE;
        }

        if (trade.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX) {
            return MISS_HOT_REVERSE;
        }

        if (trade.falseStrongRisk >= AppConfig.FALSE_STRONG_RISK_MAX) {
            return MISS_HOT_REVERSE;
        }

        if (trade.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && (AppConfig.MODE_RECOVERING.equals(trade.stateMode)
                        || AppConfig.MODE_PULLBACK.equals(trade.stateMode))) {
            return MISS_FALSE_RECOVERY;
        }

        if (trade.effectiveSample > 0 && trade.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            return MISS_SAMPLE_WEAK;
        }

        if (trade.comboSample > 0 && trade.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            return MISS_SAMPLE_WEAK;
        }

        if (trade.comboGap < AppConfig.COMBO_GAP_MIN) {
            return MISS_THIRD_WEAK;
        }

        if (trade.championGap < AppConfig.CHAMPION_GAP_MIN) {
            return MISS_POSITION_LOST;
        }

        if (trade.reasonSummary != null && trade.reasonSummary.length() > 0) {
            if (containsBoth(trade.reasonSummary, "Markov", "不足")
                    || containsBoth(trade.reasonSummary, "马尔可夫", "不足")) {
                return MISS_MARKOV_FAILED;
            }

            if (containsBoth(trade.reasonSummary, "样本", "不足")) {
                return MISS_SAMPLE_WEAK;
            }

            if (trade.reasonSummary.indexOf("破位") >= 0 || trade.reasonSummary.indexOf("断点") >= 0) {
                return MISS_TREND_BROKEN;
            }

            if (trade.reasonSummary.indexOf("漂移") >= 0 && trade.reasonSummary.indexOf("高") >= 0) {
                return MISS_DRIFT_SPIKE;
            }

            if (trade.reasonSummary.indexOf("反杀") >= 0 || trade.reasonSummary.indexOf("反转") >= 0) {
                return MISS_HOT_REVERSE;
            }

            if (trade.reasonSummary.indexOf("假恢复") >= 0) {
                return MISS_FALSE_RECOVERY;
            }
        }

        if (trade.finalP >= AppConfig.DEFAULT_DYNAMIC_GATE
                && trade.comboGap < AppConfig.COMBO_GAP_MIN + 0.010d) {
            return MISS_K3_RANK_4_6;
        }

        return AppConfig.MISS_UNKNOWN;
    }

    public String resultText(Models.Trade trade) {
        if (trade == null) {
            return "暂无交易";
        }

        String result = normalizeResultText(trade.result);

        if (result == null || result.length() == 0) {
            result = AppConfig.RESULT_WAIT;
        }

        if (AppConfig.RESULT_WAIT.equals(result)) {
            return "等待开奖：P"
                    + trade.position
                    + " 投注="
                    + UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3);
        }

        if (AppConfig.RESULT_WIN.equals(result)) {
            return "命中：P"
                    + trade.position
                    + " 投注="
                    + UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3)
                    + "，实际="
                    + UiFormatter.number(trade.actualNumber);
        }

        if (AppConfig.RESULT_MISS.equals(result)) {
            return "未中：P"
                    + trade.position
                    + " 投注="
                    + UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3)
                    + "，实际="
                    + UiFormatter.number(trade.actualNumber)
                    + "，错因="
                    + UiFormatter.missReasonText(trade.missReason);
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(result)) {
            return "结算数据异常：目标期="
                    + safe(trade.targetIssue)
                    + "，投注=P"
                    + trade.position
                    + " "
                    + UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3)
                    + "，错因="
                    + UiFormatter.missReasonText(trade.missReason);
        }

        return "结算异常：" + result;
    }

    public String buildSettlementDetail(
            SettlementResult r, LearningEngine.LearningReport learningReport) {
        if (r == null || r.trade == null) {
            return "结算结果为空";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(buildSingleSettlementText(r.trade));

        if (learningReport != null
                && learningReport.detail != null
                && learningReport.detail.length() > 0) {
            sb.append("；");
            sb.append(learningReport.detail);
        }

        return trimDetail(sb.toString());
    }

    public ArrayList<Models.ReasonItem> buildSettlementReasons(Models.Trade trade) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();

        if (trade == null) {
            Models.ReasonItem item = new Models.ReasonItem();
            item.level = AppConfig.REASON_INFO;
            item.code = "NO_TRADE";
            item.title = "无交易";
            item.detail = "当前没有需要显示的真实BET结算";
            item.value = 0.0d;
            item.createdAt = System.currentTimeMillis();
            out.add(item);
            return out;
        }

        Models.ReasonItem base = new Models.ReasonItem();
        base.level = AppConfig.REASON_INFO;
        base.code = "STRICT_K3_SETTLEMENT";
        base.title = "严格单位置K3结算";
        base.detail = "只看目标期、只看预测位置P" + trade.position + "，实际号码必须等于投注三号之一；不扫其他位置，不用全期命中顶替。";
        base.value = 0.0d;
        base.createdAt = System.currentTimeMillis();
        out.add(base);

        if (AppConfig.RESULT_WIN.equals(trade.result)
                || AppConfig.RESULT_MISS.equals(trade.result)) {
            Models.ReasonItem item = new Models.ReasonItem();
            item.level =
                    AppConfig.RESULT_WIN.equals(trade.result)
                            ? AppConfig.REASON_PASS
                            : AppConfig.REASON_WARN;
            item.code = trade.result;
            item.title = AppConfig.RESULT_WIN.equals(trade.result) ? "结算命中" : "结算未中";
            item.detail = resultText(trade);
            item.value = trade.pnl;
            item.createdAt = System.currentTimeMillis();
            out.add(item);
        }

        if (AppConfig.RESULT_MISS.equals(trade.result)) {
            Models.ReasonItem miss = new Models.ReasonItem();
            miss.level = AppConfig.REASON_WARN;
            miss.code = trade.missReason;
            miss.title = "错因归因";
            miss.detail = UiFormatter.missReasonText(trade.missReason);
            miss.value = 0.0d;
            miss.createdAt = System.currentTimeMillis();
            out.add(miss);
        }

        return out;
    }

    private void applyResultToReport(SettlementReport report, SettlementResult r) {
        if (report == null || r == null || r.trade == null) {
            return;
        }

        report.settledCount++;

        if (r.hit) {
            report.winCount++;
        } else {
            if (AppConfig.RESULT_MISS.equals(r.trade.result)) {
                report.missCount++;
            } else if (AppConfig.RESULT_DATA_ERROR.equals(r.trade.result)) {
                report.dataErrorCount++;
            }
        }

        report.pnl += r.pnl;
        report.actualNumber = r.actualNumber;
        report.result = r.trade.result == null ? "" : r.trade.result;
        report.missReason = r.trade.missReason == null ? "" : r.trade.missReason;

        report.teacherMatrixDrawCount += r.teacherMatrixDrawCount;
        report.teacherMatrixCellCount += r.teacherMatrixCellCount;

        if (r.learningSummary != null && r.learningSummary.length() > 0) {
            if (report.learningSummary == null || report.learningSummary.length() == 0) {
                report.learningSummary = r.learningSummary;
            } else {
                report.learningSummary =
                        trimDetail(report.learningSummary + " | " + r.learningSummary);
            }
        }
    }

    private int safeSettleTrade(DbStore store, Models.Trade trade) {
        if(store==null || trade==null || trade.targetIssue==null)return 0;
        Models.Draw draw=store.getDrawByIssue(trade.targetIssue);
        if(draw==null || !draw.isValid())return 0;
        SettlementReport report=SettlementCoordinatorR815.settleNow(store,draw,SettlementCoordinatorR815.Reason.LEGACY_API);
        return report==null?0:report.settledCount;
    }

    private boolean verifyTradeSettlement(DbStore store, Models.Trade trade) {
        if (store == null
                || trade == null
                || trade.targetIssue == null
                || trade.targetIssue.length() == 0) {
            return false;
        }

        Models.Trade saved = safeGetTradeByTargetIssue(store, trade.targetIssue);

        if (saved == null) {
            return false;
        }

        if (saved.settled != 1) {
            return false;
        }

        if (!sameText(normalizeResultText(trade.result), normalizeResultText(saved.result))) {
            return false;
        }

        if (saved.actualNumber != trade.actualNumber) {
            return false;
        }

        if (saved.hit != trade.hit) {
            return false;
        }

        if (Math.abs(saved.pnl - trade.pnl) > 0.000001d) {
            return false;
        }

        return true;
    }

    private void saveCleanLearningSampleState(DbStore store, Models.Trade trade) {
        if (store == null || trade == null) {
            return;
        }

        String result = normalizeResultText(trade.result);
        boolean clean =
                isRealBetTradeScopeAndAction(trade)
                        && trade.settled == 1
                        && (AppConfig.RESULT_WIN.equals(result)
                                || AppConfig.RESULT_MISS.equals(result))
                        && AppConfig.isValidPosition(trade.position)
                        && AppConfig.isValidNumber(trade.actualNumber)
                        && isStrictValidTradeK3(trade);

        try {
            store.setState("settlement_learning_sample_ready", clean ? "1" : "0");
            store.setState(
                    "settlement_learning_sample_issue", clean ? safe(trade.targetIssue) : "");
            store.setState("settlement_learning_sample_result", clean ? result : "");
            store.setState(
                    "settlement_learning_sample_rule",
                    "ONLY_REAL_TRADE_BET_SETTLED_WIN_MISS_STRICT_POSITION_K3");

            if (clean) {
                store.setState("last_clean_settled_issue", safe(trade.targetIssue));
                store.setState("last_clean_settled_position", String.valueOf(trade.position));
                store.setState(
                        "last_clean_settled_numbers",
                        UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3));
                store.setState("last_clean_settled_actual", UiFormatter.number(trade.actualNumber));
                store.setState("last_clean_settled_result", result);
                store.setState("last_clean_settled_hit", String.valueOf(trade.hit));
                store.setState("last_clean_settled_pnl", String.valueOf(trade.pnl));
            }
        } catch (Throwable e) {
        }
    }

    private void markShadowLearnedSafe(DbStore store, String targetIssue) {
        if (store == null || targetIssue == null || targetIssue.length() == 0) {
            return;
        }

        try {
            store.markShadowLearned(targetIssue);
        } catch (Throwable e) {
        }
    }

    private int repairMissingTradesFromRecentBetDecisions(
            DbStore store, HashMap<String, Models.Draw> drawMap, StringBuilder detail) {
        if (store == null) {
            return 0;
        }
        List<Models.Decision> decisions = getRecentDecisionsSafe(store, DECISION_REPAIR_SCAN_LIMIT);
        if (decisions == null || decisions.size() == 0) {
            return 0;
        }
        int created = 0;
        for (int i = 0; i < decisions.size(); i++) {
            Models.Decision d = decisions.get(i);
            if (d == null) {
                continue;
            }
            if (!canCreateTradeFromDecision(d)) {
                continue;
            }
            if (d.targetIssue == null || d.targetIssue.length() == 0) {
                continue;
            }
            Models.Trade old = safeGetTradeByTargetIssue(store, d.targetIssue);
            if (old != null) {
                continue;
            }
            boolean ok = saveBetDecisionAsTrade(store, d);
            if (ok) {
                created++;
                appendDetail(detail, "补建真实BET交易：" + safe(d.targetIssue));
                continue;
            }
            Models.Trade t = createTradeFromDecision(d);
            long id = safeSaveTrade(store, t);
            if (id > 0L) {
                created++;
                appendDetail(detail, "兜底补建真实BET交易：" + safe(d.targetIssue));
            }
        }
        return created;
    }

    private List<Models.Decision> getRecentDecisionsSafe(DbStore store, int limit) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (store == null) {
            return out;
        }
        try {
            List<Models.Decision> list = store.getRecentDecisions(limit);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Models.Decision d = list.get(i);
                    if (d != null) {
                        out.add(d);
                    }
                }
            }
        } catch (Throwable e) {
        }
        return out;
    }

    private void syncDecisionSettlementStateSafe(DbStore store, Models.Trade trade) {
        if (store == null
                || trade == null
                || trade.targetIssue == null
                || trade.targetIssue.length() == 0) {
            return;
        }
        try {
            store.setState("settlement_last_sync_version", SETTLEMENT_SPEED_VERSION);
            store.setState("settlement_last_trade_issue", safe(trade.targetIssue));
            store.setState("settlement_last_trade_result", safe(trade.result));
            store.setState("settlement_last_trade_pnl", String.valueOf(trade.pnl));
            store.setState("settlement_last_trade_actual", String.valueOf(trade.actualNumber));
            store.setState("last_strict_trade_settlement_detail", buildSingleSettlementText(trade));
            store.setState("last_strict_settlement_detail", buildSingleSettlementText(trade));
        } catch (Throwable e) {
        }
        tryInvokeStore(
                store,
                "updateDecisionSettlementFromTrade",
                new Class[] {Models.Trade.class},
                new Object[] {trade});
        tryInvokeStore(
                store,
                "markDecisionSettledByTrade",
                new Class[] {Models.Trade.class},
                new Object[] {trade});
        tryInvokeStore(
                store,
                "syncDecisionSettlement",
                new Class[] {Models.Trade.class},
                new Object[] {trade});
    }

    private Object tryInvokeStore(DbStore store, String name, Class[] types, Object[] args) {
        if (store == null || name == null || name.length() == 0) {
            return null;
        }
        try {
            MethodHolder holder = new MethodHolder();
            return holder.invoke(store, name, types, args);
        } catch (Throwable e) {
            return null;
        }
    }

    private static final class MethodHolder {
        Object invoke(Object target, String name, Class[] types, Object[] args) throws Exception {
            java.lang.reflect.Method m = target.getClass().getMethod(name, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        }
    }

    private List<Models.Trade> getOpenTradesSafe(DbStore store) {
        ArrayList<Models.Trade> out = new ArrayList<Models.Trade>();

        if (store == null) {
            return out;
        }

        try {
            List<Models.Trade> list = store.getOpenTrades(OPEN_TRADE_SCAN_LIMIT);

            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Models.Trade t = list.get(i);

                    if (t != null && t.settled == 0 && isRealBetTradeScopeAndAction(t)) {
                        out.add(t);
                    }
                }
            }
        } catch (Throwable e) {
        }

        if (out.size() > 0) {
            return out;
        }

        try {
            Models.Trade one = store.getOpenTrade();

            if (one != null && one.settled == 0 && isRealBetTradeScopeAndAction(one)) {
                out.add(one);
            }
        } catch (Throwable e) {
        }

        return out;
    }

    private ArrayList<Models.Draw> loadRecentDrawsWithLatest(
            DbStore store, Models.Draw latestDraw) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (store == null) {
            if (latestDraw != null && latestDraw.isValid()) {
                out.add(latestDraw);
            }
            return out;
        }

        List<Models.Draw> list = getRecentDrawsSafe(store, RECENT_DRAW_SCAN_LIMIT);
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(list);

        if (clean != null) {
            for (int i = 0; i < clean.size(); i++) {
                Models.Draw d = clean.get(i);

                if (d != null && d.isValid()) {
                    out.add(d);
                }
            }
        }

        if (latestDraw != null && latestDraw.isValid()) {
            boolean found = false;

            for (int i = 0; i < out.size(); i++) {
                Models.Draw d = out.get(i);

                if (d != null && sameIssue(d.issue, latestDraw.issue)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                out.add(latestDraw);
            }
        }

        return dataGuard.normalizeHistory(out);
    }

    private List<Models.Draw> getRecentDrawsSafe(DbStore store, int limit) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (store == null) {
            return out;
        }

        try {
            List<Models.Draw> list = store.getRecentDraws(limit);

            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Models.Draw d = list.get(i);

                    if (d != null) {
                        out.add(d);
                    }
                }
            }
        } catch (Throwable e) {
        }

        return out;
    }

    private HashMap<String, Models.Draw> buildDrawMap(List<Models.Draw> list) {
        HashMap<String, Models.Draw> map = new HashMap<String, Models.Draw>();

        if (list == null) {
            return map;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.Draw d = list.get(i);

            if (d == null || !d.isValid() || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            map.put(d.issue, d);
        }

        return map;
    }

    private Models.Draw findDrawByIssueMap(HashMap<String, Models.Draw> map, String issue) {
        if (map == null || map.size() == 0 || issue == null || issue.length() == 0) {
            return null;
        }

        Models.Draw d = map.get(issue);

        if (d != null) {
            return d;
        }

        for (String key : map.keySet()) {
            if (sameIssue(issue, key)) {
                return map.get(key);
            }
        }

        return null;
    }

    private Models.Draw safeGetDrawByIssue(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) {
            return null;
        }

        try {
            Models.Draw d = store.getDrawByIssue(issue);

            if (d != null) {
                return d;
            }
        } catch (Throwable e) {
        }

        try {
            Models.Draw d = store.loadDrawByIssue(issue);

            if (d != null) {
                return d;
            }
        } catch (Throwable e) {
        }

        return null;
    }

    private Models.Trade safeGetTradeByTargetIssue(DbStore store, String targetIssue) {
        if (store == null || targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        try {
            Models.Trade t = store.getTradeByTargetIssue(targetIssue);

            if (t != null && isRealBetTradeScopeAndAction(t)) {
                return t;
            }
        } catch (Throwable e) {
        }

        return null;
    }

    private long safeSaveTrade(DbStore store, Models.Trade trade) {
        if(store==null || trade==null || trade.targetIssue==null)return -1L;
        Models.Trade existing=store.getTradeByTargetIssue(trade.targetIssue);
        return existing==null?-1L:existing.id;
    }

    private void copyV20SettlementEvidenceFields(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }
        String[] fields =
                new String[] {
                    "windowStateKey",
                    "stateKey12",
                    "stateKey24",
                    "stateKey48",
                    "stateKey96",
                    "stateKey360",
                    "teacherWindowStateKey",
                    "dynamicWindowStateKey",
                    "probabilityWindowStateKey",
                    "dynamicGateWindowStateKey",
                    "decisionWindowStateKey",
                    "finalDecisionWindowStateKey",
                    "evidenceContribution",
                    "counterContribution",
                    "sampleReliability",
                    "recentValidity",
                    "featureDistance",
                    "probabilityEvidenceContribution",
                    "probabilityCounterContribution",
                    "probabilitySampleReliability",
                    "probabilityRecentValidity",
                    "probabilityFeatureDistance",
                    "dynamicGateEvidenceContribution",
                    "dynamicGateCounterContribution",
                    "dynamicGateSampleReliability",
                    "dynamicGateRecentValidity",
                    "dynamicGateFeatureDistance",
                    "decisionEvidenceContribution",
                    "decisionCounterContribution",
                    "decisionSampleReliability",
                    "decisionRecentValidity",
                    "decisionFeatureDistance",
                    "teacherEvidenceContribution",
                    "teacherCounterContribution",
                    "teacherSampleTrust",
                    "teacherRecentEffectiveness",
                    "teacherFeatureDistance",
                    "similarWindowSample",
                    "similarWindowHitRate",
                    "teacherSimilarWindowSample",
                    "teacherSimilarWindowHitRate",
                    "evidenceText",
                    "counterEvidenceText",
                    "actionAdvice",
                    "decisionEvidenceText",
                    "decisionCounterEvidenceText",
                    "decisionActionAdvice",
                    "probabilityEvidenceText",
                    "probabilityCounterEvidenceText",
                    "probabilityActionAdvice",
                    "dynamicGateEvidenceText",
                    "dynamicGateCounterEvidenceText",
                    "dynamicGateActionAdvice",
                    "teacherEvidenceText",
                    "teacherCounterEvidenceText",
                    "teacherActionAdvice"
                };
        for (int i = 0; i < fields.length; i++) {
            copyOptionalField(from, to, fields[i]);
        }
    }

    private void copyOptionalField(Object from, Object to, String fieldName) {
        if (from == null || to == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f1 = from.getClass().getField(fieldName);
            Field f2 = to.getClass().getField(fieldName);
            Object value = f1.get(from);
            f2.set(to, value);
        } catch (Throwable e) {
        }
    }

    private void saveSettlementState(DbStore store, SettlementReport report) {
        if (store == null || report == null) {
            return;
        }

        try {
            if (report.settledCount > 0 || report.winCount > 0 || report.missCount > 0) {
                RiskCoverageController.invalidate();
            }
            store.setState("settlement_status", report.ok == 1 ? "OK" : "ERROR");
            store.setState("settlement_issue", report.issue == null ? "" : report.issue);
            store.setState("settlement_open_count", String.valueOf(report.openCount));
            store.setState("settlement_settled_count", String.valueOf(report.settledCount));
            store.setState("settlement_waiting_count", String.valueOf(report.waitingCount));
            store.setState("settlement_win_count", String.valueOf(report.winCount));
            store.setState("settlement_miss_count", String.valueOf(report.missCount));
            store.setState("settlement_data_error_count", String.valueOf(report.dataErrorCount));
            store.setState(
                    "settlement_created_trade_count", String.valueOf(report.createdTradeCount));
            store.setState(
                    "settlement_strict_repair_count", String.valueOf(report.strictRepairCount));
            store.setState(
                    "settlement_teacher_matrix_draw_count",
                    String.valueOf(report.teacherMatrixDrawCount));
            store.setState(
                    "settlement_teacher_matrix_cell_count",
                    String.valueOf(report.teacherMatrixCellCount));
            store.setState("settlement_pnl", String.valueOf(report.pnl));
            store.setState("settlement_result", report.result == null ? "" : report.result);
            store.setState(
                    "settlement_miss_reason", report.missReason == null ? "" : report.missReason);
            store.setState(
                    "settlement_learning_summary",
                    report.learningSummary == null ? "" : trimDetail(report.learningSummary));
            store.setState(
                    "settlement_detail", report.detail == null ? "" : trimDetail(report.detail));
            store.setState("settlement_summary", report.toSummaryText());
            store.setState(
                    "settlement_rule",
                    "STRICT_REAL_TRADE_BET_ONLY_TARGET_ISSUE_ONLY_POSITION_ONLY_ACTUAL_EQUALS_N1_N2_N3_NO_OTHER_POSITION_NO_SHADOW_NO_HOLD");
            store.setState("settlement_speed_version", SETTLEMENT_SPEED_VERSION);
            store.setState("settlement_catchup_summary", report.toSummaryText());
            store.setState("settlement_updated_at", String.valueOf(System.currentTimeMillis()));

            store.setState("last_settlement_result", report.result == null ? "" : report.result);
            store.setState("last_settlement_summary", report.toSummaryText());
            store.setState("last_settlement_issue", report.issue == null ? "" : report.issue);
        } catch (Throwable e) {
        }
    }

    private String buildSingleSettlementText(Models.Trade trade) {
        if (trade == null) {
            return "结算结果为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("结算完成：");
        sb.append(readableResult(trade.result));

        sb.append("，目标期=");
        sb.append(safe(trade.targetIssue));

        sb.append("，P");
        sb.append(trade.position);

        sb.append("，投注=");
        sb.append(UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3));

        sb.append("，实际=");
        sb.append(UiFormatter.number(trade.actualNumber));

        sb.append("，规则=只看目标期P");
        sb.append(trade.position);
        sb.append("，actual必须等于投注三号之一；不扫其它位置");

        sb.append("，盈亏=");
        sb.append(UiFormatter.money(trade.pnl));

        if (AppConfig.RESULT_MISS.equals(trade.result)) {
            sb.append("，错因=");
            sb.append(UiFormatter.missReasonText(trade.missReason));
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(trade.result)) {
            sb.append("，数据异常=");
            sb.append(UiFormatter.missReasonText(trade.missReason));
        }

        return sb.toString();
    }

    private String readableResult(String result) {
        result = normalizeResultText(result);

        if (AppConfig.RESULT_WIN.equals(result)) {
            return "命中";
        }

        if (AppConfig.RESULT_MISS.equals(result)) {
            return "未中";
        }

        if (AppConfig.RESULT_WAIT.equals(result)) {
            return "等待";
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(result)) {
            return "数据异常";
        }

        if (AppConfig.RESULT_VOID.equals(result)) {
            return "作废";
        }

        return result == null ? "" : result;
    }

    private void appendSettlementResultDetail(SettlementResult r, String text) {
        if (r == null || text == null || text.length() == 0) {
            return;
        }

        if (r.detail == null || r.detail.length() == 0) {
            r.detail = text;
        } else {
            r.detail = trimDetail(r.detail + "；" + text);
        }
    }

    private void appendDetail(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append(" | ");
        }

        sb.append(text);

        if (sb.length() > MAX_DETAIL_LEN) {
            sb.setLength(MAX_DETAIL_LEN);
            sb.append("...");
        }
    }

    private boolean containsBoth(String text, String a, String b) {
        if (text == null) {
            return false;
        }

        return text.indexOf(a) >= 0 && text.indexOf(b) >= 0;
    }

    private boolean sameIssue(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        try {
            return dataGuard.isSameIssue(a, b);
        } catch (Throwable e) {
            return a.equals(b);
        }
    }

    private boolean sameText(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    private boolean isRealTradeScope(String scope) {
        if (scope == null) {
            return false;
        }

        String s = scope.trim();

        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(s)) {
            return true;
        }

        return "REAL".equals(s)
                || "REAL_TRADE".equals(s)
                || "REALBET".equals(s)
                || "REAL_BET".equals(s);
    }

    private boolean isRealBetTradeScopeAndAction(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (!isRealTradeScope(t.statScope)) {
            return false;
        }

        String action = t.action == null ? "" : t.action.trim();

        if (action.length() == 0) {
            return true;
        }

        return isBetText(action);
    }

    private void normalizeLegacyRealTradeAction(Models.Trade t) {
        if (t == null) {
            return;
        }

        t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        t.action = AppConfig.ACTION_BET;
    }

    private boolean isBetDecisionOrAction(String decision, String action) {
        if (isBetText(decision)) {
            return true;
        }

        return isBetText(action);
    }

    private boolean isBetText(String value) {
        if (value == null) {
            return false;
        }

        String s = value.trim();

        if (s.length() == 0) {
            return false;
        }

        if (AppConfig.DECISION_BET.equals(s)) {
            return true;
        }

        if (AppConfig.ACTION_BET.equals(s)) {
            return true;
        }

        try {
            String d = AppConfig.normalizeDecision(s);
            return AppConfig.DECISION_BET.equals(d);
        } catch (Throwable e) {
        }

        return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeDefault(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback == null ? "" : fallback;
        }

        return s;
    }

    private String trimDetail(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_DETAIL_LEN) {
            return text;
        }

        return text.substring(0, MAX_DETAIL_LEN) + "...";
    }

    private Models.Trade copyTrade(Models.Trade source) {
        if (source == null) {
            return null;
        }

        Models.Trade t = new Models.Trade();

        try {
            Field[] fields = Models.Trade.class.getFields();

            for (int i = 0; i < fields.length; i++) {
                try {
                    Field f = fields[i];

                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }

                    f.set(t, f.get(source));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return t;
    }

    private static final class ShadowSettlementSummary {
        public int settledCount;
        public int hitCount;
        public int missCount;

        public ShadowSettlementSummary() {
            settledCount = 0;
            hitCount = 0;
            missCount = 0;
        }
    }

    public static final class SettlementResult {
        public boolean canSettle;
        public boolean hit;
        public Models.Trade trade;
        public int actualNumber;
        public double pnl;
        public int dbUpdated;
        public int teacherMatrixDrawCount;
        public int teacherMatrixCellCount;
        public int shadowSettledCount;
        public int shadowHitCount;
        public int shadowMissCount;
        public String learningSummary;
        public String detail;

        public SettlementResult() {
            canSettle = false;
            hit = false;
            trade = null;
            actualNumber = 0;
            pnl = 0.0d;
            dbUpdated = 0;
            teacherMatrixDrawCount = 0;
            teacherMatrixCellCount = 0;
            shadowSettledCount = 0;
            shadowHitCount = 0;
            shadowMissCount = 0;
            learningSummary = "";
            detail = "";
        }
    }

    public static final class SettlementReport {
        public int ok;
        public String issue;
        public long createdAt;

        public int openCount;
        public int settledCount;
        public int waitingCount;
        public int winCount;
        public int missCount;
        public int dataErrorCount;
        public int createdTradeCount;
        public int strictRepairCount;
        public int teacherMatrixDrawCount;
        public int teacherMatrixCellCount;
        public int shadowSettledCount;
        public int shadowHitCount;
        public int shadowMissCount;

        public int actualNumber;
        public String result;
        public String missReason;
        public double pnl;

        public String learningSummary;
        public String detail;

        public SettlementReport() {
            ok = 0;
            issue = "";
            createdAt = 0L;

            openCount = 0;
            settledCount = 0;
            waitingCount = 0;
            winCount = 0;
            missCount = 0;
            dataErrorCount = 0;
            createdTradeCount = 0;
            strictRepairCount = 0;
            teacherMatrixDrawCount = 0;
            teacherMatrixCellCount = 0;
            shadowSettledCount = 0;
            shadowHitCount = 0;
            shadowMissCount = 0;

            actualNumber = 0;
            result = "";
            missReason = "";
            pnl = 0.0d;

            learningSummary = "";
            detail = "";
        }

        public String toSummaryText() {
            StringBuilder sb = new StringBuilder();

            sb.append("结算报告：open=");
            sb.append(openCount);
            sb.append(" settled=");
            sb.append(settledCount);
            sb.append(" shadow=");
            sb.append(shadowSettledCount);
            sb.append("(");
            sb.append(shadowHitCount);
            sb.append("/");
            sb.append(shadowMissCount);
            sb.append(")");
            sb.append(" wait=");
            sb.append(waitingCount);
            sb.append(" win=");
            sb.append(winCount);
            sb.append(" miss=");
            sb.append(missCount);

            if (createdTradeCount > 0) {
                sb.append(" autoTrade=");
                sb.append(createdTradeCount);
            }

            if (strictRepairCount > 0) {
                sb.append(" strictRepair=");
                sb.append(strictRepairCount);
            }

            if (dataErrorCount > 0) {
                sb.append(" dataError=");
                sb.append(dataErrorCount);
            }

            if (teacherMatrixCellCount > 0) {
                sb.append(" teacher=");
                sb.append(teacherMatrixDrawCount);
                sb.append("期/");
                sb.append(teacherMatrixCellCount);
                sb.append("格");
            }

            sb.append(" pnl=");
            sb.append(UiFormatter.money(pnl));

            if (learningSummary != null && learningSummary.length() > 0) {
                sb.append("｜学习=");
                sb.append(learningSummary);
            }

            if (detail != null && detail.length() > 0) {
                sb.append("｜");
                sb.append(detail);
            }

            return sb.toString();
        }
    }

    private void updateRealDomainEvidenceV17(DbStore store, Models.Trade t) {
        if (store == null
                || t == null
                || !AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(t.action))
                || t.settled != 1) return;
        if (t.evidenceDomainFrozen != 1
                || t.promotionFineDomain == null
                || t.promotionFineDomain.length() == 0) {
            try {
                store.setState("real_domain_missing_at_settlement_issue", t.targetIssue);
                store.setState("real_domain_missing_at_settlement", "1");
            } catch (Throwable ignore) {
            }
            return;
        }
        String[] domains =
                new String[] {
                    AppConfig.sanitizePromotionDomainKey(t.promotionCoarseDomain),
                    AppConfig.sanitizePromotionDomainKey(t.promotionMidDomain),
                    AppConfig.sanitizePromotionDomainKey(t.promotionFineDomain)
                };
        boolean hit =
                AppConfig.RESULT_WIN.equals(t.result)
                        || "HIT".equals(t.result)
                        || "命中".equals(t.result);
        for (int i = 0; i < domains.length; i++) {
            String key = domains[i];
            if (key == null || key.length() == 0) continue;
            Models.DomainEvidenceStat x =
                    store.loadDomainEvidenceStat(DbHelper.TABLE_DYNAMIC_DOMAIN_STAT_V20, key);
            if (x == null) x = new Models.DomainEvidenceStat();
            x.domainKey = key;
            x.parentDomainKey = i == 2 ? domains[1] : (i == 1 ? domains[0] : "");
            x.coarseDomainKey = domains[0];
            x.sampleCount++;
            if (hit) x.hitCount++;
            else x.missCount++;
            x.hitRate = x.sampleCount <= 0 ? 0.0d : ((double) x.hitCount) / x.sampleCount;
            x.posteriorMean = ((double) x.hitCount + 3.0d) / (x.sampleCount + 10.0d);
            x.lowerBound = Models.calcWilsonLowerBound(x.hitCount, x.sampleCount);
            x.upperBound = Models.calcWilsonUpperBound(x.hitCount, x.sampleCount);
            x.recentSampleCount = Math.min(60, x.sampleCount);
            x.recentHitCount = Math.min(x.hitCount, x.recentSampleCount);
            x.recentHitRate =
                    x.recentSampleCount <= 0
                            ? 0.0d
                            : ((double) x.recentHitCount) / x.recentSampleCount;
            String tier =
                    AppConfig.dynamicRiskTier(
                            x.status, x.sampleCount, x.posteriorMean, x.upperBound);
            if ("HARD_BLOCK".equals(tier)) x.status = "HARD_RISK";
            else if ("SOFT_RISK".equals(tier)) x.status = "SOFT_RISK";
            else if (x.sampleCount >= 30 && x.posteriorMean >= 0.35d) x.status = "READY";
            else x.status = "WATCH";
            x.version = MODULE_BUILD_VERSION;
            x.updatedAt = System.currentTimeMillis();
            store.saveDomainEvidenceStat(DbHelper.TABLE_DYNAMIC_DOMAIN_STAT_V20, x);
            store.saveDomainEvidenceStat(DbHelper.TABLE_TEACHER_DOMAIN_STAT_V20, x);
        }
        try {
            store.setState("settlement_last_frozen_domain", domains[2]);
            store.setState("settlement_domain_version", MODULE_BUILD_VERSION);
        } catch (Throwable ignore) {
        }
    }

    public static final String V57_TERMINAL_OWNER = "SettlementEngine";

    public V51Repository.SettlementReport settleV51Strict(DbStore store, Models.Draw draw) {
        return SettlementCoordinatorR815.settleV51Compatibility(
                store, draw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public static final String B12_UNIQUE_SETTLEMENT_OWNER = "SettlementEngine#settleV51ByDraw";

    public static final class V51SettlementToken {
        private final String targetIssue;
        private final long issuedAt;
        private final String nonce;

        private V51SettlementToken(String targetIssue, long issuedAt) {
            this.targetIssue = targetIssue == null ? "" : targetIssue;
            this.issuedAt = issuedAt;
            nonce =
                    V51ChecksumEngine.sha256(
                            "B12_SETTLEMENT_OWNER|" + this.targetIssue + '|' + issuedAt);
        }

        public boolean validFor(String issue) {
            return targetIssue.equals(issue == null ? "" : issue)
                    && nonce.length() == 64
                    && issuedAt > 0;
        }
    }

    public V51Repository.SettlementReport settleV51ByDraw(DbStore store, Models.Draw draw) {
        return settleV51Strict(store, draw);
    }

    public static boolean b12IsSettlementOwnerToken(V51SettlementToken token, String target) {
        return token != null && token.validFor(target);
    }
}
