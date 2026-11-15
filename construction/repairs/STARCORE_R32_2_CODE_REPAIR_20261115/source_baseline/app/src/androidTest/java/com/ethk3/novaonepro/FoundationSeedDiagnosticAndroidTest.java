package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Debug;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.r952.DevelopmentProductionCP12;
import com.ethk3.novaonepro.r952.ModernSeedFactoryCP12;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Bounded retained-fixture observations; JUnit success is not full-import acceptance. */
@RunWith(AndroidJUnit4.class)
public final class FoundationSeedDiagnosticAndroidTest {
    private static String checkpoint(DbStore store) {
        try(Cursor c=store.readable().rawQuery("SELECT scan_row,scan_offset,material_row,material_offset,generation,state FROM "
                +ModernSeedFactoryCP12.CHECKPOINT+" ORDER BY source_hash",null)) {
            assertTrue("retained checkpoint required",c.moveToFirst());
            return c.getInt(0)+":"+c.getLong(1)+":"+c.getInt(2)+":"+c.getLong(3)+":"+c.getLong(4)+":"+c.getString(5);
        }
    }
    private static String pressure() {
        RealtimePressureGateR811.Snapshot p=RealtimePressureGateR811.snapshot();
        return "analysis="+p.analysisDepth+",settlement="+p.settlementDepth
                +",active="+RealtimePressureGateR811.hasActiveCriticalWork()
                +",settlementGate="+SettlementExecutionGateR819.isActive();
    }
    @Test public void observeRetainedSeedSlicesDefaultServiceState(){observe(false);}
    @Test public void observeRetainedSeedSlicesServiceDisabled(){observe(true);}
    private static void observe(boolean disableService) {
        Context context=ApplicationProvider.getApplicationContext();
        if(disableService) {
            BootReceiver.persistServiceUserEnabled(context,false);
            BootReceiver.cancelAllRecovery(context,"SYNTHETIC_SEED_DIAGNOSTIC");
            context.stopService(new Intent(context,AutoFetchService.class));
            long stopBy=SystemClock.elapsedRealtime()+10000L;
            while(AnalysisSingleFlightR36.shared().currentLease()!=null
                    &&SystemClock.elapsedRealtime()<stopBy)SystemClock.sleep(20L);
            assertNull(AnalysisSingleFlightR36.shared().currentLease());
        }
        DbStore store=new DbStore(context);Map<String,Integer> outcomes=new LinkedHashMap<>();
        int calls=0;boolean reportedYieldStack=false;long stopBy=SystemClock.elapsedRealtime()+120000L;
        try {
            while(calls<200&&SystemClock.elapsedRealtime()<stopBy
                    &&!ModernSeedFactoryCP12.completeCP12(store.readable())) {
                String before=checkpoint(store),beforePressure=pressure(),result="";boolean deferred=false;
                Throwable yieldStack=null;
                long start=SystemClock.elapsedRealtime(),cpu=Debug.threadCpuTimeNanos();
                ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(250L);
                try {result=ModernSeedFactoryCP12.maintain(store,System.nanoTime()+250000000L);}
                catch(ResearchWorkBudgetR927.Yield yielded){result="COOPERATIVE_YIELD:"+yielded.getMessage();yieldStack=yielded;}
                catch(DevelopmentProductionCP12.Wait wait){if(!"TIME_BUDGET_EXCEEDED".equals(wait.reason))throw wait;result="WAIT_TIME_BUDGET_EXCEEDED";}
                catch(IllegalStateException failure) {
                    Throwable cause=failure.getCause();String message=failure.getMessage();
                    if(message==null||!message.startsWith("F01_WRITE_FAILED|operation=CP12_MOLECULE_")
                            ||!(cause instanceof IllegalStateException)
                            ||!("CP3_DB_BUSY_DEADLINE".equals(cause.getMessage())
                                ||"CP3_DB_BUSY_DEADLINE_AFTER_ACQUIRE".equals(cause.getMessage())))throw failure;
                    assertEquals(before,checkpoint(store));result="PREWRITE_"+cause.getMessage();deferred=true;
                } finally {ResearchWorkBudgetR927.leave(scope);}
                long wall=SystemClock.elapsedRealtime()-start,cpuNs=Debug.threadCpuTimeNanos()-cpu;
                String after=checkpoint(store);outcomes.put(result,outcomes.getOrDefault(result,0)+1);calls++;
                System.out.println("SEED_DIAGNOSTIC|call="+calls+"|serviceDisabled="+disableService
                        +"|wallMs="+wall+"|threadCpuNs="+cpuNs+"|before="+before+"|after="+after
                        +"|pressureBefore="+beforePressure+"|pressureAfter="+pressure()+"|result="+result);
                if(yieldStack!=null&&!reportedYieldStack) {
                    System.out.println("SEED_DIAGNOSTIC_FIRST_YIELD_STACK");yieldStack.printStackTrace(System.out);reportedYieldStack=true;
                }
                if(deferred)SystemClock.sleep(1000L);
            }
            System.out.println("SEED_DIAGNOSTIC_SUMMARY|calls="+calls+"|serviceDisabled="+disableService
                    +"|complete="+ModernSeedFactoryCP12.completeCP12(store.readable())
                    +"|checkpoint="+checkpoint(store)+"|outcomes="+outcomes
                    +"|scope=OBSERVATION_ONLY_NOT_FOUNDATION_ACCEPTANCE");
        } finally {store.close();}
    }
}
