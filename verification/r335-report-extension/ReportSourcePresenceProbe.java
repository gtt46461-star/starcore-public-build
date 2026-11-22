package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/** Exercise the signed provider's predicate on a private new DB, a stale installed journal, and a reopened read-only DB. */
public final class ReportSourcePresenceProbe {
    private static final String[] TABLES = {
        "r952_research_outbox", "r952_candidate", "r952_candidate_funnel", "r952_learning_cursor",
        "r952_artifact", "research_historical_genome_archive_r967", "research_historical_genome_use_r967",
        "research_exact_probability_f05"
    };
    private static void require(boolean b,String msg) { if (!b) throw new AssertionError(msg); }
    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        require(!f.exists(), "Start with a private new database");
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(f, null)) {
            require(!ReportSchemaSourceProviderR335.sourcesCommitted(db),"Empty catalog");
            try (Cursor rows = db.rawQuery("SELECT COUNT(*) FROM main.sqlite_master WHERE name NOT LIKE 'android_%'",null)) {
                require(rows.moveToFirst() && rows.getInt(0)==0,"Readiness must not provision tables");
            }
            String journal=SchemaDomainCoordinatorR926.TABLE_JOURNAL;
            db.execSQL("CREATE TABLE "+journal+"(domain TEXT, revision INTEGER, step_id TEXT, status TEXT)");
            SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
            ContentValues v = new ContentValues();
            v.put("domain",contract.domain); v.put("revision",contract.revision);
            v.put("step_id","R952_EXTENSION"); v.put("status","STARTED");
            db.insertOrThrow(journal,null,v);
            for(int i=0;i<TABLES.length-1;i++) db.execSQL("CREATE TABLE "+TABLES[i]+"(id)");
            require(!ReportSchemaSourceProviderR335.sourcesCommitted(db),"Uncommitted owner");
            db.execSQL("UPDATE "+journal+" SET status='COMPLETED'");
            require(!ReportSchemaSourceProviderR335.sourcesCommitted(db),"Journal cannot replace a missing source");
            db.beginTransaction();
            try { db.execSQL("CREATE TABLE "+TABLES[TABLES.length-1]+"(id)"); }
            finally { db.endTransaction(); }
            require(!ReportSchemaSourceProviderR335.sourcesCommitted(db),"Rolled back source");
            db.execSQL("CREATE TABLE "+TABLES[TABLES.length-1]+"(id)");
            require(ReportSchemaSourceProviderR335.sourcesCommitted(db),"Committed sources");
        }
        try(SQLiteDatabase reopened=SQLiteDatabase.openDatabase(f.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            require(ReportSchemaSourceProviderR335.sourcesCommitted(reopened),"Reopened read-only database");
        }
        ReportSourceJournalR335Test journal = new ReportSourceJournalR335Test();
        journal.installedReceiptWithoutPreparationKeepsOriginalSnapshotIdentity();
        journal.preparationObservationDoesNotChangeReportVerdict();
        System.out.println("REPORT_SOURCE_PRESENCE_PROBE_PASS");
    }
}
