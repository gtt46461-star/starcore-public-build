package com.ethk3.novaonepro.strategyv2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual production coordinator/writer on an isolated SYNTHETIC database, never natural evidence. */
@RunWith(AndroidJUnit4.class)
public final class R26RuntimeDeviceTest {
    private static final class Sandbox extends Application {
        final File root;final String prefix;
        Sandbox(Context base)throws IOException {attachBaseContext(base);prefix="r26-synthetic-"+UUID.randomUUID();root=new File(base.getCacheDir(),prefix).getCanonicalFile();if(!root.mkdirs())throw new IOException("sandbox directory");}
        @Override public Context getApplicationContext(){return this;}
        @Override public Context createDeviceProtectedStorageContext(){return this;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+"-"+name,mode);}
        File directory(String name){File f=new File(root,name);if(!f.isDirectory()&&!f.mkdirs())throw new IllegalStateException("sandbox directory");return f;}
        @Override public File getFilesDir(){return directory("files");}
        @Override public File getCacheDir(){return directory("cache");}
        @Override public File getDatabasePath(String name){return new File(directory("databases"),name);}
        @Override public ApplicationInfo getApplicationInfo(){ApplicationInfo info=new ApplicationInfo(super.getApplicationInfo());info.dataDir=root.getPath();return info;}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler errors){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,errors);}
        @Override public boolean deleteDatabase(String name){return SQLiteDatabase.deleteDatabase(getDatabasePath(name));}
    }
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static Models.Draw draw(long first,int index,long opened){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+index);d.numbers=new int[10];for(int p=0;p<10;p++)d.numbers[p]=(p+index%7)%10+1;d.openTime=opened+index*60000L;d.fetchTime=d.openTime+1000;return d;}
    private static void waitStopped(SQLiteDatabase db)throws Exception {long end=System.nanoTime()+10_000_000_000L;while(System.nanoTime()<end){if(scalar(db,"SELECT count(*) FROM r26_session WHERE user_enabled=1")==0)return;Thread.sleep(20);}fail("coordinator did not persist stop");}
    private static void drain(RuntimeCoordinatorR26 runtime)throws Exception {java.lang.reflect.Field field=RuntimeCoordinatorR26.class.getDeclaredField("worker");field.setAccessible(true);Thread worker=(Thread)field.get(runtime);worker.join(10000);assertFalse("coordinator thread did not drain",worker.isAlive());}
    private static void remove(File f){File[] children=f.listFiles();if(children!=null)for(File child:children)remove(child);if(f.exists())assertTrue(f.delete());}

    @Test public void allMechanismsAdvanceWithRealWriterPressureAndRestartWithoutStaleCommit()throws Exception {
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull("Fresh instrumentation process must not already own another database",DatabaseCoordinator.peek());Sandbox app=new Sandbox(original);DbStore store=null;RuntimeCoordinatorR26 runtime=null;JSONObject result=new JSONObject().put("scope","SYNTHETIC_ISOLATED_PRODUCTION_COORDINATOR; NOT_NATURAL_QUALITY; NOT_USER_PHONE_ACCEPTANCE").put("api",android.os.Build.VERSION.SDK_INT).put("engineeringWriteProbes",1000).put("fullPredrawOpportunitySloClaimed",false).put("restartKind","RUNTIME_COORDINATOR_REPLACEMENT_IN_SAME_PROCESS");
        try{
            store=new DbStore(app);SQLiteDatabase db=store.readable();assertEquals(app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath(),new File(db.getPath()).getCanonicalPath());assertFalse(new File(db.getPath()).getCanonicalPath().equals(original.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath()));
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),opened=System.currentTimeMillis()-4401L*60000;List<Models.Draw> history=new ArrayList<>();for(int i=0;i<3400;i++)history.add(draw(first,i,opened));assertEquals(3400,store.saveDraws(history));
            new ServiceHeartbeatStore(app).setUserEnabled(true);runtime=new RuntimeCoordinatorR26(store);long start=System.nanoTime(),end=start+240_000_000_000L;int families=0,experts=0;
            while(System.nanoTime()<end){
                if(table(db,"r26_capability")){families=(int)scalar(db,"SELECT count(*) FROM r26_capability WHERE kind='FAMILY' AND last_progress>0");experts=(int)scalar(db,"SELECT count(DISTINCT expert_id) FROM (SELECT expert_id FROM r26_expert_experiment WHERE cursor>0 UNION SELECT expert_id FROM r26_expert_audit)");if(families==24&&experts==16)break;}
                assertTrue("research coordinator exited",runtime.isRunning());Thread.sleep(100);
            }
            result.put("familyMechanismsWithCommittedProgress",families).put("expertsWithActualReceipts",experts).put("allMechanismsServiceMillis",(System.nanoTime()-start)/1000000);
            assertEquals("Each family must commit real numerical work",24,families);assertEquals("Each expert must complete a pair sample or an audit",16,experts);
            long[] latency=new long[1000];Handler ui=new Handler(Looper.getMainLooper());
            for(int i=0;i<1000;i++){
                RealtimePressureGateR811.beginSettlement();long t=System.nanoTime();try{assertTrue("canonical probe write rejected",store.saveDraw(draw(first,3400+i,opened))>=0);}finally{latency[i]=(System.nanoTime()-t)/1000000;RealtimePressureGateR811.endSettlement();}
                if(i%25==0){CountDownLatch responsive=new CountDownLatch(1);assertTrue(ui.post(responsive::countDown));assertTrue("main looper stalled under research pressure",responsive.await(2,TimeUnit.SECONDS));Thread.sleep(10);}RuntimeCoordinatorR26.wake();
            }
            Arrays.sort(latency);result.put("canonicalDrawWriteP50Ms",latency[499]).put("canonicalDrawWriteP95Ms",latency[949]).put("canonicalDrawWriteP99Ms",latency[989]).put("canonicalDrawWriteMaxMs",latency[999]);assertTrue("writer p99 exceeded critical owner's 2s envelope",latency[989]<2000);
            long before=scalar(db,"SELECT coalesce(sum(cursor),0) FROM r26_candidate"),progress=scalar(db,"SELECT coalesce(max(actual_progress),0) FROM r26_session");assertTrue(before>0);result.put("committedCandidateSamplesBeforeRestart",before).put("actualProgressBeforeRestart",progress);
            RuntimeCoordinatorR26.Claim stale=null;end=System.nanoTime()+2_000_000_000L;while(stale==null&&System.nanoTime()<end){try(Cursor c=db.rawQuery("SELECT task_id,owner,fence,cursor FROM r26_task WHERE status='RUNNING' AND owner IS NOT NULL LIMIT 1",null)){if(c.moveToFirst())stale=new RuntimeCoordinatorR26.Claim(c.getString(0),c.getString(1),c.getLong(2),c.getLong(3));}if(stale==null)Thread.sleep(5);}
            runtime.close();drain(runtime);waitStopped(db);if(stale!=null)try{RuntimeCoordinatorR26.requireFence(db,stale);fail("stopped owner could commit");}catch(IllegalStateException expected){}
            runtime=new RuntimeCoordinatorR26(store);end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end&&scalar(db,"SELECT coalesce(sum(cursor),0) FROM r26_candidate")<=before)Thread.sleep(50);
            long after=scalar(db,"SELECT coalesce(sum(cursor),0) FROM r26_candidate");assertTrue("restart lost or stopped numerical progress",after>before);result.put("committedCandidateSamplesAfterRestart",after).put("restartResumed",true).put("staleOwnerRejected",stale!=null);
            result.put("capacityFailReceipts",scalar(db,"SELECT count(*) FROM r26_progress WHERE blocked_reason LIKE 'CAPACITY_FAIL:%'")).put("checkpointFileBytes",CheckpointArtifactsR26.forStore(store).bytes()).put("result","PASS_WITH_STATED_ENGINEERING_SCOPE");
        }catch(Throwable failure){result.put("result","FAIL").put("error",failure.toString());
            if(store!=null&&table(store.readable(),"r26_task")){JSONArray tasks=new JSONArray();try(Cursor c=store.readable().rawQuery("SELECT task_id,cursor,status,blocked_reason,last_progress,last_cpu_ms FROM r26_task ORDER BY task_id",null)){while(c.moveToNext())tasks.put(new JSONObject().put("task",c.getString(0)).put("cursor",c.getLong(1)).put("status",c.getString(2)).put("reason",c.isNull(3)?JSONObject.NULL:c.getString(3)).put("lastProgress",c.getLong(4)).put("cpuMs",c.getLong(5)));}result.put("tasksAtFailure",tasks).put("legacySchema",ResearchSchemaCoordinatorR926.summary());}
            System.out.println("R26_RUNTIME_FAILURE_RECEIPT="+result.toString());throw failure;}
        finally{
            if(runtime!=null){new ServiceHeartbeatStore(app).setUserEnabled(false);runtime.close();drain(runtime);if(store!=null&&table(store.readable(),"r26_session"))waitStopped(store.readable());}if(store!=null)store.close();
            File receipt=new File(original.getCacheDir(),"r26-runtime-synthetic.json");try(FileOutputStream output=new FileOutputStream(receipt)){output.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));output.getFD().sync();}
            remove(app.root);
        }
    }
}
