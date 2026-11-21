package com.ethk3.novaonepro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class R335VivoFieldRepairTest {
    @Test public void pendingRetentionArchivesBlockOnlyActualRetirement() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE r26_foundry(campaign_id TEXT,status TEXT,numerical_abi TEXT,snapshot BLOB,snapshot_hash TEXT,source_through INTEGER,created_at INTEGER)");
            db.execSQL("CREATE TABLE r26_candidate(campaign_id TEXT,policy_hash TEXT,checkpoint_hash TEXT,state_hash TEXT,cursor INTEGER,status TEXT,state BLOB,canonical_text TEXT)");
            db.execSQL("CREATE TABLE r26_live_model(policy_hash TEXT)");
            db.execSQL("INSERT INTO r26_foundry VALUES('current','RUNNING','current',X'0102','source',19,1)");
            try (WarmCandidateR26.PreparedAbiR31 abi = WarmCandidateR26.prepareAbiR31(db, "current");
                 WarmCandidateR26.PreparedPreservationR31 retention = WarmCandidateR26.preparePreservationR31(db)) {
                assertTrue(abi.campaigns().isEmpty());
                assertEquals(0, retention.rows());
            }
            try (WarmCandidateR26.PreparedAbiR31 ignored = WarmCandidateR26.prepareAbiR31(db, "new")) {
                fail("An old snapshot requires authenticated archive protection");
            } catch (DatabaseCoordinator.SchemaPendingR31 expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("SOURCE_NOT_PRESENT"));
            }
            db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION'");
            db.execSQL("INSERT INTO r26_candidate VALUES('current','child','checkpoint','state',1,'DONE',X'03','learned')");
            try (WarmCandidateR26.PreparedPreservationR31 ignored = WarmCandidateR26.preparePreservationR31(db)) {
                fail("A retained candidate requires authenticated archive protection");
            } catch (DatabaseCoordinator.SchemaPendingR31 expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("SOURCE_NOT_PRESENT"));
            }
            try (android.database.Cursor c = db.rawQuery("SELECT hex(snapshot),hex(state) FROM r26_foundry JOIN r26_candidate USING(campaign_id)", null)) {
                assertTrue(c.moveToFirst());
                assertEquals("0102", c.getString(0));
                assertEquals("03", c.getString(1));
            }
        }
    }

    @Test public void largeOriginalAndCorruptSiblingRemainIndependentForEveryGroup() {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE r30r1_analysis_run(run_id TEXT PRIMARY KEY, dataset_hash TEXT,role TEXT,through_ordinal INTEGER,source_commit TEXT,status TEXT,summary_json TEXT,contract_hash TEXT)");
            db.execSQL("CREATE TABLE r30r1_chart_cache(view_id TEXT,payload_json TEXT,payload_hash TEXT,run_id TEXT,frame_id TEXT,updated_at INTEGER)");
            char[] padding=new char[2300000]; Arrays.fill(padding,'a');
            for(int group=0;group<3;group++) {
                for(ResearchGraphSpecsR30R1.Spec spec:ResearchGraphSpecsR30R1.group(group)) {
                    Map<String,Object> source=new HashMap<>(), frame=new HashMap<>(), display=new HashMap<>();
                    source.put("sourceContract",Collections.singletonMap("abi",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256));
                    String hash;
                    if("R06".equals(spec.id)) {
                        source.put("mean",0.125);source.put("evaluatedIssues",30);source.put("componentPath","test.component");
                        display.putAll(source);hash=StatsR27.sha256("paired-fixture");
                    } else {
                        Map<String,Object> point=new HashMap<>();point.put("label","sample");point.put("value",0.125);point.put("n",30);
                        frame.put("points",Collections.singletonList(point));frame.put("role","SELECT");
                        if("R01".equals(spec.id))frame.put("fixturePadding",new String(padding));
                        hash=StatsR27.sha256(JsonR27.write(frame));
                        display.put("points",frame.get("points"));display.put("originalMachineFrameSha256",hash);
                        source.put("viewId",spec.id);source.put("inputHash",hash);source.put("frame",frame);
                    }
                    String summary=JsonR27.write(source),page=JsonR27.write(display),run="fixture-"+spec.id;
                    db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?)",new Object[]{run,"controlled","SELECT",128,
                            BuildConfig.SOURCE_COMMIT_R26,"COMMITTED",summary,hash});
                    db.execSQL("INSERT INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",new Object[]{spec.id,page,StatsR27.sha256(page),run,run,1});
                }
                List<ResearchVisualDataR82.Panel> panels=ResearchWorkbenchRepositoryR30R1.load(db,group,null);
                assertEquals(6,panels.size());
                for(ResearchVisualDataR82.Panel p:panels) {assertFalse(p.title+" "+p.note,"STATUS".equals(p.kind));assertEquals(1,p.points.size());}
            }
            db.execSQL("UPDATE r30r1_analysis_run SET contract_hash='invalid' WHERE run_id='fixture-R02'");
            List<ResearchVisualDataR82.Panel> read=ResearchWorkbenchRepositoryR30R1.load(db,0,null);
            assertEquals(1,read.get(0).points.size());assertEquals("READ_ERROR",read.get(1).status);assertEquals(1,read.get(2).points.size());
        }
    }

    @Test public void historicalReplayAndMissingFreezeDoNotReplaceLatestSettlement() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        // A separate preferences directory keeps persisted test observations out of application health.
        Context isolated=new android.content.ContextWrapper(context) {
            @Override public Context getApplicationContext(){return this;}
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){
                return context.getSharedPreferences("field_repair_"+name,mode);
            }
            @Override public Context createDeviceProtectedStorageContext(){return this;}
        };
        isolated.getSharedPreferences("v20_service_runtime_light",0).edit().clear().commit();
        ServiceHeartbeatStore health=new ServiceHeartbeatStore(isolated);
        health.markSettlementTerminal("202611210820","COMPLETED","first",2000);
        health.markSettlementTerminal("202611210803","NO_PREDRAW_CANONICAL_TERMINAL","FROZEN_OBSERVATION_MISSING",3000);
        health.markSettlementTerminal("202611170079","COMPLETED","historical",4000);
        assertEquals("202611210820",health.getLastSettlementIssue());assertEquals(2000L,health.getLastSettlementSuccessAt());
        assertEquals(4000L,isolated.getSharedPreferences("v20_service_runtime_light",0).getLong("settlement_terminal_progress_at",0));
    }
}
