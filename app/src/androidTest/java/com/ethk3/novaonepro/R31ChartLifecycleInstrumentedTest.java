package com.ethk3.novaonepro;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android tab/stop/resume/rotation and owned-resource closure.
 * Missing scientific inputs remain missing; no data, training or quality fixture
 * is manufactured. Worker termination and detached windows are measured, not
 * inferred from Future.cancel(), forced GC or an alive Activity heartbeat. */
@RunWith(AndroidJUnit4.class)
public final class R31ChartLifecycleInstrumentedTest {
    private static Object field(Object owner,String name){
        try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static Button button(View view,String title){
        if(view instanceof Button&&title.contentEquals(((Button)view).getText()))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=button(group.getChildAt(i),title);if(found!=null)return found;}}
        return null;
    }
    private static void ready(ActivityScenario<ResearchWorkbenchActivityR30R1> scenario,int group,int orientation){
        AtomicBoolean ready=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+10000;
        do{
            scenario.onActivity(activity->{
                ViewGroup content=(ViewGroup)field(activity,"content");
                ready.set(content.getChildCount()==6&&Integer.valueOf(group).equals(field(activity,"group"))
                        &&String.format(java.util.Locale.ROOT,"R%02d",group*6+1).equals(content.getChildAt(0).getTag())
                        &&(orientation==0||activity.getResources().getConfiguration().orientation==orientation));
            });
            if(ready.get())return;SystemClock.sleep(40);
        }while(SystemClock.elapsedRealtime()<until);
        fail("ACTUAL_CHART_LIFECYCLE_FRAME_NOT_READY");
    }
    @Test(timeout=900000) public void hundredRealTabBackgroundRotationCyclesCloseTheirActualOwners()throws Exception{
        String[] tabs={"结构与归因","创造与融合","质量与应用"};
        List<ExecutorService> retired=new ArrayList<>();int rotations=0,backgrounds=0,tabClicks=0;
        AtomicReference<ExecutorService> finalWorker=new AtomicReference<>();
        AtomicReference<View> finalWindow=new AtomicReference<>();
        try(ActivityScenario<ResearchWorkbenchActivityR30R1> scenario=ActivityScenario.launch(ResearchWorkbenchActivityR30R1.class)){
            ready(scenario,0,0);
            for(int cycle=0;cycle<100;cycle++){
                final int group=cycle%3;
                scenario.onActivity(activity->{Button tab=button(activity.getWindow().getDecorView(),tabs[group]);assertNotNull(tab);assertTrue(tab.performClick());});
                tabClicks++;ready(scenario,group,0);
                // A real source-details dialog must be dismissed by onStop,
                // including while the actual read worker has outstanding work.
                scenario.onActivity(activity->{Button detail=button(activity.getWindow().getDecorView(),"查看来源与分析口径");assertNotNull(detail);assertTrue(detail.performClick());});
                scenario.moveToState(Lifecycle.State.CREATED);
                scenario.moveToState(Lifecycle.State.RESUMED);backgrounds++;
                scenario.onActivity(activity->assertNull("stopped Activity retained its dialog",field(activity,"details")));
                ready(scenario,group,0);
                AtomicReference<ExecutorService> oldWorker=new AtomicReference<>();
                AtomicReference<View> oldWindow=new AtomicReference<>();
                final int orientation=(cycle&1)==0?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
                scenario.onActivity(activity->{oldWorker.set((ExecutorService)field(activity,"worker"));oldWindow.set(activity.getWindow().getDecorView());activity.setRequestedOrientation(orientation==Configuration.ORIENTATION_LANDSCAPE?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);});
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();ready(scenario,group,orientation);rotations++;
                AtomicReference<ExecutorService> current=new AtomicReference<>();scenario.onActivity(activity->current.set((ExecutorService)field(activity,"worker")));
                if(current.get()!=oldWorker.get()){
                    assertTrue("destroyed Activity read owner was not shut down",oldWorker.get().isShutdown());
                    assertTrue("cancelled SQL/read owner did not actually terminate",oldWorker.get().awaitTermination(5,TimeUnit.SECONDS));
                    assertFalse("destroyed Activity retained an attached window",oldWindow.get().isAttachedToWindow());retired.add(oldWorker.get());
                }
            }
            scenario.onActivity(activity->{finalWorker.set((ExecutorService)field(activity,"worker"));finalWindow.set(activity.getWindow().getDecorView());});
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(finalWorker.get().isShutdown());assertTrue(finalWorker.get().awaitTermination(5,TimeUnit.SECONDS));
        assertFalse(finalWindow.get().isAttachedToWindow());
        for(ExecutorService worker:retired)assertTrue("previous Activity owner revived",worker.isTerminated());
        assertEquals(100,tabClicks);assertEquals(100,backgrounds);assertEquals(100,rotations);
        assertTrue("physical rotation never recreated an actual Activity",retired.size()>0);
        System.out.println("R31_UI_LIFECYCLE|ACTUAL_ACTIVITY|TAB=100|STOP_RESUME=100|ROTATION=100|OWNED_WORKERS_TERMINATED|WINDOWS_DETACHED|NO_BAD_TOKEN|VM_HEAP_RETENTION_NOT_PROVEN|SCIENTIFIC_QUALITY_NOT_EVALUATED|PASS");
    }
}
