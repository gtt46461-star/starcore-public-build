package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B04 bounded multi-scale rolling-window cache.
 *
 * <p>The cache owns no probability, lane, BET or settlement decision. It only materializes
 * deterministic point-in-time statistics for completed draws. The cache is bounded by entry count
 * and never retains the caller history array.
 */
public final class RollingWindowCache {
    public static final String B04_VERSION = "STARCORE_TIANSHU_B04_ROLLING_CACHE_V1";
    public static final int[] CANONICAL_WINDOWS = new int[] {12, 24, 48, 96, 360, 720, 2880};
    public static final int MAX_CACHE_ENTRIES = 3;
    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_COUNT = 10;
    private static final RollingWindowCache INSTANCE = new RollingWindowCache();

    private final LinkedHashMap<String, SnapshotSet> cache;
    private long hitCount;
    private long missCount;
    private long evictionCount;

    private RollingWindowCache() {
        cache =
                new LinkedHashMap<String, SnapshotSet>(8, 0.75f, true) {
                    protected boolean removeEldestEntry(Map.Entry<String, SnapshotSet> eldest) {
                        boolean remove = size() > MAX_CACHE_ENTRIES;
                        if (remove) {
                            evictionCount++;
                        }
                        return remove;
                    }
                };
    }

    public static RollingWindowCache shared() {
        return INSTANCE;
    }

    public synchronized SnapshotSet getOrBuild(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] normalized = normalize(history);
        String key = keyOf(normalized);
        SnapshotSet found = cache.get(key);
        if (found != null) {
            hitCount++;
            return found;
        }
        missCount++;
        SnapshotSet built = buildInternal(normalized, key);
        cache.put(key, built);
        return built;
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized CacheStats stats() {
        return new CacheStats(cache.size(), hitCount, missCount, evictionCount);
    }

    public synchronized String diagnostics() {
        CacheStats x = stats();
        return B04_VERSION
                + "|entries="
                + x.entries
                + "|hit="
                + x.hits
                + "|miss="
                + x.misses
                + "|evict="
                + x.evictions;
    }

    private SnapshotSet buildInternal(V51Models.DrawFrame[] history, String key) {
        ArrayList<WindowSnapshot> list = new ArrayList<WindowSnapshot>();
        for (int i = 0; i < CANONICAL_WINDOWS.length; i++) {
            list.add(buildWindow(history, CANONICAL_WINDOWS[i]));
        }
        String firstIssue = history.length == 0 ? "" : history[0].issue;
        String lastIssue = history.length == 0 ? "" : history[history.length - 1].issue;
        StringBuilder canonical = new StringBuilder();
        canonical
                .append(B04_VERSION)
                .append('|')
                .append(history.length)
                .append('|')
                .append(firstIssue)
                .append('|')
                .append(lastIssue)
                .append('|')
                .append(key);
        for (int i = 0; i < list.size(); i++) {
            canonical.append('|').append(list.get(i).checksum);
        }
        return new SnapshotSet(
                Collections.unmodifiableList(list),
                history.length,
                firstIssue,
                lastIssue,
                V51ChecksumEngine.sha256(canonical.toString()),
                key);
    }

