#!/usr/bin/env python3
"""Apply the one-time D06 selected-action calibration repair deterministically.

The transformer is intentionally strict: every original block must match once, otherwise it
fails instead of partially rewriting production source.
"""
from pathlib import Path

PATH = Path("app/src/main/java/com/ethk3/novaonepro/Batch34TrainingCoreR929.java")


def replace_once(text: str, old: str, new: str, code: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{code}|EXPECTED_1|ACTUAL_{count}")
    return text.replace(old, new, 1)


def main() -> None:
    text = PATH.read_text(encoding="utf-8")

    text = replace_once(
        text,
        '''        public double predict(double rawScore, String runtimeSelectorSha256) {
            if (!selectorSha256.equalsIgnoreCase(safe(runtimeSelectorSha256))) {
                throw new IllegalStateException("D06_SELECTOR_HASH_MISMATCH_RECALIBRATION_REQUIRED");
            }
            if (!Double.isFinite(rawScore)) throw new IllegalArgumentException("D06_RAW_SCORE_NONFINITE");
            for (int i = 0; i < upperRawScore.length; i++) {
                if (rawScore <= upperRawScore[i]) return probability[i];
            }
            return probability[probability.length - 1];
        }
''',
        '''        public double predict(double rawScore, String runtimeSelectorSha256) {
            if (!selectorSha256.equalsIgnoreCase(safe(runtimeSelectorSha256))) {
                throw new IllegalStateException("D06_SELECTOR_HASH_MISMATCH_RECALIBRATION_REQUIRED");
            }
            if (!Double.isFinite(rawScore)) throw new IllegalArgumentException("D06_RAW_SCORE_NONFINITE");
            if (rawScore <= upperRawScore[0]) return probability[0];
            for (int i = 1; i < upperRawScore.length; i++) {
                if (rawScore <= upperRawScore[i]) {
                    double leftRaw = upperRawScore[i - 1];
                    double rightRaw = upperRawScore[i];
                    if (!(rightRaw > leftRaw)) return probability[i];
                    double fraction = (rawScore - leftRaw) / (rightRaw - leftRaw);
                    double calibrated = probability[i - 1]
                            + fraction * (probability[i] - probability[i - 1]);
                    return Math.max(0.0d, Math.min(1.0d, calibrated));
                }
            }
            return probability[probability.length - 1];
        }
''',
        "D06_PREDICT_BLOCK_NOT_FOUND",
    )

    text = replace_once(
        text,
        '''        double[] upper = new double[order.length];
        double[] sum = new double[order.length];
        int[] weight = new int[order.length];
        int blocks = 0;
        for (int sorted = 0; sorted < order.length; sorted++) {
            int source = order[sorted].intValue();
            upper[blocks] = rawSelectedScores[source];
            sum[blocks] = selectedHitLabels[source];
            weight[blocks] = 1;
            blocks++;
            while (blocks >= 2
                    && sum[blocks - 2] / weight[blocks - 2] > sum[blocks - 1] / weight[blocks - 1]) {
                upper[blocks - 2] = upper[blocks - 1];
                sum[blocks - 2] += sum[blocks - 1];
                weight[blocks - 2] += weight[blocks - 1];
                blocks--;
            }
        }
''',
        '''        // Equal raw scores are one statistical knot. Keeping them as separate knots would make
        // prediction depend on input row order and could create a zero-width interpolation span.
        double[] groupedUpper = new double[order.length];
        double[] groupedSum = new double[order.length];
        int[] groupedWeight = new int[order.length];
        int groups = 0;
        for (int sorted = 0; sorted < order.length; sorted++) {
            int source = order[sorted].intValue();
            double raw = rawSelectedScores[source];
            if (groups > 0 && Double.compare(raw, groupedUpper[groups - 1]) == 0) {
                groupedSum[groups - 1] += selectedHitLabels[source];
                groupedWeight[groups - 1]++;
            } else {
                groupedUpper[groups] = raw;
                groupedSum[groups] = selectedHitLabels[source];
                groupedWeight[groups] = 1;
                groups++;
            }
        }
        double[] upper = new double[groups];
        double[] sum = new double[groups];
        int[] weight = new int[groups];
        int blocks = 0;
        for (int group = 0; group < groups; group++) {
            upper[blocks] = groupedUpper[group];
            sum[blocks] = groupedSum[group];
            weight[blocks] = groupedWeight[group];
            blocks++;
            while (blocks >= 2
                    && sum[blocks - 2] / weight[blocks - 2] > sum[blocks - 1] / weight[blocks - 1]) {
                upper[blocks - 2] = upper[blocks - 1];
                sum[blocks - 2] += sum[blocks - 1];
                weight[blocks - 2] += weight[blocks - 1];
                blocks--;
            }
        }
''',
        "D06_FIT_BLOCK_NOT_FOUND",
    )

    text = replace_once(
        text,
        '''            if (!Double.isFinite(model.upperRawScore[i]) || model.upperRawScore[i] < lastRaw) {
                failures.add("D06_RAW_BREAKS_NOT_SORTED");
            }
''',
        '''            if (!Double.isFinite(model.upperRawScore[i]) || model.upperRawScore[i] <= lastRaw) {
                failures.add("D06_RAW_BREAKS_NOT_STRICTLY_INCREASING");
            }
''',
        "D06_SORT_VALIDATION_NOT_FOUND",
    )

    text = replace_once(
        text,
        '''        if (!Batch34EvidenceContractR929.isSha256(model.modelSha256)) {
            failures.add("D06_CALIBRATION_HASH_INVALID");
        }
''',
        '''        if (!Batch34EvidenceContractR929.isSha256(model.modelSha256)) {
            failures.add("D06_CALIBRATION_HASH_INVALID");
        } else {
            String expectedModelSha256 = Batch34EvidenceContractR929.sha256(
                    "D06_SELECTED_ISOTONIC|" + model.canonicalWithoutHash());
            if (!expectedModelSha256.equalsIgnoreCase(model.modelSha256)) {
                failures.add("D06_CALIBRATION_HASH_MISMATCH");
            }
        }
''',
        "D06_HASH_VALIDATION_NOT_FOUND",
    )

    PATH.write_text(text, encoding="utf-8")
    print(f"R929_D06_REPAIR_APPLIED|path={PATH}|chars={len(text)}|lines={text.count(chr(10)) + 1}")


if __name__ == "__main__":
    main()
