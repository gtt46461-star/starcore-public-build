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


        /** R33.5 exact prepared-payload hydration; invoked only by the closed codec.
         * It restores frozen fields without consulting mutable current model state. */
        V51RuntimeResult(PreparedCanonicalCodecR335.Decoded decodedR335) {
            this.success = ((Boolean) decodedR335.value(0)).booleanValue();
            this.context = (V51Models.PredictionContext) decodedR335.value(1);
            this.features = (V51Models.PermutationFeatureSnapshot) decodedR335.value(2);
            this.states = (V51Models.StateTrajectorySnapshot[]) decodedR335.value(3);
            this.numberModel = (V51PermutationNumberModelEngine.Result) decodedR335.value(4);
            this.consensus = (V51EffectiveConsensusEngine.Consensus[]) decodedR335.value(5);
            this.funnel = (K3FunnelOrchestrator.Audit) decodedR335.value(6);
            this.actions = (V51Models.PositionAction[]) decodedR335.value(7);
            this.selected = (V51Models.SelectedActionEnvelope) decodedR335.value(8);
            this.authorized = (V51Models.AuthorizedDecision) decodedR335.value(9);
            this.contract = (V51CompiledActionContract) decodedR335.value(10);
            this.opportunityAuditR922 = (ControlledCandidateOpportunityAuditR922.Opportunity) decodedR335.value(11);
            this.contributionAuditR922 = (ResearchMainlineContributionAuditR922.Contribution) decodedR335.value(12);
            this.status = (String) decodedR335.value(13);
            this.checksum = (String) decodedR335.value(14);
            this.frozenConsumerRecipe = (String) decodedR335.value(15);
            this.elapsedMs = ((Long) decodedR335.value(16)).longValue();
        }
}
