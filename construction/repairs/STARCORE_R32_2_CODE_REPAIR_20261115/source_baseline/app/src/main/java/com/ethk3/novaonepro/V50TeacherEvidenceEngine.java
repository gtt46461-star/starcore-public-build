package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Hierarchical Teacher evidence with sample shrinkage. Status is derived from evidence and can
 * never be assigned by the runtime pipeline.
 */
public final class V50TeacherEvidenceEngine {
    public V50Models.TeacherEvidence[][] evaluate(
            DbStore store, V50Models.StateTrajectorySnapshot[] states) {
        V50Models.TeacherEvidence[][] out = new V50Models.TeacherEvidence[10][10];
        SQLiteDatabase db = store == null ? null : store.readable();
        for (int p = 1; p <= 10; p++) {
            V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, p);
            String domain = state == null ? "*" : state.dynamicDomain;
            String lifecycle =
                    state == null ? V50Enums.Lifecycle.UNKNOWN.name() : state.lifecycle.name();
            for (int n = 1; n <= 10; n++) {
                Stats stats = db == null ? new Stats() : read(db, domain, lifecycle, p, n);
                out[p - 1][n - 1] = build(p, n, stats);
            }
        }
        return out;
    }

    private static Stats read(
            SQLiteDatabase db, String domain, String lifecycle, int position, int number) {
        Cursor c = null;
        Stats[] levels = new Stats[] {null, null, null};
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_TEACHER,
                            new String[] {
                                "evidence_level",
                                "state_domain",
                                "lifecycle",
                                "sample",
                                "hits",
                                "recent_sample",
                                "recent_hits",
                                "oos_sample",
                                "real_sample",
                                "shadow_sample",
                                "block0_sample",
                                "block0_hits",
                                "block1_sample",
                                "block1_hits",
                                "block2_sample",
                                "block2_hits",
                                "block3_sample",
                                "block3_hits",
                                "mislead_count"
                            },
                            "position=? AND number=? AND (state_domain=? OR state_domain='*')"
                                    + " AND (lifecycle=? OR lifecycle='UNKNOWN')",
                            new String[] {
                                String.valueOf(position),
                                String.valueOf(number),
                                safe(domain),
                                safe(lifecycle)
                            },
                            null,
                            null,
                            null);
            while (c.moveToNext()) {
                String rowDomain = safe(c.getString(1));
                String rowLifecycle = safe(c.getString(2));
                boolean exactDomain = safe(domain).equals(rowDomain);
                boolean exactLifecycle = safe(lifecycle).equals(rowLifecycle);
                int index = exactDomain && exactLifecycle ? 2 : (exactDomain ? 1 : 0);
                Stats candidate = stats(c);
                if (levels[index] == null || candidate.sample > levels[index].sample) {
                    levels[index] = candidate;
                }
            }
        } catch (Throwable ignore) {
            return new Stats();
        } finally {
            if (c != null) c.close();
        }
        double parent = 0.1d;
        int parentSample = 0;
        Stats best = new Stats();
        for (int i = 0; i < levels.length; i++) {
            Stats level = levels[i];
            if (level == null) continue;
            double priorStrength =
                    i == 0 ? 10.0d : Math.max(12.0d, Math.min(40.0d, parentSample * 0.50d));
            level.shrunkPosterior =
                    (level.hits + priorStrength * parent) / (level.sample + priorStrength);
            level.posteriorStrength = priorStrength;
            parent = level.shrunkPosterior;
            parentSample = level.sample;
            best = level;
        }
        return best;
    }

    private static V50Models.TeacherEvidence build(int position, int number, Stats s) {
        double posterior =
                s.shrunkPosterior >= 0.0d
                        ? s.shrunkPosterior
                        : (s.hits + 1.0d) / (s.sample + 10.0d);
        double variance =
                posterior
                        * (1.0d - posterior)
                        / Math.max(1.0d, s.sample + s.posteriorStrength + 1.0d);
        double low = Math.max(0.0d, posterior - 1.96d * Math.sqrt(variance));
        int blocks = 0;
        double minRate = 1.0d;
        double maxRate = 0.0d;
        for (int i = 0; i < 4; i++) {
            if (s.blockSample[i] < 8) continue;
            blocks++;
            double rate = ((double) s.blockHits[i]) / s.blockSample[i];
            minRate = Math.min(minRate, rate);
            maxRate = Math.max(maxRate, rate);
        }
        double stability = blocks < 2 ? 0.0d : Math.max(0.0d, 1.0d - (maxRate - minRate) / 0.20d);
        double recentRate = (s.recentHits + 1.0d) / (s.recentSample + 10.0d);
        double recentValidity = Math.max(0.0d, 1.0d - Math.abs(recentRate - posterior) / 0.12d);
        double lift = posterior - 0.1d;
        double misleadRisk =
                s.sample == 0 ? 0.0d : Math.min(1.0d, ((double) s.mislead) / Math.max(1, s.sample));
        double transfer = Math.min(1.0d, (s.oosSample + s.realSample) / 80.0d) * stability;
        V50Enums.TeacherStatus status;
        if (s.sample == 0) status = V50Enums.TeacherStatus.NO_SAMPLE;
        else if (misleadRisk > 0.45d) status = V50Enums.TeacherStatus.HARD_FREEZE;
        else if (s.recentSample >= 20 && recentRate < 0.075d)
            status = V50Enums.TeacherStatus.SOFT_RISK;
        else if (s.sample >= 120 && blocks >= 3 && low > 0.11d && stability >= 0.65d) {
            status = V50Enums.TeacherStatus.STRONG;
        } else if (s.sample >= 50 && blocks >= 2 && low > 0.10d && stability >= 0.50d) {
            status = V50Enums.TeacherStatus.READY;
        } else status = V50Enums.TeacherStatus.WATCH;
        String checksum =
                V50ChecksumEngine.sha256(
                        position
                                + "|"
                                + number
                                + "|"
                                + s.sample
                                + "|"
                                + s.hits
                                + "|"
                                + s.recentSample
                                + "|"
                                + s.recentHits
                                + "|"
                                + blocks
                                + "|"
                                + status);
        return new V50Models.TeacherEvidence(
                position,
                number,
                posterior,
                low,
                s.sample,
                s.recentSample,
                s.oosSample,
                s.realSample,
                s.shadowSample,
                blocks,
                stability,
                recentValidity,
                lift,
                misleadRisk,
                transfer,
                status,
                s.level,
                checksum);
    }

    private static final class Stats {
        String level = "NO_SAMPLE";
        int sample;
        int hits;
        int recentSample;
        int recentHits;
        int oosSample;
        int realSample;
        int shadowSample;
        int[] blockSample = new int[4];
        int[] blockHits = new int[4];
        int mislead;
        double shrunkPosterior = -1.0d;
        double posteriorStrength = 10.0d;
    }

    private static Stats stats(Cursor c) {
        Stats out = new Stats();
        out.level = safe(c.getString(0));
        out.sample = c.getInt(3);
        out.hits = c.getInt(4);
        out.recentSample = c.getInt(5);
        out.recentHits = c.getInt(6);
        out.oosSample = c.getInt(7);
        out.realSample = c.getInt(8);
        out.shadowSample = c.getInt(9);
        out.blockSample = new int[] {c.getInt(10), c.getInt(12), c.getInt(14), c.getInt(16)};
        out.blockHits = new int[] {c.getInt(11), c.getInt(13), c.getInt(15), c.getInt(17)};
        out.mislead = c.getInt(18);
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
