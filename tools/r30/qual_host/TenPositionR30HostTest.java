package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Unsafe;

/** Actual ten-position production task, atomic checkpoint rollback, legacy preservation, and JVM death mid-issue. */
public final class TenPositionR30HostTest {
    static void require(boolean yes,String code){if(!yes)throw new AssertionError(code);}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261030%04d",n);}
    static void add(Connection c,int n)throws Exception{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)")){
            p.setString(1,issue(n));for(int j=1;j<=10;j++)p.setInt(j+1,(j+n)%10+1);p.setLong(12,System.currentTimeMillis());p.executeUpdate();
        }
    }
    static void commit(Connection c,SQLiteDatabase db,ResearchWriteQueueR27.Batch batch,boolean fail){
        try{c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,"ten-position-test");if(fail)throw new IllegalStateException("INJECTED_ROLLBACK");c.commit();c.setAutoCommit(true);}
        catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
    }
    static byte[] artifact(SQLiteDatabase db){try(android.database.Cursor c=db.rawQuery("SELECT artifact FROM r29_model_checkpoint WHERE owner=?",new String[]{AtomNetSessionR30.OWNER})){require(c.moveToFirst(),"STATE_MISSING");return c.getBlob(0);}}
    static String text(SQLiteDatabase db,String sql){try(android.database.Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    public static void main(String[] args)throws Exception{
        if(args.length==0){Path db=Files.createTempFile("r30-ten-position-",".db");try{child("write-and-kill",db);child("restart-and-check",db);}finally{Files.deleteIfExists(db);}return;}
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+args[1])){
            SQLiteDatabase db=new SQLiteDatabase(c);boolean initial=args[0].equals("write-and-kill");
            if(initial){try(Statement s=c.createStatement()){
                for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);
                s.execute(ModelArtifactR29.DDL);s.execute(HistoryImportR27.DDL);for(String ddl:AtomNetStoreR30.DDL)s.execute(ddl);for(String ddl:ModelSurfaceStoreR30.DDL)s.execute(ddl);
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,open_time INTEGER,dirty INTEGER)");
            }
            for(int n=1;n<=80;n++)add(c,n);
            c.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);c.commit();c.setAutoCommit(true);
            AtomNetTrainerR27 legacy=new AtomNetTrainerR27(172,5);legacy.onlineLr=.0001;
            ResearchWriteQueueR27.Batch old=new ResearchWriteQueueR27.Batch();AtomWorldR27.checkpoint(old,new ModelArtifactR29(legacy,new FtrlR27(7,.05,1,1e-4,1e-3),"legacy",issue(79)),1);commit(c,db,old,false);
            }
            String legacyHash=text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'");
            Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);Unsafe u=(Unsafe)field.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);
            AtomicBoolean fail=new AtomicBoolean(false);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,b)->commit(c,db,b,op.equals("ATOMNET_R30_SLICE")&&fail.getAndSet(false)),()->false);
            u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);
            ResearchPoolR27.Worker worker=pool.new Worker(db,"ten-position-test");
            rt.evoData=StoreR27.loadDataset(db,100);
            if(initial){
                int slices=0;do{
                    AtomNetSessionR30 prior=rt.atomSessionR30==null?AtomNetStoreR30.restore(db):rt.atomSessionR30;
                    long before=prior==null?0:prior.materializedSamples;
                    AtomNetRuntimeR30.step(rt,worker);slices++;
                    require(rt.atomSessionR30.materializedSamples-before<=120,"CACHE_SLICE_EXCEEDED_ORIGINAL_BUDGET");
                    if(slices%13==0){rt.atomSessionR30=null;rt.atomDataR30=null;}
                    require(slices<2000,"PRETRAIN_STUCK");
                }while(rt.atomSessionR30==null||!rt.atomSessionR30.ready());
                require("560".equals(text(db,"SELECT count(*) FROM r30_atomnet_sample")),"SAMPLES_NOT_PERIODS_TIMES_TEN");
                require(legacyHash.equals(text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'")),"LEGACY_OVERWRITTEN");
                require("10".equals(text(db,"SELECT count(*) FROM r30_atomnet_forecast")),"TEN_FUTURE_PREDICTIONS_NOT_FROZEN");
                require(rt.atomSessionR30.trainer.net.in==182,"PRODUCTION_ONE_HOT_NOT_CONSUMED");
                add(c,81);AtomNetRuntimeR30.step(rt,worker);
                require(rt.atomSessionR30.nextPosition==2,"POSITION_ONE_NOT_COMMITTED");
                byte[] before=artifact(db),memory=rt.atomSessionR30.encode();fail.set(true);
                try{AtomNetRuntimeR30.step(rt,worker);throw new AssertionError("FAILURE_NOT_INJECTED");}catch(IllegalStateException expected){}
                require(Arrays.equals(before,artifact(db))&&Arrays.equals(memory,rt.atomSessionR30.encode()),"MODEL_CURSOR_NOT_ATOMIC_ON_ROLLBACK");
                for(int p=2;p<=5;p++)AtomNetRuntimeR30.step(rt,worker);
                require(rt.atomSessionR30.nextPosition==6&&rt.atomSessionR30.partialIssue.equals(issue(81)),"MID_ISSUE_CURSOR");
                AtomNetSessionR30 expected=AtomNetSessionR30.decode(artifact(db),StatsR27.sha256(artifact(db)));
                ReplayDatasetR27 data=StoreR27.loadDataset(db,100);AtomReplayR27.Frame frame=AtomReplayR27.build(data,80,81);
                for(int p=6;p<=10;p++){
                    double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p-1],frame.present[p-1],true),p);
                    expected.online(new AtomNetTrainerR27.Sample(x,frame.hit[p-1],frame.actual[p-1]-1,AtomNetSessionR30.eventKey(issue(81),p)),issue(81),data.ordinals[80],p);
                }
                ResearchWriteQueueR27.Batch receipt=new ResearchWriteQueueR27.Batch();receipt.add("INSERT OR REPLACE INTO r27_chart_cache VALUES('host','expected',?,1)",StatsR27.sha256(expected.encode()));commit(c,db,receipt,false);
                System.out.println("R30_TEN_SQLITE|PRETRAIN_SLICES="+slices+"|560_SAMPLES_56_PERIODS|182_INPUTS|10_FROZEN_POSITIONS|FAILED_COMMIT_ROLLED_BACK|HALT_AFTER_POSITION_5");System.out.flush();Runtime.getRuntime().halt(0);
            }
            AtomNetRuntimeR30.step(rt,worker);require(rt.atomSessionR30.nextPosition==6,"RESTORE_LOST_POSITION_CURSOR");
            for(int p=6;p<=10;p++)AtomNetRuntimeR30.step(rt,worker);
            require(rt.atomSessionR30.trainer.onlineSteps==10&&rt.atomSessionR30.completeIssue.equals(issue(81)),"TEN_ONLINE_POSITIONS_NOT_ONCE");
            require(StatsR27.sha256(artifact(db)).equals(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='host' AND key='expected'")),"PRODUCTION_RESTART_NOT_EXACT");
            byte[] settled=artifact(db);AtomNetRuntimeR30.step(rt,worker);require(Arrays.equals(settled,artifact(db)),"DUPLICATE_LEARNING_AFTER_RETRY");
            require(legacyHash.equals(text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'")),"LEGACY_NOT_PRESERVED");
            Map<?,?> report=(Map<?,?>)JsonR27.parse(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='atomnet' AND key='ten-position-r30'"));
            require(((Number)report.get("materializedSamples")).longValue()==560&&((Number)report.get("onlineSamples")).longValue()==10,"REPORT_DOES_NOT_MATCH_COMMITTED_TRAINING");
            System.out.println("R30_TEN_SQLITE|REAL_JVM_RESTART_POSITION_6_TO_10_EXACT|NO_DUPLICATE|LEGACY_BYTES_PRESERVED|THROUGHPUT_REPORT_MATCHES|PASS");
            System.out.println("R30_TEN_SQLITE_REPORT "+JsonR27.write(report));
        }
    }
    static void child(String mode,Path db)throws Exception{
        Process p=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx768m","-cp",System.getProperty("java.class.path"),TenPositionR30HostTest.class.getName(),mode,db.toString()).redirectErrorStream(true).start();
        if(!p.waitFor(120,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("TEN_CHILD_TIMEOUT:"+mode);}
        String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);System.out.print(output);require(p.exitValue()==0,"TEN_CHILD_FAILED:"+mode+":"+p.exitValue());
    }
}
