package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Test-only failure visibility; uses the unchanged schema writer name, body and deadlines. */
@RunWith(AndroidJUnit4.class)
public final class SchemaFailureDiagnosticAndroidTest {
    private static void snapshot(SQLiteDatabase db) {
        for (String sql : new String[]{
                "SELECT sqlite_version()",
                "SELECT * FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" ORDER BY domain",
                "SELECT * FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" ORDER BY domain,revision,step_id",
                "SELECT name FROM sqlite_master WHERE type='table' AND (name LIKE '%schema%' OR name LIKE '%migration%') ORDER BY name",
                "SELECT state_key,state_value FROM app_state WHERE state_key LIKE '%schema%' OR state_key LIKE '%migration%' ORDER BY state_key"}) {
            try (Cursor c=db.rawQuery(sql,null)) {
                while(c.moveToNext()) {
                    StringBuilder row=new StringBuilder("SCHEMA_DIAG_ROW|");
                    for(int i=0;i<c.getColumnCount();i++) row.append(c.getColumnName(i)).append('=').append(c.getString(i)).append('|');
                    System.out.println(row);
                }
            } catch(Exception e) {System.out.println("SCHEMA_DIAG_SNAPSHOT_ERROR|"+e);}
        }
    }
    @Test public void exposeOriginalWriterFailureWithoutChangingPolicy() throws Exception {
        DbStore store=new DbStore(ApplicationProvider.getApplicationContext());
        java.lang.reflect.Field slotField=DatabaseCoordinator.class.getDeclaredField("WRITE_FAILURE_F01");
        slotField.setAccessible(true);
        @SuppressWarnings("unchecked") ThreadLocal<Object> slot=(ThreadLocal<Object>)slotField.get(null);
        Class<?> frameClass=Class.forName("com.ethk3.novaonepro.DatabaseCoordinator$WriteFailureFrameF01");
        java.lang.reflect.Constructor<?> constructor=frameClass.getDeclaredConstructor();constructor.setAccessible(true);
        java.lang.reflect.Field errorField=frameClass.getDeclaredField("error");errorField.setAccessible(true);
        Object previous=slot.get();String prior="";boolean captured=false;int count=0;
        long until=SystemClock.elapsedRealtime()+600000;
        try {
            while(true) {
                Object frame=constructor.newInstance();slot.set(frame);
                long start=SystemClock.elapsedRealtime();
                boolean ready=store.ensureResearchSidecarR811();
                Throwable failure=(Throwable)errorField.get(frame);
                String state=ResearchSchemaCoordinatorR926.summary();
                System.out.println("SCHEMA_DIAG_PUBLIC_STEP|n="+(++count)+"|elapsed="+(SystemClock.elapsedRealtime()-start)+"|state="+state+"|ownCause="+(failure==null?"NONE":DatabaseCoordinator.describeCauseChain(failure,12000)));
                if((failure!=null||state.startsWith("FAILED"))&&!captured){captured=true;if(failure!=null)failure.printStackTrace(System.out);snapshot(store.readable());}
                try(Cursor c=store.readable().rawQuery("SELECT step_id,status,error_code FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='RESEARCH' ORDER BY started_at DESC LIMIT 1",null)) {
                    if(c.moveToFirst()){String last=c.getString(0)+"|"+c.getString(1)+"|"+c.getString(2);if(!last.equals(prior)){System.out.println("SCHEMA_DIAG_LAST_JOURNAL|"+last);prior=last;}}
                }
                if(ready){System.out.println("SCHEMA_DIAG_PUBLIC_READY");snapshot(store.readable());return;}
                assertTrue(state,SystemClock.elapsedRealtime()<until);
                SystemClock.sleep(20);
            }
        } finally {
            snapshot(store.readable());
            if(previous==null)slot.remove();else slot.set(previous);
        }
    }
    /** Direct schema-owner diagnostic, deliberately distinct from the DbStore public wrapper. */
    @Test public void exposeOwnedStepResultWithoutChangingPolicy() {
        DbStore store=new DbStore(ApplicationProvider.getApplicationContext());
        long until=SystemClock.elapsedRealtime()+600000L;int count=0;String prior="";
        try {
            while(true) {
                long start=SystemClock.elapsedRealtime();
                long cpuStart=android.os.Debug.threadCpuTimeNanos();
                ResearchSchemaCoordinatorR926.StepResult result=
                        ResearchSchemaCoordinatorR926.ensureOneStepResult(store.coordinatorR36());
                System.out.println("SCHEMA_OWNED_STEP|n="+(++count)
                        +"|elapsed="+(SystemClock.elapsedRealtime()-start)
                        +"|threadCpuNs="+(android.os.Debug.threadCpuTimeNanos()-cpuStart)
                        +"|ready="+result.ready+"|enteredWriter="+result.enteredWriter
                        +"|status="+result.status+"|detail="+result.detail
                        +"|cause="+(result.cause==null?"NONE":DatabaseCoordinator.describeCauseChain(result.cause,12000)));
                try(Cursor c=store.readable().rawQuery("SELECT step_id,status,error_code FROM "
                        +SchemaDomainCoordinatorR926.TABLE_JOURNAL
                        +" WHERE domain='RESEARCH' ORDER BY started_at DESC LIMIT 1",null)) {
                    if(c.moveToFirst()) {String last=c.getString(0)+"|"+c.getString(1)+"|"+c.getString(2);
                        if(!last.equals(prior)){System.out.println("SCHEMA_OWNED_LAST_JOURNAL|"+last);prior=last;}}
                }
                if(result.ready){System.out.println("SCHEMA_OWNED_READY");return;}
                assertTrue(result.status+"|"+result.detail,SystemClock.elapsedRealtime()<until);
                SystemClock.sleep(20L);
            }
        } finally {snapshot(store.readable());}
    }

}
