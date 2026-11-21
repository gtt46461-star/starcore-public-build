package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.TargetIssueResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class R335DatasetJournalAndroidTest {
    @Test public void strictlyOutsideRollingChangesSurviveAndKnowledgeTimeCorrectionsInvalidate() {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)");
                db.execSQL("CREATE TABLE r27_history_import(issue TEXT,ordinal INTEGER,numbers TEXT,status TEXT)");
                db.execSQL("CREATE TABLE r30r1_analysis_run(run_id TEXT PRIMARY KEY,status TEXT)");
                for(String sql:SchemaR27.sourceRevisionDefinitionsR31())db.execSQL(sql);
                db.execSQL("INSERT INTO r30_live_source_revision VALUES(1,0)");
                ResearchSourceJournalR31.ensureSchemaR31(db);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            db.execSQL("INSERT INTO draws(issue,first_seen_at) VALUES('202611210818',100),('202611210819',101),('202611210820',102)");
            long first=TargetIssueResolver.sequenceOrdinalSU1("202611210819"),through=first+1;
            ResearchSourceJournalR31.DatasetRead read=ResearchSourceJournalR31.DatasetRead.capture(db);
            db.execSQL("INSERT INTO draws(issue,first_seen_at) VALUES('202611210821',103)");
            db.execSQL("DELETE FROM draws WHERE issue='202611210818'");
            assertTrue(read.valid(db,first,through));
            db.execSQL("UPDATE draws SET first_seen_at=first_seen_at-1 WHERE issue='202611210819'");
            assertFalse(read.valid(db,first,through));
            ResearchSourceJournalR31.DatasetRead beforeGap=ResearchSourceJournalR31.DatasetRead.capture(db);
            db.execSQL("UPDATE r30_live_source_revision SET generation=generation+1 WHERE singleton=1");
            assertFalse(beforeGap.valid(db,first,through));
        }
    }
}
