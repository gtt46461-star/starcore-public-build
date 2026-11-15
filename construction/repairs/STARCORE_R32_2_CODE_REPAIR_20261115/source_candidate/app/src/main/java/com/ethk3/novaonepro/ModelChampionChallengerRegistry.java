package com.ethk3.novaonepro;

/** Explicit live owner registry. Legacy models are background challengers only. */
public final class ModelChampionChallengerRegistry {
    public static final String MODULE_VERSION =
            "V46_CHAMPION_CHALLENGER_REGISTRY_20260721_46072130";
    public static final String LIVE_CHAMPION =
            "NUMBER_DISTRIBUTION+ONLINE_RESIDUAL+CONDITIONAL_CALIBRATION+HIERARCHICAL_POSTERIOR";
    public static final String BACKGROUND_CHALLENGERS =
            "LOGISTIC_L2|PAIRWISE_LOGISTIC|CATBOOST_XGBOOST|TCN_GRU";

    private ModelChampionChallengerRegistry() {}

    public static void persist(DbStore store) {
        try {
            if (store != null) {
                store.setState("v46_model_registry", MODULE_VERSION);
                store.setState("v46_live_champion", LIVE_CHAMPION);
                store.setState("v46_background_challengers", BACKGROUND_CHALLENGERS);
            }
        } catch (Throwable ignore) {
        }
    }
}
