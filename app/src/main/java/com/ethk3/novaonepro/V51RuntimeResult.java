package com.ethk3.novaonepro;

/** Complete immutable result of one V51 target issue. */
public final class V51RuntimeResult {
    public final boolean success;
    public final V51Models.PredictionContext context;
    public final V51Models.PermutationFeatureSnapshot features;
    public final V51Models.StateTrajectorySnapshot[] states;
    public final V51PermutationNumberModelEngine.Result numberModel;
    public final V51EffectiveConsensusEngine.Consensus[] consensus;
    public final K3FunnelOrchestrator.Audit funnel;
    public final V51Models.PositionAction[] actions;
    public final V51Models.SelectedActionEnvelope selected;
    public final V51Models.AuthorizedDecision authorized;
    public final V51CompiledActionContract contract;
    public final ControlledCandidateOpportunityAuditR922.Opportunity opportunityAuditR922;
    public final ResearchMainlineContributionAuditR922.Contribution contributionAuditR922;
    public final String status, checksum;
    public final String frozenConsumerRecipe;
    public final long elapsedMs;

    public V51RuntimeResult(
            boolean success,
            V51Models.PredictionContext context,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51PermutationNumberModelEngine.Result numberModel,
            V51EffectiveConsensusEngine.Consensus[] consensus,
            K3FunnelOrchestrator.Audit funnel,
            V51Models.PositionAction[] actions,
            V51Models.SelectedActionEnvelope selected,
            V51Models.AuthorizedDecision authorized,
            V51CompiledActionContract contract,
            String status,
            long elapsedMs,
            String checksum) {
        this(
                success,
                context,
                features,
                states,
                numberModel,
                consensus,
                funnel,
                actions,
                selected,
                authorized,
                contract,
                status,
                elapsedMs,
                checksum,
                null,
                null);
    }

    public V51RuntimeResult(
            boolean success,
            V51Models.PredictionContext context,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51PermutationNumberModelEngine.Result numberModel,
            V51EffectiveConsensusEngine.Consensus[] consensus,
            K3FunnelOrchestrator.Audit funnel,
            V51Models.PositionAction[] actions,
            V51Models.SelectedActionEnvelope selected,
            V51Models.AuthorizedDecision authorized,
            V51CompiledActionContract contract,
            String status,
            long elapsedMs,
            String checksum,
            ControlledCandidateOpportunityAuditR922.Opportunity opportunityAuditR922,
            ResearchMainlineContributionAuditR922.Contribution contributionAuditR922) {
        this(success,context,features,states,numberModel,consensus,funnel,actions,selected,authorized,contract,status,elapsedMs,checksum,opportunityAuditR922,contributionAuditR922,"");
    }

    public V51RuntimeResult(
            boolean success,
            V51Models.PredictionContext context,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51PermutationNumberModelEngine.Result numberModel,
            V51EffectiveConsensusEngine.Consensus[] consensus,
            K3FunnelOrchestrator.Audit funnel,
            V51Models.PositionAction[] actions,
            V51Models.SelectedActionEnvelope selected,
            V51Models.AuthorizedDecision authorized,
            V51CompiledActionContract contract,
            String status,
            long elapsedMs,
            String checksum,
            ControlledCandidateOpportunityAuditR922.Opportunity opportunityAuditR922,
            ResearchMainlineContributionAuditR922.Contribution contributionAuditR922,
            String frozenConsumerRecipe) {
        this.frozenConsumerRecipe=frozenConsumerRecipe==null?"":frozenConsumerRecipe;
        this.success = success;
        this.context = context;
        this.features = features;
        this.states = states;
        this.numberModel = numberModel;
        this.consensus = consensus;
        this.funnel = funnel;
        this.actions = actions;
        this.selected = selected;
        this.authorized = authorized;
        this.contract = contract;
        this.opportunityAuditR922 = opportunityAuditR922;
        this.contributionAuditR922 = contributionAuditR922;
        this.status = status == null ? "" : status;
        this.elapsedMs = Math.max(0L, elapsedMs);
        this.checksum = checksum == null ? "" : checksum;
    }

    /** Legacy source-compatible constructor; active R3.6 always supplies the exact funnel audit. */
    public V51RuntimeResult(
            boolean success,
            V51Models.PredictionContext context,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51PermutationNumberModelEngine.Result numberModel,
            V51EffectiveConsensusEngine.Consensus[] consensus,
            V51Models.PositionAction[] actions,
            V51Models.SelectedActionEnvelope selected,
            V51Models.AuthorizedDecision authorized,
            V51CompiledActionContract contract,
            String status,
            long elapsedMs,
            String checksum) {
        this(
                success,
                context,
                features,
                states,
                numberModel,
                consensus,
                null,
                actions,
                selected,
                authorized,
                contract,
                status,
                elapsedMs,
                checksum);
    }
    /** Durable snapshot owns replay bytes; published UI/runtime view does not retain a MiB recipe. */
    public V51RuntimeResult publishedViewCP12(){
        return new V51RuntimeResult(success,context,features,states,numberModel,consensus,funnel,actions,selected,authorized,contract,status,elapsedMs,checksum,opportunityAuditR922,contributionAuditR922,"");
    }

}
