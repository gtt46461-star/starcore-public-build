package com.ethk3.novaonepro;

/**
 * B09 sole owner of permutation-constrained NumberQ. It produces a research NumberQ even when
 * artifacts are untrained, but exposes provenance so B11/B12 must block REAL.
 */
public final class V51PermutationNumberModelEngine {
    public static final String V57_B09_OWNER = "SOLE_NUMBER_Q_OWNER";

    public enum ModelStatus {
        PROVEN_NUMBER_MODEL,
        UNTRAINED_NUMBER_ARTIFACT_RESEARCH_Q,
        SINKHORN_FALLBACK_INVALID
    }

    /** Diagnostic-only export of the frozen gate; excluded from Production strategy hashes. */
    public static final class GateTelemetryR811 {
        public final String[] expertIds, healthState;
        public final double[][] gateWeight;
        public final double[] effectiveExperts, sumBeforeFallback;
        public final boolean[] fallbackUsed;
        public final int[] fallbackExpertIndex;
        public final String gateStatus, diagnosticHash;

        private GateTelemetryR811(
                String[] expertIds,
                String[] healthState,
                double[][] gateWeight,
                double[] effectiveExperts,
                double[] sumBeforeFallback,
                boolean[] fallbackUsed,
                int[] fallbackExpertIndex,
                String gateStatus) {
            this.expertIds = copy(expertIds);
            this.healthState = copy(healthState);
            this.gateWeight = V51Math.copy(gateWeight);
            this.effectiveExperts = V51Math.copy(effectiveExperts);
            this.sumBeforeFallback = V51Math.copy(sumBeforeFallback);
            this.fallbackUsed = copy(fallbackUsed);
            this.fallbackExpertIndex = copy(fallbackExpertIndex);
            this.gateStatus = gateStatus == null ? "" : gateStatus;
            this.diagnosticHash = V51ChecksumEngine.sha256(canonical());
        }

        static GateTelemetryR811 capture(
                V51Models.ExpertEvidence[] experts, V51ExpertGateEngine.Result gate) {
            int n = experts == null ? 0 : experts.length;
            String[] ids = new String[n];
            for (int i = 0; i < n; i++)
                ids[i] = experts[i] == null ? "" : experts[i].expertId;
            String[] health =
                    gate == null || gate.health == null
                            ? new String[0]
                            : new String[gate.health.length];
            if (gate != null && gate.health != null)
                for (int i = 0; i < gate.health.length; i++)
                    health[i] =
                            gate.health[i] == null || gate.health[i].state == null
                                    ? ""
                                    : gate.health[i].state.name();
            return new GateTelemetryR811(
                    ids,
                    health,
                    gate == null ? new double[0][0] : gate.weight,
                    gate == null ? new double[0] : gate.effectiveExperts,
                    gate == null ? new double[0] : gate.sumBeforeFallback,
                    gate == null ? new boolean[0] : gate.fallbackUsed,
                    gate == null ? new int[0] : gate.fallbackExpertIndex,
                    gate == null || gate.status == null ? "" : gate.status.name());
        }

        public String canonical() {
            StringBuilder out = new StringBuilder(gateStatus);
            for (int i = 0; i < expertIds.length; i++)
                out.append("|E:")
                        .append(expertIds[i])
                        .append('@')
                        .append(i < healthState.length ? healthState[i] : "");
            for (int p = 0; p < gateWeight.length; p++) {
                out.append("|P:").append(p);
                if (p < effectiveExperts.length) out.append("@N=").append(effectiveExperts[p]);
                if (p < fallbackUsed.length && fallbackUsed[p])
                    out.append("@F=")
                            .append(p < fallbackExpertIndex.length ? fallbackExpertIndex[p] : -1)
                            .append("@S=")
                            .append(p < sumBeforeFallback.length ? sumBeforeFallback[p] : 0.0d);
                for (int e = 0; gateWeight[p] != null && e < gateWeight[p].length; e++)
                    out.append(',').append(gateWeight[p][e]);
            }
            return out.toString();
        }

        private static String[] copy(String[] source) {
            if (source == null) return new String[0];
            String[] out = new String[source.length];
            for (int i = 0; i < source.length; i++)
                out[i] = source[i] == null ? "" : source[i];
            return out;
        }

        private static boolean[] copy(boolean[] source) {
            if (source == null) return new boolean[0];
            boolean[] out = new boolean[source.length];
            System.arraycopy(source, 0, out, 0, source.length);
            return out;
        }

