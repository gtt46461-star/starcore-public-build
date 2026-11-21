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
public final class R335DuplicateFeedbackTest {
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

    private static String singleText(SQLiteDatabase db, String sql, String... args) {
        try (Cursor row=db.rawQuery(sql,args)) {
            assertTrue("One row is required", row.moveToFirst());
            String value=row.getString(0);
            assertFalse("Readback must be unique", row.moveToNext());
            return value;
        }
    }

    @Test public void confirmedDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority() throws Exception {
        try (Fixture f = new Fixture(128)) {
            FoundryCampaignR26.Snapshot base = f.prefix(128);
            String campaign = f.campaign(base);
            ModelGenomeSU2 winner = f.genome("F17", base), peer = f.genome("F01",base);
            f.candidate(campaign,winner,f.trained(winner,base,128),128,"DONE",10);
            f.candidate(campaign,peer,null,0,"READY",0);
            assertEquals(1,LivePoolR26.publishOne(f.store,()->f.claim,f.budget()));
            assertEquals(f.first+128,scalar(f.db,"SELECT born_ordinal FROM r26_live_model"));
            assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_candidate WHERE status='READY' AND cursor=0"));
            assertEquals(0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));
            assertEquals(f.first+127,scalar(f.db,"SELECT through_ordinal FROM r26_live_model"));
            Models.Draw future = f.draw(128,System.currentTimeMillis()-2000);
            long ordinal=TargetIssueResolver.sequenceOrdinalSU1(future.issue);
            String event=PolicyIdentityR26.sha256("R26_SETTLED|"+future.issue);
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_feedback WHERE issue_ordinal="+ordinal));
            assertNull(SettlementTaskStoreR36.terminal(f.store,future.issue));
            assertTrue(f.store.saveDraw(future)>=0);
            assertEquals(DrawHashR36.sha256V2(future),singleText(f.db,"SELECT source_hash FROM r26_feedback WHERE event_id=? AND issue_ordinal=?",event,String.valueOf(ordinal)));
            assertEquals(event,singleText(f.db,"SELECT event_id FROM r26_feedback WHERE issue_ordinal=?",String.valueOf(ordinal)));
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_consumption WHERE event_id='"+event+"'"));
            assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));
            String checkpoint=singleText(f.db,"SELECT checkpoint_hash FROM r26_consumption WHERE event_id=? AND policy_hash=?",event,winner.key());
            assertEquals(checkpoint,singleText(f.db,"SELECT checkpoint_hash FROM r26_live_model WHERE policy_hash=?",winner.key()));
            assertEquals(f.first+128,scalar(f.db,"SELECT through_ordinal FROM r26_live_model"));
            // DbStore resolves a byte-identical existing canonical issue before insertion.
            // Its documented durable duplicate return is -1, distinct from a -2 conflict.
            assertEquals(-1L, f.store.saveDraw(future));
            assertEquals(1, scalar(f.db, "SELECT count(*) FROM draws WHERE issue='" + future.issue + "'"));
            assertEquals(0,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));
            assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_feedback WHERE issue_ordinal="+ordinal));
            assertEquals(checkpoint,singleText(f.db,"SELECT checkpoint_hash FROM r26_consumption WHERE event_id=? AND policy_hash=?",event,winner.key()));
            assertNull(CertificateSU2.read(f.db,winner.key(),System.currentTimeMillis()));
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM su2_certificate"));
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM trades WHERE action='BET' AND stat_scope='REAL_TRADE'"));
        }
    }

    @Test public void missingPredrawUsesTheSoleTerminalAndKeepsTheOriginalDrawLabel() throws Exception {
        try (Fixture f = new Fixture(128)) {
            Models.Draw future = f.draw(128,System.currentTimeMillis()-2000);
            long ordinal=TargetIssueResolver.sequenceOrdinalSU1(future.issue);
            String event=PolicyIdentityR26.sha256("R26_SETTLED|"+future.issue);
            assertTrue(f.store.saveDraw(future)>=0);
            assertNull(SettlementTaskStoreR36.terminal(f.store,future.issue));
            String source=singleText(f.db,"SELECT source_hash FROM r26_feedback WHERE event_id=?",event);
            assertEquals(DrawHashR36.sha256V2(future),source);
            SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
            SettlementTaskStoreR36.Terminal first=SettlementTaskStoreR36.terminal(f.store,future.issue);
            assertNotNull("Sole settlement owner returned without a durable terminal",first);
            assertEquals(SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL,first.status);
            SettlementCoordinatorR815.settleNow(f.store,future,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
            SettlementTaskStoreR36.Terminal second=SettlementTaskStoreR36.terminal(f.store,future.issue);
            assertNotNull(second);
            assertEquals(first.status,second.status);
            assertEquals(first.settlementHash,second.settlementHash);
            assertEquals(1,scalar(f.db,"SELECT count(*) FROM r26_feedback WHERE issue_ordinal="+ordinal));
            assertEquals(source,singleText(f.db,"SELECT source_hash FROM r26_feedback WHERE event_id=?",event));
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM r26_frozen WHERE target_ordinal="+ordinal));
            assertEquals(0,scalar(f.db,"SELECT count(*) FROM trades WHERE action='BET' AND stat_scope='REAL_TRADE'"));
        }
    }
}
