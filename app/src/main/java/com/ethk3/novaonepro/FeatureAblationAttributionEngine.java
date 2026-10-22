package com.ethk3.novaonepro;

import java.util.List;

/**
 * Lightweight per-issue ablation responsibility snapshot; it never changes EventP or authorizes
 * BET.
 */
public final class FeatureAblationAttributionEngine {
    public static final String MODULE_VERSION =
            "V47_FEATURE_ABLATION_ATTRIBUTION_20260721_47072147";

    public static final class Result {
        public String code = "";
        public String owner = "";
        public String detail = "";

        public String summary() {
            return code + "|owner=" + owner + "|" + detail;
        }
    }

    private FeatureAblationAttributionEngine() {}

    public static Result diagnose(
            List<Models.ComboCandidate> list, CrossFittedNumberModelEngine.Snapshot model) {
        Result r = new Result();
        if (model == null || model.validationTargets < 30) {
            r.code = "MODEL_NOT_PROVEN";
            r.owner = "CrossFittedNumberModelEngine";
            r.detail = "validationTargets=" + (model == null ? 0 : model.validationTargets);
            return r;
        }
        if (model.validationTop3Rate < 0.30d) {
            r.code = AppConfig.V47_ROOT_MODEL;
            r.owner = "CrossFittedNumberModelEngine";
            r.detail = "Top3=" + model.validationTop3Rate + "低于30%";
            return r;
        }
        if (model.brierGain <= 0) {
            r.code = AppConfig.V47_ROOT_FEATURE;
            r.owner = "Recent/EWMA/Markov/Gap";
            r.detail = "BrierGain=" + model.brierGain;
            return r;
        }
        if (list == null || list.size() < 40) {
            r.code = AppConfig.V47_ROOT_SURFACE;
            r.owner = "AutoFetchService/Final40ContractAuditor";
            r.detail = "formal=" + (list == null ? 0 : list.size());
            return r;
        }
        r.code = "MODEL_ENGINEERING_READY";
        r.owner = "V47";
        r.detail = model.summary();
        return r;
    }
}
