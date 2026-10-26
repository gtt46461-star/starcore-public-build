package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;

/**
 * The user's original cross-intersection method runs every period as the league's
 * protected INCUMBENT (P03). Its decision for the next target is computed in the quiet
 * time right after settlement (the burst job), frozen like every other decision, and
 * settled by ModelStoreSU2.settle. A decision not ready before the cutoff is MISSING and
 * counts against the incumbent exactly as it would against a challenger.
 */
public final class IncumbentLoopSU2 {
    private IncumbentLoopSU2() {}
    public static final String KEY = XiIncumbentSU2.GENOME_KEY, MEMBER = "XI_INCUMBENT:" + KEY;
    /** Pooled table of the frozen decision for the pending target (ranks the other 1,199 actions in the certified lane). */
    private static volatile long pendingSeq = -1; private static volatile float[][] pendingTable;
    public static float[][] pendingTable(long seq) { float[][] t = pendingTable; return pendingSeq == seq && t != null ? t : null; }
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "su2-incumbent"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });

    /** Called right after settlement of `settledIssue`: the next target's decision is computed off the settlement thread. */
    public static void scheduleAfterSettlement(DbStore store, String settledIssue) {
        if (!TargetIssueResolver.isValid(settledIssue)) return;
        final String target = TargetIssueResolver.next(settledIssue).targetIssue;
        final long seq = TargetIssueResolver.sequenceOrdinalSU1(target);
        WORKER.execute(() -> { try { freezeNext(store, target, seq); } catch (RuntimeException failure) { StrategyRuntimeSU1.failure(store, "SU2_INCUMBENT_FAILED", failure); } });
    }

    public static void seed(DbStore store, long seq) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_INCUMBENT_SEED", db -> {
            // the incumbent is certified, chosen and bound exactly like any league member: one genome row for its program
            db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{KEY, XiIncumbentSU2.CANONICAL, "XI = 用户原方法：8 专家 Top6/7/8/9 交叉交集支持度 -> 岭回归 Logistic 适用门 -> 25% 基池 -> 15% 出手", "F15", "INCUMBENT", null, "", "INCUMBENT", seq, null, null, "LEAGUE", System.currentTimeMillis()});
            ModelStoreSU2.joinLeague(db, MEMBER, "XI_INCUMBENT", "用户原方法·多位置多号码交叉交集", KEY, seq); return 0; });
    }

    /** Freeze the incumbent's decision for `target` (sequence seq) if not yet frozen. */
    public static String freezeNext(DbStore store, String target, long seq) {
        if (!StrategySchemaSU2.ready(store.readable())) return "SU2_SCHEMA_PENDING";
        try (Cursor c = store.readable().rawQuery("SELECT 1 FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?", new String[]{KEY, String.valueOf(seq)})) { if (c.moveToFirst()) return "ALREADY_FROZEN"; }
        EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, target);
        XiIncumbentSU2.Decision x = XiIncumbentSU2.decideNextFull(frame.history());
        if (x == null) return "INCUMBENT_HISTORY_SHORT";
        final ModelLiveSU2.Decision d = new ModelLiveSU2.Decision(seq, x.position, x.mask(), x.act, Double.NaN, x.phat);   // phat feeds post-selection calibration
        pendingTable = x.table; pendingSeq = seq;
        final long open = frame.targetOpenTime;
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_INCUMBENT_FREEZE", db -> {
            long now = System.currentTimeMillis(); if (now >= open) return "MISSING_AFTER_OPEN";
            ModelStoreSU2.insertDecision(db, KEY, seq, d, now, open); return d.act ? "FROZEN_ACT" : "FROZEN_ABSTAIN";
        });
    }
}
