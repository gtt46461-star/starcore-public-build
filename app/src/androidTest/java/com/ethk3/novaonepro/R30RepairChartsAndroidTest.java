package com.ethk3.novaonepro;
import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class R30RepairChartsAndroidTest {
 static Object field(Object o,String name){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
 static LinearLayout cards(ResearchWorkbenchActivityR30R1 a){return(LinearLayout)field(a,"content");}
 static void group(ResearchWorkbenchActivityR30R1 a,int g){try{Field f=a.getClass().getDeclaredField("group");f.setAccessible(true);f.setInt(a,g);Method m=a.getClass().getDeclaredMethod("load");m.setAccessible(true);m.invoke(a);}catch(Exception e){throw new AssertionError(e);}}
 static void ready(Instrumentation i,ResearchWorkbenchActivityR30R1 a,int g){long until=SystemClock.elapsedRealtime()+20000;AtomicBoolean done=new AtomicBoolean();do{i.runOnMainSync(()->done.set(cards(a).getChildCount()==6&&String.format(Locale.ROOT,"R%02d",g*6+1).equals(cards(a).getChildAt(0).getTag())&&!((TextView)field(a,"status")).getText().toString().startsWith("读取已")));if(done.get())return;SystemClock.sleep(80);}while(SystemClock.elapsedRealtime()<until);throw new AssertionError("WORKBENCH_GROUP_NOT_READY:"+g);}
 static void finishAndAwait(Instrumentation i,ResearchWorkbenchActivityR30R1 a)throws InterruptedException{
  i.runOnMainSync(a::finish);long until=SystemClock.elapsedRealtime()+10000;AtomicBoolean destroyed=new AtomicBoolean();
  do{i.waitForIdleSync();i.runOnMainSync(()->destroyed.set(a.isDestroyed()));if(destroyed.get())break;SystemClock.sleep(50);}while(SystemClock.elapsedRealtime()<until);
  assertTrue("actual Activity onDestroy must complete after asynchronous finish",destroyed.get());ExecutorService worker=(ExecutorService)field(a,"worker");
  assertTrue("onDestroy must shut down the actual read worker",worker.isShutdown());assertTrue("cancelled SQLite/read lease worker must actually terminate",worker.awaitTermination(5,TimeUnit.SECONDS));
 }
 @Test public void eighteenViewsRenderCommittedAnalysisOrExplicitMissingStatesAndCancelCleanly()throws Exception{
  Instrumentation i=InstrumentationRegistry.getInstrumentation();Context context=i.getTargetContext();DbStore store=new DbStore(context);Map<String,String> actual=RepairAnalysisAndroidFixture.create(store);assertEquals(18,actual.size());File output=new File(context.getExternalFilesDir(null),"r30-repair-chart-regression");assertTrue(output.isDirectory()||output.mkdirs());List<Object[]> originals=new ArrayList<>();try(Cursor q=store.readable().rawQuery("SELECT view_id,frame_id,run_id,payload_json,payload_hash,updated_at FROM r30r1_chart_cache",null)){while(q.moveToNext())originals.add(new Object[]{q.getString(0),q.getString(1),q.getString(2),q.getString(3),q.getString(4),q.getLong(5)});}
  int saved=0;List<Object> receipt=new ArrayList<>();
  try{for(String scenario:new String[]{"READY","EMPTY","INSUFFICIENT","READ_ERROR"}){
   store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_GALLERY_FIXTURE",db->{db.execSQL("DELETE FROM r30r1_chart_cache");if(!scenario.equals("EMPTY"))for(Object[] row:originals){Object[] copy=row.clone();if(scenario.equals("READ_ERROR"))copy[3]="broken";else if(scenario.equals("INSUFFICIENT")){@SuppressWarnings("unchecked")Map<String,Object> f=(Map<String,Object>)JsonR27.parse((String)copy[3]);f.put("status","INSUFFICIENT");f.put("reason","SYNTHETIC_INSUFFICIENT_RENDER_SCENARIO");f.put("points",Collections.emptyList());f.remove("mean");copy[3]=JsonR27.write(f);copy[4]=StatsR27.sha256((String)copy[3]);}db.execSQL("INSERT INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",copy);}return 0;});
   try{CancellationSignal cancelled=new CancellationSignal();cancelled.cancel();ResearchWorkbenchRepositoryR30R1.load(store.readable(),0,cancelled);fail("cancelled SELECT accepted");}catch(OperationCanceledException expected){}
   ResearchWorkbenchActivityR30R1 a=(ResearchWorkbenchActivityR30R1)i.startActivitySync(new Intent(context,ResearchWorkbenchActivityR30R1.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Throwable primary=null;try{
    for(int g=0;g<3;g++){final int selected=g;i.runOnMainSync(()->group(a,selected));ready(i,a,g);List<ResearchVisualDataR82.Panel> snapshot=ResearchWorkbenchRepositoryR30R1.load(store.readable(),g,new CancellationSignal());assertEquals(6,snapshot.size());for(int p=0;p<6;p++){
     ResearchVisualDataR82.Panel panel=snapshot.get(p);String id=String.format(Locale.ROOT,"R%02d",g*6+p+1);if(scenario.equals("READ_ERROR"))assertEquals("READ_ERROR",panel.status);if(scenario.equals("EMPTY"))assertEquals("WAITING_ANALYSIS",panel.status);if(scenario.equals("INSUFFICIENT"))assertEquals("INSUFFICIENT",panel.status);assertTrue(panel.points.size()<=120);final int index=p;i.runOnMainSync(()->{LinearLayout list=cards(a);View card=list.getChildAt(index);assertEquals(id,card.getTag());int offset=card.getTop();if(card instanceof LinearLayout)for(int v=0;v<((LinearLayout)card).getChildCount();v++){View child=((LinearLayout)card).getChildAt(v);if(child instanceof com.ethk3.novaonepro.r952.ResearchChartViewR82){offset+=child.getTop();break;}}((ScrollView)list.getParent()).scrollTo(0,offset);});i.waitForIdleSync();SystemClock.sleep(100);Bitmap bitmap=i.getUiAutomation().takeScreenshot();assertNotNull(bitmap);try(OutputStream stream=new FileOutputStream(new File(output,scenario+"-"+id+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}finally{bitmap.recycle();}saved++;
     Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);row.put("scenario",scenario);row.put("actualStatus",panel.status);row.put("source",panel.source);row.put("displayPoints",panel.points.size());row.put("analysisOriginalStatus",actual.get(id));row.put("quality","NOT_PROVEN");receipt.add(row);
    }}
    i.runOnMainSync(()->{for(int g=0;g<20;g++)group(a,g%3);group(a,2);});ready(i,a,2);assertEquals(6,cards(a).getChildCount());
   }catch(Exception|Error failure){primary=failure;throw failure;}finally{try{finishAndAwait(i,a);}catch(Exception|Error cleanup){if(primary!=null)primary.addSuppressed(cleanup);else throw cleanup;}}
  }assertEquals(72,saved);try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(output,"states.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(JsonR27.write(receipt));}System.out.println("R30_REPAIR_ANDROID_WORKBENCH|18_VIEWS_4_SCENARIOS_72_SCREENSHOTS|ACTUAL_PRODUCTION_ANALYSIS_OR_EXPLICIT_MISSING_SOURCES|PAGE6|RAPID_TABS_CANCEL_EXIT|NO_QUALITY_CLAIM|PASS");
  }finally{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_GALLERY_RESTORE",db->{db.execSQL("DELETE FROM r30r1_chart_cache");for(Object[] row:originals)db.execSQL("INSERT INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",row);return 0;});store.close();}
 }
}
