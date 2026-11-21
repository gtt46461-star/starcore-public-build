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

        /** R33.5 exact prepared-payload hydration; invoked only by the closed codec.
         * It restores frozen fields without consulting mutable current model state. */
        V51CompiledActionContract(PreparedCanonicalCodecR335.Decoded decodedR335) {
            this.selected = (V51Models.SelectedActionEnvelope) decodedR335.value(0);
            this.policy = (V51SelectiveRiskController.Result) decodedR335.value(1);
            this.stability = (V51RankStabilityEngine.Result) decodedR335.value(2);
            this.certifiedPrediction = (CertifiedPredictionR974.Proof) decodedR335.value(3);
            this.featureHash = (String) decodedR335.value(4);
            this.stateHash = (String) decodedR335.value(5);
            this.expertHash = (String) decodedR335.value(6);
            this.probabilityHash = (String) decodedR335.value(7);
            this.actionHash = (String) decodedR335.value(8);
            this.checksum = (String) decodedR335.value(9);
        }
}
