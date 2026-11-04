package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30;
import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Fixed synthetic contention through the actual sole writer/native SQLite. Never Android/device/SLO proof. */
public final class R31WriterContentionHost {
    private static final class Workload {
        final String operation;final DatabaseCoordinator.WriteClass kind;final long periodNanos;final int batch;
        final AtomicLong scheduled=new AtomicLong(),attempted=new AtomicLong(),committed=new AtomicLong(),failed=new AtomicLong(),missed=new AtomicLong();
        final AtomicLong lastProgress=new AtomicLong(),maxGap=new AtomicLong();
        Workload(String name,DatabaseCoordinator.WriteClass kind,long periodMs,int batch){operation=name;this.kind=kind;periodNanos=periodMs*1_000_000L;this.batch=batch;}
        Map<String,Object> snapshot(long now){Map<String,Object> row=new LinkedHashMap<>();row.put("operation",operation);row.put("writeClass",kind.name());row.put("periodMs",periodNanos/1_000_000L);row.put("batchRows",batch);row.put("scheduled",scheduled.get());row.put("attempted",attempted.get());row.put("committed",committed.get());row.put("failed",failed.get());row.put("missedTick",missed.get());row.put("lastProgressMonoNanos",lastProgress.get());row.put("sinceLastProgressMs",(now-lastProgress.get())/1_000_000L);row.put("maxGapBetweenCommitsMs",maxGap.get()/1_000_000L);return row;}
    }
    private static final Workload[] WORKLOADS={
        new Workload("HOST_DRAW_INGEST_1HZ",DatabaseCoordinator.WriteClass.DRAW_INGEST,1000,1),
        new Workload("HOST_HISTORY_BOUNDED_8",DatabaseCoordinator.WriteClass.RESEARCH_DATA,250,8),
        new Workload("HOST_RESEARCH_BOUNDED_8",DatabaseCoordinator.WriteClass.RESEARCH_DATA,100,8),
        new Workload("HOST_REPORT_BOUNDED",DatabaseCoordinator.WriteClass.REPORT,500,1),
        new Workload("HOST_RETENTION_BOUNDED_8",DatabaseCoordinator.WriteClass.RETENTION,250,8)
    };
    private static DatabaseCoordinator coordinator;private static SQLiteDatabase db;
    private static long startNanos,endNanos,startedWall;private static Path output;
    private static BufferedWriter raw;private static volatile Error processFatal;
    private static final byte[] HISTORY_PAYLOAD=new byte[256],REPORT_PAYLOAD=new byte[4096];
    private static void set(Object target,String name,Object value)throws Exception{Field field=DatabaseCoordinator.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    private static synchronized void event(Map<String,Object> row){try{raw.write(JsonR27.write(row));raw.newLine();}catch(java.io.IOException failure){throw new IllegalStateException("HOST_RAW_LOG_FAILED",failure);}}
    private static void max(AtomicLong target,long value){long old;do{old=target.get();if(value<=old)return;}while(!target.compareAndSet(old,value));}
    private static void body(SQLiteDatabase tx,Workload workload,long sequence){
        if(!tx.inTransaction())throw new AssertionError("actual transaction required");
        if(workload.operation.contains("HISTORY"))for(int i=0;i<workload.batch;i++)tx.execSQL("INSERT INTO history(payload) VALUES(?)",new Object[]{HISTORY_PAYLOAD});
        else if(workload.kind==DatabaseCoordinator.WriteClass.RETENTION)tx.execSQL("DELETE FROM history WHERE id IN (SELECT id FROM history ORDER BY id LIMIT 8)");
        else if(workload.kind==DatabaseCoordinator.WriteClass.REPORT)tx.execSQL("INSERT OR REPLACE INTO report VALUES(1,?,?)",new Object[]{sequence,REPORT_PAYLOAD});
        else if(workload.kind==DatabaseCoordinator.WriteClass.DRAW_INGEST)tx.execSQL("INSERT OR REPLACE INTO current_draw VALUES(1,?)",new Object[]{sequence});
        else for(int i=0;i<workload.batch;i++)tx.execSQL("UPDATE learner SET parameter=parameter+1 WHERE id=1");
        tx.execSQL("UPDATE counts SET committed=committed+1 WHERE operation=?",new Object[]{workload.operation});
    }
    private static void worker(Workload workload){
        long next=startNanos,sequence=0;workload.lastProgress.set(startNanos);
        while(next<endNanos&&processFatal==null){
            long now=System.nanoTime(),left=next-now;
            if(left>0){try{Thread.sleep(Math.min(1000,left/1_000_000L),left<1_000_000L?(int)left:0);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();return;}continue;}
            workload.scheduled.incrementAndGet();Map<String,Object> row=new LinkedHashMap<>();row.put("operation",workload.operation);row.put("sequence",sequence);row.put("plannedMonoNanos",next);row.put("wallMs",System.currentTimeMillis());
            if(now-next>=workload.periodNanos){workload.missed.incrementAndGet();row.put("outcome","MISSED_TICK");row.put("observedMonoNanos",now);event(row);next+=workload.periodNanos;sequence++;continue;}
            final long current=sequence;final long[] phase={-1,-1,-1};long entered=System.nanoTime();workload.attempted.incrementAndGet();
            DatabaseCoordinator.diagnosticFaultR31.set(stage->{long at=System.nanoTime();if("LOCK_WAIT".equals(stage))phase[0]=at;else if("AFTER_COMMIT".equals(stage))phase[1]=at;else if("WRITER_HOLD".equals(stage))phase[2]=at;});
            try{
                DatabaseCoordinator.CommitIdentityR31 identity=new DatabaseCoordinator.CommitIdentityR31(workload.operation+":"+sequence,"fixed-payload:"+sequence,workload.operation,"HOST_FIXED_OWNER",1,"fixed-workload-v1",sequence,"R31_HOST_CONTENTION_V1");
                DatabaseCoordinator.CommitResultR31<Integer> result=coordinator.runIdempotentAtomicWriteR31(DatabaseCoordinator.WriteContext.of(workload.kind,workload.operation),identity,tx->{body(tx,workload,current);return 1;},1);
                long committedAt=System.nanoTime(),prior=workload.lastProgress.getAndSet(committedAt);max(workload.maxGap,committedAt-prior);workload.committed.incrementAndGet();row.put("outcome",result.outcome.name());row.put("replay",result.replay);
            }catch(Throwable failure){
                workload.failed.incrementAndGet();Error fatal=FailureDispositionR31.processFatalCause(failure);if(fatal!=null)processFatal=fatal;
                row.put("outcome",failure instanceof DatabaseCoordinator.WriteRejectedR31?((DatabaseCoordinator.WriteRejectedR31)failure).commitOutcome.name():"UNOBSERVED");row.put("failureClass",failure.getClass().getName());row.put("failureCategory",FailureDispositionR31.classify(failure).category);row.put("failureMessage",String.valueOf(failure.getMessage()));
            }finally{
                DatabaseCoordinator.diagnosticFaultR31.remove();row.put("enteredMonoNanos",entered);row.put("returnedMonoNanos",System.nanoTime());row.put("lockWaitDiagnosticMonoNanos",phase[0]<0?null:phase[0]);row.put("afterCommitDiagnosticMonoNanos",phase[1]<0?null:phase[1]);row.put("afterUnlockDiagnosticMonoNanos",phase[2]<0?null:phase[2]);row.put("phaseTimestampMeaning","passive hook observations; exact wait/hold aggregates are production WriterHoldLedgerR30");event(row);
            }
            next+=workload.periodNanos;sequence++;
        }
    }
    private static void receipt(String status,long requestedSeconds)throws Exception{
        long now=System.nanoTime();Map<String,Object> state=new LinkedHashMap<>();state.put("format","R31_HOST_CONTENTION_RECEIPT_V1");state.put("status",status);state.put("startedWallMs",startedWall);state.put("sampleWallMs",System.currentTimeMillis());state.put("startedMonoNanos",startNanos);state.put("sampleMonoNanos",now);state.put("requestedSeconds",requestedSeconds);state.put("actualElapsedSeconds",(now-startNanos)/1_000_000_000.0);state.put("durationReached",now>=endNanos);state.put("deviceStatus","NOT_RUN");state.put("scope","fixed HOST synthetic workload through production coordinator and native SQLite; not Android/24h/72h/natural quality acceptance");
        List<Object> tasks=new ArrayList<>(),ledger=new ArrayList<>();long failures=0,misses=0;boolean fairness=true;
        for(Workload work:WORKLOADS){tasks.add(work.snapshot(now));failures+=work.failed.get();misses+=work.missed.get();if(work.kind!=DatabaseCoordinator.WriteClass.DRAW_INGEST&&Math.max(work.maxGap.get(),now-work.lastProgress.get())>5_000_000_000L)fairness=false;}
        for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.snapshot())ledger.add(WriterHoldLedgerR30.toMap(stat));
        state.put("workload",tasks);state.put("productionWriterLedger",ledger);state.put("failureAttempts",failures);state.put("missedTicks",misses);state.put("researchBackgroundWithin5SecondServiceWindow",fairness);state.put("writerProtocolObservation",DatabaseCoordinator.writeProtocolSnapshotR31());state.put("businessAndDeviceAcceptance","NOT_ASSERTED");
        Path temporary=output.resolve("status.json.tmp");Files.write(temporary,(JsonR27.write(state)+"\n").getBytes(StandardCharsets.UTF_8));Files.move(temporary,output.resolve("status.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        synchronized(R31WriterContentionHost.class){raw.flush();}
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("database-path output-directory seconds");
        File database=new File(args[0]);output=Path.of(args[1]);Files.createDirectories(output);long seconds=Long.parseLong(args[2]);if(seconds<1)throw new IllegalArgumentException("seconds");
        Class.forName("org.sqlite.JDBC");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+database.getPath());BufferedWriter log=Files.newBufferedWriter(output.resolve("attempts.jsonl"),StandardCharsets.UTF_8)){
            raw=log;db=new SQLiteDatabase(connection);db.execSQL("PRAGMA journal_mode=WAL");for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);
            db.execSQL("CREATE TABLE counts(operation TEXT PRIMARY KEY,committed INTEGER NOT NULL)");for(Workload workload:WORKLOADS)db.execSQL("INSERT INTO counts VALUES(?,0)",new Object[]{workload.operation});db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY,payload BLOB)");db.execSQL("CREATE TABLE learner(id INTEGER PRIMARY KEY,parameter INTEGER NOT NULL)");db.execSQL("INSERT INTO learner VALUES(1,0)");db.execSQL("CREATE TABLE report(id INTEGER PRIMARY KEY,sequence INTEGER,payload BLOB)");db.execSQL("CREATE TABLE current_draw(id INTEGER PRIMARY KEY,sequence INTEGER)");
            Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);coordinator=(DatabaseCoordinator)((Unsafe)uf.get(null)).allocateInstance(DatabaseCoordinator.class);Field lf=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lf.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lf.get(null));set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",new android.app.Application(database));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            startedWall=System.currentTimeMillis();startNanos=System.nanoTime();endNanos=startNanos+seconds*1_000_000_000L;List<Thread> workers=new ArrayList<>();for(Workload workload:WORKLOADS){Thread worker=new Thread(()->worker(workload),workload.operation);workers.add(worker);worker.start();}
            receipt("IN_PROGRESS",seconds);while(System.nanoTime()<endNanos&&processFatal==null){Thread.sleep(Math.min(1000,Math.max(1,(endNanos-System.nanoTime())/1_000_000L)));receipt("IN_PROGRESS",seconds);}
            for(Thread worker:workers)worker.join(5000);for(Thread worker:workers)if(worker.isAlive())throw new IllegalStateException("HOST_WORKER_DRAINING_NOT_TERMINATED");
            receipt(processFatal==null?"COMPLETED_HOST_WINDOW":"FAILED_PROCESS_FATAL",seconds);if(processFatal!=null)throw processFatal;
            System.out.println("R31_HOST_CONTENTION_WINDOW_FINISHED actual_monotonic_seconds="+((System.nanoTime()-startNanos)/1e9)+" required_seconds="+seconds+" scope=HOST_NOT_ANDROID_NOT_SOAK_DEVICE");
        }
    }
}
