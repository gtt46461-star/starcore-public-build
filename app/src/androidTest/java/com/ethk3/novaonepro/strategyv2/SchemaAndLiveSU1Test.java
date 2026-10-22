package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SchemaAndLiveSU1Test {
    private SQLiteDatabase database(){
        SQLiteDatabase db=SQLiteDatabase.create(null);db.setForeignKeyConstraintsEnabled(true);
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER,open_time INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
        db.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");
        db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
        return db;
    }
    private static void ready(SQLiteDatabase db){int steps=0;while(!StrategySchemaSU1.ready(db)){assertTrue("migration did not terminate",steps++<1000);StrategySchemaSU1.advance(db);}}
    private static int settle(SQLiteDatabase db,Models.Draw draw,String hash,long at){db.beginTransaction();try{int n=LiveShadowLoopSU1.settleWithin(db,draw,hash,at);db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
    @Test public void migrationRollbackAndRestartPreserveLegacyRows(){
        try(SQLiteDatabase db=database()){
            db.execSQL("INSERT INTO draws VALUES('202610210001',0,100,1,2,3,4,5,6,7,8,9,10)");
            StrategySchemaSU1.advance(db);int before=StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_migration_unit_v2");
            db.beginTransaction();try{StrategySchemaSU1.advance(db);}finally{db.endTransaction();}
            assertEquals(before,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_migration_unit_v2"));ready(db);assertTrue(StrategySchemaSU1.ready(db));
            assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM draws WHERE n1=1 AND n10=10"));
            db.execSQL("DROP TRIGGER su1_live_shadow_settle");assertFalse("READY ignored missing trigger",StrategySchemaSU1.ready(db));
        }
    }
    @Test public void onlyCompletedCanonicalLabelsSettleAndReplaysDoNotCountTwice(){
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis(),open=now+60000;String target="202610210021";long seq=LiveShadowLoopSU1.sequence(target);String data=HashingSU1.text("data");
            db.beginTransaction();String id,campaign;try{
                campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",seq-1,"{\"test\":true}",now);
                id=StrategyRepositorySU1.candidate(db,"EXPERT(0)",1,false,"EXPERT_MIX",null,campaign,seq-1,data,-1,now);
                long[] issues=new long[20],available=new long[20];int[][] history=new int[20][10];for(int t=0;t<20;t++){issues[t]=202610210001L+t;available[t]=now-1000;for(int p=0;p<10;p++)history[t][p]=p+1;}
                EventFrameSU1 frame=new EventFrameSU1(Long.parseLong(target),open,now,"CANONICAL_PK10_V1",issues,available,history);
                StrategyRepositorySU1.snapshot(db,frame,SnapshotPayloadSU1.prepare(frame));
                db.execSQL("INSERT INTO su1_live_target(sequence_ordinal,target_issue,target_open_time,snapshot_hash) VALUES(?,?,?,?)",new Object[]{seq,target,open,frame.contentHash});
                db.execSQL("INSERT INTO su1_live_shadow(strategy_id,sequence_ordinal,day_key,position,number_mask,captured_at,target_open_time) VALUES(?,?,20261021,1,7,?,?)",new Object[]{id,seq,now,open});
                db.execSQL("INSERT INTO su1_live_attempt VALUES(?,?,'FROZEN','',?)",new Object[]{id,seq,now});db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            String hash=HashingSU1.text("canonical completed");Models.Draw draw=new Models.Draw();draw.issue=target;draw.openTime=open;for(int p=0;p<10;p++)draw.numbers[p]=p+1;
            db.execSQL("INSERT INTO draws VALUES(?,0,?,1,2,3,4,5,6,7,8,9,10)",new Object[]{target,open});
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'RUNNING',?,?)",new Object[]{target,hash,open+10});
            try{settle(db,draw,hash,open+10);fail("non-completed label accepted");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" SET status='COMPLETED'");
            assertEquals(1,settle(db,draw,hash,open+10));assertEquals(0,settle(db,draw,hash,open+10));
            assertEquals(1,StrategyRepositorySU1.count(db,"SELECT actions FROM su1_live_arm_stats WHERE arm='EXPERT_MIX'"));
            draw.numbers[0]=2;draw.numbers[1]=1;try{settle(db,draw,hash,open+10);fail("forged canonical label accepted");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
            try(Cursor c=db.rawQuery("SELECT hit FROM su1_live_shadow",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
            LiveShadowLoopSU1.closeDays(db,20261022);assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_live_daily WHERE actions=1 AND hits=1"));
        }
    }
    @Test public void bothMissingConfirmKeepsCanonicalDenominatorAndNeverPasses(){
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis(),open=now+60000;String issue="202610210021",family=HashingSU1.text("test family"),hash=HashingSU1.text("settlement");long seq=LiveShadowLoopSU1.sequence(issue);String cohort;
            db.beginTransaction();try{
                String campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",seq-1,"{\"test\":true}",now);
                String id=StrategyRepositorySU1.candidate(db,"EXPERT(0)",1,false,"EXPERT_MIX",null,campaign,seq-1,HashingSU1.text("input"),-1,now);
                cohort=PromotionPipelineSU1.register(db,id,PromotionPipelineSU1.nullBaseline(db),family,issue,seq-1,1,"VS_NULL_BASELINE","CONFIRM");
                db.execSQL("INSERT INTO draws VALUES(?,0,?,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue,open});
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{issue,hash,open+1});
                Models.Draw draw=new Models.Draw();draw.issue=issue;draw.openTime=open;for(int p=0;p<10;p++)draw.numbers[p]=p+1;
                EvidenceLoopSU1.settleWithin(db,draw,hash,open+1);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            EvidenceQualitySU1.Look look=EvidenceQualitySU1.prepare(db,cohort,open+2,new BudgetTokenSU1(10000,100000,null));assertNotNull(look);assertEquals(1,look.n);assertEquals(1,look.unpaired);assertEquals("INSUFFICIENT",look.state);
            db.beginTransaction();try{EvidenceQualitySU1.publish(db,look);db.setTransactionSuccessful();}finally{db.endTransaction();}
            assertFalse(EvidenceQualitySU1.verifiedPass(db,look.hash));assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_opportunity_outcome"));
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){assertFalse(c.moveToFirst());}
        }
    }
    @Test public void lifetimeBudgetCannotBeRefundedAndMainlineStaysClosed(){
        try(SQLiteDatabase db=database()){
            ready(db);assertNull(CertificateSU1.read(db,HashingSU1.text("no certificate"),System.currentTimeMillis(),true));
            db.beginTransaction();try{
                String root=HashingSU1.text("lifetime root"),claim=HashingSU1.text("claim");double alpha=AlphaLedgerSU1.family(db,root);AlphaLedgerSU1.reserveClaim(db,claim,alpha,1);
                int count=StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation");AlphaLedgerSU1.reserveClaim(db,claim,alpha,1);assertEquals(count,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));
                try{db.execSQL("DELETE FROM su1_alpha_reservation");fail("alpha refunded");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
                try{db.execSQL("INSERT INTO su1_alpha_reservation VALUES('overspend','SU1',0.05,1)");fail("global cap exceeded");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }
    @Test public void sameNamedReplacementTriggerDoesNotCountAsReady(){
        try(SQLiteDatabase db=database()){
            ready(db);db.execSQL("DROP TRIGGER su1_live_shadow_settle");db.execSQL("CREATE TRIGGER su1_live_shadow_settle BEFORE UPDATE ON su1_live_shadow BEGIN SELECT 1; END");assertFalse("altered trigger was accepted",StrategySchemaSU1.ready(db));
        }
    }
    @Test public void previewUpgradeKeepsV1ReceiptsAndAssets()throws Exception{
        try(SQLiteDatabase db=database();java.io.InputStream input=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_preview_v1.json")){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int length;while((length=input.read(block))!=-1)bytes.write(block,0,length);
            org.json.JSONArray old=new org.json.JSONArray(bytes.toString("UTF-8"));db.execSQL("CREATE TABLE su1_migration_unit(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL)");
            for(int i=0;i<old.length();i++){String sql=old.getString(i);db.execSQL(sql);db.execSQL("INSERT INTO su1_migration_unit VALUES(?,?,1)",new Object[]{i,HashingSU1.text(sql)});}
            String asset;db.beginTransaction();try{asset=PromotionPipelineSU1.nullBaseline(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            assertFalse(StrategySchemaSU1.ready(db));ready(db);assertTrue(StrategySchemaSU1.ready(db));assertEquals(old.length(),StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_migration_unit"));
            assertTrue(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_strategy_version WHERE strategy_id=?",asset));assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM pragma_table_info('su1_runtime_program') WHERE name='kernel_hash'"));
        }
    }
    @Test public void researchCohortsHaveSeparateOrdinalsAndSpendNoAlpha(){
        try(SQLiteDatabase db=database()){
            ready(db);long seq=LiveShadowLoopSU1.sequence("202610220001"),now=System.currentTimeMillis();String family=HashingSU1.text("role namespaces");
            db.beginTransaction();try{
                String campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",seq-1,"{\"test\":true}",now),id=StrategyRepositorySU1.candidate(db,"EXPERT(0)",1,false,"EXPERT_MIX",null,campaign,seq-1,HashingSU1.text("dataset"),-1,now);
                String research=PromotionPipelineSU1.register(db,id,null,family,"202610220001",seq-1,1,"PARENT_CHILD","RESEARCH_FUTURE");
                assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));assertEquals(1000000,StrategyRepositorySU1.count(db,"SELECT global_claim_ordinal FROM su1_eval_cohort WHERE evidence_role='RESEARCH_FUTURE'"));
                String confirm=PromotionPipelineSU1.register(db,id,PromotionPipelineSU1.nullBaseline(db),family,"202610220001",seq-1,1,"VS_NULL_BASELINE","CONFIRM");
                assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));assertEquals(1,StrategyRepositorySU1.count(db,"SELECT global_claim_ordinal FROM su1_eval_cohort WHERE evidence_role='CONFIRM'"));assertNotEquals(research,confirm);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }
}
