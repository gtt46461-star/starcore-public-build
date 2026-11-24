package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class R341FoundryTestCases {
    private R341FoundryTestCases() {}

    public static void retainedTailAndProductionFoundry(Context app) throws Exception {
        long now=System.currentTimeMillis();
        List<Models.Draw> rows=new ArrayList<>();
        rows.add(draw(4000,now-6001L*60000L));
        for(int i=9745;i<=10000;i++) rows.add(draw(i,now-(10001L-i)*60000L));
        FoundryCampaignR26.Snapshot tail=FoundryCampaignR26.retainedTrainingTail(rows,now);
        assertNotNull(tail);
        assertEquals(256,tail.draws.length);
        assertEquals(9745L,tail.ordinal[0]);
        assertEquals(10000L,tail.through);
        byte[] original=tail.encode();
        ColdRebirthContractR34.requireSnapshot(256,original.length,9745,10000,9900);
        assertArrayEquals(original,FoundryCampaignR26.Snapshot.decode(original).encode());
        assertNull(FoundryCampaignR26.retainedTrainingTail(rows.subList(rows.size()-63,rows.size()),now));
        rows.get(rows.size()-1).fetchTime=now+1L;
        assertNull(FoundryCampaignR26.retainedTrainingTail(rows,now));
        new R26PublicationDeviceTest().failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality();
    }

    public static void restoredScopeAndFence(Context app) throws Exception {
        new R26DeviceTest().oldWorkerCannotCommitAfterFenceOrLeaseChanges();
        try { ColdRebirthContractR34.requireSnapshot(64,3000,5000,10000,10000);fail("expired source accepted"); }
        catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("BOUND")); }
        ColdRebirthContractR34.requireSnapshot(64,3000,5001,10000,10000);
        try { ColdRebirthContractR34.requireParentIdentity("",100,0,"","");fail("unknown ABI accepted"); }
        catch(IllegalArgumentException expected) { assertEquals("R34_COLD_PARENT_ABI_OWNER_MISSING",expected.getMessage()); }
    }

    public static void missingParentAbiRemainsInQueue(Context app) throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            installSchemas(db);
            db.beginTransaction();
            try {
                for(String ddl:ColdRebirthSchemaR34.DDL) db.execSQL(ddl);
                db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,status) VALUES('UNKNOWN_SOURCE','UNKNOWN','original-parent','BLOCKED_WARM_NUMERICAL_ABI_PRESERVED')");
                String old=StatsR27.sha256("old ABI fixture");
                db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES('KNOWN_SOURCE','frozen',100,X'0001','ABANDONED_NUMERICAL_ABI',1,?,20)",new Object[]{old});
                db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,status) VALUES('KNOWN_SOURCE','KNOWN','second-parent','BLOCKED_WARM_NUMERICAL_ABI_PRESERVED')");
                assertEquals(Long.valueOf(2L),ColdRebirthR34.pendingParentsForHealth(db));
                assertEquals("original-parent", cell(db,"SELECT canonical_text FROM r26_candidate WHERE policy_hash='UNKNOWN'"));
                db.setTransactionSuccessful();
            } finally {db.endTransaction();}
        }
    }

    public static void trainedSymbolicCodecRestoresActualVariant(Context app) throws Exception {
        SymbolicFamilyR26 model=new SymbolicFamilyR26("F13",1);
        for(int i=0;i<5;i++)model.update(draw(i+1,60000L+i*60000L).numbers);
        assertTrue(model.lastTrainingStep().gradientNorm>0.0);
        assertTrue(model.lastTrainingStep().parameterDeltaNorm>0.0);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)){model.write(out);}
        SymbolicFamilyR26 restored=new SymbolicFamilyR26("F13",1);
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))){restored.read(in);assertEquals(-1,in.read());}
        assertEquals(model.completedTrainingUpdates(),restored.completedTrainingUpdates());
        float[][] a=new float[10][10],b=new float[10][10];
        model.predict(a,new float[2]);restored.predict(b,new float[2]);
        for(int p=0;p<10;p++) assertArrayEquals(a[p],b[p],0.0f);
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
            new SymbolicFamilyR26("F13",2).read(in);fail("different executed formula loaded weights");
        }catch(IOException expected){assertEquals("R26_AST_FORMULA_BINDING",expected.getMessage());}
    }

    public static void freshInputMayRetryExactlyOneBirth(Context app) throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            installSchemas(db);
            List<ReplayDatasetR27.Row> source=new ArrayList<>();
            for(int i=0;i<120;i++){
                Models.Draw d=draw(5000L+i,300000L+i*60000L);
                source.add(new ReplayDatasetR27.Row(d.issue,5000L+i,d.numbers));
            }
            ReplayDatasetR27 longData=ReplayDatasetR27.of(source);
            ReplayDatasetR27 shortData=ReplayDatasetR27.of(source.subList(0,4));
            PolicySpecR27 spec=policy("(zscore (freq 1 16))");
            try(FeatureCacheR27 shortCache=new FeatureCacheR27(shortData);FeatureCacheR27 longCache=new FeatureCacheR27(longData)) {
                BirthGateR27.Verdict missing=new BirthGateR27(shortCache,4).checkCandidate(spec);
                assertFalse(missing.ok);
                assertTrue(missing.detail,missing.detail.startsWith("AVAILABLE_ROWS="));
                StoreR27.EvolutionSink failed=new StoreR27.EvolutionSink("MAIN",longData.issues[0]);
                failed.policy(spec,"REJECTED",missing.code()+':'+missing.detail,1,shortData.datasetHash);
                apply(db,failed.batch);
                assertTrue(StoreR27.recordedPolicies(db,Collections.singleton(spec.policyHash()),shortData.datasetHash).contains(spec.policyHash()));
                assertFalse(StoreR27.recordedPolicies(db,Collections.singleton(spec.policyHash()),longData.datasetHash).contains(spec.policyHash()));
                BirthGateR27.Verdict ready=new BirthGateR27(longCache,120).checkCandidate(spec);
                assertTrue(ready.detail,ready.ok);
                StoreR27.EvolutionSink born=new StoreR27.EvolutionSink("MAIN",longData.issues[119]);
                born.admitted(spec,"ENGINEERING_INPUT_READY",2,longData.datasetHash,"ROOT_PROPOSAL_AWAITING_FIRST_EVALUATION",ready.signature.length>0);
                apply(db,born.batch);
                assertEquals("BORN",cell(db,"SELECT state FROM r27_policy"));
                assertTrue(StoreR27.recordedPolicies(db,Collections.singleton(spec.policyHash()),longData.datasetHash).contains(spec.policyHash()));
                assertEquals("1",cell(db,"SELECT count(*) FROM r26_evolution WHERE attempt_id LIKE 'R27:REJECTED:%'"));
                assertEquals("1",cell(db,"SELECT count(*) FROM r26_evolution WHERE attempt_id LIKE 'R27:%:BORN:%'"));
            }
        }
    }

    public static void distinguishTopologyAndParameterVersions(Context app) {
        PolicySpecR27 first=policy("(add (zscore (freq 1 8)) (zscore (trans1)))");
        PolicySpecR27 swapped=policy("(add (zscore (trans1)) (zscore (freq 1 8)))");
        PolicySpecR27 window=policy("(add (zscore (freq 1 16)) (zscore (trans1)))");
        PolicySpecR27 cross=policy("(add (zscore (freq 1 8)) (zscore (cross 2 1)))");
        assertNotEquals(first.policyHash(),swapped.policyHash());
        assertEquals(FormulaIdentityR341.normalizedPolicy(first),FormulaIdentityR341.normalizedPolicy(swapped));
        assertEquals(FormulaIdentityR341.structure(first),FormulaIdentityR341.structure(window));
        assertNotEquals(FormulaIdentityR341.structure(first),FormulaIdentityR341.structure(cross));
        assertTrue(FormulaIdentityR341.diff(first.ast,swapped.ast).isEmpty());
        assertFalse(FormulaIdentityR341.diff(first.ast,cross.ast).isEmpty());
    }

    public static void preserveInvalidConfirmationBytes(Context app) throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            for(String ddl:FrozenConfirmationStoreR30.DDL) db.execSQL(ddl);
            String bad="{invalid retained packet";
            db.execSQL("INSERT INTO r30_frozen_confirm(task_key,policy_hash,dataset_hash,block_id,generation,packet_json,created_at) VALUES('KEY_A','P','D',1,1,?,1)", new Object[]{bad});
            try {FrozenConfirmationStoreR30.nextPending(db);fail("bad original executed");}
            catch(FrozenConfirmationStoreR30.InvalidPacket invalid){
                assertEquals(StatsR27.sha256(bad),invalid.digest);
                apply(db,FrozenConfirmationStoreR30.quarantine(invalid));
                apply(db,FrozenConfirmationStoreR30.quarantine(invalid));
            }
            assertEquals(bad,cell(db,"SELECT packet_json FROM r30_frozen_confirm"));
            assertEquals("QUARANTINED_ORIGINAL_PACKET",cell(db,"SELECT state FROM r30_frozen_confirm"));
            assertNull(FrozenConfirmationStoreR30.nextPending(db));
        }
    }

    private static Models.Draw draw(long ordinal,long opened) {
        Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal);
        d.openTime=opened;d.fetchTime=opened+500L;
        for(int i=0;i<10;i++)d.numbers[i]=(int)((i+ordinal)%10L)+1;
        return d;
    }
    private static PolicySpecR27 policy(String ast) {
        return new PolicySpecR27(PolicyAstR27.parse(ast),1,PolicySpecR27.Rule.ALWAYS,0.0,1.0,1.0,false,0.01,0.01,null);
    }
    private static void installSchemas(SQLiteDatabase db) {
        db.beginTransaction();
        try {RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);db.setTransactionSuccessful();}
        finally {db.endTransaction();}
    }
    private static void apply(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch) throws Exception {
        db.beginTransaction();
        try {
            if(batch.before!=null) assertTrue(batch.before.call(db)>=0);
            for(ResearchWriteQueueR27.Op op:batch.ops) db.execSQL(op.sql,op.args);
            if(batch.after!=null) assertTrue(batch.after.call(db)>=0);
            db.setTransactionSuccessful();
        }finally {db.endTransaction();}
    }
    private static String cell(SQLiteDatabase db,String sql){
        try(Cursor found=db.rawQuery(sql,null)){assertTrue(found.moveToFirst());return found.getString(0);}
    }
}
