package com.ethk3.novaonepro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit, non-exported alarm endpoint. It validates and submits only a recovery request. */
public final class InternalWatchdogReceiverR815 extends BroadcastReceiver {
    public static final String ACTION = "com.ethk3.novaonepro.R815_INTERNAL_WATCHDOG";
    public static final String EXTRA_GENERATION = "r815_generation";
    public static final String EXTRA_REASON = "r815_reason";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        final Context app=context.getApplicationContext()==null?context:context.getApplicationContext();
        final Intent copy=intent==null?null:new Intent(intent);
        final PendingResult pending=goAsync();
        RecoveryDispatchR965.submit(()->receiveOffMain(app,copy),()->{if(pending!=null)pending.finish();});
    }

    private static void receiveOffMain(Context context, Intent intent) {
        try {
            if (intent == null
                    || !ACTION.equals(intent.getAction())
                    || !ReleaseIdentityR815.APPLICATION_ID.equals(intent.getPackage())
                    || intent.getComponent() == null
                    || !InternalWatchdogReceiverR815.class.getName()
                            .equals(intent.getComponent().getClassName())) {
                record(context, "WATCHDOG_INTENT_REJECTED", "", null, false);
                return;
            }
            long generation = intent.getLongExtra(EXTRA_GENERATION, 0L);
            String reason = intent.getStringExtra(EXTRA_REASON);
            Context app = context.getApplicationContext() == null
                    ? context : context.getApplicationContext();
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            heartbeat.markAlarmTriggered(ACTION, heartbeat.getHeartbeatAgeMs());
            heartbeat.markRecoveryTrigger("alarm:" + (reason == null ? "" : reason));
            RecoveryCoordinatorR815.Result result =
                    RecoveryCoordinatorR815.request(context, reason, generation);
            if (!result.accepted)
                record(context, "WATCHDOG_RECOVERY_REJECTED", "", null, true);
        } catch (RuntimeException failure) {
            record(context, "WATCHDOG_RECEIVE_FAILED", "", failure, true);
        }
    }

    private static void record(
            Context context, String code, String target, Throwable failure, boolean retryable) {
        Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
        String detail =
                code
                        + "|target="
                        + (target == null ? "" : target)
                        + "|retryable="
                        + (retryable ? 1 : 0)
                        + "|cause="
                        + (failure == null ? "" : failure.getClass().getSimpleName());
        new ServiceHeartbeatStore(app).markRecoveryResult("r815_watchdog", false, detail);
        android.util.Log.e("R815Watchdog", detail);
    }
}
