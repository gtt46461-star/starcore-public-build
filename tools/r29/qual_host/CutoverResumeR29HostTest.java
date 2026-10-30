package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Actual SQLite publication receipts and sealed database files. Controlled Root writer failures are HOST
 * exception/rollback injection; Android process death and full-App ownership handoff remain separate tests. */
public final class CutoverResumeR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_CUTOVER_RESUME|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static String hash(File file)throws Exception{return ResearchCutoverR29.fileHash(file,Long.MAX_VALUE,()->false);}
    static final long EPOCH=1729000001234L;
    static final String SCHEMA=StatsR27.sha256("HOST_FULL_TYPED_SCHEMA"),ROOT=StatsR27.sha256("HOST_COPIED_TYPED_ROOT");
    static final class Fixture implements AutoCloseable {
        final Path dir;final File rootFile,candidate,published,baseline;SQLiteDatabase root;
        final String originalHash;
        Fixture(String phase,boolean renamed)throws Exception{
            dir=Files.createTempDirectory("r29-cutover-resume-");rootFile=dir.resolve("root.db").toFile();candidate=dir.resolve("starcore-r29-research.candidate.db").toFile();published=dir.resolve(ResearchRouteR29.FILE_NAME).toFile();baseline=dir.resolve("starcore-r29-baseline-"+EPOCH+".db").toFile();
            root=SQLiteDatabase.openOrCreateDatabase(rootFile,null);root.execSQL("PRAGMA journal_mode=WAL");root.execSQL("PRAGMA synchronous=FULL");root.execSQL(ResearchCutoverR29.PUBLICATION);root.execSQL(ResearchRouteR29.ROUTE_DDL);root.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT)");root.execSQL("INSERT INTO app_state VALUES('live','SOURCE_BEFORE_SNAPSHOT')");
            try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(candidate,null)){
                db.execSQL("PRAGMA journal_mode=WAL");db.execSQL("PRAGMA synchronous=FULL");db.execSQL(ResearchRouteR29.OWNER_DDL);db.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,?,?)",new Object[]{SCHEMA,ROOT,EPOCH,1L});
                db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY,dirty INTEGER)");db.execSQL("INSERT INTO draws VALUES(1,0)");db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,status TEXT)");db.execSQL("INSERT INTO r26_task VALUES('durable-original','YIELDED')");
                db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT)");db.execSQL("INSERT INTO app_state VALUES('snapshot','IMMUTABLE_BASELINE')");
                try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new AssertionError("FIXTURE_CHECKPOINT_FAILED");}
            }
            ResearchCutoverR29.sync(candidate);ResearchCutoverR29.copyBaseline(candidate,baseline,Long.MAX_VALUE,()->false);originalHash=hash(baseline);
            if(renamed&&!candidate.renameTo(published))throw new AssertionError("FIXTURE_RENAME_FAILED");
            boolean pinned=!phase.equals("PREPARED");root.execSQL("INSERT INTO r29_migration_publication(singleton,epoch,candidate_name,phase,baseline_name,baseline_sha,source_root,updated_at) VALUES(1,?,?,?,?,?,?,?)",new Object[]{EPOCH,candidate.getName(),phase,pinned?baseline.getName():"",pinned?originalHash:"",pinned?ROOT:"",1L});
        }
        void restart(){root.close();root=SQLiteDatabase.openDatabase(rootFile.getPath(),null,SQLiteDatabase.OPEN_READWRITE);}
        void moveRoot(){root.execSQL("INSERT OR REPLACE INTO app_state VALUES('live','ROOT_ADVANCED_AFTER_SEAL')");}
        String phase(){return s(root,"SELECT phase FROM r29_migration_publication WHERE singleton=1");}
        boolean unchanged()throws Exception{return baseline.isFile()&&hash(baseline).equals(originalHash)&&(!candidate.isFile()||hash(candidate).equals(originalHash))&&(!published.isFile()||hash(published).equals(originalHash));}
        public void close()throws Exception{
            root.close();try(java.util.stream.Stream<Path> p=Files.walk(dir)){p.sorted(Comparator.reverseOrder()).forEach(f->{try{Files.deleteIfExists(f);}catch(Exception e){throw new RuntimeException(e);}});}
        }
    }
    static final class InjectedWriter implements MigrationCopyR29.Transaction {
        final Fixture fixture;String beforeCommitPhase,afterCommitPhase;boolean fired;int commits;
        InjectedWriter(Fixture f){fixture=f;}
        public void write(MigrationCopyR29.Body body)throws Exception{
            SQLiteDatabase root=fixture.root;root.beginTransactionNonExclusive();String next=null;
            try{body.run(root);next=fixture.phase();if(!fired&&next.equals(beforeCommitPhase)){fired=true;throw new IllegalStateException("CP3_DB_BUSY_HOST_PHASE_BEFORE_COMMIT:"+next);}root.setTransactionSuccessful();}
            finally{root.endTransaction();}
            commits++;if(!fired&&next.equals(afterCommitPhase)){fired=true;throw new IllegalStateException("CP3_DB_BUSY_HOST_PHASE_AFTER_COMMIT:"+next);}
        }
    }
    interface Attempt {void run()throws Exception;}
    static void rejects(String name,Attempt work)throws Exception{boolean failed=false;try{work.run();}catch(Exception expected){failed=true;System.out.println("R29_CUTOVER_RESUME|REJECTED|"+name+"|"+expected.getClass().getSimpleName()+":"+expected.getMessage());}check(name,failed);}
    static ResearchCutoverR29.Publication resume(Fixture f,InjectedWriter writer)throws Exception{
        return ResearchCutoverR29.resumePublication(f.root,f.candidate,f.published,writer,Long.MAX_VALUE,()->false);
    }
    static void unchanged(String name,Fixture f)throws Exception{check(name,f.unchanged());}
    public static void main(String[]args)throws Exception{
        if(args.length>0&&args[0].equals("--historical-delete-control")){
            try(Fixture f=new Fixture("RENAMED",true)){
                Class<?> old=Class.forName("com.ethk3.novaonepro.strategyv2.HistoricalCutoverRetryR29");
                old.getDeclaredMethod("cleanup",File.class,File.class).invoke(null,f.candidate,f.published);
                if(!f.published.isFile())throw new AssertionError("OLD_RETRY_DELETED_VERIFIED_PUBLISHED");
            }
            throw new AssertionError("HISTORICAL_DELETE_CONTROL_UNEXPECTED_PASS");
        }
        try(Fixture f=new Fixture("VERIFIED",false)){
            InjectedWriter first=new InjectedWriter(f);first.beforeCommitPhase="RENAMED";
            rejects("R1_RENAME_SUCCEEDS_BUT_ROOT_RECEIPT_BUSY_RETAINS_ORIGINAL_ERROR",()->resume(f,first));
            check("R2_AFTER_ROLLBACK_ROOT_STILL_VERIFIED_BUT_RENAMED_FILE_RETAINED",first.fired&&f.phase().equals("VERIFIED")&&f.published.isFile()&&!f.candidate.exists());
            unchanged("R3_BUSY_AFTER_RENAME_PRESERVES_SEALED_BASELINE_AND_PUBLISHED_BYTES",f);
            f.restart();f.moveRoot();ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R4_RESTART_RESUMES_ACTUAL_RENAMED_FILE_WITHOUT_MOVING_ROOT_RECOPY",p!=null&&p.phase.equals("RENAMED")&&f.phase().equals("RENAMED")&&p.epoch==EPOCH&&p.sourceRoot.equals(ROOT)&&p.schemaHash.equals(SCHEMA));
            unchanged("R5_RETRY_WITH_ADVANCED_ROOT_PRESERVES_ORIGINAL_VERIFIED_BYTES",f);
        }
        try(Fixture f=new Fixture("VERIFIED",false)){
            InjectedWriter first=new InjectedWriter(f);first.afterCommitPhase="RENAMED";
            rejects("R6_LOST_RENAMED_ACK_RETAINS_REAL_COMMITTED_RECEIPT",()->resume(f,first));
            check("R7_LOST_ACK_ROOT_AND_FILE_BOTH_DURABLY_RENAMED",first.fired&&f.phase().equals("RENAMED")&&f.published.isFile());
            f.restart();ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R8_IDEMPOTENT_RENAMED_RESUME_AFTER_RESTART",p!=null&&p.phase.equals("RENAMED")&&p.baselineSha.equals(f.originalHash));
            unchanged("R9_LOST_ACK_RETRY_DOES_NOT_REWRITE_BASELINE_OR_RP",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R10_EXISTING_RENAMED_PROOF_ACCEPTED_WITHOUT_ROOT_REBUILD",p!=null&&p.phase.equals("RENAMED")&&p.sourceRoot.equals(ROOT));
            unchanged("R11_RENAMED_CANDIDATE_AND_BASELINE_BINDING_BYTES_PRESERVED",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));InjectedWriter first=new InjectedWriter(f);first.beforeCommitPhase="PUBLISHED";
            rejects("R25_SHORT_ROOT_ROUTE_PUBLICATION_BUSY_ROLLS_BACK_BOTH_RECEIPTS",()->first.write(db->ResearchRouteR29.publishWithin(db,p.schemaHash,p.sourceRoot,p.epoch)));
            check("R26_FAILED_ROUTE_PUBLICATION_LEAVES_RENAMED_AND_NO_ACTIVE_ROUTE",first.fired&&f.phase().equals("RENAMED")&&n(f.root,"SELECT count(*) FROM r29_database_route")==0);
            unchanged("R27_FINAL_ROUTE_BUSY_PRESERVES_RP_AND_SEALED_BASELINE_BYTES",f);
            f.restart();f.moveRoot();ResearchCutoverR29.Publication retry=resume(f,new InjectedWriter(f));
            new InjectedWriter(f).write(db->ResearchRouteR29.publishWithin(db,retry.schemaHash,retry.sourceRoot,retry.epoch));
            check("R28_RESTART_FINAL_PUBLISH_COMMITS_REAL_ROUTE_AND_PUBLICATION_ATOMICALLY",f.phase().equals("PUBLISHED")&&ResearchRouteR29.active(f.root)&&n(f.root,"SELECT epoch FROM r29_database_route WHERE singleton=1")==EPOCH&&s(f.root,"SELECT source_root FROM r29_database_route WHERE singleton=1").equals(ROOT));
            unchanged("R29_FINAL_ROUTE_RETRY_NEVER_REWRITES_VERIFIED_RESEARCH_IMAGE",f);
        }
        try(Fixture f=new Fixture("PREPARED",false)){
            ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R12_CRASH_AFTER_SEAL_BEFORE_ROOT_PIN_RESUMES_SAME_READY_BYTES",p!=null&&p.phase.equals("RENAMED")&&p.baselineSha.equals(f.originalHash));
            unchanged("R13_ORPHAN_SEAL_RECOVERY_KEEPS_ORIGINAL_COPIED_BASELINE",f);
        }
        try(Fixture f=new Fixture("PREPARED",false)){
            Files.delete(f.baseline.toPath());ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R14_UNSEALED_PREPARED_RETURNS_FRESH_COPY_REQUIRED_WITHOUT_PROMOTION",p==null&&f.phase().equals("PREPARED")&&!f.candidate.exists()&&!f.published.exists());
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            f.root.execSQL("UPDATE r29_migration_publication SET source_root=? WHERE singleton=1",new Object[]{StatsR27.sha256("WRONG_ROOT")});
            rejects("R15_ROOT_RECEIPT_OWNER_MISMATCH_FAILS_CLOSED",()->resume(f,new InjectedWriter(f)));
            unchanged("R16_WRONG_ROOT_RECEIPT_NEVER_DELETES_VERIFIED_FILES",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(f.baseline,true)){out.write(1);}
            rejects("R17_SEALED_BASELINE_SHA_CORRUPTION_FAILS_CLOSED",()->resume(f,new InjectedWriter(f)));
            check("R18_BAD_BASELINE_DOES_NOT_REBUILD_OR_DELETE_ORIGINAL_RP",f.published.isFile()&&hash(f.published).equals(f.originalHash));
        }
        try(Fixture f=new Fixture("VERIFIED",false)){
            try(SQLiteDatabase changed=SQLiteDatabase.openDatabase(f.candidate.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){changed.execSQL("UPDATE app_state SET state_value='TAMPERED' WHERE state_key='snapshot'");try(Cursor c=changed.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){c.moveToFirst();}}
            rejects("R19_CANDIDATE_SHA_NOT_SEALED_BASELINE_FAILS_CLOSED",()->resume(f,new InjectedWriter(f)));
            check("R20_BAD_CANDIDATE_IS_NOT_PUBLISHED_OR_BASELINE_OVERWRITTEN",!f.published.exists()&&hash(f.baseline).equals(f.originalHash));
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            f.root.execSQL("UPDATE r29_migration_publication SET baseline_name='../foreign.db' WHERE singleton=1");
            rejects("R21_UNOWNED_BASELINE_PATH_FAILS_CLOSED",()->resume(f,new InjectedWriter(f)));
            unchanged("R22_PATH_REJECTION_PRESERVES_REAL_VERIFIED_ARTIFACTS",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            rejects("R23_CANCELLED_RESUME_CANNOT_PROMOTE_ROUTE",()->ResearchCutoverR29.resumePublication(f.root,f.candidate,f.published,new InjectedWriter(f),Long.MAX_VALUE,()->true));
            unchanged("R24_CANCELLED_RESUME_PRESERVES_VERIFIED_BYTES",f);
        }
        try(Fixture f=new Fixture("VERIFIED",false)){
            Files.delete(f.candidate.toPath());f.moveRoot();ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R30_VERIFIED_MISSING_CANDIDATE_RESTORES_EXACT_ORIGINAL_SEALED_IMAGE",p!=null&&p.phase.equals("RENAMED")&&f.published.isFile()&&!f.candidate.exists());
            unchanged("R31_RECOVERY_READS_SEALED_BASELINE_INSTEAD_OF_NEW_ROOT_ROWS",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            Files.delete(f.published.toPath());f.restart();ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R32_RENAMED_BUT_MISSING_RP_RESTORES_PINNED_ORIGINAL_BYTES",p!=null&&p.phase.equals("RENAMED")&&f.published.isFile()&&f.phase().equals("RENAMED"));
            unchanged("R33_RENAMED_REPAIR_PRESERVES_SEALED_BASELINE_HASH",f);
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            ResearchCutoverR29.Publication proof=resume(f,new InjectedWriter(f));new InjectedWriter(f).write(db->ResearchRouteR29.publishWithin(db,proof.schemaHash,proof.sourceRoot,proof.epoch));
            try(SQLiteDatabase db=SQLiteDatabase.openDatabase(f.published.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
                db.execSQL("UPDATE r26_task SET status='LEGAL_POST_PUBLICATION_PROGRESS' WHERE task_id='durable-original'");
                try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new AssertionError("POST_PUBLICATION_CHECKPOINT_FAILED");}
            }
            String progressed=hash(f.published);check("R34_LEGAL_RESEARCH_PROGRESS_CHANGES_CURRENT_RP_BYTES",!progressed.equals(f.originalHash));
            f.restart();ResearchCutoverR29.Publication p=resume(f,new InjectedWriter(f));
            check("R35_PUBLISHED_RESUME_ACCEPTS_LEGAL_PROGRESS_WITH_SAME_OWNER_AND_ROUTE",p!=null&&p.phase.equals("PUBLISHED")&&ResearchRouteR29.active(f.root)&&hash(f.published).equals(progressed)&&hash(f.baseline).equals(f.originalHash));
            Files.delete(f.published.toPath());
            rejects("R36_PUBLISHED_RP_MISSING_NEVER_REBUILDS_FROM_OLD_BASELINE_LOSING_PROGRESS",()->resume(f,new InjectedWriter(f)));
            check("R37_MISSING_PUBLISHED_FILE_REMAINS_FAIL_CLOSED_WITH_BASELINE_INTACT",!f.published.exists()&&!f.candidate.exists()&&hash(f.baseline).equals(f.originalHash));
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            Files.write(Paths.get(f.published.getPath()+"-wal"),new byte[]{1,2,3});
            rejects("R38_UNSEALED_RENAMED_WAL_FAILS_CLOSED",()->resume(f,new InjectedWriter(f)));
            check("R39_UNSEALED_WAL_REJECTION_DOES_NOT_DELETE_EVIDENCE",f.published.isFile()&&new File(f.published.getPath()+"-wal").isFile()&&hash(f.baseline).equals(f.originalHash));
        }
        try(Fixture f=new Fixture("RENAMED",true)){
            rejects("R40_COPY_BASELINE_NEVER_OVERWRITES_EXISTING_SEAL",()->ResearchCutoverR29.copyBaseline(f.published,f.baseline,Long.MAX_VALUE,()->false));
            unchanged("R41_EXISTING_SEAL_REJECTION_PRESERVES_ORIGINAL_BYTE_PROOF",f);
            rejects("R42_FINAL_ROUTE_PUBLISH_REQUIRES_REAL_ROOT_TRANSACTION",()->ResearchRouteR29.publishWithin(f.root,SCHEMA,ROOT,EPOCH));
            check("R43_NO_ROUTE_OR_PHASE_ADVANCEMENT_OUTSIDE_ROOT_TRANSACTION",f.phase().equals("RENAMED")&&n(f.root,"SELECT count(*) FROM r29_database_route")==0);
        }
        System.out.println("R29_CUTOVER_RESUME|ALL_PASS|checks="+checks+"|REAL_SQLITE_DURABLE_PHASE_HOST_FAULT_ROLLBACK_ONLY_ANDROID_PROCESS_KILL_SEPARATE");
    }
}
