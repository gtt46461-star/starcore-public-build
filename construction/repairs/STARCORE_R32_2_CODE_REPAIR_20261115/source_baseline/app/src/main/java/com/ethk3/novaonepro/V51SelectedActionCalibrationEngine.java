package com.ethk3.novaonepro;

/**
 * B11/R929 sole SelectedHitP owner. Unproven or selection-mismatched calibration returns NaN;
 * rank score is never clamped and published as probability.
 */
public final class V51SelectedActionCalibrationEngine {
    public static final String V57_B11_OWNER = "SOLE_SELECTED_HIT_P_OWNER";

    public enum Status {
        PROVEN_SELECTION_CONDITIONED_CALIBRATION,
        ARTIFACT_NOT_PROVEN,
        SELECTION_POLICY_MISMATCH,
        INPUT_SCHEMA_MISMATCH,
        SUPPORT_INSUFFICIENT,
        INPUT_INVALID
    }

    public static final class CalibrationResult {
        public final double selectedHitP;
        public final Status status;
        public final boolean proven;
        public final String artifactId, checksum, selectionPolicyHash, inputSchemaHash;
        public final int supportN, reliabilityBinMinN;
        public final double brier, ece;
        public final SelectionEvidenceContractR929.WilsonInterval wilson95;

        CalibrationResult(double p, Status s, boolean proven,String actualPolicy) {
            selectedHitP = p;
            status = s;
            this.proven = proven;
            artifactId = ModelBundleRuntimeR932.calibration().ARTIFACT_ID;
            selectionPolicyHash = actualPolicy;
            inputSchemaHash = V51ActionMetaFeatureEngine.featureManifestHash();
            supportN = Math.max(0, ModelBundleRuntimeR932.calibration().CALIBRATION_SUPPORT_N);
            reliabilityBinMinN = Math.max(0, ModelBundleRuntimeR932.calibration().RELIABILITY_BIN_MIN_N);
            brier = ModelBundleRuntimeR932.calibration().OOS_BRIER;
            ece = ModelBundleRuntimeR932.calibration().OOS_ECE;
            wilson95 = SelectionEvidenceContractR929.wilson95(
                    ModelBundleRuntimeR932.calibration().CALIBRATION_HITS, supportN);
            checksum = V51ChecksumEngine.sha256(
                    String.valueOf(s) + '|' + p + '|' + artifactId + '|'
                            + ModelBundleRuntimeR932.calibration().DATA_SHA256 + '|'
                            + selectionPolicyHash + '|' + inputSchemaHash + "|N=" + supportN
                            + "|BIN=" + reliabilityBinMinN);
        }

        public SelectionEvidenceContractR929.Probability probabilityEvidence() {
            if (!proven || Double.isNaN(selectedHitP) || Double.isInfinite(selectedHitP)) {
                return new SelectionEvidenceContractR929.Probability(
                        Double.NaN, supportN, reliabilityBinMinN, brier, ece, artifactId,
                        selectionPolicyHash, false);
            }
            return new SelectionEvidenceContractR929.Probability(
                    selectedHitP, supportN, reliabilityBinMinN, brier, ece, artifactId,
                    selectionPolicyHash, true);
        }
    }

    public double calibrate(
            V51Models.PositionAction action,
            V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,
            V51Models.DomainCertificate domain,
            V51UncertaintyEngine.Breakdown uncertainty,
            double falsifier,
            double predictability) {
        return calibrateDetailed(action, consensus, state, domain, uncertainty, falsifier, predictability)
                .selectedHitP;
    }

