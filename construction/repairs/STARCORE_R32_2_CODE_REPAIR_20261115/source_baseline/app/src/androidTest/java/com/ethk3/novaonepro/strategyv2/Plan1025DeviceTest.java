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
                com.ethk3.novaonepro.RetentionRulesSU2.foldDecisions(db,10082,1);
                com.ethk3.novaonepro.RetentionRulesSU2.foldDecisions(db,10083,2);
                com.ethk3.novaonepro.RetentionRulesSU2.foldDecisions(db,10582,3);
                com.ethk3.novaonepro.RetentionRulesSU2.foldDecisions(db,10582,4);
                assertEquals(3,scalar(db,"SELECT sum(periods) FROM su2_live_daily"));assertEquals(2,scalar(db,"SELECT sum(actions) FROM su2_live_daily"));assertEquals(1,scalar(db,"SELECT sum(hits) FROM su2_live_daily"));
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
    @Test public void stageBundlePersistsAndRejectsModifiedTrainedParameters() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            ensure(db); Map<String,String> p=new TreeMap<>();p.put("alpha","1");p.put("scales","(0.9, 0.97, 0.99, 0.997, 1.0)");p.put("share","0.003");
            ModelGenomeSU2 g=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MSDIR",p,1)),"STATIC",false,"MASS",.3,.2,true);
            double[][] mu=new double[1][93],var=new double[1][93];Arrays.fill(var[0],1);double[] sd=new double[93];Arrays.fill(sd,1);
            StageLayerSU2.Gmm mixture=new StageLayerSU2.Gmm(mu,var,new double[]{1},1);
            StageLayerSU2.Layer layer=new StageLayerSU2.Layer(mixture,new double[93],sd,new double[1][4],new double[]{1});
            double[] seed=new double[720];Arrays.fill(seed,.4);
            StageStrategySU2.Definition d=new StageStrategySU2.Definition("F15",.15,3000,Collections.singletonList(g.text()),layer,seed,0,45,15,42,12);
            db.beginTransaction();try{StageStoreSU2.save(db,Collections.singletonList(d),"test",3000,1);db.setTransactionSuccessful();}finally{db.endTransaction();}
            StageStoreSU2.load(db,d.text());assertEquals(d.key(),StageStrategySU2.definition(d.text()).key());
            assertEquals(1,scalar(db,"SELECT count(*) FROM su2_stage_bundle"));
            byte[] changed=d.bytes();changed[changed.length-1]^=1;
            db.execSQL("UPDATE su2_stage_bundle SET bundle=? WHERE genome_key=?",new Object[]{changed,d.key()});
            try{StageStoreSU2.load(db,d.text());fail("tampered stage model accepted");}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("HASH"));}
        }
    }
    @Test public void matrixReportAndDashboardReadTheSameBoundedSnapshots() throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            ensure(db);String key=HashingSU1.text("matrix");
            db.beginTransaction();try{
                for(int i=0;i<10;i++){
                    String json="{\"checkpoint_sequence\":"+i+",\"factor_matrix\":[],\"teacher_matrix\":[],\"atomic_explanation\":{},\"reason_grades\":{}}";
                    MatrixStoreSU2.save(db,key,i,new MatrixSnapshotSU2.Data(json,Collections.emptyList(),new AtomStreamSU2()),i);
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            assertEquals(8,scalar(db,"SELECT count(*) FROM su2_matrix_snapshot"));
            org.json.JSONObject latest=new org.json.JSONObject(MatrixStoreSU2.report(db)).getJSONArray("snapshots").getJSONObject(0);
            assertEquals(9,latest.getLong("checkpoint_sequence"));assertEquals(0,latest.getJSONObject("reason_grades").length());
            assertTrue(MatrixStoreSU2.render(db).contains("冻结序号 9"));
            db.execSQL("UPDATE su2_matrix_snapshot SET stats_json=? WHERE checkpoint_sequence=9",new Object[]{"{\"FREQUENCY\":[200,90,200,45]}"});
            latest=new org.json.JSONObject(MatrixStoreSU2.report(db)).getJSONArray("snapshots").getJSONObject(0);
            assertEquals("A_VALID",latest.getJSONObject("reason_grades").getJSONObject("FREQUENCY").getString("grade"));
            db.execSQL("UPDATE su2_matrix_snapshot SET stats_json=? WHERE checkpoint_sequence=9",new Object[]{"{\"FREQUENCY\":[200,90,200,45],\"__MISSING_SOURCE\":[1,0,0,0]}"});
            latest=new org.json.JSONObject(MatrixStoreSU2.report(db)).getJSONArray("snapshots").getJSONObject(0);
            assertEquals("C_EXPLAIN_ONLY",latest.getJSONObject("reason_grades").getJSONObject("FREQUENCY").getString("grade"));
        }
    }
}
