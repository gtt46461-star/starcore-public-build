package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** R30-B01-04 HOST test on real SQLite (sqlite-jdbc + tools/r29/qual_host shim), production LiveShadowsR27 code:
 *   S1 640 settleable rows are split into 4 transactions of 160 rows;
 *   S2 the chunked settlement leaves r27_live_frozen and r27_opportunity row-for-row equal to the pre-R30 single
 *      batch (full-history r27_opportunity recount), on the same data set;
 *   S3 a process death after chunk 2 rolls that chunk back; the next pass settles the rest exactly once and the
 *      final tables equal the reference (hit IN(-1,-2) keeps every UPDATE idempotent between chunks).
 * Run by tools/r30/ci_build_r30.sh (sub-card R30-B01-04-a). Synthetic data checks mechanics only. */
public final class SettleChunkR30HostTest {
    static final List<String> FAIL = new ArrayList<>();
    static void check(String name, boolean ok, String detail) {
        System.out.println("R30_SETTLE|" + name + "|" + (ok ? "OK" : "FAILED") + "|" + detail);
        if (!ok) FAIL.add(name);
    }
    static final int POLICIES = 8, ORDINALS = 80; // 640 settleable rows

    static SQLiteDatabase world(long seed) throws Exception {
        SQLiteDatabase db = QualInboxR29HostTest.open(); Random r = new Random(seed);
        for (int o = 1; o <= ORDINALS; o++) {
            int[] d = new int[10]; List<Integer> p = new ArrayList<>(); for (int i = 1; i <= 10; i++) p.add(i); java.util.Collections.shuffle(p, r);
            for (int i = 0; i < 10; i++) d[i] = p.get(i);
            db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,0)", new Object[] {"I" + (100000 + o), d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]});
            db.execSQL("INSERT INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,?,?,0,0,0)", new Object[] {(long) o, "I" + (100000 + o), POLICIES, POLICIES, 0});
            for (int k = 0; k < POLICIES; k++) {
                double x = r.nextDouble(); String st = x < 0.9 ? "FROZEN" : "DEADLINE_MISSED";
                db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit) VALUES(?,?,?,?,x'00',?,?,?,?,?,?,?,?)",
                        new Object[] {"P" + k, (long) o, "I" + (100000 + o), 1 + r.nextInt(10), 1 + r.nextInt(10), 1 + r.nextInt(10), 1 + r.nextInt(10), r.nextInt(2), st, (long) o * 1000, (long) o * 1000 + 500, r.nextDouble() < 0.1 ? -2 : -1});
            }
        }
        return db;
    }
    static void commit(SQLiteDatabase db, ResearchWriteQueueR27.Batch b, boolean die) throws Exception {
        db.jdbc.setAutoCommit(false);
        try { ResearchWriteQueueR27.execute(db, b, "LIVE_SETTLE"); if (die) throw new QualInboxR29HostTest.Killed(); db.jdbc.commit(); }
        catch (Exception e) { db.jdbc.rollback(); throw e; }
        finally { db.jdbc.setAutoCommit(true); }
    }
    /** pre-R30 batch: per-row outcome UPDATE + one full-history r27_opportunity recount (bb607cf settleBatch). */
    static void legacy(SQLiteDatabase db, long latest) throws Exception {
        long[] k = new long[1]; ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        for (ResearchWriteQueueR27.Batch c : LiveShadowsR27.settleChunks(db, latest, k, Integer.MAX_VALUE))
            for (ResearchWriteQueueR27.Op op : c.ops) if (!op.sql.startsWith("UPDATE r27_opportunity")) b.ops.add(op);
        if (k[0] > 0) b.add("UPDATE r27_opportunity SET settled=(SELECT count(*) FROM r27_live_frozen f WHERE f.target_ordinal=r27_opportunity.target_ordinal AND f.hit>=0) WHERE target_ordinal<=?", latest);
        commit(db, b, false);
    }
    static String dump(SQLiteDatabase db) {
        StringBuilder s = new StringBuilder();
        try (Cursor c = db.rawQuery("SELECT policy_hash,target_ordinal,hit FROM r27_live_frozen ORDER BY policy_hash,target_ordinal", null)) { while (c.moveToNext()) s.append(c.getString(0)).append(':').append(c.getLong(1)).append('=').append(c.getLong(2)).append(';'); }
        s.append('|');
        try (Cursor c = db.rawQuery("SELECT target_ordinal,settled FROM r27_opportunity ORDER BY target_ordinal", null)) { while (c.moveToNext()) s.append(c.getLong(0)).append('=').append(c.getLong(1)).append(';'); }
        return s.toString();
    }
    static long scalar(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : -999; } }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        for (long seed = 1; seed <= 3; seed++) {
            SQLiteDatabase ref = world(seed); legacy(ref, ORDINALS);
            SQLiteDatabase a = world(seed); long[] k = new long[1];
            List<ResearchWriteQueueR27.Batch> chunks = LiveShadowsR27.settleChunks(a, ORDINALS, k, LiveShadowsR27.SETTLE_CHUNK_R30);
            long outcome = 0; boolean sized = chunks.size() == 4;
            for (ResearchWriteQueueR27.Batch c : chunks) { long n = 0; for (ResearchWriteQueueR27.Op op : c.ops) if (op.sql.startsWith("UPDATE r27_live_frozen")) n++; outcome += n; sized &= n == 160; }
            check("S1_640_ROWS_FOUR_CHUNKS_seed" + seed, sized && outcome == 640 && k[0] == 640, "chunks=" + chunks.size() + " rows=" + outcome);
            for (ResearchWriteQueueR27.Batch c : chunks) commit(a, c, false);
            check("S2_EQUALS_PRE_R30_seed" + seed, dump(a).equals(dump(ref)), "settled=" + scalar(a, "SELECT count(*) FROM r27_live_frozen WHERE hit>=0"));

            SQLiteDatabase b = world(seed); k = new long[1];
            chunks = LiveShadowsR27.settleChunks(b, ORDINALS, k, LiveShadowsR27.SETTLE_CHUNK_R30);
            boolean died = false;
            try { for (int i = 0; i < chunks.size(); i++) commit(b, chunks.get(i), i == 1); } catch (QualInboxR29HostTest.Killed expected) { died = true; }
            long afterDeath = scalar(b, "SELECT count(*) FROM r27_live_frozen WHERE hit>=0");
            long[] k2 = new long[1]; List<ResearchWriteQueueR27.Batch> resume = LiveShadowsR27.settleChunks(b, ORDINALS, k2, LiveShadowsR27.SETTLE_CHUNK_R30);
            for (ResearchWriteQueueR27.Batch c : resume) commit(b, c, false);
            long[] k3 = new long[1]; LiveShadowsR27.settleChunks(b, ORDINALS, k3, LiveShadowsR27.SETTLE_CHUNK_R30);
            check("S3_KILL_AFTER_CHUNK2_RESUMES_ONCE_seed" + seed, died && afterDeath == 160 && k2[0] == 480 && k3[0] == 0 && dump(b).equals(dump(ref)),
                    "settledAfterDeath=" + afterDeath + " resumed=" + k2[0] + " leftover=" + k3[0]);
        }
        System.out.println("R30_SETTLE|SUMMARY|" + (FAIL.isEmpty() ? "OK" : "FAILED " + FAIL));
        if (!FAIL.isEmpty()) System.exit(1);
    }
}
