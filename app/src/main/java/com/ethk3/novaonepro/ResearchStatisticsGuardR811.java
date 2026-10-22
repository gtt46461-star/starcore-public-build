package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Statistical guard fixing the independent proof unit to target_issue. */
public final class ResearchStatisticsGuardR811 {
    private ResearchStatisticsGuardR811() {}

    public static Map<String, List<Row>> clusterByIssue(List<Row> rows) {
        LinkedHashMap<String, List<Row>> out = new LinkedHashMap<String, List<Row>>();
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i); if (row == null || !row.eligible || !TargetIssueResolver.isValid(row.targetIssue) || Double.isNaN(row.value) || Double.isInfinite(row.value)) continue;
                List<Row> group = out.get(row.targetIssue);
                if (group == null) { group = new ArrayList<Row>(); out.put(row.targetIssue, group); }
                group.add(row);
            }
        LinkedHashMap<String, List<Row>> frozen = new LinkedHashMap<String, List<Row>>();
        for (Map.Entry<String, List<Row>> entry : out.entrySet())
            frozen.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<Row>(entry.getValue())));
        return Collections.unmodifiableMap(frozen);
    }

    public static Bootstrap blockBootstrap(List<Row> rows, int replicates, long seed) {
        Map<String, List<Row>> groups = clusterByIssue(rows);
        ArrayList<String> issues = new ArrayList<String>(groups.keySet());
        if (issues.isEmpty()) return new Bootstrap(0, 0.0d, 0.0d, 0.0d);
        int r = Math.max(100, Math.min(10000, replicates));
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(seed);
        ArrayList<Double> estimates = new ArrayList<Double>(r);
        for (int k = 0; k < r; k++) {
            double sum = 0.0d; int n = 0;
            for (int i = 0; i < issues.size(); i++) {
                List<Row> group = groups.get(issues.get(rng.nextInt(issues.size())));
                double issueSum = 0.0d; for (int j = 0; j < group.size(); j++) issueSum += group.get(j).value;
                sum += issueSum / Math.max(1, group.size()); n++;
            }
            estimates.add(Double.valueOf(sum / Math.max(1, n)));
        }
        Collections.sort(estimates);
        return new Bootstrap(issues.size(), percentile(estimates, 0.50d), percentile(estimates, 0.025d), percentile(estimates, 0.975d));
    }

    public static int proofSampleN(List<Row> rows) {
        Set<String> issues = new LinkedHashSet<String>();
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                if (row != null
                        && row.eligible
                        && TargetIssueResolver.isValid(row.targetIssue))
                    issues.add(row.targetIssue);
            }
        return issues.size();
    }

    public static final class Row {
        public final String targetIssue; public final double value; public final boolean eligible;
        public Row(String issue, double value, boolean eligible) { targetIssue = issue == null ? "" : issue; this.value = value; this.eligible = eligible; }
    }
    public static final class Bootstrap {
        public final int issueN; public final double estimate, lower, upper;
        Bootstrap(int n, double e, double l, double u) { issueN = n; estimate = e; lower = l; upper = u; }
    }
    private static double percentile(List<Double> x, double q) { int i = Math.max(0, Math.min(x.size() - 1, (int) Math.ceil(q * x.size()) - 1)); return x.get(i).doubleValue(); }
}
