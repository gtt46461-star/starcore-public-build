package com.ethk3.novaonepro;

/** The only transfer object accepted by persistence and authorization. */
public final class V51DecisionEnvelope {
    public final V51CompiledActionContract contract;
    public final V51Models.AuthorizedDecision decision;
    public final String checksum;

    public V51DecisionEnvelope(
            V51CompiledActionContract contract, V51Models.AuthorizedDecision decision) {
        this.contract = contract;
        this.decision = decision;
        this.checksum =
                V51ChecksumEngine.sha256(contract.checksum + '|' + decision.authorizationHash);
    }
}
