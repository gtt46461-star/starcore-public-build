package com.ethk3.novaonepro;

import android.content.Context;
import android.os.Process;
import java.util.concurrent.atomic.AtomicBoolean;

/** R912 launch fence: expensive installed-artifact verification never runs on the UI thread. */
public final class StartupHealthR912 {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private StartupHealthR912() {}

    public static void verifyInstalledArtifactAsync(Context context) {
        if (context == null || !STARTED.compareAndSet(false, true)) return;
        final Context app = context.getApplicationContext() == null
                ? context : context.getApplicationContext();
        Thread worker = new Thread(
                new Runnable() {
                    @Override public void run() {
                        try {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                            RecoveryDispatchR965.warmUpOffMain();
                            RealtimePressureGateR811.initializeCrossProcessOffMain();
                            // Result is retained by BuildProvenanceR815.lastSnapshot(). Do not
                            // overwrite the live service/UI crash stage from this side thread.
                            BuildProvenanceR815.verifyRuntime(app);
                        } catch (Throwable failure) {
                            CrashRecorder.recordCaught(
                                    app, failure, "R912_ASYNC_ARTIFACT_VERIFY");
                        }
                    }
                },
                "starcore-r912-artifact-verify");
        worker.setDaemon(true);
        worker.start();
    }
}
