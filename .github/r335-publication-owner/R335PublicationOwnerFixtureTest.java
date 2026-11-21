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
public final class R335PublicationOwnerFixtureTest {
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static final class Fixture implements AutoCloseable {
        final R26RuntimeDeviceTest.Sandbox app;
        DbStore store;
        SQLiteDatabase db;
        final boolean keepFiles;
        final long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001");
        final RuntimeCoordinatorR26.Claim claim;
        Fixture(int count)throws Exception{this(count,null,true,false);}
        Fixture(int count,String name,boolean initialize,boolean keep)throws Exception{
            assertNull("Each fixture must start in a fresh instrumentation process",DatabaseCoordinator.peek());keepFiles=keep;
            app=name==null?new R26RuntimeDeviceTest.Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext()):new R26RuntimeDeviceTest.Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext(),name);store=new DbStore(app);db=store.readable();
            if (!initialize) throw new IllegalStateException("Fresh publication fixture required");
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION, "R335_TEST_PUBLICATION_SCHEMA"),
                    writer -> {
                        RuntimeSchemaR26.ensure(writer);
                        SchemaR27.ensure(writer);
                        R31FixtureSchemaOwners.installSu1WithinWriterR31(writer);
                        StrategySchemaSU2.ensure(writer);
                        writer.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',?,0)",
                                new Object[]{System.currentTimeMillis()});
                        writer.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('publication-test','P2','test',0,0,NULL,'READY')");
                        return 0;
                    });
            List<Models.Draw> history=new ArrayList<>();for(int i=0;i<count;i++)history.add(draw(i,System.currentTimeMillis()-(count-i+1L)*60000));assertEquals(count,store.saveDraws(history));
            claim = store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL, "R335_TEST_PUBLICATION_CLAIM"),
                    writer -> RuntimeCoordinatorR26.claimWithinR31(writer, "publication-test", "SYNTHETIC", "R27", null));
            assertNotNull("Production owner did not acquire publication task", claim);
            RuntimeCoordinatorR26.requireFence(db, claim);
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
}
