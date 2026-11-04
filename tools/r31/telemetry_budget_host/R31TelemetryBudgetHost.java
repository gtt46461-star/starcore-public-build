package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Fault fixtures are explicit. All business SQL/CAS/receipt logic is the actual production source. */
public final class R31TelemetryBudgetHost {
    static String scenario;static File output;static SQLiteDatabase db;static DatabaseCoordinator coordinator;static DbStore store;
    static final List<Map<String,Object>> transactions=new ArrayList<>(),sqlCalls=new ArrayList<>();
    static Map<String,Object> current;static boolean observe,baseline,hold,injected;static int targetBegins,assertions;
    static Map<String,Object> lastEnded;static WriterHoldLedgerR30.Stat lastWriterStat;static long previousHoldSum,previousWaitSum;
    static Runnable atFirstWrite;static int failureTx;static long sourceBefore;
    static void need(boolean test,String message){assertions++;if(!test)throw new AssertionError(message);}
    static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    static long scalar(String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static Object[] copy(Object[] values){return values==null?new Object[0]:values.clone();}
    static long bytes(Object[] values){long n=0;if(values!=null)for(Object value:values)n+=value instanceof String?((String)value).getBytes(StandardCharsets.UTF_8).length:value instanceof byte[]?((byte[])value).length:8;return n;}
    static final SQLiteDatabase.Observer observer=new SQLiteDatabase.Observer(){
        public void begin(SQLiteDatabase actual){
            if(actual!=db||!observe)return;
            current=new LinkedHashMap<>();current.put("sequence",++targetBegins);current.put("sqlCount",0L);current.put("dmlCount",0L);current.put("ddlCount",0L);current.put("bindBytes",0L);current.put("case",scenario);
            transactions.add(current);
            if(!injected&&scenario.contains("before-commit-failure")&&targetBegins==2){actual.failBeforeCommitOnce=true;injected=true;}
            if(!injected&&scenario.contains("after-commit-ack-failure")&&targetBegins==2){actual.failAfterCommitOnce=true;injected=true;}
            if(!injected&&scenario.contains("unknown-then-receipt")&&targetBegins==2){actual.failAfterCommitOnce=true;SQLiteDatabase.rejectReadback=true;injected=true;}
        }
        public void sql(SQLiteDatabase actual,String sql,Object[] args,boolean inTransaction){
            if(actual!=db||!observe)return;
            Map<String,Object> call=new LinkedHashMap<>();call.put("case",scenario);call.put("tx",inTransaction&&current!=null?current.get("sequence"):null);call.put("sql",sql);call.put("bindBytes",bytes(args));call.put("argumentCount",args==null?0:args.length);call.put("thread",Thread.currentThread().getName());sqlCalls.add(call);
            if(!inTransaction||current==null)return;
            current.put("sqlCount",number(current,"sqlCount")+1);current.put("bindBytes",number(current,"bindBytes")+bytes(args));
            boolean dml=sql.matches("(?is)^\\s*(INSERT|UPDATE|DELETE|REPLACE)\\b.*");
            if(dml)current.put("dmlCount",number(current,"dmlCount")+1);
            if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP|VACUUM|REINDEX|ATTACH|DETACH)\\b.*"))current.put("ddlCount",number(current,"ddlCount")+1);
            boolean own=sql.startsWith("UPDATE r27_chart_cache SET payload_json=")||sql.startsWith("INSERT OR IGNORE INTO r30_writer_hold");
            if(baseline)own=own||sql.startsWith("INSERT OR REPLACE INTO r30_writer_hold");
            if(own&&atFirstWrite!=null){Runnable action=atFirstWrite;atFirstWrite=null;action.run();}
            if(own&&!injected&&(scenario.contains("first-sql-failure")&&targetBegins==1||scenario.contains("middle-sql-failure")&&targetBegins==2)){
                injected=true;failureTx=targetBegins;throw new SQLiteException("HOST_INJECTED_REAL_SQL_BODY_FAILURE");
            }
        }
        public void end(SQLiteDatabase actual,boolean committed,long duration){
            if(actual!=db||!observe||current==null)return;
            current.put("committed",committed);current.put("actualTransactionElapsedNanos",duration);current.put("actualTransactionElapsedMs",duration/1_000_000.0);lastEnded=current;current=null;
        }
    };
    static void schemas(boolean holdTable){
        db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT NOT NULL,key TEXT NOT NULL,payload_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(panel_id,key))");
        for(String ddl:SchemaR27.R31_WRITE_DDL)db.execSQL(ddl);
        if(holdTable)db.execSQL(HoldLedgerFlushR30.DDL);
        db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,owner TEXT,fence INTEGER,status TEXT)");
        db.execSQL("CREATE TABLE r26_session(session_id TEXT PRIMARY KEY,user_enabled INTEGER,state TEXT)");
        db.execSQL("INSERT INTO r26_session VALUES('host-owner',1,'RUNNING')");
        db.execSQL("INSERT INTO r26_task VALUES('r27-health','host-owner',1,'RUNNING')");
        db.execSQL("INSERT INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms) VALUES('r27-health','host-owner',1,?,?)",new Object[]{IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+600000});
    }
    static void tx(DatabaseCoordinator.WriteCallable<Integer> body){coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,"R30_TIMELINE_COMMIT_OBSERVATIONS"),body);}
    static void seedTimeline(int count,int padding){
        tx(actual->{for(int n=0;n<count;n++){
            String trace="ORIGINAL_REQUEST_"+n;String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,trace,"draw-hash");
            IssueTimelineWriterR30.markEventAt(actual,"202611040001",IssueTimelineR30.EventRole.LIVE,IssueTimelineR30.EventKind.DRAW,trace,null,null,"draw-hash",IssueTimelineR30.Stage.FIRST_OBSERVED,1000+n,2000+n,"HOST_FIXTURE_BOOT");
            IssueTimelineWriterR30.markEventAt(actual,"202611040001",IssueTimelineR30.EventRole.LIVE,IssueTimelineR30.EventKind.DRAW,trace,null,null,"draw-hash",IssueTimelineR30.Stage.DRAW_COMMITTED,1100+n,2100+n,"HOST_FIXTURE_BOOT");
            if(padding>0){Map<String,Object> row=IssueTimelineWriterR30.readEvent(actual,id);char[] repeated=new char[padding];Arrays.fill(repeated,'x');row.put("original_opaque_source_field",new String(repeated));IssueTimelineWriterR30.saveEvent(actual,row);}
            IssueTimelineWriterR30.committedAt(id,IssueTimelineR30.Stage.SETTLED,3000+n,4000+n,"ORIGINAL_OBSERVER_BOOT");
        }return 0;});
    }
    static long timelinePending(){return ((Number)IssueTimelineWriterR30.commitObservationState().get("pending")).longValue();}
    static long timelineDurable(){long n=0;try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='"+IssueTimelineR30.EVENT_DRAW_PANEL+"'",null)){while(c.moveToNext())if(((Map<?,?>)JsonR27.parse(c.getString(0))).get("durable_mono")!=null)n++;}return n;}
    static void timelineFlush(){IssueTimelineWriterR30.flushCommitted(store);}
    static void holdFlush(){HoldLedgerFlushR30.maybeFlush(store);}
    static void flush(){if(hold)holdFlush();else timelineFlush();}
    static long holdPending()throws Exception{Method method=HoldLedgerFlushR30.class.getMethod("flushStateR31");return ((Number)((Map<?,?>)method.invoke(null)).get("pendingRows")).longValue();}
    static void holdSamples(int count){WriterHoldLedgerR30.resetForTest();for(int i=0;i<count;i++){WriterHoldLedgerR30.record("FIXTURE_HOLD_"+i,false,7+i,11+i,i%3==0);WriterHoldLedgerR30.record("FIXTURE_HOLD_"+i,false,9+i,15+i,false);}}
    static void diagnose(boolean enable)throws Exception{
        Field hook=DatabaseCoordinator.class.getDeclaredField("diagnosticFaultR31");hook.setAccessible(true);ThreadLocal<Object> local=(ThreadLocal<Object>)hook.get(null);
        if(!enable){local.remove();return;}
        Class<?> api=Class.forName("com.ethk3.novaonepro.DatabaseCoordinator$DiagnosticFaultR31");
        Object proxy=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(object,method,args)->{
            if(args!=null&&args.length==1&&"AFTER_COMMIT".equals(args[0])&&scenario.contains("postcommit-diagnostic"))throw new IllegalStateException("HOST_POST_COMMIT_DIAGNOSTIC_FAULT");
            if(args!=null&&args.length==1&&"WRITER_HOLD".equals(args[0])&&observe&&lastEnded!=null){
                String operation=hold?"R30_WRITER_HOLD_FLUSH":"R30_TIMELINE_COMMIT_OBSERVATIONS";
                for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.snapshot())if(stat.operation.equals(operation)){
                    long holdSum=stat.holdSum.get(),waitSum=stat.waitSum.get();if(stat!=lastWriterStat){previousHoldSum=0;previousWaitSum=0;}
                    lastEnded.put("actualWriterHoldMsFromProductionLedger",holdSum-previousHoldSum);lastEnded.put("actualWriterWaitMsFromProductionLedger",waitSum-previousWaitSum);previousHoldSum=holdSum;previousWaitSum=waitSum;lastWriterStat=stat;break;
                }
            }
            return null;
        });local.set(proxy);
    }
    static void concurrent(Runnable work){
        Thread thread=new Thread(work,"host-real-concurrent-telemetry-producer");thread.start();try{thread.join(3000);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new AssertionError(error);}need(!thread.isAlive(),"actual producer thread exited before continuing");
    }
    static void verifyTimeline(int count){
        need(timelineDurable()==count,"every original timeline event persisted once");need(timelinePending()==0,"successful commit removes exactly those pending observation instances");
        for(int n=0;n<count;n++){
            String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_"+n,"draw-hash");Map<String,Object> row=IssueTimelineWriterR30.readEvent(db,id);
            need(((Number)row.get("wall_first_observed")).longValue()==1000+n,"original stage wall clock");need(((Number)row.get("mono_first_observed")).longValue()==2000+n,"original stage monotonic clock");
            need(((Number)row.get("durable_wall")).longValue()==3000+n,"original observation wall clock");need(((Number)row.get("durable_mono")).longValue()==4000+n,"original observation monotonic clock");need("ORIGINAL_OBSERVER_BOOT".equals(row.get("durable_boot")),"original observation boot");
        }
    }
    static void verifyHold(int count){
        need(scalar("SELECT count(*) FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'")==count,"all distinct operation histograms retained");
        need(scalar("SELECT sum(count) FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'")==2L*count,"no histogram samples dropped/duplicated");
        need(scalar("SELECT count(DISTINCT window_start) FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'")==1,"same original complete sample window start");
        need(scalar("SELECT count(DISTINCT window_end) FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'")==1,"same original complete sample window end");
        try(Cursor c=db.rawQuery("SELECT count,histogram_json,operation FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'",null)){
            while(c.moveToNext()){
                Map<?,?> histogram=(Map<?,?>)JsonR27.parse(c.getString(1));long holds=0,waits=0;for(Object n:(List<?>)histogram.get("holdCounts"))holds+=((Number)n).longValue();for(Object n:(List<?>)histogram.get("waitCounts"))waits+=((Number)n).longValue();
                need(holds==2&&waits==2&&c.getLong(0)==2,"both raw histogram denominators remain2: "+c.getString(2));
            }
        }
    }
    static void budgetEvidence(){
        long cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);int attempts=0;long maxSql=0,maxBytes=0;double maxMs=0;
        for(Map<String,Object> record:transactions){
            attempts++;maxSql=Math.max(maxSql,number(record,"sqlCount"));maxBytes=Math.max(maxBytes,number(record,"bindBytes"));if(record.get("actualTransactionElapsedMs")!=null)maxMs=Math.max(maxMs,((Number)record.get("actualTransactionElapsedMs")).doubleValue());
            need(record.get("actualWriterHoldMsFromProductionLedger")!=null,"per-attempt real sole-writer hold recorded by actual production ledger");
            if(!baseline){need(number(record,"ddlCount")==0,"real business transaction executed no DDL");need(number(record,"sqlCount")<=64,"every actual transaction SQL count including protocol<=64: "+record);need(number(record,"bindBytes")<=cap,"actual binds including all protocol<=dynamic cap: "+record);}
        }
        need(baseline||attempts>0||scenario.equals("missing-schema"),"real production owner transaction observed");
        if(!baseline)need(SQLiteDatabase.maxPreparedPayloadBytes<=cap,"actual source text returned from SQLite is bounded before Java JSON materialization");
        Map<String,Object> report=new LinkedHashMap<>();report.put("case",scenario);report.put("scope","HOST_NATIVE_SQLITE_PRODUCTION_OWNERS_NOT_ANDROID_NOT_DEVICE_NOT_QUALITY");report.put("assertions",assertions);report.put("maxMemory",Runtime.getRuntime().maxMemory());report.put("dynamicBindCap",cap);report.put("maxActualSourcePayloadBytesRead",SQLiteDatabase.maxPreparedPayloadBytes);report.put("transactions",transactions);report.put("maxSqlStatements",maxSql);report.put("maxBindBytes",maxBytes);report.put("maxActualHostTransactionMs",maxMs);report.put("injected",injected);report.put("independentReadbackOpens",SQLiteDatabase.independentOpens);report.put("status",baseline?"BASELINE_OVERSIZE_REPRODUCED":"TARGETED_HOST_ASSERTIONS_COMPLETED");
        try{Files.write(new File(output,scenario+".json").toPath(),(JsonR27.write(report)+"\n").getBytes(StandardCharsets.UTF_8));try(PrintWriter out=new PrintWriter(new File(output,scenario+"-sql.jsonl"),"UTF-8")){for(Map<String,Object> call:sqlCalls)out.println(JsonR27.write(call));}}catch(Exception error){throw new AssertionError(error);}
        System.out.println(JsonR27.write(report));
    }
    public static void main(String[] args)throws Exception{
        scenario=args[0];output=new File(args[1]);baseline=scenario.startsWith("baseline-");hold=scenario.contains("hold");Class.forName("org.sqlite.JDBC");
        File file=File.createTempFile("r31-telemetry-", ".db");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file.getPath())){
            db=new SQLiteDatabase(connection);Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Unsafe unsafe=(Unsafe)unsafeField.get(null);coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);
            Field lockField=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lockField.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lockField.get(null));set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",new android.app.Application(file));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());store=new DbStore(db,coordinator);
            schemas(!scenario.equals("missing-schema"));SQLiteDatabase.observer=observer;
            if(hold||scenario.equals("missing-schema"))holdSamples(65);else if(scenario.contains("byte-budget"))seedTimeline(4,40000);else seedTimeline(64,0);
            if(scenario.equals("hold-prune")){
                long end=System.currentTimeMillis()-8*86400000L;for(int i=0;i<160;i++)db.execSQL("INSERT INTO r30_writer_hold(window_start,window_end,operation,critical,count,hold_max,wait_max,over_budget,over_hard,failures,process_session) VALUES(?,?,'OLD_'||?,0,1,1,1,0,0,0,'OLD_SESSION')",new Object[]{end,end,i});
            }
            if(scenario.equals("timeline-existing-durable")){
                String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_0","draw-hash");tx(actual->{Map<String,Object> row=IssueTimelineWriterR30.readEvent(actual,id);row.put("durable_wall",3000L);row.put("durable_mono",4000L);row.put("durable_boot","ORIGINAL_OBSERVER_BOOT");IssueTimelineWriterR30.saveEvent(actual,row);return 0;});
            }
            if(scenario.contains("byte-budget")){
                String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_0","draw-hash");tx(actual->{Map<String,Object> row=IssueTimelineWriterR30.readEvent(actual,id);char[] large=new char[200000];Arrays.fill(large,'q');row.put("original_opaque_source_field",new String(large));IssueTimelineWriterR30.saveEvent(actual,row);return 0;});
            }
            if(scenario.equals("timeline-concurrent-source")){
                Method prepare=IssueTimelineWriterR30.class.getDeclaredMethod("prepareCommittedR31",SQLiteDatabase.class);prepare.setAccessible(true);setStatic(IssueTimelineWriterR30.class,"preparedCommittedR31",prepare.invoke(null,db));
                concurrent(()->tx(actual->{IssueTimelineWriterR30.markEventAt(actual,"202611040001",IssueTimelineR30.EventRole.LIVE,IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_0",null,null,"draw-hash",IssueTimelineR30.Stage.VALIDATED,3333,4444,"CONCURRENT_ORIGINAL_STAGE");return 0;}));
            }
            if(scenario.contains("concurrent-new"))atFirstWrite=()->concurrent(()->{if(hold)WriterHoldLedgerR30.record("CONCURRENT_NEW_SAMPLE",false,99,101,false);else {String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_63","draw-hash");IssueTimelineWriterR30.committedAt(id,IssueTimelineR30.Stage.NEXT_FROZEN,8888,9999,"CONCURRENT_LATER_OBSERVATION");}});
            if(!hold&&!scenario.equals("missing-schema"))WriterHoldLedgerR30.resetForTest(); // isolate setup-only observations, never discard the missing-schema fixture's65 live samples
            diagnose(true);
            if(!baseline)RuntimeCoordinatorR26.bindBackgroundClaim(new RuntimeCoordinatorR26.Claim("r27-health","host-owner",1,0));
            observe=true;
            if(scenario.equals("missing-schema")){
                holdFlush();need(WriterHoldLedgerR30.snapshot().size()>=65,"missing schema does not rotate away live stats");need(transactions.isEmpty(),"schema missing is dependency before writer admission");need(!HoldLedgerFlushR30.flushStateR31().get("reason").equals("COMMITTED"),"no false completed flush");
            }else if(baseline){
                flush();need(!transactions.isEmpty(),"baseline actual transaction ran");need(transactions.stream().anyMatch(t->number(t,"sqlCount")>64),"baseline count violation reproduced");if(hold)need(transactions.stream().anyMatch(t->number(t,"ddlCount")>0),"baseline business DDL reproduced");else need(timelineDurable()==64,"baseline64 events actually saved");
            }else {
                flush();
                if(scenario.contains("first-sql-failure")){need(injected,"first-body failure executed");need(hold?scalar("SELECT count(*) FROM r30_writer_hold WHERE operation LIKE 'FIXTURE_HOLD_%'")==0:timelineDurable()==0,"failed first transaction rolled back data");}
                if(scenario.equals("timeline-concurrent-source")){need(timelineDurable()==0&&timelinePending()==64,"whole prepared chunk rollback retains all original clocks");}
                SQLiteDatabase.rejectReadback=false;
                for(int n=0;n<40;n++){
                    if(!hold&&timelinePending()==0)break;
                    flush();SQLiteDatabase.rejectReadback=false;
                    if(hold&&holdPending()==0&&!scenario.equals("hold-prune"))break;
                }
                if(hold){verifyHold(65);need(holdPending()==0,"all original closed-window rows confirmed");if(scenario.contains("concurrent-new")){long samples=0;for(WriterHoldLedgerR30.Stat s:WriterHoldLedgerR30.snapshot())if(s.operation.equals("CONCURRENT_NEW_SAMPLE"))samples+=s.count();need(samples==1,"new producer sample stays in next live window");}if(scenario.equals("hold-prune"))need(scalar("SELECT count(*) FROM r30_writer_hold WHERE operation LIKE 'OLD_%'")==0,"bounded keyset retention drains actual expired rows");}
                else if(scenario.contains("byte-budget")){need(timelineDurable()==3&&timelinePending()==1,"oversized original retained as capacity debt, smaller events still progress");need(IssueTimelineWriterR30.commitObservationState().get("flushReason").toString().contains("BUDGET_DEBT"),"explicit debt reason");}
                else {verifyTimeline(64);if(scenario.equals("timeline-concurrent-source")){String id=IssueTimelineWriterR30.eventId("202611040001",IssueTimelineR30.EventKind.DRAW,"ORIGINAL_REQUEST_0","draw-hash");Map<String,Object> row=IssueTimelineWriterR30.readEvent(db,id);need(((Number)row.get("wall_validated")).longValue()==3333,"concurrent original stage survives CAS/rebase");}}
                if(scenario.contains("sql-failure")||scenario.contains("commit-failure")||scenario.contains("ack-failure")||scenario.contains("unknown-then"))need(injected,"requested native fault executed");
                if(scenario.contains("unknown-then")||scenario.contains("ack-failure"))need(SQLiteDatabase.independentOpens>0,"actual independent receipt resolution attempted");
                need(!db.inTransaction(),"actual SQLite transaction exited");
            }
            diagnose(false);RuntimeCoordinatorR26.unbindBackgroundClaimR31();budgetEvidence();
        }catch(Throwable failure){
            Map<String,Object> failed=new LinkedHashMap<>();failed.put("case",scenario);failed.put("status","FAILED_ORIGINAL_ASSERTION");failed.put("failure",failure.toString());failed.put("transactions",transactions);failed.put("sqlCalls",sqlCalls);
            Files.write(new File(output,scenario+"-failure.json").toPath(),(JsonR27.write(failed)+"\n").getBytes(StandardCharsets.UTF_8));throw failure;
        }finally{SQLiteDatabase.observer=null;file.delete();}
    }
    static void setStatic(Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
}
