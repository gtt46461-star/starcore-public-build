package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.SettlementTaskStoreR36;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Runs the original learner, model and SQLite operators within the instrumentation sandbox. */
public final class R341LearningTestCases {
    private static final String COMPLETE="202611240040", NEXT="202611240041";
    private static final String DATA=HashingSU1.text("isolated R34.1 sample store");
    private static final String PIN=HashingSU1.text("exact prepared ten-position input fixture");
    private static final long THROUGH=TargetIssueResolver.sequenceOrdinalSU1(COMPLETE);
    private static final long TARGET=TargetIssueResolver.sequenceOrdinalSU1(NEXT);

    private R341LearningTestCases() {}

    private static AtomNetSessionR30 ready() {
        AtomNetSessionR30 state=new AtomNetSessionR30(DATA,COMPLETE,THROUGH,12,10,10,0);
        state.completeIssue=COMPLETE;
        state.onlineObservedOrdinal=THROUGH;
        state.phase="ONLINE";
        state.trainer.onlineLr=1e-4;
        state.trainer.checkpoint=state.trainer.net.encode();
        return state;
    }

    private static AtomNetTrainerR27.Sample sample(String issue,int position) {
        return new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.3,position*.07},position),
            position%2,position-1,AtomNetSessionR30.eventKey(issue,position));
    }

    private static AtomNetSessionR30 checkpoint(AtomNetSessionR30 state) {
        byte[] bytes=state.encode();
        return AtomNetSessionR30.decode(bytes,StatsR27.sha256(bytes));
    }

    private static void step(AtomNetSessionR30 state,int position) {
        state.online(sample(NEXT,position),NEXT,TARGET,position,PIN);
    }

    public static void tenPositionResume(Context ignored) {
        AtomNetSessionR30 state=ready();
        for(int p=1;p<=9;p++)step(state,p);
        assertFalse(state.consumableR31());
        assertEquals(COMPLETE,state.completeIssue);
        AtomNetSessionR30 restored=checkpoint(state);
        assertEquals(10,restored.nextPosition);
        assertEquals(PIN,restored.partialInputHashR341);
        try { step(restored,9);fail("duplicate position accepted"); }
        catch(IllegalStateException refused) { assertEquals("R30_POSITION_CURSOR",refused.getMessage()); }
        try { restored.online(sample(NEXT,10),NEXT,TARGET,10,HashingSU1.text("other prepared history"));fail("unbound source accepted"); }
        catch(IllegalStateException refused) { assertEquals("R341_ATOMNET_TEN_POSITION_INPUT_MISMATCH",refused.getMessage()); }
        assertEquals(9,restored.trainer.onlineSteps);
        step(restored,10);
        assertTrue(restored.consumableR31());
        assertEquals(NEXT,restored.completeIssue);
        assertEquals(10,restored.trainer.onlineSteps);
    }

    public static void sameHistoryBeforeTarget(Context ignored) {
        long targetOpen=3_000_000L;
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();
        for(int i=0;i<=32;i++) {
            long ordinal=TARGET+i-32,open=targetOpen-(32-i)*60_000L;
            rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(ordinal),ordinal,
                new int[]{1,2,3,4,5,6,7,8,9,10},open,open+100,"fixture","DRAW_FIRST_SEEN"));
        }
        ReplayDatasetR27 original=ReplayDatasetR27.of(rows);
        AtomReplayR27.VisibleFrame before=AtomReplayR27.buildAsOf(original,32,targetOpen-4000);
        assertNotNull(before);
        rows.set(32,new ReplayDatasetR27.Row(NEXT,TARGET,new int[]{2,1,3,4,5,6,7,8,9,10}));
        AtomReplayR27.VisibleFrame changed=AtomReplayR27.buildAsOf(ReplayDatasetR27.of(rows),32,targetOpen-4000);
        assertNotNull(changed);
        assertEquals(before.historyHash,changed.historyHash);
        for(int position=0;position<10;position++)assertArrayEquals(before.frame.values[position],changed.frame.values[position],0);
        assertNotEquals(before.frame.actual[0],changed.frame.actual[0]);
        ReplayDatasetR27.Row last=rows.get(31);
        rows.set(31,new ReplayDatasetR27.Row(last.issue,last.ordinal,last.numbers,last.openTime,targetOpen+5,"fixture","DRAW_FIRST_SEEN"));
        assertNull(AtomReplayR27.buildAsOf(ReplayDatasetR27.of(rows),32,targetOpen-4000));
    }

    public static void trainingRoles(Context ignored) {
        List<AtomNetTrainerR27.Sample> train=new ArrayList<>();
        for(int p=1;p<=10;p++)train.add(sample(COMPLETE,p));
        String select=TargetIssueResolver.fromSequenceOrdinalSU1(THROUGH-1);
        List<AtomNetTrainerR27.Sample> validation=Arrays.asList(sample(select,1));
        AtomNetTrainerR27 byList=new AtomNetTrainerR27(12,21);
        assertTrue(byList.pretrain(train,validation)>0);
        byList.online(sample(NEXT,1));
        assertEquals(11,byList.lastOnlineInputsR341().size());
        for(AtomNetTrainerR27.Sample input:byList.lastOnlineInputsR341())assertFalse(input.issue.startsWith(select));
        AtomNetSessionR30 bySession=ready();
        bySession.phase="FILL";
        bySession.work(i->{assertTrue(i>=0&&i<train.size());return train.get(i);});
        bySession=checkpoint(bySession);
        step(bySession,1);
        for(AtomNetTrainerR27.Sample input:bySession.trainer.lastOnlineInputsR341())assertFalse(input.issue.startsWith(select));
        byte[] state=bySession.encode();
        try { bySession.trainer.checkDevelopment(validation,ResearchContractR30R1.Role.FUTURE);fail("future entered optimizer"); }
        catch(IllegalArgumentException refused) { assertEquals("FORMAL_OR_SYNTHETIC_LABELS_CANNOT_ADAPT_MODEL",refused.getMessage()); }
        assertArrayEquals(state,bySession.encode());
    }

    public static void optimizerRestore(Context ignored) {
        AtomNetSessionR30 ordered=ready(),restarted=ready();
        for(int p=1;p<=10;p++) {
            step(ordered,p);
            step(restarted,p);
            if(p==5)restarted=checkpoint(restarted);
        }
        assertArrayEquals(ordered.encode(),restarted.encode());
        assertArrayEquals(ordered.trainer.net.forward(sample(NEXT,4).x,null).pClass,
            restarted.trainer.net.forward(sample(NEXT,4).x,null).pClass,0);
    }

    public static void optimizerActuallyChangesParameters(Context ignored) {
        AtomNetSessionR30 state=ready();
        byte[] original=state.trainer.net.encode();
        step(state,1);
        AtomNetR27.OptimizerStep update=state.trainer.net.lastStep();
        assertNotNull(update);
        assertTrue(update.gradientNorm>0 && update.parameterChange>0 && Double.isFinite(update.batchLoss));
        assertFalse(Arrays.equals(original,state.trainer.net.encode()));
        assertEquals(1,update.batchSize);
        assertEquals(64,update.inputHash.length());
    }

    public static void rejectsNonfiniteInput(Context ignored) {
        AtomNetR27 net=new AtomNetR27(12,33);
        byte[] before=net.encode();
        double[] input=sample(NEXT,1).x;
        input[0]=Double.NaN;
        try { net.trainBatch(new double[][]{input},new int[]{1},new int[]{0},1e-4,new StatsR27.SplitMix(8));fail("invalid optimizer input accepted"); }
        catch(IllegalArgumentException|IllegalStateException expected) { assertArrayEquals(before,net.encode()); }
        assertNull(net.lastStep());
    }

    private static ModelSurfaceProviderR30.Pit pit(int position,int inputPosition,long through) {
        return new ModelSurfaceProviderR30.Pit(TARGET,through,position,PIN,sample(NEXT,inputPosition).x,19_000,20_000,21_000,30_000);
    }

    public static void predictionUsesBoundVersion(Context ignored) {
        AtomNetSessionR30 first=ready();
        byte[] weights=first.trainer.net.encode();
        ModelSurfaceProviderR30.FrozenModelRef model=new ModelSurfaceProviderR30.FrozenModelRef(weights,THROUGH,17_000);
        String oldHash=HashingSU1.fields("R30_REPAIR_MODEL_REF_2",model.weightsHash,AtomNetSessionR30.ABI,
            AtomNetSessionR30.LEGACY_LEARNING_PROTOCOL,Long.toString(THROUGH),"17000");
        ModelSurfaceProviderR30.FrozenModelRef legacy=ModelSurfaceProviderR30.FrozenModelRef.archived(oldHash,weights,THROUGH,17_000);
        double[] a=ModelSurfaceProviderR30.predict(pit(1,1,THROUGH),legacy).probabilities();
        step(first,1);
        ModelSurfaceProviderR30.FrozenModelRef other=new ModelSurfaceProviderR30.FrozenModelRef(first.trainer.net.encode(),THROUGH,17_500);
        assertArrayEquals(a,ModelSurfaceProviderR30.predict(pit(1,1,THROUGH),legacy).probabilities(),0);
        assertFalse(Arrays.equals(a,ModelSurfaceProviderR30.predict(pit(1,1,THROUGH),other).probabilities()));
        assertEquals(oldHash,legacy.modelHash);
    }

    public static void rejectsWrongPositionAndTarget(Context ignored) {
        ModelSurfaceProviderR30.FrozenModelRef model=new ModelSurfaceProviderR30.FrozenModelRef(ready().trainer.net.encode(),THROUGH,17_000);
        try { ModelSurfaceProviderR30.predict(pit(1,2,THROUGH),model);fail("wrong position accepted"); }
        catch(ModelSurfaceProviderR30.Blocked refused) { assertTrue(refused.getMessage().contains("FEATURE_POSITION_MISMATCH")); }
        try { ModelSurfaceProviderR30.predict(pit(1,1,TARGET),model);fail("target feature accepted"); }
        catch(ModelSurfaceProviderR30.Blocked refused) { assertTrue(refused.getMessage().contains("FEATURE_SEES_TARGET")); }
    }

    public static void labelNeedsMatchingTerminal(Context ignored) {
        String draw=HashingSU1.text("draw-v2"),settlement=HashingSU1.text("settlement-v2");
        OnlineResearchWindowR30R1.Window window=new OnlineResearchWindowR30R1.Window(PIN,TARGET,TARGET+5,1000);
        OnlineResearchWindowR30R1.Label label=new OnlineResearchWindowR30R1.Label(NEXT,2000,2100,window,draw,settlement,2200,"COMPLETED");
        assertTrue(label.authoritative());
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            String table=DbHelper.TABLE_R36_SETTLEMENT_TASK;
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER,draw_hash_v2 TEXT,open_time INTEGER,first_seen_at INTEGER)");
            db.execSQL("CREATE TABLE "+table+"(target_issue TEXT PRIMARY KEY,draw_issue TEXT,draw_hash TEXT,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
            db.execSQL("INSERT INTO draws VALUES(?,0,?,2000,2100)",new Object[]{NEXT,draw});
            db.execSQL("INSERT INTO "+table+" VALUES(?,?,?,'PENDING',?,2200)",new Object[]{NEXT,NEXT,draw,settlement});
            assertFalse(OnlineResearchWindowR30R1.matchesSettledLabel(db,label));
            db.execSQL("UPDATE "+table+" SET status='COMPLETED'");
            assertTrue(OnlineResearchWindowR30R1.matchesSettledLabel(db,label));
            db.execSQL("UPDATE "+table+" SET status=?",new Object[]{SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL});
            assertFalse(OnlineResearchWindowR30R1.matchesSettledLabel(db,label));
            OnlineResearchWindowR30R1.Label excluded=new OnlineResearchWindowR30R1.Label(NEXT,2000,2100,window,draw,settlement,2200,SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL);
            assertTrue(excluded.excludedNoPredraw());
            assertTrue(OnlineResearchWindowR30R1.matchesSettledLabel(db,excluded));
            db.execSQL("UPDATE draws SET draw_hash_v2='other'");
            assertFalse(OnlineResearchWindowR30R1.matchesSettledLabel(db,excluded));
        }
    }

    public static void archiveCorruptionStaysLocal(Context ignored) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            for(String sql:AtomNetPublicationR31.DDL)db.execSQL(sql);
            for(String sql:AtomNetStoreR30.DDL)db.execSQL(sql);
            AtomNetSessionR30 state=ready();
            state.materializedSamples=1;
            byte[] x=AtomNetStoreR30.features(sample(COMPLETE,1).x);
            String id=HashingSU1.text("hot"),archive=HashingSU1.text("cold");
            String sql="INSERT INTO r31_atomnet_publication VALUES(?,?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,1,0,1,1000,?,NULL,NULL,'PUBLISHED',10,20)";
            for(String key:Arrays.asList(id,archive))db.execSQL(sql,new Object[]{key,AtomNetPublicationR31.VERSION,AtomNetSessionR30.OWNER,DATA,"",PIN,new byte[]{1},THROUGH,PIN});
            String hash=AtomNetPublicationR31.sampleHash(new Object[]{DATA,0,COMPLETE,1,x,1,0});
            db.execSQL("INSERT INTO r31_atomnet_sample_stage VALUES(?,0,?,1,?,1,0,?)",new Object[]{id,COMPLETE,x,hash});
            db.execSQL("INSERT INTO r324_atomnet_sample_archive VALUES(?,?,?,1,0,?,40)",new Object[]{archive,new byte[]{1,2,3},PIN,PIN});
            assertArrayEquals(sample(COMPLETE,1).x,AtomNetStoreR30.samples(db,state).get(0).x,0);
            try { AtomNetSampleArchiveR324.read(db,DATA,java.util.Collections.singleton(0),row->fail("corrupt archive decoded"));fail("invalid archive accepted as unique source"); }
            catch(IllegalStateException expected) { assertTrue(expected.getMessage().contains("R324_SAMPLE_ARCHIVE_READ_FAILED")); }
        }
    }
}
