package com.ethk3.novaonepro.strategyv2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** R27-013 replay dataset: ordered legal permutations, gap-aware segments and a content hash.
 * Missing issues are never invented: a gap only starts a new segment (docs/03 3.1). Numbers are 1..10. */
public final class ReplayDatasetR27 {
    public final int[][] draws;
    public final String[] issues;
    public final long[] ordinals;
    /** Segment index for every row; rows t-1,t are causally consecutive iff segment[t-1]==segment[t]. */
    public final int[] segment;
    /** First row of every segment. */
    public final int[] segmentStart;
    public final String datasetHash;
    public final int rejectedRows;

    public static final class Row {
        public final String issue; public final long ordinal; public final int[] numbers;
        public Row(String issue, long ordinal, int[] numbers) { this.issue = issue; this.ordinal = ordinal; this.numbers = numbers; }
    }

    private ReplayDatasetR27(int[][] draws, String[] issues, long[] ordinals, int rejected) {
        this.draws = draws; this.issues = issues; this.ordinals = ordinals; this.rejectedRows = rejected;
        int t = draws.length; segment = new int[t];
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < t; i++) {
            CooperativeContinuationR26.poll();
            if (i == 0 || ordinals[i] != ordinals[i - 1] + 1) starts.add(i);
            segment[i] = starts.size() - 1;
        }
        segmentStart = new int[starts.size()];
        for (int i = 0; i < segmentStart.length; i++) { CooperativeContinuationR26.poll(); segmentStart[i] = starts.get(i); }
        datasetHash = hash(issues, draws, segment);
    }

    public int size() { return draws.length; }
    public int segments() { return segmentStart.length; }
    /** Number of gaps (missing-issue holes) — equals segments-1 by construction. */
    public int gaps() { return Math.max(0, segmentStart.length - 1); }
    /** True when rows t-lag..t are all in one causally consecutive segment. */
    public boolean consecutive(int t, int lag) { return t - lag >= 0 && segment[t - lag] == segment[t]; }
    public int number(int t, int position) { return draws[t][position - 1]; }

    public static boolean legal(int[] p) {
        if (p == null || p.length != 10) return false;
        int seen = 0;
        for (int v : p) { if (v < 1 || v > 10 || (seen & (1 << v)) != 0) return false; seen |= 1 << v; }
        return true;
    }

    /** Builds from arbitrary rows: invalid permutations and duplicate ordinals are rejected (counted), order by ordinal. */
    public static ReplayDatasetR27 of(List<Row> rows) {
        Row[] sorted = rows.toArray(new Row[0]);
        Arrays.sort(sorted, (a, b) -> Long.compare(a.ordinal, b.ordinal));
        List<Row> kept = new ArrayList<>(sorted.length); int rejected = 0;
        for (Row r : sorted) {
            if (!legal(r.numbers) || (!kept.isEmpty() && kept.get(kept.size() - 1).ordinal == r.ordinal)) { rejected++; continue; }
            kept.add(r);
        }
        int[][] d = new int[kept.size()][]; String[] is = new String[kept.size()]; long[] o = new long[kept.size()];
        for (int i = 0; i < d.length; i++) { d[i] = kept.get(i).numbers.clone(); is[i] = kept.get(i).issue; o[i] = kept.get(i).ordinal; }
        return new ReplayDatasetR27(d, is, o, rejected);
    }

    /** Exact pin decoder: no sorting/rejection or replacement of the captured replay inputs. */
    static ReplayDatasetR27 pinnedExact(List<Row> rows,int rejected) {
        if(rows==null||rejected<0)throw new IllegalArgumentException("R29_DATASET_PIN_SOURCE_METADATA");
        int[][] draws=new int[rows.size()][];String[] issues=new String[rows.size()];long[] ordinals=new long[rows.size()];
        for(int i=0;i<rows.size();i++){CooperativeContinuationR26.poll();Row row=rows.get(i);
            if(row==null||!legal(row.numbers)||(i>0&&row.ordinal<=ordinals[i-1]))throw new IllegalArgumentException("R29_DATASET_PIN_SOURCE_ROWS");
            draws[i]=row.numbers.clone();issues[i]=row.issue;ordinals[i]=row.ordinal;
        }
        return new ReplayDatasetR27(draws,issues,ordinals,rejected);
    }

    /** Incremental append of one newer draw (docs/03 3.2): old rows and their segments are unchanged. */
    public ReplayDatasetR27 append(Row r) {
        if (!legal(r.numbers)) throw new IllegalArgumentException("R27_REPLAY_ILLEGAL_PERMUTATION");
        if (size() > 0 && r.ordinal <= ordinals[size() - 1]) throw new IllegalArgumentException("R27_REPLAY_APPEND_NOT_NEWER");
        int n = size();
        int[][] d = Arrays.copyOf(draws, n + 1); d[n] = r.numbers.clone();
        String[] is = Arrays.copyOf(issues, n + 1); is[n] = r.issue;
        long[] o = Arrays.copyOf(ordinals, n + 1); o[n] = r.ordinal;
        return new ReplayDatasetR27(d, is, o, rejectedRows);
    }

    /** Keeps only the newest {@code limit} rows (the database retains ≤5000 draws). */
    public ReplayDatasetR27 tail(int limit) {
        if (size() <= limit) return this;
        int from = size() - limit;
        return new ReplayDatasetR27(Arrays.copyOfRange(draws, from, size()), Arrays.copyOfRange(issues, from, size()), Arrays.copyOfRange(ordinals, from, size()), rejectedRows);
    }

    /** Same rows with a different label sequence (used by tests and the shuffled-label control). */
    public ReplayDatasetR27 withDraws(int[][] replacement) {
        if (replacement.length != size()) throw new IllegalArgumentException("R27_REPLAY_SHAPE");
        for (int[] p : replacement) if (!legal(p)) throw new IllegalArgumentException("R27_REPLAY_ILLEGAL_PERMUTATION");
        return new ReplayDatasetR27(replacement, issues.clone(), ordinals.clone(), rejectedRows);
    }

    static String hash(String[] issues, int[][] draws, int[] segment) {
        StringBuilder b = new StringBuilder("R27_REPLAY_DATASET_V1");
        for (int i = 0; i < draws.length; i++) {
            CooperativeContinuationR26.poll();
            b.append('|').append(issues[i]).append(':');
            for (int v : draws[i]) b.append((char) ('0' + v - 1));
            b.append(':').append(segment[i]);
        }
        return StatsR27.sha256(b.toString());
    }

    /** Ordinal of a yyyyMMddNNNN issue (1440 issues per UTC day); only differences are used. */
    public static long ordinalOf(String issue) {
        if (issue == null || !issue.matches("\\d{12}")) throw new IllegalArgumentException("R27_ISSUE_FORMAT:" + issue);
        java.time.LocalDate day = java.time.LocalDate.of(Integer.parseInt(issue.substring(0, 4)), Integer.parseInt(issue.substring(4, 6)), Integer.parseInt(issue.substring(6, 8)));
        int n = Integer.parseInt(issue.substring(8));
        if (n < 1 || n > 1440) throw new IllegalArgumentException("R27_ISSUE_RANGE:" + issue);
        return day.toEpochDay() * 1440L + n;
    }

    /** Reads the repository CSV format (period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time). */
    public static List<Row> readCsv(Reader in) throws IOException {
        List<Row> rows = new ArrayList<>();
        BufferedReader r = new BufferedReader(in);
        String header = r.readLine();
        if (header == null || !header.startsWith("period,")) throw new IOException("R27_CSV_HEADER");
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty()) continue;
            int q1 = line.indexOf('"'), q2 = line.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) throw new IOException("R27_CSV_NUMBERS");
            String period = line.substring(0, line.indexOf(','));
            String[] tail = line.substring(q2 + 2).split(",");
            if (tail.length < 3 || !"1".equals(tail[2].trim())) continue;
            String[] ns = line.substring(q1 + 1, q2).split(",");
            int[] p = new int[ns.length];
            for (int i = 0; i < ns.length; i++) p[i] = Integer.parseInt(ns[i].trim());
            rows.add(new Row(period, ordinalOf(period), p));
        }
        return rows;
    }

    public static ReplayDatasetR27 readCsv(java.io.InputStream in) throws IOException {
        return of(readCsv(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)));
    }

    /** Synthetic dataset for tests/null controls: i.i.d. uniform permutations, optional gaps. */
    public static ReplayDatasetR27 synthetic(int n, long seed, int[] gapAfterRows) {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(seed);
        List<Row> rows = new ArrayList<>(n);
        long ordinal = 1_000_000L; int g = 0;
        for (int i = 0; i < n; i++) {
            if (gapAfterRows != null && g < gapAfterRows.length && i == gapAfterRows[g]) { ordinal += 3; g++; }
            rows.add(new Row(String.format(java.util.Locale.ROOT, "S%011d", ordinal), ordinal, randomPermutation(rng)));
            ordinal++;
        }
        return of(rows);
    }

    public static int[] randomPermutation(StatsR27.SplitMix rng) {
        int[] p = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 9; i > 0; i--) { int j = rng.nextInt(i + 1); int x = p[i]; p[i] = p[j]; p[j] = x; }
        return p;
    }

    /** Planted weak signal (docs/01 F, docs/12 H06): with probability q, position 1 takes the previous row's
     * position-2 number by swapping two places, so every row stays a legal permutation. */
    public ReplayDatasetR27 planted(double q, long seed) {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(seed);
        int[][] d = new int[size()][];
        for (int t = 0; t < size(); t++) {
            d[t] = draws[t].clone();
            if (t > 0 && consecutive(t, 1) && rng.chance(q)) {
                int want = d[t - 1][1];
                for (int j = 0; j < 10; j++) if (d[t][j] == want) { d[t][j] = d[t][0]; d[t][0] = want; break; }
            }
        }
        return withDraws(d);
    }
}
