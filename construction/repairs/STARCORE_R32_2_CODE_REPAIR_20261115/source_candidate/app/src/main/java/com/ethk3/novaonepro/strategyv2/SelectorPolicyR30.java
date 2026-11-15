package com.ethk3.novaonepro.strategyv2;

import java.util.List;
import java.util.Locale;

/** Combines a fixed score/update program with a position and ACT rule selected before confirmation. */
public final class SelectorPolicyR30 {
    private SelectorPolicyR30() {}

    public static PolicySpecR27 combine(PolicySpecR27 base, int position, PolicySpecR27.Rule rule, double threshold,
            double rate, EvalContextR27 context, PolicySpecR27.Meta provenance) {
        PolicySpecR27 candidate = base.withSelector(position, rule, threshold, rate, provenance);
        List<String> violations = PolicyContractR30.violations(candidate);
        if (!violations.isEmpty()) throw new IllegalArgumentException("R30_SELECTOR_CONTRACT:" + violations);
        String study;
        double registered = rate;
        if (context == null) study = "act=NOT_RUN@H_SELECT";
        else {
            double tau = context.temperature(candidate); // the existing, birth-fixed fitting rule reads H_TRAIN
            ReplayScoreR27 score = ReplayEvaluatorR27.evaluate(candidate, context, SplitR27.SELECT, tau, null);
            if (score.nSched == 0 || score.skippedGap == score.nSched) study = "act=INSUFFICIENT@H_SELECT; scheduled=" + score.nSched + "; skipped=" + score.skippedGap;
            else {
                // MARGIN does not use rate to decide ACT. Bind its estimated qualification rate into policy identity.
                // Preserve QUANTILE/RANDOM rate: changing it would change the execution rule being measured.
                if (rule == PolicySpecR27.Rule.MARGIN || rule == PolicySpecR27.Rule.ALWAYS) registered = Math.max(0.01, score.actRate());
                study = String.format(Locale.ROOT, "act≈%.6f@H_SELECT; acts=%d; scheduled=%d; skipped=%d; registeredRate=%.6f; range=[%d,%d); dataset=%s",
                        score.actRate(), score.nAct, score.nSched, score.skippedGap, registered,
                        context.split.selectFrom, context.split.selectTo, context.data.datasetHash);
                if ((rule == PolicySpecR27.Rule.MARGIN || rule == PolicySpecR27.Rule.ALWAYS) && score.actRate() < 0.01)
                    study += "; registeredRate uses existing deltaMin floor 0.01; observed rate is reported above";
            }
        }
        PolicySpecR27.Meta meta = new PolicySpecR27.Meta(provenance.idea, provenance.operator, provenance.parents,
                provenance.generation, provenance.seed, provenance.reason + "；" + study);
        return base.withSelector(position, rule, threshold, registered, meta);
    }

    /** Existing unestimated MARGIN policies keep the 0.15 fallback; fitted selector rates are already in identity. */
    public static double registeredRate(PolicySpecR27 spec) {
        if (spec.rule == PolicySpecR27.Rule.QUANTILE || spec.rule == PolicySpecR27.Rule.RANDOM) return spec.rate;
        return Double.isFinite(spec.rate) && spec.rate > 0 && spec.rate <= 1 ? spec.rate : 0.15;
    }
}
