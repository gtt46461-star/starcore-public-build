package com.ethk3.novaonepro;

import android.app.Application;

/**
 * R3.2.2 intentionally-light process entry point. No SQLite, HTTP, model graph, prediction,
 * settlement or BET work is allowed here.
 */
public final class StarcoreApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KeyProvider.initialize(getApplicationContext());
        if (!isMainRuntimeProcess(this)) return;
        RealtimePressureGateR811.configureCrossProcess(this);
        try {
            CrashRecorder.install(getApplicationContext());
            // R912: hashing the installed APK and every classes*.dex entry can take seconds on
            // slower/OEM storage. Application.onCreate() is the Android launch critical path,
            // therefore only the generated manifest may be read synchronously here.
            BuildProvenanceR815.Snapshot provenance =
                    BuildProvenanceR815.fromGeneratedManifest();
            String identity = ReleaseIdentityR815.isConsistent() ? "IDENTITY_OK" : "IDENTITY_MISMATCH";
            CrashRecorder.setRuntimeContext("", "APPLICATION_LIGHT_BOOT_R912|" + identity + '|' + provenance.status);
            StartupHealthR912.verifyInstalledArtifactAsync(getApplicationContext());
            // Re-arm only lightweight Alarm/Job recovery.  No SQLite, HTTP, prediction,
            // settlement or model work is allowed from Application.onCreate().
            if (new ServiceHeartbeatStore(getApplicationContext()).isUserEnabled())
                BootReceiver.scheduleAllRecovery(
                        getApplicationContext(), "R912_APPLICATION_PROCESS_REARM");
        } catch (Throwable failure) {
            // Application startup remains light, but the caught failure is preserved by the crash
            // recorder instead of being silently discarded.
            CrashRecorder.recordCaught(getApplicationContext(), failure, "APPLICATION_LIGHT_BOOT_R912");
        }
    }
    static boolean isMainRuntimeProcess(android.content.Context context) {
        String name = null;
        if (android.os.Build.VERSION.SDK_INT >= 28) name = Application.getProcessName();
        else {
            android.app.ActivityManager manager = (android.app.ActivityManager)
                    context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> list =
                    manager == null ? null : manager.getRunningAppProcesses();
            if (list != null) for (android.app.ActivityManager.RunningAppProcessInfo item : list)
                if (item.pid == android.os.Process.myPid()) { name = item.processName; break; }
        }
        // Unknown identity fails closed. No DB or recovery request in any secondary process.
        return context.getPackageName().equals(name);
    }
}
