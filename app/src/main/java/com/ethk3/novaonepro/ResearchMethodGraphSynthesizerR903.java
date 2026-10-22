package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates method-family-level Research plans, not coefficient variants.
 *
 * <p>Each blueprint changes the representation, learner and inference contract.  It also carries
 * a point-in-time training protocol, calibration method, risk veto and falsification rule so a
 * name such as “random forest” cannot be stored as an empty slogan.  Blueprints require offline
 * artifact construction and future OOS certification before they can become executable.
 */
public final class ResearchMethodGraphSynthesizerR903 {
    private ResearchMethodGraphSynthesizerR903() {}

    public static List<Blueprint> create(
            String failureLayer, String hypothesis, String datasetHash, int issueN) {
        long root =
                ResearchDeterministicRngR811.seed(
                        "R903_METHOD_GRAPH|"
                                + safe(failureLayer)
                                + '|'
                                + safe(hypothesis)
                                + '|'
                                + safe(datasetHash));
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(root);
        ArrayList<Blueprint> out = new ArrayList<Blueprint>();
        String protocol =
                "PIT_PROTOCOL[split=expanding_walk_forward,purge=1_issue,group=target_issue,"
                        + "action_universe=1200,baseline_k3=0.30,metrics=wilson95+brier+ece+coverage+latency,"
                        + "min_issue_n=" + Math.max(120, issueN) + ",post_draw_feature_forbidden=true]";

        out.add(blueprint("MARKOV_REGIME",
                "REPRESENTATION[transition_tensor=number_position+rank_bucket,order=1|2,regime=change_point]",
                "LEARNER[bayesian_markov=dirichlet_smoothed,backoff=order2_to_order1_to_global]",
                "CALIBRATION[beta_binomial_by_regime]",
                "RISK[posterior_entropy+transition_support_min]", protocol, rng));
        out.add(blueprint("ONLINE_FOREST",
                "REPRESENTATION[action_tabular=raw_p+rank+third+entropy+stability+consensus+assignment]",
                "LEARNER[online_hoeffding_forest,trees=" + choose(rng, 24, 32, 48)
                        + ",subspace=sqrt,split=hoeffding,drift=ADWIN]",
                "CALIBRATION[online_isotonic_shadow]",
                "RISK[tree_vote_disagreement+leaf_support_min]", protocol, rng));
        out.add(blueprint("GRADIENT_TREES",
                "REPRESENTATION[action_tabular+window_deltas+position_collision]",
                "LEARNER[hist_gradient_boosting,depth=" + choose(rng, 3, 4, 5)
                        + ",loss=selection_conditioned_logloss,monotone=stability:+|entropy:-]",
                "CALIBRATION[temperature_then_isotonic]",
                "RISK[ensemble_margin+time_block_stability]", protocol, rng));
        out.add(blueprint("TEMPORAL_NEURAL",
                "REPRESENTATION[permutation_sequence=120x10,embedding=number+position+relative_rank]",
                "LEARNER[causal_tcn_gru,width=" + choose(rng, 24, 32, 48)
                        + ",layers=2,dropout=0.15,loss=permutation_ce+selected_k3_aux]",
                "CALIBRATION[temperature_validation_only]",
                "RISK[mc_dropout_disagreement+regime_shift_veto]", protocol, rng));
        out.add(blueprint("PERMUTATION_GRAPH",
                "REPRESENTATION[graph_nodes=number@position,edges=migration+collision+lagged_dependence]",
                "LEARNER[message_passing_network,layers=2,aggregation=attention,output=100_cells]",
                "CALIBRATION[vector_temperature_by_position]",
                "RISK[graph_ensemble_disagreement+position_entropy]", protocol, rng));
        out.add(blueprint("ANALOG_RETRIEVAL",
                "REPRESENTATION[state_signature=transition+entropy+regime+window_slope]",
                "LEARNER[metric_knn,k=" + choose(rng, 24, 36, 48)
                        + ",distance=learned_diagonal,recency_decay=true]",
                "CALIBRATION[wilson_shrinkage_by_neighbor_support]",
                "RISK[neighbor_distance+effective_sample_floor]", protocol, rng));
        out.add(blueprint("BAYESIAN_CHANGEPOINT",
                "REPRESENTATION[run_length_state+number_hazard+position_migration]",
                "LEARNER[bayesian_online_changepoint,hazard=learned_shadow,emission=dirichlet_multinomial]",
                "CALIBRATION[posterior_predictive]",
                "RISK[run_length_uncertainty+hazard_spike_veto]", protocol, rng));
        out.add(blueprint("DIVERSE_MOE",
                "REPRESENTATION[experts=markov+forest+temporal_neural+analog+formula,gate=regime_features]",
                "LEARNER[mixture_of_experts,gate=online_ftrl,diversity_penalty=error_correlation]",
                "CALIBRATION[cross_fitted_stacking_then_isotonic]",
                "RISK[expert_disagreement+gate_concentration+worst_block]", protocol, rng));
        String noveltyTag = Long.toHexString(root).toUpperCase(java.util.Locale.US);
        if (noveltyTag.length() > 8) noveltyTag = noveltyTag.substring(0, 8);
        out.add(blueprint("NOVA_RESIDUAL_TRANSPORT_" + noveltyTag,
                "REPRESENTATION[signed_error_field=action_neighborhood+position_migration+regime_residual]",
                "LEARNER[dual_timescale_residual_transport,update=conservation_normalized_mirror_descent,"
                        + "memory=fast_miss_trace+slow_counterfactual_trace]",
                "CALIBRATION[conformal_residual_bucket_by_regime]",
                "RISK[residual_mass_concentration+transport_instability+novelty_veto]", protocol, rng));
        out.add(blueprint("NOVA_MOTIF_COMPOSER_" + noveltyTag,
                "REPRESENTATION[causal_motif_grammar=transition+collision+rank_reversal+window_disagreement]",
                "LEARNER[typed_program_graph_coevolution,credit=counterfactual_shapley_shadow,"
                        + "controller=complexity_regularized_bandit]",
                "CALIBRATION[cross_fitted_motif_isotonic]",
                "RISK[motif_support_floor+counterfactual_disagreement+description_length_veto]", protocol, rng));
        out.add(blueprint("NOVA_EVIDENCE_WEAVE_" + noveltyTag,
                "REPRESENTATION[evidence_threads=rank_margin+entropy_flow+window_survival+cross_position_residual]",
                "LEARNER[bidirectional_evidence_weave,operators=reinforce|cancel|veto,credit=future_counterfactual_regret]",
                "CALIBRATION[blockwise_conformal_weave]",
                "RISK[thread_contradiction+support_collapse+regime_break_veto]", protocol, rng));
        out.add(blueprint("NOVA_CAUSAL_SURVIVAL_LATTICE_" + noveltyTag,
                "REPRESENTATION[lattice=action@window@regime,links=survival+rank_migration+miss_recovery]",
                "LEARNER[causal_survival_lattice,update=pit_only_competing_risk,selection=stable_path_maximin]",
                "CALIBRATION[survival_probability_to_k3_isotonic]",
                "RISK[path_fragility+counterfactual_flip_rate+effective_support_floor]", protocol, rng));
        return Collections.unmodifiableList(out);
    }

