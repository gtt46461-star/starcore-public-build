package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.ChartsActivityR83;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SchemaAndChartsR13Test {
    private static void ensure(SQLiteDatabase db){db.beginTransaction();try{StrategySchemaSU2.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    @Test public void deployedSchemaMigratesAndRollsBackWithoutTouchingLegacy(){
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER)");
            db.execSQL("INSERT INTO draws VALUES('202610240001',7)");
            db.beginTransaction();try{StrategySchemaSU2.ensure(db);}finally{db.endTransaction();}
            assertFalse(StrategySchemaSU2.ready(db));ensure(db);ensure(db);assertTrue(StrategySchemaSU2.ready(db));
            try(Cursor c=db.rawQuery("SELECT n1 FROM draws",null)){assertTrue(c.moveToFirst());assertEquals(7,c.getInt(0));}
            try(Cursor c=db.rawQuery("PRAGMA table_info(su2_live_state)",null)){assertEquals(5,c.getCount());}
        }
    }
    @Test public void productionFreezeAndSettlementAreIdempotent(){
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            ensure(db);db.beginTransaction();try{
                ModelLiveSU2.Decision d=new ModelLiveSU2.Decision(1,1,7,true,.5,.4);
                ModelStoreSU2.insertDecision(db,"test",1,d,10,20);
                ModelStoreSU2.insertDecision(db,"test",1,d,11,20);
                int[] draw={1,2,3,4,5,6,7,8,9,10};
                assertEquals(1,ModelStoreSU2.settle(db,1,draw,21));assertEquals(0,ModelStoreSU2.settle(db,1,draw,22));
                try{ModelStoreSU2.insertDecision(db,"late",2,d,20,20);fail("late freeze accepted");}catch(IllegalStateException expected){assertNotNull(expected.getMessage());}
                try{db.execSQL("UPDATE su2_live_decision SET number_mask=11");fail("frozen action changed");}catch(android.database.sqlite.SQLiteException expected){assertNotNull(expected.getMessage());}
                assertNull(CertificateSU2.read(db,"test",21));db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }
    @Test public void restoredChartsActivityLaunches() throws Exception {
        try(ActivityScenario<ChartsActivityR83> scenario=ActivityScenario.launch(ChartsActivityR83.class)){
            java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
            for(int i=0;i<200&&!ready.get();i++){
                scenario.onActivity(a->{
                    assertFalse(a.isFinishing());java.util.ArrayList<android.view.View> labels=new java.util.ArrayList<>();
                    a.getWindow().getDecorView().findViewsWithText(labels,"张图，",android.view.View.FIND_VIEWS_WITH_TEXT);ready.set(!labels.isEmpty());
                });
                if(!ready.get())android.os.SystemClock.sleep(100);
            }
            assertTrue("charts did not finish loading real database panels",ready.get());
            android.graphics.Bitmap screenshot=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            java.io.File cache=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(cache,"r13-charts.png"))){assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{screenshot.recycle();}
        }
    }
}
