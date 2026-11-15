package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BootReceiver;
import com.ethk3.novaonepro.ServiceHeartbeatStore;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class StartupLeaseR13Test {
    @SuppressWarnings("unchecked")
    private static void restore(SharedPreferences prefs, Map<String,?> values) {
        SharedPreferences.Editor e=prefs.edit().clear();
        for(Map.Entry<String,?> row:values.entrySet()) {
            Object v=row.getValue();String k=row.getKey();
            if(v instanceof String)e.putString(k,(String)v);
            else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);
            else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);
            else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);
        }
        assertTrue(e.commit());
    }
    private void verify(boolean legacyLease) {
        Context app=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=app.getSharedPreferences("v20_service_runtime_light",Context.MODE_PRIVATE);
        SharedPreferences device=android.os.Build.VERSION.SDK_INT>=24
                ?app.createDeviceProtectedStorageContext().getSharedPreferences("v20_service_runtime_light",Context.MODE_PRIVATE):prefs;
        Map<String,?> original=prefs.getAll(), originalDevice=device.getAll();
        try {
            long now=System.currentTimeMillis();int pid=android.os.Process.myPid();
            SharedPreferences.Editor e=prefs.edit().putBoolean("user_enabled",true)
                    .putInt("process_id",pid+100000).putInt("process_alive",1)
                    .putLong("heartbeat_at",now).putLong("runtime_lease_until",now+45000)
                    .putLong("service_ready_at",now).putLong("last_start_at",now)
                    .putString("init_status","READY").putLong("recovery_lease_until",now+120000)
                    .putLong("recovery_lease_acquired_at",now);
            if(legacyLease)e.remove("recovery_lease_process_id");
            else e.putInt("recovery_lease_process_id",pid+100000);
            assertTrue(e.commit());
            ServiceHeartbeatStore state=new ServiceHeartbeatStore(app);
            assertFalse("dead process heartbeat cannot suppress service start",state.isServiceHealthyForRecovery());
            assertEquals("STALE_WAIT_RECOVERY",state.runtimeTruthState());
            assertTrue("old process lease must not block new process",state.tryAcquireRecoveryLease("r13-device-test"));
            assertEquals(pid,prefs.getInt("recovery_lease_process_id",0));
            assertEquals("STEAL_PREVIOUS_PROCESS",prefs.getString("recovery_lease_mode",""));
            assertFalse("same-process lease still coalesces duplicate requests",state.tryAcquireRecoveryLease("duplicate"));
            state.setUserEnabled(false);
            assertFalse("automatic recovery cannot undo an explicit stop",BootReceiver.reconcileAutoFetchService(app,"r13-user-disabled"));
            assertFalse(state.isUserEnabled());
        } finally {restore(prefs,original);if(device!=prefs)restore(device,originalDevice);}
    }
    @Test public void newProcessReclaimsLegacyLeaseWithoutOwnerField(){verify(true);}
    @Test public void newProcessReclaimsOwnedLeaseAndPreservesCurrentOwnerAndUserStop(){verify(false);}
}
