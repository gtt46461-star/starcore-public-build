package com.ethk3.novaonepro;

/** Compatibility facade. V48 uses one settled Rank1 observation per issue. */
public final class VersionScopedCalibrationEngine {
    public static final String MODULE_VERSION = RankConditionalCalibrationEngine.MODULE_VERSION;

    public static final class Snapshot {
        final RankConditionalCalibrationEngine.Snapshot delegate;

        Snapshot(RankConditionalCalibrationEngine.Snapshot d) {
            delegate = d;
        }

        public String summary() {
            return delegate == null ? "EMPTY" : delegate.summary();
        }
    }

    private VersionScopedCalibrationEngine() {}

    public static Snapshot load(DbStore store) {
        return new Snapshot(RankConditionalCalibrationEngine.load(store));
    }

    public static void apply(Snapshot s, Models.ComboCandidate c) {
        RankConditionalCalibrationEngine.apply(s == null ? null : s.delegate, c);
    }
}
