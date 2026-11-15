package com.ethk3.novaonepro;

import java.util.List;

/** Structured hierarchical Teacher posterior; neutral when evidence is absent. */
public final class TeacherPosteriorNumberExpert implements V50NumberExpert {
    private final V50TeacherEvidenceEngine engine = new V50TeacherEvidenceEngine();

    public String id() {
        return "TEACHER_HIERARCHICAL_POSTERIOR";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        V50Models.TeacherEvidence[][] teacher = engine.evaluate(store, states);
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        String[] reason = new String[10];
        String[] counter = new String[10];
        for (int p = 0; p < 10; p++) {
            double trustSum = 0.0d;
            boolean ready = false;
            for (int n = 0; n < 10; n++) {
                V50Models.TeacherEvidence value = teacher[p][n];
                probability[p][n] = value == null ? 0.1d : value.posteriorP;
                if (value != null) {
                    trustSum +=
                            Math.min(1.0d, value.totalSample / 120.0d)
                                    * value.crossBlockStability
                                    * (1.0d - value.misleadRisk);
                    ready |=
                            value.status == V50Enums.TeacherStatus.READY
                                    || value.status == V50Enums.TeacherStatus.STRONG;
                }
            }
            FeatureContractRegistry.normalize10(probability[p]);
            trust[p] = ready ? V50ExpertSupport.clamp01(trustSum / 10.0d) : 0.0d;
            reason[p] = ready ? "TEACHER_EVIDENCE_READY" : "TEACHER_NOT_PROVEN";
            counter[p] = ready ? "TEACHER_RECENT_VALIDITY_CHECKED" : "NO_TRANSFERABLE_SAMPLE";
        }
        return V50ExpertSupport.evidence(
                id(), probability, trust, reason, counter, "V50_TEACHER_EVIDENCE_V1");
    }
}
