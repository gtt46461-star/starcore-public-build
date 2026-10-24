package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/**
 * Every-period live loop for SU2 strategies (P06/P07/P09).
 *
 *   capture(target)    before the draw: every live strategy decides (act or not),
 *                      all decisions are frozen in one transaction before open time;
 *   settleWithin(...)  inside the settlement owner transaction: every decision of the
 *                      period gets its label, then every strategy and every shared
 *                      model trains on the settled draw.
 *
 * Runtime state is the exact continuation of a replay that started at the runtime
 * genesis (ModelLiveSU2Test proves live == replay). It is checkpointed every
 * CHECKPOINT_EVERY periods and rebuilt from the retained history (<=5000 draws)
 * at most once per REGENESIS_EVERY periods, so a strategy born later can always
 * bootstrap from draws that are still stored.
 */
public final class ModelLiveLoopSU2 {
    private ModelLiveLoopSU2() {}
    public static final int CHECKPOINT_EVERY = 240, REGENESIS_EVERY = 1440, START_META = 1500, WINDOW = 2500, REFIT = 250;
    /** P08: pre-draw compute budget for all SU2 decisions together; strategies not reached are MISSING. */
    public static final long PREDRAW_BUDGET_NANOS = 1_500_000_000L;

    static final class Runtime {
        long genesisSeq, through = -1; ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        final LinkedHashMap<String, ModelLiveSU2> lives = new LinkedHashMap<>();
        long pendingSeq = -1;
    }
    private static Runtime RT;
    private static final Object CAPTURE_LOCK = new Object();

    static long sequence(String issue) { return TargetIssueResolver.sequenceOrdinalSU1(issue); }

    /** Pooled table of a live strategy's pending decision for `seq`, or null (then the mainline abstains). */
    public static synchronized float[][] pendingTable(String genomeKey, long seq) {
        Runtime rt = RT; if (rt == null || rt.pendingSeq != seq) return null;
        ModelLiveSU2 l = rt.lives.get(genomeKey); if (l == null || l.lastPooled == null || l.lastPooledT != seq - rt.genesisSeq) return null;
        float[][] out = new float[10][]; for (int p = 0; p < 10; p++) out[p] = l.lastPooled[p].clone(); return out;
    }

