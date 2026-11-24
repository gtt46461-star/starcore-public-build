package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Narrow native probes for the existing study, snapshot writer and generation executors. */
public final class R341ResearchTestCases {
    private static final long FIRST=TargetIssueResolver.sequenceOrdinalSU1("202611240001");
    private R341ResearchTestCases() {}

    private static void create(SQLiteDatabase db,int count) {
        db.beginTransaction();
        try{
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)");
            db.execSQL("CREATE TABLE r27_history_import(issue TEXT,ordinal INTEGER,numbers TEXT,status TEXT,db_numbers TEXT)");
            db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");
            for(String sql:ResearchSchemaR30R1.DDL)db.execSQL(sql);
            for(String sql:SchemaR27.sourceRevisionDefinitionsR31())db.execSQL(sql);
            db.execSQL("INSERT INTO r30_live_source_revision VALUES(1,0)");
            ResearchSourceJournalR31.ensureSchemaR31(db);
            for(int i=0;i<count;i++){
                long open=60_000L*(i+1);
                db.execSQL("INSERT INTO draws VALUES(?,?,?,0,1,2,3,4,5,6,7,8,9,10)",
                    new Object[]{TargetIssueResolver.fromSequenceOrdinalSU1(FIRST+i),open,open+10});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void apply(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch)throws Exception{
        db.beginTransaction();
        try{
            ResearchWriteQueueR27.execute(db,batch,"R341_FIXTURE_WRITER");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void page(SQLiteDatabase db,ReplayDatasetBuildR341 stage)throws Exception{
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        stage.preparePage(db,batch,10L+stage.pages);
        apply(db,batch);
    }

    public static void stagedDatasetRestarts(Context app)throws Exception {
        File file=File.createTempFile("test-r341-b08-", ".db",app.getCacheDir());
        String head=TargetIssueResolver.fromSequenceOrdinalSU1(FIRST+519);
        String original;
        try{
            try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)){
                create(db,520);
                original=StoreR27.loadDataset(db,5000).datasetHash;
                ReplayDatasetBuildR341 stage=ReplayDatasetBuildR341.start(db,head);
                assertNotNull(stage);
                page(db,stage);
                try(Cursor q=db.rawQuery("SELECT count(*) FROM r30r1_analysis_run",null)){
                    assertTrue(q.moveToFirst());assertEquals(0,q.getInt(0));
                }
            }
            try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)){
                ReplayDatasetBuildR341 stage=ReplayDatasetBuildR341.read(db);
                assertEquals(512,stage.rows);
                for(int i=0;!stage.complete();i++){
                    assertTrue("bounded stage did not terminate",i<4);
                    page(db,stage);stage=ReplayDatasetBuildR341.read(db);
                }
                assertEquals(original,stage.assemble(db).datasetHash);
                db.execSQL("UPDATE draws SET first_seen_at=first_seen_at+100 WHERE issue=?",new Object[]{head});
                assertFalse(stage.currentSource(db));
                try { stage.assemble(db);fail("corrected source published"); }
                catch(ResearchStoreR30R1.StaleResearch expected) { assertNotNull(expected); }
            }
        }finally{SQLiteDatabase.deleteDatabase(file);}
    }

    private static EvalContextR27 fixture(){
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(1800,341,null);
        return new EvalContextR27(new FeatureCacheR27(data),SplitR27.of(data,null),null);
    }

    public static void matricesHaveIndependentInputs(Context ignored)throws Exception {
        try(EvalContextR27 c=fixture()){
            for(String kind:MatrixResearchR30R1.SOURCE_ONLY_KINDS_R31){
                MatrixResearchR30R1.OutcomeR31 outcome=MatrixResearchR30R1.produceR31(kind,false,c.data.datasetHash,3,
                    ()->MatrixResearchR30R1.build(kind,c,null,3));
                assertEquals(MatrixResearchR30R1.StatusR31.PRODUCED,outcome.status);
                assertTrue(outcome.tensor.presentCells>0);
                Map<String,Object> envelope=MatrixResearchR30R1.receiptR31("R930:"+kind,"source",c.data.datasetHash,3,outcome,null,10);
                assertEquals(outcome.tensor.hash,envelope.get("matrixHash"));
                assertEquals(10,((List<?>)envelope.get("axisRows")).size());
            }
            MatrixResearchR30R1.OutcomeR31 waiting=MatrixResearchR30R1.produceR31("STUDENT_ERROR",false,c.data.datasetHash,3,
                ()->{fail("a student producer ran without a student");return null;});
            assertEquals(MatrixResearchR30R1.StatusR31.WAITING_STUDENT,waiting.status);
        }
    }

    public static void refusedTensorLeavesOldOriginal(Context ignored)throws Exception {
        try(EvalContextR27 c=fixture()){
            String kind="NUMBER_POSITION_COMPATIBILITY";
            MatrixTensorR30R1.Tensor valid=MatrixResearchR30R1.build(kind,c,null,3);
            MatrixResearchR30R1.OutcomeR31 first=MatrixResearchR30R1.produceR31(kind,false,c.data.datasetHash,3,()->valid);
            Map<String,Object> old=MatrixResearchR30R1.receiptR31("R930:"+kind,"source",c.data.datasetHash,3,first,null,10);
            MatrixResearchR30R1.OutcomeR31 wrong=MatrixResearchR30R1.produceR31(kind,false,c.data.datasetHash,3,
                ()->MatrixResearchR30R1.build("CROSS_POSITION_FLOW",c,null,3));
            assertEquals("MATRIX_PRODUCER_KIND_MISMATCH",wrong.reason);
            Map<String,Object> receipt=MatrixResearchR30R1.receiptR31("R930:"+kind,"changed",c.data.datasetHash,3,wrong,old,10);
            assertEquals(valid.hash,receipt.get("lastValidMatrixHash"));assertEquals(true,receipt.get("lastValidIsStale"));
        }
    }

