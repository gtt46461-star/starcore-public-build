package com.ethk3.novaonepro;

/** Explains one typed selected action without changing it. */
public final class V51OpportunityDiagnosisEngine {
    public String explain(V51RuntimeResult result) {
        if (result == null) return "V51_NO_RUNTIME_RESULT";
        if (!result.success) return "V51_RUNTIME_FAILURE|" + result.status;
        if (result.selected == null || result.authorized == null) return "V51_SELECTION_INCOMPLETE";
        V51Models.SelectedActionEnvelope s = result.selected;
        return "target="
                + s.context.targetIssue
                + "|P"
                + s.action.position
                + "|K3="
                + s.action.n1
                + ','
                + s.action.n2
                + ','
                + s.action.n3
                + "|selectedHitP="
                + s.selectedHitP
                + "|predictability="
                + (s.predictability == null ? 0.0d : s.predictability.probability)
                + "|lane="
                + s.lane.name()
                + "|action="
                + result.authorized.action.name()
                + "|reject="
                + result.authorized.rejectCode.name()
                + "|release="
                + V51BuildManifest.releaseState();
    }
}
