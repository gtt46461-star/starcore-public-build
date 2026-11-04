package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Native SQLite + actual POSIX/default budgets. Every row is synthetic engineering input, never quality evidence. */
public final class HistoryImportPublicationR31Host {
    static final String NUMBERS="1,2,3,4,5,6,7,8,9,10", REVERSE="10,9,8,7,6,5,4,3,2,1";
    static int checks;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    static long scalar(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static String text(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()?c.getString(0):null;}}
    static String issue(int row){return java.time.LocalDate.of(2020,1,1).plusDays(row/1440).toString().replace("-","")+String.format(Locale.ROOT,"%04d",row%1440+1);}
    static String header(){return "period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time\n";}
    static String row(int ordinal,String numbers,String metadata){return issue(ordinal)+",0,,\""+numbers+"\","+metadata+",HOST,1,0\n";}
    static byte[] source(int rows,boolean conflicts){StringBuilder s=new StringBuilder(header());for(int n=0;n<rows;n++)s.append(row(n,NUMBERS,"HOST"));if(conflicts){s.append(row(5000,NUMBERS,"HOST")).append(row(5001,REVERSE,"HOST"));}return s.toString().getBytes(StandardCharsets.UTF_8);}
    static String importBytes(DbStore store,byte[] bytes)throws Exception{return HistoryImportR27.importCsv(store,new ByteArrayInputStream(bytes));}
    static long published(SQLiteDatabase db){return scalar(db,"SELECT "+HistoryImportR27.publishedCountSqlR31());}
    static long rejects(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count FROM r27_fetch_reject WHERE reason='HISTORY_IMPORT_CONFLICT'",null)){return c.moveToFirst()?c.getLong(0):0;}}
    static long files(File directory){if(!directory.exists())return 0;if(directory.isFile())return 1;long n=0;File[] children=directory.listFiles();if(children==null)throw new AssertionError("unreadable fixture "+directory);for(File child:children)n+=files(child);return n;}
    static void migrate(DbStore store){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_HOST_HISTORY_ADDITIVE_MIGRATION"),db->{HistoryImportR27.ensureR31PublicationSchema(db);ResearchSourceJournalR31.ensureSchemaR31(db);return 0;});}
    static void seedCanonical(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,open_time INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0,draw_hash_v2 TEXT)");
        try{db.jdbc.setAutoCommit(false);try(java.sql.PreparedStatement p=db.jdbc.prepareStatement("INSERT OR IGNORE INTO draws VALUES(?,0,1,2,3,4,5,6,7,8,9,10,0,'HOST_CANONICAL')")){for(int n=5000;n<10000;n++){p.setString(1,issue(n));p.addBatch();}p.executeBatch();}db.jdbc.commit();}catch(java.sql.SQLException failure){throw new IllegalStateException(failure);}finally{try{db.jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new IllegalStateException(failure);}}
    }
    static String canonicalHash(SQLiteDatabase db){StringBuilder s=new StringBuilder();try(Cursor c=db.rawQuery("SELECT * FROM draws ORDER BY issue",null)){while(c.moveToNext())for(int n=0;n<c.getColumnCount();n++)s.append(c.getString(n)).append('|');}return StatsR27.sha256(s.toString());}
    static File dataRoot(File database){return database.getParentFile().getParentFile();}
    static void noOwners(File root)throws Exception{Map<String,Object> budget=new StorageBudgetR31(root).snapshot();check(((List<?>)budget.get("owners")).isEmpty(),"storage owners released only after exit");check(((Number)budget.get("unallocatedReservationBytes")).longValue()==0,"no pending disk promises");}
    static void baselineReader(DbStore store){
        List<ReplayDatasetR27.Row> old=new ArrayList<>();HistoryImportR27.appendOlder(store.readable(),old,issue(5000),10000);check(old.size()==1&&old.get(0).issue.equals(issue(4999)),"legacy evidence readable before startup migration");
    }
    static void seedLegacy(DbStore store){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_HOST_LEGACY_HISTORY_FIXTURE"),db->{db.execSQL("INSERT OR IGNORE INTO r27_history_import VALUES(?,?,?,'IMPORTED',NULL,'PRE_R31_ORIGINAL',7)",new Object[]{issue(4999),TargetIssueResolver.sequenceOrdinalSU1(issue(4999)),NUMBERS});return 0;});}
    static void suite(DbStore store,File database)throws Exception{
        SQLiteDatabase db=store.readable();File root=dataRoot(database);String original=canonicalHash(db);byte[] large=source(4000,true);
        baselineReader(store);migrate(store);migrate(store);check(HistoryImportR27.publicationSchemaReadyReadOnlyR31(db),"exact history tables/indexes/triggers ready");
        AtomicInteger stagedObservations=new AtomicInteger();SQLiteDatabase.traceHistoryR31=true;
        SQLiteDatabase.afterCommitR31=()->{if(scalar(db,"SELECT count(*) FROM r31_history_import_run WHERE status='STAGING' AND cursor_line>0")>0){check(published(db)==1,"partial stage must not leak to scalar/reader");stagedObservations.incrementAndGet();}};
        String message;try{message=importBytes(store,large);}finally{SQLiteDatabase.afterCommitR31=null;}
        check(stagedObservations.get()>100,"many actual chunk commits observed before publication");check(message.contains("比对一致 1")&&message.contains("冲突 1")&&message.contains("新增回放专用 4000"),"same legal classification");
        check(published(db)==4003,"published complete rows plus untouched legacy");check(scalar(db,"SELECT count(*) FROM r31_history_import_row")==4002,"all legal rows retained");
        check(scalar(db,"SELECT count(*) FROM r31_source_event WHERE source_name='CSV_IMPORT'")==1,"one actual input INSERT event");check(scalar(db,"SELECT count(*) FROM r31_source_member")==4000,"precise imported members exclude canonical rows");
        check(scalar(db,"SELECT count(*) FROM r27_history_import")==1,"legacy source evidence not rewritten");check(rejects(db)==1,"conflict evidence exactly once");
        long fence=ResearchSourceJournalR31.currentFence(db),sourceFiles=files(new File(root,"files/r31-history-inputs"));String replayMessage=importBytes(store,large);
        check(message.equals(replayMessage),"same manifest resumes same completed result");check(rejects(db)==1,"same CSV cannot repeat conflict counter");check(ResearchSourceJournalR31.currentFence(db)==fence,"same CSV cannot repeat source event");
        check(scalar(db,"SELECT count(*) FROM r31_history_import_run")==1,"one durable task per source ABI");check(files(new File(root,"files/r31-history-inputs"))==sourceFiles&&sourceFiles==1,"unreferenced duplicate snapshot really deleted");
        byte[] same=(header()+row(2,NUMBERS,"DIFFERENT_METADATA")).getBytes(StandardCharsets.UTF_8);importBytes(store,same);check(ResearchSourceJournalR31.currentFence(db)==fence,"unchanged effective values do not invalidate inputs");
        byte[] replacement=(header()+row(0,REVERSE,"NEW_VALUES")).getBytes(StandardCharsets.UTF_8);importBytes(store,replacement);
        check(REVERSE.equals(text(db,"SELECT numbers FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(0))),"latest complete issue replacement consumed");
        check(NUMBERS.equals(text(db,"SELECT numbers FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(1))),"missing issues in CSV preserve prior complete source");
        check(scalar(db,"SELECT count(*) FROM r31_source_event WHERE operation='IMPORT_REPLACE'")==1,"replacement event is distinct from insertion");
        check(scalar(db,"SELECT count(*) FROM r31_source_member m JOIN r31_source_event e ON e.member_set=m.member_set WHERE e.operation='IMPORT_REPLACE'")==1,"replacement has one precise changed ordinal");
        long priorFence=ResearchSourceJournalR31.currentFence(db),priorRejects=rejects(db);
        byte[] self=(header()+row(1,NUMBERS,"SELF")+row(1,REVERSE,"SELF")).getBytes(StandardCharsets.UTF_8);String selfMessage=importBytes(store,self);importBytes(store,self);
        check(selfMessage.contains("自相矛盾 1"),"self conflict remains visible");check(rejects(db)==priorRejects+1,"self conflict counted once across replay");check(ResearchSourceJournalR31.currentFence(db)==priorFence,"self conflict cannot invalidate unchanged effective input");
        check(NUMBERS.equals(text(db,"SELECT numbers FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(1))),"self conflicting issue cannot shadow valid earlier evidence");
        List<ReplayDatasetR27.Row> older=new ArrayList<>();HistoryImportR27.appendOlder(db,older,issue(5000),10000);check(older.size()==4001,"replay consumes complete imported older rows only");
        check(older.stream().noneMatch(r->r.issue.compareTo(issue(5000))>=0),"canonical overlap never enters imported replay role");
        check(scalar(db,"SELECT count(*) FROM draws")==5000&&original.equals(canonicalHash(db)),"all canonical 5000 rows and values preserved");
        check(SQLiteDatabase.historyDdlR31==0,"zero business runtime DDL");check(SQLiteDatabase.maxHistoryOpsR31<=65,"physical top-level chunk ops bounded including receipt");
        Map<?,?> inventory=(Map<?,?>)ResearchWriteQueueR27.batchInventoryR31().get("HISTORY_IMPORT_R31");check(inventory!=null&&((Number)inventory.get("maxOps")).longValue()<=64,"prepared producer remains <=64 ops");check(((Number)inventory.get("overInitialBudget")).longValue()==0,"all prepared binds fit real cap");
        noOwners(root);
        System.out.println("HISTORY_NEW_NATIVE|checks="+checks+"|maxSqlOpsInTx="+SQLiteDatabase.maxHistoryOpsR31+"|maxNativeTxMs="+SQLiteDatabase.maxHistoryNanosR31/1e6+"|runtimeDdl="+SQLiteDatabase.historyDdlR31+"|stagedCommitsObserved="+stagedObservations+"|canonicalRows=5000|historyInputs=REPLAY_ONLY|qualityEvidence=NONE|PASS");
    }
    static void rollbackAndCancel(DbStore store,File database)throws Exception{
        SQLiteDatabase db=store.readable();File root=dataRoot(database);migrate(store);byte[] csv=source(128,true);SQLiteDatabase.traceHistoryR31=true;
        AtomicBoolean injected=new AtomicBoolean();db.beforeSuccessful=()->{if(!injected.get()&&scalar(db,"SELECT COALESCE(MAX(cursor_line),0) FROM r31_history_import_run")>=16){injected.set(true);throw new IllegalStateException("HOST_FAIL_BEFORE_TRUE_COMMIT");}};
        try{importBytes(store,csv);throw new AssertionError("failed chunk accepted");}catch(RuntimeException expected){check(injected.get(),"real precommit fault reached");}finally{db.beforeSuccessful=null;}
        check(scalar(db,"SELECT COALESCE(MAX(cursor_line),0) FROM r31_history_import_run")==0,"rollback cursor did not advance");check(scalar(db,"SELECT count(*) FROM r31_history_import_row")==0,"rollback rows absent");check(published(db)==1,"rollback not published");
        AtomicBoolean cancel=new AtomicBoolean();SQLiteDatabase.afterCommitR31=()->{if(scalar(db,"SELECT COALESCE(MAX(cursor_line),0) FROM r31_history_import_run")>=16)cancel.set(true);};
        try{HistoryImportR27.importCsv(store,new ByteArrayInputStream(csv),cancel::get);throw new AssertionError("cancel ignored");}catch(InterruptedException expected){check(cancel.get(),"cancel returned only after actual work exited");}finally{SQLiteDatabase.afterCommitR31=null;}
        check(scalar(db,"SELECT cursor_line FROM r31_history_import_run")==16,"cancel retains last committed cursor");check(scalar(db,"SELECT count(*) FROM r31_history_import_row")==16,"cancel retains bounded staged input");check(published(db)==1,"cancel stage invisible");check(files(new File(root,"files/r31-history-inputs"))==1,"one original input remains referenced for recovery");noOwners(root);
        String jobId=text(db,"SELECT job_id FROM r31_history_import_run");java.lang.reflect.Method get=HistoryImportR27.class.getDeclaredMethod("job",SQLiteDatabase.class,String.class);get.setAccessible(true);Object old=get.invoke(null,db,jobId);
        check(jobId.equals(HistoryImportR27.pendingJobIdR31(store)),"persisted recovery input discoverable without original URI");
        File original=new File(root,text(db,"SELECT source_path FROM r31_history_import_run"));int first;
        try(RandomAccessFile corrupt=new RandomAccessFile(original,"rw")){first=corrupt.read();corrupt.seek(0);corrupt.write('X');}
        try{HistoryImportR27.resumeSavedR31(store,jobId,()->false);throw new AssertionError("damaged saved input became a new source task");}catch(IllegalStateException integrity){check(integrity instanceof FailureDispositionR31.ClassifiedFailure&&"INTEGRITY".equals(((FailureDispositionR31.ClassifiedFailure)integrity).failureCategory()),"saved-input hash mismatch is actual typed integrity failure");}
        check(scalar(db,"SELECT count(*) FROM r31_history_import_run")==1&&scalar(db,"SELECT cursor_line FROM r31_history_import_run")==16,"damaged input cannot create another task or advance cursor");
        try(RandomAccessFile restoreFixture=new RandomAccessFile(original,"rw")){restoreFixture.write(first);} // Explicit engineering corruption injection/restore only.
        HistoryDiagnosticFaultR31Host.install();try{HistoryImportR27.resumeSavedR31(store,jobId,()->false);}finally{HistoryDiagnosticFaultR31Host.clear();}
        check(HistoryDiagnosticFaultR31Host.count.get()>5,"real after-COMMIT diagnostics failed repeatedly");check(rejects(db)==1,"postcommit callback cannot repeat settlement/counter");check(published(db)==131,"resumed source complete once");
        check(scalar(db,"SELECT count(*) FROM r31_history_import_run")==1&&scalar(db,"SELECT count(*) FROM r31_history_import_row")==130,"reentry never duplicates task or rows");
        java.lang.reflect.Method pulse=HistoryImportR27.class.getDeclaredMethod("pulse",DbStore.class,old.getClass());pulse.setAccessible(true);
        try{pulse.invoke(null,store,old);throw new AssertionError("old owner wrote after successor");}catch(java.lang.reflect.InvocationTargetException stale){check(stale.getCause() instanceof DatabaseCoordinator.WriteRejectedR31,"old owner rejected by real coordinator guard");}
        check(files(new File(root,"files/r31-history-inputs"))==1,"resume reuses original source; temporary copy released");noOwners(root);
        System.out.println("HISTORY_ROLLBACK_CANCEL_POSTCOMMIT|checks="+checks+"|originalData=RETAINED|partialPublication=NONE|diagnosticFaults="+HistoryDiagnosticFaultR31Host.count+"|qualityEvidence=NONE|PASS");
    }
    static void stockUnknown(DbStore store,File root)throws Exception{
        migrate(store);AtomicInteger read=new AtomicInteger();InputStream unused=new InputStream(){public int read(){read.incrementAndGet();return -1;}};
        try{HistoryImportR27.importCsv(store,unused);throw new AssertionError("stock HOST UNKNOWN admitted");}catch(StorageBudgetR31.Rejected expected){check("ALLOCATED_BYTES_UNKNOWN".equals(expected.reason),"unknown allocation denies default admission");}
        check(read.get()==0&&scalar(store.readable(),"SELECT count(*) FROM r31_history_import_run")==0,"admission happens before input materialization/job");
        System.out.println("HISTORY_STOCK_HOST_DEFAULT_ADMISSION|ALLOCATED_BYTES_UNKNOWN|inputReads=0|androidBoundary=NOT_RUN|PASS");
    }
    static ReplayDatasetR27 pin(DbStore store,int ordinal)throws Exception{
        int[] numbers=new int[10];String[] parts=text(store.readable(),"SELECT numbers FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(ordinal)).split(",");for(int n=0;n<10;n++)numbers[n]=Integer.parseInt(parts[n]);
        ReplayDatasetR27 data=ReplayDatasetR27.of(Collections.singletonList(new ReplayDatasetR27.Row(issue(ordinal),TargetIssueResolver.sequenceOrdinalSU1(issue(ordinal)),numbers)));
        long revision=SchemaR27.liveSourceRevision(store.readable());ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(b,data,revision,System.currentTimeMillis());
        ResearchWriteQueueR27.ImmutableBatch prepared=b.prepareR31("HISTORY_NATIVE_PIN",StatsR27.sha256("HOST_PIN|"+data.datasetHash+'|'+revision),"HISTORY_NATIVE_PIN","HOST_NATIVE_PIN",1,data.datasetHash,0,"R31_HISTORY_PIN_FIXTURE_V1");
        ResearchWriteQueueR27 queue=new ResearchWriteQueueR27(store);while(!queue.submit("HISTORY_NATIVE_PIN",prepared,()->false))Thread.sleep(25);
        check(!ResearchInputPinR30R1.invalid(store.readable(),data.datasetHash),"actual original input pinned before source change");return data;
    }
    static void sourceMembersAndGuards(DbStore store,File database)throws Exception{
        migrate(store);SQLiteDatabase db=store.readable();String initial=header()+row(10,NUMBERS,"FIRST")+row(12,NUMBERS,"FIRST")+row(20,NUMBERS,"FIRST");importBytes(store,initial.getBytes(StandardCharsets.UTF_8));
        ReplayDatasetR27 hole=pin(store,12),touched=pin(store,10);long fence=ResearchSourceJournalR31.currentFence(db);
        String changed=header()+row(10,REVERSE,"SECOND")+row(12,NUMBERS,"SECOND")+row(20,REVERSE,"SECOND");importBytes(store,changed.getBytes(StandardCharsets.UTF_8));
        check(!ResearchInputPinR30R1.invalid(db,hole.datasetHash),"unchanged hole inside range envelope remains valid");check(ResearchInputPinR30R1.invalid(db,touched.datasetHash),"actual changed imported input invalidates prior captured pin");
        check(scalar(db,"SELECT count(*) FROM r31_source_member m JOIN r31_source_event e ON e.member_set=m.member_set WHERE e.event_seq>? AND e.source_name='CSV_IMPORT'",String.valueOf(fence))==2,"only two actual changed ordinals published, not entire envelope");
        long beforeShort=ResearchSourceJournalR31.currentFence(db);String shortWindow=header()+row(9000,NUMBERS,"ONLY_SHORT_READER_HOLE");
        // Remove only a synthetic canonical fixture row through the real writer; the CSV fills that hole.
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_SHORT_READER_FIXTURE"),tx->{tx.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{issue(9000)});return 0;});
        importBytes(store,shortWindow.getBytes(StandardCharsets.UTF_8));
        ReplayDatasetR27 shortData=StoreR27.loadDataset(db,544);check(Arrays.asList(shortData.issues).contains(issue(9000)),"real 544-row consumer uses CSV hole below its own selected cutoff");
        check(scalar(db,"SELECT count(*) FROM r31_source_member m JOIN r31_source_event e ON e.member_set=m.member_set WHERE e.event_seq>? AND e.source_name='CSV_IMPORT' AND m.ordinal=?",String.valueOf(beforeShort),String.valueOf(TargetIssueResolver.sequenceOrdinalSU1(issue(9000))))==1,"short-reader source change cannot be missed by global minimum cutoff");
        String id=text(db,"SELECT job_id FROM r31_history_import_run WHERE status='COMPLETE' ORDER BY publish_seq LIMIT 1");long count=published(db);
        for(String sql:new String[]{"UPDATE r31_history_import_run SET row_count=row_count+1 WHERE job_id=?","DELETE FROM r31_history_import_run WHERE job_id=?","UPDATE r31_history_import_row SET numbers='10,9,8,7,6,5,4,3,2,1' WHERE job_id=?","DELETE FROM r31_history_import_row WHERE job_id=?"}){
            try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_HOST_IMMUTABILITY_NEGATIVE"),tx->{tx.execSQL(sql,new Object[]{id});return 0;});throw new AssertionError("immutable published evidence mutated");}catch(DatabaseCoordinator.WriteRejectedR31 expected){check(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"published run/row immutable with true rollback");}
        }
        check(published(db)==count,"failed evidence edits have no publication effect");
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_HOST_EXACT_SCHEMA_NEGATIVE"),tx->{tx.execSQL("DROP INDEX r31_history_import_order");return 0;});
        check(!HistoryImportR27.publicationSchemaReadyReadOnlyR31(db),"missing exact index detected read-only");AtomicInteger reads=new AtomicInteger();
        try{HistoryImportR27.importCsv(store,new InputStream(){public int read(){reads.incrementAndGet();return -1;}});throw new AssertionError("schema missing silently repaired in business import");}catch(IllegalStateException expected){check(String.valueOf(expected.getMessage()).contains("SCHEMA_PENDING"),"missing schema is dependency, no business DDL");}
        check(reads.get()==0,"schema dependency checked before materialization");
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_HOST_EXACT_OWNER_REPAIR"),tx->{SchemaR27.ensure(tx);return 0;});
        check(HistoryImportR27.publicationSchemaReadyReadOnlyR31(db)&&published(db)==count,"actual startup owner restores only missing schema, preserves published evidence");noOwners(dataRoot(database));
        System.out.println("HISTORY_SOURCE_MEMBERS_AND_GUARDS|checks="+checks+"|preciseHolePin=VALID|changedPin=INVALID|actual544Reader=CONSUMED|schemaMissing=DEPENDENCY_NO_INPUT_READ|PASS");
    }
    static void streamingFormat(DbStore store,File database)throws Exception{
        migrate(store);String prefix=header()+row(30,NUMBERS,"DUP")+row(30,NUMBERS,"DUP")+row(30,REVERSE,"DUP")+issue(31)+",0,,\""+NUMBERS+"\",";
        String longNumbers=new String(new char[260]).replace('\0','1');
        String tail=",HOST,1,0\n"+row(32,"1,1,3,4,5,6,7,8,9,10","ILLEGAL")+"202013010001,0,,\""+NUMBERS+"\",BAD_ISSUE,HOST,1,0\n"+row(33,longNumbers,"BOUNDED_SELECTED_FIELD")+row(34,NUMBERS,"VALID_FLAG_ZERO").replace(",HOST,1,0",",HOST,0,0");
        InputStream metadata=new InputStream(){long remaining=2L<<20;public int read(){if(remaining==0)return -1;remaining--;return 'x';}public int read(byte[] bytes,int off,int len){if(remaining==0)return -1;int n=(int)Math.min(len,remaining);Arrays.fill(bytes,off,off+n,(byte)'x');remaining-=n;return n;}};
        InputStream csv=new SequenceInputStream(Collections.enumeration(Arrays.asList(new ByteArrayInputStream(prefix.getBytes(StandardCharsets.UTF_8)),metadata,new ByteArrayInputStream(tail.getBytes(StandardCharsets.UTF_8)))));
        String result;try(InputStream input=csv){result=HistoryImportR27.importCsv(store,input);}
        SQLiteDatabase db=store.readable();check(result.contains("非法行 3")&&result.contains("重复期 2")&&result.contains("自相矛盾 1"),"bounded streaming preserves legality/duplicate counters");
        check(scalar(db,"SELECT row_count FROM r31_history_import_run")==1&&scalar(db,"SELECT source_bytes FROM r31_history_import_run")>(2L<<20),"full long metadata retained on disk, not truncated to fit parser");
        check(scalar(db,"SELECT count(*) FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(30))==0,"self contradicting issue never published");check(scalar(db,"SELECT count(*) FROM "+HistoryImportR27.publishedRowsSqlR31()+" WHERE issue=?",issue(31))==1,"legal long-metadata record actually published");
        check(scalar(db,"SELECT count(*) FROM r31_source_member")==1,"only legal effective input generates source membership");check(rejects(db)==1,"one self conflict diagnostic");noOwners(dataRoot(database));
        System.out.println("HISTORY_STREAMING_FORMAT|checks="+checks+"|ignoredMetadataBytes=2097152|selectedFieldBound=128|invalidRows=3|duplicates=2|selfConflicts=1|fullSourceRetained=true|PASS");
    }
    static void finalSourceRace(DbStore store,File database)throws Exception{
        migrate(store);SQLiteDatabase db=store.readable();SQLiteDatabase.traceHistoryR31=true;AtomicInteger phase=new AtomicInteger(),finished=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();List<Thread> writers=new java.util.concurrent.CopyOnWriteArrayList<>();
        java.lang.reflect.Field field=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");field.setAccessible(true);java.util.concurrent.locks.ReentrantLock writerLock=(java.util.concurrent.locks.ReentrantLock)field.get(null);
        java.util.function.IntConsumer launch=n->{Thread writer=new Thread(()->{try{
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_REAL_SOURCE_RACE_"+n),tx->{
                if(n==3)tx.execSQL("UPDATE draws SET open_time=open_time+1 WHERE issue=?",new Object[]{issue(9999)});
                else tx.execSQL(n==1?"UPDATE draws SET n1=10,n2=9,n3=8,n4=7,n5=6,n6=5,n7=4,n8=3,n9=2,n10=1 WHERE issue=?":"UPDATE draws SET n1=1,n2=2,n3=3,n4=4,n5=5,n6=6,n7=7,n8=8,n9=9,n10=10 WHERE issue=?",new Object[]{issue(5000)});return 0;
            });finished.incrementAndGet();
        }catch(Throwable problem){failure.set(problem);}},"history-real-critical-source-writer-"+n);writers.add(writer);writer.start();};
        SQLiteDatabase.afterCommitR31=()->{
            if(scalar(db,"SELECT count(*) FROM r31_history_import_run WHERE status='STAGING' AND cursor_line=34")==0)return;
            if(phase.get()==0&&phase.compareAndSet(0,1))launch.accept(1);
            else if(phase.get()==1&&scalar(db,"SELECT count(*) FROM r31_history_import_row WHERE issue='"+issue(5000)+"' AND status='CONFLICT'")>0&&scalar(db,"SELECT count(*) FROM r31_source_member")>0&&phase.compareAndSet(1,2))launch.accept(2);
            else if(phase.get()==2&&scalar(db,"SELECT count(DISTINCT member_set) FROM r31_source_member")>=2&&phase.compareAndSet(2,3))launch.accept(3);
        };
        java.util.function.BooleanSupplier cancelled=()->{
            if(!writerLock.isHeldByCurrentThread()){
                long until=System.nanoTime()+5_000_000_000L;int target=phase.get();
                while(finished.get()<target&&failure.get()==null){if(System.nanoTime()>until)throw new IllegalStateException("REAL_SOURCE_WRITER_TIMEOUT");try{Thread.sleep(1);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();return true;}}
                if(failure.get()!=null)throw new IllegalStateException("REAL_SOURCE_WRITER_FAILED",failure.get());
            }
            return false;
        };
        String result;try{result=HistoryImportR27.importCsv(store,new ByteArrayInputStream(source(32,true)),cancelled);}finally{SQLiteDatabase.afterCommitR31=null;for(Thread writer:writers)writer.join(10000);}
        check(phase.get()==3&&finished.get()==3&&failure.get()==null,"three independent production critical writers really committed phase="+phase+" finished="+finished+" failure="+failure.get());
        check("MATCH".equals(text(db,"SELECT status FROM r31_history_import_row WHERE issue=?",issue(5000))),"final canonical classification replaced the stale staged status");
        check(result.contains("比对一致 1")&&result.contains("冲突 1")&&rejects(db)==1,"manifest/counter agree with final actual canonical values");
        check(scalar(db,"SELECT count(*) FROM r31_source_event WHERE source_name='draws' AND operation='UPDATE'")==3,"source changes are real committed trigger events");
        check(scalar(db,"SELECT count(*) FROM r31_source_event WHERE source_name='CSV_IMPORT'")==1&&scalar(db,"SELECT count(*) FROM r31_source_member m JOIN r31_source_event e ON e.member_set=m.member_set WHERE e.source_name='CSV_IMPORT'")==32,"only final exact member set published after relevant retry/unrelated rebase");
        check(scalar(db,"SELECT count(*) FROM draws")==5000,"race fixture preserves canonical rolling row count");noOwners(dataRoot(database));
        System.out.println("HISTORY_REAL_SOURCE_RACE|checks="+checks+"|stageThenCanonicalCorrection=RECLASSIFIED|prepareThenCorrection=RETRIED|unrelatedCorrection=REBASED|criticalWriterCommits=3|PASS");
    }
    static void ioExitAndDebt(DbStore store,File database)throws Exception{
        migrate(store);File root=dataRoot(database);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean cancelled=new AtomicBoolean();AtomicReference<Throwable> outcome=new AtomicReference<>();
        InputStream blocked=new InputStream(){public int read(){throw new AssertionError("bulk read required");}public int read(byte[] bytes,int off,int len)throws IOException{entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}bytes[off]='x';return 1;}};
        Thread thread=new Thread(()->{try{HistoryImportR27.importCsv(store,blocked,cancelled::get);outcome.set(new AssertionError("cancel ignored"));}catch(Throwable expected){outcome.set(expected);}},"history-host-actual-blocked-io");thread.start();
        check(entered.await(10,java.util.concurrent.TimeUnit.SECONDS),"worker actually entered blocking input");cancelled.set(true);
        Map<String,Object> active=new StorageBudgetR31(root).snapshot();check(!((List<?>)active.get("owners")).isEmpty()&&thread.isAlive(),"cancel request does not release active IO reservation");
        release.countDown();thread.join(10000);check(!thread.isAlive()&&outcome.get() instanceof InterruptedException,"real IO exit precedes cancellation result");check(files(new File(root,"files/r31-history-inputs"))==0,"failed unreferenced source actually removed");noOwners(root);
        File debt=new File(root,"files/retained-evidence-debt.bin");try(RandomAccessFile file=new RandomAccessFile(debt,"rw")){file.setLength(2049L*1024*1024);file.seek(file.length()-1);file.write(1);}
        long allocated=HistoryPosixStatR31Host.allocated(debt);check(allocated>=0&&allocated<debt.length(),"sparse logical and kernel-allocated metrics remain distinct");AtomicInteger reads=new AtomicInteger();
        try{HistoryImportR27.importCsv(store,new InputStream(){public int read(){reads.incrementAndGet();return -1;}});throw new AssertionError("protected capacity debt admitted");}catch(StorageBudgetR31.Rejected expected){check("BLOCKED_PROTECTED_DEBT".equals(expected.reason),"default total capacity debt refuses new low priority input");}
        check(reads.get()==0&&debt.exists(),"rejected import neither reads input nor deletes protected evidence");
        System.out.println("HISTORY_DEFAULT_CAPACITY_DEBT|logicalBytes="+debt.length()+"|actualKernelAllocatedBytes="+allocated+"|freeSpaceExhaustion=NOT_TESTED|PASS");check(debt.delete(),"fixture debt removed by its explicit owner");noOwners(root);
        System.out.println("HISTORY_ACTUAL_EXIT_AND_RELEASE|checks="+checks+"|cancelledActiveReservation=HELD_UNTIL_IO_EXIT|unreferencedSourceFiles=0|PASS");
    }
    static void crashOrResume(DbStore store,File database,String mode)throws Exception{
        migrate(store);SQLiteDatabase db=store.readable();byte[] bytes=source(64,true);SQLiteDatabase.traceHistoryR31=true;
        if(mode.equals("crash-chunk"))SQLiteDatabase.afterCommitR31=()->{if(scalar(db,"SELECT COALESCE(MAX(cursor_line),0) FROM r31_history_import_run")>=16){System.out.println("HISTORY_TRUE_COMMIT_THEN_PROCESS_HALT|phase=STAGED_CHUNK|pid="+ProcessHandle.current().pid()+"|published="+published(db));System.out.flush();Runtime.getRuntime().halt(73);}};
        if(mode.equals("crash-members"))SQLiteDatabase.afterCommitR31=()->{if(scalar(db,"SELECT count(*) FROM r31_source_member")>0){System.out.println("HISTORY_TRUE_COMMIT_THEN_PROCESS_HALT|phase=PREPARED_MEMBERS|pid="+ProcessHandle.current().pid()+"|published="+published(db));System.out.flush();Runtime.getRuntime().halt(73);}};
        if(mode.equals("crash-publish"))SQLiteDatabase.afterCommitR31=()->{if(scalar(db,"SELECT count(*) FROM r31_history_import_run WHERE status='COMPLETE'")>0){System.out.println("HISTORY_TRUE_COMMIT_THEN_PROCESS_HALT|phase=FINAL_PUBLICATION|pid="+ProcessHandle.current().pid()+"|published="+published(db)+"|rejects="+rejects(db));System.out.flush();Runtime.getRuntime().halt(73);}};
        if(mode.equals("lease-pending")||mode.equals("member-lease-pending")){
            try{importBytes(store,bytes);throw new AssertionError("live lease silently stolen");}catch(DatabaseCoordinator.WriteRejectedR31 expected){check(String.valueOf(expected.getCause()).contains("OWNER_DRAINING"),"old process lease must really expire");}
            check(published(db)==1,"killed stage remains unpublished");System.out.println("HISTORY_DEAD_PROCESS_LEASE_PENDING|now="+android.os.SystemClock.elapsedRealtime()+"|until="+scalar(db,"SELECT lease_elapsed_ms FROM r31_history_import_run")+"|PASS");return;
        }
        String existing=text(db,"SELECT job_id FROM r31_history_import_run LIMIT 1");
        if(mode.startsWith("resume-")&&existing!=null)HistoryImportR27.resumeSavedR31(store,existing,()->false);else importBytes(store,bytes);
        importBytes(store,bytes);check(published(db)==67&&rejects(db)==1,"actual process reentry publishes/counters once");check(scalar(db,"SELECT count(*) FROM r31_history_import_run")==1,"process reentry same job");check(files(new File(dataRoot(database),"files/r31-history-inputs"))==1,"original source retained once across crash/reentry");noOwners(dataRoot(database));
        System.out.println("HISTORY_ACTUAL_PROCESS_REENTRY|phase="+mode+"|checks="+checks+"|published="+published(db)+"|rejects="+rejects(db)+"|PASS");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File database=new File(args[1]).getCanonicalFile();database.getParentFile().mkdirs();File root=dataRoot(database);new File(root,"files").mkdirs();
        if(!args[0].equals("stock-unknown"))HistoryPosixStatR31Host.install(args[2]);
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+database)){
            SQLiteDatabase db=new SQLiteDatabase(connection);seedCanonical(db);
            try(R31OwnedRuntimeFixture owner=new R31OwnedRuntimeFixture(db,"HOST_HISTORY_PUBLICATION")){
                DbStore store=new DbStore(owner.coordinator,db,new android.app.Application(database));seedLegacy(store);
                switch(args[0]){
                    case "stock-unknown":stockUnknown(store,root);break;
                    case "suite":suite(store,database);break;
                    case "rollback-cancel":rollbackAndCancel(store,database);break;
                    case "io-debt":ioExitAndDebt(store,database);break;
                    case "source-members":sourceMembersAndGuards(store,database);break;
                    case "source-race":finalSourceRace(store,database);break;
                    case "streaming-format":streamingFormat(store,database);break;
                    default:crashOrResume(store,database,args[0]);break;
                }
            }
        }
    }
}
