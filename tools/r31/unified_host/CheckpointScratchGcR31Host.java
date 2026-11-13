package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.util.*;

/** Actual scratch owner, native persistent SQLite and real private files. All
 * bytes/paths are controlled fixtures; no application or user data is opened. */
public final class CheckpointScratchGcR31Host {
    static int assertions;
    static void check(boolean v,String code){assertions++;if(!v)throw new AssertionError(code);}
    static final String NAME=".tmp-01234567-89ab-cdef-0123-456789abcdef";
    static final byte[] BYTES="actual abandoned pre-rename checkpoint bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    static final class Fixture implements AutoCloseable {
        final Path directory,root,database;final Connection c;final SQLiteDatabase db;String halt;
        Fixture(Path directory)throws Exception{
            this.directory=directory;Files.createDirectories(directory);root=directory.resolve("checkpoints");Files.createDirectories(root);database=directory.resolve("scratch.db");c=DriverManager.getConnection("jdbc:sqlite:"+database);db=new SQLiteDatabase(c);
            db.execSQL("PRAGMA journal_mode=WAL");for(String ddl:CheckpointScratchGcR31.ddlR31())db.execSQL(ddl);
        }
        long writer(String operation,CheckpointArtifactsR26.GcMutationR31 mutation){
            if(operation.equals("R31_GC_SCRATCH_UNLINK_COMMIT")&&"after-unlink".equals(halt))Runtime.getRuntime().halt(73);
            try{check(c.getAutoCommit(),"NO_NESTED_WRITER");c.setAutoCommit(false);try{
                long value=mutation.run(db);
                if(operation.equals("R31_GC_SCRATCH_CLAIM")&&"before-claim-commit".equals(halt))Runtime.getRuntime().halt(71);
                c.commit();
                if(operation.equals("R31_GC_SCRATCH_CLAIM")&&"after-claim-commit".equals(halt))Runtime.getRuntime().halt(72);
                if(operation.equals("R31_GC_SCRATCH_UNLINK_COMMIT")&&"after-unlink-commit".equals(halt))Runtime.getRuntime().halt(74);
                return value;
            }catch(Exception|Error failure){c.rollback();throw failure;}finally{c.setAutoCommit(true);}}
            catch(RuntimeException|Error failure){throw failure;}catch(Exception failure){throw new IllegalStateException(failure);}
        }
        CheckpointScratchGcR31.Result step()throws Exception{return CheckpointScratchGcR31.step(db,root.toFile(),this::writer,System.currentTimeMillis()+120000,60000,64);}
        long scalar(String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("SCALAR_ABSENT");return c.getLong(0);}}
        public void close()throws Exception{c.close();}
    }
    static void killRecovery(Path base,String boundary,int code)throws Exception{
        Path directory=base.resolve(boundary);Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),CheckpointScratchGcR31Host.class.getName(),"child",directory.toString(),boundary).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();check(child.waitFor()==code,"ACTUAL_PROCESS_HALT_"+boundary);
        try(Fixture f=new Fixture(directory)){
            if(boundary.equals("before-claim-commit"))check(f.scalar("SELECT count(*) FROM r31_gc_checkpoint_scratch")==0,"KILL_BEFORE_CLAIM_COMMIT_ROLLS_BACK");
            if(boundary.equals("after-claim-commit")||boundary.equals("after-unlink"))check(f.scalar("SELECT count(*) FROM r31_gc_checkpoint_scratch WHERE state='DELETING'")==1,"DURABLE_TOMBSTONE_SURVIVES_"+boundary);
            CheckpointScratchGcR31.Result recovered=f.step();check(!Files.exists(f.root.resolve(NAME)),"RESTART_FINISHES_EXACT_SCRATCH_"+boundary);
            check(recovered.releasedBytes==(boundary.equals("after-unlink-commit")?0:BYTES.length),"RELEASE_COUNTED_ONCE_AFTER_"+boundary);
            check(recovered.physicalUnlinkedBytes==((boundary.equals("before-claim-commit")||boundary.equals("after-claim-commit"))?BYTES.length:0),"USAGE_ONLY_COUNTS_REAL_UNLINK_NOT_ABSENT_RECOVERY_"+boundary);
            check(f.scalar("SELECT sum(released_bytes) FROM r31_gc_checkpoint_scratch")==BYTES.length,"PERSISTENT_RELEASE_RECEIPT_EXACT_"+boundary);check(f.step().releasedBytes==0,"REPEATED_RECOVERY_ZERO_"+boundary);
        }
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        if(args[0].equals("child")){try(Fixture f=new Fixture(Path.of(args[1]))){Files.write(f.root.resolve(NAME),BYTES);f.halt=args[2];f.step();}throw new AssertionError("KILL_BOUNDARY_NOT_REACHED");}
        Path base=Path.of(args[0]);Files.createDirectories(base);
        try(Fixture f=new Fixture(base.resolve("regular"))){
            Files.write(f.root.resolve(NAME),BYTES);Path canonical=f.root.resolve("a".repeat(64)+".bin");Files.write(canonical,new byte[]{1,2,3});Path unknown=f.root.resolve(".tmp-not-an-owner-uuid");Files.write(unknown,new byte[]{4,5});
            CheckpointScratchGcR31.Result done=f.step();check(done.claimed==1&&done.releasedBytes==BYTES.length&&done.physicalUnlinkedBytes==BYTES.length&&done.physicalUnlinkedFiles==1,"REAL_SCRATCH_CLAIM_UNLINK_COMMIT");check(Files.exists(canonical)&&Files.exists(unknown),"CANONICAL_AND_UNREGISTERED_NAMES_UNTOUCHED");check(f.step().releasedBytes==0,"TERMINAL_RECEIPT_NEVER_RELEASED_TWICE");
            Files.write(f.root.resolve(NAME),new byte[]{9});check(f.step().releasedBytes==0&&Files.exists(f.root.resolve(NAME)),"TERMINAL_NAME_REUSE_NEVER_DELETES_NEW_INCARCATION");
            Path outside=base.resolve("outside.bin");Files.write(outside,new byte[]{7,8,9});String symlink=".tmp-22222222-2222-2222-2222-222222222222";Files.createSymbolicLink(f.root.resolve(symlink),outside.toAbsolutePath());check(f.step().releasedBytes==0&&Files.exists(outside)&&Files.isSymbolicLink(f.root.resolve(symlink)),"SYMLINK_TO_OUTSIDE_PRESERVED");
            String recent=".tmp-33333333-3333-3333-3333-333333333333";Files.write(f.root.resolve(recent),BYTES);CheckpointScratchGcR31.Result grace=CheckpointScratchGcR31.step(f.db,f.root.toFile(),f::writer,System.currentTimeMillis(),60000,64);check(grace.claimed==0&&Files.exists(f.root.resolve(recent)),"PREPARING_GRACE_PERIOD_PRESERVED");
            boolean immutable=false;try{f.db.execSQL("DELETE FROM r31_gc_checkpoint_scratch");}catch(android.database.sqlite.SQLiteException expected){immutable=true;}check(immutable,"COMMITTED_RECEIPTS_ARE_IMMUTABLE");
        }
        try(Fixture f=new Fixture(base.resolve("unlink-failure"))){
            Files.write(f.root.resolve(NAME),BYTES);Files.setPosixFilePermissions(f.root,PosixFilePermissions.fromString("r-xr-xr-x"));try{
                CheckpointScratchGcR31.Result blocked=f.step();check(blocked.releasedBytes==0&&blocked.physicalUnlinkedFiles==0&&Files.exists(f.root.resolve(NAME)),"ACTUAL_UNLINK_FAILURE_RELEASES_NOTHING");check(f.scalar("SELECT count(*) FROM r31_gc_checkpoint_scratch WHERE state='DELETING' AND attempts=1")==1,"ACTUAL_UNLINK_FAILURE_LEAVES_RETRY_TOMBSTONE");
            }finally{Files.setPosixFilePermissions(f.root,PosixFilePermissions.fromString("rwx------"));}
            Files.write(f.root.resolve(NAME),new byte[]{8,8,8});CheckpointScratchGcR31.Result changed=f.step();check(changed.releasedBytes==0&&changed.identityPreserved==1&&Files.exists(f.root.resolve(NAME)),"CLAIMED_FILE_CHANGED_PRESERVED");
        }
        killRecovery(base,"before-claim-commit",71);killRecovery(base,"after-claim-commit",72);killRecovery(base,"after-unlink",73);killRecovery(base,"after-unlink-commit",74);
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=new SQLiteDatabase(c);check(!CheckpointScratchGcR31.ready(db),"MISSING_SCHEMA_IS_PENDING_NOT_ZERO_COMPLETE");check("SCHEMA_PENDING".equals(CheckpointScratchGcR31.report(db).get("state")),"MISSING_SCHEMA_REPORT_EXPLICIT_PENDING");}
        System.out.println("CHECKPOINT_SCRATCH_NATIVE_PASS assertions="+assertions+" realProcessKillBoundaries=4 actualFilesAndNativeSqlite=true AndroidService=NOT_RUN naturalQuality=NOT_PROVEN");
    }
}
