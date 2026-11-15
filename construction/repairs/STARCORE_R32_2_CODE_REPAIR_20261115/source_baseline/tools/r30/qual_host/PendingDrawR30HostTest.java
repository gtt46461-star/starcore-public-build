package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** R30-B02-03 HOST test on real SQLite with production LiveShadowsR27/QualificationInboxR29 code:
 *   P1 a frozen period whose draw is missing becomes one PENDING_DRAW row; an immediate second pass does not recheck it
 *      (recheck backoff 1,2,4,...,300 s from the checks count);
 *   P2 the draw arrives three periods later: the original freeze is settled exactly once (hit stays, later passes
 *      settle 0 rows) and the pending row is resolved;
 *   P3 a row open for more than 48 h (or >2880 checks) is STALE: not rechecked, kept, listed by diagnostics;
 *   P4 the backoff sequence is 1,2,4,8,...,256,300,300 s. Run by tools/r30/ci_build.sh (sub-card R30-B02-03-a). */
public final class PendingDrawR30HostTest {
    static int fails;
    static void check(String n, boolean ok, String d) { System.out.println("R30_PENDING|" + n + "|" + (ok ? "OK" : "FAILED") + "|" + d); if (!ok) fails++; }
    static long scalar(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : -999; } }
    static void commit(SQLiteDatabase db, ResearchWriteQueueR27.Batch b) throws Exception {
        db.jdbc.setAutoCommit(false);
        try { ResearchWriteQueueR27.execute(db, b, "LIVE_SETTLE"); db.jdbc.commit(); } catch (Exception e) { db.jdbc.rollback(); throw e; } finally { db.jdbc.setAutoCommit(true); }
    }
    static long pass(SQLiteDatabase db, long latest) throws Exception {
        long[] k = new long[1]; for (ResearchWriteQueueR27.Batch b : LiveShadowsR27.settleChunks(db, latest, k, LiveShadowsR27.SETTLE_CHUNK_R30)) commit(db, b); return k[0];
    }
    static void draw(SQLiteDatabase db, int o) {
        db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,1,2,3,4,5,6,7,8,9,10,0)", new Object[] {"I" + (100000 + o)});
    }
    public static void main(String[] a) throws Exception {
        Class.forName("org.sqlite.JDBC");
        SQLiteDatabase db = QualInboxR29HostTest.open();
        for (int o = 1; o <= 6; o++) {
            db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit) VALUES('P',?,?,1,x'00',1,4,7,1,'FROZEN',?,?,-1)",
                    new Object[] {(long) o, "I" + (100000 + o), (long) o * 1000, (long) o * 1000 + 500});
            if (o != 2) draw(db, o);
        }
        pass(db, 3);
        long rows = scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE target_ordinal=2 AND resolved_at=0"), checks1 = scalar(db, "SELECT checks FROM r29_pending_draw WHERE target_ordinal=2");
        ResearchWriteQueueR27.Batch again = QualificationInboxR29.pendingDraws(db, 3);
        int rechecks = 0; for (ResearchWriteQueueR27.Op op : again.ops) if (op.sql.contains("checks=checks+1")) rechecks++;
        check("P1_PENDING_ROW_AND_BACKOFF_NO_RECHECK", rows == 1 && checks1 == 1 && rechecks == 0, "rows=" + rows + " checks=" + checks1 + " immediateRechecks=" + rechecks);
        pass(db, 4); pass(db, 5);
        draw(db, 2); // backfill arrives three periods later
        long settledNow = pass(db, 6);
        long hit2 = scalar(db, "SELECT hit FROM r27_live_frozen WHERE target_ordinal=2"), settledAt = scalar(db, "SELECT settled_at FROM r27_live_frozen WHERE target_ordinal=2");
        long resolved = scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE target_ordinal=2 AND resolved_at>0");
        long later = pass(db, 6) + pass(db, 6);
        check("P2_LATE_DRAW_SETTLES_ORIGINAL_FREEZE_ONCE", hit2 == 1 && settledNow >= 1 && later == 0 && resolved == 1
                && scalar(db, "SELECT settled_at FROM r27_live_frozen WHERE target_ordinal=2") == settledAt, "hit=" + hit2 + " laterPasses=" + later + " resolved=" + resolved);
        db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit) VALUES('P',7,'I100007',1,x'00',1,4,7,1,'FROZEN',7000,7500,-1)");
        long old = System.currentTimeMillis() - QualificationInboxR29.PENDING_STALE_AGE_MS - 1000;
        db.execSQL("INSERT INTO r29_pending_draw(target_ordinal,target_issue,rows,act_rows,first_seen_at,last_checked_at,checks) VALUES(7,'I100007',1,1,?,?,40)", new Object[] {old, old});
        ResearchWriteQueueR27.Batch staleBatch = QualificationInboxR29.pendingDraws(db, 9);
        boolean touches7 = false; for (ResearchWriteQueueR27.Op op : staleBatch.ops) for (Object x : op.args) if (Long.valueOf(7).equals(x)) touches7 = true;
        Object staleCount = QualificationInboxR29.diagnostics(db).get("pendingDrawStale");
        check("P3_STALE_KEPT_NOT_RECHECKED_LISTED", !touches7 && Long.valueOf(1).equals(staleCount) && scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE target_ordinal=7") == 1, "stale=" + staleCount + " rechecked=" + touches7);
        StringBuilder seq = new StringBuilder(); for (int c = 1; c <= 11; c++) seq.append(QualificationInboxR29.nextCheckDelayMs(c) / 1000).append(',');
        check("P4_BACKOFF_SEQUENCE", seq.toString().equals("1,2,4,8,16,32,64,128,256,300,300,"), seq.toString());
        System.out.println("R30_PENDING|SUMMARY|" + (fails == 0 ? "OK" : "FAILED"));
        if (fails > 0) System.exit(1);
    }
}
