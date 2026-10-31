package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import sun.misc.Unsafe;

/** Exercises actual policy freezing and call receipts, including a source commit between preparation and writer. */
public final class ModelSurfaceR30HostTest {
    static void require(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261030%04d",n);}
    static void add(Connection c,int n)throws Exception{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)")){
            p.setString(1,issue(n));for(int k=1;k<=10;k++)p.setInt(k+1,(k+n)%10+1);p.setLong(12,System.currentTimeMillis());p.executeUpdate();
        }
    }
    static long count(SQLiteDatabase db,String sql){return ModelSurfaceStoreR30.count(db,sql);}
    static PolicySpecR27 spec(){return new PolicySpecR27(PolicyAstR27.op("add",PolicyAstR27.op("zscore",PolicyAstR27.leaf("model_surface",1)),PolicyAstR27.op("zscore",PolicyAstR27.leaf("model_surface",2))),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I08","TEST",0,30,"actual model consumer host fixture"));}
    public static void main(String[] args)throws Exception{
        String mode=args.length==0?"valid":args[0];Class.forName("org.sqlite.JDBC");
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);try(Statement s=c.createStatement()){
                for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);s.execute(HistoryImportR27.DDL);s.execute(ModelArtifactR29.DDL);for(String ddl:ModelSurfaceStoreR30.DDL)s.execute(ddl);
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,open_time INT,dirty INT)");
            }
            for(int n=1;n<=100;n++)add(c,n);c.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);c.commit();c.setAutoCommit(true);
            ReplayDatasetR27 data=StoreR27.loadDataset(db,100);long latest=data.ordinals[99],deadline=RuntimeR27.latestOpen(db)+60_000L;
            PolicySpecR27 spec=spec();db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES(?,?,1,?,1,'ACTIVE','HOST',1,1)",new Object[]{"model-fixture",spec.fullJson(),new byte[80]});
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                try{
                    if(mode.equals("race")&&op.equals("LIVE_FREEZE"))add(c,101);
                    c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,op);c.commit();c.setAutoCommit(true);
                }catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
            },()->false);u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);
            ResearchPoolR27.Worker worker=pool.new Worker(db,"model-surface-test");
            if(!mode.equals("missing")){
                AtomNetSessionR30 state=new AtomNetSessionR30(data.datasetHash,issue(99),latest-1,182,10,10,0);
                AtomReplayR27.Frame train=AtomReplayR27.build(data,70,72);
                while(!state.ready())state.work(index->{int pos=index%10+1;return new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(AtomReplayR27.features(train.values[index],train.present[index],true),pos),train.hit[index],train.actual[index]-1,AtomNetSessionR30.eventKey(data.issues[70+index/10],pos));});
                rt.atomSessionR30=state;
                if(mode.equals("target"))rt.atomSessionR30=new AtomNetSessionR30(data.datasetHash,issue(101),latest+1,182,10,10,0);
                if(mode.equals("target")){rt.atomSessionR30.phase="ONLINE";rt.atomSessionR30.completeIssue=issue(101);}
            }
            LiveShadowsR27.freeze(rt,worker,issue(100),latest,deadline,"('ACTIVE')",true);
            Map<String,Object> report=ModelSurfaceStoreR30.report(db);
            boolean valid=mode.equals("valid");
            require(count(db,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN'")==(valid?1:0),"INVALID_FREEZE_COUNT:"+mode+":"+report);
            require(count(db,"SELECT count(*) FROM r30_model_call WHERE status='MATCHED'")==(valid?2:0),"CALLS_NOT_ONE_PER_ACTUAL_SOURCE_POSITION:"+report);
            if(!valid){require(count(db,"SELECT count(*) FROM r30_model_call WHERE status='BLOCKED' AND reason<>''")==2,"BLOCKED_REASON_MISSING:"+mode);require("BLOCKED".equals(report.get("status")),"NOT_BLOCKED:"+report);}
            if(valid){
                require("MATCHED".equals(report.get("status"))&&((Number)report.get("matchingRatio")).doubleValue()==1,"M24_NOT_MATCHED:"+report);
                LiveShadowsR27.freeze(rt,worker,issue(100),latest,deadline,"('ACTIVE')",true);require(count(db,"SELECT count(*) FROM r30_model_call WHERE status='MATCHED'")==2,"DUPLICATE_CALLS_ON_RETRY");
                ReplayDatasetR27 withTarget=data.append(new ReplayDatasetR27.Row(issue(101),latest+1,new int[]{1,2,3,4,5,6,7,8,9,10}));FeatureCacheR27 cache=new FeatureCacheR27(withTarget);ModelSurfaceStoreR30.bindHistorical(db,cache);
                require(cache.modelSurface(100,1)!=null&&cache.modelSurface(100,2)!=null,"VALID_ARCHIVED_SURFACE_NOT_RESTORED");
                int[][] changed=withTarget.draws.clone();changed[99]=new int[]{10,9,8,7,6,5,4,3,2,1};FeatureCacheR27 bad=new FeatureCacheR27(withTarget.withDraws(changed));ModelSurfaceStoreR30.bindHistorical(db,bad);
                require(bad.modelSurface(100,1)==null&&bad.modelBlockReason(100,1).equals("ARCHIVED_HISTORY_MISMATCH"),"HISTORY_TAMPER_NOT_BLOCKED");
                db.execSQL("UPDATE r30_model_surface SET probability_hash='bad' WHERE position=1");FeatureCacheR27 corrupt=new FeatureCacheR27(withTarget);ModelSurfaceStoreR30.bindHistorical(db,corrupt);require(corrupt.modelSurface(100,1)==null,"CORRUPT_ARCHIVE_ACCEPTED");
                db.execSQL("UPDATE r30_model_call SET target_ordinal=target_ordinal+1 WHERE position=2 AND status='MATCHED'");require("FAIL".equals(ModelSurfaceStoreR30.report(db).get("status")),"ORPHAN_CALL_NOT_DETECTED");
                System.out.println("R30_MODEL_SQLITE_REPORT "+JsonR27.write(report));
                System.out.println("R30_MODEL_SQLITE|TWO_ACTUAL_MODEL_POSITIONS_ONE_FREEZE|M24_100_PERCENT|RETRY_NO_DUPLICATE|ARCHIVE_PIT_RECOVERED|HISTORY_AND_HASH_TAMPER_BLOCKED|ORPHAN_DETECTED|PASS");
            }else System.out.println("R30_MODEL_SQLITE|"+mode.toUpperCase(java.util.Locale.ROOT)+"|NO_ILLEGAL_FREEZE|TWO_EXPLICIT_BLOCKED_CALLS|PASS");
        }
    }
}
