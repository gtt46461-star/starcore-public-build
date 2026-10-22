package com.ethk3.novaonepro;

/** Generated V50 within-issue action ranker artifact. */
public final class V50ActionRankerArtifact {
    private V50ActionRankerArtifact() {}

    public static final String ARTIFACT_ID = "V50_ACTION_RANKER_PAIRWISE_LOGISTIC";
    public static final String DATA_SHA256 =
            "f2c22c332cf43a573d77be4484b3cfa42db5251188d2558545d0baee9e112acd";
    public static final String PARAMETER_SHA256 =
            "ffed775b842a86f79d985ea4174b3e515dd47721613c337ad7a930bd2b763bcf";
    public static final String OOS_METRICS_JSON =
            "{\"PAIRWISE_LOGISTIC\":{\"issues\":4160,\"rank1_rate\":0.30432692307692305,\"rank2_rate\":0.3019230769230769,\"rank3_rate\":0.30264423076923075,\"rank1_minus_rank2\":0.0024038461538461453,\"rank1_minus_rank3\":0.0016826923076923017,\"ndcg_at_1\":0.30432692307692305,\"top1_regret\":0.6622596153846154,\"top1_hit_given_any_position_hit\":0.3148470529718975,\"folds\":[{\"issues\":1040,\"rank1_rate\":0.2875,\"rank2_rate\":0.3096153846153846,\"rank3_rate\":0.2846153846153846,\"rank1_minus_rank2\":-0.022115384615384648,\"rank1_minus_rank3\":0.0028846153846153744,\"ndcg_at_1\":0.2875,\"top1_regret\":0.6721153846153847,\"top1_hit_given_any_position_hit\":0.2995991983967936,\"fold\":1,\"train_groups\":2773,\"test_start_index\":2773,\"test_end_index_exclusive\":3813,\"train_last_issue\":\"202603060806\",\"test_first_issue\":\"202603060807\",\"test_last_issue\":\"202603070406\"},{\"issues\":1040,\"rank1_rate\":0.33076923076923076,\"rank2_rate\":0.2903846153846154,\"rank3_rate\":0.3105769230769231,\"rank1_minus_rank2\":0.04038461538461535,\"rank1_minus_rank3\":0.020192307692307676,\"ndcg_at_1\":0.33076923076923076,\"top1_regret\":0.6375,\"top1_hit_given_any_position_hit\":0.3416087388282026,\"fold\":2,\"train_groups\":3813,\"test_start_index\":3813,\"test_end_index_exclusive\":4853,\"train_last_issue\":\"202603070406\",\"test_first_issue\":\"202603070407\",\"test_last_issue\":\"202603090789\"},{\"issues\":1040,\"rank1_rate\":0.2875,\"rank2_rate\":0.29615384615384616,\"rank3_rate\":0.3,\"rank1_minus_rank2\":-0.008653846153846179,\"rank1_minus_rank3\":-0.012500000000000011,\"ndcg_at_1\":0.2875,\"top1_regret\":0.6817307692307693,\"top1_hit_given_any_position_hit\":0.29662698412698413,\"fold\":3,\"train_groups\":4853,\"test_start_index\":4853,\"test_end_index_exclusive\":5893,\"train_last_issue\":\"202603090789\",\"test_first_issue\":\"202603090790\",\"test_last_issue\":\"202603100389\"},{\"issues\":1040,\"rank1_rate\":0.31153846153846154,\"rank2_rate\":0.31153846153846154,\"rank3_rate\":0.3153846153846154,\"rank1_minus_rank2\":0.0,\"rank1_minus_rank3\":-0.0038461538461538325,\"ndcg_at_1\":0.31153846153846154,\"top1_regret\":0.6576923076923077,\"top1_hit_given_any_position_hit\":0.32142857142857145,\"fold\":4,\"train_groups\":5893,\"test_start_index\":5893,\"test_end_index_exclusive\":6933,\"train_last_issue\":\"202603100389\",\"test_first_issue\":\"202603100390\",\"test_last_issue\":\"202603110295\"}]},\"BINARY_LOGISTIC\":{\"issues\":4160,\"rank1_rate\":0.30264423076923075,\"rank2_rate\":0.3076923076923077,\"rank3_rate\":0.3108173076923077,\"rank1_minus_rank2\":-0.005048076923076961,\"rank1_minus_rank3\":-0.00817307692307695,\"ndcg_at_1\":0.30264423076923075,\"top1_regret\":0.6639423076923077,\"top1_hit_given_any_position_hit\":0.3131061924894305,\"folds\":[{\"issues\":1040,\"rank1_rate\":0.2932692307692308,\"rank2_rate\":0.30288461538461536,\"rank3_rate\":0.31346153846153846,\"rank1_minus_rank2\":-0.009615384615384581,\"rank1_minus_rank3\":-0.020192307692307676,\"ndcg_at_1\":0.2932692307692308,\"top1_regret\":0.6663461538461538,\"top1_hit_given_any_position_hit\":0.30561122244488975,\"fold\":1,\"train_groups\":2773,\"test_start_index\":2773,\"test_end_index_exclusive\":3813,\"train_last_issue\":\"202603060806\",\"test_first_issue\":\"202603060807\",\"test_last_issue\":\"202603070406\"},{\"issues\":1040,\"rank1_rate\":0.30288461538461536,\"rank2_rate\":0.32019230769230766,\"rank3_rate\":0.30865384615384617,\"rank1_minus_rank2\":-0.0173076923076923,\"rank1_minus_rank3\":-0.005769230769230804,\"ndcg_at_1\":0.30288461538461536,\"top1_regret\":0.6653846153846154,\"top1_hit_given_any_position_hit\":0.3128103277060576,\"fold\":2,\"train_groups\":3813,\"test_start_index\":3813,\"test_end_index_exclusive\":4853,\"train_last_issue\":\"202603070406\",\"test_first_issue\":\"202603070407\",\"test_last_issue\":\"202603090789\"},{\"issues\":1040,\"rank1_rate\":0.2932692307692308,\"rank2_rate\":0.30288461538461536,\"rank3_rate\":0.29423076923076924,\"rank1_minus_rank2\":-0.009615384615384581,\"rank1_minus_rank3\":-0.0009615384615384581,\"ndcg_at_1\":0.2932692307692308,\"top1_regret\":0.6759615384615385,\"top1_hit_given_any_position_hit\":0.30257936507936506,\"fold\":3,\"train_groups\":4853,\"test_start_index\":4853,\"test_end_index_exclusive\":5893,\"train_last_issue\":\"202603090789\",\"test_first_issue\":\"202603090790\",\"test_last_issue\":\"202603100389\"},{\"issues\":1040,\"rank1_rate\":0.3211538461538462,\"rank2_rate\":0.30480769230769234,\"rank3_rate\":0.3269230769230769,\"rank1_minus_rank2\":0.016346153846153844,\"rank1_minus_rank3\":-0.005769230769230749,\"ndcg_at_1\":0.3211538461538462,\"top1_regret\":0.6480769230769231,\"top1_hit_given_any_position_hit\":0.33134920634920634,\"fold\":4,\"train_groups\":5893,\"test_start_index\":5893,\"test_end_index_exclusive\":6933,\"train_last_issue\":\"202603100389\",\"test_first_issue\":\"202603100390\",\"test_last_issue\":\"202603110295\"}]},\"LISTWISE_SOFTMAX\":{\"issues\":4160,\"rank1_rate\":0.29591346153846154,\"rank2_rate\":0.3105769230769231,\"rank3_rate\":0.303125,\"rank1_minus_rank2\":-0.014663461538461542,\"rank1_minus_rank3\":-0.007211538461538436,\"ndcg_at_1\":0.29591346153846154,\"top1_regret\":0.6706730769230769,\"top1_hit_given_any_position_hit\":0.3061427505595623,\"folds\":[{\"issues\":1040,\"rank1_rate\":0.28557692307692306,\"rank2_rate\":0.31153846153846154,\"rank3_rate\":0.2951923076923077,\"rank1_minus_rank2\":-0.02596153846153848,\"rank1_minus_rank3\":-0.009615384615384637,\"ndcg_at_1\":0.28557692307692306,\"top1_regret\":0.6740384615384616,\"top1_hit_given_any_position_hit\":0.29759519038076154,\"fold\":1,\"train_groups\":2773,\"test_start_index\":2773,\"test_end_index_exclusive\":3813,\"train_last_issue\":\"202603060806\",\"test_first_issue\":\"202603060807\",\"test_last_issue\":\"202603070406\"},{\"issues\":1040,\"rank1_rate\":0.3096153846153846,\"rank2_rate\":0.3057692307692308,\"rank3_rate\":0.29903846153846153,\"rank1_minus_rank2\":0.0038461538461538325,\"rank1_minus_rank3\":0.010576923076923095,\"ndcg_at_1\":0.3096153846153846,\"top1_regret\":0.6586538461538461,\"top1_hit_given_any_position_hit\":0.31976166832174774,\"fold\":2,\"train_groups\":3813,\"test_start_index\":3813,\"test_end_index_exclusive\":4853,\"train_last_issue\":\"202603070406\",\"test_first_issue\":\"202603070407\",\"test_last_issue\":\"202603090789\"},{\"issues\":1040,\"rank1_rate\":0.29423076923076924,\"rank2_rate\":0.30480769230769234,\"rank3_rate\":0.30096153846153845,\"rank1_minus_rank2\":-0.010576923076923095,\"rank1_minus_rank3\":-0.006730769230769207,\"ndcg_at_1\":0.29423076923076924,\"top1_regret\":0.675,\"top1_hit_given_any_position_hit\":0.30357142857142855,\"fold\":3,\"train_groups\":4853,\"test_start_index\":4853,\"test_end_index_exclusive\":5893,\"train_last_issue\":\"202603090789\",\"test_first_issue\":\"202603090790\",\"test_last_issue\":\"202603100389\"},{\"issues\":1040,\"rank1_rate\":0.29423076923076924,\"rank2_rate\":0.32019230769230766,\"rank3_rate\":0.3173076923076923,\"rank1_minus_rank2\":-0.025961538461538425,\"rank1_minus_rank3\":-0.02307692307692305,\"ndcg_at_1\":0.29423076923076924,\"top1_regret\":0.675,\"top1_hit_given_any_position_hit\":0.30357142857142855,\"fold\":4,\"train_groups\":5893,\"test_start_index\":5893,\"test_end_index_exclusive\":6933,\"train_last_issue\":\"202603100389\",\"test_first_issue\":\"202603100390\",\"test_last_issue\":\"202603110295\"}]},\"HIST_GBDT_CHALLENGER\":{\"issues\":4160,\"rank1_rate\":0.30504807692307695,\"rank2_rate\":0.31153846153846154,\"rank3_rate\":0.29639423076923077,\"rank1_minus_rank2\":-0.006490384615384592,\"rank1_minus_rank3\":0.008653846153846179,\"ndcg_at_1\":0.30504807692307695,\"top1_regret\":0.6615384615384615,\"top1_hit_given_any_position_hit\":0.31559313603581196,\"folds\":[{\"issues\":1040,\"rank1_rate\":0.2903846153846154,\"rank2_rate\":0.3105769230769231,\"rank3_rate\":0.28076923076923077,\"rank1_minus_rank2\":-0.020192307692307676,\"rank1_minus_rank3\":0.009615384615384637,\"ndcg_at_1\":0.2903846153846154,\"top1_regret\":0.6692307692307692,\"top1_hit_given_any_position_hit\":0.3026052104208417,\"fold\":1,\"train_groups\":2773,\"test_start_index\":2773,\"test_end_index_exclusive\":3813,\"train_last_issue\":\"202603060806\",\"test_first_issue\":\"202603060807\",\"test_last_issue\":\"202603070406\"},{\"issues\":1040,\"rank1_rate\":0.30865384615384617,\"rank2_rate\":0.2951923076923077,\"rank3_rate\":0.3057692307692308,\"rank1_minus_rank2\":0.01346153846153847,\"rank1_minus_rank3\":0.0028846153846153744,\"ndcg_at_1\":0.30865384615384617,\"top1_regret\":0.6596153846153846,\"top1_hit_given_any_position_hit\":0.3187686196623635,\"fold\":2,\"train_groups\":3813,\"test_start_index\":3813,\"test_end_index_exclusive\":4853,\"train_last_issue\":\"202603070406\",\"test_first_issue\":\"202603070407\",\"test_last_issue\":\"202603090789\"},{\"issues\":1040,\"rank1_rate\":0.3125,\"rank2_rate\":0.3278846153846154,\"rank3_rate\":0.2798076923076923,\"rank1_minus_rank2\":-0.015384615384615385,\"rank1_minus_rank3\":0.03269230769230769,\"ndcg_at_1\":0.3125,\"top1_regret\":0.6567307692307692,\"top1_hit_given_any_position_hit\":0.32242063492063494,\"fold\":3,\"train_groups\":4853,\"test_start_index\":4853,\"test_end_index_exclusive\":5893,\"train_last_issue\":\"202603090789\",\"test_first_issue\":\"202603090790\",\"test_last_issue\":\"202603100389\"},{\"issues\":1040,\"rank1_rate\":0.30865384615384617,\"rank2_rate\":0.3125,\"rank3_rate\":0.3192307692307692,\"rank1_minus_rank2\":-0.0038461538461538325,\"rank1_minus_rank3\":-0.01057692307692304,\"ndcg_at_1\":0.30865384615384617,\"top1_regret\":0.6605769230769231,\"top1_hit_given_any_position_hit\":0.31845238095238093,\"fold\":4,\"train_groups\":5893,\"test_start_index\":5893,\"test_end_index_exclusive\":6933,\"train_last_issue\":\"202603100389\",\"test_first_issue\":\"202603100390\",\"test_last_issue\":\"202603110295\"}]}}";
    public static final double OOS_RANK1_RATE = 0.30432692307692305d;
    public static final double OOS_RANK2_RATE = 0.30192307692307691d;
    public static final double OOS_RANK3_RATE = 0.30264423076923075d;
    public static final boolean OOS_RANK_ORDER_PASSED = true;
    public static final int SCORE_DECIMALS = 6;
    public static final double SCORE_SCALE = 1000000d;
    public static final int FEATURE_COUNT = 20;
    public static final String[] FEATURE_NAMES =
            new String[] {
                "raw_action_p",
                "top1_p",
                "top2_p",
                "third_p",
                "fourth_p",
                "third_margin",
                "top1_margin",
                "entropy",
                "signal_strength",
                "position_norm",
                "mean_top3_slope",
                "mean_top3_accel",
                "mean_top3_gap_percentile",
                "mean_top3_volatility",
                "switch_risk",
                "family_strength",
                "raw_minus_issue_mean",
                "raw_minus_issue_second",
                "raw_issue_z",
                "number_model_entropy"
            };
    public static final double[] MEAN =
            new double[] {
                0.3280924736803269,
                0.11396267777529402,
                0.10860021461023152,
                0.10552958129480242,
                0.10298225302053135,
                0.0025473282742697187,
                0.0053624631650617972,
                0.99841185465919668,
                0.053345706970987485,
                0.5,
                0.003722544115785468,
                0.0023147608853467693,
                0.55313842021860837,
                0.19836816707975935,
                0.74935468578066089,
                0.23718134581013176,
                -5.8449647192872076e-20,
                -0.0074801239880287994,
                1.9728757627840657e-18,
                0.99841185465919668
            };
    public static final double[] SCALE =
            new double[] {
                0.011253259337524892,
                0.0064172078312165455,
                0.0039469777527127246,
                0.0030594130485654971,
                0.0023441931147263088,
                0.0025627966705578146,
                0.0051507461029143232,
                0.001213682768949667,
                0.02226348320247255,
                0.31914236925208822,
                0.027877582955326222,
                0.033295502336260059,
                0.1678204385214403,
                0.061449071299138881,
                0.13321176206244487,
                0.20178259871051082,
                0.0070830547684028796,
                0.0077891026667452111,
                1.0000000000000093,
                0.001213682768949667
            };
    public static final double[] WEIGHT =
            new double[] {
                -0.00050734691731527484,
                -0.0027258226743569246,
                0.0010786902806281253,
                0.0003254848833656337,
                -0.00046773160790206027,
                0.00078399993870910346,
                -0.0035955450819321463,
                0.00010973700905474974,
                -0.0053294539468899177,
                -0.012330180400455347,
                0.0112363634844889,
                0.016282795679934781,
                0.020034515494955971,
                0.019301908535062137,
                -0.0041253120976567645,
                -0.010031711515423595,
                -0.00031933557151228413,
                -0.00035116734699678097,
                0.023423115900281028,
                0.00010973700905474974
            };
    public static final double BIAS = 0.19896979049202454d;
}
