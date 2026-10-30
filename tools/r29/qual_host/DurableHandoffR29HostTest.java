package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real Root/RP rows and sealed files; forced phase failure is exception injection, not OS death. */
public final class DurableHandoffR29HostTest {
    static int checks;
    static final long EPOCH=1793400000001L;
    static void check(String id,boolean yes){System.out.println("R29_DURABLE_HANDOFF|"+id+"|"+(yes?"OK":"FAILED"));if(!yes)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):null;}}
    static String sha(File f)throws Exception{return ResearchCutoverR29.fileHash(f,Long.MAX_VALUE,()->false);}
    static String rootResearchHash(SQLiteDatabase root){List<MigrationCopyR29.Table> tables=new ArrayList<>();for(MigrationCopyR29.Table table:ResearchCutoverR29.tables(root))if(MigrationCopyR29.ownedResearch(table.name))tables.add(table);return ResearchCutoverR29.root(root,tables,Long.MAX_VALUE,()->false);}
    static final class Fixture implements AutoCloseable{
        final EventYieldR29HostTest.Fixture base=new EventYieldR29HostTest.Fixture();
        final File candidate=base.store.contextForR26().getDatabasePath("starcore-r29-research.candidate.db"),published=base.store.contextForR26().getDatabasePath(ResearchRouteR29.FILE_NAME),baseline=new File(candidate.getParentFile(),"starcore-r29-baseline-"+EPOCH+".db");
        final ResearchPoolR27.Worker worker;final String baselineSha,sourceHash,schemaHash;
        Fixture(String phase)throws Exception{
            base.root.beginTransaction();try{RuntimeSchemaR26.ensure(base.root);base.root.execSQL("CREATE TABLE r26_handoff_model(v INTEGER NOT NULL)");base.root.execSQL("INSERT INTO r26_handoff_model VALUES(1)");base.root.execSQL(ResearchCutoverR29.PUBLICATION);base.root.execSQL(ResearchRouteR29.ROUTE_DDL);base.root.execSQL(ResearchCutoverR29.FENCE);base.root.setTransactionSuccessful();}finally{base.root.endTransaction();}
            List<MigrationCopyR29.Table> ts=ResearchCutoverR29.tables(base.root);sourceHash=ResearchCutoverR29.root(base.root,ts,Long.MAX_VALUE,()->false);schemaHash=ResearchCutoverR29.schemaHash(ts);
            try(SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(candidate,null)){
                target.execSQL("PRAGMA journal_mode=WAL");target.beginTransaction();try{
                    target.execSQL(ResearchRouteR29.OWNER_DDL);target.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,?,1)",new Object[]{schemaHash,sourceHash,EPOCH});
                    for(MigrationCopyR29.Table t:ts){target.execSQL(t.createSql);Long after=null;for(;;){List<MigrationCopyR29.Row> rows=ResearchCutoverR29.page(base.root,t,after);if(rows.isEmpty())break;ResearchCutoverR29.restorePage(target,t,rows);after=rows.get(rows.size()-1).rowid;}}
                    target.setTransactionSuccessful();
                }finally{target.endTransaction();}
                try(Cursor c=target.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new AssertionError("CANDIDATE_CHECKPOINT_REQUIRED");}
            }
            ResearchCutoverR29.sync(candidate);ResearchCutoverR29.copyBaseline(candidate,baseline,Long.MAX_VALUE,()->false);baselineSha=sha(baseline);
            base.root.execSQL("INSERT INTO r29_migration_publication VALUES(1,?,?,?, ?,?,?, '',1)",new Object[]{EPOCH,candidate.getName(),phase,phase.equals("PREPARED")?"":baseline.getName(),phase.equals("PREPARED")?"":baselineSha,phase.equals("PREPARED")?"":sourceHash});
            base.root.execSQL("INSERT INTO r29_migration_owner_fence VALUES(1,?,?,?,1)",new Object[]{EPOCH,"R29_ROOT_RESEARCH_FROZEN_V1",rootResearchHash(base.root)});
            if(phase.equals("RENAMED")&&!candidate.renameTo(published))throw new AssertionError("RENAME_REQUIRED");
            EventYieldR29HostTest.set(base.runtime.pool,"writes",new ResearchWriteQueueR27(base.store));worker=base.runtime.pool.new Worker(base.root,base.root,"durable-root-worker");
        }
        void append64(){base.root.beginTransaction();try{for(int i=2;i<=64;i++){String issue=String.format(java.util.Locale.ROOT,"20261030%04d",i);base.root.execSQL("INSERT INTO draws VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{issue});ResearchEventBridgeR29.appendCanonical(base.root,issue,true,100+i);}base.root.setTransactionSuccessful();}finally{base.root.endTransaction();}}
        void write(MigrationCopyR29.Body body)throws Exception{base.root.beginTransaction();try{body.run(base.root);base.root.setTransactionSuccessful();}finally{base.root.endTransaction();}}
        ResearchCutoverR29.Publication resume()throws Exception{return ResearchCutoverR29.resumePublication(base.root,candidate,published,this::write,Long.MAX_VALUE,()->false);}
        void failFirstPhase()throws Exception{AtomicBoolean fired=new AtomicBoolean();boolean failed=false;try{ResearchCutoverR29.resumePublication(base.root,candidate,published,body->{write(db->{body.run(db);if(fired.compareAndSet(false,true))throw new IllegalStateException("CP3_DB_BUSY_INJECTED_ROOT_PHASE");});},Long.MAX_VALUE,()->false);}catch(IllegalStateException expected){failed=expected.getMessage().contains("CP3_DB_BUSY_INJECTED");}if(!failed||!fired.get())throw new AssertionError("ACTUAL_PHASE_ROLLBACK_REQUIRED");}
        void verifyUnchanged()throws Exception{if(!sha(baseline).equals(baselineSha))throw new AssertionError("SEALED_BASELINE_MUTATED");}
        public void close()throws Exception{base.close();}
    }
    static void oldGap()throws Exception{
        try(Fixture f=new Fixture("PREPARED")){
            f.failFirstPhase();f.append64();
            ResearchRouteR29.write(f.base.store,"OLD_ROOT_MODEL_PROGRESS",db->{db.execSQL("UPDATE r26_handoff_model SET v=2");return 1;});
            f.base.event.step(f.worker);f.base.event.step(f.worker);
            ResearchCutoverR29.Publication p=f.resume();ResearchRouteR29.publish(f.base.store,f.published,p.schemaHash,p.sourceRoot,p.epoch);
            try(SQLiteDatabase rp=SQLiteDatabase.openDatabase(f.published.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                long ack=n(f.base.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0"),inbox=n(rp,"SELECT count(*) FROM r29_event_inbox");
                System.out.println("R29_DURABLE_HANDOFF|OLD_REAL_GAP|rootACK="+ack+"|rpInbox="+inbox+"|rootModel="+n(f.base.root,"SELECT v FROM r26_handoff_model")+"|rpModel="+n(rp,"SELECT v FROM r26_handoff_model"));
                check("H0_SEALED_RETRY_MUST_NOT_LOSE_ROOT_MODEL_OR_TWO_EVENT_PAGES",ack==inbox&&n(rp,"SELECT v FROM r26_handoff_model")==n(f.base.root,"SELECT v FROM r26_handoff_model"));
            }
        }
    }
    static void continuityLifecycle(boolean oldReasonControl)throws Exception{
        if(!oldReasonControl)try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            f.root.beginTransaction();try{f.root.execSQL("CREATE TABLE r26_old_pulse_probe(v INTEGER NOT NULL)");f.root.execSQL("INSERT INTO r26_old_pulse_probe VALUES(1)");LegacyCapabilityAdapterR29.initialize(f.root,"lifecycle-owner");f.root.setTransactionSuccessful();}finally{f.root.endTransaction();}
            check("L1_UNPUBLISHED_OLD_OWNER_CAN_ENTER",LegacyCapabilityAdapterR29.enterContinuity(f.store,false)==null);
            java.util.concurrent.ExecutorService other=java.util.concurrent.Executors.newSingleThreadExecutor();
            try{
                f.coordinator.runAtomicWriteOrThrowF01("SYNTHETIC_OLD_PULSE_PROGRESS",db->{db.execSQL("UPDATE r26_old_pulse_probe SET v=2");return 0;});
                check("L2_REAL_OLD_PULSE_PROGRESS_COMMITS_BEFORE_DRAIN",n(f.root,"SELECT v FROM r26_old_pulse_probe")==2);
                java.util.concurrent.Future<String> blocked=other.submit(()->ResearchCutoverR29.run(f.store,f.worker));
                check("L3_COPY_PUBLICATION_WAITS_FOR_PREEXISTING_OLD_PULSE",blocked.get(2,java.util.concurrent.TimeUnit.SECONDS).equals("WAITING_LEGACY_MAINTENANCE_DRAIN")&&!ResearchRouteR29.active(f.root));
                java.util.concurrent.Future<Boolean> dispatcher=other.submit(()->{try{LegacyCapabilityAdapterR29.publish(f.store,"lifecycle-owner");return false;}catch(IllegalStateException expected){return expected.getMessage().contains("CONTINUITY_DRAIN");}});
                check("L4_DISPATCHER_PUBLICATION_ALSO_WAITS_FOR_REAL_OLD_PULSE",dispatcher.get(2,java.util.concurrent.TimeUnit.SECONDS));
            }finally{LegacyCapabilityAdapterR29.leaveContinuity();other.shutdownNow();other.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS);}
            try(LegacyCapabilityAdapterR29.CutoverLease lease=LegacyCapabilityAdapterR29.drainForCutover()){check("L4B_DRAIN_BECOMES_AVAILABLE_ONLY_AFTER_OLD_PULSE_EXITS",lease!=null);}
        }
        try(Fixture f=new Fixture("PREPARED")){
            f.base.root.beginTransaction();try{LegacyCapabilityAdapterR29.initialize(f.base.root,"lifecycle-owner");f.base.root.setTransactionSuccessful();}finally{f.base.root.endTransaction();}
            ResearchCutoverR29.resumeBeforeRuntime(f.base.store);
            check("L5_LIFECYCLE_FIXTURE_HAS_REAL_PUBLISHED_DATABASE_AND_LEGACY_DISPATCHER",ResearchRouteR29.active(f.base.root)&&!ResearchRouteR29.pendingHandoff(f.base.root)&&!LegacyCapabilityAdapterR29.hasPoolOwner(f.base.store));
            String reason=LegacyCapabilityAdapterR29.enterContinuity(f.base.store,false);
            System.out.println("R29_DURABLE_HANDOFF|PUBLISHED_LEGACY_ENTRY_REASON|"+reason);
            check("L6_PUBLISHED_LEGACY_OWNER_WAIT_REASON_NAMES_POOL_HANDOFF","WAITING_VERIFIED_RESEARCH_POOL_HANDOFF".equals(reason));
            LegacyCapabilityAdapterR29.publish(f.base.store,"lifecycle-owner");
            check("L7_PUBLISHED_POOL_OWNER_REASON_IS_ACCURATE_AND_OLD_ENTRY_STAYS_CLOSED",LegacyCapabilityAdapterR29.enterContinuity(f.base.store,false).equals("R29_LEGACY_CONTINUITY_OWNED_BY_RESEARCH_POOL"));
            java.util.concurrent.ExecutorService other=java.util.concurrent.Executors.newSingleThreadExecutor();
            try{check("L8_ONLY_POOL_ENTRY_CAN_ACQUIRE_ON_ANOTHER_THREAD",other.submit(()->{String blocked=LegacyCapabilityAdapterR29.enterContinuity(f.base.store,true);if(blocked!=null)return false;try{return LegacyCapabilityAdapterR29.onPoolContinuityThread();}finally{LegacyCapabilityAdapterR29.leaveContinuity();}}).get(2,java.util.concurrent.TimeUnit.SECONDS));}
            finally{other.shutdownNow();other.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS);}
            check("L9_PUBLISHED_OWNER_CHECKS_CANNOT_MUTATE_ORIGINAL_ROOT_MODELS",n(f.base.root,"SELECT v FROM r26_handoff_model")==1);f.verifyUnchanged();
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("--old-gap")){oldGap();return;}
        if(args.length>0&&args[0].equals("--old-b8-reason")){continuityLifecycle(true);return;}
        continuityLifecycle(false);
        try(Fixture f=new Fixture("PREPARED")){
            f.failFirstPhase();f.append64();boolean blocked=false;try{ResearchRouteR29.write(f.base.store,"ROOT_MODEL_DURING_RETRY",db->{db.execSQL("UPDATE r26_handoff_model SET v=2");return 1;});}catch(IllegalStateException e){blocked=ResearchWriteQueueR27.writerBusy(e);}
            check("H1_DURABLE_PENDING_FENCE_REJECTS_ROOT_MODEL_ADVANCE",blocked&&n(f.base.root,"SELECT v FROM r26_handoff_model")==1);
            ResearchPoolR27.Outcome a=f.base.event.step(f.worker),b=f.base.event.step(f.worker);
            check("H2_TWO_EVENT_PAGES_WAIT_WITHOUT_ACK_OR_FAKE_PROGRESS",a.units==0&&b.units==0&&a.reason.contains("DURABLE_RESEARCH_HANDOFF")&&n(f.base.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==0&&n(f.base.root,"SELECT count(*) FROM r29_event_inbox")==0);
            check("H3_REALTIME_DRAWS_CONTINUE_DURING_RESEARCH_FENCE",n(f.base.root,"SELECT count(*) FROM draws")==64);
            check("H4_OLD_MAINTENANCE_OWNER_ALSO_WAITS",LegacyCapabilityAdapterR29.enterContinuity(f.base.store,false).contains("DURABLE_RESEARCH_OWNER_HANDOFF"));
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.base.store)){check("H5_STARTUP_RECOVERS_SEALED_PREPARED_BEFORE_RUNTIME_WRITES",ResearchCutoverR29.resumeBeforeRuntime(f.base.store).startsWith("ACTIVE_RECOVERED"));}
            f.worker.ro=ResearchRouteR29.reader(f.base.coordinator.realtimeReadOnlyR29());
            for(int i=0;i<8&&n(f.base.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")<64;i++)ResearchEventBridgeR29.deliver(f.worker);
            check("H6_POST_PUBLICATION_DELIVERS_ALL64_WITH_MATCHING_RP_INBOX",n(f.base.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==64&&n(f.worker.ro,"SELECT count(*) FROM r29_event_inbox")==64);
            check("H7_DELIVERY_STILL_IS_NOT_LEARNING",n(f.worker.ro,"SELECT count(*) FROM r29_learning_work")==128&&n(f.worker.ro,"SELECT count(*) FROM r29_learning_receipt")==0);
            ResearchEventBridgeR29.deliver(f.worker);check("H8_REPEATED_DELIVERY_IS_EXACTLY_ONCE",n(f.worker.ro,"SELECT count(*) FROM r29_event_inbox")==64&&n(f.worker.ro,"SELECT count(*) FROM r29_learning_work")==128);
            ResearchRouteR29.write(f.base.store,"RP_MODEL_AFTER_HANDOFF",db->{db.execSQL("UPDATE r26_handoff_model SET v=3");return 1;});
            check("H9_MODEL_AUTHORITY_MOVES_TO_RP_WITH_ROOT_ORIGINAL_RETAINED",n(f.worker.ro,"SELECT v FROM r26_handoff_model")==3&&n(f.base.root,"SELECT v FROM r26_handoff_model")==1);f.verifyUnchanged();check("H10_SEALED_BASELINE_NEVER_REWRITTEN",true);
        }
        for(String phase:new String[]{"VERIFIED","RENAMED"})try(Fixture f=new Fixture(phase)){
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.base.store)){check("H11_"+phase+"_STARTUP_RECOVERY_WITHOUT_POOL_OR_ROOT_WRITE_DEADLOCK",ResearchCutoverR29.resumeBeforeRuntime(f.base.store).startsWith("ACTIVE_RECOVERED"));}f.verifyUnchanged();check("H12_"+phase+"_ORIGINAL_BYTES_RETAINED",ResearchRouteR29.active(f.base.root));
        }
        try(Fixture f=new Fixture("PREPARED")){
            f.base.root.execSQL("UPDATE r26_handoff_model SET v=9");boolean rejected=false;try{ResearchCutoverR29.resumeBeforeRuntime(f.base.store);}catch(IllegalStateException e){rejected=e.getMessage().contains("ROOT_RESEARCH_ADVANCED");}
            check("H13_HISTORIC_ROOT_MODEL_PROGRESS_FAILS_CLOSED_INSTEAD_OF_OLD_RP_PUBLISH",rejected&&!ResearchRouteR29.active(f.base.root)&&n(f.base.root,"SELECT v FROM r26_handoff_model")==9);f.verifyUnchanged();check("H14_CONFLICT_PRESERVES_ROOT_AND_BOTH_SEALED_FILES",f.candidate.isFile()&&f.baseline.isFile());check("H14B_RECOVERY_REQUIRED_REASON_IS_DURABLE",s(f.base.root,"SELECT last_error FROM r29_migration_publication WHERE singleton=1").contains("RECOVERY_REQUIRED:ROOT_RESEARCH_ADVANCED"));
        }
        try(Fixture f=new Fixture("PREPARED")){
            if(!f.baseline.delete())throw new AssertionError("BASELINE_REMOVE_REQUIRED");String result=ResearchCutoverR29.resumeBeforeRuntime(f.base.store);
            check("H15_UNSEALED_RESERVATION_CANCEL_RETURNS_ROOT_NOT_FAKE_ACTIVE",result.startsWith("UNSEALED_RESERVATION_CANCELLED")&&!ResearchRouteR29.active(f.base.root)&&!ResearchRouteR29.pendingHandoff(f.base.root)&&n(f.base.root,"SELECT v FROM r26_handoff_model")==1);
            ResearchRouteR29.write(f.base.store,"ROOT_AFTER_UNSEALED_CANCEL",db->{db.execSQL("UPDATE r26_handoff_model SET v=2");return 1;});check("H16_ROOT_CAN_BOOTSTRAP_AGAIN_AFTER_SAFE_UNSEALED_CANCEL",n(f.base.root,"SELECT v FROM r26_handoff_model")==2);
        }
        try(Fixture f=new Fixture("PREPARED")){
            if(!f.baseline.delete())throw new AssertionError("BASELINE_REMOVE_REQUIRED");f.base.root.execSQL("UPDATE r29_migration_publication SET baseline_name='unexpected-pinned-proof' WHERE singleton=1");boolean rejected=false;
            try{ResearchCutoverR29.resumeBeforeRuntime(f.base.store);}catch(IllegalStateException e){rejected=e.getMessage().contains("PREPARED_PROOF_CONFLICT");}
            check("H17_AMBIGUOUS_PREPARED_PROOF_PRESERVES_CANDIDATE_AND_RESERVATION",rejected&&f.candidate.isFile()&&n(f.base.root,"SELECT count(*) FROM r29_migration_publication")==1);
        }
        System.out.println("R29_DURABLE_HANDOFF|ALL_PASS|checks="+checks+"|REAL_SQLITE_PUBLICATION_RETRY_AND_RESTART_NOT_OS_KILL");
    }
}
