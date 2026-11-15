package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.File;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** HOST setup only. Uses actual DbStore/Coordinator/Runtime/freeze/qualification
 * business code. The controlled history is explicitly synthetic mechanics data. */
public final class TypedFreezeRuntimeR31Fixture implements AutoCloseable {
    public final SQLiteDatabase db;public final DbStore store;public final RuntimeR27 runtime;public final DatabaseCoordinator coordinator;
    public final String policy,target,base;public final long targetOrdinal,deadline;private final ResearchPoolR27.Worker worker;
    private final String owner="typed-host-"+UUID.randomUUID();private RuntimeCoordinatorR26.Claim claim;
    static void set(Object owner,String name,Object value)throws Exception{Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("FIXTURE_SCALAR");return c.getLong(0);}}
    public TypedFreezeRuntimeR31Fixture(Path root)throws Exception{
        Files.createDirectories(root);Path file=root.resolve("main.db");db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file));
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe unsafe=(Unsafe)f.get(null);
        coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);DbHelper helper=new DbHelper(db);android.app.Application app=new android.app.Application(file.toFile());
        Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lock.get(null));set(coordinator,"helper",helper);set(coordinator,"app",app);set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
        store=(DbStore)unsafe.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"researchAppCP12",app);set(store,"researchInputRootCP11",root.resolve("research-input-v2").toFile());set(store,"columnCache",new HashMap<String,HashSet<String>>());
        for(Field field:DbStore.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())&&field.getType()==ThreadLocal.class){field.setAccessible(true);field.set(store,new ThreadLocal<>());}
        base="202611130501";targetOrdinal=TargetIssueResolver.sequenceOrdinalSU1(base)+1;target=TargetIssueResolver.fromSequenceOrdinalSU1(targetOrdinal);long now=System.currentTimeMillis();deadline=now+59_000;
        transaction(()->{
            db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY AUTOINCREMENT,issue TEXT UNIQUE,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE su1_source_clock(singleton INTEGER PRIMARY KEY,generation INTEGER)");db.execSQL("INSERT INTO su1_source_clock VALUES(1,1)");
            db.execSQL("CREATE TABLE IF NOT EXISTS b03_transaction_audit(idempotency_key TEXT PRIMARY KEY,operation TEXT,game_code TEXT,target_issue TEXT,transaction_hash TEXT,status TEXT,error_detail TEXT,created_at INTEGER)");
            db.execSQL("CREATE TABLE trades(_id INTEGER PRIMARY KEY,target_issue TEXT,action TEXT,stat_scope TEXT,settled INTEGER,bet_time INTEGER,created_at INTEGER)");
            // Schema setup only: take every field and SQLite affinity from the
            // actual terminal writer's ContentValues projection, not a business
            // writer stub or a shortened eight-column REAL table.
            try{
                Method projection=DbStore.class.getDeclaredMethod("tradeToValues",Models.Trade.class);projection.setAccessible(true);
                android.content.ContentValues values=(android.content.ContentValues)projection.invoke(store,new Models.Trade());
                Set<String> present=new HashSet<>(Arrays.asList("_id","target_issue","action","stat_scope","settled","bet_time","created_at"));
                for(Map.Entry<String,Object> entry:values.valueSet())if(present.add(entry.getKey())){
                    Object value=entry.getValue();String type=value instanceof byte[]?"BLOB":value instanceof Double||value instanceof Float?"REAL":value instanceof Number||value instanceof Boolean?"INTEGER":"TEXT";
                    db.execSQL("ALTER TABLE trades ADD COLUMN \""+entry.getKey().replace("\"","\"\"")+"\" "+type);
                }
                Method schema=com.ethk3.novaonepro.r952.ComputationReceiptR974.class.getDeclaredMethod("schema",SQLiteDatabase.class);schema.setAccessible(true);schema.invoke(null,db);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("HOST_ACTUAL_TERMINAL_SCHEMA_PROJECTION",error);}
            db.execSQL("CREATE TABLE IF NOT EXISTS research_exact_probability_f05(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,trade_id INTEGER NOT NULL UNIQUE REFERENCES trades(_id),candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL CHECK(length(model_hash)=64),certificate_hash TEXT NOT NULL CHECK(length(certificate_hash)=64),calibration_hash TEXT NOT NULL CHECK(length(calibration_hash)=64),selection_hash TEXT NOT NULL CHECK(length(selection_hash)=64),scope_policy_hash TEXT NOT NULL CHECK(length(scope_policy_hash)=64),selected_hit_p REAL NOT NULL CHECK(selected_hit_p>0 AND selected_hit_p<1),proof_hash TEXT NOT NULL CHECK(length(proof_hash)=64),action_hash TEXT NOT NULL CHECK(length(action_hash)=64),authorization_hash TEXT NOT NULL,captured_at INTEGER NOT NULL,committed_at INTEGER NOT NULL CHECK(committed_at>=captured_at),PRIMARY KEY(game_code,target_issue))");
            db.execSQL("CREATE TABLE v57_shadow_observation_r35(target_issue TEXT PRIMARY KEY,decision_action TEXT,pre_draw_frozen INTEGER)");
            RuntimeSchemaR26.ensure(db);StrategySchemaSU2.ensure(db);SchemaR27.ensure(db);
            ResearchPlatformStoreR929.createSchema(db);
            try{Method schema=ResearchCohortR932.class.getDeclaredMethod("schema",SQLiteDatabase.class);schema.setAccessible(true);schema.invoke(null,db);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            db.execSQL("CREATE TABLE IF NOT EXISTS r974_prediction_rejection(target_issue TEXT,candidate_id TEXT,certificate_hash TEXT,reason TEXT,recorded_at INTEGER,PRIMARY KEY(target_issue,candidate_id,certificate_hash))");
            db.execSQL("INSERT INTO app_state VALUES(?,?,?)",new Object[]{BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK,now});db.execSQL("INSERT INTO app_state VALUES(?,?,?)",new Object[]{RealAuthoritySU2.USER_SWITCH,"1",now});
            db.execSQL("INSERT OR REPLACE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',?,0)",new Object[]{owner,IssueTimelineWriterR30.bootId(),now});
            db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status) VALUES('r27-live','R27','HOST_SYNTHETIC_TYPED_MECHANICS','READY')");
            insertDraw(targetOrdinal-501,deadline-60_000-500*60_000L);
        });
        installLifecycleThroughActualOwner();
        runtime=new RuntimeR27(store,owner);worker=runtime.pool().new Worker(db,"typed-host");runtime.operatorAbi=Collections.singletonMap("freq",StatsR27.sha256("HOST_SYNTHETIC_OPERATOR_ABI"));runtime.operatorAbiHash=SemanticAbiR27.operatorSetHash(runtime.operatorAbi);
        PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),3,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,IdeaFamilyR27.meta("I09","R31_TEST_NAMESPACE",0,1,"HOST_SYNTHETIC_MECHANICS_NOT_NATURAL_QUALITY"));policy=spec.policyHash();
        String controlA=StatsR27.sha256("HOST_TYPED_CONTROL_I00"),controlB=StatsR27.sha256("HOST_TYPED_CONTROL_NULL");
        transaction(()->{
            StoreR27.insertShadow(db,spec,1,"HOST_SYNTHETIC_MECHANICS",runtime.semanticAbi(spec),policy,policy);
            StoreR27.insertShadow(db,spec,1,"HOST_SYNTHETIC_CONTROL",runtime.semanticAbi(spec),controlA,policy);db.execSQL("UPDATE r27_live_shadow SET status='CONTROL_I00' WHERE policy_hash=?",new Object[]{controlA});
            StoreR27.insertShadow(db,spec,1,"HOST_SYNTHETIC_CONTROL",runtime.semanticAbi(spec),controlB,policy);db.execSQL("UPDATE r27_live_shadow SET status='CONTROL_NULL' WHERE policy_hash=?",new Object[]{controlB});
            // Explicit test-calendar clock; these generated rows are never
            // represented as past natural predictions or production quality.
            db.execSQL("UPDATE r29_qual_calendar SET registered_at=? WHERE experiment_id IN (?,?,?)",new Object[]{deadline-60_000-500*60_000L-20_000,policy,controlA,controlB});
            for(int i=0;i<500;i++){
                long ordinal=targetOrdinal-500+i,opened=deadline-60_000-(499-i)*60_000L;insertDraw(ordinal,opened);int hit=i%10==9?0:1;
                frozen(policy,ordinal,opened,hit,hit==1?1:4);frozen(controlA,ordinal,opened,0,4);frozen(controlB,ordinal,opened,0,4);
            }
            db.execSQL("INSERT INTO r27_semantic_fingerprint(policy_hash,build,fingerprint,operator_abi,computed_at) VALUES(?,?,?,?,?)",new Object[]{policy,BuildConfig.SOURCE_COMMIT_R26,StatsR27.sha256("HOST_SYNTHETIC_SEMANTIC_FINGERPRINT"),runtime.operatorAbiHash,now-1});
        });
        owned(()->{
            for(int pass=0;pass<40&&StoreR27.readEprocess(db,policy).n<500;pass++){
                QualificationInboxR29.consume(db,(op,batch)->{RuntimeR27.submit(worker,op,batch);return true;},"('ACTIVE','ACTIVE_REPAIR1')",targetOrdinal-1);
            }return null;
        });
        SequentialQualificationR27.State state=StoreR27.readEprocess(db,policy);if(state.n!=500||!SequentialQualificationR27.QUALIFIED.equals(state.state)||QualificationInboxR29.blockedForCertificate(db,policy))throw new AssertionError("ACTUAL_SYNTHETIC_QUALIFICATION_NOT_READY:"+state.n+":"+state.state);
        List<CalibrationRowsR27.Row> rows=new ArrayList<>();
        for(int i=0;i<122;i++){long ord=targetOrdinal-623+i;rows.add(new CalibrationRowsR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(ord),"REPLAY",0,0,.3,i%10==9?0:1,3,true,true,ord));}
        for(int i=0;i<500;i++){long ord=targetOrdinal-500+i,opened=deadline-60_000-(499-i)*60_000L;rows.add(new CalibrationRowsR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(ord),"FUTURE",opened-10_000,opened,.3,i%10==9?0:1,3,true,true,ord));}
        CalibrationRowsR27.Result result=CalibrationRowsR27.fitRolling(rows);if(!"FITTED".equals(result.state)||!Double.isFinite(result.ece)||result.ece>CertificateGateR27.MAX_ECE)throw new AssertionError("ACTUAL_SYNTHETIC_R27_CALIBRATION_NOT_READY");
        transaction(()->db.execSQL("INSERT INTO r27_calibration VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{policy,result.method,result.fitSource,result.fitRows,result.validationRows,result.excludedLate,result.excludedGap,result.a,result.b,result.ece,JsonR27.write(result.eceByStratum),result.state,result.reason,System.currentTimeMillis()-1}));
        verifyHotQueryPlans();
    }
    private void installLifecycleThroughActualOwner()throws Exception{
        if(!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract())||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract())||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31ModelPublicationContract()))throw new AssertionError("TYPED_UPGRADE_OLD_DOMAINS_NOT_READY");
        transaction(()->db.execSQL("CREATE INDEX idx_r31_mainline_unknown_attachment ON r31_mainline_choice(policy_hash)"));
        boolean rejected=false;int commits=0;
        for(int step=0;step<512;step++){
            try{LifecycleSchemaR31.Result result=coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"HOST_ACTUAL_LIFECYCLE_UNKNOWN_INDEX_CONTROL"),LifecycleSchemaR31::advanceWithinMigration);if(result.committedUnit)commits++;if(result.ready)break;}
            catch(RuntimeException expected){boolean schema=false;for(Throwable cause=expected;cause!=null;cause=cause.getCause())if(cause instanceof DatabaseCoordinator.SchemaPendingR31)schema=true;if(!schema)throw expected;rejected=true;break;}
        }
        if(!rejected)throw new AssertionError("TYPED_UPGRADE_UNKNOWN_ATTACHMENT_WAS_TRUSTED");
        transaction(()->db.execSQL("DROP INDEX idx_r31_mainline_unknown_attachment"));
        boolean ready=false;
        for(int step=0;step<512;step++){
            LifecycleSchemaR31.Result result=coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"HOST_ACTUAL_LIFECYCLE_TYPED_UPGRADE"),LifecycleSchemaR31::advanceWithinMigration);if(result.committedUnit)commits++;if(result.ready){ready=true;break;}
        }
        if(!ready||!MainlineFreezeProofR31.ready(db)||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract())||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31ModelPublicationContract()))throw new AssertionError("TYPED_UPGRADE_DECLARED_INDEX_DOMAIN_NOT_READY:"+SchemaDomainCoordinatorR926.matchReason(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()));
        System.out.println("TYPED_MAINLINE_LIFECYCLE_UPGRADE_PASS actualMigrationUnits="+commits+" originalReadyDomainsPreserved=true unknownAttachment=REFUSED directDdlOrManualRestamp=false");
    }
    private void verifyHotQueryPlans(){
        String[][] queries={
            {"idx_r31_mainline_qualification_open","SELECT 1 FROM r29_qual_conflict WHERE experiment_id=? AND resolved_at=0 LIMIT 1",policy},
            {"idx_r31_mainline_choice_current","SELECT choice_id,policy_hash,experiment_id,gate_contract,certificate_digest,model_state_hash,source_revision,effective_from_ordinal,selection_fence,status FROM r31_mainline_choice WHERE status IN ('ACTIVE','FALLBACK') ORDER BY selection_fence DESC,created_at DESC LIMIT 1"},
            {"idx_r31_mainline_choice_fence","SELECT coalesce(max(selection_fence),0) FROM r31_mainline_choice"},
            {"idx_r31_mainline_model_call","SELECT position,model_hash,status FROM r30_model_call WHERE policy_hash=? AND target_ordinal=? AND status='MATCHED' LIMIT 11",policy,String.valueOf(targetOrdinal)},
            {"idx_r31_mainline_model_publication","SELECT publication_id,state_hash,weights_hash,through_ordinal,model_available_at,published_at FROM r31_atomnet_publication WHERE model_hash=? AND weights_hash=? AND status='PUBLISHED' ORDER BY published_at DESC,publication_id LIMIT 1",StatsR27.sha256("fixture-model"),StatsR27.sha256("fixture-weights")}
        };
        for(String[] query:queries){boolean indexed=false;try(Cursor plan=db.rawQuery("EXPLAIN QUERY PLAN "+query[1],query.length==2?null:Arrays.copyOfRange(query,2,query.length))){while(plan.moveToNext()){String detail=plan.getString(3);if(detail.contains("TEMP B-TREE"))throw new AssertionError("TYPED_HOT_QUERY_TEMP_SORT:"+query[0]);indexed|=detail.contains(query[0]);}}if(!indexed)throw new AssertionError("TYPED_HOT_QUERY_NOT_INDEXED:"+query[0]);}
        System.out.println("TYPED_MAINLINE_QUERY_PLAN_PASS actualIndexes=5 exactProductionPredicates=true");
    }
    private void insertDraw(long ordinal,long opened){Object[] row=new Object[14];row[0]=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal);row[1]=opened;row[2]=opened+1;row[3]=0;for(int p=0;p<10;p++)row[p+4]=p+1;db.execSQL("INSERT INTO draws(issue,open_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",row);}
    private void frozen(String id,long ordinal,long opened,int hit,int first){double[] probabilities=new double[10];Arrays.fill(probabilities,.1);db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES(?,?,?,3,?,?,?,?,1,'FROZEN',?,?,?,?)",new Object[]{id,ordinal,TargetIssueResolver.fromSequenceOrdinalSU1(ordinal),StoreR27.pack(probabilities),first,first+1,first+2,opened-10_000,opened,hit,opened+1});}
    public <T>T owned(Callable<T> work)throws Exception{
        claim=coordinator.runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("HOST_TYPED_ACTUAL_CLAIM"),tx->{RuntimeCoordinatorR26.Claim next=RuntimeCoordinatorR26.claimWithinR31(tx,"r27-live",owner,"R27",claim);if(next==null)throw new IllegalStateException("HOST_TYPED_CLAIM_NOT_ACQUIRED");RuntimeCoordinatorR26.requireFence(tx,next);return next;});
        RuntimeCoordinatorR26.bindBackgroundClaim(claim);try{return work.call();}finally{RuntimeCoordinatorR26.unbindBackgroundClaimR31();}
    }
    public void certifyAndFreeze()throws Exception{
        owned(()->runtime.new LiveTask().step(worker));
        MainlineFreezeProofR31.Authority proof=MainlineFreezeProofR31.read(db,targetOrdinal,System.currentTimeMillis());if(proof==null)throw new AssertionError("ACTUAL_LIVE_TASK_DID_NOT_PUBLISH_TYPED_FREEZE");
        long issued=scalar(db,"SELECT issued_at FROM r27_certificate WHERE policy_hash='"+policy+"'"),expiry=scalar(db,"SELECT expires_at FROM r27_certificate WHERE policy_hash='"+policy+"'");
        owned(()->runtime.new LiveTask().step(worker));
        MainlineFreezeProofR31.Authority repeated=MainlineFreezeProofR31.read(db,targetOrdinal,System.currentTimeMillis());
        if(repeated==null||!proof.proofHash.equals(repeated.proofHash)||issued!=scalar(db,"SELECT issued_at FROM r27_certificate WHERE policy_hash='"+policy+"'")||expiry!=scalar(db,"SELECT expires_at FROM r27_certificate WHERE policy_hash='"+policy+"'"))throw new AssertionError("UNCHANGED_LIVE_TASK_REISSUED_CERTIFICATE_OR_LOST_ORIGINAL_FREEZE");
        System.out.println("TYPED_ACTUAL_LIVE_TASK_PASS executions=2 unchangedCertificateTimes=true originalFreezePreserved=true");
    }
    public void transaction(Runnable work){coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"HOST_TYPED_FIXTURE_BOUNDARY"),tx->{work.run();return 0;});}
    public void close()throws Exception{RuntimeCoordinatorR26.unbindBackgroundClaimR31();runtime.close();if(!runtime.pool().awaitResourcesTerminatedR31(2000))throw new AssertionError("TYPED_FIXTURE_RUNTIME_DRAIN");db.jdbc.close();}
}
