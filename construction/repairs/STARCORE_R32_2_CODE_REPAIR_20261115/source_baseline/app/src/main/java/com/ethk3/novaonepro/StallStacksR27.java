package com.ethk3.novaonepro;

/** R27.3 diagnostics (debug builds only): for the first 30 minutes after the research runtime starts, logs the stacks of
 * the app's business and research threads every 60 s under tag STARCORE_STACK. The CI emulator prints these lines into
 * the job log, so a stalled fetch/analysis/settlement thread is identified by its blocking frame. */
public final class StallStacksR27 {
    private static volatile boolean started;
    private StallStacksR27() {}

    public static synchronized void start() {
        if (started || !BuildConfig.DEBUG) return;
        started = true;
        Thread t = new Thread(() -> {
            long end = System.currentTimeMillis() + 30 * 60_000L;
            int round = 0;
            while (System.currentTimeMillis() < end) {
                try { Thread.sleep(60_000L); } catch (InterruptedException e) { return; }
                round++;
                for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                    String name = e.getKey().getName();
                    if (!(name.startsWith("eth_k3_") || name.startsWith("starcore") || name.startsWith("r927") || name.startsWith("api-"))) continue;
                    StringBuilder b = new StringBuilder("round=").append(round).append('|').append(name).append('|').append(e.getKey().getState());
                    StackTraceElement[] st = e.getValue();
                    for (int i = 0; i < st.length && i < 18; i++) b.append("\n  at ").append(st[i]);
                    android.util.Log.w("STARCORE_STACK", b.toString());
                }
            }
        }, "starcore-stall-stacks");
        t.setDaemon(true);
        t.start();
    }
}