        private static int[] copy(int[] source) {
            if (source == null) return new int[0];
            int[] out = new int[source.length];
            System.arraycopy(source, 0, out, 0, source.length);
            return out;
        }
    }

    public static final class Result {
        public final V51Models.ExpertEvidence[] experts;
        public final double[][] gateWeight, rawScore;
        public final V51Models.NumberProbabilityMatrix matrix;
        public final V51ExpertGateEngine.Result gate;
        public final V51SinkhornEngine.ProjectionResult projection;
        public final NumberSurfaceTensorEngine.TensorResult tensor;
        public final ModelStatus status;
        public final boolean artifactProven;
        /** Read-only R811 diagnostics; excluded from the NumberQ strategy checksum. */
        public final GateTelemetryR811 telemetry;
        public final String checksum;

        Result(
                V51Models.ExpertEvidence[] experts,
                double[][] gateWeight,
                double[][] rawScore,
                V51Models.NumberProbabilityMatrix matrix,
                V51ExpertGateEngine.Result gate,
                V51SinkhornEngine.ProjectionResult projection,
                NumberSurfaceTensorEngine.TensorResult tensor,
                ModelStatus status) {
            this.experts = experts;
            this.gateWeight = V51Math.copy(gateWeight);
            this.rawScore = V51Math.copy(rawScore);
            this.matrix = matrix;
            this.gate = gate;
            this.projection = projection;
            this.tensor = tensor;
            this.status = status;
            this.artifactProven =
                    ModelBundleRuntimeR932.number().PROVEN && gate != null && gate.artifactProven;
            this.telemetry = GateTelemetryR811.capture(experts, gate);
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public boolean productionModelProven() {
            return artifactProven
                    && status == ModelStatus.PROVEN_NUMBER_MODEL
                    && projection != null
                    && projection.productionValid();
        }

        public String canonical() {
            return String.valueOf(status)
                    + '|'
                    + artifactProven
                    + '|'
                    + (matrix == null ? "" : matrix.checksum)
                    + '|'
                    + (gate == null ? "" : gate.checksum)
                    + '|'
                    + (tensor == null ? "" : tensor.checksum);
        }
    }

    private final V51NumberExpert[] experts =
            new V51NumberExpert[] {
                new V51HierarchicalPriorNumberExpert(),
                new V51MultiScaleTrendNumberExpert(),
                new V51ContextTreeNumberExpert(),
                new V51StateDurationNumberExpert(),
                new V51MotifNumberExpert(),
                new V51RelationGraphNumberExpert(),
                new V51TeacherPosteriorNumberExpert(),
                new V51OfflineResidualNumberExpert(),
                new V51OnlineResidualNumberExpert()
            };
    private final V51ExpertGateEngine gateEngine = new V51ExpertGateEngine();
    private final V51RawScoreModelEngine fusion = new V51RawScoreModelEngine();
    private final V51SinkhornEngine sinkhorn = new V51SinkhornEngine();
    private final V51ExpertRegretEngine regret = new V51ExpertRegretEngine();
    private final NumberSurfaceTensorEngine tensorEngine = new NumberSurfaceTensorEngine();

    public Result predict(
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.DrawFrame[] history) {
        return predict(features, states, history, null);
    }

    public Result predict(
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.DrawFrame[] history,
            AnalysisSingleFlightR36.CancellationToken token) {
        if (features == null || !features.valid())
            throw new IllegalArgumentException("B09_FEATURES_INVALID");
        if (states == null || states.length != 10)
            throw new IllegalArgumentException("B09_STATES_INVALID");
        V51Models.ExpertEvidence[] evidence = new V51Models.ExpertEvidence[experts.length];
        for (int i = 0; i < experts.length; i++) {
            if (token != null) token.checkpoint("NUMBER_EXPERT_" + i + "_BEGIN");
            evidence[i] = experts[i].evaluate(features, states, history);
            validateEvidence(evidence[i]);
            if (token != null) token.checkpoint("NUMBER_EXPERT_" + i + "_DONE");
        }
        if (token != null) token.checkpoint("NUMBER_GATE_BEGIN");
        V51ExpertGateEngine.Result gate = gateEngine.evaluate(evidence, states, regret);
        if (token != null) token.checkpoint("NUMBER_GATE_DONE");
        double[] slow=null;ModelBundleParametersR932.Number model=ModelBundleRuntimeR932.number();
        if(model.TRAINED_MODEL!=null&&model.TRAINED_MODEL.kind.equals("D2")) {
            if (token != null) token.checkpoint("NUMBER_SLOW_STATE_BEGIN");
            double[] upstream=UpstreamModelMathR932.features(UpstreamModelMathR932.stateValues(states),UpstreamModelMathR932.expertValues(evidence));
            slow=SlowStateR932.capture(ModelBundleRuntimeR932.currentStore(),ModelBundleRuntimeR932.current().artifactHashes.get("NUMBER"),features.context.targetIssue,upstream,model.SLOW_INITIAL,model.SLOW_CHECKPOINT_ISSUE);
            if (token != null) token.checkpoint("NUMBER_SLOW_STATE_DONE");
        }
        if (token != null) token.checkpoint("NUMBER_FUSION_BEGIN");
        Result result=predictFrozen(features,states,evidence,gate,slow);
        if (token != null) token.checkpoint("NUMBER_FUSION_DONE");
        return result;
    }

