package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Strict point-in-time V50 feature owner for all ten positions and ten numbers. */
public final class V50NumberFeatureEngine {
    public static final String MODULE_VERSION = V50BuildManifest.FEATURE_SCHEMA;
    public static final int[] WINDOWS = new int[] {6, 12, 24, 48, 96, 180, 360, 720, 1440, 2880};

    private final V50RelationGraphVerifier relationVerifier = new V50RelationGraphVerifier();

    public V50Models.NumberFeatureSnapshot build(
            V50Models.PredictionContext context, List<Models.Draw> input) {
        ArrayList<Models.Draw> history = clean(input);
        if (context == null || history.size() < 12) {
            return new V50Models.NumberFeatureSnapshot(
                    context,
                    new V50Models.NumberFeatureRow[0],
                    V50ChecksumEngine.sha256("V50_FEATURE_INVALID"));
        }
        V50RelationGraphVerifier.Result relation = relationVerifier.evaluate(history);
        V50Models.NumberFeatureRow[] rows = new V50Models.NumberFeatureRow[100];
        int index = 0;
        StringBuilder snapshotHash =
                new StringBuilder(MODULE_VERSION)
                        .append('|')
                        .append(context.targetIssue)
                        .append('|')
                        .append(context.historyHash)
                        .append('|')
                        .append(relation.checksum);
        for (int position = 1; position <= 10; position++) {
            double[][] frequency = frequencies(history, position);
            double entropy = FeatureContractRegistry.entropy10(frequency[2]);
            double concentration = top3Sum(frequency[2]);
            double drift = js(frequency[2], frequency[6]);
            int last = history.get(history.size() - 1).getNumberAtPosition(position);
            for (int number = 1; number <= 10; number++) {
                int n = number - 1;
                int gap = currentGap(history, position, number);
                GapEvidence gapEvidence = gapEvidence(history, position, number, gap);
                double slope = frequency[1][n] - frequency[3][n];
                double acceleration =
                        (frequency[0][n] - frequency[1][n]) - (frequency[2][n] - frequency[3][n]);
                double volatility = blockVolatility(history, position, number, 24, 6);
                double lastDistance = last <= 0 ? 0.5d : Math.abs(number - last) / 9.0d;
                double neighbor = last > 0 && Math.abs(number - last) == 1 ? 1.0d : 0.0d;
                double parity = last > 0 && ((number & 1) == (last & 1)) ? 1.0d : 0.0d;
                double big = last > 0 && ((number >= 6) == (last >= 6)) ? 1.0d : 0.0d;
                double attribute = 0.34d * neighbor + 0.33d * parity + 0.33d * big;
                double[] model =
                        new double[] {
                            (position - 1) / 9.0d,
                            (number - 1) / 9.0d,
                            frequency[0][n],
                            frequency[1][n],
                            frequency[2][n],
                            frequency[3][n],
                            frequency[4][n],
                            frequency[5][n],
                            frequency[6][n],
                            frequency[7][n],
                            frequency[8][n],
                            Math.min(60, gap) / 60.0d,
                            gapEvidence.percentile,
                            slope,
                            acceleration,
                            volatility,
                            entropy,
                            concentration,
                            lastDistance,
                            neighbor,
                            parity,
                            big,
                            estimateSwitchRisk(frequency, entropy),
                            estimateFamilyStrength(frequency, entropy, drift)
                        };
                double[] windowRow = new double[WINDOWS.length];
                for (int w = 0; w < WINDOWS.length; w++) windowRow[w] = frequency[w][n];
                String featureHash =
                        rowHash(
                                context.targetIssue,
                                position,
                                number,
                                model,
                                relation.lift[position - 1][n],
                                drift);
                rows[index++] =
                        new V50Models.NumberFeatureRow(
                                position,
                                number,
                                windowRow,
                                model,
                                Math.min(60, gap) / 60.0d,
                                gapEvidence.percentile,
                                gapEvidence.hazard,
                                gapEvidence.releaseFailure,
                                slope,
                                acceleration,
                                volatility,
                                entropy,
                                concentration,
                                attribute,
                                relation.lift[position - 1][n],
                                drift,
                                featureHash);
                snapshotHash.append('|').append(featureHash);
            }
        }
        return new V50Models.NumberFeatureSnapshot(
                context, rows, V50ChecksumEngine.sha256(snapshotHash.toString()));
    }

