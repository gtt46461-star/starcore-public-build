package com.ethk3.novaonepro;

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.OperationCanceledException;

/** Each chart has its own read deadline. Closing the page cancels the active read. */
final class ChartPageReadR34 {
    private ChartPageReadR34() {}

    interface Read<T> {
        T run(CancellationSignal signal);
    }

    static <T> T read(CancellationSignal page, Handler timer, long timeoutMs, Read<T> reader) {
        page.throwIfCanceled();
        CancellationSignal item = new CancellationSignal();
        Runnable deadline = item::cancel;
        page.setOnCancelListener(item::cancel);
        timer.postDelayed(deadline, timeoutMs);
        try {
            item.throwIfCanceled();
            T value = reader.run(item);
            item.throwIfCanceled();
            return value;
        } finally {
            timer.removeCallbacks(deadline);
            page.setOnCancelListener(null);
        }
    }

    static ResearchVisualDataR82.Panel panel(CancellationSignal page, Handler timer,
            long timeoutMs, Read<ResearchVisualDataR82.Panel> reader,
            ResearchVisualDataR82.Panel timedOut) {
        try {
            return read(page, timer, timeoutMs, reader);
        } catch (OperationCanceledException stopped) {
            page.throwIfCanceled();
            return timedOut;
        }
    }
}