    private static Blueprint blueprint(
            String family,
            String representation,
            String learner,
            String calibration,
            String risk,
            String protocol,
            ResearchDeterministicRngR811 rng) {
        String objective =
                "OBJECTIVE[maximize=wilson95_lower_hit_rate,subject_to=brier+ece+coverage+latency+ram,"
                        + "selection=future_oos_only,real_authority=false]";
        String falsifier =
                "FALSIFY[reject_if=wilson_upper_below_0.30|ece_above_0.08|unstable_time_blocks|"
                        + "latency_budget_exceeded|feature_leakage]";
        String spec =
                "METHOD_GRAPH_R903[family=" + family
                        + ",idea_seed=" + Long.toUnsignedString(rng.xorshift())
                        + "," + representation + ',' + learner + ',' + calibration + ',' + risk
                        + ',' + objective + ',' + falsifier + ',' + protocol + ']';
        return new Blueprint(family, spec);
    }

    private static int choose(ResearchDeterministicRngR811 rng, int a, int b, int c) {
        int which = rng.nextInt(3);
        return which == 0 ? a : (which == 1 ? b : c);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Blueprint {
        public final String family, canonicalSpec;

        Blueprint(String family, String canonicalSpec) {
            this.family = safe(family);
            this.canonicalSpec = safe(canonicalSpec);
        }
    }
}
