package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.IOException;
import java.util.*;

/**
 * Fast historical creation on the phone (P05/P07/P20-P25): one slice = one
 * generation of the continuing evolution on the retained history, run by the
 * existing burst scheduler in quiet windows (ReplayBurstSchedulerSU1). When a
 * campaign completes, its frequency-tier champions join the league and go live
 * immediately (bootstrapped from history), and the evolution state is kept for
 * the next campaign - nothing restarts, parents are never discarded.
 */
public final class ModelFoundryJobSU2 {
    private ModelFoundryJobSU2() {}
    public static final int GENERATIONS = 8, POPULATION = 24, HALL_OF_FAME = 12, VAL_LEN = 1200, REFRESH_EVERY = 300;
    public static final long MODEL_BYTES = 96L << 20;

    /** Returns a short status for the dashboard; never throws into the scheduler. */
    public static String slice(DbStore store, String target) {
        try {
            if (SettlementExecutionGateR819.isActive() || RealtimePressureGateR811.hasActiveCriticalWork()) return "SU2_FOUNDRY_YIELD_REALTIME";
            SQLiteDatabase r = store.readable();
            if (!StrategySchemaSU2.ready(r)) return "SU2_FOUNDRY_SCHEMA_PENDING";
            EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, target);
            int[][] h = frame.history();
            if (h.length < 3400) return "SU2_FOUNDRY_HISTORY_SHORT:" + h.length;
            long through = TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(frame.sourceMaxIssue));
            String campaign = running(r);
            if (campaign == null) {
                long lastDone = lastThrough(r);
                if (lastDone >= 0 && through - lastDone < REFRESH_EVERY) return "SU2_FOUNDRY_WAIT_REFRESH";
                campaign = HashingSU1.fields("SU2_CAMPAIGN_V1", String.valueOf(through), frame.sourceVersionHash).substring(0, 32);
                final String id = campaign; final int T = h.length;
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CAMPAIGN_BEGIN", db -> {
                    db.execSQL("INSERT OR IGNORE INTO su2_campaign(campaign_id,through_sequence,dev_start,dev_end,val_start,val_end,generations,population,status,started_at) VALUES(?,?,?,?,?,?,?,?,'RUNNING',?)",
                        new Object[]{id, through, 1500, T - VAL_LEN, T - VAL_LEN, T, GENERATIONS, POPULATION, System.currentTimeMillis()});
                    return 0;
                });
            }
            int done = generationsDone(r, campaign);
            ModelReplaySU2.Library lib = new ModelReplaySU2.Library(h, MODEL_BYTES);
            ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, seed(campaign, done), 1500, 2500, 250);
            ModelEvolutionSU2.State s = ModelStoreSU2.loadEvolution(r);
            ModelEvolutionSU2.Eras e = new ModelEvolutionSU2.Eras(1500, h.length - VAL_LEN, h.length - VAL_LEN, h.length);
            s = evo.campaign(s, e, 1, POPULATION, HALL_OF_FAME);
            final ModelEvolutionSU2.State fs = s; final String id = campaign; final boolean finished = done + 1 >= GENERATIONS;
            final long now = System.currentTimeMillis();
            final List<double[]> scores = new ArrayList<>();
            for (ModelEvolutionSU2.Ranked x : s.population) scores.add(new double[]{evo.dev(x.g, e), evo.val(x.g, e)});
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CAMPAIGN_SLICE", db -> {
                for (int i = 0; i < fs.population.size(); i++) ModelStoreSU2.saveGenome(db, fs.population.get(i), id, through, scores.get(i)[0], scores.get(i)[1], "RESEARCH", now);
                for (ModelEvolutionSU2.Ranked x : fs.hallOfFame) ModelStoreSU2.saveGenome(db, x, id, through, evo.dev(x.g, e), evo.val(x.g, e), "HALL_OF_FAME", now);
                ModelStoreSU2.saveEvolution(db, id, fs, now);
                db.execSQL("UPDATE su2_campaign SET evaluated=evaluated+?,effective_generations=? WHERE campaign_id=?", new Object[]{fs.population.size(), fs.effectiveGenerations, id});
                if (finished) {
                    for (Map.Entry<String, ModelEvolutionSU2.Ranked> t : fs.tiers.entrySet()) {
                        ModelEvolutionSU2.Ranked x = t.getValue();
                        ModelStoreSU2.saveGenome(db, x, id, through, evo.dev(x.g, e), evo.val(x.g, e), "LEAGUE", now);
                        ModelStoreSU2.joinLeague(db, "SU2:" + x.g.key(), "SU2", t.getKey() + "@" + through, x.g.key(), through + 1);
                    }
                    db.execSQL("UPDATE su2_campaign SET status='DONE',finished_at=? WHERE campaign_id=? AND status='RUNNING'", new Object[]{now, id});
                }
                return 0;
            });
            return (finished ? "SU2_CAMPAIGN_DONE:" : "SU2_GENERATION_DONE:") + id + "|gen=" + (done + 1) + "|effective=" + fs.effectiveGenerations
                + "|champion=" + (fs.champion == null ? "" : fs.champion.g.formula());
        } catch (IOException | RuntimeException failure) {
            return "SU2_FOUNDRY_FAILED:" + failure.getClass().getSimpleName() + ":" + String.valueOf(failure.getMessage());
        }
    }

    static long seed(String campaign, int generation) { return Long.parseLong(campaign.substring(0, 15), 16) ^ (0x9E3779B97F4A7C15L * (generation + 1)); }
    static String running(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT campaign_id FROM su2_campaign WHERE status='RUNNING' ORDER BY started_at DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getString(0) : null; } }
    static long lastThrough(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT max(through_sequence) FROM su2_campaign WHERE status='DONE'", null)) { return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : -1; } }
    static int generationsDone(SQLiteDatabase db, String campaign) {
        try (Cursor c = db.rawQuery("SELECT evaluated,population FROM su2_campaign WHERE campaign_id=?", new String[]{campaign})) { return c.moveToFirst() ? c.getInt(0) / Math.max(1, c.getInt(1)) : 0; }
    }
}