    /** Pure Number inference over already-frozen evidence; does not evaluate experts or write D2. */
    Result predictFrozen(V51Models.PermutationFeatureSnapshot features,V51Models.StateTrajectorySnapshot[] states,
            V51Models.ExpertEvidence[] evidence,V51ExpertGateEngine.Result gate,double[] slow) {
        if(features==null||!features.valid()||states==null||states.length!=10||evidence==null||evidence.length!=9||gate==null)throw new IllegalArgumentException("R932_FROZEN_NUMBER_INPUT");
        for(V51Models.ExpertEvidence item:evidence)validateEvidence(item);
        double[][] raw=fusion.fuse(evidence,gate.weight);
        ModelBundleParametersR932.Number model=ModelBundleRuntimeR932.number();
        if(model.TRAINED_MODEL!=null) {
            double[][][] expertValues=UpstreamModelMathR932.expertValues(evidence);
            double[] upstream=UpstreamModelMathR932.features(UpstreamModelMathR932.stateValues(states),expertValues);
            raw=model.TRAINED_MODEL.logits(upstream,slow,expertValues,model.TRAINED_MODEL.kind.equals("GATE9")?gate.weight[0]:null);
        }else if(model.PROVEN)addArtifactResidual(raw,features);
        V51SinkhornEngine.ProjectionResult projection =
                sinkhorn.projectDetailed(raw, ModelBundleRuntimeR932.number().TEMPERATURE, 240, 1e-10);
        NumberSurfaceTensorEngine.TensorResult tensor =
                tensorEngine.build(evidence, gate.weight, projection.matrix, states);
        ModelStatus status =
                !projection.productionValid()
                        ? ModelStatus.SINKHORN_FALLBACK_INVALID
                        : (ModelBundleRuntimeR932.number().PROVEN && gate.artifactProven
                                ? ModelStatus.PROVEN_NUMBER_MODEL
                                : ModelStatus.UNTRAINED_NUMBER_ARTIFACT_RESEARCH_Q);
        return new Result(
                evidence, gate.weight, raw, projection.matrix, gate, projection, tensor, status);
    }

    private static void validateEvidence(V51Models.ExpertEvidence e) {
        if (e == null || e.rawScore == null || e.rawScore.length != 10)
            throw new IllegalStateException("B09_EXPERT_EVIDENCE_INVALID");
        for (int p = 0; p < 10; p++)
            if (e.rawScore[p] == null || e.rawScore[p].length != 10)
                throw new IllegalStateException("B09_EXPERT_SCORE_SHAPE");
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)
            if(!Double.isFinite(e.rawScore[p][n]))
                throw new IllegalStateException("B09_EXPERT_SCORE_NONFINITE");
    }

    private static void addArtifactResidual(
            double[][] raw, V51Models.PermutationFeatureSnapshot features) {
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                double z = ModelBundleRuntimeR932.number().POSITION_NUMBER_BIAS[p][n];
                double[] x = features.numberFeatures[p][n];
                for (int i = 0;
                        i < x.length && i < ModelBundleRuntimeR932.number().FEATURE_WEIGHT.length;
                        i++) {
                    double scale = Math.max(1e-9, ModelBundleRuntimeR932.number().FEATURE_SCALE[i]);
                    z +=
                            ((x[i] - ModelBundleRuntimeR932.number().FEATURE_MEAN[i]) / scale)
                                    * ModelBundleRuntimeR932.number().FEATURE_WEIGHT[i];
                }
                raw[p][n] = V51Math.clamp(raw[p][n] + z, -30, 30);
            }
    }

    public static boolean canCreateBet() {
        return false;
    }

    public static boolean canWriteSelectedHitP() {
        return false;
    }

    public static boolean canWriteLane() {
        return false;
    }
}
