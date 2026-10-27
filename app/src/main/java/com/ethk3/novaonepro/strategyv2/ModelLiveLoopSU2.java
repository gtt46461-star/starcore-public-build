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
        final LinkedHashMap<String, LiveStrategySU2> lives = new LinkedHashMap<>();   // programs and R14 committees
        long pendingSeq = -1;
    }
    private static Runtime RT;
    private static final Object CAPTURE_LOCK = new Object();

    static long sequence(String issue) { return TargetIssueResolver.sequenceOrdinalSU1(issue); }

    /** Pooled table of a live strategy's pending decision for `seq`, or null (then the mainline abstains). */
    public static float[][] pendingTable(String genomeKey,long seq){return LegacyReplayR26.pendingTable(genomeKey,seq);}

    /** P50 memory budget: at most 32 live strategies and 48 distinct calibrated members (~1 MB calibration ring each). */
    public static final int MAX_LIVE = 32, MAX_HUB_CALIBRATORS = 48;

    /**
     * Live set: SU2 league members - programs and R14 committees; the XI incumbent runs in IncumbentLoopSU2. P50: the
     * current mainline member first, then the newest members, while the hub stays within MAX_HUB_CALIBRATORS
     * calibration rings (RAM and the HUB checkpoint stay bounded however many champions the league accumulates).
     */
    static List<String> liveGenomes(SQLiteDatabase db) {
        List<String> cand = new ArrayList<>(); String main = null;
        try (Cursor c = db.rawQuery("SELECT member_id FROM su2_mainline_choice ORDER BY from_sequence DESC LIMIT 1", null)) { if (c.moveToFirst()) main = c.getString(0); }
        try (Cursor c = db.rawQuery("SELECT g.canonical_text,'SU2:'||g.genome_key FROM su2_model_genome g JOIN su2_league l ON l.genome_key=g.genome_key WHERE g.state='LEAGUE' AND g.origin_op<>'INCUMBENT' AND g.origin_op<>'R26_LIVE' AND l.state='ACTIVE' AND NOT EXISTS(SELECT 1 FROM r26_prepared_policy p WHERE p.policy_hash=g.genome_key) AND NOT EXISTS(SELECT 1 FROM r26_live_model a WHERE a.policy_hash=g.genome_key) ORDER BY CASE WHEN l.member_id=? THEN 0 ELSE 1 END,g.born_sequence DESC,g.genome_key LIMIT 96", new String[]{main == null ? "" : main})) {
            while (c.moveToNext()) {
                String text = c.getString(0);
                if (StageStrategySU2.isStage(text)) StageStoreSU2.load(db, text);
                if (c.getString(1).equals(main)) cand.add(0, text); else cand.add(text);
            }
        }
        int availableSlots=MAX_LIVE;
        if(RuntimeSchemaR26.ready(db))try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'",null)){if(c.moveToFirst())availableSlots=Math.max(0,MAX_LIVE-c.getInt(0));}
        List<String> out = new ArrayList<>(); Set<String> cals = new HashSet<>();
        for (String text : cand) {
            if (out.size() >= availableSlots) break;
            Set<String> mine = calibratedMembers(text); Set<String> union = new HashSet<>(cals); union.addAll(mine);
            if (union.size() > MAX_HUB_CALIBRATORS) continue;
            cals = union; out.add(text);
        }
        return out;
    }

    static Set<String> calibratedMembers(String text) {
        if (StageStrategySU2.isStage(text)) return StageStrategySU2.definition(text).calibrators();
        Set<String> out = new HashSet<>();
        List<ModelGenomeSU2> gs = CommitteeSU2.isCommittee(text) ? CommitteeSU2.parse(text).subs : Collections.singletonList(ModelGenomeSU2.parse(text));
        for (ModelGenomeSU2 g : gs) if (g.calibrated) for (ModelGenomeSU2.Member m : g.members) out.add(m.modelKey());
        return out;
    }

    static String keyOf(String text) { return StageStrategySU2.isStage(text) ? StageStrategySU2.keyOf(text) : CommitteeSU2.isCommittee(text) ? CommitteeSU2.parse(text).key() : ModelGenomeSU2.parse(text).key(); }

    static LiveStrategySU2 make(String text, ModelLiveSU2.Hub hub, long genesis) {
        if (StageStrategySU2.isStage(text)) return new StageStrategySU2(StageStrategySU2.definition(text), hub, genesis, START_META, WINDOW, REFIT);
        return CommitteeSU2.isCommittee(text) ? new CommitteeLiveSU2(CommitteeSU2.parse(text), hub, START_META, WINDOW, REFIT)
            : new ModelLiveSU2(ModelGenomeSU2.parse(text), hub, START_META, WINDOW, REFIT);
    }

    /** Rebuild the runtime from the retained history window (exact replay). */
    static Runtime genesis(List<String> genomes, int[][] history, long firstSeq) {
        Runtime rt = new Runtime(); rt.genesisSeq = firstSeq;
        for (String g : genomes) rt.lives.put(keyOf(g), make(g, rt.hub, firstSeq));
        for (int[] d : history) { for (LiveStrategySU2 l : rt.lives.values()) { l.decide(); l.settle(d); } rt.hub.update(d); }
        rt.through = firstSeq + history.length - 1;
        return rt;
    }

    static synchronized Runtime ensure(DbStore store, EventFrameSU1 frame, long seq) {
        int[][] h = frame.history(); long first = sequence(String.valueOf(frame.issue(0))), last = sequence(String.valueOf(frame.issue(h.length - 1)));
        if (last != seq - 1) throw new IllegalStateException("SU2_LIVE_FRAME_NOT_CONTIGUOUS");
        for (int i = 1; i < h.length; i++) if (sequence(String.valueOf(frame.issue(i))) != first + i) throw new IllegalStateException("SU2_LIVE_HISTORY_GAP");
        List<String> genomes = liveGenomes(store.readable());
        boolean rebuild = RT == null || RT.genesisSeq < first || seq - RT.genesisSeq > (long) h.length + REGENESIS_EVERY || !RT.lives.keySet().equals(keys(genomes));
        if (!rebuild && RT.through > seq - 1) rebuild = true;                 // source revision went backwards
        if (rebuild) {
            Runtime restored = restore(store.readable(), genomes, h, first);
            RT = restored != null ? restored : genesis(genomes, h, first);
        }
        for (long s = RT.through + 1; s <= seq - 1; s++) {                      // catch up missed settlements exactly
            int[] d = h[(int) (s - first)];
            for (LiveStrategySU2 l : RT.lives.values()) { l.decide(); l.settle(d); }
            RT.hub.update(d); RT.through = s;
        }
        return RT;
    }

    static Set<String> keys(List<String> gs) { Set<String> s = new HashSet<>(); for (String g : gs) s.add(keyOf(g)); return s; }

    /** Restore from the last checkpoint and replay forward; null if the checkpoint is unusable. */
    static Runtime restore(SQLiteDatabase db, List<String> genomes, int[][] h, long first) {
        try {
            long[] through = new long[1]; byte[] hubBlob = ModelStoreSU2.liveBlob(db, "HUB", through);
            if (hubBlob == null) return null;
            DataInputStream hin = new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(hubBlob)));
            long genesisSeq = hin.readLong();
            if (genesisSeq < 1 || through[0] < genesisSeq-1) return null;                                   // checkpoint older than retained draws
            Runtime rt = new Runtime(); rt.genesisSeq = genesisSeq;
            for (String g : genomes) rt.lives.put(keyOf(g), make(g, rt.hub, genesisSeq));
            rt.hub.readModelsInto(hin);                                            // P50: calibration rings are separate rows
            for (String k : rt.hub.calKeys()) {
                long[] ct = new long[1]; byte[] cb = ModelStoreSU2.liveBlob(db, ModelLiveSU2.Hub.calOwner(k), ct);
                if (cb == null || ct[0] != through[0]) return null;
                rt.hub.readCal(k, new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(cb))));
            }
            for (LiveStrategySU2 l : rt.lives.values()) {
                long[] t = new long[1]; byte[] b = ModelStoreSU2.liveBlob(db, l.key(), t);
                if (b == null || t[0] != through[0]) return null;
                l.readInto(new DataInputStream(new ByteArrayInputStream(ModelStoreSU2.gunzip(b))));
            }
            rt.through = through[0];
            return rt;
        } catch (IOException | RuntimeException unusable) { return null; }
    }

    static void checkpoint(SQLiteDatabase db, Runtime rt, long now) throws IOException {
        ByteArrayOutputStream hb = new ByteArrayOutputStream(); DataOutputStream ho = new DataOutputStream(hb);
        ho.writeLong(rt.genesisSeq); rt.hub.writeModels(ho); ho.flush();
        ModelStoreSU2.saveLive(db, "HUB", rt.through, ModelStoreSU2.gzip(hb.toByteArray()), now);
        for (String k : rt.hub.calKeys()) {                                        // P50: one row per calibration ring (each ~1 MB < 16 MB)
            ByteArrayOutputStream cb = new ByteArrayOutputStream(); DataOutputStream co = new DataOutputStream(cb); rt.hub.writeCal(k, co); co.flush();
            ModelStoreSU2.saveLive(db, ModelLiveSU2.Hub.calOwner(k), rt.through, ModelStoreSU2.gzip(cb.toByteArray()), now);
        }
        for (LiveStrategySU2 l : rt.lives.values()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b); l.write(o); o.flush();
            ModelStoreSU2.saveLive(db, l.key(), rt.through, ModelStoreSU2.gzip(b.toByteArray()), now);
        }
    }

    /** Before the draw. Returns the number of frozen decisions. */
    public static int capture(DbStore store,String target){return LegacyReplayR26.capture(store,target);}

    /** Inside the original settlement owner transaction (same place as LiveShadowLoopSU1.settleWithin). */
    public static synchronized int settleWithin(SQLiteDatabase db, Models.Draw draw, long completedAt) throws IOException {
        if (!StrategySchemaSU2.ready(db)) return 0;
        long seq = sequence(draw.issue);
        int[] d = new int[10]; for (int p = 0; p < 10; p++) d[p] = draw.getNumberAtPosition(p + 1);
        int labelled = ModelStoreSU2.settle(db, seq, d, completedAt);
        // Numerical learning runs only in the fenced P2 replay queue after the sole owner commits feedback.
        RuntimeCoordinatorR26.wake();
        return labelled;
    }
}
