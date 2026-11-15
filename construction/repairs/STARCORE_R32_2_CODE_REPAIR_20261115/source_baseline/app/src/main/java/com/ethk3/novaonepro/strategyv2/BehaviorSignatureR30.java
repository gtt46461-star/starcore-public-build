package com.ethk3.novaonepro.strategyv2;

/**
 * R30-B04-05 行为指纹（M22 语义去重唯一口径）。纯Java，HOST可测。
 *
 * 两个策略语法不同但在同一组“探针期”上做出完全相同的决定（出手与否、位置、K3集合），视为同一行为，只保留先出生者。
 * 探针期：由 ReplayDatasetR27 的固定 dataset hash 决定的 256 个历史期（只在 H_TRAIN 段内取，不碰确认段），
 * 选取规则固定为 ordinal ≡ 0 (mod step)，step = max(1, trainLength/256)。探针集合的 hash 与指纹一起存储，
 * 探针集合变化（数据集换代）后指纹不可跨代比较。
 */
public final class BehaviorSignatureR30 {
    public static final int PROBES = 256;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r30_behavior_dup(dataset_hash TEXT NOT NULL,probe_set_hash TEXT NOT NULL,signature TEXT NOT NULL,kept_policy TEXT NOT NULL,dropped_policy TEXT NOT NULL,stage TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(dataset_hash,probe_set_hash,signature,kept_policy,dropped_policy,stage))";
    private BehaviorSignatureR30() {}

    public static final class Probes {
        public final String datasetHash, hash; public final int[] rows;
        Probes(String dataset, String hash, int[] rows) { datasetHash = dataset; this.hash = hash; this.rows = rows; }
        public boolean ready() { return rows.length == PROBES; }
    }
    public interface Rejected { void duplicate(String signature, String keptPolicy, String droppedPolicy); }
    public static final class Duplicate {
        public final String datasetHash, probeSetHash, signature, keptPolicy, droppedPolicy, stage;
        public Duplicate(Probes probes, String signature, String kept, String dropped, String stage) {
            datasetHash = probes.datasetHash; probeSetHash = probes.hash; this.signature = signature;
            keptPolicy = kept; droppedPolicy = dropped; this.stage = stage;
        }
    }

    /** Fixed, ascending ordinal-grid probes, capped at 256 and entirely inside H_TRAIN. */
    public static Probes probes(ReplayDatasetR27 data, SplitR27 split) {
        int step = Math.max(1, split.trainSize() / PROBES);
        int[] rows = new int[PROBES]; int count = 0;
        StringBuilder identity = new StringBuilder(data.datasetHash).append('|').append(split.trainFrom).append(':').append(split.trainTo).append('|');
        for (int t = split.trainFrom; t < split.trainTo && count < PROBES; t++) {
            if (Math.floorMod(data.ordinals[t], (long)step) != 0) continue;
            rows[count++] = t; identity.append(data.ordinals[t]).append(',');
        }
        return new Probes(data.datasetHash, StatsR27.sha256(identity.toString()), java.util.Arrays.copyOf(rows, count));
    }

    /** Uses already-computed prequential outputs; no extra fit, replay, or H_SELECT/H_CONFIRM label access. */
    public static String fromTrace(Probes probes, ReplayEvaluatorR27.Trace trace) {
        if (!probes.ready()) return null;
        boolean[] act = new boolean[PROBES]; int[] positions = new int[PROBES], masks = new int[PROBES];
        for (int i = 0; i < PROBES; i++) {
            int t = probes.rows[i];
            if (t < trace.from || t >= trace.to) throw new IllegalArgumentException("R30_PROBE_OUTSIDE_TRACE");
            act[i] = !trace.skipped[t] && trace.act[t];
            if (act[i]) { positions[i] = trace.position; masks[i] = mask(trace.k3[t*3]+1, trace.k3[t*3+1]+1, trace.k3[t*3+2]+1); }
        }
        return signature(probes.hash, act, positions, masks);
    }

    /** 每个探针期的决定：act=false 时 position/k3Mask 不参与指纹（不出手就是不出手）。 */
    public static String signature(String probeSetHash, boolean[] act, int[] position, int[] k3Mask) {
        if (probeSetHash == null || probeSetHash.isEmpty()) throw new IllegalArgumentException("R30_PROBE_SET_HASH");
        int n = act.length;
        if (position.length != n || k3Mask.length != n) throw new IllegalArgumentException("R30_PROBE_LENGTH");
        StringBuilder b = new StringBuilder(16 + n * 6).append("R30_BEHAVIOR_V1|").append(probeSetHash).append('|');
        int acts = 0;
        for (int i = 0; i < n; i++) {
            if (!act[i]) { b.append('-'); continue; }
            if (position[i] < 1 || position[i] > 10 || Integer.bitCount(k3Mask[i]) != 3 || (k3Mask[i] & ~0x7FE) != 0)
                throw new IllegalArgumentException("R30_PROBE_DECISION:" + i);
            acts++;
            b.append(Character.forDigit(position[i] - 1, 10)).append(Integer.toHexString(k3Mask[i])).append(',');
        }
        b.append("|acts=").append(acts);
        return StatsR27.sha256(b.toString());
    }

    /** K3 集合转位掩码：号码 n（1..10）对应 bit n。 */
    public static int mask(int a, int b, int c) {
        if (a == b || b == c || a == c) throw new IllegalArgumentException("R30_K3_DUPLICATE");
        for (int x : new int[] {a, b, c}) if (x < 1 || x > 10) throw new IllegalArgumentException("R30_K3_RANGE");
        return (1 << a) | (1 << b) | (1 << c);
    }
}
