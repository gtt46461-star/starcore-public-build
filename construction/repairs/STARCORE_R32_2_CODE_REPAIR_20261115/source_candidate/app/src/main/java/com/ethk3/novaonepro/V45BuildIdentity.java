package com.ethk3.novaonepro;

/** Canonical identity for the V45 pipeline-contract/ranking build. */
public final class V45BuildIdentity {
    public static final String VERSION_NAME = "V45.0.450720";
    public static final int VERSION_CODE = 450450720;
    public static final String STRATEGY_VERSION = "V45_PIPELINE_CONTRACT_RANKING_20260720";
    public static final String BRANCH = "agent/v45-pipeline-contract-ranking";
    public static final String PACKAGE_NAME = "com.ethk3.novaonepro";
    public static final String PLAY_RULE = "ETHEREUM1_PK10_SINGLE_POSITION_FIXED_K3";

    private V45BuildIdentity() {}

    public static String summary() {
        return VERSION_NAME
                + "|"
                + STRATEGY_VERSION
                + "|"
                + BRANCH
                + "|"
                + PACKAGE_NAME
                + "|"
                + PLAY_RULE;
    }
}
