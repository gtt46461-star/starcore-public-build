package com.ethk3.novaonepro;

/** Only transport object allowed between the V50 gate and persistence adapter. */
public final class V50DecisionEnvelope {
    public final V50CompiledActionContract compiledContract;
    public final V50Models.AuthorizedDecision authorizedDecision;
    public final LedgerReconciliationEngine.Report reconciliation;
    public final String checksum;

    public V50DecisionEnvelope(
            V50CompiledActionContract compiledContract,
            V50Models.AuthorizedDecision authorizedDecision,
            LedgerReconciliationEngine.Report reconciliation) {
        this.compiledContract = compiledContract;
        this.authorizedDecision = authorizedDecision;
        this.reconciliation = reconciliation;
        this.checksum =
                V50ChecksumEngine.sha256(
                        (compiledContract == null ? "" : compiledContract.checksum)
                                + '|'
                                + (authorizedDecision == null ? "" : authorizedDecision.decisionId)
                                + '|'
                                + (authorizedDecision == null
                                        ? ""
                                        : authorizedDecision.authorizationSignature)
                                + '|'
                                + (reconciliation == null ? "" : reconciliation.detail));
    }
}
