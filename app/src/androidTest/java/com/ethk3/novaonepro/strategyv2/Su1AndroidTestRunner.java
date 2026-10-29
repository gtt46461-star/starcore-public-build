package com.ethk3.novaonepro.strategyv2;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;
import com.ethk3.novaonepro.ServiceHeartbeatStore;

/** Isolate explicit database tests from the product's default automatic recovery service. */
public final class Su1AndroidTestRunner extends AndroidJUnitRunner {
    private boolean heapProbeOnly;
    @Override public void onCreate(Bundle arguments){
        heapProbeOnly=arguments!=null&&"true".equals(arguments.getString("r26HeapProbeOnly"));
        super.onCreate(arguments);
    }
    @Override public void onStart(){
        if(!heapProbeOnly){super.onStart();return;}
        Bundle receipt=new Bundle();
        receipt.putString("r26HeapEnvironment","{\"api\":"+android.os.Build.VERSION.SDK_INT
                +",\"maxMemoryBytes\":"+Runtime.getRuntime().maxMemory()
                +",\"largeHeapFlag\":"+((getTargetContext().getApplicationInfo().flags
                &android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP)!=0)+"}");
        finish(android.app.Activity.RESULT_OK,receipt);
    }
    @Override public Application newApplication(ClassLoader loader,String name,Context context)
            throws InstantiationException,IllegalAccessException,ClassNotFoundException {
        Application app=super.newApplication(loader,name,context);
        // This runs before Application.onCreate can re-arm its default-enabled service.
        // Only the instrumentation sandbox is configured; no production default changes.
        new ServiceHeartbeatStore(app).setUserEnabled(false);
        return app;
    }
}