    public static void chartsRetainSpecificMissingSources(Context ignored){
        try(EvalContextR27 c=fixture()){
            Map<String,Object> position=ResearchObservatoryR30R1.compute("R03",c,Collections.emptyList(),3);
            ResearchObservatoryR30R1.classifyFrameR335(position);
            assertEquals(true,position.get("usableResearchProduct"));
            assertTrue(((Number)position.get("validPointCount")).longValue()>0);
            Map<String,Object> without=ResearchObservatoryR30R1.compute("R05",c,Collections.emptyList(),3);
            ResearchObservatoryR30R1.classifyFrameR335(without);
            assertEquals("ORIGINAL_SELECT_FAILURE_PARENT_REQUIRED",without.get("reason"));
            assertEquals(false,without.get("usableResearchProduct"));
            assertEquals(18,ResearchGraphSpecsR30R1.ALL.size());
        }
    }

    public static void isolateOnlyViewOwnedErrors(Context ignored){
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            try(Cursor c=db.rawQuery("SELECT absent FROM bad_view_fixture",null)){
                c.moveToFirst();fail("absent view accepted");
            }catch(android.database.sqlite.SQLiteException missing){
                assertNotNull(ResearchObservatoryR30R1.localFailureR341(missing));
            }
            assertNull(ResearchObservatoryR30R1.localFailureR341(new android.database.sqlite.SQLiteDatabaseCorruptException("damaged database")));
        }
    }

    public static void formalReadOnlyEvidenceCannotTriggerAction(Context ignored){
        String hash=StatsR27.sha256("engineered measurement");
        for(String view:Arrays.asList("R11","R14","R18")){
            ResearchActionPolicyR33.Source source=new ResearchActionPolicyR33.Source(view,hash,hash,"abi","source",
                "FUTURE",hash,hash,3,10,20);
            try { source.requireAction(hash,3,"abi","source",11,21);fail("formal view executed a research action"); }
            catch(IllegalArgumentException held) { assertEquals("FORMAL_VIEW_READ_ONLY:"+view,held.getMessage()); }
        }
    }

    private static PolicySpecR27 policy(PolicyAstR27.Node ast,int position){
        return new PolicySpecR27(ast,position,PolicySpecR27.Rule.ALWAYS,0,1,1,false,0,0,null);
    }

    public static void selectedProgramsUseOnlyEffectiveInputs(Context ignored){
        PolicySpecR27 a=policy(PolicyAstR27.leaf("freq",1,32),1),b=policy(PolicyAstR27.leaf("cross",2,1),1);
        try(EvalContextR27 c=fixture()){
            PolicySpecR27 mix=FusionOpsR27.weighted(Arrays.asList(a,b),new double[]{1,1},a);
            PolicySpecR27 onlyA=FusionOpsR27.weighted(Arrays.asList(a,b),new double[]{1,0},a);
            PolicySpecR27 onlyB=FusionOpsR27.weighted(Arrays.asList(a,b),new double[]{0,1},a);
            for(PolicySpecR27 reference:Arrays.asList(onlyA,onlyB)){
                Map<String,Object> result=FusionStudyR30R1.componentAblationR341(mix,reference,c);
                assertTrue(((Number)result.get("sameValidOpportunities")).longValue()>0);
                assertTrue(((Number)result.get("selectionChanges")).longValue()>0);
            }
            assertEquals(0,((Number)FusionStudyR30R1.componentAblationR341(onlyA,onlyA,c).get("selectionChanges")).intValue());
            try { FusionStudyR30R1.pair(a,policy(b.ast,2),c);fail("cross target accepted"); }
            catch(IllegalArgumentException rejected){assertEquals("FUSION_DIFFERENT_TARGET",rejected.getMessage());}
        }
    }

    public static void onlyActualTopologyEditGetsCounterfactual(Context ignored){
        PolicyAstR27.Node a=PolicyAstR27.leaf("freq",1,32),b=PolicyAstR27.leaf("cross",2,1);
        PolicyAstR27.Node candidate=PolicyAstR27.op("add",a,b);
        assertEquals(a.toString(),FormulaIdentityR341.revertSingleEditR341(a,candidate).toString());
        try(EvalContextR27 c=fixture()){
            Map<String,Object> result=FusionStudyR30R1.componentAblationR341(policy(candidate,1),policy(a,1),c);
            assertTrue(((Number)result.get("selectionChanges")).intValue()>0);
        }
    }

    public static void fixedPriorsKeepFutureEvidenceOut(Context ignored){
        Map<String,Double> priors=new LinkedHashMap<>();priors.put("one",1d);priors.put("two",1d);
        Map<String,Double> noCredit=ResearchActionPolicyR33.weights(priors,Collections.emptyList(),"development",200,.25);
        assertEquals(.5,noCredit.get("one"),0);
        ResearchActionPolicyR33.Credit qualified=new ResearchActionPolicyR33.Credit("one","declared","development",256,.01,.1);
        Map<String,Double> reallocated=ResearchActionPolicyR33.weights(priors,Arrays.asList(qualified,qualified),"development",200,.25);
        assertTrue(reallocated.get("one")>noCredit.get("one"));assertTrue(reallocated.get("two")>0);
        assertEquals(reallocated,ResearchActionPolicyR33.weights(priors,Arrays.asList(qualified),"development",200,.25));
        assertEquals(noCredit,ResearchActionPolicyR33.weights(priors,Arrays.asList(qualified),"future-only",200,.25));
    }
}
