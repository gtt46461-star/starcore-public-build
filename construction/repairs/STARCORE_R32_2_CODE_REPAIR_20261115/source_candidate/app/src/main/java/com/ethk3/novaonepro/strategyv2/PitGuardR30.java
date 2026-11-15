package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * R30-B03-01 赛前冻结/回放样本的时点正确性守卫（M18 唯一判定）。纯Java，HOST可测。
 *
 * 冻结记录（实时或历史回放）在写库前必须调用 {@link #check}；返回非空即拒绝写入，并把违规码写进
 * r30_pit_violation（见 docs/B03）。施工方不得以“只告警”方式放行。
 */
public final class PitGuardR30 {
    public static final long MIN_MARGIN_MS = 5000L;
    public static final double PROB_SUM_TOLERANCE = 1e-6;
    private PitGuardR30() {}

    public static final class Freeze {
        public long targetOrdinal;          // 预测的目标期序号
        public long trainedThroughOrdinal;  // 模型最后学到的期序号
        public long featureThroughOrdinal;  // 特征最后使用的期序号
        public int position;                // 1..10
        public int[] k3;                    // 恰好3个不同号码，1..10
        public double[] probabilities;      // 长度10或null（未校准时允许null，但不得伪造）
        public long modelAvailableAt, featureAvailableAt, preparedAt, frozenAt, deadlineAt; // 墙钟ms；历史回放用模拟时钟
        public boolean replay;              // 历史回放样本：deadlineAt 由期号推算
    }

    public static List<String> check(Freeze f) {
        // Replay checks every candidate at every input row. Valid rows need neither a list
        // nor a temporary seen-array; retain the complete ordered evidence for invalid rows.
        List<String> v = null;
        if (f == null) return violation(v, "NULL_FREEZE");
        if (f.trainedThroughOrdinal >= f.targetOrdinal) v = violation(v, "MODEL_SEES_TARGET:" + f.trainedThroughOrdinal + ">=" + f.targetOrdinal);
        if (f.featureThroughOrdinal >= f.targetOrdinal) v = violation(v, "FEATURE_SEES_TARGET:" + f.featureThroughOrdinal + ">=" + f.targetOrdinal);
        if (f.position < 1 || f.position > 10) v = violation(v, "POSITION_RANGE:" + f.position);
        if (f.k3 == null || f.k3.length != 3) v = violation(v, "K3_SIZE");
        else {
            int seen = 0;
            for (int n : f.k3) {
                if (n < 1 || n > 10) { v = violation(v, "K3_RANGE:" + n); continue; }
                int bit = 1 << n;
                if ((seen & bit) != 0) v = violation(v, "K3_DUPLICATE:" + n);
                seen |= bit;
            }
        }
        if (f.probabilities != null) {
            if (f.probabilities.length != 10) v = violation(v, "PROB_LENGTH:" + f.probabilities.length);
            else {
                double sum = 0;
                for (double p : f.probabilities) {
                    if (Double.isNaN(p) || p < 0 || p > 1) v = violation(v, "PROB_RANGE");
                    sum += p;
                }
                if (Math.abs(sum - 1.0) > PROB_SUM_TOLERANCE) v = violation(v, "PROB_SUM:" + sum);
            }
        }
        if (f.modelAvailableAt > f.frozenAt) v = violation(v, "MODEL_AFTER_FREEZE");
        if (f.featureAvailableAt > f.frozenAt) v = violation(v, "FEATURE_AFTER_FREEZE");
        if (f.preparedAt > f.frozenAt) v = violation(v, "PREPARED_AFTER_FREEZE");
        if (f.deadlineAt <= 0) v = violation(v, "DEADLINE_UNKNOWN");
        else if (f.frozenAt > f.deadlineAt - MIN_MARGIN_MS) v = violation(v, "FREEZE_MARGIN_LT_5000:" + (f.deadlineAt - f.frozenAt));
        return v == null ? Collections.emptyList() : v;
    }

    private static List<String> violation(List<String> violations, String code) {
        if (violations == null) violations = new ArrayList<>();
        violations.add(code);
        return violations;
    }
}
