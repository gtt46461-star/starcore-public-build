package com.ethk3.novaonepro;

/** Immutable evidence bundle presented to the sole DecisionGateEngine owner. */
public final class V50CompiledActionContract {
    public final V50Models.SelectedActionEnvelope envelope;
    public final V50SelectiveRiskController.Result policy;
    public final V50Models.DomainCertificate domainCertificate;
    public final V50Models.UncertaintyBreakdown uncertainty;
    public final V50CounterEvidenceEngine.Result counterEvidence;
    public final String featureHash;
    public final String stateHash;
    public final String expertHash;
    public final String probabilityHash;
    public final String actionHash;
    public final String checksum;

    public V50CompiledActionContract(
            V50Models.SelectedActionEnvelope envelope,
            V50SelectiveRiskController.Result policy,
            V50Models.DomainCertificate domainCertificate,
            V50Models.UncertaintyBreakdown uncertainty,
            V50CounterEvidenceEngine.Result counterEvidence,
            String featureHash,
            String stateHash,
            String expertHash,
            String probabilityHash,
            String actionHash) {
        this.envelope = envelope;
        this.policy = policy;
        this.domainCertificate = domainCertificate;
        this.uncertainty = uncertainty;
        this.counterEvidence = counterEvidence;
        this.featureHash = safe(featureHash);
        this.stateHash = safe(stateHash);
        this.expertHash = safe(expertHash);
        this.probabilityHash = safe(probabilityHash);
        this.actionHash = safe(actionHash);
        this.checksum =
                V50ChecksumEngine.sha256(
                        (envelope == null ? "" : envelope.contractHash)
                                + '|'
                                + (policy == null ? "" : policy.lane.name())
                                + '|'
                                + (domainCertificate == null ? "" : domainCertificate.checksum)
                                + '|'
                                + this.featureHash
                                + '|'
                                + this.stateHash
                                + '|'
                                + this.expertHash
                                + '|'
                                + this.probabilityHash
                                + '|'
                                + this.actionHash);
    }

    public V50PredictionContract.Result validate() {
        if (policy == null
                || domainCertificate == null
                || uncertainty == null
                || counterEvidence == null) {
            return new V50PredictionContract.Result(
                    false, V50Enums.RejectCode.DATA_INCOMPLETE, "V50_COMPILED_EVIDENCE_INCOMPLETE");
        }
        return V50PredictionContract.validate(envelope);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
