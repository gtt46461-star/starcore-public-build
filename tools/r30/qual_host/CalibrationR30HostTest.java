package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Unsafe;

/** Calls the production calibration task on real SQLite with explicitly synthetic FUTURE receipts. */
public final class CalibrationR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261030%04d",n);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    @SuppressWarnings("unchecked")static Map<String,Object> cached(SQLiteDatabase db,String policy){try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='calibration-r30' AND key=?",new String[]{policy})){require(c.moveToFirst(),"Q05_CACHE_MISSING");return(Map<String,Object>)JsonR27.parse(c.getString(0));}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);try(Statement s=c.createStatement()){
                for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);s.execute(HistoryImportR27.DDL);
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,dirty INT)");
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,0)")){
                for(int i=1;i<=1000;i++){p.setString(1,issue(i));for(int j=1;j<=10;j++)p.setInt(j+1,(i+j-1)%10+1);p.executeUpdate();}
            }
            ReplayDatasetR27 data=StoreR27.loadDataset(db,1000);
            PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","TEST",0,30,"synthetic calibration host fixture"));
            double[] p=new double[10];Arrays.fill(p,.1);
            for(String owner:new String[]{"calibration-a","calibration-b"}){
                db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES(?,?,1,?,?,'ACTIVE','HOST',1,1)",new Object[]{owner,spec.fullJson(),new byte[80],owner.endsWith("a")?1:2});
                for(int i=600;i<=1000;i++){
                    long ordinal=data.ordinals[i-1],open=1_000_000L+i*60_000L;int actual=data.draws[i-1][0],hit=actual<=3?1:0;
                    db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES(?,?,?,1,?,1,2,3,1,'FROZEN',?,?,?,?)",new Object[]{owner,ordinal,issue(i),StoreR27.pack(p),open-5000,open,hit,open+1000});
                }
            }
            db.execSQL("UPDATE r27_live_frozen SET probabilities=? WHERE policy_hash='calibration-a' AND target_issue=?",new Object[]{new byte[0],issue(1000)});
            db.execSQL("UPDATE r27_live_frozen SET target_ordinal=target_ordinal+20000 WHERE policy_hash='calibration-a' AND target_issue=?",new Object[]{issue(998)});
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);AtomicBoolean fail=new AtomicBoolean(false);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                try{c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,op);if(fail.getAndSet(false))throw new IllegalStateException("CALIBRATION_COMMIT_FAILURE");c.commit();c.setAutoCommit(true);}
                catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
            },()->false);u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);ResearchPoolR27.Worker worker=pool.new Worker(db,"calibration-test");
            EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;rt.evo=new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new StoreR27.EvolutionSink("HOST",issue(1000)),()->0L);rt.evo.stepDataset();
            LiveShadowsR27.lastCalibration=0;LiveShadowsR27.calibrate(rt,worker);
            require(count(db,"SELECT count(*) FROM r27_calibration")==1&&rt.calibrationIndexR30==1,"MORE_THAN_ONE_POLICY_PER_SLICE");
            Map<String,Object> a=cached(db,"calibration-a");require(((Number)a.get("n")).longValue()>=200,"Q05_STILL_CAPPED_AT_120");require(((Number)a.get("excludedInvalid")).longValue()==2,"CORRUPT_PROBABILITY_OR_ORDINAL_NOT_EXPLICITLY_EXCLUDED");
            List<?> rows=(List<?>)a.get("rollingEvaluations");for(Object value:rows){Map<?,?> e=(Map<?,?>)value;require(((Number)e.get("fitThroughOrdinal")).longValue()<((Number)e.get("ordinal")).longValue()-1,"FUTURE_USED_IN_ROLLING_FIT");}
            fail.set(true);try{LiveShadowsR27.calibrate(rt,worker);throw new AssertionError("COMMIT_FAILURE_NOT_INJECTED");}catch(IllegalStateException expected){}
            require(rt.calibrationIndexR30==1&&count(db,"SELECT count(*) FROM r27_calibration")==1&&count(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='calibration-r30'")==1,"FAILED_COMMIT_ADVANCED_CALIBRATION");
            LiveShadowsR27.calibrate(rt,worker);require(rt.calibrationIndexR30==0&&count(db,"SELECT count(*) FROM r27_calibration")==2,"RETRY_DID_NOT_FINISH_ROUND");
            Map<String,Object> report=ReportR29.calibrationQualityR30(db);require(((Number)report.get("measuredPolicies")).longValue()==2,"REPORT_NOT_FROM_COMMITTED_Q05");
            try(Cursor old=db.rawQuery("SELECT ece,validation_rows FROM r27_calibration WHERE policy_hash='calibration-a'",null)){require(old.moveToFirst()&&old.getDouble(0)==((Number)a.get("eceEqualWidth")).doubleValue()&&old.getLong(1)==((Number)a.get("n")).longValue(),"LEGACY_AND_NEW_REPORT_DISAGREE");}
            require(a.get("eceEqualMass") instanceof Number&&a.get("brier") instanceof Number&&a.get("bss") instanceof Number&&a.get("supportCoverage") instanceof Number,"Q05_FIELDS_MISSING");
            Map<String,Object> summary=new LinkedHashMap<>(a);summary.remove("rollingEvaluations");summary.put("scope","HOST_SYNTHETIC_ONLY_NOT_NATURAL_QUALITY");System.out.println("R30_CALIBRATION_SQLITE_REPORT "+JsonR27.write(summary));
            System.out.println("R30_CALIBRATION_SQLITE|ACTUAL_TASK_ONE_POLICY_PER_SLICE|STRICT_PRIOR_ORDINAL_AND_EMBARGO|Q05_BOTH_ECE_BRIER_BSS_SUPPORT|CORRUPT_ROW_EXCLUDED|FAILED_COMMIT_ROLLED_BACK_AND_RETRIED|OLD_AND_NEW_REPORT_MATCH|PASS");
        }
    }
}
