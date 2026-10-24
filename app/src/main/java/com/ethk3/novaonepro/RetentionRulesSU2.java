package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Bounded growth for the tables that grew per period without a TTL (P33).
 * Evidence from the 5.4.3 report: research_row_revision_v2 184,964 rows and
 * v57_runtime_event_r926 72,883 rows for only 4,995 draws (3.5 days).
 *
 *  - SU2 live decisions: raw rows kept 7 days (10,080 periods), then folded into
 *    su2_live_daily (one row per strategy per day) - league scoring needs 3,000.
 *  - SU2 lineage/genomes: RESEARCH genomes older than 30 campaigns and not in the
 *    hall of fame / league are deleted; lineage rows of deleted children go with them.
 *  - runtime events: identical (stage, target, detail) repeats are collapsed into one
 *    row with first/last/count by the writer (patch to the R926 writer); rows older
 *    than 7 days leave through the existing archive protocol (permit + CAS), whose
 *    CAS files are themselves capped by su2_storage_budget RESEARCH_CAS.
 * Each pass deletes at most 2,000 rows inside one short writer transaction.
 */
public final class RetentionRulesSU2 {
    private RetentionRulesSU2() {}
    public static final int RAW_DECISION_PERIODS = 10080, BATCH = 2000;

    static int changes(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT changes()", null)) { return c.moveToFirst() ? c.getInt(0) : 0; } }

    public static int foldDecisions(SQLiteDatabase db, long currentSequence, long now) {
        if (!db.inTransaction()) throw new IllegalStateException("SU2_RETENTION_WRITER");
        long cut = currentSequence - RAW_DECISION_PERIODS;
        db.execSQL("INSERT OR IGNORE INTO su2_live_daily(genome_key,day_key,periods,actions,hits,closed_at) "
            + "SELECT genome_key,CAST(sequence_ordinal/1440 AS INTEGER),count(*),sum(act),sum(CASE WHEN act=1 THEN hit ELSE 0 END),? FROM su2_live_decision "
            + "WHERE sequence_ordinal<? AND hit IS NOT NULL GROUP BY genome_key,CAST(sequence_ordinal/1440 AS INTEGER)", new Object[]{now, cut});
        // WITHOUT ROWID table and minSdk-21 SQLite (no row values): delete a bounded range of the oldest periods
        db.execSQL("DELETE FROM su2_live_decision WHERE hit IS NOT NULL AND sequence_ordinal<? AND sequence_ordinal<(SELECT min(sequence_ordinal)+60 FROM su2_live_decision)", new Object[]{cut});
        return changes(db);
    }

    public static int pruneResearchGenomes(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("SU2_RETENTION_WRITER");
        db.execSQL("DELETE FROM su2_lineage WHERE child_key IN (SELECT genome_key FROM su2_model_genome WHERE state='RESEARCH' AND born_campaign NOT IN (SELECT campaign_id FROM su2_campaign ORDER BY started_at DESC LIMIT 30) LIMIT " + BATCH + ")");
        db.execSQL("DELETE FROM su2_model_genome WHERE genome_key IN (SELECT genome_key FROM su2_model_genome WHERE state='RESEARCH' AND born_campaign NOT IN (SELECT campaign_id FROM su2_campaign ORDER BY started_at DESC LIMIT 30) LIMIT " + BATCH + ")");
        return changes(db);
    }
}
