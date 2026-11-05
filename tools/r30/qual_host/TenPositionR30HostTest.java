package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual ten-position production task, atomic checkpoint rollback, legacy preservation, and JVM death mid-issue. */
public final class TenPositionR30HostTest {
    static final int STOP_POSITION=Integer.getInteger("r30.host.stopPosition",5);
    static void require(boolean yes,String code){if(!yes)throw new AssertionError(code);}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261030%04d",n);}
    static void add(Connection c,int n)throws Exception{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0,?)")){
            p.setString(1,issue(n));for(int j=1;j<=10;j++)p.setInt(j+1,(j+n)%10+1);p.setLong(12,System.currentTimeMillis());p.setLong(13,System.currentTimeMillis());p.executeUpdate();
        }
    }
    static void commit(Connection c,SQLiteDatabase db,ResearchWriteQueueR27.Batch batch,boolean fail){
        try{c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,"ten-position-test");if(fail)throw new IllegalStateException("INJECTED_ROLLBACK");c.commit();c.setAutoCommit(true);}
        catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
    }
    static byte[] artifact(SQLiteDatabase db){try(android.database.Cursor c=db.rawQuery("SELECT artifact FROM r29_model_checkpoint WHERE owner=?",new String[]{AtomNetSessionR30.OWNER})){require(c.moveToFirst(),"STATE_MISSING");return c.getBlob(0);}}
    // R31: the checkpoint row may hold a verified file-artifact reference; resolve through the production reader.
    static byte[] resolved(SQLiteDatabase db){return CheckpointArtifactsR26.resolveSerializedR31(db,artifact(db));}
    static String text(SQLiteDatabase db,String sql){try(android.database.Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    public static void main(String[] args)throws Exception{
        if(args.length==0){Path db=Files.createTempFile("r30-ten-position-",".db");try{long oldPid=child("write-and-kill",db,-1);child("restart-and-check",db,oldPid);}finally{Files.deleteIfExists(db);}return;}
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+args[1])){
            SQLiteDatabase db=new SQLiteDatabase(c);boolean initial=args[0].equals("write-and-kill");
            if(initial){try(Statement s=c.createStatement()){
                for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);
                s.execute(ModelArtifactR29.DDL);s.execute(ModelArtifactR29.LEGACY_DDL);for(String ddl:ResearchSchemaR30R1.DDL)s.execute(ddl);s.execute(HistoryImportR27.DDL);for(String ddl:AtomNetStoreR30.DDL)s.execute(ddl);for(String ddl:ModelSurfaceStoreR30.DDL)s.execute(ddl);
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,open_time INTEGER,dirty INTEGER,first_seen_at INTEGER)");
            }
            for(int n=1;n<=80;n++)add(c,n);
            c.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);c.commit();c.setAutoCommit(true);
            AtomNetTrainerR27 legacy=new AtomNetTrainerR27(172,5);legacy.onlineLr=.0001;
            ResearchWriteQueueR27.Batch old=new ResearchWriteQueueR27.Batch();AtomWorldR27.checkpoint(old,new ModelArtifactR29(legacy,new FtrlR27(7,.05,1,1e-4,1e-3),"legacy",issue(79)),1);commit(c,db,old,false);
            }
            String legacyHash=text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'");
            try(R31OwnedRuntimeFixture owned=new R31OwnedRuntimeFixture(db,"r27-atomnet")){
            RuntimeR27 rt=owned.runtime;ResearchPoolR27.Worker worker=owned.worker;
            if(!initial)owned.recoverAfterConfirmedProcessExit(Long.getLong("r31.host.previousPid",-1));
            rt.evoData=StoreR27.loadDataset(db,100);
            if(initial){
                int slices=0;do{
                    AtomNetSessionR30 prior=rt.atomSessionR30==null?AtomNetStoreR30.restore(db):rt.atomSessionR30;
                    long before=prior==null?0:prior.materializedSamples;
                    owned.owned(()->AtomNetRuntimeR30.step(rt,worker));slices++;
                    require(rt.atomSessionR30.materializedSamples-before<=120,"CACHE_SLICE_EXCEEDED_ORIGINAL_BUDGET");
                    if(slices%13==0){rt.atomSessionR30=null;rt.atomDataR30=null;}
                    require(slices<2000,"PRETRAIN_STUCK");
                }while(rt.atomSessionR30==null||!rt.atomSessionR30.ready());
                // R31: new materialized samples are published through r31_atomnet_sample_stage; count the union exactly once.
                require("560".equals(text(db,"SELECT count(*) FROM (SELECT dataset_hash,sample_index FROM r30_atomnet_sample UNION SELECT p.dataset_hash,s.sample_index FROM r31_atomnet_sample_stage s JOIN r31_atomnet_publication p ON p.publication_id=s.publication_id WHERE p.status='PUBLISHED')")),"SAMPLES_NOT_PERIODS_TIMES_TEN");
                require(String.valueOf(rt.atomSessionR30.materializedSamples).equals("560"),"SESSION_MATERIALIZED_NOT_PERIODS_TIMES_TEN");
                require(legacyHash.equals(text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'")),"LEGACY_OVERWRITTEN");
                require("10".equals(text(db,"SELECT count(*) FROM r30_atomnet_forecast")),"TEN_FUTURE_PREDICTIONS_NOT_FROZEN");
                require(rt.atomSessionR30.trainer.net.in==182,"PRODUCTION_ONE_HOT_NOT_CONSUMED");
                long learnedBefore=rt.atomSessionR30.trainer.onlineSteps;add(c,81);owned.owned(()->AtomNetRuntimeR30.step(rt,worker));require(rt.atomSessionR30.trainer.onlineSteps==learnedBefore,"IN_FLIGHT_UNREGISTERED_INPUT_LEARNED");
                add(c,82);owned.owned(()->AtomNetRuntimeR30.step(rt,worker));
                require(rt.atomSessionR30.nextPosition==2,"POSITION_ONE_NOT_COMMITTED");
                byte[] before=artifact(db),memory=rt.atomSessionR30.encode();owned.failNextCheckpoint(AtomNetSessionR30.OWNER);
                try{owned.owned(()->AtomNetRuntimeR30.step(rt,worker));throw new AssertionError("FAILURE_NOT_INJECTED");}catch(IllegalStateException expected){}
                require(Arrays.equals(before,artifact(db))&&Arrays.equals(memory,rt.atomSessionR30.encode()),"MODEL_CURSOR_NOT_ATOMIC_ON_ROLLBACK");
                for(int p=2;p<=STOP_POSITION;p++)owned.owned(()->AtomNetRuntimeR30.step(rt,worker));
                require(rt.atomSessionR30.nextPosition==STOP_POSITION+1&&rt.atomSessionR30.partialIssue.equals(issue(82)),"MID_ISSUE_CURSOR");
                byte[] committedState=resolved(db);AtomNetSessionR30 expected=AtomNetSessionR30.decode(committedState,StatsR27.sha256(committedState));
                ReplayDatasetR27 data=StoreR27.loadDataset(db,100);AtomReplayR27.Frame frame=AtomReplayR27.build(data,81,82);
                for(int p=STOP_POSITION+1;p<=10;p++){
                    double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p-1],frame.present[p-1],true),p);
                    expected.online(new AtomNetTrainerR27.Sample(x,frame.hit[p-1],frame.actual[p-1]-1,AtomNetSessionR30.eventKey(issue(82),p)),issue(82),data.ordinals[81],p);
                }
                ResearchWriteQueueR27.Batch receipt=new ResearchWriteQueueR27.Batch();receipt.add("INSERT OR REPLACE INTO r27_chart_cache VALUES('host','expected',?,1)",StatsR27.sha256(expected.encode()));commit(c,db,receipt,false);
                System.out.println("R30_TEN_SQLITE|PRETRAIN_SLICES="+slices+"|560_SAMPLES_56_PERIODS|182_INPUTS|10_FROZEN_POSITIONS|FAILED_COMMIT_ROLLED_BACK|HALT_AFTER_POSITION_"+STOP_POSITION);System.out.flush();Runtime.getRuntime().halt(0);
            }
            owned.owned(()->AtomNetRuntimeR30.step(rt,worker));require(rt.atomSessionR30.nextPosition==STOP_POSITION+1,"RESTORE_LOST_POSITION_CURSOR");
            for(int p=STOP_POSITION+1;p<=10;p++)owned.owned(()->AtomNetRuntimeR30.step(rt,worker));
            require(rt.atomSessionR30.trainer.onlineSteps==10&&rt.atomSessionR30.completeIssue.equals(issue(82)),"TEN_ONLINE_POSITIONS_NOT_ONCE");
            require(StatsR27.sha256(resolved(db)).equals(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='host' AND key='expected'")),"PRODUCTION_RESTART_NOT_EXACT");
            byte[] settled=artifact(db);owned.owned(()->AtomNetRuntimeR30.step(rt,worker));require(Arrays.equals(settled,artifact(db)),"DUPLICATE_LEARNING_AFTER_RETRY");
            require(legacyHash.equals(text(db,"SELECT state_hash FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'")),"LEGACY_NOT_PRESERVED");
            Map<?,?> report=(Map<?,?>)JsonR27.parse(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='atomnet' AND key='ten-position-r30'"));
            require(((Number)report.get("materializedSamples")).longValue()==560&&((Number)report.get("onlineSamples")).longValue()==10,"REPORT_DOES_NOT_MATCH_COMMITTED_TRAINING");
            System.out.println("R30_TEN_SQLITE|REAL_JVM_RESTART_DECLARED_POSITION_TO_10_EXACT|PRODUCTION_FENCED_RETRY_AFTER_CONFIRMED_EXIT|NO_DUPLICATE|LEGACY_BYTES_PRESERVED|THROUGHPUT_REPORT_MATCHES|PASS");
            System.out.println("R30_TEN_SQLITE_REPORT "+JsonR27.write(report));
            }
        }
    }
    static long child(String mode,Path db,long oldPid)throws Exception{
        Process p=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx768m","-Dr30.host.stopPosition="+STOP_POSITION,"-Dr31.host.previousPid="+oldPid,"-cp",System.getProperty("java.class.path"),TenPositionR30HostTest.class.getName(),mode,db.toString()).redirectErrorStream(true).start();
        if(!p.waitFor(120,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("TEN_CHILD_TIMEOUT:"+mode);}
        String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);System.out.print(output);require(p.exitValue()==0,"TEN_CHILD_FAILED:"+mode+":"+p.exitValue());return p.pid();
    }
}
