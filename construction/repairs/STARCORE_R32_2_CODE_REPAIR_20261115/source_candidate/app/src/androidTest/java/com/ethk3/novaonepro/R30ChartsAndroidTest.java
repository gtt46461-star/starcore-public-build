package com.ethk3.novaonepro;

import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real product Activity screenshots. All temporary cache fixtures are labelled SYNTHETIC and restored. */
@RunWith(AndroidJUnit4.class)
public final class R30ChartsAndroidTest {
    static final String[] KINDS={"TRIPLE","LINE3","STACK","LINES","GRAPH","BAR","BAR","LINE","LINE","LINES","BAR","LINE","BAR","BAR"};
    static Map<String,Object> m(Object... pairs){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    static Map<String,Object> point(String label,double value){return m("label",label,"value",value,"lower",null,"upper",null,"n",1L,"details","SYNTHETIC_SCREENSHOT_ONLY","stack",Arrays.asList(.1,.2,.3));}
    static LinearLayout cards(ChartsActivityR83 activity){try{Field f=ChartsActivityR83.class.getDeclaredField("charts");f.setAccessible(true);return(LinearLayout)f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    static List<String> keys(){List<String> out=new ArrayList<>(Arrays.asList(ChartCacheR27.PANELS));out.add("r30-charts");return out;}
    @Test public void allFourteenLegacyAndTwoSnapshotChartsOpenInThreeStatesAndSaveScreenshots()throws Exception{
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();DbStore store=new DbStore(context);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_CHART_SCREENSHOT_SCHEMA",db->{db.execSQL("CREATE TABLE IF NOT EXISTS r27_chart_cache(panel_id TEXT NOT NULL,key TEXT NOT NULL,payload_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(panel_id,key))");return 0;});
        File output=new File(context.getExternalFilesDir(null),"r30-chart-regression");assertTrue(output.isDirectory()||output.mkdirs());
        List<Object[]> originals=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT panel_id,key,payload_json,updated_at FROM r27_chart_cache",null)){while(c.moveToNext())if(keys().contains(c.getString(0)))originals.add(new Object[]{c.getString(0),c.getString(1),c.getString(2),c.getLong(3)});}
        int saved=0;
        try{
            for(String scenario:new String[]{"READY","EMPTY","INSUFFICIENT","READ_ERROR"}){
                Map<String,Object> facts=m("registeredCapabilities",3L,"qualifiedExperiments",0L),mark=m("latestIssue","SYNTHETIC_SCREENSHOT_ONLY","factsSha256","SYNTHETIC_"+scenario);
                Map<String,Object> holdout=m("taskKey","SYNTHETIC_SCREENSHOT_ONLY","datasetHash","synthetic","confirmationOutsideEvolution",true,"status","COMPLETED","verified",true,"pairedLcbVsPrevious",.02);
                Map<String,Object> generation=m("generation",1L,"loop","MAIN","dataset_hash","synthetic","paired_lcb",.02,"verified",1L,"detail_json",JsonR27.write(m("holdout",holdout)));
                Map<String,Object> data=m("evolution",Collections.singletonList(generation),"capabilities",Arrays.asList(m("kind","FAMILY","id","fixture-1","implemented",true,"executable",true),m("kind","EXPERT","id","fixture-2","implemented",true,"executable",false),m("kind","MODEL","id","fixture-3","implemented",false,"executable",false)));
                ReportR27.R30Input snapshot=new ReportR27.R30Input(facts,mark,data,Collections.emptyList());
                String packet=JsonR27.write(ReportChartsR30.packet(snapshot));
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_CHART_SCREENSHOT_FIXTURE",db->{
                    for(String key:keys())db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id=?",new Object[]{key});
                    for(int i=0;i<ChartCacheR27.PANELS.length;i++){
                        List<Object> points="READY".equals(scenario)?Arrays.asList(point("fixture-A",.2),point("fixture-B",.3)):Collections.emptyList();
                        Map<String,Object> panel=m("title",ChartCacheR27.PANELS[i]+" [SYNTHETIC]","kind",KINDS[i],"source","SYNTHETIC_SCREENSHOT_ONLY","window",scenario,"note","截图夹具，不是自然样本或质量证明","status","READY".equals(scenario)?"READY":"INSUFFICIENT","points",points,"refs",Collections.singletonList(0.0),"series",Arrays.asList("fixture-1","fixture-2","fixture-3"),"edges",Collections.singletonList(m("from","fixture-A","to","fixture-B","label","SYNTHETIC")));
                        db.execSQL("INSERT INTO r27_chart_cache VALUES(?,'panel',?,?)",new Object[]{ChartCacheR27.PANELS[i],"READ_ERROR".equals(scenario)?"broken":JsonR27.write(panel),System.currentTimeMillis()});
                    }
                    if(!"EMPTY".equals(scenario))db.execSQL("INSERT INTO r27_chart_cache VALUES('r30-charts','snapshot',?,?)",new Object[]{"READ_ERROR".equals(scenario)?"broken":packet,System.currentTimeMillis()});return 0;
                });
                ChartsActivityR83 activity=(ChartsActivityR83)instrumentation.startActivitySync(new Intent(context,ChartsActivityR83.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                try{
                    long deadline=SystemClock.elapsedRealtime()+20_000;AtomicInteger count=new AtomicInteger();
                    do{instrumentation.runOnMainSync(()->count.set(cards(activity).getChildCount()));if(count.get()==29)break;SystemClock.sleep(100);}while(SystemClock.elapsedRealtime()<deadline);
                    java.util.concurrent.atomic.AtomicReference<String> actualStatus=new java.util.concurrent.atomic.AtomicReference<>();
                    instrumentation.runOnMainSync(()->{try{Field f=ChartsActivityR83.class.getDeclaredField("status");f.setAccessible(true);actualStatus.set(((TextView)f.get(activity)).getText().toString());}catch(Exception error){throw new AssertionError(error);}});
                    assertEquals("headline +14 R27 +2 R30 +12 retained R83 panels in "+scenario+"; status="+actualStatus.get(),29,count.get());
                    instrumentation.runOnMainSync(()->{for(int retained=17;retained<29;retained++)assertTrue("original R83 panel retained",cards(activity).getChildAt(retained) instanceof LinearLayout);});
                    for(int index=0;index<29;index++){
                        R30GalleryCaptureR30.Frame capture=R30GalleryCaptureR30.capture(instrumentation,cards(activity),index,null,null);
                        Bitmap bitmap=capture.bitmap;
                        try(OutputStream stream=new FileOutputStream(new File(output,String.format(Locale.ROOT,"%s-%02d.png",scenario,index)))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}finally{bitmap.recycle();}saved++;
                    }
                }finally{instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            }
            assertEquals(116,saved);System.out.println("R30_ANDROID_CHARTS|ACTUAL_ACTIVITY|29_REGISTERED_OBJECTS|FOUR_DISPLAY_SCENARIOS|116_SCREENSHOTS|SYNTHETIC_CACHES_RESTORED|NOT_PHYSICAL_OR_NATURAL_ACCEPTANCE|PASS");
        }finally{
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_CHART_SCREENSHOT_RESTORE",db->{for(String key:keys())db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id=?",new Object[]{key});for(Object[] row:originals)db.execSQL("INSERT INTO r27_chart_cache VALUES(?,?,?,?)",row);return 0;});store.close();
        }
    }
}
