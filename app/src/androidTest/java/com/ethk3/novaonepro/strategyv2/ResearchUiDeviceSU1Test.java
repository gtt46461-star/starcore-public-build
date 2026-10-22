package com.ethk3.novaonepro.strategyv2;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ResearchUiDeviceSU1Test {
    @Test public void researchScreenOpensAndReadsItsRealState()throws Exception{
        try(ActivityScenario<StrategyResearchActivitySU1> scenario=ActivityScenario.launch(StrategyResearchActivitySU1.class)){
            java.util.concurrent.atomic.AtomicBoolean loading=new java.util.concurrent.atomic.AtomicBoolean(true);
            for(int i=0;i<100&&loading.get();i++){
                scenario.onActivity(activity->{
                    assertEquals(activity.getString(R.string.su1_research_title),String.valueOf(activity.getTitle()));
                    java.util.ArrayList<android.view.View> views=new java.util.ArrayList<>();activity.getWindow().getDecorView().findViewsWithText(views,activity.getString(R.string.su1_initializing),android.view.View.FIND_VIEWS_WITH_TEXT);loading.set(!views.isEmpty());
                });
                if(loading.get())android.os.SystemClock.sleep(100);
            }
            assertFalse("research database read did not return",loading.get());
            android.graphics.Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            java.io.File directory=InstrumentationRegistry.getInstrumentation().getContext().getExternalFilesDir(null);assertNotNull(directory);
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(directory,"su1-research.png"))){assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));}finally{screenshot.recycle();}
        }
    }
}
