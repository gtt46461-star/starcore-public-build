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
        db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER,draw_hash TEXT)");
        return db;
    }
    private static void ready(SQLiteDatabase db){int steps=0;while(!StrategySchemaSU1.ready(db)){assertTrue("migration did not terminate",steps++<1000);StrategySchemaSU1.advance(db);}}
    private static int settle(SQLiteDatabase db,Models.Draw draw,String hash,long at){db.beginTransaction();try{int n=LiveShadowLoopSU1.settleWithin(db,draw,hash,at);db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
    @Test public void completedMigrationJournalCannotChangeOrBypassWarmReadiness(){
        try(SQLiteDatabase db=database()){ready(db);assertTrue(StrategySchemaSU1.ready(db));
            for(String sql:new String[]{"UPDATE su1_migration_unit_v2 SET sql_hash='changed' WHERE unit=0","DELETE FROM su1_migration_unit_v2 WHERE unit=0","INSERT OR REPLACE INTO su1_migration_unit_v2 VALUES(0,'changed',1)"})try{db.execSQL(sql);fail("immutable migration journal changed");}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.getMessage().contains("SU1_MIGRATION_JOURNAL_IMMUTABLE"));}
            assertTrue(StrategySchemaSU1.ready(db));db.execSQL("INSERT INTO su1_migration_unit_v2 VALUES(?,'SYNTHETIC_AHEAD',1)",new Object[]{SqlStatementsSU1.SQL.length});assertFalse("warm cache hid an ahead journal",StrategySchemaSU1.ready(db));try{StrategySchemaSU1.advance(db);fail("ahead journal accepted");}catch(IllegalStateException expected){assertEquals("SU1_MIGRATION_AHEAD",expected.getMessage());}
        }
    }
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
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'RUNNING',?,?,?)",new Object[]{target,hash,open+10,DrawHashR36.sha256V2(draw)});
            try{settle(db,draw,hash,open+10);fail("non-completed label accepted");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" SET status='COMPLETED'");
            assertEquals(1,settle(db,draw,hash,open+10));assertEquals(0,settle(db,draw,hash,open+10));
            assertEquals(1,StrategyRepositorySU1.count(db,"SELECT actions FROM su1_live_arm_stats WHERE arm='EXPERT_MIX'"));
            draw.numbers[0]=2;draw.numbers[1]=1;try{settle(db,draw,hash,open+10);fail("forged canonical label accepted");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
            // Matching a changed hot row is insufficient: the immutable terminal must bind the same draw hash.
            db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue=?",new Object[]{target});
            try{settle(db,draw,hash,open+10);fail("changed canonical row reused a stale terminal hash");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE draws SET n1=1,n2=2 WHERE issue=?",new Object[]{target});
            try(Cursor c=db.rawQuery("SELECT hit FROM su1_live_shadow",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
            LiveShadowLoopSU1.closeDays(db,20261022);assertEquals(1,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_live_daily WHERE actions=1 AND hits=1"));
            String dashboard=ResearchDashboardSU1.render(db);assertTrue(dashboard.contains("命中 1 / 结算 1 = 100.00%"));assertTrue(dashboard.contains("冻结 1；待结算 0"));assertTrue(dashboard.contains("预算拒绝 0"));
            assertEquals(0,MainlineHeadSU1.read(db).generation);
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
                Models.Draw draw=new Models.Draw();draw.issue=issue;draw.openTime=open;for(int p=0;p<10;p++)draw.numbers[p]=p+1;
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?,?)",new Object[]{issue,hash,open+1,DrawHashR36.sha256V2(draw)});
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
    @Test public void transitivePinsKeepActiveLineageAndCompiledManifests(){
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis();String data=HashingSU1.text("pin data");String parent,child,extra;
            db.beginTransaction();try{
                String campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",10,"{}",now);
                parent=StrategyRepositorySU1.candidate(db,"EXPERT(0)",1,false,"GRAMMAR",null,campaign,10,data,-1,now);
                child=StrategyRepositorySU1.candidate(db,"ADD(EXPERT(0),XT(3))",1,false,"GRAMMAR",parent,campaign,10,data,-1,now);
                db.execSQL("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=11,retire_reason='TEST' WHERE strategy_id=?",new Object[]{parent});
                extra=StrategyRepositorySU1.blob(db,"nested pinned input","SU1_TEST_PIN",now);
                String program;try(Cursor c=db.rawQuery("SELECT program_hash FROM su1_strategy_version WHERE strategy_id=?",new String[]{parent})){assertTrue(c.moveToFirst());program=c.getString(0);}
                StrategyRepositorySU1.dependency(db,"PROGRAM",program,"BLOB",extra,"TEST_TRANSITIVE",now);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            java.util.Set<String> roots=ArchiveStoreSU1.pinnedBlobRoots(db,new BudgetTokenSU1(10000,100000,null));assertTrue(roots.contains(extra));
            try(Cursor c=db.rawQuery("SELECT p.typed_ir_hash,m.weights_hash,m.training_report_hash FROM su1_strategy_version v JOIN su1_program p ON p.program_hash=v.program_hash JOIN su1_model_artifact m ON m.artifact_hash=v.artifact_hash WHERE v.strategy_id IN (?,?)",new String[]{parent,child})){while(c.moveToNext())for(int i=0;i<3;i++)assertTrue(roots.contains(c.getString(i)));}
        }
    }
    @Test public void mainlineHeadCannotBeFabricatedOrReplaced(){
        try(SQLiteDatabase db=database()){
            ready(db);assertEquals(0,MainlineHeadSU1.read(db).generation);assertFalse(MainlineHeadSU1.rollback(db,System.currentTimeMillis()));
            try{db.execSQL("INSERT INTO su1_mainline_head_event VALUES(2,?,?,NULL,NULL,'REVOKE',?,1)",new Object[]{HashingSU1.text("bad generation"),new String(new char[64]).replace('\0','0'),MainlineHeadSU1.POLICY});fail("skipped head generation accepted");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
        }
    }
    @Test public void auditChampionCannotBeChangedOrOpenedTwice(){
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis();String source=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",5000,"{}",now),audit=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",5000,"{\"audit\":true}",now),data=StrategyRepositorySU1.blob(db,"test source","SU1_TEST_DATA",now),selected=HashingSU1.text("selected"),report=StrategyRepositorySU1.blob(db,"test report","SU1_TEST_REPORT",now);
            StrategyRepositorySU1.row(db,"su1_campaign_audit","source_campaign",source,"variant","REAL","audit_campaign",audit,"dataset_blob",data,"generation",0,"phase","SEARCH","updated_at",now);
            try{StrategyRepositorySU1.row(db,"su1_campaign_audit_open","source_campaign",source,"variant","REAL","selected_hash",selected,"report_hash",report,"opened_at",now);fail("unselected champion opened");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE su1_campaign_audit SET phase='SEALED',selected_hash=?,selected_program='EXPERT(0)',generation=1",new Object[]{selected});
            try{db.execSQL("UPDATE su1_campaign_audit SET selected_hash=NULL,selected_program=NULL,generation=2,phase='DONE'");fail("selected champion cleared");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            StrategyRepositorySU1.row(db,"su1_campaign_audit_open","source_campaign",source,"variant","REAL","selected_hash",selected,"report_hash",report,"opened_at",now);
            try{StrategyRepositorySU1.row(db,"su1_campaign_audit_open","source_campaign",source,"variant","REAL","selected_hash",selected,"report_hash",report,"opened_at",now);fail("sealed audit opened twice");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            String cancelledCampaign=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",5000,"{\"auditCancelled\":true}",now),oldCheckpoint=StrategyRepositorySU1.blob(db,"old checkpoint","SU1_TEST_DATA",now);
            StrategyRepositorySU1.row(db,"su1_campaign_audit","source_campaign",source,"variant","TIME_SHUFFLED","audit_campaign",cancelledCampaign,"dataset_blob",data,"generation",0,"phase","SEARCH","updated_at",now);
            db.execSQL("UPDATE su1_campaign_audit SET phase='SEALED',selected_hash=?,selected_program='EXPERT(0)',checkpoint_hash=?,generation=1 WHERE variant='TIME_SHUFFLED'",new Object[]{selected,oldCheckpoint});
            assertTrue(ArchiveStoreSU1.pinnedBlobRoots(db,new BudgetTokenSU1(5000,200000,null)).contains(oldCheckpoint));
            StrategyRepositorySU1.row(db,"su1_campaign_audit_cancelled","source_campaign",source,"variant","TIME_SHUFFLED","reason","EXECUTION_KERNEL_CHANGED","cancelled_at",now);
            assertFalse(ArchiveStoreSU1.pinnedBlobRoots(db,new BudgetTokenSU1(5000,200000,null)).contains(oldCheckpoint));
            assertTrue(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_campaign_audit WHERE source_campaign=? AND variant='TIME_SHUFFLED' AND phase='SEALED' AND selected_hash=?",source,selected));
            assertFalse(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_campaign_audit_open WHERE source_campaign=? AND variant='TIME_SHUFFLED'",source));
            try{db.execSQL("DELETE FROM su1_campaign_audit_cancelled");fail("cancellation receipt deleted");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE su1_campaign_audit SET phase='DONE',generation=2 WHERE variant='REAL'");assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_research_pool"));
        }
    }
    @Test public void priorSu1V2V3AndV4BuildsUpgradeWithoutRewritingTheirMigrationReceipts()throws Exception{
        for(String fixture:new String[]{"su1_preview_v2.json","su1_preview_v3.json","su1_preview_v4.json"}){
        try(SQLiteDatabase db=database();java.io.InputStream input=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(fixture)){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=input.read(block))!=-1)bytes.write(block,0,n);org.json.JSONArray old=new org.json.JSONArray(bytes.toString("UTF-8"));
            db.execSQL("CREATE TABLE su1_migration_unit_v2(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL)");
            for(int i=0;i<old.length();i++){String sql=old.getString(i);db.execSQL(sql);db.execSQL("INSERT INTO su1_migration_unit_v2 VALUES(?,?,1)",new Object[]{i,HashingSU1.text(sql)});}
            assertFalse(StrategySchemaSU1.ready(db));ready(db);assertTrue(StrategySchemaSU1.ready(db));
            try(Cursor c=db.rawQuery("SELECT unit,sql_hash,installed_at FROM su1_migration_unit_v2 WHERE unit<? ORDER BY unit",new String[]{String.valueOf(old.length())})){int count=0;while(c.moveToNext()){assertEquals(count,c.getInt(0));assertEquals(HashingSU1.text(old.getString(count)),c.getString(1));assertEquals(1,c.getLong(2));count++;}assertEquals(old.length(),count);}
        }
        }
    }
    @Test public void fittedGraphParametersArePublishedAndActuallyConsumedWithoutInheritedEvidence()throws Exception{
        java.util.Map<String,Object> fixture;
        try(java.io.InputStream input=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_relgraph_parity.json")){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=input.read(block))!=-1)bytes.write(block,0,n);fixture=com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(bytes.toString("UTF-8"));
        }
        PortableArtifactSU1 asset=PortableArtifactSU1.decode((String)fixture.get("portableGraph"));assertNotNull(asset.graph);
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis(),sequence=LiveShadowLoopSU1.sequence("202610210020");String strategy;
            db.beginTransaction();try{
                String campaign=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",sequence,"{\"testGraph\":true}",now);
                strategy=StrategyRepositorySU1.candidate(db,asset.program,1,false,"DESKTOP_RELGRAPH",null,campaign,sequence,asset.dataset,-1,now,null,asset.graph);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            try(Cursor c=db.rawQuery("SELECT m.trained_through_sequence,p.validation_last_sequence,p.protocol_json FROM su1_model_artifact m JOIN su1_train_protocol p ON p.protocol_hash=m.protocol_hash JOIN su1_strategy_version v ON v.artifact_hash=m.artifact_hash WHERE v.strategy_id=?",new String[]{strategy})){
                assertTrue(c.moveToFirst());assertEquals(sequence,c.getLong(0));assertTrue(c.getLong(0)>=c.getLong(1));assertTrue(c.getString(2).contains("PENDING_NO_OBSERVATIONS_CREDITED"));
                assertTrue(c.getString(2).contains("\"parameterTrainedThrough\":"+LiveShadowLoopSU1.sequence(asset.graph.throughIssue)));
            }
            try{StrategyRepositorySU1.candidate(db,asset.program,1,false,"DESKTOP_RELGRAPH",null,"unused",LiveShadowLoopSU1.sequence(asset.graph.throughIssue),asset.dataset,-1,now,null,asset.graph);fail("import without post-training local history was accepted");}catch(IllegalArgumentException expected){assertEquals("SU1_IMPORT_WAIT_POSTTRAIN_LOCAL_HISTORY",expected.getMessage());}
            long[] issues=new long[20],available=new long[20];int[][] draws=new int[20][10];for(int t=0;t<20;t++){issues[t]=202610210001L+t;available[t]=now-1000;for(int p=0;p<10;p++)draws[t][p]=(p+t)%10+1;}
            EventFrameSU1 frame=new EventFrameSU1(202610210021L,now+60000,now,"SYNTHETIC_ANDROID_CONTRACT",issues,available,draws);
            java.util.Map<String,ProgramTreeSU1.GraphFitted> loaded=ModelAssetsSU1.loadGraph(db,asset.program,frame);assertEquals(1,loaded.size());
            double[][] actual=ProgramTreeSU1.parse(asset.program).score(new ProgramTreeSU1.Context(draws,20,null,new ScoreHistoryCacheSU1(128,frame),new BudgetTokenSU1(60000,1000000,null)).withGraphFitted(loaded));
            double[][] expected=RelGraphModelSU1.predict(asset.graph.state.weights,new RelationTensorAdapterSU1.Factory(frame).input(20,4,new BudgetTokenSU1(60000,1000000,null)),new BudgetTokenSU1(60000,1000000,null));
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)assertEquals(ProgramTreeSU1.clean(Math.log(Math.max(1e-12,expected[p][n]))),actual[p][n],0);
            java.util.Map<String,Object> stateFixture;
            try(java.io.InputStream input=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_training_parity.json")){
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=input.read(block))!=-1)bytes.write(block,0,n);stateFixture=com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(bytes.toString("UTF-8"));
            }
            PortableArtifactSU1 stateAsset=PortableArtifactSU1.decode((String)stateFixture.get("portable"));String stateCampaign=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",sequence,"{\"testStateImport\":true}",now);
            String stateStrategy=StrategyRepositorySU1.candidate(db,stateAsset.program,1,false,"DESKTOP_FITTED_MODEL",null,stateCampaign,sequence,stateAsset.dataset,-1,now,stateAsset.fitted);
            double[][] stateActual=ProgramTreeSU1.parse(stateAsset.program).score(new ProgramTreeSU1.Context(draws,20).withFitted(ModelAssetsSU1.load(db,stateAsset.program,frame)));
            double[][] stateExpected=RelationalStateInference.predict(stateAsset.fitted.weights,java.util.Arrays.copyOfRange(draws,20-stateAsset.fitted.window,20));
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)assertEquals(ProgramTreeSU1.clean(Math.log(Math.max(1e-12,stateExpected[p][n]))),stateActual[p][n],0);
            assertTrue(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_model_artifact m JOIN su1_strategy_version v ON v.artifact_hash=m.artifact_hash WHERE v.strategy_id=? AND m.trained_through_sequence=?",stateStrategy,String.valueOf(sequence)));
            assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_prediction"));assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_certificate"));assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));assertTrue(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_strategy_version WHERE strategy_id=? AND calibration_state='MISSING'",strategy));
        }
    }
    @Test public void modelAblationFreezeCannotChangeAndReportsNeverGrantAlpha(){
        try(SQLiteDatabase db=database()){
            ready(db);long now=System.currentTimeMillis();String source=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",100,"{\"modelAudit\":true}",now),data=StrategyRepositorySU1.blob(db,"model audit input","TEST",now),checkpoint=StrategyRepositorySU1.blob(db,"model audit checkpoint","TEST",now),report=StrategyRepositorySU1.blob(db,"model audit report","TEST",now),frozen=HashingSU1.text("locked model set");
            StrategyRepositorySU1.row(db,"su1_model_ablation","source_campaign",source,"dataset_blob",data,"kernel_hash",com.ethk3.novaonepro.TrainingCodeIdentityR964.SHA256,"generation",0,"phase","TRAIN","updated_at",now);
            try{db.execSQL("UPDATE su1_model_ablation SET phase='DONE',generation=1,checkpoint_hash=?,frozen_hash=?,report_hash=?",new Object[]{checkpoint,frozen,report});fail("model report skipped durable freeze");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE su1_model_ablation SET phase='LOCKED',generation=1,checkpoint_hash=?,frozen_hash=?",new Object[]{checkpoint,frozen});
            try{db.execSQL("UPDATE su1_model_ablation SET generation=2,frozen_hash=?",new Object[]{HashingSU1.text("replacement model set")});fail("frozen models changed");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            db.execSQL("UPDATE su1_model_ablation SET phase='DONE',generation=2,report_hash=?",new Object[]{report});
            try{db.execSQL("UPDATE su1_model_ablation SET generation=3,phase='TRAIN'");fail("model holdout reopened");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            try{db.execSQL("DELETE FROM su1_model_ablation");fail("model audit deleted");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
            assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_alpha_reservation"));assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_research_pool"));assertEquals(0,StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_certificate"));
        }
    }
}
