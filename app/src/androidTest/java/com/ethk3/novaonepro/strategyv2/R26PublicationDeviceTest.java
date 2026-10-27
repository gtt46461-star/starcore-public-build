package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real writer/checkpoints on isolated synthetic history. No natural evidence or phone SLO claim. */
@RunWith(AndroidJUnit4.class)
public final class R26PublicationDeviceTest {
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static final class Fixture implements AutoCloseable {
        final R26RuntimeDeviceTest.Sandbox app;
        DbStore store;
        SQLiteDatabase db;
        final long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001");
        final RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("publication-test","SYNTHETIC",1,0);
        Fixture(int count)throws Exception{
            assertNull(DatabaseCoordinator.peek());app=new R26RuntimeDeviceTest.Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext());store=new DbStore(app);db=store.readable();
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_PUBLICATION_SCHEMA",w->{RuntimeSchemaR26.ensure(w);for(String sql:SqlStatementsSU1.SQL)w.execSQL(sql);StrategySchemaSU2.ensure(w);long now=System.currentTimeMillis();w.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',?,0)",new Object[]{now});w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('publication-test','P2','test',1,?,'SYNTHETIC','RUNNING')",new Object[]{now+600000});return 0;});
            List<Models.Draw> history=new ArrayList<>();for(int i=0;i<count;i++)history.add(draw(i,System.currentTimeMillis()-(count-i+1L)*60000));assertEquals(count,store.saveDraws(history));
        }
        Models.Draw draw(int index,long opened){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+index);d.numbers=new int[10];for(int p=0;p<10;p++)d.numbers[p]=(p+index%7)%10+1;d.openTime=opened;d.fetchTime=opened+500;return d;}
        BudgetTokenSU1 budget(){return new BudgetTokenSU1(30000,100000000,()->false);}
        FoundryCampaignR26.Snapshot prefix(int count){FoundryCampaignR26.Snapshot all=FoundryCampaignR26.capture(store),s=new FoundryCampaignR26.Snapshot();s.ordinal=Arrays.copyOf(all.ordinal,count);s.issues=Arrays.copyOf(all.issues,count);s.opened=Arrays.copyOf(all.opened,count);s.available=Arrays.copyOf(all.available,count);s.draws=Arrays.copyOf(all.draws,count);s.through=s.ordinal[count-1];return s;}
        String campaign(FoundryCampaignR26.Snapshot s)throws Exception{byte[] bytes=s.encode();String hash=LiveStateR26.hash(bytes),id="SYNTHETIC:"+hash;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_CAMPAIGN",w->{w.execSQL("INSERT INTO r26_foundry VALUES(?,?,?,?,'RUNNING',?,?,?)",new Object[]{id,hash,s.through,bytes,System.currentTimeMillis(),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,s.ordinal[0]});return 0;});return id;}
        ModelGenomeSU2 genome(String family,FoundryCampaignR26.Snapshot s)throws Exception{return FamilyRegistryR26.genome(family).bindBirth(LiveStateR26.hash(s.encode()),s.through);}
        byte[] trained(ModelGenomeSU2 g,FoundryCampaignR26.Snapshot s,int count)throws Exception{LiveStateR26 state=new LiveStateR26(g);for(int i=0;i<count;i++)state.learn(s.draws[i]);return state.encode();}
        void candidate(String campaign,ModelGenomeSU2 g,byte[] bytes,int cursor,String status,long cpu)throws Exception{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_CANDIDATE",w->{w.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,operation,service_cpu_ms) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{campaign,g.key(),g.text(),cursor,bytes,bytes==null?"":LiveStateR26.hash(bytes),"SYNTHETIC:"+g.key(),status,"REGISTERED_SEED",cpu});return 0;});}
        void live(ModelGenomeSU2 g,byte[] bytes,long through,long born,long updated){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_LIVE",w->{w.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,?,?,?,?,'',?,?,?,?,?)",new Object[]{g.key(),g.text(),bytes,LiveStateR26.hash(bytes),"SYNTHETIC:"+g.key(),through,first,born,updated,"F17"});return 0;});}
        @Override public void close(){store.close();R26RuntimeDeviceTest.remove(app.root);}
    }

    @Test public void completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot s=f.prefix(128);String campaign=f.campaign(s);ModelGenomeSU2 complete=f.genome("F17",s),peer=f.genome("F01",s);f.candidate(campaign,complete,f.trained(complete,s,128),128,"DONE",10);f.candidate(campaign,peer,null,0,"READY",0);
            assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_foundry WHERE status='RUNNING'"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='READY' AND cursor=0"));assertEquals(f.first+128,scalar(f.db,"SELECT born_ordinal FROM r26_live_model"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_cohort WHERE incumbent<>''"));assertNull(CertificateSU2.read(f.db,complete.key(),System.currentTimeMillis()));
            Models.Draw future=f.draw(128,System.currentTimeMillis()-2000);assertTrue(f.store.saveDraw(future)>=0);
            assertEquals("Raw future data cannot bypass terminal feedback",0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(f.first+127,scalar(f.db,"SELECT through_ordinal FROM r26_live_model"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_consumption"));
            // Synthetic terminal event fixture, not a claim that the full product settlement path was exercised.
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_TERMINAL_EVENT",w->{RuntimeSchemaR26.feedbackWithinTransaction(w,future);RuntimeSchemaR26.feedbackWithinTransaction(w,future);return 0;});
            assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(f.first+128,scalar(f.db,"SELECT through_ordinal FROM r26_live_model"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_consumption"));
            String target=TargetIssueResolver.fromSequenceOrdinalSU1(f.first+129);assertEquals(1,LivePoolR26.capture(f.store,target));float[][] table=LivePoolR26.frozenTable(f.db,complete.key(),f.first+129);assertNotNull(table);assertEquals(10,table.length);assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_frozen WHERE input_through<target_ordinal AND frozen_at<deadline_at AND length(probability_table)=400"));
        }
    }
    @Test public void forgedCompletionCannotAllocateFutureQualification()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot s=f.prefix(128);String campaign=f.campaign(s);ModelGenomeSU2 g=f.genome("F17",s);f.candidate(campaign,g,f.trained(g,s,64),128,"DONE",0);
            assertEquals(0,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_live_model"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));
            ModelGenomeSU2 wrongBirth=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("WRONG_BIRTH"),s.through);f.candidate(campaign,wrongBirth,f.trained(wrongBirth,s,128),128,"DONE",0);
            assertEquals(0,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));
        }
    }
    @Test public void waitingFuturePolicyDoesNotStarveReadyPrebirthCatchup()throws Exception{
        try(Fixture f=new Fixture(132)){
            FoundryCampaignR26.Snapshot a=f.prefix(129),b=f.prefix(128);ModelGenomeSU2 ga=f.genome("F17",a),gb=f.genome("F17",b);f.live(ga,f.trained(ga,a,129),f.first+128,f.first+129,1);f.live(gb,f.trained(gb,b,128),f.first+127,f.first+132,2);
            assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(2,scalar(f.db,"SELECT count(*) FROM r26_live_model WHERE through_ordinal="+(f.first+128)));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_consumption"));
        }
    }
    @Test public void candidateSchedulingUsesMeasuredCpuAndCorruptPeerIsQuarantinedAlone()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot s=f.prefix(128);String campaign=f.campaign(s);ModelGenomeSU2 light=f.genome("F17",s),heavy=f.genome("F01",s);f.candidate(campaign,light,f.trained(light,s,8),8,"READY",0);f.candidate(campaign,heavy,null,0,"READY",100000);
            assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);assertEquals(0,scalar(f.db,"SELECT cursor FROM r26_candidate WHERE service_cpu_ms=100000"));assertTrue(scalar(f.db,"SELECT service_cpu_ms FROM r26_candidate WHERE policy_hash='"+light.key()+"'")>0);
            ModelGenomeSU2 corrupt=f.genome("F18",s);f.candidate(campaign,corrupt,new byte[]{1,2,3},1,"READY",0);assertEquals(0,FoundryCampaignR26.step(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='BLOCKED_CHECKPOINT_OR_PROGRAM' AND length(state)=3"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_live_failure"));assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
        }
    }

    private static XiIncumbentSU2.Decision originalFixture(){
        float[][] raw=new float[10][10];for(float[] row:raw){Arrays.fill(row,.08f);row[0]=.28f;}
        return new XiIncumbentSU2.Decision(false,1,new int[]{1,2,3},.3,raw);
    }
    @Test public void protectedOriginalFreezeIsAtomicPersistentAndSourceFenced()throws Exception{
        try(Fixture f=new Fixture(128)){
            assertTrue(f.store.saveDraw(f.draw(128,System.currentTimeMillis()-2000))>=0);
            String target=TargetIssueResolver.fromSequenceOrdinalSU1(f.first+129);EventFrameSU1 frame=EventFrameBuilderSU1.capture(f.store,target);XiIncumbentSU2.Decision x=originalFixture();
            RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim("publication-test","SYNTHETIC",2,0);
            try{IncumbentLoopSU2.commitPrepared(f.store,()->stale,frame,x);fail("stale owner froze original");}catch(RuntimeException expected){}
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            assertTrue(f.store.saveDraw(f.draw(129,System.currentTimeMillis()-2000))>=0);assertEquals(0,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,frame,x));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));
            long seq=f.first+130;EventFrameSU1 fresh=EventFrameBuilderSU1.capture(f.store,TargetIssueResolver.fromSequenceOrdinalSU1(seq));assertEquals(1,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,fresh,x));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            f.store.close();f.store=new DbStore(f.app);f.db=f.store.readable();float[][] restored=IncumbentLoopSU2.pendingTable(f.db,seq);assertNotNull(restored);
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)assertEquals(Float.floatToIntBits(x.table[p][n]),Float.floatToIntBits(restored[p][n]));assertTrue(restored[0][0]*10>2);
            assertNull(CertificateSU2.read(f.db,IncumbentLoopSU2.KEY,System.currentTimeMillis()));
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_OLD_OPTIONAL_LAYOUT",w->{w.execSQL("DROP TABLE r26_incumbent_frozen");return 0;});assertTrue(RuntimeSchemaR26.ready(f.db));assertNull(IncumbentLoopSU2.pendingTable(f.db,seq));
        }
    }
    @Test public void protectedOriginalFreezeRejectsExpiredDeadlineAndReplacement()throws Exception{
        try(Fixture f=new Fixture(128)){
            assertTrue(f.store.saveDraw(f.draw(128,System.currentTimeMillis()-2000))>=0);
            EventFrameSU1 fresh=EventFrameBuilderSU1.capture(f.store,TargetIssueResolver.fromSequenceOrdinalSU1(f.first+129));long[] issues=new long[fresh.size()],available=new long[fresh.size()];for(int i=0;i<issues.length;i++){issues[i]=fresh.issue(i);available[i]=fresh.availableAt(i);}
            long now=System.currentTimeMillis();EventFrameSU1 expired=new EventFrameSU1(fresh.targetIssue,now-500,now-1000,fresh.sourceEpoch,issues,available,fresh.history(),fresh.sourceGeneration);XiIncumbentSU2.Decision x=originalFixture();
            assertEquals(0,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,expired,x));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            assertEquals(1,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,fresh,x));assertEquals(0,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,fresh,x));
            try{f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_IMMUTABLE_ORIGINAL_FREEZE",w->{w.execSQL("INSERT OR REPLACE INTO r26_incumbent_frozen SELECT * FROM r26_incumbent_frozen");return 0;});fail("original preferences replaced");}catch(RuntimeException expected){}
            assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));
        }
    }

    @Test public void protectedOriginalRunsThroughTheFencedContinuationOnActualCanonicalHistory()throws Exception{
        try(Fixture f=new Fixture(1600)){
            try{
                assertTrue(f.store.saveDraw(f.draw(1600,System.currentTimeMillis()-2000))>=0);long target=f.first+1601;
                f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_ORIGINAL_OWNER_TASK",w->{w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('incumbent-forecast','P1','incumbent-forecast',1,?,'SYNTHETIC','RUNNING')",new Object[]{System.currentTimeMillis()+120000});return 0;});
                RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("incumbent-forecast","SYNTHETIC",1,0);long stop=System.nanoTime()+55_000_000_000L,done=0;int slices=0;
                while(done==0&&System.nanoTime()<stop){done=TaskContinuationR26.step(f.store,claim,f.budget(),target);slices++;}
                assertEquals("Original method did not finish before its real next-target cutoff",1,done);assertTrue(slices>0);
                assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen WHERE target_ordinal="+target+" AND input_through="+(target-1)+" AND frozen_at<deadline_at"));
                assertNotNull(IncumbentLoopSU2.pendingTable(f.db,target));assertEquals(1,scalar(f.db,"SELECT count(*) FROM su2_live_decision WHERE sequence_ordinal="+target+" AND hit IS NULL"));
            }finally{TaskContinuationR26.cancel("SYNTHETIC");assertTrue("Original helper did not drain before database close",CooperativeContinuationR26.awaitOwnerStopped("SYNTHETIC",10000));}
        }
    }
}
