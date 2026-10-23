package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.R;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class ResearchUiDeviceSU1Test {
    private static void awaitRead(ActivityScenario<StrategyResearchActivitySU1> scenario) {
        java.util.concurrent.atomic.AtomicBoolean loading=new java.util.concurrent.atomic.AtomicBoolean(true);
        for(int i=0;i<100&&loading.get();i++){
            scenario.onActivity(activity->{
                assertEquals(activity.getString(R.string.su1_research_title),String.valueOf(activity.getTitle()));
                java.util.ArrayList<View> views=new java.util.ArrayList<>();
                activity.getWindow().getDecorView().findViewsWithText(views,activity.getString(R.string.su1_initializing),View.FIND_VIEWS_WITH_TEXT);
                loading.set(!views.isEmpty());
            });
            if(loading.get())SystemClock.sleep(100);
        }
        DatabaseCoordinator owner=DatabaseCoordinator.peek();
        assertFalse("research read exceeded 10 seconds; "+(owner==null?"database not started":owner.startupSchemaSummary()),loading.get());
        scenario.onActivity(activity->{
            java.util.ArrayList<View> errors=new java.util.ArrayList<>();
            activity.getWindow().getDecorView().findViewsWithText(errors,activity.getString(R.string.su1_error,""),View.FIND_VIEWS_WITH_TEXT);
            assertTrue("research state read reported an error",errors.isEmpty());
            java.util.ArrayList<View> controls=new java.util.ArrayList<>();
            activity.getWindow().getDecorView().findViewsWithText(controls,activity.getString(R.string.su1_import),View.FIND_VIEWS_WITH_TEXT);
            assertFalse("import control missing",controls.isEmpty());
        });
    }
    private static void shows(ActivityScenario<StrategyResearchActivitySU1> scenario,String value){
        scenario.onActivity(activity->{
            java.util.ArrayList<View> views=new java.util.ArrayList<>();
            activity.getWindow().getDecorView().findViewsWithText(views,value,View.FIND_VIEWS_WITH_TEXT);
            assertFalse("missing visible state: "+value,views.isEmpty());
        });
    }
    @Test public void a_researchScreenDoesNotInitializeAnUnstartedDatabase(){
        assertNull("cold-screen fixture already has a database owner",DatabaseCoordinator.peek());
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.io.File database=context.getDatabasePath(com.ethk3.novaonepro.AppConfig.DB_NAME);
        assertFalse("cold-screen fixture already has a database file",database.exists());
        try(ActivityScenario<StrategyResearchActivitySU1> scenario=ActivityScenario.launch(StrategyResearchActivitySU1.class)){
            awaitRead(scenario);
            shows(scenario,"数据库尚未启动");
            shows(scenario,"https://api.duoqu.online/data");
            shows(scenario,"https://api1.duoqu.online/data");
            assertNull("viewing research started a database writer",DatabaseCoordinator.peek());
            assertFalse("viewing research created a database",database.exists());
        }
    }
    @Test(timeout=60000) public void b_researchScreenReadsTheReadyDatabaseAndPersistsControls()throws Exception{
        // Explicit setup is off the UI thread. The screen keeps its original ten-second
        // read deadline; it must never own this cold schema initialization.
        DbStore store=new DbStore(InstrumentationRegistry.getInstrumentation().getTargetContext());
        DatabaseCoordinator owner=store.coordinatorForResearchR952();
        assertTrue(owner.isCriticalSchemaReady());
        int steps=0;
        while(!StrategySchemaSU1.ready(owner.readable())){
            assertTrue("SU1 migration did not terminate",steps++<1000);
            owner.runAtomicWriteOrThrowF01("SU1_UI_SCHEMA_TEST",db->{StrategySchemaSU1.advance(db);return 0;});
        }
        owner.runAtomicWriteOrThrowF01("SU1_UI_CONTROL_FIXTURE",db->{
            db.execSQL("UPDATE su1_runtime_control SET mode='DISABLED',reason='USER_CONTROL' WHERE singleton=1");return 0;
        });
        try(ActivityScenario<StrategyResearchActivitySU1> scenario=ActivityScenario.launch(StrategyResearchActivitySU1.class)){
            awaitRead(scenario);shows(scenario,"研究状态：DISABLED");shows(scenario,"实时研究池排行");
            scenario.onActivity(activity->{
                java.util.ArrayList<View> buttons=new java.util.ArrayList<>();
                activity.getWindow().getDecorView().findViewsWithText(buttons,activity.getString(R.string.su1_resume),View.FIND_VIEWS_WITH_TEXT);
                assertFalse(buttons.isEmpty());buttons.get(0).performClick();
            });
            awaitRead(scenario);shows(scenario,"研究状态：SHADOW");
            try(Cursor c=owner.readable().rawQuery("SELECT mode,reason FROM su1_runtime_control WHERE singleton=1",null)){
                assertTrue(c.moveToFirst());assertEquals("SHADOW",c.getString(0));assertEquals("USER_CONTROL",c.getString(1));
            }
            android.graphics.Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            java.io.File directory=InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();assertNotNull(directory);
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(directory,"su1-research.png"))){
                assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));
            }finally{screenshot.recycle();}
        }finally{store.close();}
    }
}
