package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import com.ethk3.novaonepro.AppConfig;
import java.util.function.LongSupplier;

/** CPU lease owned only by an explicitly enabled continuous research session.
 * OS expiry bounds an abandoned owner; renewing power never records numerical progress.
 * Prediction keeps its independent bounded PIT-to-canonical lease.
 */
public final class RuntimePowerLeaseR26 implements AutoCloseable {
    public static final long MAX_HOLD_MS=60000,RENEW_AFTER_MS=15000;
    interface Backend {void acquire(long millis);boolean isHeld();void release();}
    private final Backend backend;private final LongSupplier clock;
    private boolean closed;private long renewAt,expiresAt,renewals;
    public RuntimePowerLeaseR26(Context context,String owner){this(platform(context,owner),SystemClock::elapsedRealtime);}
    RuntimePowerLeaseR26(Backend backend,LongSupplier clock){if(backend==null||clock==null)throw new IllegalArgumentException("R26_POWER_LEASE_BACKEND");this.backend=backend;this.clock=clock;}
    private static Backend platform(Context context,String owner){
        PowerManager manager=(PowerManager)context.getSystemService(Context.POWER_SERVICE);if(manager==null)throw new IllegalStateException("R26_POWER_SERVICE_UNAVAILABLE");
        final PowerManager.WakeLock lock=manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,AppConfig.PROJECT_NAME+":R26_RESEARCH:"+owner);lock.setReferenceCounted(false);
        return new Backend(){public void acquire(long millis){lock.acquire(millis);}public boolean isHeld(){return lock.isHeld();}public void release(){lock.release();}};
    }
    public synchronized void keepAlive(){
        if(closed)throw new IllegalStateException("R26_POWER_LEASE_CLOSED");long now=clock.getAsLong();if(!backend.isHeld()||now>=renewAt){backend.acquire(MAX_HOLD_MS);renewAt=now+RENEW_AFTER_MS;expiresAt=now+MAX_HOLD_MS;renewals++;}
    }
    public synchronized boolean isHeld(){return backend.isHeld();}
    public synchronized long renewals(){return renewals;}
    public synchronized long expiresAtElapsedMs(){return expiresAt;}
    @Override public synchronized void close(){
        if(closed)return;closed=true;try{if(backend.isHeld())backend.release();}catch(RuntimeException timedOut){if(backend.isHeld())android.util.Log.w("STARCORE_R26","continuous research CPU lease release failed; OS timeout remains bounded",timedOut);}
    }
}
