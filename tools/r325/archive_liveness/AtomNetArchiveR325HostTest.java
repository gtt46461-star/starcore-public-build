package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;

/** Actual production maintenance, learner restore and batch guards/native SQLite. */
public final class AtomNetArchiveR325HostTest {
    private static int assertions;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
    private interface Work{void run()throws Exception;}
    private static void throwsAs(Work work,Class<?> type,String label)throws Exception{try{work.run();throw new AssertionError(label);}catch(Exception expected){if(!type.isInstance(expected))throw expected;check(true,label);}}
    private static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    private static ResearchPoolR27.Outcome run(ArchiveLivenessFixtureR325 f,long now)throws Exception{return AtomNetSampleArchiveR324.maintenance(f.db,()->f.files,(op,b,key)->f.batch(op,b),now);}
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path parent=Paths.get(args[0]);
        try(ArchiveLivenessFixtureR325 f=new ArchiveLivenessFixtureR325(parent)){
            f.insert(ArchiveLivenessFixtureR325.BAD,9,1);f.insert(ArchiveLivenessFixtureR325.GOOD,10,2);AtomNetSessionR30 current=f.current();
            f.tx(()->{SchemaDomainCoordinatorR926.ensureInfrastructure(f.db);SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(f.db);});
            check(AtomNetPublicationR31.REVISION==4&&SchemaDomainCoordinatorR926.matches(f.db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()),"attempt_schema_registered_in_exact_revision4_domain");
            RuntimeR27 runtime=f.runtime();ResearchPoolR27.Outcome cold=AtomNetRuntimeR30.step(runtime,f.worker());
            check(cold.units==1&&runtime.atomSessionR30!=null,"actual_production_cold_restore_advances_despite_bad_old_segment");
            check(runtime.atomPublicationR30.model.weightsHash.equals(StatsR27.sha256(current.trainer.net.encode())),"cold_restore_preserves_exact_current_complete_model");
            check(f.scalar("SELECT count(*) FROM r325_atomnet_archive_attempt")==0,"learner_restore_does_not_run_archive_task");
            AtomNetSessionR30 saved=runtime.atomSessionR30;runtime.invalidateTaskDraftR31("r27-atomnet-archive");
            check(runtime.atomSessionR30==saved,"archive_cancellation_does_not_invalidate_learner");
            check(!ResearchTaskRegistryR30R1.require("r27-atomnet-archive").training&&ResearchTaskRegistryR30R1.ENTRIES.size()==57,"independent_nontraining_owner_preserves_original57_registry");
            ResearchPoolR27.Outcome bad=run(f,10000);
            check(bad.units==0&&bad.reason.contains("ARCHIVE_OBJECT_PRESERVED"),"known_bad_source_is_object_level_dependency");
            check(f.text("SELECT status FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(1)+"'").equals("FAULT_PRESERVED"),"bad_source_failure_persisted");
            check(f.text("SELECT failure_code FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(1)+"'").equals("R324_SAMPLE_ARCHIVE_SOURCE_INCOMPLETE"),"precise_failure_code_persisted");
            check(f.scalar("SELECT count(*) FROM r31_atomnet_sample_stage WHERE publication_id='"+id(1)+"'")==9,"damaged_original_rows_not_deleted_or_synthesized");
            check(run(f,10001).units==1,"next_valid_publication_archives_without_wait_for_bad_one");
            check(f.scalar("SELECT count(*) FROM r324_atomnet_sample_archive WHERE publication_id='"+id(2)+"'")==1,"valid_segment_committed");
            check(f.text("SELECT status FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(2)+"'").equals("ARCHIVED"),"success_and_retirement_receipt_atomic");
            f.reopen();check(run(f,10002).units==0,"reopen_retains_bad_object_backoff");
            check(f.scalar("SELECT attempts FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(1)+"'")==1,"not_retried_before_persisted_deadline");
            check(run(f,70000).units==0&&f.scalar("SELECT attempts FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(1)+"'")==2,"due_bad_object_retried_after_restart");
            check(f.scalar("SELECT next_attempt_at FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(1)+"'")==190000,"retry_delay_expands_without_busy_loop");
            f.tx(()->f.db.execSQL("INSERT INTO r31_atomnet_sample_stage VALUES(?,?,?,?,?,?,?,?)",f.row(id(1),9)));
            check(run(f,190000).units==1,"genuine_restored_original_can_close_object_failure");
            check(f.scalar("SELECT count(*) FROM r31_atomnet_sample_stage")==0,"both_completed_segments_retire_exact_rows");
            check(f.scalar("SELECT count(*) FROM r31_atomnet_publication")==3,"all_original_publication_identities_retained");
            Map<String,Object> health=AtomNetPublicationR31.storageHealthR32(f.db);check(health.get("archiveMaintenance") instanceof Map,"maintenance_observation_exposed_to_existing_report");
            check(run(f,190001).units==0,"completed_archive_repeat_has_no_new_write");
        }
        try(ArchiveLivenessFixtureR325 f=new ArchiveLivenessFixtureR325(parent)){
            f.insert(id(3),10,1);f.insert(id(4),10,2);
            throwsAs(()->AtomNetSampleArchiveR324.maintenance(f.db,()->{throw new InterruptedIOException("R325_CANCELLED");},(op,b,key)->f.batch(op,b),1000),InterruptedIOException.class,"cancellation_propagates_without_object_failure_relabel");
            check(f.text("SELECT status FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(3)+"'").equals("CLAIMED"),"cancelled_work_retains_durable_retry_claim");
            check(f.scalar("SELECT count(*) FROM r31_atomnet_sample_stage")==20,"cancellation_preserves_all_samples");
            check(run(f,1001).units==1,"unrelated_segment_advances_after_cancelled_attempt");
            f.reopen();check(run(f,1002).units==0,"interruption_claim_survives_reopen");
            check(run(f,61000).units==1,"cancelled_segment_retries_exact_source_after_deadline");
        }
        try(ArchiveLivenessFixtureR325 f=new ArchiveLivenessFixtureR325(parent)){
            f.insert(id(5),10,1);f.insert(id(6),10,2);
            throwsAs(()->AtomNetSampleArchiveR324.maintenance(f.db,()->{throw new IOException("UNKNOWN_IO_FAULT");},(op,b,key)->f.batch(op,b),2000),IOException.class,"unknown_io_is_not_swallowed");
            check(f.text("SELECT status FROM r325_atomnet_archive_attempt WHERE publication_id='"+id(5)+"'").equals("CLAIMED"),"unknown_failure_has_bounded_durable_retry");
            check(run(f,2001).units==1,"different_due_publication_not_starved_by_old_unknown_failure");
            check(!AtomNetSampleArchiveR324.localObjectFailure(new IOException("SQLITE_FULL")),"disk_full_not_downgraded_to_object_fault");
            check(!AtomNetSampleArchiveR324.localObjectFailure(new IllegalStateException("BUG")),"unexpected_runtime_failure_not_swallowed");
            check(!AtomNetSampleArchiveR324.localObjectFailure(new IOException("R324_SAMPLE_ARCHIVE_DIGEST",new IOException("SQLITE_FULL"))),"nested_storage_failure_not_downgraded");
            check(!AtomNetSampleArchiveR324.localObjectFailure(new IOException("R324_SAMPLE_ARCHIVE_DIGEST",new OutOfMemoryError("fixture"))),"nested_process_fatal_not_downgraded");
            check(AtomNetSampleArchiveR324.retryDelay(Long.MAX_VALUE)==1800000L,"retry_backoff_saturates_without_overflow");
            check(run(f,1000).units==1,"backwards_clock_allows_revalidated_bounded_retry");
        }
        try(ArchiveLivenessFixtureR325 f=new ArchiveLivenessFixtureR325(parent)){
            f.insert(id(7),10,1);
            throwsAs(()->AtomNetSampleArchiveR324.maintenance(f.db,()->f.files,(op,b,key)->{
                if(op.equals("ATOMNET_R324_ARCHIVE")){
                    f.db.beginTransaction();try{ResearchWriteQueueR27.execute(f.db,b,op);throw new IOException("SIMULATED_DEATH_BEFORE_COMMIT");}finally{f.db.endTransaction();}
                }else f.batch(op,b);
            },3000),IOException.class,"retirement_failure_not_acknowledged_as_success");
            check(f.scalar("SELECT count(*) FROM r324_atomnet_sample_archive")==0&&f.scalar("SELECT count(*) FROM r31_atomnet_sample_stage")==10,"rollback_restores_all_original_rows_and_archive_receipt");
            check(f.text("SELECT status FROM r325_atomnet_archive_attempt").equals("CLAIMED"),"rolled_back_success_does_not_clear_durable_retry");
            check(run(f,63000).units==1,"failed_retirement_resumes_with_verified_prepared_file");
        }
        System.out.println("ARCHIVE_LIVENESS_ASSERTIONS="+assertions);
    }
}
