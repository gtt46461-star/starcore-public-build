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
    private BehaviorSignatureR30() {}

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
