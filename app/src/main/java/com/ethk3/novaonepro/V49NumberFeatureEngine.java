package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Strict point-in-time 10-position x 10-number feature builder. */
public final class V49NumberFeatureEngine {
    public static final String MODULE_VERSION = V49BuildManifest.FEATURE_SCHEMA;

    public V49FeatureSnapshot build(List<Models.Draw> input, String targetIssue) {
        ArrayList<Models.Draw> history = clean(input);
        V49FeatureSnapshot out = new V49FeatureSnapshot();
        out.historySize = history.size();
        if (history.size() == 0) return out;
        Models.Draw latest = history.get(history.size() - 1);
        out.baseIssue = latest.issue == null ? "" : latest.issue;
        out.targetIssue = targetIssue == null ? "" : targetIssue;
        for (int p = 0; p < 10; p++) {
            out.lastNumber[p] = latest.getNumberAtPosition(p + 1);
            for (int wi = 0; wi < out.windows.length; wi++) {
                int observed = Math.min(out.windows[wi], history.size());
                int[] count = new int[10];
                int start = history.size() - observed;
                for (int i = start; i < history.size(); i++) {
                    int n = history.get(i).getNumberAtPosition(p + 1);
                    if (n >= 1 && n <= 10) count[n - 1]++;
                }
                for (int n = 0; n < 10; n++) {
                    out.frequency[wi][p][n] =
                            FeatureContractRegistry.smoothFrequency(count[n], observed);
                }
                FeatureContractRegistry.normalize10(out.frequency[wi][p]);
            }
            out.entropy[p] = FeatureContractRegistry.entropy10(out.frequency[2][p]);
            out.concentration[p] = top3Sum(out.frequency[2][p]);
            for (int n = 0; n < 10; n++) {
                int number = n + 1;
                int gap = currentGap(history, p + 1, number);
                out.gapNorm[p][n] = Math.min(60, gap) / 60.0d;
                out.gapPercentile[p][n] = historicalGapPercentile(history, p + 1, number, gap);
                out.slope[p][n] = out.frequency[1][p][n] - out.frequency[3][p][n];
                out.acceleration[p][n] =
                        (out.frequency[0][p][n] - out.frequency[1][p][n])
                                - (out.frequency[2][p][n] - out.frequency[3][p][n]);
                out.volatility[p][n] = blockVolatility(history, p + 1, number, 24, 6);
                int last = out.lastNumber[p];
                out.lastDistance[p][n] = last <= 0 ? 0.5d : Math.abs(number - last) / 9.0d;
                out.neighborLast[p][n] = last > 0 && Math.abs(number - last) == 1 ? 1.0d : 0.0d;
                out.sameParityLast[p][n] = last > 0 && ((number & 1) == (last & 1)) ? 1.0d : 0.0d;
                out.sameBigLast[p][n] = last > 0 && ((number >= 6) == (last >= 6)) ? 1.0d : 0.0d;
            }
        }
        V49WindowStateEngine.apply(out, history);
        out.checksum = FeatureContractRegistry.sha256(serialize(out));
        return out;
    }

    private static ArrayList<Models.Draw> clean(List<Models.Draw> input) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (input == null) return out;
        int start = Math.max(0, input.size() - AppConfig.V49_HISTORY_LIMIT);
        for (int i = start; i < input.size(); i++) {
            Models.Draw d = input.get(i);
            if (d != null && d.isValid()) out.add(d);
        }
        return out;
    }

    private static int currentGap(List<Models.Draw> h, int position, int number) {
        int gap = 0;
        for (int i = h.size() - 1; i >= 0; i--) {
            if (h.get(i).getNumberAtPosition(position) == number) return gap;
            gap++;
        }
        return gap;
    }

    private static double historicalGapPercentile(
            List<Models.Draw> h, int position, int number, int current) {
        ArrayList<Integer> gaps = new ArrayList<Integer>();
        int last = -1;
        for (int i = 0; i < h.size(); i++) {
            if (h.get(i).getNumberAtPosition(position) == number) {
                if (last >= 0) gaps.add(Integer.valueOf(i - last - 1));
                last = i;
            }
        }
        if (gaps.size() < 3) return Math.min(1.0d, current / 20.0d);
        int le = 0;
        for (int i = 0; i < gaps.size(); i++) if (gaps.get(i).intValue() <= current) le++;
        return ((double) le) / ((double) gaps.size());
    }

    private static double blockVolatility(
            List<Models.Draw> h, int position, int number, int window, int block) {
        int observed = Math.min(window, h.size());
        if (observed <= 0) return 0.0d;
        int start = h.size() - observed;
        ArrayList<Double> rates = new ArrayList<Double>();
        for (int s = start; s < h.size(); s += block) {
            int end = Math.min(h.size(), s + block);
            int hit = 0;
            for (int i = s; i < end; i++)
                if (h.get(i).getNumberAtPosition(position) == number) hit++;
            rates.add(Double.valueOf(FeatureContractRegistry.smoothFrequency(hit, end - s)));
        }
        double mean = 0.0d;
        for (int i = 0; i < rates.size(); i++) mean += rates.get(i).doubleValue();
        mean /= Math.max(1, rates.size());
        double var = 0.0d;
        for (int i = 0; i < rates.size(); i++) {
            double x = rates.get(i).doubleValue() - mean;
            var += x * x;
        }
        return FeatureContractRegistry.clamp01(Math.sqrt(var / Math.max(1, rates.size())) * 4.0d);
    }

    private static double top3Sum(double[] p) {
        ArrayList<Double> v = new ArrayList<Double>();
        for (int i = 0; i < p.length; i++) v.add(Double.valueOf(p[i]));
        Collections.sort(v, Collections.reverseOrder());
        return v.get(0).doubleValue() + v.get(1).doubleValue() + v.get(2).doubleValue();
    }

    private static String serialize(V49FeatureSnapshot s) {
        StringBuilder b = new StringBuilder();
        b.append(V49BuildManifest.FEATURE_SCHEMA)
                .append('|')
                .append(s.baseIssue)
                .append('|')
                .append(s.targetIssue);
        for (int p = 0; p < 10; p++) {
            b.append('|').append(s.family[p]).append(':').append(s.lifecycle[p]);
            for (int n = 0; n < 10; n++) {
                b.append(',').append(Double.doubleToLongBits(s.frequency[4][p][n]));
                b.append(',').append(Double.doubleToLongBits(s.slope[p][n]));
                b.append(',').append(Double.doubleToLongBits(s.gapPercentile[p][n]));
            }
        }
        return b.toString();
    }
}
