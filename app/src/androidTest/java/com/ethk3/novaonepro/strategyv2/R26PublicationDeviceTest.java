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
        final boolean keepFiles;
        final long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001");
        final RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("publication-test","SYNTHETIC",1,0);
        Fixture(int count)throws Exception{this(count,null,true,false);}
        Fixture(int count,String name,boolean initialize,boolean keep)throws Exception{
            assertNull("Each fixture must start in a fresh instrumentation process",DatabaseCoordinator.peek());keepFiles=keep;
            app=name==null?new R26RuntimeDeviceTest.Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext()):new R26RuntimeDeviceTest.Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext(),name);store=new DbStore(app);db=store.readable();
            if(initialize){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_PUBLICATION_SCHEMA",w->{RuntimeSchemaR26.ensure(w);for(String sql:SqlStatementsSU1.SQL)w.execSQL(sql);StrategySchemaSU2.ensure(w);long now=System.currentTimeMillis();w.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',?,0)",new Object[]{now});w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('publication-test','P2','test',1,?,'SYNTHETIC','RUNNING')",new Object[]{now+600000});return 0;});
            }
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
        @Override public void close(){store.close();if(!keepFiles)R26RuntimeDeviceTest.remove(app.root);}
    }

    @Test public void failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality()throws Exception{try(Fixture f=new Fixture(128)){verifyFailureDirectedResearch(f.store,f.db,f.claim);}}
    public static void verifyFailureDirectedResearch(DbStore store,SQLiteDatabase db,RuntimeCoordinatorR26.Claim claim)throws Exception{
        FoundryCampaignR26.Snapshot source=FoundryCampaignR26.capture(store);String snapshot=LiveStateR26.hash(source.encode());long birth=source.ordinal[0]-QualificationWindowR26.LENGTH;String digest=PolicyIdentityR26.sha256("SYNTHETIC_FAILED_LOOK");ModelGenomeSU2 parent=FamilyRegistryR26.genome("F03").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_PARENT_SOURCE"),birth-1);byte[] initial=new LiveStateR26(parent).encode();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_FAILED_PARENT",w->{w.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,?,?,?,?,'',?,?,?,1,'F03')",new Object[]{parent.key(),parent.text(),initial,LiveStateR26.hash(initial),"SYNTHETIC",source.through,birth,birth});w.execSQL("INSERT INTO r26_cohort(genome_key,from_sequence,to_sequence,incumbent,protocol,family_alpha) VALUES(?,?,?,'SYNTHETIC_BASELINE',?,0.01)",new Object[]{parent.key(),birth,birth+QualificationWindowR26.LENGTH,QualificationWindowR26.PROTOCOL});w.execSQL("INSERT INTO r26_qualification_result VALUES(?,?,?,1,?,20000,6000,0.3,0.25,?,0.3,-0.02,15000,5000,0.02,'OBSERVED_HIT_RATE_LT_035',1)",new Object[]{parent.key(),birth,birth+QualificationWindowR26.LENGTH,digest,20000.0/QualificationWindowR26.LENGTH});return 0;});
        assertTrue(FailureDirectedResearchR26.select(db,snapshot,source.ordinal[0]-2).isEmpty());List<FailureDirectedResearchR26.Batch> batches=FailureDirectedResearchR26.select(db,snapshot,source.through);assertEquals(1,batches.size());FailureDirectedResearchR26.Batch batch=batches.get(0);assertEquals(3,batch.proposals.size());assertEquals("NO_OP",batch.proposals.get(0).category);for(StructuralProposalR26.Proposal p:batch.proposals){assertNotEquals(parent.key(),p.genome.key());assertEquals(snapshot,p.genome.members.get(0).params.get("_birth_snapshot"));assertEquals(Long.toString(source.through),p.genome.members.get(0).params.get("_birth_through"));assertFalse(StructuralProposalR26.control(p.genome));}assertEquals(StructuralProposalR26.structure(parent),StructuralProposalR26.structure(batch.proposals.get(0).genome));for(int i=1;i<batch.proposals.size();i++)assertNotEquals(StructuralProposalR26.structure(parent),StructuralProposalR26.structure(batch.proposals.get(i).genome));
        for(String excluded:new String[]{"PASS","CALENDAR_INCOMPLETE","ACTS_LT_600"}){db.execSQL("UPDATE r26_qualification_result SET reason=?",new Object[]{excluded});assertTrue(FailureDirectedResearchR26.select(db,snapshot,source.through).isEmpty());}try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_CHANGED_FAILURE_EVIDENCE",w->{FailureDirectedResearchR26.recordWithin(w,"SYNTHETIC_UNBORN",batches);return 0;});fail("Changed failure evidence registered children");}catch(IllegalStateException expected){assertEquals("R26_FAILURE_EVIDENCE_CHANGED",expected.getMessage());}db.execSQL("UPDATE r26_qualification_result SET reason='OBSERVED_HIT_RATE_LT_035'");
        RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim(claim.task,claim.owner,0,0);try{FoundryCampaignR26.step(store,()->stale,new BudgetTokenSU1(30000,100000000,()->false));fail("Stale owner created research");}catch(IllegalStateException expected){}assertEquals(0,scalar(db,"SELECT count(*) FROM r26_foundry"));
        db.execSQL("CREATE TRIGGER synthetic_abort_failure_cursor BEFORE INSERT ON r26_failure_research_cursor BEGIN SELECT RAISE(ABORT,'SYNTHETIC_FAILURE_CURSOR_ABORT'); END");try{FoundryCampaignR26.step(store,()->claim,new BudgetTokenSU1(30000,100000000,()->false));fail("Failed cursor left a campaign");}catch(RuntimeException expected){}db.execSQL("DROP TRIGGER synthetic_abort_failure_cursor");assertEquals(0,scalar(db,"SELECT count(*) FROM r26_foundry"));assertEquals(0,scalar(db,"SELECT count(*) FROM r26_candidate"));assertEquals(0,scalar(db,"SELECT count(*) FROM r26_evolution"));assertEquals(0,scalar(db,"SELECT count(*) FROM r26_failure_research_cursor"));
        assertEquals(0,FoundryCampaignR26.step(store,()->claim,new BudgetTokenSU1(30000,100000000,()->false)));assertEquals(1,scalar(db,"SELECT count(*) FROM r26_failure_research_cursor"));assertEquals(3,scalar(db,"SELECT count(*) FROM r26_evolution WHERE parent='"+parent.key()+"' AND operation LIKE 'FAILURE_GUIDED_V1:%'"));assertTrue(FailureDirectedResearchR26.select(db,snapshot,source.through).isEmpty());String campaign;try(Cursor c=db.rawQuery("SELECT campaign_id,proposal_json FROM r26_failure_research_cursor",null)){assertTrue(c.moveToFirst());campaign=c.getString(0);org.json.JSONObject meta=new org.json.JSONObject(c.getString(1));assertEquals(digest,meta.getString("failureEvidenceDigest"));assertEquals(3,meta.getJSONArray("children").length());assertTrue(meta.getString("evidenceScope").contains("RESEARCH_INPUT_ONLY"));}
        final String id=campaign;try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_DUPLICATE_FAILURE_CURSOR",w->{FailureDirectedResearchR26.recordWithin(w,id,batches);return 0;});fail("Old selection consumed twice");}catch(IllegalStateException expected){}
        String child=batch.proposals.get(1).genome.key();store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_SERVE_FAILURE_CHILD",w->{w.execSQL("UPDATE r26_candidate SET status='DONE' WHERE policy_hash<>?",new Object[]{child});return 0;});assertTrue(FoundryCampaignR26.step(store,()->claim,new BudgetTokenSU1(30000,100000000,()->false))>0);assertTrue(scalar(db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+child+"'")>0);assertTrue(scalar(db,"SELECT count(*) FROM r26_training_step WHERE policy_hash='"+child+"'")>0);assertTrue(TrainingStepStoreR26.verify(db));assertEquals(1,scalar(db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(db,"SELECT count(*) FROM su2_certificate"));assertEquals(0,scalar(db,"SELECT sum(prospective_qualified+verified_improvement) FROM r26_evolution"));org.json.JSONObject research=StrategyAssessmentR26.snapshot(db).getJSONObject("failureDirectedResearch");assertEquals(1,research.getLong("totalBatches"));org.json.JSONObject reportBatch=research.getJSONArray("batches").getJSONObject(0);assertEquals("OBSERVED_RECORDS",reportBatch.getString("status"));assertEquals(digest,reportBatch.getJSONObject("birthRecord").getString("failureEvidenceDigest"));boolean trained=false;org.json.JSONArray childStates=reportBatch.getJSONArray("currentChildren");for(int i=0;i<childStates.length();i++)if(childStates.getJSONObject(i).getString("policy").equals(child)){assertTrue(childStates.getJSONObject(i).getLong("labelsConsumed")>0);assertTrue(childStates.getJSONObject(i).getLong("retainedMeasuredUpdates")>0);trained=true;}assertTrue(trained);assertFalse(reportBatch.getBoolean("authorityGranted"));
    }

    @Test public void completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot s=f.prefix(128);String campaign=f.campaign(s);ModelGenomeSU2 complete=f.genome("F17",s),peer=f.genome("F01",s);f.candidate(campaign,complete,f.trained(complete,s,128),128,"DONE",10);f.candidate(campaign,peer,null,0,"READY",0);
            assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_foundry WHERE status='RUNNING'"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='READY' AND cursor=0"));assertEquals(f.first+128,scalar(f.db,"SELECT born_ordinal FROM r26_live_model"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_cohort WHERE incumbent<>''"));assertNull(CertificateSU2.read(f.db,complete.key(),System.currentTimeMillis()));
            Models.Draw future=f.draw(128,System.currentTimeMillis()-2000);assertTrue(f.store.saveDraw(future)>=0);
            assertEquals("Raw future data cannot bypass terminal feedback",0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(f.first+127,scalar(f.db,"SELECT through_ordinal FROM r26_live_model"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_consumption"));
            // Actual sole settlement owner on the isolated synthetic DB, with no invented pre-draw OOS row.
            SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
            SettlementTaskStoreR36.Terminal terminal=SettlementTaskStoreR36.terminal(f.store,future.issue);assertNotNull("Sole settlement owner did not commit a terminal",terminal);assertEquals(SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL,terminal.status);
            assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_feedback"));SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_feedback"));
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
        java.io.File prior=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"r26-synthetic-original-freeze-restart");if(prior.exists())R26RuntimeDeviceTest.remove(prior);
        try(Fixture f=new Fixture(128,"r26-synthetic-original-freeze-restart",true,true)){
            assertTrue(f.store.saveDraw(f.draw(128,System.currentTimeMillis()-2000))>=0);
            String target=TargetIssueResolver.fromSequenceOrdinalSU1(f.first+129);EventFrameSU1 frame=EventFrameBuilderSU1.capture(f.store,target);XiIncumbentSU2.Decision x=originalFixture();
            RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim("publication-test","SYNTHETIC",2,0);
            try{IncumbentLoopSU2.commitPrepared(f.store,()->stale,frame,x);fail("stale owner froze original");}catch(RuntimeException expected){}
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            assertTrue(f.store.saveDraw(f.draw(129,System.currentTimeMillis()-2000))>=0);assertEquals(0,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,frame,x));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));
            long seq=f.first+130;EventFrameSU1 fresh=EventFrameBuilderSU1.capture(f.store,TargetIssueResolver.fromSequenceOrdinalSU1(seq));assertEquals(1,IncumbentLoopSU2.commitPrepared(f.store,()->f.claim,fresh,x));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            float[][] restored=IncumbentLoopSU2.pendingTable(f.db,seq);assertNotNull(restored);
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)assertEquals(Float.floatToIntBits(x.table[p][n]),Float.floatToIntBits(restored[p][n]));assertTrue(restored[0][0]*10>2);
            assertNull(CertificateSU2.read(f.db,IncumbentLoopSU2.KEY,System.currentTimeMillis()));

        }
    }

    /** Ordered after its producer by the source-bound manifest; the harness force-stops between methods. */
    @Test public void protectedOriginalFreezeSurvivesFreshInstrumentationProcess()throws Exception{
        try(Fixture f=new Fixture(0,"r26-synthetic-original-freeze-restart",false,false)){
            long seq=f.first+130;assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_incumbent_frozen"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM su2_live_decision"));
            float[][] restored=IncumbentLoopSU2.pendingTable(f.db,seq);assertNotNull("Original preferences were not persisted across process replacement",restored);XiIncumbentSU2.Decision x=originalFixture();
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)assertEquals(Float.floatToIntBits(x.table[p][n]),Float.floatToIntBits(restored[p][n]));
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

    private static StageStrategySU2.Definition warmStage(String text,long through){
        int columns=StageLayerSU2.BASE_COLS+1;double[][] variance=new double[1][columns];Arrays.fill(variance[0],1);double[] sd=new double[columns];Arrays.fill(sd,1);double[] seed=new double[100];Arrays.fill(seed,.3);
        StageLayerSU2.Layer layer=new StageLayerSU2.Layer(new StageLayerSU2.Gmm(new double[1][columns],variance,new double[]{1},0),new double[columns],sd,new double[1][4],new double[]{1});
        return new StageStrategySU2.Definition("F30",.3,through,Collections.singletonList(text),layer,seed);
    }
    @Test public void expertPairContinuesLearnedModelsButResetsMetricsAndUnpinsExpiredRawState()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source),snapshotHash=LiveStateR26.hash(source.encode()),expert="E07",id=HashingSU1.fields("SYNTHETIC_EXPERT_PARENT",expert,campaign);ExpertExperimentR26.Pair pair=new ExpertExperimentR26.Pair(ExpertExperimentR26.spec(expert),snapshotHash,source.through);for(int i=0;i<40;i++)pair.advance(source.draws[i],false);byte[] state=pair.encode();String stateHash=LiveStateR26.hash(state),checkpoint=HashingSU1.fields("R26_EXPERT_CHECKPOINT_V1",id,"","40",stateHash);CheckpointArtifactsR26 files=CheckpointArtifactsR26.forStore(f.store);byte[] expiredReference;
            RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("expert-E07","SYNTHETIC",1,0);try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(f.store,state)){expiredReference=file.reference();f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPERT_PARENT",db->{db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('expert-E07','P2','E07',1,?,'SYNTHETIC','RUNNING')",new Object[]{System.currentTimeMillis()+600000});db.execSQL("INSERT INTO r26_expert_experiment(experiment_id,expert_id,campaign_id,cursor,state,state_hash,checkpoint_hash,parent_hash,numerical_abi,proposal_json,created_at) VALUES(?,?,?,40,?,?,?,'',?,'{}',1)",new Object[]{id,expert,campaign,file.reference(),stateHash,checkpoint,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});CheckpointManifestR26.record(db,checkpoint,id,campaign,"",source.ordinal[39],file.reference());return 0;});}
            for(int window=1;window<=2;window++){
                List<Models.Draw> later=new ArrayList<>();for(int i=0;i<64;i++)later.add(f.draw(window*6000+i,System.currentTimeMillis()-(65-i)*60000L));assertEquals(64,f.store.saveDraws(later));f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPERT_EXPIRY",db->{db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE length(snapshot)>0");ExpertContinuationR26.preserveExpired(db);return 0;});
                if(window==1){try{ExpertContinuationR26.step(f.store,()->new RuntimeCoordinatorR26.Claim("expert-E07","SYNTHETIC",0,0),f.budget(),expert);fail("Stale expert owner transferred models");}catch(IllegalStateException expected){}}
                assertEquals(1,ExpertContinuationR26.step(f.store,()->claim,f.budget(),expert));try(Cursor row=f.db.rawQuery("SELECT e.state,e.state_hash,f.snapshot FROM r26_expert_experiment e JOIN r26_foundry f ON f.campaign_id=e.campaign_id WHERE e.status='RUNNING'",null)){assertTrue(row.moveToFirst());ExpertExperimentR26.Pair child=ExpertExperimentR26.Pair.decode(CheckpointArtifactsR26.resolve(f.store,row.getBlob(0)));FoundryCampaignR26.Snapshot suffix=FoundryCampaignR26.Snapshot.decode(row.getBlob(2));child.requireSource(LiveStateR26.hash(row.getBlob(2)),suffix.ordinal[0],suffix.through);assertEquals(f.first+window*6000,suffix.ordinal[0]);assertEquals(40,child.retainedModelSamples);assertEquals(40,child.left.samples());assertEquals(40,child.right.samples());assertEquals(0,child.cursor);assertEquals(0,child.actions[0]+child.actions[1]+child.probabilityN[0]+child.probabilityN[1]);pair.left.onGap();pair.right.onGap();assertArrayEquals(pair.left.encode(),child.left.encode());assertArrayEquals(pair.right.encode(),child.right.encode());}
            }
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_live_model"));assertFalse(CheckpointReferencesR26.hashes(f.db).contains(CheckpointArtifactsR26.referenceHash(expiredReference)));files.prune(()->CheckpointReferencesR26.hashes(f.db),System.currentTimeMillis()+120000,0,256);try{CheckpointArtifactsR26.resolve(f.store,expiredReference);fail("Expired expert raw context stayed pinned");}catch(java.io.IOException expected){}
            ExpertExperimentR26.Pair reference;FoundryCampaignR26.Snapshot suffix;try(Cursor row=f.db.rawQuery("SELECT e.state,f.snapshot FROM r26_expert_experiment e JOIN r26_foundry f ON f.campaign_id=e.campaign_id WHERE e.status='RUNNING'",null)){assertTrue(row.moveToFirst());reference=ExpertExperimentR26.Pair.decode(CheckpointArtifactsR26.resolve(f.store,row.getBlob(0)));suffix=FoundryCampaignR26.Snapshot.decode(row.getBlob(1));}assertTrue(ExpertWorkshopR26.step(f.store,()->claim,f.budget(),suffix.through)>0);
            try(Cursor row=f.db.rawQuery("SELECT state,cursor FROM r26_expert_experiment WHERE status='RUNNING'",null)){assertTrue(row.moveToFirst());int learned=row.getInt(1);for(int i=0;i<learned;i++)reference.advance(suffix.draws[i],i>0&&suffix.ordinal[i]!=suffix.ordinal[i-1]+1);assertArrayEquals(reference.encode(),CheckpointArtifactsR26.resolve(f.store,row.getBlob(0)));assertEquals(40+learned,reference.left.samples());assertEquals(2*learned,scalar(f.db,"SELECT count(*) FROM r26_training_step"));}
            assertTrue(TrainingStepStoreR26.verify(f.db));assertEquals(41,scalar(f.db,"SELECT min(kernel_update) FROM r26_training_step"));assertEquals(f.first+12000,scalar(f.db,"SELECT min(sample_ordinal) FROM r26_training_step"));
        }
    }
    @Test public void expiredLegacyProgramCommitteeAndStageKeepLearnedStateAcrossTwoRawWindows()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot oldSource=f.prefix(128);String sourceHash=LiveStateR26.hash(oldSource.encode());ModelGenomeSU2 first=FamilyRegistryR26.genome("F20").bindBirth(sourceHash,oldSource.through),second=FamilyRegistryR26.genome("F19").bindBirth(sourceHash,oldSource.through);
            StageStrategySU2.Definition stage=warmStage(first.text(),oldSource.through);StageStrategySU2.register(stage);List<String> canonical=Arrays.asList(first.text(),new CommitteeSU2(Arrays.asList(first,second),.3).text(),stage.text());LegacyReplayStateR26 old=new LegacyReplayStateR26(canonical,f.first);for(int i=0;i<37;i++)old.advance(oldSource.draws[i],oldSource.ordinal[i]);
            String roster=HashingSU1.fields("R26_LEGACY_ROSTER_V1",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,PolicyIdentityR26.canonical(canonical));CheckpointArtifactsR26 files=CheckpointArtifactsR26.forStore(f.store);String parent;byte[] expiredLegacyReference;
            try(CheckpointArtifactsR26.Prepared file=files.prepareLegacy(old)){
                expiredLegacyReference=file.reference();String hash=LiveStateR26.hash(files.resolve(file.reference()));parent=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",roster,sourceHash,"0",hash);final String checkpoint=parent;
                f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPIRED_LEGACY",db->{db.execSQL("INSERT INTO r26_legacy_runtime(singleton,roster_hash,snapshot_hash,snapshot,source_through,genesis_ordinal,through_ordinal,cursor,state,state_hash,checkpoint_hash,parent_hash,numerical_abi,status,updated_at) VALUES(1,?,?,X'',?,?,?,?,?,?,?,'',?,'EXPIRED_RETENTION',1)",new Object[]{roster,sourceHash,oldSource.through,f.first,old.through(),37,file.reference(),hash,checkpoint,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});CheckpointManifestR26.record(db,checkpoint,"LEGACY:"+roster,sourceHash,"",old.through(),file.reference());return 0;});
            }
            old.onGap();
            for(int window=1;window<=2;window++){
                List<Models.Draw> later=new ArrayList<>();for(int i=0;i<64;i++)later.add(f.draw(window*6000+i,System.currentTimeMillis()-(65-i)*60000L));assertEquals(64,f.store.saveDraws(later));final String expectedParent=parent;
                if(window==1){RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim(f.claim.task,f.claim.owner,0,0);try{LegacyReplayR26.resumeExpired(f.store,()->stale,f.budget());fail("Stale legacy owner forked state");}catch(IllegalStateException expected){}assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_legacy_runtime WHERE status='BOOTSTRAP'"));}
                assertEquals(1,LegacyReplayR26.resumeExpired(f.store,()->f.claim,f.budget()));
                try(Cursor row=f.db.rawQuery("SELECT snapshot,state,state_hash,checkpoint_hash,parent_hash,cursor,source_through FROM r26_legacy_runtime",null)){assertTrue(row.moveToFirst());FoundryCampaignR26.Snapshot suffix=FoundryCampaignR26.Snapshot.decode(row.getBlob(0));assertEquals(64,suffix.draws.length);assertEquals(f.first+window*6000,suffix.ordinal[0]);assertTrue(suffix.ordinal[0]>oldSource.through);LegacyReplayStateR26 restored=files.restoreLegacy(row.getBlob(1),row.getString(2));assertArrayEquals(old.encode(),restored.encode());assertEquals(37,restored.samples());assertEquals(expectedParent,row.getString(4));assertEquals(0,row.getInt(5));parent=row.getString(3);
                    LegacyReplayStateR26 reference=LegacyReplayStateR26.decode(old.encode());restored.advance(suffix.draws[0],suffix.ordinal[0]);reference.advance(suffix.draws[0],suffix.ordinal[0]);assertArrayEquals(reference.encode(),restored.encode());assertEquals(38,restored.samples());}
                assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_live_model"));assertEquals(window+1,scalar(f.db,"SELECT count(*) FROM r26_checkpoint_artifact WHERE policy_hash='LEGACY:"+roster+"'"));
                if(window==1)f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_UNSERVED_LEGACY_EXPIRY",db->{db.execSQL("UPDATE r26_legacy_runtime SET status='EXPIRED_RETENTION',snapshot=X''");return 0;});
            }
            assertFalse(CheckpointReferencesR26.hashes(f.db).contains(CheckpointArtifactsR26.referenceHash(expiredLegacyReference)));files.prune(()->CheckpointReferencesR26.hashes(f.db),System.currentTimeMillis()+120000,0,256);try{CheckpointArtifactsR26.resolve(f.store,expiredLegacyReference);fail("Expired legacy raw context stayed pinned");}catch(java.io.IOException expected){}
            byte[] preserved;try(Cursor row=f.db.rawQuery("SELECT state FROM r26_legacy_runtime",null)){assertTrue(row.moveToFirst());preserved=row.getBlob(0);}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_DAMAGED_LEGACY_BINDING",db->{db.execSQL("UPDATE r26_legacy_runtime SET status='EXPIRED_RETENTION',state_hash='DAMAGED'");return 0;});assertEquals(0,LegacyReplayR26.resumeExpired(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_legacy_runtime WHERE status='BLOCKED_WARM_STATE_PRESERVED'"));try(Cursor row=f.db.rawQuery("SELECT state FROM r26_legacy_runtime",null)){assertTrue(row.moveToFirst());assertArrayEquals(preserved,row.getBlob(0));}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_OLD_LEGACY_ABI",db->{db.execSQL("UPDATE r26_legacy_runtime SET status='EXPIRED_RETENTION',numerical_abi='OLD'");return 0;});assertEquals(0,LegacyReplayR26.resumeExpired(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_legacy_runtime WHERE status='BLOCKED_NUMERICAL_ABI_PRESERVED'"));
        }
    }
    @Test public void fragmentMemoryCheckpointsRunThroughFoundryAndDestructiveControlsCannotPublish()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 full=f.genome("F20",source);
            List<StructuralProposalR26.Proposal> proposals=StructuralProposalR26.mechanismChildren(full);
            for(int i=1;i<proposals.size();i++){ModelGenomeSU2 control=proposals.get(i).genome;assertTrue(StructuralProposalR26.control(control));f.candidate(campaign,control,f.trained(control,source,128),128,"DONE",0);}
            assertEquals("Destructive memory controls must not receive future qualification",0,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));
            f.candidate(campaign,full,null,0,"READY",0);int slices=0;
            while(scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+full.key()+"'")<128&&slices++<130)assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
            assertTrue("Fragment model did not finish actual numerical continuation",slices<=130);
            assertEquals(128,scalar(f.db,"SELECT count(*) FROM r26_training_step WHERE policy_hash='"+full.key()+"'"));assertTrue(TrainingStepStoreR26.verify(f.db));assertTrue(scalar(f.db,"SELECT count(*) FROM r26_training_step WHERE policy_hash='"+full.key()+"' AND parameter_delta_norm>0")>0);
            try(Cursor row=f.db.rawQuery("SELECT state,state_hash,status FROM r26_candidate WHERE policy_hash=?",new String[]{full.key()})){assertTrue(row.moveToFirst());assertEquals("DONE",row.getString(2));byte[] reference=row.getBlob(0);assertFalse(CheckpointArtifactsR26.referenceHash(reference).isEmpty());LiveStateR26 restored=LiveStateR26.decode(full.text(),CheckpointArtifactsR26.resolve(f.store,reference),row.getString(1));assertEquals(128,restored.samples());assertTrue(restored.hub.models.values().iterator().next() instanceof FamilyF20KernelR26);assertTrue(((FamilyF20KernelR26)restored.hub.models.values().iterator().next()).occupied()>0);}
            assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_live_model WHERE family_id='F20' AND status='ACTIVE'"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_live_model"));
            assertEquals("Bootstrap memory is not forward qualification",0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
        }
    }

    @Test public void expertScaleReportUsesExecutedMasksAndPersistsObservedCounts()throws Exception{
        try(Fixture f=new Fixture(128)){
            f.campaign(f.prefix(128));RuntimeCoordinatorR26.Claim expert=new RuntimeCoordinatorR26.Claim("expert-E04","SYNTHETIC",1,0);
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_SCALE_EXPERT",w->{w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('expert-E04','P2','expert-E04',1,?,'SYNTHETIC','RUNNING')",new Object[]{System.currentTimeMillis()+600000});return 0;});
            int calls=0;while(scalar(f.db,"SELECT coalesce(max(cursor),0) FROM r26_expert_experiment WHERE expert_id='E04'")<40&&calls++<130)assertTrue(ExpertWorkshopR26.step(f.store,()->expert,f.budget(),f.first+128)>0);
            try(Cursor row=f.db.rawQuery("SELECT cursor,state,result_json FROM r26_expert_experiment WHERE expert_id='E04'",null)){assertTrue(row.moveToFirst());int count=row.getInt(0);assertTrue(count>=40);org.json.JSONObject result=new org.json.JSONObject(row.getString(2));org.json.JSONArray scales=result.getJSONArray("scaleAvailability");assertEquals(6,scales.length());org.json.JSONArray scale8=scales.getJSONObject(0).getJSONArray("members"),scale32=scales.getJSONObject(1).getJSONArray("members");assertEquals(count-8,scale8.getJSONObject(0).getInt("pastOnlySupportedPredictions"));assertFalse(scale32.getJSONObject(0).getBoolean("implementationDeclaresScale"));assertTrue(scale32.getJSONObject(0).isNull("pastOnlySupportedPredictions"));assertEquals("NOT_DECLARED_BY_EXECUTED_MODEL",scale32.getJSONObject(0).getString("availability"));assertEquals(count-32,scale32.getJSONObject(1).getInt("pastOnlySupportedPredictions"));
                ExpertExperimentR26.Pair restored=ExpertExperimentR26.Pair.decode(CheckpointArtifactsR26.resolve(f.store,row.getBlob(1)));assertEquals(count-8,restored.scalePredictions[0][0]);int previous=restored.scalePredictions[1][1];restored.advance(f.draw(128,System.currentTimeMillis()).numbers,true);assertEquals("Gap must mask unavailable long context",previous,restored.scalePredictions[1][1]);
            }
        }
    }

    private static void laterWindow(Fixture f,int first)throws Exception{
        List<Models.Draw> draws=new ArrayList<>();for(int i=0;i<128;i++)draws.add(f.draw(first+i,System.currentTimeMillis()-(129-i)*60000L));assertEquals(128,f.store.saveDraws(draws));
        f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_RAW_SOURCE_EXPIRY",w->{RetentionR26.prune(w,f.first+first+128);return 0;});
    }
    @Test public void tensorTrainingReceiptsFollowActualCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F19",true);}
    @Test public void matchingTrainingReceiptsFollowActualCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F17",false);}
    @Test public void f03NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F03",false);}
    @Test public void f04NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F04",false);}
    @Test public void f05NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F05",false);}
    @Test public void f06NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F06",false);}
    @Test public void f07NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F07",false);}
    @Test public void chainTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F02",false);}
    @Test public void autoregressiveTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F18",false);}
    @Test public void sparseTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F01",false);}
    @Test public void durationTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas()throws Exception{measuredTrainingReceipts("F09",false);}
    @Test public void statisticalTrainingReceiptsPreserveNoGradientAndFollowTerminalLiveCheckpoint()throws Exception{statisticalTrainingReceipts("F08");}
    @Test public void suffixStatisticsFollowCandidateAndTerminalLiveCheckpoint()throws Exception{statisticalTrainingReceipts("F12");}
    @Test public void relationContextFillDoesNotFabricateStatisticsAndTerminalTrainingCommits()throws Exception{statisticalTrainingReceipts("F21");}
    private void statisticalTrainingReceipts(String family)throws Exception{
        int updates=128-(family.equals("F21")?new FamilyF21KernelR26().contextRequired():0);
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 full=f.genome(family,source);f.candidate(campaign,full,null,0,"READY",0);int slices=0;
            while(scalar(f.db,"SELECT cursor FROM r26_candidate")<128&&slices++<130)assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
            assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertTrue(TrainingStepStoreR26.verify(f.db));
            try(Cursor c=f.db.rawQuery("SELECT state,state_hash FROM r26_candidate",null)){assertTrue(c.moveToFirst());LiveStateR26 state=LiveStateR26.decode(full.text(),CheckpointArtifactsR26.resolve(f.store,c.getBlob(0)),c.getString(1));OnlineModelsSU2.Model kernel=state.hub.models.values().iterator().next();assertTrue(kernel instanceof StatisticalTrainingDiagnosticsR26);assertFalse(kernel instanceof TrainingDiagnosticsR26);assertEquals(updates,((StatisticalTrainingDiagnosticsR26)kernel).completedTrainingUpdates());assertNull(((StatisticalTrainingDiagnosticsR26)kernel).lastStatisticalTrainingStep());}
            try(Cursor c=f.db.rawQuery("PRAGMA table_info(r26_statistic_training_step)",null)){while(c.moveToNext())assertNotEquals("Statistical update must not fill a gradient placeholder","gradient_norm",c.getString(1));}
            assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step s JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=s.batch_checkpoint AND a.policy_hash=s.policy_hash AND a.source_hash=s.campaign_id"));
            for(String sql:new String[]{"UPDATE r26_statistic_training_step SET statistic_delta_norm=0","INSERT OR REPLACE INTO r26_statistic_training_step SELECT * FROM r26_statistic_training_step LIMIT 1"})try{f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_IMMUTABLE_STATISTIC",w->{w.execSQL(sql);return 0;});fail("Statistical training witness mutated");}catch(RuntimeException expected){}
            assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));Models.Draw future=f.draw(128,System.currentTimeMillis()-2000);assertTrue(f.store.saveDraw(future)>=0);assertEquals(0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step"));SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(updates+1,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step"));assertTrue(TrainingStepStoreR26.verify(f.db));
            try(Cursor c=f.db.rawQuery("SELECT s.source_hash,s.source_kind,s.kernel_update,s.batch_checkpoint,l.checkpoint_hash FROM r26_statistic_training_step s JOIN r26_live_model l ON l.policy_hash=s.policy_hash WHERE s.sample_ordinal=?",new String[]{Long.toString(f.first+128)})){assertTrue(c.moveToFirst());assertEquals(DrawHashR36.sha256V2(future),c.getString(0));assertEquals("CANONICAL_TERMINAL_DRAW",c.getString(1));assertEquals(updates+1,c.getLong(2));assertEquals(c.getString(3),c.getString(4));}
            org.json.JSONObject report=new org.json.JSONObject(RuntimeReportR26.json(f.db));assertFalse(report.has("captureFactsError"));org.json.JSONArray sections=report.getJSONArray("sections");boolean measured=false;for(int i=0;i<sections.length();i++)if(sections.getJSONObject(i).getString("sectionId").equals("TRAINING")){org.json.JSONObject result=sections.getJSONObject(i).getJSONObject("metricResults").getJSONObject("weightDelta");assertEquals("OBSERVED",result.getString("status"));org.json.JSONArray rows=result.getJSONArray("value");assertEquals(64,rows.length());for(int j=0;j<rows.length();j++){org.json.JSONObject row=rows.getJSONObject(j);assertTrue(row.has("gradient_norm")&&row.isNull("gradient_norm"));assertEquals("NOT_APPLICABLE_STATISTICAL_UPDATE",row.getString("gradient_availability"));assertTrue(row.getDouble("statistic_delta_norm")>=0);}measured=true;}assertTrue(measured);assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
        }
    }
    @Test public void expertPairTrainingReceiptsCommitWithBothModelsAndRollbackWithCheckpoint()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);RuntimeCoordinatorR26.Claim expert=new RuntimeCoordinatorR26.Claim("expert-E07","SYNTHETIC",1,0);String id=HashingSU1.fields("R26_EXPERT_EXPERIMENT_V1","E07",campaign);
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPERT_RECEIPT_TASK",w->{w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('expert-E07','P2','E07',1,?,'SYNTHETIC','RUNNING')",new Object[]{System.currentTimeMillis()+600000});return 0;});
            assertEquals(1,ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertTrue(ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through)>0);
            long before=scalar(f.db,"SELECT cursor FROM r26_expert_experiment"),rows=scalar(f.db,"SELECT count(*) FROM r26_training_step");assertEquals(2*before,rows);assertTrue(TrainingStepStoreR26.verify(f.db));byte[] state;String checkpoint;
            try(Cursor c=f.db.rawQuery("SELECT state,checkpoint_hash FROM r26_expert_experiment",null)){assertTrue(c.moveToFirst());state=c.getBlob(0);checkpoint=c.getString(1);}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPERT_RECEIPT_FAILURE",w->{w.execSQL("CREATE TRIGGER synthetic_block_expert_receipt BEFORE INSERT ON r26_training_step BEGIN SELECT RAISE(ABORT,'SYNTHETIC_RECEIPT_COMMIT_FAILURE'); END");return 0;});
            try{ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through);fail("Receipt failure did not abort paired checkpoint");}catch(RuntimeException expected){}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EXPERT_RECEIPT_UNBLOCK",w->{w.execSQL("DROP TRIGGER synthetic_block_expert_receipt");return 0;});
            try{ExpertWorkshopR26.step(f.store,()->new RuntimeCoordinatorR26.Claim("expert-E07","SYNTHETIC",0,0),f.budget(),source.through);fail("Stale expert fence committed diagnostics");}catch(RuntimeException expected){}
            assertEquals(before,scalar(f.db,"SELECT cursor FROM r26_expert_experiment"));assertEquals(rows,scalar(f.db,"SELECT count(*) FROM r26_training_step"));try(Cursor c=f.db.rawQuery("SELECT state,checkpoint_hash FROM r26_expert_experiment",null)){assertTrue(c.moveToFirst());assertArrayEquals(state,c.getBlob(0));assertEquals(checkpoint,c.getString(1));}
            int slices=0;while(scalar(f.db,"SELECT cursor FROM r26_expert_experiment")<128&&slices++<130)assertTrue(ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through)>0);assertEquals(256,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertTrue(TrainingStepStoreR26.verify(f.db));assertEquals(1,scalar(f.db,"SELECT min(kernel_update) FROM r26_training_step"));assertEquals(128,scalar(f.db,"SELECT max(kernel_update) FROM r26_training_step"));
            try(Cursor c=f.db.rawQuery("SELECT count(*) FROM r26_training_step s JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=s.batch_checkpoint AND a.policy_hash=s.campaign_id AND a.source_hash=? WHERE s.campaign_id=? AND s.source_hash=? AND s.source_kind='EXPERT_PAIRED_FROZEN_SNAPSHOT'",new String[]{campaign,id,LiveStateR26.hash(source.encode())})){assertTrue(c.moveToFirst());assertEquals(256,c.getInt(0));}
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_live_model"));
        }
    }
    @Test public void mixedStatisticalAndGradientExpertRowsRollbackAsOnePair()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source),id=HashingSU1.fields("R26_EXPERT_EXPERIMENT_V1","E03",campaign);RuntimeCoordinatorR26.Claim expert=new RuntimeCoordinatorR26.Claim("expert-E03","SYNTHETIC",1,0);
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_MIXED_EXPERT_TASK",w->{w.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('expert-E03','P2','E03',1,?,'SYNTHETIC','RUNNING')",new Object[]{System.currentTimeMillis()+600000});return 0;});assertEquals(1,ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through));assertTrue(ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through)>0);
            long cursor=scalar(f.db,"SELECT cursor FROM r26_expert_experiment");assertEquals(cursor,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertEquals(cursor,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step"));byte[] state;String checkpoint;try(Cursor c=f.db.rawQuery("SELECT state,checkpoint_hash FROM r26_expert_experiment",null)){assertTrue(c.moveToFirst());state=c.getBlob(0);checkpoint=c.getString(1);}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_MIXED_EXPERT_FAILURE",w->{w.execSQL("CREATE TRIGGER synthetic_block_mixed_gradient BEFORE INSERT ON r26_training_step BEGIN SELECT RAISE(ABORT,'SYNTHETIC_GRADIENT_AFTER_STATISTIC_FAILURE'); END");return 0;});try{ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through);fail("Failed gradient side committed mixed pair");}catch(RuntimeException expected){}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_MIXED_EXPERT_UNBLOCK",w->{w.execSQL("DROP TRIGGER synthetic_block_mixed_gradient");return 0;});assertEquals(cursor,scalar(f.db,"SELECT cursor FROM r26_expert_experiment"));assertEquals(cursor,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertEquals(cursor,scalar(f.db,"SELECT count(*) FROM r26_statistic_training_step"));try(Cursor c=f.db.rawQuery("SELECT state,checkpoint_hash FROM r26_expert_experiment",null)){assertTrue(c.moveToFirst());assertArrayEquals(state,c.getBlob(0));assertEquals(checkpoint,c.getString(1));}
            int slices=0;while(scalar(f.db,"SELECT cursor FROM r26_expert_experiment")<128&&slices++<130)assertTrue(ExpertWorkshopR26.step(f.store,()->expert,f.budget(),source.through)>0);for(String table:new String[]{"r26_training_step","r26_statistic_training_step"})try(Cursor c=f.db.rawQuery("SELECT count(*) FROM "+table+" s JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=s.batch_checkpoint AND a.policy_hash=s.campaign_id AND a.source_hash=? WHERE s.campaign_id=?",new String[]{campaign,id})){assertTrue(c.moveToFirst());assertEquals(128,c.getInt(0));}assertTrue(TrainingStepStoreR26.verify(f.db));
            ExpertExperimentR26.Pair reference=new ExpertExperimentR26.Pair(ExpertExperimentR26.spec("E03"),LiveStateR26.hash(source.encode()),source.through);for(int i=0;i<128;i++)reference.advance(source.draws[i],false);try(Cursor c=f.db.rawQuery("SELECT state FROM r26_expert_experiment",null)){assertTrue(c.moveToFirst());assertArrayEquals(reference.encode(),CheckpointArtifactsR26.resolve(f.store,c.getBlob(0)));}assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_live_model"));
        }
    }
    @Test public void retrievalSeedDoesNotFabricateTrainingAndNextTerminalUpdateCommits()throws Exception{measuredTrainingReceipts("F11",false);}
    @Test public void symbolicF13TrainingCommitsWithCandidateAndTerminalLiveCheckpoint()throws Exception{measuredTrainingReceipts("F13",false);}
    @Test public void symbolicF14TrainingCommitsWithCandidateAndTerminalLiveCheckpoint()throws Exception{measuredTrainingReceipts("F14",false);}
    private static void measuredTrainingReceipts(String family,boolean tensor)throws Exception{
        int seedOnly=family.equals("F11")?1:0,updates=128-seedOnly;
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 full;
            if(tensor){List<StructuralProposalR26.Proposal> proposals=StructuralProposalR26.mechanismChildren(f.genome(family,source));full=proposals.get(0).genome;ModelGenomeSU2 control=proposals.get(1).genome;assertTrue(StructuralProposalR26.control(control));f.candidate(campaign,control,f.trained(control,source,128),128,"DONE",0);assertEquals(0,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));}else full=f.genome(family,source);
            f.candidate(campaign,full,null,0,"READY",0);int slices=0;while(scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+full.key()+"'")<128&&slices++<130)assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);assertTrue(slices<=130);
            try(Cursor c=f.db.rawQuery("SELECT state,state_hash FROM r26_candidate WHERE policy_hash=?",new String[]{full.key()})){assertTrue(c.moveToFirst());LiveStateR26 restored=LiveStateR26.decode(full.text(),CheckpointArtifactsR26.resolve(f.store,c.getBlob(0)),c.getString(1));OnlineModelsSU2.Model kernel=restored.hub.models.values().iterator().next();assertTrue(tensor?kernel instanceof FamilyF19TensorR26:family.equals("F17")?kernel instanceof PermutationModelR26:family.equals("F02")?kernel instanceof FamilyF02KernelR26:family.equals("F18")?kernel instanceof FamilyF18KernelR26:family.equals("F01")?kernel instanceof FamilyF01KernelR26:family.equals("F09")?kernel instanceof FamilyF09KernelR26:family.equals("F11")?kernel instanceof FamilyF11KernelR26:family.equals("F13")||family.equals("F14")?kernel instanceof SymbolicFamilyR26:kernel instanceof NeuralFamilyR26);assertEquals(updates,((TrainingDiagnosticsR26)kernel).completedTrainingUpdates());assertNull(((TrainingDiagnosticsR26)kernel).lastTrainingStep());}
            assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertTrue(TrainingStepStoreR26.verify(f.db));assertEquals(1,scalar(f.db,"SELECT min(kernel_update) FROM r26_training_step"));assertEquals(updates,scalar(f.db,"SELECT max(kernel_update) FROM r26_training_step"));assertTrue(scalar(f.db,"SELECT count(*) FROM r26_training_step WHERE gradient_norm>0 AND parameter_delta_norm>0")>0);
            try(Cursor c=f.db.rawQuery("SELECT source_hash,source_kind,min(sample_ordinal),max(sample_ordinal),sum(gap_reset) FROM r26_training_step",null)){assertTrue(c.moveToFirst());assertEquals(LiveStateR26.hash(source.encode()),c.getString(0));assertEquals("FROZEN_FOUNDRY_SNAPSHOT",c.getString(1));assertEquals(f.first+seedOnly,c.getLong(2));assertEquals(f.first+127,c.getLong(3));assertEquals(0,c.getLong(4));}
            assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_training_step s JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=s.batch_checkpoint AND a.policy_hash=s.policy_hash AND a.source_hash=s.campaign_id"));
            for(String sql:new String[]{"UPDATE r26_training_step SET loss_after=0","INSERT OR REPLACE INTO r26_training_step SELECT * FROM r26_training_step LIMIT 1"})try{f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_IMMUTABLE_TENSOR_STEP",w->{w.execSQL(sql);return 0;});fail("Training witness was mutable");}catch(RuntimeException expected){}
            assertTrue(TrainingStepStoreR26.verify(f.db));assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
            Models.Draw future=f.draw(128,System.currentTimeMillis()-2000);assertTrue(f.store.saveDraw(future)>=0);assertEquals(0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(updates,scalar(f.db,"SELECT count(*) FROM r26_training_step"));
            SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));assertEquals(updates+1,scalar(f.db,"SELECT count(*) FROM r26_training_step"));assertTrue(TrainingStepStoreR26.verify(f.db));
            try(Cursor c=f.db.rawQuery("SELECT s.source_hash,s.source_kind,s.kernel_update,s.batch_checkpoint,l.checkpoint_hash FROM r26_training_step s JOIN r26_live_model l ON l.policy_hash=s.policy_hash WHERE s.sample_ordinal=?",new String[]{Long.toString(f.first+128)})){assertTrue(c.moveToFirst());assertEquals(DrawHashR36.sha256V2(future),c.getString(0));assertEquals("CANONICAL_TERMINAL_DRAW",c.getString(1));assertEquals(updates+1,c.getLong(2));assertEquals(c.getString(3),c.getString(4));}
            org.json.JSONObject report=new org.json.JSONObject(RuntimeReportR26.json(f.db));org.json.JSONArray sections=report.getJSONArray("sections");boolean observed=false;for(int i=0;i<sections.length();i++)if(sections.getJSONObject(i).getString("sectionId").equals("TRAINING")){org.json.JSONObject metrics=sections.getJSONObject(i).getJSONObject("metricResults");assertEquals("OBSERVED",metrics.getJSONObject("weightDelta").getString("status"));assertTrue(metrics.getJSONObject("weightDelta").getString("definition").contains("same frozen pre-label input"));observed=true;}assertTrue(observed);
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
        }
    }
    private static ModelGenomeSU2 expiredLearnedParent(Fixture f)throws Exception{
        FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 parent=f.genome("F20",source);f.candidate(campaign,parent,null,0,"READY",0);
        assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);assertTrue(scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+parent.key()+"'")>0);
        laterWindow(f,6000);assertEquals(0,scalar(f.db,"SELECT length(snapshot) FROM r26_foundry WHERE campaign_id='"+campaign+"'"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='WAITING_WARM_FORK'"));return parent;
    }
    @Test public void expiredPrivateWeightsForkFromVerifiedKernelsAndOnlyLaterLabels()throws Exception{
        try(Fixture f=new Fixture(128)){
            ModelGenomeSU2 parent=expiredLearnedParent(f);byte[] oldReference;String oldHash,kernelHash;long exposed=f.first+127;
            try(Cursor c=f.db.rawQuery("SELECT state,state_hash FROM r26_candidate WHERE policy_hash=?",new String[]{parent.key()})){assertTrue(c.moveToFirst());oldReference=c.getBlob(0);oldHash=c.getString(1);LiveStateR26 old=LiveStateR26.decode(parent.text(),CheckpointArtifactsR26.resolve(f.store,oldReference),oldHash);old.onGap();kernelHash=((FamilyF20KernelR26)old.hub.models.values().iterator().next()).learnedParameterHash();}
            RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim("publication-test","SYNTHETIC",2,0);try{WarmCandidateR26.step(f.store,()->stale,f.budget());fail("Stale owner transferred learned state");}catch(IllegalStateException expected){}
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_warm_initialization"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='WAITING_WARM_FORK'"));
            assertEquals(1,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));assertEquals(0,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE policy_hash='"+parent.key()+"'"));
            String child;byte[] initializerReference;
            try(Cursor c=f.db.rawQuery("SELECT c.policy_hash,c.canonical_text,c.state,c.state_hash,c.cursor,i.state,i.parent_trained_through,i.parent_exposed_through,f.snapshot FROM r26_candidate c JOIN r26_warm_initialization i ON i.policy_hash=c.policy_hash JOIN r26_foundry f ON f.campaign_id=c.campaign_id",null)){
                assertTrue(c.moveToFirst());child=c.getString(0);assertNotEquals(parent.key(),child);assertEquals(0,c.getInt(4));assertEquals(exposed,c.getLong(7));assertTrue(c.getLong(6)<=exposed);initializerReference=c.getBlob(5);
                ModelGenomeSU2 program=ModelGenomeSU2.parse(c.getString(1));LiveStateR26 seeded=LiveStateR26.decode(program.text(),CheckpointArtifactsR26.resolve(f.store,c.getBlob(2)),c.getString(3));assertEquals(0,seeded.samples());assertEquals(kernelHash,((FamilyF20KernelR26)seeded.hub.models.values().iterator().next()).learnedParameterHash());
                byte[] initial=CheckpointArtifactsR26.resolve(f.store,initializerReference);assertEquals(program.members.get(0).params.get("_warm_kernel"),LiveStateR26.hash(initial));assertTrue(FoundryCampaignR26.Snapshot.decode(c.getBlob(8)).ordinal[0]>exposed);assertTrue(CheckpointReferencesR26.hashes(f.db).contains(CheckpointArtifactsR26.referenceHash(initializerReference)));
            }
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
            try{f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_IMMUTABLE_INIT",w->{w.execSQL("UPDATE r26_warm_initialization SET initializer_hash='changed'");return 0;});fail("Initializer provenance was mutable");}catch(RuntimeException expected){}
            int slices=0;while(scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+child+"'")<128&&slices++<130)assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
            assertTrue(slices<=130);assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));assertEquals(f.first+6128,scalar(f.db,"SELECT from_sequence FROM r26_cohort WHERE genome_key='"+child+"'"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
            CheckpointArtifactsR26 files=CheckpointArtifactsR26.forStore(f.store);files.prune(()->CheckpointReferencesR26.hashes(f.db),System.currentTimeMillis()+120000,0,256);assertNotNull(CheckpointArtifactsR26.resolve(f.store,initializerReference));try{CheckpointArtifactsR26.resolve(f.store,oldReference);fail("Expired parent raw context remained pinned");}catch(java.io.IOException expected){}
            assertNotNull(PolicyIdentityLedgerR26.canonical(f.db,parent.key()));
        }
    }
    @Test public void initializedButUnservedChildSurvivesAnotherRawWindowExpiry()throws Exception{
        try(Fixture f=new Fixture(128)){
            expiredLearnedParent(f);assertEquals(1,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));String firstInitializer;
            try(Cursor c=f.db.rawQuery("SELECT initializer_hash FROM r26_warm_initialization",null)){assertTrue(c.moveToFirst());firstInitializer=c.getString(0);}
            laterWindow(f,12000);assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE cursor=0 AND status='WAITING_WARM_FORK' AND length(state)>0"));
            assertEquals(1,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));
            try(Cursor c=f.db.rawQuery("SELECT i.initializer_hash,i.parent_exposed_through,f.source_first FROM r26_warm_initialization i JOIN r26_candidate c ON c.policy_hash=i.policy_hash JOIN r26_foundry f ON f.campaign_id=c.campaign_id",null)){assertTrue(c.moveToFirst());assertEquals(firstInitializer,c.getString(0));assertEquals(f.first+6127,c.getLong(1));assertTrue(c.getLong(2)>c.getLong(1));}
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_INIT_GC",w->{WarmCandidateR26.pruneInitializers(w);return 0;});assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_warm_initialization"));
            assertEquals(2,CheckpointReferencesR26.hashes(f.db).size()); // Seed state plus its complete kernel initializer.
        }
    }
    @Test public void badWarmParentIsPreservedAndDoesNotBlockAValidParent()throws Exception{
        try(Fixture f=new Fixture(128)){
            ModelGenomeSU2 good=expiredLearnedParent(f);String campaign;try(Cursor c=f.db.rawQuery("SELECT campaign_id FROM r26_candidate",null)){assertTrue(c.moveToFirst());campaign=c.getString(0);}ModelGenomeSU2 bad=FamilyRegistryR26.genome("F18").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_BAD_PARENT_SOURCE"),f.first+127);
            f.candidate(campaign,bad,new byte[]{1,2,3},1,"WAITING_WARM_FORK",0);String malformed=String.join("",Collections.nCopies(64,"0"));f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_BAD_PARENT_KEY",w->{w.execSQL("UPDATE r26_candidate SET policy_hash=? WHERE policy_hash=?",new Object[]{malformed,bad.key()});return 0;});
            assertEquals(0,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE policy_hash='"+malformed+"' AND status='BLOCKED_WARM_CHECKPOINT_PRESERVED' AND length(state)=3"));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE policy_hash='"+good.key()+"' AND status='WAITING_WARM_FORK'"));assertEquals(1,WarmCandidateR26.step(f.store,()->f.claim,f.budget()));
        }
    }
    @Test public void missingWarmCheckpointCannotSilentlyBecomeColdTraining()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);LiveStateR26 old=new LiveStateR26(FamilyRegistryR26.genome("F20").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_EARLIER_SOURCE"),f.first-10));for(int i=0;i<16;i++)old.learn(new int[]{1,2,3,4,5,6,7,8,9,10});byte[] bytes=old.encode();WarmModelSeedR26.Seed seed=WarmModelSeedR26.prepare(old.genome.text(),bytes,LiveStateR26.hash(bytes),PolicyIdentityR26.sha256("SYNTHETIC_EARLIER_CP"),f.first-20,LiveStateR26.hash(source.encode()),source.ordinal[0],source.through);
            f.candidate(campaign,seed.genome,null,0,"READY",0);ModelGenomeSU2 peer=f.genome("F17",source);f.candidate(campaign,peer,null,0,"READY",1000000);
            assertEquals(0,FoundryCampaignR26.step(f.store,()->f.claim,f.budget()));assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='BLOCKED_CHECKPOINT_OR_PROGRAM' AND cursor=0 AND state IS NULL"));assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_cohort"));assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
        }
    }

    @Test public void campaignQueueCountsOnlyUnfinishedMemberCpu()throws Exception{
        try(Fixture f=new Fixture(129)){
            FoundryCampaignR26.Snapshot a=f.prefix(128),b=f.prefix(129);String first=f.campaign(a),second=f.campaign(b);ModelGenomeSU2 ready=f.genome("F17",a),done=f.genome("F18",a),peer=f.genome("F17",b);
            f.candidate(first,done,f.trained(done,a,128),128,"DONE",1000000000);f.candidate(first,ready,null,0,"READY",0);f.candidate(second,peer,null,0,"READY",1000000);
            assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);assertTrue(scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+ready.key()+"'")>0);assertEquals(0,scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+peer.key()+"'"));
        }
    }

    @Test public void yieldDuringSerializationCommitsOnlyTheLastCompleteSampleAndMetrics()throws Exception{
        ModelReplaySU2.Adapter prior=ModelReplaySU2.ADAPTERS.get("R26F18");java.util.concurrent.atomic.AtomicBoolean yielded=new java.util.concurrent.atomic.AtomicBoolean();
        ModelReplaySU2.ADAPTERS.put("R26F18",parameters->new OnlineModelsSU2.Model(){
            final FamilyF18KernelR26 delegate=new FamilyF18KernelR26();int updates;
            public int stateWidth(){return delegate.stateWidth();}
            public void predict(float[][] p,float[] state){delegate.predict(p,state);}
            public void update(int[] draw){delegate.update(draw);updates++;}
            public void write(java.io.DataOutput out)throws java.io.IOException{delegate.write(out);if(updates==42&&yielded.compareAndSet(false,true))throw new BudgetTokenSU1.Yield("SYNTHETIC_SERIALIZATION_PREEMPTION");out.writeInt(updates);}
            public void read(java.io.DataInput in)throws java.io.IOException{delegate.read(in);updates=in.readInt();}
        });
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 genome=f.genome("F18",source);byte[] before=f.trained(genome,source,40);f.candidate(campaign,genome,before,40,"READY",0);
            LiveStateR26 expected=LiveStateR26.decode(genome.text(),before,LiveStateR26.hash(before));ModelLiveSU2.Decision decision=expected.forecast();float[][] table=expected.table();double loss=0;for(int p=0;p<10;p++)loss-=Math.log(Math.max(1e-12,table[p][source.draws[40][p]-1]))/10;expected.learn(source.draws[40]);byte[] complete=expected.encode();
            assertEquals(1,FoundryCampaignR26.step(f.store,()->f.claim,f.budget()));assertTrue(yielded.get());
            try(Cursor c=f.db.rawQuery("SELECT cursor,state,state_hash,loss_sum,dev_n,dev_k,checkpoint_hash FROM r26_candidate WHERE policy_hash=?",new String[]{genome.key()})){
                assertTrue(c.moveToFirst());assertEquals(41,c.getInt(0));byte[] persisted=CheckpointArtifactsR26.resolve(f.store,c.getBlob(1));assertArrayEquals(complete,persisted);assertEquals(41,LiveStateR26.decode(genome.text(),persisted,c.getString(2)).samples());assertEquals(loss,c.getDouble(3),1e-12);assertEquals(decision.act?1:0,c.getInt(4));assertEquals(decision.act&&(decision.mask&(1<<(source.draws[40][decision.position-1]-1)))!=0?1:0,c.getInt(5));
                try(Cursor manifest=f.db.rawQuery("SELECT trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{c.getString(6)})){assertTrue(manifest.moveToFirst());assertEquals(source.ordinal[40],manifest.getLong(0));}
            }
            assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);
            try(Cursor c=f.db.rawQuery("SELECT cursor,state,state_hash FROM r26_candidate WHERE policy_hash=?",new String[]{genome.key()})){assertTrue(c.moveToFirst());assertTrue(c.getInt(0)>41);for(int i=41;i<c.getInt(0);i++)expected.learn(source.draws[i]);assertArrayEquals(expected.encode(),CheckpointArtifactsR26.resolve(f.store,c.getBlob(1)));}
        }finally{if(prior==null)ModelReplaySU2.ADAPTERS.remove("R26F18");else ModelReplaySU2.ADAPTERS.put("R26F18",prior);}
    }

    @Test public void completedFirstServiceDoesNotStarveAblationFromCpuQueue()throws Exception{
        try(Fixture f=new Fixture(128)){
            FoundryCampaignR26.Snapshot source=f.prefix(128);String campaign=f.campaign(source);ModelGenomeSU2 parent=f.genome("F08",source),control=StructuralProposalR26.mechanismChildren(parent).get(1).genome;
            f.candidate(campaign,parent,f.trained(parent,source,16),16,"READY",100000);f.candidate(campaign,control,null,0,"READY",0);
            f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_SERVED_FAMILY_CONTROL",w->{w.execSQL("INSERT INTO r26_capability(kind,mechanism,implementation,state,last_progress,reason) VALUES('FAMILY','F08','FamilyF08KernelR26','TRAINED',123,'SYNTHETIC')");w.execSQL("UPDATE r26_candidate SET operation='REMOVE_HIDDEN_TRANSITION_MEMORY' WHERE policy_hash=?",new Object[]{control.key()});return 0;});
            assertTrue(FoundryCampaignR26.step(f.store,()->f.claim,f.budget())>0);assertTrue("Ablation starved behind an already-served expensive parent",scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+control.key()+"'")>0);
            assertEquals(16,scalar(f.db,"SELECT cursor FROM r26_candidate WHERE policy_hash='"+parent.key()+"'"));assertEquals("Control work must not receive family capability credit",123,scalar(f.db,"SELECT last_progress FROM r26_capability WHERE mechanism='F08'"));
        }
    }
}
