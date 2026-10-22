package com.ethk3.novaonepro;

/** Read-only duplicate poll admission. New/conflicting/dirty or repaired-time input always
 * reaches the existing canonical writer; an identical confirmed row needs no second write.
 * Fetch receipts and reconciliation still execute on every validated provider response. */
public final class DrawPollingReplayR67 {
    private DrawPollingReplayR67() {}
    public static boolean alreadyDurable(Models.Draw saved,Models.Draw incoming) {
        return saved!=null&&incoming!=null&&saved.dirty==0&&incoming.dirty==0
                &&saved.isValid()&&incoming.isValid()&&saved.issue.equals(incoming.issue)
                &&saved.openTime>0L&&saved.openTime==incoming.openTime
                &&saved.sourceLine!=null&&!saved.sourceLine.isEmpty()
                &&java.util.Arrays.equals(saved.numbers,incoming.numbers);
    }
}
