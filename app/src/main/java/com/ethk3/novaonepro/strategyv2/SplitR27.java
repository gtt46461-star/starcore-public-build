package com.ethk3.novaonepro.strategyv2;

/** R27-016 four-way time split (docs/03 3.1): H_TRAIN | H_SELECT | H_HOLDOUT | (current open block) | LIVE.
 * H_HOLDOUT is the latest complete 720-ordinal grid block, so it rotates exactly once every 720 issues.
 * Rows of the still-open block are withheld from training/selection: they become the next sealed block. */
public final class SplitR27 {
    public static final int BLOCK = 720;
    public static final String TRAIN = "H_TRAIN", SELECT = "H_SELECT", HOLDOUT = "H_HOLDOUT";
    /** R30-B03-03: H_CONFIRM is the sealed H_HOLDOUT segment. All three ranges are a pure function of the dataset, so a
     * dataset hash fixes them; each candidate may read H_CONFIRM once per dataset hash (HoldoutLedgerR27.confirm). */
    public static final String CONFIRM = "H_CONFIRM";
    /** Role of row t: H_TRAIN, H_SELECT, H_CONFIRM, or OPEN (withheld current block / live). */
    public String role(int t) { return t < trainTo ? TRAIN : t < selectTo ? SELECT : t < holdoutTo && holdoutAvailable ? CONFIRM : "OPEN"; }
    public final int trainFrom, trainTo, selectFrom, selectTo, holdoutFrom, holdoutTo, openFrom;
    public final long holdoutBlock;
    public final boolean holdoutAvailable;

    private SplitR27(int tf, int tt, int sf, int st, int hf, int ht, int open, long block, boolean available) {
        trainFrom = tf; trainTo = tt; selectFrom = sf; selectTo = st; holdoutFrom = hf; holdoutTo = ht; openFrom = open; holdoutBlock = block; holdoutAvailable = available;
    }

    public static long blockOf(long ordinal) { return Math.floorDiv(ordinal, (long) BLOCK); }

    /** @param degraded holdout blocks whose exposure budget is exhausted (HoldoutLedgerR27). */
    public static SplitR27 of(ReplayDatasetR27 d, java.util.Set<Long> degraded) {
        int t = d.size();
        if (t < 3 * BLOCK) {
            // Too short for a sealed block: train/select only; no holdout claim is possible.
            int st = (int) Math.round(t * 0.7);
            return new SplitR27(0, st, st, t, t, t, t, -1, false);
        }
        long current = blockOf(d.ordinals[t - 1]);
        long block = current - 1;
        int hf = firstAtLeast(d.ordinals, block * BLOCK), ht = firstAtLeast(d.ordinals, current * BLOCK);
        if (ht - hf < BLOCK / 2) { // the previous grid block is mostly missing: use the one before it
            block = current - 2; ht = hf; hf = firstAtLeast(d.ordinals, block * BLOCK);
        }
        boolean ok = ht - hf >= BLOCK / 2 && (degraded == null || !degraded.contains(block));
        int pre = hf;
        int tt = (int) Math.round(pre * (60.0 / 85.0));
        return new SplitR27(0, tt, tt, pre, hf, ht, ht, block, ok);
    }

    static int firstAtLeast(long[] sorted, long value) {
        int lo = 0, hi = sorted.length;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (sorted[mid] < value) lo = mid + 1; else hi = mid; }
        return lo;
    }

    public int trainSize() { return trainTo - trainFrom; }
    public int selectSize() { return selectTo - selectFrom; }
    public int holdoutSize() { return holdoutTo - holdoutFrom; }
    public String describe() {
        return "train=[" + trainFrom + "," + trainTo + ") select=[" + selectFrom + "," + selectTo + ") holdout=[" + holdoutFrom + "," + holdoutTo + ") block=" + holdoutBlock + " available=" + holdoutAvailable + " open=[" + openFrom + ",..)";
    }
}
