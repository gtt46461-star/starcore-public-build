package com.ethk3.novaonepro;

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ChartPageReadR34Test {
    private final Handler timer = new Handler(Looper.getMainLooper());

    @Test public void middleTimeoutLeavesBothAdjacentPanelsAndPageCancellationStillStopsReads() {
        CancellationSignal page = new CancellationSignal();
        ResearchVisualDataR82.Panel original = new ResearchVisualDataR82.Panel(
                "charts", "original", "STATUS", "test-source", "", "", "ORIGINAL", null, null);
        ResearchVisualDataR82.Panel timeout = new ResearchVisualDataR82.Panel(
                "charts", "slow", "STATUS", "test-source", "", "", "READ_TIMEOUT", null, null);
        assertSame(original, ChartPageReadR34.panel(page, timer, 1000, child -> original, timeout));
        assertSame(timeout, ChartPageReadR34.panel(page, timer, 40, child -> {
            CountDownLatch stopped = new CountDownLatch(1);
            child.setOnCancelListener(stopped::countDown);
            try { assertTrue("deadline must interrupt the active read", stopped.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException interrupted) { throw new AssertionError(interrupted); }
            child.throwIfCanceled();
            return original;
        }, timeout));
        assertFalse(page.isCanceled());
        assertSame(original, ChartPageReadR34.panel(page, timer, 1000, child -> original, timeout));
        try {
            ChartPageReadR34.panel(page, timer, 1000, child -> {
                page.cancel();
                child.throwIfCanceled();
                return original;
            }, timeout);
            fail("closed page rendered a panel");
        } catch (OperationCanceledException expected) { assertTrue(page.isCanceled()); }
    }
}
