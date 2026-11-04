package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.GenesisSchemaRegistryR915;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.sqlite.SQLiteConnection;
import sun.misc.Unsafe;

/** Android/JDBC platform adaptation only; every publication/fence/receipt/score is production code. */
public final class R31AtomPublicationHost implements AutoCloseable {
    static int assertions;
    static void need(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static final Unsafe U;
    static {try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void set(Object o,String field,Object value)throws Exception{Field f=o.getClass().getDeclaredField(field);f.setAccessible(true);f.set(o,value);}
    final File databaseFile,output;final Connection connection;final SQLiteDatabase db;final DatabaseCoordinator coordinator;final DbStore store;final ResearchPoolR27 pool;final ResearchPoolR27.Worker worker;
    RuntimeR27 runtime;ReplayDatasetR27 data;SplitR27 split;
    final String scenario;final List<Map<String,Object>> transactions=new ArrayList<>(),calls=new ArrayList<>(),rejections=new ArrayList<>();
    Map<String,Object> transaction,lastCall,lastEnded;boolean observing,injected,diagnosticFault;int stageCalls,failStage=-1,haltStage=-1,haltInBody=-1,afterCommitStage=-1,everyStageCalls;String faultKind="";long totalChangesAtBegin;Runnable afterStage,everyStage;Throwable hookFailure;
    final Map<String,long[]> priorWriterStats=new HashMap<>();
    String currentOwner="host-atom-owner";long currentFence=1;
    final SQLiteDatabase.Observer observer=new SQLiteDatabase.Observer(){
        public void begin(SQLiteDatabase actual){if(actual!=db||!observing)return;transaction=new LinkedHashMap<>();transaction.put("sequence",transactions.size()+1);transaction.put("sqlCount",0L);transaction.put("bindBytes",0L);transaction.put("ddlCount",0L);transaction.put("stageGuardExecutions",0L);transaction.put("triggerGuardUpperBound",0L);transaction.put("primaryStageAttempt",false);totalChangesAtBegin=totalChanges();transactions.add(transaction);}
        public void sql(SQLiteDatabase actual,String sql,Object[] args,boolean inTransaction){
            if(actual!=db||!observing)return;
            Map<String,Object> row=new LinkedHashMap<>();row.put("transaction",inTransaction&&transaction!=null?transaction.get("sequence"):null);row.put("sql",sql);row.put("bindBytes",bindBytes(args));row.put("argumentCount",args==null?0:args.length);row.put("thread",Thread.currentThread().getName());calls.add(row);lastCall=row;
            if(inTransaction&&transaction!=null){add(transaction,"sqlCount",1);add(transaction,"bindBytes",bindBytes(args));if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP|VACUUM|REINDEX|ATTACH|DETACH)\\b.*"))add(transaction,"ddlCount",1);
                if(sql.matches("(?is)^INSERT OR IGNORE INTO r31_(atom_score_stage|atom_contribution_stage|molecule_stage|atom_result_chunk).*")){add(transaction,"stageGuardExecutions",1);add(transaction,"triggerGuardUpperBound",1);}
                if(sql.startsWith("UPDATE r31_atom_result_run SET state=")||sql.startsWith("INSERT INTO r31_atom_result_active")||sql.startsWith("UPDATE r31_atom_result_active SET"))add(transaction,"triggerGuardUpperBound",1);
            }
            if(sql.startsWith("INSERT OR IGNORE INTO r31_atom_score_stage")||sql.startsWith("INSERT OR IGNORE INTO r31_molecule_stage")){
                if(transaction!=null)transaction.put("primaryStageAttempt",true);
                stageCalls++;
                if(!injected&&stageCalls==failStage){injected=true;if(faultKind.equals("INTERRUPT_BODY"))Thread.currentThread().interrupt();else throw new SQLiteException("HOST_EXPLICIT_ATOM_STAGE_SQL_FAILURE");}
                if(!injected&&stageCalls==afterCommitStage){injected=true;if(faultKind.equals("BEFORE_COMMIT"))db.failBeforeCommitOnce=true;else db.failAfterCommitOnce=true;if(faultKind.equals("UNKNOWN"))SQLiteDatabase.rejectReadback=true;}
            }
        }
        public void executed(SQLiteDatabase actual,String sql,Object[] args,long changed,long total){if(actual==db&&observing&&lastCall!=null&&Objects.equals(lastCall.get("sql"),sql)){lastCall.put("nativeChanges",changed);lastCall.put("nativeTotalChanges",total);if(sql.startsWith("INSERT OR IGNORE INTO r31_atom_contribution_stage")&&stageCalls==haltInBody){writeEvidence("INTERRUPTED_PROCESS_IN_NATIVE_UNCOMMITTED_TRANSACTION");System.out.flush();System.err.flush();Runtime.getRuntime().halt(74);}}}
        public void end(SQLiteDatabase actual,boolean committed,long elapsed){
            if(actual!=db||!observing||transaction==null)return;
            transaction.put("committed",committed);transaction.put("actualTransactionElapsedNanos",elapsed);transaction.put("nativeTotalChangesIncludingTriggersAndRolledBackAttempts",totalChanges()-totalChangesAtBegin);lastEnded=transaction;transaction=null;
        }
    };
    static void add(Map<String,Object> map,String key,long n){map.put(key,((Number)map.get(key)).longValue()+n);}
    static long bindBytes(Object[] args){long n=0;if(args!=null)for(Object v:args)n+=v instanceof String?((String)v).getBytes(StandardCharsets.UTF_8).length:v instanceof byte[]?((byte[])v).length:8;return n;}
    long totalChanges(){try{return ((SQLiteConnection)connection).getDatabase().total_changes();}catch(SQLException e){throw new IllegalStateException(e);}}
    long count(String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    String text(String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    void raw(String sql,Object...args){boolean prior=observing;observing=false;try{db.execSQL(sql,args);}finally{observing=prior;}}
    R31AtomPublicationHost(String scenario,File output,File persistent,boolean create)throws Exception{
        this.scenario=scenario;this.output=output;databaseFile=persistent==null?File.createTempFile("r31-atoms-", ".db"):persistent;
        Class.forName("org.sqlite.JDBC");connection=DriverManager.getConnection("jdbc:sqlite:"+databaseFile);db=new SQLiteDatabase(connection);
        coordinator=(DatabaseCoordinator)U.allocateInstance(DatabaseCoordinator.class);Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);set(coordinator,"writeLock",lock.get(null));set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",new android.app.Application(databaseFile));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());store=new DbStore(db,coordinator);
        if(create)schemas();
        pool=(ResearchPoolR27)U.allocateInstance(ResearchPoolR27.class);set(pool,"store",store);set(pool,"session",currentOwner);set(pool,"writes",new ResearchWriteQueueR27(store));set(pool,"revokedTaskFencesR31",new java.util.concurrent.ConcurrentHashMap<String,Long>());worker=pool.new Worker(db,"host-actual-atoms");
        runtime=newRuntime();SQLiteDatabase.observer=observer;bind();diagnostic(false);observing=true;
    }
    RuntimeR27 newRuntime()throws Exception{RuntimeR27 r=(RuntimeR27)U.allocateInstance(RuntimeR27.class);set(r,"atomStates",new HashMap<Integer,String>());r.derived=new ArrayList<>();return r;}
    void schemas(){
        db.beginTransaction();try{
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            for(String ddl:ResearchSchemaR30R1.DDL)db.execSQL(ddl);
            for(String ddl:SchemaR27.R31_WRITE_DDL)db.execSQL(ddl);
            db.execSQL(AtomScoreR27.CONTRIBUTION_DDL_R30);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
            SchemaR27.ensureLiveSourceRevision(db);if(!scenario.equals("legacy-upgrade"))AtomPublicationStoreR31.ensureSchemaR31(db);
            db.execSQL("ALTER TABLE r26_task ADD COLUMN first_progress_at INTEGER NOT NULL DEFAULT 0");
            db.execSQL("INSERT INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',0,0)",new Object[]{currentOwner,IssueTimelineWriterR30.bootId()});
            db.execSQL("INSERT INTO r26_task(task_id,owner,fence,status,lane,mechanism) VALUES('r27-atoms',?,1,'RUNNING','R27','HOST_ORIGINAL_ATOMS')",new Object[]{currentOwner});
            db.execSQL("INSERT INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms) VALUES('r27-atoms',?,1,?,?)",new Object[]{currentOwner,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+600000});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    void bind(){RuntimeCoordinatorR26.bindBackgroundClaim(new RuntimeCoordinatorR26.Claim("r27-atoms",currentOwner,currentFence,0));}
    void transfer()throws Exception{
        currentOwner="host-new-atom-owner";currentFence++;
        raw("INSERT OR IGNORE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',0,0)",currentOwner,IssueTimelineWriterR30.bootId());
        raw("UPDATE r26_task SET owner=?,fence=?,status='RUNNING' WHERE task_id='r27-atoms'",currentOwner,currentFence);
        raw("UPDATE r31_task_lease SET owner_epoch=?,fence=?,boot_id=?,deadline_elapsed_ms=? WHERE task_id='r27-atoms'",currentOwner,currentFence,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+600000);bind();
    }
    void scientificFrame()throws Exception{
        data=ReplayDatasetR27.synthetic(3000,601,new int[0]);split=SplitR27.of(data,null);AtomReplayR27.Frame frame=AtomReplayR27.buildLegacy(data,0,split.selectTo);
        runtime.frameValues=Arrays.copyOf(frame.values,data.size());runtime.framePresent=Arrays.copyOf(frame.present,data.size());runtime.frameHit=Arrays.copyOf(frame.hit,data.size());
        for(int t=split.selectTo;t<data.size();t++){runtime.frameValues[t]=new double[240];Arrays.fill(runtime.frameValues[t],.5);runtime.framePresent[t]=new boolean[240];Arrays.fill(runtime.framePresent[t],true);runtime.frameHit[t]=Integer.MIN_VALUE;}
    }
    void score()throws Exception{AtomWorldR27.score(runtime,worker,data,split);}
    Map<String,Object> scientificProjection(boolean visible){
        Map<String,Object> result=new LinkedHashMap<>();String[][] sources={
            {visible?"r31_atom_score_visible":"r27_atom_score","atom_id,atom_index,dataset_hash,n_select,gain_select,lcb_select,ucb_select,n_holdout,gain_holdout,lcb_holdout,missing_rate,state,derived,definition","atom_id"},
            {visible?"r31_atom_contribution_visible":"r30_atom_contribution","atom_id,dataset_hash,from_row,to_row,n_periods,paired_comparisons,mean_gain,lcb95,source","atom_id,dataset_hash"},
            {visible?"r31_molecule_visible":"r27_molecule","molecule_hash,definition,generation,state,select_gain,reason","molecule_hash"}};
        for(int i=0;i<sources.length;i++){String[] source=sources[i];List<Object> rows=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT "+source[1]+" FROM "+source[0]+" ORDER BY "+source[2],null)){while(c.moveToNext()){List<Object> row=new ArrayList<>();for(int column=0;column<c.getColumnCount();column++)row.add(c.isNull(column)?null:c.getString(column));rows.add(row);}}result.put(new String[]{"scores165","pairedContributions","moleculeRegistry"}[i],rows);}
        result.put("fixture","ORIGINAL_3000_ROWS_SEED601_SAME_SPLIT_POISONED_CONFIRM_NOT_NATURAL_QUALITY");return result;
    }
    void compareActualBaselineScience()throws Exception{
        String expected=new String(Files.readAllBytes(new File(output,"actual-baseline-scientific-projection.json").toPath()),StandardCharsets.UTF_8).trim(),actual=JsonR27.write(scientificProjection(true));
        Files.write(new File(output,"actual-new-scientific-projection.json").toPath(),(actual+"\n").getBytes(StandardCharsets.UTF_8));need(expected.equals(actual),"all165 score fields, all paired period/count/mean/LCB/source fields and original32 molecule first-birth values match actual old production producer exactly; timestamps intentionally excluded");
    }
    void evolution()throws Exception{EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;runtime.evo=new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new StoreR27.EvolutionSink("HOST","fixture"),()->0L);runtime.evo.stepDataset();}
    AtomPublicationStoreR31.Plan engineeringPlan(String token){
        long revision=ResearchInputPinR30R1.revision(db,"HOST_PAIR_PROTOCOL_FIXTURE");
        AtomPublicationStoreR31.Input input=AtomPublicationStoreR31.open(worker,"SCORES","HOST_PAIR_PROTOCOL_FIXTURE",HashingSU1.fields("NOT_LEARNING_ENGINEERING_FIXTURE",token,Long.toString(revision)),-1,revision,1000001);
        List<AtomPublicationStoreR31.Pair> pairs=new ArrayList<>();int[] eligible=AtomReplayR27.eligible();
        for(int i=0;i<eligible.length;i++){int atom=eligible[i];String id=GenesisSchemaRegistryR915.atoms().get(atom).id;
            pairs.add(new AtomPublicationStoreR31.Pair(i,new Object[]{id,atom,input.dataset,10,0.0,-0.01,0.01,0,null,null,0.0,"NEUTRAL",0,"",input.computedAt},new Object[]{id,input.dataset,10,20,10,10,0.0,-0.01,"HOST_PROTOCOL_PAIR_FIXTURE_NOT_LEARNING",input.computedAt}));
        }
        return AtomPublicationStoreR31.prepare(input,pairs,1650,null);
    }
    void publish(AtomPublicationStoreR31.Plan p)throws Exception{AtomPublicationStoreR31.publish(worker,p);}
    void exactViewsAndReport(){
        AtomPublicationStoreR31.requireReadyR31(db);long changes=totalChanges();
        for(String view:new String[]{AtomPublicationStoreR31.SCORE_VIEW,AtomPublicationStoreR31.CONTRIBUTION_VIEW,AtomPublicationStoreR31.MOLECULE_VIEW})try(Cursor c=db.rawQuery("SELECT * FROM "+view+" LIMIT 1",null)){need(c.getColumnCount()>0,"actual view SELECT exposes real columns");}
        ReportR27.R30Input report=ReportSnapshotR30.capture(db);need(report.errors.isEmpty(),"actual full ReportSnapshot SELECT succeeds: "+report.errors);need(((Number)report.facts.get("atoms")).longValue()==165,"actual snapshot consumer complete score count");
        Map<?,?> tables=(Map<?,?>)report.watermark.get("tables"),view=(Map<?,?>)tables.get(AtomPublicationStoreR31.SCORE_VIEW);need("view".equals(view.get("objectType"))&&view.get("maxRowid")==null&&"VIEW_HAS_NO_STABLE_ROWID".equals(view.get("rowidReason")),"view has no invented rowid watermark");need(totalChanges()==changes,"all report consumers are read only");
    }
    String physicalLegacyHash(){List<Object> rows=new ArrayList<>();for(String table:new String[]{"r27_atom_score","r30_atom_contribution","r27_molecule"})try(Cursor c=db.rawQuery("SELECT * FROM "+table+" ORDER BY 1",null)){while(c.moveToNext()){List<Object> row=new ArrayList<>();row.add(table);for(int i=0;i<c.getColumnCount();i++)row.add(c.isNull(i)?null:c.getString(i));rows.add(row);}}return HashingSU1.text(JsonR27.write(rows));}
    void legacyCase()throws Exception{
        observing=false;db.beginTransaction();try{
            int[] el=AtomReplayR27.eligible();for(int atom:el){String id=GenesisSchemaRegistryR915.atoms().get(atom).id;db.execSQL("INSERT INTO r27_atom_score VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,atom,"HOST_PAIR_PROTOCOL_FIXTURE",10,0.37,0.1,0.5,0,null,null,0.0,"NEUTRAL",0,"",1234});db.execSQL("INSERT INTO r30_atom_contribution VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{id,"HOST_PAIR_PROTOCOL_FIXTURE",10,20,10,10,0.37,0.1,"LEGACY_EXACT_BYTES",1234});}
            db.execSQL("INSERT INTO r27_atom_score VALUES('DERIVED_OLD',-1,'HOST_PAIR_PROTOCOL_FIXTURE',10,0.37,0.1,0.5,0,NULL,NULL,0,'DERIVED',1,'OLD_DERIVED',1234)");db.execSQL("INSERT INTO r30_atom_contribution VALUES('DERIVED_OLD','HOST_PAIR_PROTOCOL_FIXTURE',10,20,10,10,0.37,0.1,'LEGACY_DERIVED',1234)");db.execSQL("INSERT INTO r27_molecule VALUES('LEGACY_MOLECULE','LEGACY_DEFINITION',7,'RETIRED',0.37,'LEGACY_REASON',1234)");
            db.execSQL("CREATE TABLE unrelated_original_fixture(id INTEGER PRIMARY KEY,payload BLOB NOT NULL)");db.execSQL("INSERT INTO unrelated_original_fixture VALUES(1,?)",new Object[]{new byte[]{0,1,-1,4}});db.execSQL("PRAGMA user_version=601");db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        String before=physicalLegacyHash();long schema=count("SELECT count(*) FROM sqlite_master WHERE name LIKE 'r31_atom_%' OR name LIKE 'r31_molecule_%'");need(schema==0,"actual legacy database begins without new publication schema");
        db.beginTransaction();try{AtomPublicationStoreR31.ensureSchemaR31(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
        AtomPublicationStoreR31.requireReadyR31(db);need(physicalLegacyHash().equals(before),"actual additive migration preserves every original row byte rendering");need(count("PRAGMA user_version")==601,"app version is preserved by domain migration");need("0001FF04".equals(text("SELECT hex(payload) FROM unrelated_original_fixture")),"unrelated original BLOB bytes preserved");
        need(count("SELECT count(*) FROM r31_atom_score_visible")==166&&count("SELECT count(*) FROM r31_atom_contribution_visible")==166&&count("SELECT count(*) FROM r31_molecule_visible")==1,"legacy complete consumers available immediately after additive migration");observing=true;
        everyStage=()->{need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0 AND gain_select=0.37")==165,"every actual intermediate commit retains original complete165 scientific values");need(count("SELECT count(*) FROM r31_atom_score_visible")==166,"legacy derived row is retained during every partial commit");};
        publish(engineeringPlan("MIGRATED_COMPLETE"));everyStage=null;need(everyStageCalls==165&&hookFailure==null,"all165 actual intermediate publications observed without a swallowed diagnostic assertion: "+hookFailure);
        need(physicalLegacyHash().equals(before),"publishing a new result set never rewrites historical legacy evidence");need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0 AND gain_select=0.0")==165,"new complete set actually replaces legacy current only after publication");need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=1")==1,"original derived capabilities remain visible");need(count("SELECT count(*) FROM r31_atom_contribution_visible WHERE source='HOST_PROTOCOL_PAIR_FIXTURE_NOT_LEARNING'")==165&&count("SELECT count(*) FROM r31_atom_contribution_visible")==166,"new completed paired results overlay matching legacy keys without losing derived evidence");
        assertBudget(false);writeEvidence("ACTUAL_LEGACY_ADDITIVE_MIGRATION_AND_COMPLETE_CONSUMERS_VERIFIED");
    }
    void protocolCase()throws Exception{
        AtomPublicationStoreR31.Plan old=engineeringPlan("OLD_COMPLETE");publish(old);String before=old.source.run;AtomPublicationStoreR31.Plan fresh=engineeringPlan("NEW_COMPLETE");
        Map<Integer,String> oldStates=AtomPublicationStoreR31.activeStates(db);need(oldStates.size()==165,"old full set loads into actual leafWeights input");runtime.atomStates.putAll(oldStates);Map<String,Double> oldWeights=runtime.leafWeights();
        boolean interrupted=scenario.equals("cancel-between-chunks")||scenario.equals("interrupt-in-transaction");
        if(scenario.equals("cancel-between-chunks"))afterStage=()->{injected=true;((java.util.concurrent.ConcurrentHashMap<String,Long>)field(pool,"revokedTaskFencesR31")).put("r27-atoms",currentFence);};
        if(scenario.equals("interrupt-in-transaction")){failStage=stageCalls+3;faultKind="INTERRUPT_BODY";}
        if(scenario.equals("source-correction"))afterStage=()->{injected=true;newInputSameScientificOpportunities();};
        if(scenario.equals("owner-transfer"))afterStage=()->{injected=true;try{transfer();RuntimeCoordinatorR26.bindBackgroundClaim(new RuntimeCoordinatorR26.Claim("r27-atoms","host-atom-owner",1,0));}catch(Exception e){throw new IllegalStateException(e);}};
        if(scenario.equals("predecessor-cas")){
            failStage=stageCalls+2;try{publish(fresh);throw new AssertionError("PREDECESSOR_INTERLEAVING_NOT_STAGED");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(injected,"actual interrupted preparation creates old predecessor identity");}
            AtomPublicationStoreR31.Plan other=engineeringPlan("OTHER_COMPLETE");publish(other);
            try{publish(fresh);throw new AssertionError("PREDECESSOR_CAS_NOT_ENFORCED");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(chainContains(expected,"ACTIVE_PREDECESSOR_CHANGED"),"actual final transaction rejected frozen predecessor mismatch");}
            need(other.source.run.equals(AtomPublicationStoreR31.activeRun(db,"SCORES")),"completed interleaving publication remains current");need(runtime.leafWeights().equals(oldWeights),"failed older proposal never changes memory");need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"no older partially staged result replaces real current");assertBudget(false);writeEvidence("TARGETED_HOST_ASSERTIONS_COMPLETED");return;
        }
        if(scenario.equals("cancel-between-chunks")||scenario.equals("interrupt-in-transaction")||scenario.equals("source-correction")||scenario.equals("owner-transfer")||scenario.equals("predecessor-cas")){
            try{publish(fresh);throw new AssertionError("EXPECTED_REAL_PROTOCOL_FAILURE_MISSING");}catch(InterruptedException expected){need(interrupted&&injected,"actual cancellation reached prepared owner");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(injected,"actual source/fence/CAS/body rejection reached production coordinator");if(scenario.equals("source-correction"))need(chainContains(expected,"RESEARCH_CONSUME_STALE"),"actual source guard rejected old original input");if(scenario.equals("owner-transfer"))need(chainContains(expected,"STALE_FENCE_OR_LEASE_OR_SESSION"),"actual owner/fence/session guard rejected superseded worker");}
            if(scenario.equals("interrupt-in-transaction")){need(Thread.currentThread().isInterrupted(),"interrupt flag preserved through rollback");Thread.interrupted();need(transactions.stream().anyMatch(t->!Boolean.TRUE.equals(t.get("committed"))),"real interrupted transaction rolled back");}
            need(runtime.leafWeights().equals(oldWeights),"incomplete results never change actual leafWeights input");need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"partial generation never counted as completed");
            if(!scenario.equals("predecessor-cas"))need(before.equals(AtomPublicationStoreR31.activeRun(db,"SCORES")),"old current remains usable until complete publication");
            need(count("SELECT count(*) FROM r31_atom_result_run WHERE state='PREPARING'")>0,"real incomplete publication header preserved");
            if(scenario.equals("cancel-between-chunks"))((java.util.concurrent.ConcurrentHashMap<?,?>)field(pool,"revokedTaskFencesR31")).clear();
            if(scenario.equals("owner-transfer")){bind();fresh=engineeringPlan("NEW_COMPLETE");need(fresh.source.creator.equals("host-atom-owner")&&fresh.source.fence==1,"new owner keeps original prepared metadata audit identity");}
            if(scenario.equals("source-correction"))fresh=engineeringPlan("NEW_COMPLETE");
            if(scenario.equals("predecessor-cas")){String other=AtomPublicationStoreR31.activeRun(db,"SCORES");need(!other.equals(before)&&!other.equals(fresh.source.run),"concurrent real full publication wins predecessor CAS");assertBudget(false);writeEvidence("TARGETED_HOST_ASSERTIONS_COMPLETED");return;}
        }
        publish(fresh);need(fresh.source.run.equals(AtomPublicationStoreR31.activeRun(db,"SCORES")),"full publication is actual current");need(AtomPublicationStoreR31.activeStates(db).size()==165,"real complete map restores after retry/owner transfer");
        if(scenario.equals("superseded-replay")){int attempts=transactions.size();try{publish(old);throw new AssertionError("SUPERSEDED_COMPLETE_REPLAY_REPORTED_CURRENT");}catch(AtomPublicationStoreR31.Dependency expected){need(expected.getMessage().contains("COMPLETED_RESULT_SUPERSEDED"),"completed historical receipt never pretends to be current publication");}need(attempts==transactions.size()&&fresh.source.run.equals(AtomPublicationStoreR31.activeRun(db,"SCORES")),"superseded replay performs no writer work and preserves newer current");}
        if(scenario.equals("exact-schema-and-report")){
            exactViewsAndReport();boolean prior=observing;observing=false;db.execSQL("DROP VIEW r31_atom_contribution_visible");db.execSQL("CREATE VIEW r31_atom_contribution_visible AS SELECT * FROM r30_atom_contribution WHERE 0");observing=prior;
            try{AtomPublicationStoreR31.requireReadyR31(db);throw new AssertionError("WRONG_VIEW_DEFINITION_ACCEPTED");}catch(AtomPublicationStoreR31.Dependency expected){need(expected.getMessage().contains("SCHEMA_DEFINITION_MISMATCH:r31_atom_contribution_visible"),"same-name wrong view exact definition rejected");}
        }
        assertBudget(false);writeEvidence("TARGETED_HOST_ASSERTIONS_COMPLETED");
    }
    void processPhase(boolean resume,boolean inBody)throws Exception{
        if(!resume){
            AtomPublicationStoreR31.Plan old=engineeringPlan("OLD_COMPLETE");publish(old);AtomPublicationStoreR31.Plan fresh=engineeringPlan("NEW_COMPLETE");
            if(inBody)haltInBody=stageCalls+74;else haltStage=stageCalls+73;
            publish(fresh);throw new AssertionError("ACTUAL_HALT_DID_NOT_EXECUTE");
        }
        String old=AtomPublicationStoreR31.activeRun(db,"SCORES");need(count("SELECT count(*) FROM r31_atom_score_stage WHERE run_id<> (SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES')")==73,"fresh JVM sees exactly73 committed partial results; interrupted transaction recovered atomically");
        need(count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"old complete165 still current after actual process halt");
        transfer();AtomPublicationStoreR31.Plan fresh=engineeringPlan("NEW_COMPLETE");long originalAt=fresh.source.computedAt;need(fresh.source.creator.equals("host-atom-owner")&&fresh.source.fence==1,"fresh JVM preserves original owner/timestamp metadata");
        publish(fresh);need(stageCalls==92,"exactly92 missing result pairs execute; committed73 receipts replay without reapplying");need(!old.equals(AtomPublicationStoreR31.activeRun(db,"SCORES"))&&fresh.source.run.equals(AtomPublicationStoreR31.activeRun(db,"SCORES")),"new complete generation activates after actual process restart");
        need(count("SELECT count(*) FROM r31_atom_score_stage WHERE run_id='"+fresh.source.run+"' AND computed_at="+originalAt)==165,"original preparation time retained for all165 rows across process/heap restart");need(count("SELECT count(*) FROM r31_atom_contribution_stage WHERE run_id='"+fresh.source.run+"' AND recorded_at="+originalAt)==165,"paired scientific receipt clocks retained");
        need(count("SELECT count(*) FROM r31_commit_receipt")==334,"two exact167 commits only, old/staged receipts deduplicate across owner and JVM");need(count("SELECT count(*) FROM r31_atom_result_chunk")==330,"two exact165 part manifests, no heap-dependent repartition");
        try(SQLiteDatabase freshRead=SQLiteDatabase.openDatabase(databaseFile.getPath(),null,SQLiteDatabase.OPEN_READONLY)){try(Cursor c=freshRead.rawQuery("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0",null)){need(c.moveToFirst()&&c.getLong(0)==165,"independent native connection observes completed165 after restart");}}
        assertBudget(false);writeEvidence("ACTUAL_HOST_PROCESS_RESTART_CROSS_HEAP_RECEIPTS_COMPLETED");
    }
    void atomsStepRetry()throws Exception{
        scientificFrame();evolution();runtime.evoData=data;runtime.frameDataset="UNBUILT_ORIGINAL_FIXTURE";runtime.frameCursor=0;failStage=1;
        boolean failed=false;int slices=0;
        for(;slices<64;slices++){
            try{ResearchPoolR27.Outcome outcome=AtomWorldR27.atomsStep(runtime,worker);need(outcome.units>0&&outcome.units<=120,"actual original frame producer keeps120-row slices");need(runtime.frameCursor<runtime.frameValues.length&&transactions.isEmpty(),"incomplete frame never publishes an all-missing complete score");}
            catch(DatabaseCoordinator.WriteRejectedR31 expected){rejected(expected);need(injected&&runtime.frameCursor==runtime.frameValues.length,"real score commit failed only after full original frame computed");failed=true;break;}
        }
        need(failed&&slices>0,"bounded actual producer reached requested durable commit failure");need(AtomPublicationStoreR31.activeRun(db,"SCORES")==null&&runtime.atomStates.isEmpty(),"failed first publication is not exposed as complete");
        ResearchPoolR27.Outcome retry=AtomWorldR27.atomsStep(runtime,worker);need(retry.units==1&&AtomPublicationStoreR31.activeStates(db).size()==165&&runtime.atomStates.size()==165,"next real atomsStep retries failed scoring after frame cursor advanced; it never skips into molecules");need(AtomWorldR27.publishedMoleculeGenerationR31(runtime)==-1,"retry did not bypass score into another research phase");assertBudget(false);writeEvidence("ACTUAL_ATOMS_STEP_FRAME_AND_FAILED_PUBLICATION_RETRY_COMPLETED");
    }
    static Object field(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){throw new AssertionError(e);}}
    static boolean chainContains(Throwable error,String message){for(int n=0;n<16&&error!=null;n++,error=error.getCause())if(String.valueOf(error.getMessage()).contains(message))return true;return false;}
    void newInputSameScientificOpportunities(){raw("UPDATE r30_live_source_revision SET generation=generation+1 WHERE singleton=1");}
    void rejected(DatabaseCoordinator.WriteRejectedR31 failure){Map<String,Object> row=new LinkedHashMap<>();row.put("category",failure.category);row.put("operation",failure.operation);row.put("phase",failure.phase);row.put("commitOutcome",failure.commitOutcome.name());rejections.add(row);}
    void scoreFailure()throws Exception{try{score();throw new AssertionError("REQUESTED_ACTUAL_FAILURE_NOT_EXECUTED");}catch(DatabaseCoordinator.WriteRejectedR31 expected){rejected(expected);need(injected,"actual injected SQL/end fault reached production writer");if(scenario.equals("stage-unknown-then-readback"))need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.UNKNOWN,"unknown commit never silently becomes rollback/retry permission");}}
    void diagnostic(boolean enabled)throws Exception{
        diagnosticFault=enabled;Field f=DatabaseCoordinator.class.getDeclaredField("diagnosticFaultR31");f.setAccessible(true);ThreadLocal<Object> local=(ThreadLocal<Object>)f.get(null);
        Class<?> api=Class.forName("com.ethk3.novaonepro.DatabaseCoordinator$DiagnosticFaultR31");local.set(Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(o,m,args)->{
            if(args!=null&&args.length==1&&"AFTER_COMMIT".equals(args[0])&&diagnosticFault){injected=true;throw new IllegalStateException("HOST_ATOM_AFTER_COMMIT_DIAGNOSTIC_FAILURE");}
            if(args!=null&&args.length==1&&"WRITER_HOLD".equals(args[0])&&lastEnded!=null){
                Map<String,Object> ended=lastEnded;lastEnded=null;
                for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.snapshot())if(stat.operation.startsWith("R27_")){
                    long[] old=priorWriterStats.get(stat.operation),now={stat.count.get(),stat.holdSum.get(),stat.waitSum.get()};
                    if(old==null)old=new long[3];if(now[0]>old[0]){ended.put("actualWriterHoldMsFromProductionLedger",now[1]-old[1]);ended.put("actualWriterWaitMsFromProductionLedger",now[2]-old[2]);ended.put("operation",stat.operation);}priorWriterStats.put(stat.operation,now);
                }
                // Fault boundaries occur only after the production writer actually released its lock.
                if(Boolean.TRUE.equals(ended.get("committed"))&&Boolean.TRUE.equals(ended.get("primaryStageAttempt"))&&everyStage!=null){everyStageCalls++;try{everyStage.run();}catch(Throwable failure){hookFailure=failure;}}
                if(Boolean.TRUE.equals(ended.get("committed"))&&Boolean.TRUE.equals(ended.get("primaryStageAttempt"))&&afterStage!=null){Runnable action=afterStage;afterStage=null;action.run();}
                if(Boolean.TRUE.equals(ended.get("committed"))&&Boolean.TRUE.equals(ended.get("primaryStageAttempt"))&&stageCalls==haltStage){writeEvidence("INTERRUPTED_ACTUAL_PROCESS_HALT");System.out.flush();System.err.flush();Runtime.getRuntime().halt(73);}
            }return null;}));
    }
    void assertBudget(boolean baseline){
        long cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);boolean oversize=false;
        for(Map<String,Object> tx:transactions){long sql=((Number)tx.get("sqlCount")).longValue(),triggers=((Number)tx.get("triggerGuardUpperBound")).longValue();oversize|=sql>64;
            need(tx.get("actualWriterHoldMsFromProductionLedger")!=null,"actual sole writer lock hold measured for every transaction");
            if(!baseline){need(sql+triggers<=64,"all public SQL calls + conservative trigger guard execution upper bound<=64: "+tx);need(((Number)tx.get("bindBytes")).longValue()<=cap,"actual binds including callback and receipts<=dynamic cap");need(((Number)tx.get("ddlCount")).longValue()==0,"business transaction DDL=0");}
        }
        need(!transactions.isEmpty(),"actual production transactions observed");if(baseline)need(oversize,"actual old producer transaction over64 reproduced");need(!db.inTransaction(),"actual transaction resource exited");
    }
    void writeEvidence(String status){try{
        Map<String,Object> report=new LinkedHashMap<>();report.put("scope","ACTUAL_PRODUCTION_NATIVE_SQLITE_HOST_ONLY_NOT_ANDROID_NOT_NATURAL_QUALITY");report.put("case",scenario);report.put("status",status);report.put("assertions",assertions);report.put("maxMemory",Runtime.getRuntime().maxMemory());report.put("dynamicBindCap",Math.min(262144L,Runtime.getRuntime().maxMemory()/512L));report.put("injected",injected);report.put("transactions",transactions);report.put("stageInsertAttempts",stageCalls);report.put("sqliteIndependentReadbackOpens",SQLiteDatabase.independentOpens);report.put("actualTypedRejections",rejections);
        Files.write(new File(output,scenario+".json").toPath(),(JsonR27.write(report)+"\n").getBytes(StandardCharsets.UTF_8));try(PrintWriter p=new PrintWriter(new File(output,scenario+"-sql.jsonl"),"UTF-8")){for(Map<String,Object> call:calls)p.println(JsonR27.write(call));}
        System.out.println("R31_ATOM_HOST case="+scenario+" assertions="+assertions+" transactions="+transactions.size()+" status="+status);
    }catch(IOException e){throw new AssertionError(e);}}
    public void close()throws Exception{Field f=DatabaseCoordinator.class.getDeclaredField("diagnosticFaultR31");f.setAccessible(true);((ThreadLocal<?>)f.get(null)).remove();RuntimeCoordinatorR26.unbindBackgroundClaimR31();observing=false;SQLiteDatabase.observer=null;need(!db.inTransaction(),"native transaction actually closed before releasing fixture");db.close();need(connection.isClosed(),"actual native connection closed");}
    public static void main(String[] args)throws Exception{
        String name=args[0];File output=new File(args[1]);
        boolean process=name.startsWith("process-"),resume=name.contains("resume"),body=name.contains("inbody");File persistent=process?new File(output,body?"process-inbody-fixture.db":"process-aftercommit-fixture.db"):null;
        try(R31AtomPublicationHost h=new R31AtomPublicationHost(name,output,persistent,!resume)){
            try{
                if(process){h.processPhase(resume,body);return;}
                if(name.equals("legacy-upgrade")){h.legacyCase();return;}
                if(name.equals("atoms-step-retry")){h.atomsStepRetry();return;}
                if(Arrays.asList("cancel-between-chunks","interrupt-in-transaction","source-correction","owner-transfer","predecessor-cas","exact-schema-and-report","superseded-replay").contains(name)){h.protocolCase();return;}
                h.scientificFrame();h.score();
                if(name.startsWith("baseline-")){if(name.equals("baseline-molecules32")){h.evolution();AtomWorldR27.molecules(h.runtime,h.worker,h.runtime.evo);need(h.count("SELECT count(*) FROM r27_molecule")>0,"old32 molecule work actually persisted");need(h.transactions.stream().anyMatch(t->"R27_MOLECULES".equals(t.get("operation"))&&((Number)t.get("sqlCount")).longValue()>64),"old32 producer plus real protocol exceeds64");Files.write(new File(output,"actual-baseline-scientific-projection.json").toPath(),(JsonR27.write(h.scientificProjection(false))+"\n").getBytes(StandardCharsets.UTF_8));}h.assertBudget(true);h.writeEvidence("BASELINE_OVERSIZE_REPRODUCED");return;}
                need(h.count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"actual complete165score consumer");
                String old=h.text("SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES'");Map<Integer,String> oldMemory=new HashMap<>(h.runtime.atomStates);
                h.newInputSameScientificOpportunities();
                if(name.equals("stage-sql-failure")||name.equals("stage-first-sql-failure"))h.failStage=h.stageCalls+(name.equals("stage-first-sql-failure")?1:83);
                if(name.equals("stage-end-rollback")){h.afterCommitStage=h.stageCalls+83;h.faultKind="BEFORE_COMMIT";}
                if(name.equals("stage-after-commit")){h.afterCommitStage=h.stageCalls+83;h.faultKind="AFTER_COMMIT";}
                if(name.equals("stage-unknown-then-readback")){h.afterCommitStage=h.stageCalls+83;h.faultKind="UNKNOWN";}
                if(name.equals("stage-postcommit-diagnostic"))h.diagnostic(true);
                if(name.equals("stage-sql-failure")||name.equals("stage-first-sql-failure")||name.equals("stage-end-rollback")||name.equals("stage-unknown-then-readback")){
                    h.scoreFailure();need(old.equals(h.text("SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES'")),"old complete current retained after real partial failure");need(oldMemory.equals(h.runtime.atomStates),"old memory unchanged after failed partial result");need(h.count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"no partial score exposed");long partial=h.count("SELECT count(*) FROM r31_atom_score_stage WHERE run_id<>(SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES')");need(name.equals("stage-first-sql-failure")?partial==0:partial>0,"actual incomplete staged rows/count retained yet invisible");SQLiteDatabase.rejectReadback=false;
                }
                h.score();h.diagnostic(false);need(!old.equals(h.text("SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES'")),"new full set actually activated");need(h.count("SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==165,"exact165 retained after new full set publication");
                if(name.equals("stage-after-commit"))need(h.injected&&SQLiteDatabase.independentOpens>0,"real committed ACK fault independently resolved with durable receipt");if(name.equals("stage-postcommit-diagnostic"))need(h.injected,"postcommit diagnostic fault actually executed");
                long receipts=h.count("SELECT count(*) FROM r31_commit_receipt"),rows=h.count("SELECT count(*) FROM r31_atom_score_stage");need(receipts==334&&rows==330,"two complete original165 sets each have exact167 durable receipts with no duplicate data");h.score();need(receipts==h.count("SELECT count(*) FROM r31_commit_receipt")&&rows==h.count("SELECT count(*) FROM r31_atom_score_stage"),"same input repeat adds no receipt/data");
                h.assertBudget(false);h.writeEvidence("TARGETED_HOST_ASSERTIONS_COMPLETED");
            }catch(Throwable failure){h.writeEvidence("FAILED_ORIGINAL_ASSERTION");throw failure;}
        }
    }
}