    private static ArrayList<Models.Draw> clean(List<Models.Draw> input) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (input != null) {
            int start = Math.max(0, input.size() - 2880);
            for (int i = start; i < input.size(); i++) {
                Models.Draw draw = input.get(i);
                if (draw != null && draw.isValid()) out.add(draw);
            }
        }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        return safe(a == null ? "" : a.issue)
                                .compareTo(safe(b == null ? "" : b.issue));
                    }
                });
        return out;
    }

    private static double[][] frequencies(List<Models.Draw> history, int position) {
        double[][] out = new double[WINDOWS.length][10];
        for (int w = 0; w < WINDOWS.length; w++) {
            int observed = Math.min(WINDOWS[w], history.size());
            int[] count = new int[10];
            int start = history.size() - observed;
            for (int i = start; i < history.size(); i++) {
                int number = history.get(i).getNumberAtPosition(position);
                if (number >= 1 && number <= 10) count[number - 1]++;
            }
            for (int n = 0; n < 10; n++) {
                out[w][n] = FeatureContractRegistry.smoothFrequency(count[n], observed);
            }
            FeatureContractRegistry.normalize10(out[w]);
        }
        return out;
    }

    private static int currentGap(List<Models.Draw> history, int position, int number) {
        int gap = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getNumberAtPosition(position) == number) return gap;
            gap++;
        }
        return gap;
    }

    private static GapEvidence gapEvidence(
            List<Models.Draw> history, int position, int number, int currentGap) {
        ArrayList<Integer> gaps = new ArrayList<Integer>();
        int last = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getNumberAtPosition(position) != number) continue;
            if (last >= 0) gaps.add(Integer.valueOf(i - last - 1));
            last = i;
        }
        if (gaps.size() < 3) {
            return new GapEvidence(Math.min(1.0d, currentGap / 20.0d), 0.10d, 0.50d);
        }
        int le = 0;
        int events = 0;
        int survivors = 0;
        int longSignals = 0;
        int failedLong = 0;
        int threshold = 10;
        for (int i = 0; i < gaps.size(); i++) {
            int gap = gaps.get(i).intValue();
            if (gap <= currentGap) le++;
            if (gap >= currentGap) survivors++;
            if (gap == currentGap) events++;
            if (gap >= threshold) {
                longSignals++;
                if (gap >= threshold + 6) failedLong++;
            }
        }
        double percentile = ((double) le) / gaps.size();
        double hazard = (events + 0.5d) / (survivors + 2.0d);
        double releaseFailure = (failedLong + 1.0d) / (longSignals + 2.0d);
        return new GapEvidence(clamp01(percentile), clamp01(hazard), clamp01(releaseFailure));
    }

    private static double blockVolatility(
            List<Models.Draw> history, int position, int number, int window, int block) {
        int observed = Math.min(window, history.size());
        int start = history.size() - observed;
        ArrayList<Double> rates = new ArrayList<Double>();
        for (int s = start; s < history.size(); s += block) {
            int end = Math.min(history.size(), s + block);
            int hit = 0;
            for (int i = s; i < end; i++) {
                if (history.get(i).getNumberAtPosition(position) == number) hit++;
            }
            rates.add(Double.valueOf(FeatureContractRegistry.smoothFrequency(hit, end - s)));
        }
        double mean = 0.0d;
        for (int i = 0; i < rates.size(); i++) mean += rates.get(i).doubleValue();
        mean /= Math.max(1, rates.size());
        double variance = 0.0d;
        for (int i = 0; i < rates.size(); i++) {
            double d = rates.get(i).doubleValue() - mean;
            variance += d * d;
        }
        return clamp01(Math.sqrt(variance / Math.max(1, rates.size())) * 4.0d);
    }

    private static double estimateSwitchRisk(double[][] frequency, double entropy) {
        double shortLong = 0.5d * l1(frequency[0], frequency[3]);
        double mediumLong = 0.5d * l1(frequency[2], frequency[4]);
        return clamp01(
                0.18d + 1.35d * shortLong + 0.75d * mediumLong + 0.18d * Math.abs(entropy - 0.90d));
    }

    private static double estimateFamilyStrength(
            double[][] frequency, double entropy, double drift) {
        double maxSlope = 0.0d;
        for (int n = 0; n < 10; n++) {
            maxSlope = Math.max(maxSlope, Math.abs(frequency[1][n] - frequency[3][n]));
        }
        return clamp01(0.18d + maxSlope * 4.0d + drift * 1.6d + Math.abs(entropy - 0.90d) * 0.45d);
    }

    private static double top3Sum(double[] probability) {
        int[] top = V49ExpertSupport.top3(probability);
        return probability[top[0]] + probability[top[1]] + probability[top[2]];
    }

    private static double js(double[] a, double[] b) {
        return V49ExpertSupport.js(a, b);
    }

    private static double l1(double[] a, double[] b) {
        double value = 0.0d;
        for (int i = 0; i < Math.min(a.length, b.length); i++) value += Math.abs(a[i] - b[i]);
        return value;
    }

    private static String rowHash(
            String issue, int position, int number, double[] model, double relation, double drift) {
        StringBuilder value =
                new StringBuilder(MODULE_VERSION)
                        .append('|')
                        .append(safe(issue))
                        .append('|')
                        .append(position)
                        .append('|')
                        .append(number);
        for (int i = 0; i < model.length; i++) {
            value.append('|').append(Double.doubleToLongBits(model[i]));
        }
        value.append('|')
                .append(Double.doubleToLongBits(relation))
                .append('|')
                .append(Double.doubleToLongBits(drift));
        return V50ChecksumEngine.sha256(value.toString());
    }

    private static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class GapEvidence {
        final double percentile;
        final double hazard;
        final double releaseFailure;

        GapEvidence(double percentile, double hazard, double releaseFailure) {
            this.percentile = percentile;
            this.hazard = hazard;
            this.releaseFailure = releaseFailure;
        }
    }
}
