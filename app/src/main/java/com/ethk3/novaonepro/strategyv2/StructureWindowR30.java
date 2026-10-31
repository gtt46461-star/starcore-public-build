package com.ethk3.novaonepro.strategyv2;

/** R30-B04-04 短期结构窗口：纯函数，只读 [target-w, target) 区间的开奖（PIT 安全）。 */
public final class StructureWindowR30 {
    public static final int[] WINDOWS = {8, 32, 128};
    private StructureWindowR30() {}

    /** draws[t][p]：第 t 期第 p 位（0..9）的号码（1..10）。返回 counts[p][n]（n=1..10），统计 [target-w, target)。 */
    public static int[][] counts(int[][] draws, int target, int w) {
        if (w < 1 || target < w || target > draws.length) throw new IllegalArgumentException("R30_WINDOW_RANGE:" + target + "/" + w);
        int[][] c = new int[10][11];
        for (int t = target - w; t < target; t++) for (int p = 0; p < 10; p++) {
            int n = draws[t][p];
            if (n < 1 || n > 10) throw new IllegalArgumentException("R30_WINDOW_DRAW_VALUE:" + t + ":" + p);
            c[p][n]++;
        }
        return c;
    }

    /** 某位置在窗口内出现最多的 3 个号码（并列按号码升序）。 */
    public static int[] top3(int[][] counts, int position1to10) {
        int[] row = counts[position1to10 - 1]; int[] best = {0, 0, 0};
        for (int k = 0; k < 3; k++) {
            int arg = -1;
            for (int n = 1; n <= 10; n++) {
                if (n == best[0] || n == best[1]) continue;
                if (arg < 0 || row[n] > row[arg]) arg = n;
            }
            best[k] = arg;
        }
        return best;
    }
}
