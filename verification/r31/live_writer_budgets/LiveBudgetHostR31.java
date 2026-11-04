package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.*;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Actual producers -> RuntimeR27.submit -> immutable queue -> coordinator ->
 * real SQLite, with real task/session/boot/elapsed fences and commit receipts.
 * Controlled fixtures prove mechanics, never Android/REAL quality or uptime. */
public final class LiveBudgetHostR31 {
    static final Unsafe UNSAFE;static int assertions;
    static{try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);UNSAFE=(Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    static Object get(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),"MISSING_SCALAR:"+sql);return c.getLong(0);}}
    static String text(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),"MISSING_TEXT:"+sql);return c.getString(0);}}
    static final String ID=StatsR27.sha256("CONTROLLED_LIVE_EXPERIMENT_R31"),OWNER="HOST_LIVE_OWNER_R31";
    static final long BASE=TargetIssueResolver.sequenceOrdinalSU1("202611040001")-1;
    static String issue(int n){return TargetIssueResolver.fromSequenceOrdinalSU1(BASE+n);}
    static final class World implements AutoCloseable {
        final SQLiteDatabase db,ro;final DatabaseCoordinator coordinator;final DbStore store;
        final ResearchPoolR27 pool;final ResearchPoolR27.Worker worker;final RuntimeR27 runtime;
        RuntimeCoordinatorR26.Claim claim;
        World(Path file)throws Exception{
            db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file));SQLiteOpenHelper.hostDatabase=db;
            SQLiteDatabase.scenario="BOOTSTRAP";db.execSQL("PRAGMA journal_mode=WAL");
            DbHelper helper=(DbHelper)UNSAFE.allocateInstance(DbHelper.class);
            Method base=DbHelper.class.getDeclaredMethod("createBaseTables",SQLiteDatabase.class);base.setAccessible(true);base.invoke(helper,db);
            for(String name:new String[]{"migrateDrawColumns","migrateTradeColumns"}){Method m=DbHelper.class.getDeclaredMethod(name,SQLiteDatabase.class);m.setAccessible(true);m.invoke(helper,db);}
            for(String sql:RuntimeSchemaR26.DDL)db.execSQL(sql);
            for(String sql:SchemaR27.DDL)db.execSQL(sql);
            for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);
            for(String sql:ResearchSchemaR30R1.DDL)db.execSQL(sql);
            for(String[] column:SchemaR27.COLUMNS){
                try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{column[0]})){if(!c.moveToFirst())continue;}
                boolean present=false;try(Cursor c=db.rawQuery("PRAGMA table_info("+column[0]+")",null)){while(c.moveToNext())present|=column[1].equals(c.getString(1));}
                if(!present)db.execSQL("ALTER TABLE "+column[0]+" ADD COLUMN "+column[1]+" "+column[2]);
            }
            db.beginTransaction();try{SchemaR27.ensureLiveSourceRevision(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            coordinator=(DatabaseCoordinator)UNSAFE.allocateInstance(DatabaseCoordinator.class);
            Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);
            set(coordinator,"writeLock",(ReentrantLock)lock.get(null));set(coordinator,"helper",helper);set(coordinator,"app",new android.app.Application(file.toFile()));
            set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=(DbStore)UNSAFE.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<String,HashSet<String>>());
            ResearchWriteQueueR27 queue=new ResearchWriteQueueR27(store);
            pool=(ResearchPoolR27)UNSAFE.allocateInstance(ResearchPoolR27.class);set(pool,"store",store);set(pool,"writes",queue);set(pool,"revokedTaskFencesR31",new java.util.concurrent.ConcurrentHashMap<String,Long>());
            ro=SQLiteDatabase.openDatabase(file.toString(),null,SQLiteDatabase.OPEN_READONLY);
            worker=pool.new Worker(ro,"controlled-r31-live-worker");runtime=(RuntimeR27)UNSAFE.allocateInstance(RuntimeR27.class);
            bind(1);
        }
        void bind(long fence)throws Exception {
            SQLiteDatabase.scenario="OWNER_CONTROL";long cursor=0;
            try(Cursor c=db.rawQuery("SELECT cursor FROM r26_task WHERE task_id='r27-live'",null)){if(c.moveToFirst())cursor=c.getLong(0);}
            db.execSQL("INSERT OR REPLACE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',0,0)",new Object[]{OWNER,IssueTimelineWriterR30.bootId()});
            db.execSQL("INSERT OR REPLACE INTO r26_task(task_id,lane,mechanism,cursor,fence,owner,status) VALUES('r27-live','R27','CONTROLLED_FIXTURE',?,?,?,'RUNNING')",new Object[]{cursor,fence,OWNER});
            db.execSQL("INSERT OR REPLACE INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms,next_elapsed_ms) VALUES('r27-live',?,?,?,?,0)",new Object[]{OWNER,fence,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+3600000});
            claim=new RuntimeCoordinatorR26.Claim("r27-live",OWNER,fence,cursor);RuntimeCoordinatorR26.bindBackgroundClaim(claim);
        }
        void fixture(int n,boolean missing,boolean settled)throws Exception {
            SQLiteDatabase.scenario="FIXTURE";
            if(scalar(db,"SELECT count(*) FROM r27_live_shadow WHERE policy_hash=?",ID)>0)return;
            SequentialQualificationR27.State s=new SequentialQualificationR27.State(ID,1,.15);
            db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,'{}',1,x'',1,'ACTIVE_REPAIR1','CONTROLLED',0,0,?,'','HOST')",new Object[]{ID,BASE+n});
            db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,1,?,0,0,0,0,0,0,0,-1,'ACCUMULATING',.15,0)",new Object[]{ID,s.alpha});
            db.execSQL("INSERT INTO r29_qual_calendar VALUES(?,?,0,'CONTROLLED_ORIGINAL_BOUNDARY')",new Object[]{ID,BASE+1});
            for(int o=1;o<=n;o++){
                if(!missing)draw(o);
                int act=o%4==0?0:1;int hit=settled?(o%3==0?0:1):-1;
                int a=o%3==0?4:1,b=o%3==0?5:2,c=o%3==0?6:3;
                db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit) VALUES(?,?,?,1,?,?,?,?,?,'FROZEN',1,10000,?)",new Object[]{ID,BASE+o,issue(o),StoreR27.pack(new double[]{.1,.1,.1,.1,.1,.1,.1,.1,.1,.1}),a,b,c,act,hit});
                db.execSQL("INSERT INTO r27_opportunity VALUES(?,?,1,1,?,?,0,0)",new Object[]{BASE+o,issue(o),act,settled?1:0});
            }
            if(missing)draw(n+1);
            db.execSQL("INSERT INTO r27_holdout_exposure VALUES(1,?,1)",new Object[]{ID});
            db.execSQL("INSERT INTO r27_brier_pair VALUES(?,'REPLAY','CONTROLLED',.3,.3,1,0,1)",new Object[]{issue(1)});
            db.execSQL("INSERT INTO "+DbHelper.TABLE_TRADES+"(target_issue,position,n1,n2,n3,k3_mask,action,stat_scope,settled,hit,pnl) VALUES('202610010001',1,1,2,3,7,'BET','REAL_TRADE',1,1,6.9)");
            for(int i=0;i<40;i++)db.execSQL("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,operation,category,structure_hash) VALUES(?,?,?,'CONTROLLED','CONTROLLED','CONTROLLED')",new Object[]{"R27:HOST:"+i,"HOST",ID});
        }
        void draw(int o){db.execSQL("INSERT OR IGNORE INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,1,1)",new Object[]{issue(o)});}
        void pendingFixture(int n){SQLiteDatabase.scenario="FIXTURE";for(int o=1;o<=n;o++)db.execSQL("INSERT INTO r29_pending_draw(target_ordinal,target_issue,rows,act_rows,first_seen_at,last_checked_at,checks) VALUES(?,?,1,1,1,1,1)",new Object[]{BASE+o,issue(o)});}
        void settle(int n)throws Exception{LiveShadowsR27.settle(runtime,worker,BASE+n);}
        void qualify(int n)throws Exception{LiveShadowsR27.qualify(runtime,worker,BASE+n);}
        void commit(String operation,ResearchWriteQueueR27.Batch b)throws Exception{
            // The exact production submit path is used even for an explicitly prepared fault fixture.
            String snapshot=StatsR27.sha256("CONTROLLED_PREPARED_PREFIX|"+operation+"|"+b.ops.size());
            RuntimeR27.submit(worker,operation,b,false,snapshot);
        }
        void invariants(int expected)throws Exception{
            long n=scalar(db,"SELECT n FROM r27_eprocess WHERE policy_hash=?",ID);
            need(n==expected,"WRONG_EPROCESS_N:"+n+" != "+expected);
            need(scalar(db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID)==n,"RECEIPT_NOT_EQUAL_EPROCESS_N");
            need(scalar(db,"SELECT coalesce(sum(consumed_total),0) FROM r29_qual_cursor WHERE experiment_id=?",ID)==n,"CURSOR_TOTAL_NOT_EQUAL_EPROCESS_N");
            need(scalar(db,"SELECT count(DISTINCT target_ordinal) FROM r29_qual_receipt WHERE experiment_id=?",ID)==n,"DUPLICATE_QUALIFICATION_RECEIPTS");
            if(n>0)need(scalar(db,"SELECT max(target_ordinal)-min(target_ordinal)+1 FROM r29_qual_receipt WHERE experiment_id=?",ID)==n,"NONCONTIGUOUS_RECEIPTS");
            SequentialQualificationR27.State actual=StoreR27.readEprocess(ro,ID),golden=new SequentialQualificationR27.State(ID,1,.15);
            for(int o=1;o<=expected;o++)golden.observe(BASE+o,o%4!=0,o%4!=0&&o%3!=0);
            need(Double.doubleToLongBits(actual.logUp)==Double.doubleToLongBits(golden.logUp)&&Double.doubleToLongBits(actual.logDown)==Double.doubleToLongBits(golden.logDown),"E_PROCESS_MATH_CHANGED");
            need(Double.doubleToLongBits(actual.sumU)==Double.doubleToLongBits(golden.sumU)&&Double.doubleToLongBits(actual.sumU2)==Double.doubleToLongBits(golden.sumU2),"SUFFICIENT_STATISTICS_CHANGED");
            need(actual.globalIndex==1&&Double.doubleToLongBits(actual.alpha)==Double.doubleToLongBits(golden.alpha),"EXPOSURE_OR_ALPHA_RESET");
            need(scalar(db,"SELECT count(*) FROM r27_holdout_exposure")==1,"INDEPENDENT_EXPOSURE_LEDGER_CHANGED");
            need(scalar(db,"SELECT count(*) FROM r27_brier_pair WHERE source='REPLAY'")==1,"REPLAY_MIXED_WITH_FUTURE");
            need(scalar(db,"SELECT count(*) FROM "+DbHelper.TABLE_TRADES+" WHERE action='BET' AND stat_scope='REAL_TRADE' AND settled=1")==1,"REAL_TRADE_COUNT_CHANGED");
            need("6.9".equals(text(db,"SELECT CAST(sum(pnl) AS TEXT) FROM "+DbHelper.TABLE_TRADES+" WHERE stat_scope='REAL_TRADE'")),"REAL_FUNDS_CHANGED");
        }
        void settlementInvariants(int expected,int total){
            need(scalar(db,"SELECT count(*) FROM r27_live_frozen WHERE hit>=0")==expected,"WRONG_SETTLED_PREFIX");
            need(scalar(db,"SELECT sum(settled) FROM r27_opportunity")==expected,"SETTLEMENT_SUMMARY_NOT_ATOMIC");
            need(scalar(db,"SELECT count(*) FROM r27_live_frozen")==total,"ORIGINAL_FREEZES_DROPPED");
        }
        public void close(){RuntimeCoordinatorR26.unbindBackgroundClaimR31();ro.close();db.close();Thread.interrupted();}
    }
    static void diagnostic(String phase,Runnable fault)throws Exception {
        Field field=DatabaseCoordinator.class.getDeclaredField("diagnosticFaultR31");field.setAccessible(true);@SuppressWarnings("unchecked")ThreadLocal<Object> hook=(ThreadLocal<Object>)field.get(null);
        if(phase==null){hook.remove();return;}
        Class<?> type=Class.forName("com.ethk3.novaonepro.DatabaseCoordinator$DiagnosticFaultR31");
        Object proxy=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,args)->{if(args!=null&&args.length==1&&phase.equals(args[0]))fault.run();return null;});hook.set(proxy);
    }
    static void budgets(int start,boolean expectedFail){
        long cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512);boolean over=false;int max=0;long maxBind=0,maxRows=0;
        for(Map<String,Object> frame:SQLiteDatabase.frames.subList(start,SQLiteDatabase.frames.size())) {
            int sql=((Number)frame.get("allSqlIncludingBeginEnd")).intValue();long bind=((Number)frame.get("bindBytes")).longValue(),rows=((Number)frame.get("directChangedRowsIncludingAttemptedRollback")).longValue();
            max=Math.max(max,sql);maxBind=Math.max(maxBind,bind);maxRows=Math.max(maxRows,rows);over|=sql>64||bind>cap||rows>64;
            if(!expectedFail){need(sql<=64,"R31_PHYSICAL_SQL_BUDGET_EXCEEDED:"+sql);need(bind<=cap,"R31_BIND_BUDGET_EXCEEDED:"+bind);need(rows<=64,"R31_CHANGED_ROWS_BUDGET_EXCEEDED:"+rows);need(((Number)frame.get("fenceReads")).intValue()>0,"MISSING_REAL_FENCE_CHECK");
                boolean ddl=false;for(Object item:(List<?>)frame.get("sql")){String statement=((Map<?,?>)item).get("sql").toString().trim().toUpperCase(Locale.ROOT);ddl|=statement.matches("(?s)^(CREATE|ALTER|DROP|REINDEX|VACUUM|ATTACH|DETACH)\\b.*");}need(!ddl,"BUSINESS_TRANSACTION_EXECUTED_DDL");}
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("test",expectedFail?"BASELINE_BUDGET_COUNTEREXAMPLE":"NEW_PHYSICAL_BUDGET");result.put("frames",SQLiteDatabase.frames.size()-start);result.put("maxAllSqlIncludingBeginEnd",max);result.put("maxBindBytes",maxBind);result.put("bindCap",cap);result.put("maxDirectChangedRows",maxRows);result.put("over",over);System.out.println("RESULT "+JsonR27.write(result));
        if(expectedFail)throw new AssertionError(over?"BASELINE_FAIL_REPRODUCED_R31_WRITER_BUDGET":"BASELINE_COUNTEREXAMPLE_NOT_REPRODUCED");
    }
    static void basic(Path folder,String mode)throws Exception{
        int n=mode.contains("settle")?160:mode.contains("pending")||mode.contains("resolve")?128:256;
        try(World w=new World(folder.resolve(mode+".db"))){
            boolean pending=mode.contains("pending"),resolve=mode.contains("resolve");w.fixture(n,pending,mode.contains("consume")||resolve);if(resolve)w.pendingFixture(n);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario=mode;
            if(mode.contains("consume"))w.qualify(n);else w.settle(n+(pending?1:0));
            budgets(start,mode.startsWith("baseline"));
            if(pending){need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at=0")==n,"PENDING_PREFIX_WAS_DROPPED");need(scalar(w.db,"SELECT sum(checks) FROM r29_pending_draw")==n,"PENDING_CHECKS_DUPLICATED");SQLiteDatabase.scenario="FIXTURE_BACKFILL";for(int o=1;o<=n;o++)w.draw(o);start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="pending_resolve";w.settle(n+1);budgets(start,false);need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==n,"RESOLVED_PREFIX_INCOMPLETE");}
            if(resolve)need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==n,"INDEPENDENT_RESOLUTION_INCOMPLETE");
            if(!mode.contains("consume")){need(scalar(w.db,"SELECT count(*) FROM r27_live_frozen WHERE hit>=0")==n,"SETTLEMENT_ROWS_LOST");if(!resolve)need(scalar(w.db,"SELECT sum(settled) FROM r27_opportunity")==n,"OPPORTUNITY_SUMMARY_INCOMPLETE");}
            if(!mode.contains("consume")){start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="post_settlement_consume";w.qualify(n);budgets(start,false);}
            w.invariants(n);start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="duplicate_reentry";w.settle(n);w.qualify(n);budgets(start,false);w.invariants(n);
        }System.out.println("CHECK basic_"+mode+"_all_original_rows_and_math_once");
    }
    static void rollbackCuts(Path folder)throws Exception{
        for(String edge:new String[]{"BEFORE_BODY","AFTER_BODY"})for(int cut=1;cut<=16;cut++)try(World w=new World(folder.resolve("cut-"+edge+"-"+cut+".db"))){
            w.fixture(256,false,true);SQLiteDatabase.scenario="rollback_cut_"+edge+"_"+cut;int start=SQLiteDatabase.frames.size();SQLiteDatabase.failTransaction=SQLiteDatabase.txCounter+cut;SQLiteDatabase.failSqlPrefix=edge.equals("BEFORE_BODY")?"INSERT INTO r29_qual_receipt":"INSERT INTO r31_commit_receipt";
            try{w.qualify(256);throw new AssertionError("INJECTED_ROLLBACK_NOT_REACHED:"+cut);}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"ROLLBACK_NOT_PROVEN");}finally{SQLiteDatabase.failTransaction=-1;}
            int committed=(cut-1)*16;w.invariants(committed);w.qualify(256);w.invariants(256);budgets(start,false);
        }System.out.println("CHECK every_qualification_prefix_rollback_then_reentry_exact");
    }
    static void producerCuts(Path folder)throws Exception {
        for(String path:new String[]{"settle","pending","resolve"})for(String edge:new String[]{"BEFORE_BODY","AFTER_BODY"}) {
            int n=path.equals("settle")?160:128,per=path.equals("settle")?12:8,pages=(n+per-1)/per;
            for(int cut=1;cut<=pages;cut++)try(World w=new World(folder.resolve(path+"-"+edge+"-"+cut+".db"))){
                w.fixture(n,path.equals("pending"),path.equals("resolve"));if(path.equals("resolve"))w.pendingFixture(n);
                int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario=path+"_rollback_"+edge+"_"+cut;
                SQLiteDatabase.failTransaction=SQLiteDatabase.txCounter+cut;
                SQLiteDatabase.failSqlPrefix=edge.equals("AFTER_BODY")?"INSERT INTO r31_commit_receipt":path.equals("settle")?"UPDATE r27_live_frozen":path.equals("pending")?"INSERT OR IGNORE INTO r29_pending_draw":"UPDATE r29_pending_draw SET resolved_at";
                try{w.settle(n+1);throw new AssertionError("PRODUCER_ROLLBACK_NOT_REACHED:"+path+":"+cut);}catch(DatabaseCoordinator.WriteRejectedR31 failure){need(failure.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"PRODUCER_ROLLBACK_NOT_PROVEN");}finally{SQLiteDatabase.failTransaction=-1;}
                int committed=(cut-1)*per;
                if(path.equals("settle"))w.settlementInvariants(committed,n);
                else if(path.equals("pending")){need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==committed,"PENDING_PARTIAL_COMMIT");need(scalar(w.db,"SELECT coalesce(sum(checks),0) FROM r29_pending_draw")==committed,"PENDING_ROLLBACK_CHECKS_CHANGED");}
                else need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==committed,"RESOLUTION_PARTIAL_COMMIT");
                w.invariants(0);w.settle(n+1);
                if(path.equals("settle"))w.settlementInvariants(n,n);
                else if(path.equals("pending")){need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==n,"PENDING_PREFIX_NOT_RESUMED");need(scalar(w.db,"SELECT sum(checks) FROM r29_pending_draw")==n,"PENDING_REENTRY_RECOUNTED");SQLiteDatabase.scenario="FIXTURE_BACKFILL";for(int o=1;o<=n;o++)w.draw(o);w.settle(n+1);}
                if(!path.equals("settle"))need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==n,"RESOLUTION_PREFIX_NOT_RESUMED");
                w.qualify(n);w.invariants(n);budgets(start,false);
            }
        }System.out.println("CHECK every_settlement_missing_draw_and_resolution_slice_before_after_rollback_reentry");
    }
    static void cancellationAndLateOwner(Path folder)throws Exception{
        try(World w=new World(folder.resolve("cancel-late.db"))){
            w.fixture(256,false,true);SequentialQualificationR27.State s=StoreR27.readEprocess(w.ro,ID);QualificationInboxR29.Plan p=QualificationInboxR29.plan(s,QualificationInboxR29.rows(w.ro,ID,-1,16),BASE+256,16,BASE+1);
            ResearchWriteQueueR27.Batch b=QualificationInboxR29.step(ID,s,p,-1,0);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="cancel_before";Thread.currentThread().interrupt();
            try{w.commit("R29_QUAL_CONSUME",b);throw new AssertionError("CANCELLED_COMMIT_ACCEPTED");}catch(InterruptedException good){}finally{Thread.interrupted();}
            need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt")==0,"CANCELLED_RECEIPTS_ADVANCED");
            SQLiteDatabase.scenario="cancel_during";SQLiteDatabase.interruptTransaction=SQLiteDatabase.txCounter+1;
            try{w.commit("R29_QUAL_CONSUME",b);throw new AssertionError("MID_COMMIT_CANCEL_ACCEPTED");}catch(DatabaseCoordinator.WriteRejectedR31 cancelled){need(cancelled.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"CANCEL_NOT_ROLLED_BACK");}finally{SQLiteDatabase.interruptTransaction=-1;Thread.interrupted();}
            need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt")==0,"MID_CANCEL_RECEIPTS_ADVANCED");
            RuntimeCoordinatorR26.Claim old=w.claim;w.bind(2);RuntimeCoordinatorR26.bindBackgroundClaim(old);SQLiteDatabase.scenario="late_owner";
            try{w.commit("R29_QUAL_CONSUME",b);throw new AssertionError("LATE_OWNER_ACCEPTED");}catch(DatabaseCoordinator.WriteRejectedR31 stale){need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt")==0,"LATE_OWNER_RECEIPTS_ADVANCED");}
            RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);SQLiteDatabase.scenario="new_owner_resumes";w.qualify(256);w.invariants(256);budgets(start,false);
        }System.out.println("CHECK cancel_before_mid_transaction_and_late_owner_cannot_advance_prefix");
    }
    static void sourceAndSchema(Path folder)throws Exception {
        try(World w=new World(folder.resolve("settlement-source-cas.db"))){
            w.fixture(12,false,false);ResearchWriteQueueR27.Batch b=LiveShadowsR27.settleChunks(w.ro,BASE+12,new long[1],12).get(0);int start=SQLiteDatabase.frames.size();
            SQLiteDatabase.scenario="CONTROLLED_SOURCE_CORRECTION";w.db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue=?",new Object[]{issue(1)});SQLiteDatabase.scenario="settlement_source_cas";
            try{w.commit("LIVE_SETTLE",b);throw new AssertionError("STALE_SETTLEMENT_SOURCE_COMMITTED");}catch(DatabaseCoordinator.WriteRejectedR31 stale){need(QualificationInboxR29.precondition(stale),"WRONG_SETTLEMENT_SOURCE_REJECTION");}
            w.settlementInvariants(0,12);w.settle(12);w.qualify(12);w.invariants(12);budgets(start,false);
        }
        try(World w=new World(folder.resolve("pending-source-cas.db"))){
            w.fixture(8,true,false);ResearchWriteQueueR27.Batch b=QualificationInboxR29.pendingDraws(w.ro,BASE+9);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="CONTROLLED_BACKFILL";w.draw(1);SQLiteDatabase.scenario="pending_source_cas";
            try{w.commit("LIVE_PENDING_DRAW",b);throw new AssertionError("STALE_MISSING_DRAW_COMMITTED");}catch(DatabaseCoordinator.WriteRejectedR31 stale){need(QualificationInboxR29.precondition(stale),"WRONG_PENDING_SOURCE_REJECTION");}
            need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==0,"BACKFILL_RACE_CREATED_STALE_PENDING_ROWS");w.invariants(0);w.settle(9);need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==7,"BACKFILL_RACE_LOST_OTHER_MISSING_PERIODS");budgets(start,false);
        }
        try(World w=new World(folder.resolve("qual-source-cas.db"))){
            w.fixture(16,false,true);SequentialQualificationR27.State s=StoreR27.readEprocess(w.ro,ID);QualificationInboxR29.Plan p=QualificationInboxR29.plan(s,QualificationInboxR29.rows(w.ro,ID,-1,16),BASE+16,16,BASE+1);ResearchWriteQueueR27.Batch b=QualificationInboxR29.step(ID,s,p,-1,0);int start=SQLiteDatabase.frames.size();
            SQLiteDatabase.scenario="CONTROLLED_SETTLEMENT_DRIFT";w.db.execSQL("UPDATE r27_live_frozen SET hit=0 WHERE policy_hash=? AND target_ordinal=?",new Object[]{ID,BASE+1});SQLiteDatabase.scenario="qualification_source_cas";
            try{w.commit("R29_QUAL_CONSUME",b);throw new AssertionError("STALE_QUALIFICATION_FACTS_COMMITTED");}catch(DatabaseCoordinator.WriteRejectedR31 stale){need(QualificationInboxR29.precondition(stale),"WRONG_QUAL_SOURCE_REJECTION");}w.invariants(0);
            SQLiteDatabase.scenario="CONTROLLED_ORIGINAL_RESTORED_AND_NONACT_BACKFILL";w.db.execSQL("UPDATE r27_live_frozen SET hit=1 WHERE policy_hash=? AND target_ordinal=?",new Object[]{ID,BASE+1});w.db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue=?",new Object[]{issue(4)});SQLiteDatabase.scenario="nonact_late_draw_preserves_zero_score";w.commit("R29_QUAL_CONSUME",b);w.invariants(16);budgets(start,false);
        }
        for(String object:new String[]{"r29_pending_draw","r29_qual_conflict","r27_live_frozen_order"})try(World w=new World(folder.resolve("missing-"+object+".db"))){
            w.fixture(8,false,true);SQLiteDatabase.scenario="CONTROLLED_SCHEMA_DRIFT";w.db.execSQL((object.endsWith("_order")?"DROP INDEX ":"DROP TABLE ")+object);int ddl=SQLiteDatabase.outsideDdl.size(),start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="business_schema_dependency";
            try{w.settle(8);throw new AssertionError("MISSING_SCHEMA_ACCEPTED:"+object);}catch(DatabaseCoordinator.SchemaPendingR31 missing){need(FailureDispositionR31.classify(missing).disposition==FailureDispositionR31.Disposition.DEPENDENCY,"SCHEMA_MISSING_CONSUMED_FAULT_BUDGET");}
            need(SQLiteDatabase.frames.size()==start,"SCHEMA_MISSING_ENTERED_WRITER");need(SQLiteDatabase.outsideDdl.size()==ddl,"BUSINESS_CREATED_SCHEMA");need(scalar(w.db,"SELECT count(*) FROM r27_live_frozen")==8,"SCHEMA_DEPENDENCY_DROPPED_ORIGINAL_FREEZES");need(scalar(w.db,"SELECT count(*) FROM sqlite_master WHERE name=?",object)==0,"BUSINESS_RECREATED_MISSING_SOURCE");
        }
        ResearchWriteQueueR27.Batch huge=new ResearchWriteQueueR27.Batch().add("CONTROLLED_OVERSIZE_NOT_SUBMITTED",new String(new char[300000]).replace('\0','x'));
        Method require=QualificationInboxR29.class.getDeclaredMethod("requireBudgetR31",ResearchWriteQueueR27.Batch.class,int.class);require.setAccessible(true);
        try{require.invoke(null,huge,0);throw new AssertionError("OVERSIZE_PREPARED_PAYLOAD_ACCEPTED");}catch(InvocationTargetException expected){need(FailureDispositionR31.classify(expected.getCause()).disposition==FailureDispositionR31.Disposition.DEPENDENCY,"RESOURCE_BUDGET_REJECTION_NOT_DEPENDENCY");}
        System.out.println("CHECK source_CAS_and_nonact_late_draw_schema_missing_no_DDL_and_typed_resource_dependency");
    }
    static void producerCancellationAndOwners(Path folder)throws Exception {
        for(String path:new String[]{"settle","pending","resolve"})try(World w=new World(folder.resolve("cancel-owner-"+path+".db"))){
            int n=path.equals("settle")?12:8;w.fixture(n,path.equals("pending"),path.equals("resolve"));if(path.equals("resolve"))w.pendingFixture(n);
            ResearchWriteQueueR27.Batch b=path.equals("settle")?LiveShadowsR27.settleChunks(w.ro,BASE+n,new long[1],12).get(0):QualificationInboxR29.pendingDraws(w.ro,BASE+n+1);String op=path.equals("settle")?"LIVE_SETTLE":path.equals("pending")?"LIVE_PENDING_DRAW":"LIVE_PENDING_RESOLVE";
            int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario=path+"_cancel_before";Thread.currentThread().interrupt();
            try{w.commit(op,b);throw new AssertionError("PRODUCER_CANCEL_BEFORE_ACCEPTED");}catch(InterruptedException good){}finally{Thread.interrupted();}
            SQLiteDatabase.scenario=path+"_cancel_during";SQLiteDatabase.interruptTransaction=SQLiteDatabase.txCounter+1;
            try{w.commit(op,b);throw new AssertionError("PRODUCER_MID_CANCEL_ACCEPTED");}catch(DatabaseCoordinator.WriteRejectedR31 good){need(good.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"PRODUCER_CANCEL_OUTCOME_NOT_ROLLBACK");}finally{SQLiteDatabase.interruptTransaction=-1;Thread.interrupted();}
            RuntimeCoordinatorR26.Claim old=w.claim;w.bind(2);RuntimeCoordinatorR26.bindBackgroundClaim(old);SQLiteDatabase.scenario=path+"_late_owner";
            try{w.commit(op,b);throw new AssertionError("PRODUCER_OLD_OWNER_ACCEPTED");}catch(DatabaseCoordinator.WriteRejectedR31 good){need(FailureDispositionR31.classify(good).disposition==FailureDispositionR31.Disposition.STALE_OWNER,"PRODUCER_OLD_OWNER_WRONG_CLASSIFICATION");}
            need(scalar(w.db,"SELECT count(*) FROM r31_commit_receipt")==0,"PRODUCER_CANCEL_OR_OLD_OWNER_ADVANCED_DURABLE_RECEIPT");w.invariants(0);
            if(path.equals("settle"))w.settlementInvariants(0,n);
            else if(path.equals("pending"))need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==0,"OLD_OWNER_CREATED_PENDING");
            else need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==0,"OLD_OWNER_RESOLVED_PENDING");
            RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);SQLiteDatabase.scenario=path+"_current_owner_resumes";w.settle(n+1);
            if(path.equals("pending")){SQLiteDatabase.scenario="FIXTURE_BACKFILL";for(int o=1;o<=n;o++)w.draw(o);w.settle(n+1);}
            w.qualify(n);w.invariants(n);budgets(start,false);
        }System.out.println("CHECK settlement_pending_and_resolution_cancel_and_late_owner_no_receipt_advance");
    }
    static void diagnostics(Path folder)throws Exception{
        try(World w=new World(folder.resolve("diagnostic.db"))){w.fixture(256,false,true);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="after_commit_diagnostic_failure";
            diagnostic("AFTER_COMMIT",()->{throw new IllegalStateException("CONTROLLED_POST_COMMIT_DIAGNOSTIC");});try{w.qualify(256);}finally{diagnostic(null,null);}w.invariants(256);w.qualify(256);w.invariants(256);budgets(start,false);
            need(((Number)DatabaseCoordinator.writeProtocolSnapshotR31().get("diagnosticFailures")).longValue()>0,"DIAGNOSTIC_FAILURE_NOT_OBSERVED");
        }System.out.println("CHECK committed_diagnostic_failure_never_duplicates_receipt_or_math");
        for(String path:new String[]{"settle","pending","resolve"})try(World w=new World(folder.resolve("diagnostic-"+path+".db"))){
            int n=path.equals("settle")?160:128;w.fixture(n,path.equals("pending"),path.equals("resolve"));if(path.equals("resolve"))w.pendingFixture(n);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="after_commit_diagnostic_"+path;
            diagnostic("AFTER_COMMIT",()->{throw new IllegalStateException("CONTROLLED_POST_COMMIT_DIAGNOSTIC");});try{w.settle(n+1);}finally{diagnostic(null,null);}
            if(path.equals("pending")){need(scalar(w.db,"SELECT sum(checks) FROM r29_pending_draw")==n,"COMMITTED_PENDING_DIAGNOSTIC_RECOUNTED");SQLiteDatabase.scenario="FIXTURE_BACKFILL";for(int o=1;o<=n;o++)w.draw(o);w.settle(n+1);}
            if(path.equals("resolve"))need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==n,"COMMITTED_RESOLUTION_DIAGNOSTIC_DROPPED");
            if(!path.equals("resolve"))w.settlementInvariants(n,n);w.qualify(n);w.invariants(n);w.settle(n);w.qualify(n);w.invariants(n);budgets(start,false);
        }
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");String mode=args[0];Path folder=Paths.get(args[1]);Files.createDirectories(folder);
        if(mode.startsWith("baseline")||mode.equals("settle")||mode.equals("pending")||mode.equals("consume")||mode.equals("resolve")){basic(folder,mode);return;}
        if(mode.startsWith("halt")||mode.startsWith("resume")){
            String path=mode.contains("-")?mode.substring(mode.indexOf('-')+1):"consume";int n=path.equals("consume")?256:path.equals("settle")?160:128;
            try(World w=new World(folder.resolve("halt-"+path+".db"))){boolean fresh=scalar(w.db,"SELECT count(*) FROM r27_live_shadow")==0;w.fixture(n,path.equals("pending"),path.equals("consume")||path.equals("resolve"));if(fresh&&path.equals("resolve"))w.pendingFixture(n);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario=mode;
                if(mode.startsWith("halt")){diagnostic("AFTER_COMMIT",()->{System.out.flush();Runtime.getRuntime().halt(73);});if(path.equals("consume"))w.qualify(n);else w.settle(n+1);throw new AssertionError("HALT_NOT_REACHED");}
                need(scalar(w.db,"SELECT count(*) FROM r31_commit_receipt")==1,"HALT_DID_NOT_PRESERVE_EXACTLY_ONE_DURABLE_COMMIT");
                if(path.equals("consume"))w.invariants(16);
                else if(path.equals("settle"))w.settlementInvariants(12,n);
                else if(path.equals("pending"))need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==8,"HALT_LOST_COMMITTED_PENDING_PAGE");
                else need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0")==8,"HALT_LOST_COMMITTED_RESOLVED_PAGE");
                if(!path.equals("consume"))w.settle(n+1);
                if(path.equals("pending")){need(scalar(w.db,"SELECT count(*) FROM r29_pending_draw")==n,"HALT_RESUME_LOST_PENDING_ROWS");SQLiteDatabase.scenario="FIXTURE_BACKFILL";for(int o=1;o<=n;o++)w.draw(o);w.settle(n+1);}
                w.qualify(n);w.invariants(n);w.settle(n);w.qualify(n);w.invariants(n);budgets(start,false);System.out.println("CHECK real_jvm_halt_after_commit_fresh_process_reentry_"+path);
            }return;
        }
        rollbackCuts(folder);producerCuts(folder);cancellationAndLateOwner(folder);producerCancellationAndOwners(folder);sourceAndSchema(folder);diagnostics(folder);
        System.out.println("LIVE_BUDGET_NATIVE_HOST_PASS assertions="+assertions+" scope=CONTROLLED_NATIVE_SQLITE_NOT_ANDROID_NOT_NATURAL_QUALITY");
    }
}
