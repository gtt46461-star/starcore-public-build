package com.ethk3.novaonepro.strategyv2;

import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;
import com.ethk3.novaonepro.ServiceHeartbeatStore;

/** Isolate explicit database tests from the product's default automatic recovery service. */
public final class Su1AndroidTestRunner extends AndroidJUnitRunner {
    @Override public Application newApplication(ClassLoader loader,String name,Context context)
            throws InstantiationException,IllegalAccessException,ClassNotFoundException {
        Application app=super.newApplication(loader,name,context);
        // This runs before Application.onCreate can re-arm its default-enabled service.
        // Only the instrumentation sandbox is configured; no production default changes.
        new ServiceHeartbeatStore(app).setUserEnabled(false);
        return app;
    }
}
