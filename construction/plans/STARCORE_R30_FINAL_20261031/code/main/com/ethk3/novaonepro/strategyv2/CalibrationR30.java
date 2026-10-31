package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * R30-B06-01 校准质量（Q05 唯一口径）。纯Java，HOST可测。
 *
 * 与 StatsR27.ece（等宽箱）并列报告，不替换旧口径：
 *  - eceEqualMass：按预测概率排序后分成 bins 个等样本箱（最后一箱吸收余数），|Σp − Σy|/n 求和；
 *  - brier：mean((p−y)²)；
 *  - bss：1 − brier / brier_base，base 为同样本的常数基线概率（K3 命中基线 0.30），样本不足 200 返回 null；
 *  - supportCoverage：落在“训练期出现过的概率区间” [lo, hi] 内的样本占比（Q05 要求 ≥ 0.90）。
 * 输入必须是“按时间隔离”拟合后的样本：拟合只用 ordinal < 被评估样本 ordinal 的数据（由调用方保证，PitGuardR30 已检查）。
 */
public final class CalibrationR30 {
    public static final int MIN_SAMPLES = 200;
    public static final double BASE = 0.30;
    private CalibrationR30() {}

    public static Map<String, Object> evaluate(double[] p, int[] y, int n, int bins, double supportLo, double supportHi) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("n", n);
        if (n <= 0) { m.put("eceEqualMass", null); m.put("brier", null); m.put("bss", null); m.put("supportCoverage", null); m.put("q05Pass", null); return m; }
        double brier = 0, brierBase = 0; int inSupport = 0;
        for (int i = 0; i < n; i++) {
            double d = p[i] - y[i]; brier += d * d;
            double b = BASE - y[i]; brierBase += b * b;
            if (p[i] >= supportLo && p[i] <= supportHi) inSupport++;
        }
        brier /= n; brierBase /= n;
        double ece = eceEqualMass(p, y, n, bins);
        Double bss = n < MIN_SAMPLES || brierBase <= 0 ? null : 1 - brier / brierBase;
        double coverage = inSupport / (double) n;
        m.put("eceEqualMass", ece);
        m.put("eceEqualWidth", StatsR27.ece(p, y, n, bins));
        m.put("brier", brier);
        m.put("bss", bss);
        m.put("supportCoverage", coverage);
        m.put("q05Pass", n < MIN_SAMPLES ? null : (Object) (ece <= 0.04 && coverage >= 0.90));
        return m;
    }

    public static double eceEqualMass(double[] p, int[] y, int n, int bins) {
        if (n <= 0) return Double.NaN;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(p[a], p[b]));
        int k = Math.max(1, Math.min(bins, n)), size = n / k;
        double e = 0;
        for (int b = 0; b < k; b++) {
            int from = b * size, to = b == k - 1 ? n : from + size;
            double sp = 0, sy = 0;
            for (int j = from; j < to; j++) { sp += p[idx[j]]; sy += y[idx[j]]; }
            e += Math.abs(sp - sy) / n;
        }
        return e;
    }
}
