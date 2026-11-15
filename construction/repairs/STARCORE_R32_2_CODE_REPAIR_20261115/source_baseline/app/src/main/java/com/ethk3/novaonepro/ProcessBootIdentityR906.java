package com.ethk3.novaonepro;

import android.os.Process;
import android.os.SystemClock;

/** Process-generation identity used to reject stale leases after Android process death. */
public final class ProcessBootIdentityR906 {
    private static final String ID = V51ChecksumEngine.sha256(
            "R906_BOOT|" + Process.myPid() + '|' + System.currentTimeMillis() + '|'
                    + SystemClock.elapsedRealtime() + '|' + System.nanoTime()).substring(0, 24);
    private ProcessBootIdentityR906() {}
    public static String id() { return ID; }
}
