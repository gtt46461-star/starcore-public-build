package com.ethk3.novaonepro;

/** Immutable payload crossing from model runtime into DecisionGateEngine. */
public final class V51CompiledActionContract {
    public final V51Models.SelectedActionEnvelope selected;
    public final V51SelectiveRiskController.Result policy;
    public final V51RankStabilityEngine.Result stability;
    public final CertifiedPredictionR974.Proof certifiedPrediction;
    public final String featureHash, stateHash, expertHash, probabilityHash, actionHash, checksum;

    public V51CompiledActionContract(
            V51Models.SelectedActionEnvelope selected,
            V51SelectiveRiskController.Result policy,
            V51RankStabilityEngine.Result stability,
            String featureHash,
            String stateHash,
            String expertHash,
            String probabilityHash,
            String actionHash) {
        this(selected,policy,stability,featureHash,stateHash,expertHash,probabilityHash,actionHash,null);
    }
    public V51CompiledActionContract(V51Models.SelectedActionEnvelope selected,
            V51SelectiveRiskController.Result policy,V51RankStabilityEngine.Result stability,
            String featureHash,String stateHash,String expertHash,String probabilityHash,String actionHash,
            CertifiedPredictionR974.Proof proof) {
        this.certifiedPrediction=proof;
        this.selected = selected;
        this.policy = policy;
        this.stability = stability;
        this.featureHash = featureHash;
        this.stateHash = stateHash;
        this.expertHash = expertHash;
        this.probabilityHash = probabilityHash;
        this.actionHash = actionHash;
        this.checksum =
                V51ChecksumEngine.sha256(
                        selected.contractHash
                                + '|'
                                + policy.lane.name()
                                + '|'
                                + featureHash
                                + '|'
                                + stateHash
                                + '|'
                                + expertHash
                                + '|'
                                + probabilityHash
                                + '|'
                                + actionHash
                                + '|'
                                + V51ArtifactBundleManifest.BUNDLE_HASH
                                + (proof==null?"":"|"+proof.hash));
    }
}
