package com.ethk3.novaonepro;

/**
 * Fixed lightweight projection for every multi-row history read.
 *
 * <p>{@code raw_json} is intentionally absent.  Provider payload provenance remains persisted on
 * the draw row, but prediction, settlement and research history never need to duplicate it in the
 * Java heap.  Keeping the projection in one immutable owner prevents a later query from silently
 * reintroducing the R816 N-times-payload OOM.
 */
public final class DrawHistoryProjectionR817 {
    public static final String[] COLUMNS =
            new String[] {
                "_id",
                "issue",
                "open_time",
                "fetch_time",
                "source_line",
                "n1",
                "n2",
                "n3",
                "n4",
                "n5",
                "n6",
                "n7",
                "n8",
                "n9",
                "n10",
                "dirty",
                "dirty_reason"
            };

    private DrawHistoryProjectionR817() {}

    public static boolean excludesRawPayload() {
        for (int i = 0; i < COLUMNS.length; i++) {
            if ("raw_json".equals(COLUMNS[i])) return false;
        }
        return true;
    }
}
