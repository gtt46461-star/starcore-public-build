package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Closed causal conditional-transition specification, with an explicit uniform Dirichlet prior. */
public final class RelationSpecSU1 {
    public static final String ESTIMATOR = "SU1_SPARSE_CONDITIONAL_V1|ISSUE_CLUSTERS|DIRICHLET_1_EACH|NO_NEFF_CLAIM";
    public enum Slice { NUMBER, ODD, BIG }
    public static final class Condition {
        public final int positionOffset, lag;
        public final Slice slice;
        public Condition(int offset, int lag, Slice slice) {
            if (offset < 0 || offset > 9 || lag < 1 || lag > 5000 || slice == null)
                throw new IllegalArgumentException("SU1_RELATION_CONDITION");
            positionOffset = offset; this.lag = lag; this.slice = slice;
        }
        int value(int[][] d, int t, int p) {
            int n = d[t - lag][(p + positionOffset) % 10];
            return slice == Slice.ODD ? n % 2 : slice == Slice.BIG ? (n >= 6 ? 1 : 0) : n;
        }
        String canonical() { return positionOffset + ":" + lag + ":" + slice; }
    }
    public final int sourceOffset, lag, window, requiredLag;
    public final List<Condition> conditions;
    public final String hash;
    public final RelationTypeSU1 output = new RelationTypeSU1(RelationTypeSU1.Unit.PROBABILITY,
            RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE, "NEXT_POSITION_NUMBER",
            RelationTypeSU1.Axis.POSITION, RelationTypeSU1.Axis.NUMBER);

    public RelationSpecSU1(int offset, int lag, int window, Condition... conditions) {
        if (offset < 0 || offset > 9 || lag < 1 || lag > 5000 || window < 1 || window > 5000 || conditions.length > 4)
            throw new IllegalArgumentException("SU1_RELATION_SPEC");
        sourceOffset = offset; this.lag = lag; this.window = window;
        List<Condition> list = new ArrayList<>(Arrays.asList(conditions.clone()));
        for (Condition c : list) Objects.requireNonNull(c);
        // Conjunction ordering has no semantic effect. Duplicate slices are rejected.
        list.sort(Comparator.comparing(Condition::canonical));
        int required = lag;
        for (int i = 0; i < list.size(); i++) {
            required = Math.max(required, list.get(i).lag);
            if (i > 0 && list.get(i).canonical().equals(list.get(i - 1).canonical()))
                throw new IllegalArgumentException("SU1_DUPLICATE_CONDITION");
        }
        requiredLag = required;
        this.conditions = Collections.unmodifiableList(list);
        hash = HashingSU1.fields(ESTIMATOR, canonical());
    }

    public static RelationSpecSU1 xt(int offset) { return new RelationSpecSU1(offset, 1, 1000); }
    public String canonical() {
        StringBuilder b = new StringBuilder("LAG_TRANSITION|").append(sourceOffset).append('|').append(lag)
                .append('|').append(window).append('|').append(output.canonical());
        for (Condition c : conditions) b.append('|').append(c.canonical());
        return b.toString();
    }
    long key(int[][] d, int t, int p) {
        long k = p * 10L + d[t - lag][(p + sourceOffset) % 10] - 1;
        for (Condition c : conditions) k = k * 11 + c.value(d, t, p);
        return k;
    }
}
