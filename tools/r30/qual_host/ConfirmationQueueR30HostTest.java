package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Unsafe;

/** Actual refreshObservers and contiguous consumer on SQLite. Synthetic evidence checks mechanics only. */
public final class ConfirmationQueueR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static PolicySpecR27 policy(int n){return new PolicySpecR27(PolicyAstR27.leaf("freq",n%10+1,32+(n/10)*8),n%10+1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","TEST",0,30,"synthetic queue fixture"));}
    static String state(SQLiteDatabase db){List<Object> out=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT policy_hash,global_index,n,acts,hits,last_ordinal,state,log_e_up,log_e_down,sum_u,sum_u2 FROM r27_eprocess ORDER BY policy_hash",null)){while(c.moveToNext()){List<String> row=new ArrayList<>();for(int i=0;i<11;i++)row.add(c.getString(i));out.add(row);}}return JsonR27.write(out);}
    static String observers(SQLiteDatabase db){List<Object> out=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT policy_hash,status,reason,updated_at FROM r27_live_shadow ORDER BY policy_hash",null)){while(c.moveToNext())out.add(Arrays.asList(c.getString(0),c.getString(1),c.getString(2),c.getLong(3)));}return JsonR27.write(out);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,dirty INT)");db.execSQL("INSERT INTO draws(issue,dirty) VALUES('202610310001',0)");
            db.execSQL("CREATE TABLE r26_evolution(policy_hash TEXT,prospective_qualified INT)");
            for(int i=0;i<4;i++)StoreR27.insertShadow(db,policy(i),1,"HOST","host-abi",policy(i).policyHash(),policy(i).policyHash());
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            RuntimeR27 runtime=(RuntimeR27)u.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);AtomicBoolean fail=new AtomicBoolean(false);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                try{connection.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,op);if(fail.getAndSet(false))throw new IllegalStateException("QUEUE_COMMIT_FAILURE");connection.commit();connection.setAutoCommit(true);}
                catch(Exception e){try{connection.rollback();connection.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
            },()->false);u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);ResearchPoolR27.Worker worker=pool.new Worker(db,"queue-host");
            Map<String,String> expected=new LinkedHashMap<>(QualificationInboxR29.confirmationQueueR30(db));require(expected.size()==4,"REGISTERED_QUEUE_MISSING");
            // Register four non-ACT opportunities and actually consume them before re-ranking.
            long first=QualificationInboxR29.calendarStart(db,policy(0).policyHash());
            for(String id:expected.keySet())for(int k=0;k<4;k++)db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,1,?,1,2,3,0,'SKIPPED_GAP',1,2)",new Object[]{id,first+k,"queue-"+k,StoreR27.pack(new double[10])});
            QualificationInboxR29.Commit sink=(op,batch)->writes.submit(op,batch,()->false);
            QualificationInboxR29.consume(db,sink,LiveShadowsR27.LIVE_STATES,first+3);
            require(count(db,"SELECT sum(n) FROM r27_eprocess")==16&&count(db,"SELECT count(*) FROM r29_qual_receipt")==16,"ACTUAL_CONTIGUOUS_CONSUMER_NOT_USED");
            String before=state(db);
            EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;
            EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,new FeatureCacheR27(ReplayDatasetR27.synthetic(5000,17,new int[0])),null,()->0L);
            for(int generation=1;generation<=100;generation++){
                loop.generation=generation;loop.population.clear();
                for(int j=0;j<60;j++){int n=(generation+j)%200;EvolutionLoopR27.Member m=new EvolutionLoopR27.Member();m.spec=policy(n);m.tau=1;m.u=60-j;loop.population.add(m);}
                runtime.refreshObservers(worker,loop);
                require(expected.equals(QualificationInboxR29.confirmationQueueR30(db)),"CONFIRMATION_EVICTED_G"+generation);
                require(count(db,"SELECT count(*) FROM r27_live_shadow WHERE status='OBSERVE'")==20,"EXPLORATION_IGNORED_CONFIRMATION_CAP_G"+generation);
                require(count(db,"SELECT count(*) FROM r27_live_shadow o JOIN r27_live_shadow a ON o.base_policy=a.base_policy WHERE o.status='OBSERVE' AND a.status='ACTIVE'")==0,"DUPLICATE_ACTIVE_BASE_USED_EXPLORATION_SLOT");
                require(before.equals(state(db)),"RANKING_CHANGED_QUALIFICATION_G"+generation);
            }
            String rows=observers(db);loop.generation++;Collections.reverse(loop.population);for(int i=0;i<loop.population.size();i++)loop.population.get(i).u=60-i;
            fail.set(true);try{runtime.refreshObservers(worker,loop);throw new AssertionError("FAILED_COMMIT_NOT_INJECTED");}catch(IllegalStateException expectedFailure){}
            require(rows.equals(observers(db))&&before.equals(state(db)),"FAILED_COMMIT_CHANGED_ROWS_OR_CURSOR");
            runtime.refreshObservers(worker,loop);
            QualificationInboxR29.consume(db,sink,LiveShadowsR27.LIVE_STATES,first+3);
            require(before.equals(state(db))&&count(db,"SELECT count(*) FROM r29_qual_receipt")==16,"RERANK_REPLAY_DUPLICATED_RECEIPTS");
            db.execSQL("UPDATE r27_eprocess SET state=? WHERE policy_hash=?",new Object[]{SequentialQualificationR27.QUALIFIED,policy(0).policyHash()});
            db.execSQL("UPDATE r27_eprocess SET state=? WHERE policy_hash=?",new Object[]{SequentialQualificationR27.NOT_BETTER,policy(1).policyHash()});runtime.refreshObservers(worker,loop);
            require(QualificationInboxR29.confirmationQueueR30(db).size()==2&&count(db,"SELECT count(*) FROM r27_live_shadow WHERE status='OBSERVE'")==22,"TERMINAL_RESULT_DID_NOT_RELEASE_EXPLORATION_SLOT");
            // Legacy installations can already have more than 24 active registrations: preserve all of them.
            for(int i=200;i<231;i++)StoreR27.insertShadow(db,policy(i),1,"HOST","host-abi",policy(i).policyHash(),policy(i).policyHash());
            Map<String,String> oversized=new LinkedHashMap<>(QualificationInboxR29.confirmationQueueR30(db));runtime.refreshObservers(worker,loop);
            require(oversized.size()==33&&oversized.equals(QualificationInboxR29.confirmationQueueR30(db))&&count(db,"SELECT count(*) FROM r27_live_shadow WHERE status='OBSERVE'")==0,"OVERCAPACITY_REGISTRATION_WAS_EVICTED");
            System.out.println("R30_CONFIRM_QUEUE_SQLITE|100_REAL_RERANKS|M29_EVICTIONS_0|CAP_24_MINUS_CONFIRMED|NO_DUPLICATE_BASE|CURSOR_ALPHA_RECEIPTS_UNCHANGED|FAILED_COMMIT_ROLLED_BACK|TERMINAL_RELEASE|LEGACY_OVERCAP_PRESERVED|PASS");
        }
    }
}