    private WindowSnapshot buildWindow(V51Models.DrawFrame[] history, int requestedWindow) {
        int total = history.length;
        int observed = Math.min(total, requestedWindow);
        int start = Math.max(0, total - observed);
        int[][] count = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] gap = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] completedGapCount = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] completedGapLessOrEqual = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] lastSeen = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] priorSeen = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] lastNumberPosition = new int[NUMBER_COUNT][1];
        double[] mobilitySum = new double[NUMBER_COUNT];
        int[] mobilityCount = new int[NUMBER_COUNT];
        int[] reversalCount = new int[NUMBER_COUNT];
        int[] reversalTransitions = new int[NUMBER_COUNT];
        int[] lastDirection = new int[NUMBER_COUNT];
        int repeatedPosition = 0;
        int repeatedPositionDenom = 0;
        int adjacencyPreserved = 0;
        int adjacencyDenom = 0;
        int regionExchange = 0;
        int regionExchangeDenom = 0;

        for (int p = 0; p < POSITION_COUNT; p++) {
            for (int n = 0; n < NUMBER_COUNT; n++) {
                lastSeen[p][n] = -1;
                priorSeen[p][n] = -1;
            }
        }
        for (int n = 0; n < NUMBER_COUNT; n++) {
            lastNumberPosition[n][0] = -1;
        }

        for (int t = start; t < total; t++) {
            V51Models.DrawFrame frame = history[t];
            int[] current = frame.permutation;
            int[] previous = t > start ? history[t - 1].permutation : null;
            int[] previousInverse = previous == null ? null : inverse(previous);
            int[] currentInverse = inverse(current);
            for (int p = 0; p < POSITION_COUNT; p++) {
                int numberIndex = current[p] - 1;
                count[p][numberIndex]++;
                priorSeen[p][numberIndex] = lastSeen[p][numberIndex];
                lastSeen[p][numberIndex] = t;
                if (previous != null) {
                    repeatedPositionDenom++;
                    if (previous[p] == current[p]) {
                        repeatedPosition++;
                    }
                }
            }
            if (previous != null) {
                for (int n = 1; n <= NUMBER_COUNT; n++) {
                    int oldPosition = previousInverse[n];
                    int newPosition = currentInverse[n];
                    int delta = newPosition - oldPosition;
                    mobilitySum[n - 1] += Math.abs(delta) / 9.0d;
                    mobilityCount[n - 1]++;
                    int direction = delta > 0 ? 1 : (delta < 0 ? -1 : 0);
                    if (direction != 0) {
                        if (lastDirection[n - 1] != 0 && lastDirection[n - 1] != direction) {
                            reversalCount[n - 1]++;
                        }
                        if (lastDirection[n - 1] != 0) {
                            reversalTransitions[n - 1]++;
                        }
                        lastDirection[n - 1] = direction;
                    }
                    regionExchangeDenom++;
                    if (region(oldPosition) != region(newPosition)) {
                        regionExchange++;
                    }
                }
                for (int p = 0; p < POSITION_COUNT - 1; p++) {
                    adjacencyDenom++;
                    int left = previous[p];
                    int right = previous[p + 1];
                    if (currentInverse[left] + 1 == currentInverse[right]) {
                        adjacencyPreserved++;
                    }
                }
            }
        }

        for (int p = 0; p < POSITION_COUNT; p++) {
            for (int n = 0; n < NUMBER_COUNT; n++) {
                int last = lastSeen[p][n];
                gap[p][n] = last < 0 ? observed : Math.max(0, total - 1 - last);
                int currentGap = gap[p][n];
                int previousOccurrence = -1;
                for (int t = start; t < total; t++) {
                    if (history[t].permutation[p] == n + 1) {
                        if (previousOccurrence >= 0) {
                            int completed = t - previousOccurrence - 1;
                            completedGapCount[p][n]++;
                            if (completed <= currentGap) {
                                completedGapLessOrEqual[p][n]++;
                            }
                        }
                        previousOccurrence = t;
                    }
                }
            }
        }

        double[][] frequency = new double[POSITION_COUNT][NUMBER_COUNT];
        double[][] hotCold = new double[POSITION_COUNT][NUMBER_COUNT];
        double[][] gapPercentile = new double[POSITION_COUNT][NUMBER_COUNT];
        double[] positionEntropy = new double[POSITION_COUNT];
        double[] positionConcentration = new double[POSITION_COUNT];
        double[] numberMobility = new double[NUMBER_COUNT];
        double[] numberReversal = new double[NUMBER_COUNT];

        for (int p = 0; p < POSITION_COUNT; p++) {
            double[] distribution = new double[NUMBER_COUNT];
            for (int n = 0; n < NUMBER_COUNT; n++) {
                frequency[p][n] = (count[p][n] + 0.5d) / (observed + 5.0d);
                distribution[n] = frequency[p][n];
                double expected = Math.max(0.1d, observed / 10.0d);
                double variance = Math.max(0.25d, expected * 0.9d);
                hotCold[p][n] = (count[p][n] - expected) / Math.sqrt(variance);
                int denom = completedGapCount[p][n];
                gapPercentile[p][n] =
                        denom <= 0
                                ? Math.min(1.0d, gap[p][n] / (double) Math.max(1, observed))
                                : completedGapLessOrEqual[p][n] / (double) denom;
            }
            normalize(distribution);
            positionEntropy[p] = V51Math.entropy(distribution);
            positionConcentration[p] = topKSum(distribution, 3);
        }
        for (int n = 0; n < NUMBER_COUNT; n++) {
            numberMobility[n] = mobilityCount[n] <= 0 ? 0.0d : mobilitySum[n] / mobilityCount[n];
            numberReversal[n] =
                    reversalTransitions[n] <= 0
                            ? 0.0d
                            : reversalCount[n] / (double) reversalTransitions[n];
        }

        double repeatRate = ratio(repeatedPosition, repeatedPositionDenom);
        double adjacencyRate = ratio(adjacencyPreserved, adjacencyDenom);
        double regionExchangeRate = ratio(regionExchange, regionExchangeDenom);
        double density = observed / (double) Math.max(1, requestedWindow);
        String firstIssue = observed == 0 ? "" : history[start].issue;
        String lastIssue = observed == 0 ? "" : history[total - 1].issue;
        String checksum =
                windowHash(
                        requestedWindow,
                        observed,
                        firstIssue,
                        lastIssue,
                        frequency,
                        gap,
                        hotCold,
                        gapPercentile,
                        positionEntropy,
                        positionConcentration,
                        numberMobility,
                        numberReversal,
                        repeatRate,
                        adjacencyRate,
                        regionExchangeRate,
                        density);

        return new WindowSnapshot(
                requestedWindow,
                observed,
                firstIssue,
                lastIssue,
                frequency,
                gap,
                hotCold,
                gapPercentile,
                positionEntropy,
                positionConcentration,
                numberMobility,
                numberReversal,
                repeatRate,
                adjacencyRate,
                regionExchangeRate,
                density,
                checksum);
    }

    private static V51Models.DrawFrame[] normalize(V51Models.DrawFrame[] source) {
        if (source == null || source.length == 0) {
            return new V51Models.DrawFrame[0];
        }
        int start = Math.max(0, source.length - CANONICAL_WINDOWS[CANONICAL_WINDOWS.length - 1]);
        ArrayList<V51Models.DrawFrame> list = new ArrayList<V51Models.DrawFrame>();
        for (int i = start; i < source.length; i++) {
            V51Models.DrawFrame frame = source[i];
            if (frame != null
                    && V51Math.validPermutation(frame.permutation)
                    && frame.issue.length() > 0) {
                list.add(frame);
            }
        }
        Collections.sort(
                list,
                new Comparator<V51Models.DrawFrame>() {
                    public int compare(V51Models.DrawFrame left, V51Models.DrawFrame right) {
                        return left.issue.compareTo(right.issue);
                    }
                });
        ArrayList<V51Models.DrawFrame> unique = new ArrayList<V51Models.DrawFrame>();
        String lastIssue = "";
        for (int i = 0; i < list.size(); i++) {
            V51Models.DrawFrame frame = list.get(i);
            if (frame.issue.equals(lastIssue)) {
                continue;
            }
            unique.add(frame);
            lastIssue = frame.issue;
        }
        return unique.toArray(new V51Models.DrawFrame[unique.size()]);
    }

    private static String keyOf(V51Models.DrawFrame[] history) {
        StringBuilder payload = new StringBuilder();
        payload.append(B04_VERSION).append('|').append(history.length);
        // Every retained row may contribute to windows up to 2880; hash all of them.
        int start = 0;
        payload.append('|').append(V51ArtifactBundleManifest.BUNDLE_HASH);
        for (int i = start; i < history.length; i++) {
            payload.append('|').append(history[i].issue).append(':');
            for (int p = 0; p < POSITION_COUNT; p++) {
                payload.append(history[i].permutation[p]).append(',');
            }
        }
        return V51ChecksumEngine.sha256(payload.toString());
    }

    private static int[] inverse(int[] permutation) {
        int[] inverse = new int[NUMBER_COUNT + 1];
        for (int p = 0; p < POSITION_COUNT; p++) {
            inverse[permutation[p]] = p;
        }
        return inverse;
    }

    private static int region(int position) {
        if (position < 3) {
            return 0;
        }
        if (position < 7) {
            return 1;
        }
        return 2;
    }

    private static void normalize(double[] values) {
        double total = 0.0d;
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(0.0d, V51Math.finite(values[i]));
            total += values[i];
        }
        if (total <= 0.0d) {
            double uniform = 1.0d / Math.max(1, values.length);
            for (int i = 0; i < values.length; i++) {
                values[i] = uniform;
            }
            return;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] /= total;
        }
    }

    private static double topKSum(double[] source, int k) {
        double[] copy = V51Math.copy(source);
        for (int i = 0; i < copy.length; i++) {
            for (int j = i + 1; j < copy.length; j++) {
                if (copy[j] > copy[i]) {
                    double value = copy[i];
                    copy[i] = copy[j];
                    copy[j] = value;
                }
            }
        }
        double total = 0.0d;
        for (int i = 0; i < Math.min(k, copy.length); i++) {
            total += copy[i];
        }
        return V51Math.clamp(total, 0.0d, 1.0d);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator <= 0 ? 0.0d : numerator / (double) denominator;
    }

    private static String windowHash(
            int requestedWindow,
            int observed,
            String firstIssue,
            String lastIssue,
            double[][] frequency,
            int[][] gap,
            double[][] hotCold,
            double[][] gapPercentile,
            double[] entropy,
            double[] concentration,
            double[] mobility,
            double[] reversal,
            double repeatRate,
            double adjacencyRate,
            double regionExchangeRate,
            double density) {
        StringBuilder payload = new StringBuilder();
        payload.append(B04_VERSION)
                .append('|')
                .append(requestedWindow)
                .append('|')
                .append(observed)
                .append('|')
                .append(firstIssue)
                .append('|')
                .append(lastIssue);
        append(payload, frequency);
        append(payload, gap);
        append(payload, hotCold);
        append(payload, gapPercentile);
        append(payload, entropy);
        append(payload, concentration);
        append(payload, mobility);
        append(payload, reversal);
        payload.append('|')
                .append(bits(repeatRate))
                .append('|')
                .append(bits(adjacencyRate))
                .append('|')
                .append(bits(regionExchangeRate))
                .append('|')
                .append(bits(density));
        return V51ChecksumEngine.sha256(payload.toString());
    }

    private static void append(StringBuilder out, double[][] values) {
        if (values == null) {
            out.append("|null");
            return;
        }
        for (int i = 0; i < values.length; i++) {
            append(out, values[i]);
        }
    }

    private static void append(StringBuilder out, int[][] values) {
        if (values == null) {
            out.append("|null");
            return;
        }
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                out.append('|').append(values[i][j]);
            }
        }
    }

    private static void append(StringBuilder out, double[] values) {
        if (values == null) {
            out.append("|null");
            return;
        }
        for (int i = 0; i < values.length; i++) {
            out.append('|').append(bits(values[i]));
        }
    }

    private static String bits(double value) {
        return Long.toHexString(Double.doubleToLongBits(V51Math.finite(value)));
    }

    private static double[][] copy(double[][] source) {
        return V51Math.copy(source);
    }

    private static int[][] copy(int[][] source) {
        if (source == null) {
            return new int[0][0];
        }
        int[][] out = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            out[i] = V51Math.copy(source[i]);
        }
        return out;
    }

    public static List<SchemaField> schema() {
        ArrayList<SchemaField> fields = new ArrayList<SchemaField>();
        fields.add(
                new SchemaField(
                        "w12_frequency",
                        12,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_gap",
                        12,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_gap_percentile",
                        12,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_hot_cold_z",
                        12,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_position_entropy",
                        12,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_position_concentration",
                        12,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_repeat_rate",
                        12,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_adjacency_rate",
                        12,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_region_exchange",
                        12,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_number_mobility",
                        12,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_number_reversal",
                        12,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w12_density",
                        12,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_frequency",
                        24,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_gap",
                        24,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_gap_percentile",
                        24,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_hot_cold_z",
                        24,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_position_entropy",
                        24,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_position_concentration",
                        24,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_repeat_rate",
                        24,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_adjacency_rate",
                        24,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_region_exchange",
                        24,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_number_mobility",
                        24,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_number_reversal",
                        24,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w24_density",
                        24,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_frequency",
                        48,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_gap",
                        48,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_gap_percentile",
                        48,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_hot_cold_z",
                        48,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_position_entropy",
                        48,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_position_concentration",
                        48,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_repeat_rate",
                        48,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_adjacency_rate",
                        48,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_region_exchange",
                        48,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_number_mobility",
                        48,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_number_reversal",
                        48,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w48_density",
                        48,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_frequency",
                        96,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_gap",
                        96,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_gap_percentile",
                        96,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_hot_cold_z",
                        96,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_position_entropy",
                        96,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_position_concentration",
                        96,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_repeat_rate",
                        96,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_adjacency_rate",
                        96,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_region_exchange",
                        96,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_number_mobility",
                        96,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_number_reversal",
                        96,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w96_density",
                        96,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_frequency",
                        360,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_gap",
                        360,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_gap_percentile",
                        360,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_hot_cold_z",
                        360,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_position_entropy",
                        360,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_position_concentration",
                        360,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_repeat_rate",
                        360,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_adjacency_rate",
                        360,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_region_exchange",
                        360,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_number_mobility",
                        360,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_number_reversal",
                        360,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w360_density",
                        360,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_frequency",
                        720,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_gap",
                        720,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_gap_percentile",
                        720,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_hot_cold_z",
                        720,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_position_entropy",
                        720,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_position_concentration",
                        720,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_repeat_rate",
                        720,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_adjacency_rate",
                        720,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_region_exchange",
                        720,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_number_mobility",
                        720,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_number_reversal",
                        720,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w720_density",
                        720,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_frequency",
                        2880,
                        "Smoothed position-number frequency",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_gap",
                        2880,
                        "Current omission gap",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_gap_percentile",
                        2880,
                        "Empirical gap percentile",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_hot_cold_z",
                        2880,
                        "Standardized hot/cold score",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_position_entropy",
                        2880,
                        "Position distribution entropy",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_position_concentration",
                        2880,
                        "Position top-three concentration",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_repeat_rate",
                        2880,
                        "Same-position repeat rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_adjacency_rate",
                        2880,
                        "Directed adjacency preservation",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_region_exchange",
                        2880,
                        "Third-region exchange rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_number_mobility",
                        2880,
                        "Mean number position mobility",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_number_reversal",
                        2880,
                        "Number trajectory reversal rate",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        fields.add(
                new SchemaField(
                        "w2880_density",
                        2880,
                        "Window occupancy density",
                        "PIT_COMPLETED_DRAWS",
                        "DETERMINISTIC_BOUNDED_CACHE"));
        return Collections.unmodifiableList(fields);
    }

    public static final class SchemaField {
        public final String id;
        public final int window;
        public final String description;
        public final String source;
        public final String owner;

        SchemaField(String id, int window, String description, String source, String owner) {
            this.id = id == null ? "" : id;
            this.window = Math.max(0, window);
            this.description = description == null ? "" : description;
            this.source = source == null ? "" : source;
            this.owner = owner == null ? "" : owner;
        }

        public String canonical() {
            return id + ':' + window + ':' + description + ':' + source + ':' + owner;
        }
    }

    public static final class CacheStats {
        public final int entries;
        public final long hits;
        public final long misses;
        public final long evictions;

        CacheStats(int entries, long hits, long misses, long evictions) {
            this.entries = Math.max(0, entries);
            this.hits = Math.max(0L, hits);
            this.misses = Math.max(0L, misses);
            this.evictions = Math.max(0L, evictions);
        }
    }

    public static final class SnapshotSet {
        private final List<WindowSnapshot> snapshots;
        public final int historyRows;
        public final String firstIssue;
        public final String lastIssue;
        public final String checksum;
        public final String cacheKey;

        SnapshotSet(
                List<WindowSnapshot> snapshots,
                int historyRows,
                String firstIssue,
                String lastIssue,
                String checksum,
                String cacheKey) {
            this.snapshots =
                    snapshots == null ? Collections.<WindowSnapshot>emptyList() : snapshots;
            this.historyRows = Math.max(0, historyRows);
            this.firstIssue = firstIssue == null ? "" : firstIssue;
            this.lastIssue = lastIssue == null ? "" : lastIssue;
            this.checksum = checksum == null ? "" : checksum;
            this.cacheKey = cacheKey == null ? "" : cacheKey;
        }

        public List<WindowSnapshot> all() {
            return snapshots;
        }

        public WindowSnapshot exact(int window) {
            for (int i = 0; i < snapshots.size(); i++) {
                WindowSnapshot item = snapshots.get(i);
                if (item.requestedWindow == window) {
                    return item;
                }
            }
            return null;
        }

        public WindowSnapshot nearest(int window) {
            WindowSnapshot best = null;
            int distance = Integer.MAX_VALUE;
            for (int i = 0; i < snapshots.size(); i++) {
                WindowSnapshot item = snapshots.get(i);
                int d = Math.abs(item.requestedWindow - window);
                if (best == null || d < distance) {
                    best = item;
                    distance = d;
                }
            }
            return best;
        }

        public double frequency(int window, int positionIndex, int numberIndex) {
            WindowSnapshot x = nearest(window);
            return x == null ? 0.1d : x.frequency(positionIndex, numberIndex);
        }

        public int gap(int window, int positionIndex, int numberIndex) {
            WindowSnapshot x = nearest(window);
            return x == null ? Math.max(0, window) : x.gap(positionIndex, numberIndex);
        }

        public double hotCold(int window, int positionIndex, int numberIndex) {
            WindowSnapshot x = nearest(window);
            return x == null ? 0.0d : x.hotCold(positionIndex, numberIndex);
        }

        public double gapPercentile(int window, int positionIndex, int numberIndex) {
            WindowSnapshot x = nearest(window);
            return x == null ? 0.5d : x.gapPercentile(positionIndex, numberIndex);
        }

        public String replayHash() {
            StringBuilder payload = new StringBuilder();
            payload.append(B04_VERSION)
                    .append('|')
                    .append(historyRows)
                    .append('|')
                    .append(firstIssue)
                    .append('|')
                    .append(lastIssue)
                    .append('|')
                    .append(cacheKey);
            for (int i = 0; i < snapshots.size(); i++) {
                payload.append('|').append(snapshots.get(i).checksum);
            }
            return V51ChecksumEngine.sha256(payload.toString());
        }

        public Validation validate() {
            ArrayList<String> errors = new ArrayList<String>();
            if (snapshots.size() != CANONICAL_WINDOWS.length) {
                errors.add("WINDOW_COUNT");
            }
            for (int i = 0; i < snapshots.size(); i++) {
                Validation item = snapshots.get(i).validate();
                if (!item.ok) {
                    errors.add("W" + snapshots.get(i).requestedWindow + ':' + item.detail);
                }
            }
            if (!checksum.equals(replayHash())) {
                errors.add("REPLAY_HASH_MISMATCH");
            }
            return new Validation(errors.isEmpty(), join(errors));
        }
    }

    public static final class WindowSnapshot {
        public final int requestedWindow;
        public final int observedRows;
        public final String firstIssue;
        public final String lastIssue;
        public final double[][] frequencies;
        public final int[][] gaps;
        public final double[][] hotColdZ;
        public final double[][] gapPercentiles;
        public final double[] positionEntropy;
        public final double[] positionConcentration;
        public final double[] numberMobility;
        public final double[] numberReversal;
        public final double repeatRate;
        public final double adjacencyRate;
        public final double regionExchangeRate;
        public final double density;
        public final String checksum;

        WindowSnapshot(
                int requestedWindow,
                int observedRows,
                String firstIssue,
                String lastIssue,
                double[][] frequencies,
                int[][] gaps,
                double[][] hotColdZ,
                double[][] gapPercentiles,
                double[] positionEntropy,
                double[] positionConcentration,
                double[] numberMobility,
                double[] numberReversal,
                double repeatRate,
                double adjacencyRate,
                double regionExchangeRate,
                double density,
                String checksum) {
            this.requestedWindow = Math.max(1, requestedWindow);
            this.observedRows = Math.max(0, observedRows);
            this.firstIssue = firstIssue == null ? "" : firstIssue;
            this.lastIssue = lastIssue == null ? "" : lastIssue;
            this.frequencies = copy(frequencies);
            this.gaps = copy(gaps);
            this.hotColdZ = copy(hotColdZ);
            this.gapPercentiles = copy(gapPercentiles);
            this.positionEntropy = V51Math.copy(positionEntropy);
            this.positionConcentration = V51Math.copy(positionConcentration);
            this.numberMobility = V51Math.copy(numberMobility);
            this.numberReversal = V51Math.copy(numberReversal);
            this.repeatRate = V51Math.clamp(repeatRate, 0.0d, 1.0d);
            this.adjacencyRate = V51Math.clamp(adjacencyRate, 0.0d, 1.0d);
            this.regionExchangeRate = V51Math.clamp(regionExchangeRate, 0.0d, 1.0d);
            this.density = V51Math.clamp(density, 0.0d, 1.0d);
            this.checksum = checksum == null ? "" : checksum;
        }

        public double frequency(int positionIndex, int numberIndex) {
            return validIndex(positionIndex, numberIndex)
                    ? frequencies[positionIndex][numberIndex]
                    : 0.1d;
        }

        public int gap(int positionIndex, int numberIndex) {
            return validIndex(positionIndex, numberIndex)
                    ? gaps[positionIndex][numberIndex]
                    : observedRows;
        }

        public double hotCold(int positionIndex, int numberIndex) {
            return validIndex(positionIndex, numberIndex)
                    ? hotColdZ[positionIndex][numberIndex]
                    : 0.0d;
        }

        public double gapPercentile(int positionIndex, int numberIndex) {
            return validIndex(positionIndex, numberIndex)
                    ? gapPercentiles[positionIndex][numberIndex]
                    : 0.5d;
        }

        private boolean validIndex(int positionIndex, int numberIndex) {
            return positionIndex >= 0
                    && positionIndex < POSITION_COUNT
                    && numberIndex >= 0
                    && numberIndex < NUMBER_COUNT;
        }

        public Validation validate() {
            ArrayList<String> errors = new ArrayList<String>();
            if (frequencies.length != POSITION_COUNT) {
                errors.add("FREQUENCY_ROWS");
            }
            if (gaps.length != POSITION_COUNT) {
                errors.add("GAP_ROWS");
            }
            if (positionEntropy.length != POSITION_COUNT) {
                errors.add("ENTROPY_LENGTH");
            }
            if (numberMobility.length != NUMBER_COUNT) {
                errors.add("MOBILITY_LENGTH");
            }
            for (int p = 0; p < frequencies.length; p++) {
                if (frequencies[p].length != NUMBER_COUNT) {
                    errors.add("FREQUENCY_COLUMNS_P" + p);
                    continue;
                }
                double sum = 0.0d;
                for (int n = 0; n < frequencies[p].length; n++) {
                    double value = frequencies[p][n];
                    if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0d) {
                        errors.add("FREQUENCY_INVALID_P" + p + "N" + n);
                    }
                    sum += value;
                }
                if (sum <= 0.0d) {
                    errors.add("FREQUENCY_ZERO_P" + p);
                }
            }
            if (checksum.length() != 64) {
                errors.add("CHECKSUM");
            }
            return new Validation(errors.isEmpty(), join(errors));
        }

        public String summary() {
            return "W="
                    + requestedWindow
                    + " rows="
                    + observedRows
                    + " issue="
                    + firstIssue
                    + ".."
                    + lastIssue
                    + " repeat="
                    + repeatRate
                    + " adjacency="
                    + adjacencyRate
                    + " region="
                    + regionExchangeRate
                    + " density="
                    + density
                    + " hash="
                    + checksum;
        }
    }

    public static final class Validation {
        public final boolean ok;
        public final String detail;

        Validation(boolean ok, String detail) {
            this.ok = ok;
            this.detail = detail == null ? "" : detail;
        }
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(values.get(i));
            }
        }
        return out.toString();
    }
}
