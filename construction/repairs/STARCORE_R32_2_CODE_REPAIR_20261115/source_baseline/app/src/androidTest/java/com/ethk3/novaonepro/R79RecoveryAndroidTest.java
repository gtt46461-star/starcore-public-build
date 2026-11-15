package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Disposable emulator, synthetic inputs; no natural quality/phone soak assertion. */
@RunWith(AndroidJUnit4.class)
public final class R79RecoveryAndroidTest {
    private static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    private static String value(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
    private static int tableCursor(){for(int i=0;i<RetentionTableRegistryCP12.ALL_INSTALLED.size();i++)if(RetentionTableRegistryCP12.ALL_INSTALLED.get(i).table.equals("diagnostic_log"))return i;throw new AssertionError("registered diagnostic table required");}
    private static void point(SQLiteDatabase db){db.execSQL("INSERT OR REPLACE INTO app_state VALUES('retention_cp12_revision_cursor',?,1)",new Object[]{Integer.toString(tableCursor())});}

    @Test public void exactHandoffDeadlineAndBoundedRevisionRecoveryOnAndroid(){
        DbStore store=new DbStore(ApplicationProvider.getApplicationContext());
        long until=SystemClock.elapsedRealtime()+600000;
        while(!store.ensureResearchSidecarR811()){
            assertTrue(ResearchSchemaCoordinatorR926.summary(),SystemClock.elapsedRealtime()<until);
            SystemClock.sleep(20);
        }
        SQLiteDatabase db=store.readable();
        assertEquals("fresh disposable emulator required",0,n(db,"SELECT COUNT(*) FROM draws"));
        Models.Draw draw=new Models.Draw();draw.issue="202610150500";draw.openTime=System.currentTimeMillis()-1000;
        draw.fetchTime=draw.openTime+1;draw.sourceLine="SYNTHETIC_R79_ANDROID";
        for(int i=0;i<10;i++)draw.numbers[i]=i+1;
        assertTrue(store.saveDraw(draw)>0);
        long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(store.getLatestDraw());
        assertTrue(deadline>System.currentTimeMillis());
        SettlementCoordinatorR815.queueCurrentExactR914(store,draw,SettlementCoordinatorR815.Reason.RECONCILER);
        assertEquals(1,n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36 WHERE target_issue='202610150501'"));
        assertEquals(deadline,n(db,"SELECT deadline_at FROM v57_analysis_request_r36 WHERE target_issue='202610150501'"));
        assertEquals("DURABLE_REQUESTED",value(db,"r947_request_first_status"));
        SettlementCoordinatorR815.queueCurrentExactR914(store,draw,SettlementCoordinatorR815.Reason.RECONCILER);
        assertEquals(deadline,n(db,"SELECT deadline_at FROM v57_analysis_request_r36 WHERE target_issue='202610150501'"));
        assertEquals(1,n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36"));
        store.coordinatorR36().runAtomicWriteOrThrowF01("TEST_R79_REPLACEMENT_FIXTURE",tx->{
            for(int i=0;i<8192;i++)tx.execSQL("INSERT OR REPLACE INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES(?,1,'INFO','SYNTHETIC_ANDROID','R79','replacement orphan fixture','test')",new Object[]{"R79-"+(i%32)});
            point(tx);return true;
        });
        long before=n(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'"),source=n(db,"SELECT COUNT(*) FROM diagnostic_log");
        assertTrue("actual REPLACE creates old index generations",before>=8192);
        int provisional;
        db.beginTransaction();try{provisional=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+2000);}finally{db.endTransaction();}
        assertTrue(provisional>128&&provisional<=2048);
        assertEquals("rollback retains exact index",before,n(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'"));
        assertEquals(Integer.toString(tableCursor()),value(db,"retention_cp12_revision_cursor"));
        int removed;
        db.beginTransaction();try{removed=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+2000);db.setTransactionSuccessful();}finally{db.endTransaction();}
        assertTrue(removed>128&&removed<=2048);
        assertEquals(before-removed,n(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'"));
        assertEquals("raw source remains intact",source,n(db,"SELECT COUNT(*) FROM diagnostic_log"));
        assertEquals(0,n(db,"SELECT COUNT(*) FROM diagnostic_log l WHERE NOT EXISTS(SELECT 1 FROM research_row_revision_v2 r WHERE r.source_table='diagnostic_log' AND r.source_rowid=l.rowid)"));
        RealtimePressureGateR811.beginAnalysis();
        db.beginTransaction();try{assertEquals(0,DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+2000));db.setTransactionSuccessful();}finally{db.endTransaction();RealtimePressureGateR811.endAnalysis();}
        assertEquals(0,n(db,"SELECT COUNT(*) FROM trades"));
    }
}