    /** Live set: SU2 league members plus current tier champions (bounded). */
    static List<ModelGenomeSU2> liveGenomes(SQLiteDatabase db) {
        List<ModelGenomeSU2> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT g.canonical_text FROM su2_model_genome g WHERE g.state='LEAGUE' AND g.origin_op<>'INCUMBENT' ORDER BY g.genome_key LIMIT 32", null)) {
            while (c.moveToNext()) out.add(ModelGenomeSU2.parse(c.getString(0)));
        }
        return out;
    }

    /** Rebuild the runtime from the retained history window (exact replay). */
    static Runtime genesis(List<ModelGenomeSU2> genomes, int[][] history, long firstSeq) {
        Runtime rt = new Runtime(); rt.genesisSeq = firstSeq;
        for (ModelGenomeSU2 g : genomes) rt.lives.put(g.key(), new ModelLiveSU2(g, rt.hub, START_META, WINDOW, REFIT));
        for (int[] d : history) { for (ModelLiveSU2 l : rt.lives.values()) { l.decide(); l.settle(d); } rt.hub.update(d); }
        rt.through = firstSeq + history.length - 1;
        return rt;
    }

    static synchronized Runtime ensure(DbStore store, EventFrameSU1 frame, long seq) {
        int[][] h = frame.history(); long first = sequence(String.valueOf(frame.issue(0))), last = sequence(String.valueOf(frame.issue(h.length - 1)));
        if (last != seq - 1) throw new IllegalStateException("SU2_LIVE_FRAME_NOT_CONTIGUOUS");
        for (int i = 1; i < h.length; i++) if (sequence(String.valueOf(frame.issue(i))) != first + i) throw new IllegalStateException("SU2_LIVE_HISTORY_GAP");
        List<ModelGenomeSU2> genomes = liveGenomes(store.readable());
        boolean rebuild = RT == null || RT.genesisSeq < first || seq - RT.genesisSeq > (long) h.length + REGENESIS_EVERY || !RT.lives.keySet().containsAll(keys(genomes));
        if (!rebuild && RT.through > seq - 1) rebuild = true;                 // source revision went backwards
        if (rebuild) {
            Runtime restored = restore(store.readable(), genomes, h, first);
            RT = restored != null ? restored : genesis(genomes, h, first);
        }
        for (long s = RT.through + 1; s <= seq - 1; s++) {                      // catch up missed settlements exactly
            int[] d = h[(int) (s - first)];
            for (ModelLiveSU2 l : RT.lives.values()) { l.decide(); l.settle(d); }
            RT.hub.update(d); RT.through = s;
        }
        return RT;
    }

    static Set<String> keys(List<ModelGenomeSU2> gs) { Set<String> s = new HashSet<>(); for (ModelGenomeSU2 g : gs) s.add(g.key()); return s; }

    /** Restore from the last checkpoint and replay forward; null if the checkpoint is unusable. */
    static Runtime restore(SQLiteDatabase db, List<ModelGenomeSU2> genomes, int[][] h, long first) {
        try {
            long[] through = new long[1]; byte[] hubBlob = ModelStoreSU2.liveBlob(db, "HUB", through);
            if (hubBlob == null) return null;
            DataInputStream hin = new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(hubBlob)));
            long genesisSeq = hin.readLong();
            if (genesisSeq < first) return null;                                   // checkpoint older than retained draws
            Runtime rt = new Runtime(); rt.genesisSeq = genesisSeq;
            for (ModelGenomeSU2 g : genomes) rt.lives.put(g.key(), new ModelLiveSU2(g, rt.hub, START_META, WINDOW, REFIT));
            rt.hub.readInto(hin);
            for (ModelLiveSU2 l : rt.lives.values()) {
                long[] t = new long[1]; byte[] b = ModelStoreSU2.liveBlob(db, l.g.key(), t);
                if (b == null || t[0] != through[0]) return null;
                l.readInto(new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(b))));
            }
            rt.through = through[0];
            return rt;
        } catch (IOException | RuntimeException unusable) { return null; }
    }

    static void checkpoint(SQLiteDatabase db, Runtime rt, long now) throws IOException {
        ByteArrayOutputStream hb = new ByteArrayOutputStream(); DataOutputStream ho = new DataOutputStream(hb);
        ho.writeLong(rt.genesisSeq); rt.hub.write(ho); ho.flush();
        ModelStoreSU2.saveLive(db, "HUB", rt.through, ModelStoreSU2.gzip(hb.toByteArray()), now);
        for (ModelLiveSU2 l : rt.lives.values()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b); l.write(o); o.flush();
            ModelStoreSU2.saveLive(db, l.g.key(), rt.through, ModelStoreSU2.gzip(b.toByteArray()), now);
        }
    }

    /** Before the draw. Returns the number of frozen decisions. */
    public static int capture(DbStore store, String target) {
        synchronized (CAPTURE_LOCK) { return captureSerial(store, target); }
    }

    private static int captureSerial(DbStore store, String target) {
        if (!StrategySchemaSU2.ready(store.readable())) return 0;
        ProgramModelSU2.register();
        long seq = sequence(target);
        EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, target);
        final Runtime rt;
        final Map<String, ModelLiveSU2.Decision> decisions = new LinkedHashMap<>();
        synchronized (ModelLiveLoopSU2.class) {
        rt = ensure(store, frame, seq);
        if (rt.pendingSeq == seq) return 0;
        long started = System.nanoTime();
        // rotate the start so a budget overrun never starves the same strategies (P08)
        List<Map.Entry<String, ModelLiveSU2>> order = new ArrayList<>(rt.lives.entrySet());
        if (!order.isEmpty()) Collections.rotate(order, (int) (seq % order.size()));
        for (Map.Entry<String, ModelLiveSU2> e : order) {
            if (System.nanoTime() - started > PREDRAW_BUDGET_NANOS || SettlementExecutionGateR819.isActive()) break;   // rest are MISSING
            decisions.put(e.getKey(), e.getValue().decide());
        }
        } // Release the model monitor before waiting on the single DB writer.
        final long open = frame.targetOpenTime;
        int frozen = store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_LIVE_FREEZE", db -> {
            if (!EventFrameBuilderSU1.unchanged(store, frame)) return 0;
            long now = System.currentTimeMillis(); if (now >= open) return 0;      // never freeze after open
            int n = 0; for (Map.Entry<String, ModelLiveSU2.Decision> e : decisions.entrySet()) { ModelStoreSU2.insertDecision(db, e.getKey(), seq, e.getValue(), now, open); n++; }
            return n;
        });
        synchronized (ModelLiveLoopSU2.class) {
            if (frozen > 0 && RT == rt && rt.through == seq - 1) rt.pendingSeq = seq;
        }
        return frozen;
    }

    /** Inside the original settlement owner transaction (same place as LiveShadowLoopSU1.settleWithin). */
    public static synchronized int settleWithin(SQLiteDatabase db, Models.Draw draw, long completedAt) throws IOException {
        if (!StrategySchemaSU2.ready(db)) return 0;
        long seq = sequence(draw.issue);
        int[] d = new int[10]; for (int p = 0; p < 10; p++) d[p] = draw.getNumberAtPosition(p + 1);
        int labelled = ModelStoreSU2.settle(db, seq, d, completedAt);
        Runtime rt = RT;
        if (rt != null && rt.pendingSeq == seq && rt.through == seq - 1) {
            for (ModelLiveSU2 l : rt.lives.values()) { if (!l.hasPending()) l.decide(); l.settle(d); }   // MISSING ones still train
            rt.hub.update(d); rt.through = seq; rt.pendingSeq = -1;
            if (seq % CHECKPOINT_EVERY == 0) checkpoint(db, rt, completedAt);
        }
        // a process restart between capture and settle simply catches up from history at the next capture
        return labelled;
    }
}