    public CalibrationResult calibrateDetailed(
            V51Models.PositionAction action,
            V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,
            V51Models.DomainCertificate domain,
            V51UncertaintyEngine.Breakdown uncertainty,
            double falsifier,
            double predictability) {
        return calibrateDetailed(action,consensus,state,domain,uncertainty,falsifier,predictability,SelectionEvidenceContractR929.installedPolicy().hash);
    }
    public CalibrationResult calibrateDetailed(V51Models.PositionAction action,V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,V51Models.DomainCertificate domain,V51UncertaintyEngine.Breakdown uncertainty,
            double falsifier,double predictability,String actualPolicy) {
        if (action == null || consensus == null || state == null || uncertainty == null
                || actualPolicy == null || !actualPolicy.matches("[0-9a-f]{64}"))
            return new CalibrationResult(Double.NaN, Status.INPUT_INVALID, false,actualPolicy);
        if (!ModelBundleRuntimeR932.calibration().PROVEN
                || "UNTRAINED".equals(ModelBundleRuntimeR932.calibration().DATA_SHA256)
                || ModelBundleRuntimeR932.calibration().OOS_ECE > 0.04d)
            return new CalibrationResult(Double.NaN, Status.ARTIFACT_NOT_PROVEN, false,actualPolicy);
        String expectedPolicy = actualPolicy;
        if (!expectedPolicy.equals(ModelBundleRuntimeR932.calibration().SELECTION_POLICY_SHA256))
            return new CalibrationResult(Double.NaN, Status.SELECTION_POLICY_MISMATCH, false,actualPolicy);
        if (!V51ActionMetaFeatureEngine.featureManifestHash()
                .equals(ModelBundleRuntimeR932.calibration().INPUT_SCHEMA_SHA256))
            return new CalibrationResult(Double.NaN, Status.INPUT_SCHEMA_MISMATCH, false,actualPolicy);
        if (ModelBundleRuntimeR932.calibration().CALIBRATION_SUPPORT_N <= 0
                || ModelBundleRuntimeR932.calibration().RELIABILITY_BIN_MIN_N <= 0)
            return new CalibrationResult(Double.NaN, Status.SUPPORT_INSUFFICIENT, false,actualPolicy);

        double[] x = new double[] {
                action.rawActionP,
                action.rankScore,
                action.rankMargin,
                action.thirdMargin,
                action.entropy,
                action.stability,
                consensus.effective,
                1.0d - consensus.effective,
                state.confidence,
                state.changePointRisk,
                domain == null ? 0 : domain.credibleLow,
                uncertainty.total,
                falsifier,
                predictability,
                (action.position - 1) / 9.0d,
                state.remainingLife / 20.0d
        };
        ModelBundleParametersR932.Calibration artifact = ModelBundleRuntimeR932.calibration();
        if (artifact.MEAN == null || artifact.SCALE == null || artifact.WEIGHT == null
                || artifact.MEAN.length != x.length || artifact.SCALE.length != x.length
                || artifact.WEIGHT.length != x.length || !Double.isFinite(artifact.BIAS)
                || !Double.isFinite(artifact.OOS_ECE) || artifact.OOS_ECE < 0 || artifact.OOS_ECE > .04
                || !Double.isFinite(artifact.OOS_BRIER) || artifact.OOS_BRIER < 0 || artifact.OOS_BRIER > .24
                || artifact.CALIBRATION_HITS < 0 || artifact.CALIBRATION_HITS > artifact.CALIBRATION_SUPPORT_N
                || artifact.RELIABILITY_BIN_MIN_N > artifact.CALIBRATION_SUPPORT_N)
            return new CalibrationResult(Double.NaN, Status.INPUT_INVALID, false, actualPolicy);
        for (int i = 0; i < x.length; i++)
            if (!Double.isFinite(artifact.MEAN[i]) || !Double.isFinite(artifact.WEIGHT[i])
                    || !Double.isFinite(artifact.SCALE[i]) || artifact.SCALE[i] <= 0)
                return new CalibrationResult(Double.NaN, Status.INPUT_INVALID, false, actualPolicy);
        double z = artifact.BIAS;
        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(x[i]) || Double.isInfinite(x[i]))
                return new CalibrationResult(Double.NaN, Status.INPUT_INVALID, false,actualPolicy);
            z += ((x[i] - ModelBundleRuntimeR932.calibration().MEAN[i])
                    / Math.max(1e-9, ModelBundleRuntimeR932.calibration().SCALE[i]))
                    * ModelBundleRuntimeR932.calibration().WEIGHT[i];
        }
        double probability = ModelBundleRuntimeR932.calibration().TRAINED_MODEL==null ? V51Math.sigmoid(z)
                : ModelBundleRuntimeR932.calibration().TRAINED_MODEL.predict(action.rankScore);
        if (Double.isNaN(probability) || Double.isInfinite(probability)
                || probability < 0 || probability > 1)
            return new CalibrationResult(Double.NaN, Status.INPUT_INVALID, false,actualPolicy);
        return new CalibrationResult(
                probability, Status.PROVEN_SELECTION_CONDITIONED_CALIBRATION, true,actualPolicy);
    }
}
