package com.ethk3.novaonepro;

import android.os.Build;
import android.view.Choreographer;
import java.util.Locale;

/** Read-only frame and truth-bind telemetry, including a separate active-scroll cohort. */
public final class FrameMetricsR906 implements Choreographer.FrameCallback {
    private static final long SLOW_NS = 33_333_334L;
    private static final long SEVERE_NS = 50_000_000L;
    private static final long SCROLL_ACTIVE_NS = 450_000_000L;
    private static volatile String latestSummary = "R940_FRAME|status=NOT_STARTED";

    private long lastFrameNs, lastPublishedNs;
    private long frames, slowFrames, severeFrames, maxFrameNs;
    private long scrollFrames, scrollSlowFrames, scrollSevereFrames, scrollActiveUntilNs;
    private long totalBindNs, maxBindNs, binds;
    private boolean running;

    public void start() {
        if (running) return;
        running = true;
        lastFrameNs = 0L;
        if (Build.VERSION.SDK_INT >= 16) {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    public void stop() {
        latestSummary = buildSummary();
        running = false;
        if (Build.VERSION.SDK_INT >= 16) {
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    /** Called by the root scroll listener; no polling and no business-state side effect. */
    public void markScrollActivity() {
        scrollActiveUntilNs = System.nanoTime() + SCROLL_ACTIVE_NS;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (lastFrameNs > 0L) {
            long duration = Math.max(0L, frameTimeNanos - lastFrameNs);
            frames++;
            if (duration > SLOW_NS) slowFrames++;
            if (duration > SEVERE_NS) severeFrames++;
            maxFrameNs = Math.max(maxFrameNs, duration);
            if (System.nanoTime() <= scrollActiveUntilNs) {
                scrollFrames++;
                if (duration > SLOW_NS) scrollSlowFrames++;
                if (duration > SEVERE_NS) scrollSevereFrames++;
            }
        }
        lastFrameNs = frameTimeNanos;
        if (frameTimeNanos - lastPublishedNs >= 2_000_000_000L) {
            latestSummary = buildSummary(); lastPublishedNs = frameTimeNanos;
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    public long beginBind() {
        return System.nanoTime();
    }

    public void endBind(long started) {
        long duration = Math.max(0L, System.nanoTime() - started);
        binds++;
        totalBindNs += duration;
        maxBindNs = Math.max(maxBindNs, duration);
    }

    public String summary() {
        latestSummary = buildSummary();
        return latestSummary;
    }

    /** Last bounded in-process snapshot for the exported complete diagnostic report. */
    public static String latestSummary() {
        return latestSummary;
    }

    private String buildSummary() {
        return String.format(
                Locale.US,
                "R914_FRAME|frames=%d|slow33=%d|severe50=%d|maxMs=%.3f|"
                        + "scrollFrames=%d|scrollSlow33=%d|scrollSevere50=%d|scrollSlowRate=%.6f|"
                        + "truthBinds=%d|truthBindAvgMs=%.3f|truthBindMaxMs=%.3f",
                frames,
                slowFrames,
                severeFrames,
                maxFrameNs / 1_000_000.0d,
                scrollFrames,
                scrollSlowFrames,
                scrollSevereFrames,
                scrollFrames <= 0 ? 0.0d : (double) scrollSlowFrames / scrollFrames,
                binds,
                binds <= 0 ? 0.0d : (double) totalBindNs / 1_000_000.0d / binds,
                maxBindNs / 1_000_000.0d);
    }
}
