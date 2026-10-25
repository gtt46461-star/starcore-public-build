package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class Plan1025DeviceTest {
    private static void ensure(SQLiteDatabase db) { db.beginTransaction(); try { StrategySchemaSU2.ensure(db); db.setTransactionSuccessful(); } finally { db.endTransaction(); } }
    private static long scalar(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { assertTrue(c.moveToFirst()); return c.getLong(0); } }
    @Test public void oldR13GenomeConstraintMigratesAtomicallyAndPreservesData() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER)"); db.execSQL("INSERT INTO draws VALUES('legacy',7)");
            db.execSQL("CREATE TABLE su2_model_genome(genome_key TEXT PRIMARY KEY CHECK(length(genome_key)=64),canonical_text TEXT NOT NULL CHECK(length(canonical_text) BETWEEN 20 AND 2048 AND substr(canonical_text,1,4)='SU2|'),formula TEXT NOT NULL,tier TEXT NOT NULL,origin_op TEXT NOT NULL,parent_key TEXT,repair_note TEXT NOT NULL DEFAULT '',born_campaign TEXT NOT NULL,born_sequence INTEGER NOT NULL,dev_fitness REAL,val_fitness REAL,state TEXT NOT NULL DEFAULT 'RESEARCH',created_at INTEGER NOT NULL) WITHOUT ROWID");
            String key = HashingSU1.text("legacy");
            db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,created_at) VALUES(?,?,'old','F15','seed','old',1,1)", new Object[]{key, "SU2|12345678901234567890"});
            db.beginTransaction(); try { StrategySchemaSU2.ensure(db); assertTrue(StrategySchemaSU2.ready(db)); } finally { db.endTransaction(); }
            assertFalse(StrategySchemaSU2.ready(db)); assertEquals(1, scalar(db, "SELECT count(*) FROM su2_model_genome"));
            ensure(db); ensure(db); assertTrue(StrategySchemaSU2.ready(db));
            assertEquals(1, scalar(db, "SELECT count(*) FROM su2_model_genome")); assertEquals(7, scalar(db, "SELECT n1 FROM draws"));
            db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,created_at) VALUES(?,?,'committee','F15','COMMITTEE','new',2,2)", new Object[]{HashingSU1.text("committee"), "SU2C|12345678901234567890"});
            assertEquals(2, scalar(db, "SELECT count(*) FROM su2_model_genome")); assertEquals(5, scalar(db, "SELECT count(*) FROM pragma_table_info('su2_live_state')"));
        }
    }
    @Test public void blockFoldingKeepsLifetimeCountsExactlyOnce() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            ensure(db); String key = HashingSU1.text("track");
            db.beginTransaction(); try {
                db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,created_at) VALUES(?,?,'test','F15','seed','test',1,1)", new Object[]{key, "SU2|12345678901234567890"});
                for (int i = 0; i < 3; i++) db.execSQL("INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time,hit,settled_at) VALUES(?,?,1,7,?,.5,.4,1,2,?,3)", new Object[]{key, new int[]{1,2,501}[i], i == 1 ? 0 : 1, i == 0 ? 1 : 0});
                DiagnosisStoreSU2.foldTrackBlocks(db, 21000); DiagnosisStoreSU2.foldTrackBlocks(db, 21000);
                assertEquals(3, scalar(db, "SELECT sum(periods) FROM su2_track_block")); assertEquals(2, scalar(db, "SELECT sum(actions) FROM su2_track_block")); assertEquals(1, scalar(db, "SELECT sum(hits) FROM su2_track_block"));
                assertEquals(1, scalar(db, "SELECT count(*) FROM su2_track_block WHERE block_index=-1"));
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
    }
    @Test public void mainlineIsKeptWhenMoreThan96NewerGenomesExist() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            ensure(db); String first = null;
            db.beginTransaction(); try {
                for (int i = 0; i < 110; i++) {
                    Map<String,String> p = new TreeMap<>(); p.put("alpha", String.valueOf(i + 1)); p.put("scales", "(0.9, 0.97, 0.99, 0.997, 1.0)"); p.put("share", "0.003");
                    ModelGenomeSU2 g = new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MSDIR",p,1)), "STATIC",false,"MASS",.3,.2,true);
                    if (i == 0) first = g.text();
                    db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'test','F30','seed','test',?,'LEAGUE',1)", new Object[]{g.key(),g.text(),i});
                    ModelStoreSU2.joinLeague(db,"SU2:"+g.key(),"SU2","test",g.key(),i+1);
                }
                db.execSQL("INSERT INTO su2_mainline_choice VALUES(1,?,0,0,'test',0,1)",new Object[]{"SU2:"+ModelGenomeSU2.parse(first).key()}); db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            List<String> lives = ModelLiveLoopSU2.liveGenomes(db); assertEquals(first, lives.get(0)); assertTrue(lives.size() <= 32);
            Set<String> cals = new HashSet<>(); for (String text : lives) cals.addAll(ModelLiveLoopSU2.calibratedMembers(text)); assertTrue(cals.size() <= 48);
        }
    }
}
