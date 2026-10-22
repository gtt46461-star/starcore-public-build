package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The only mathematical feature contract shared by Android serving and Python training. Existing
 * method names are retained for source compatibility.
 */
public final class FeatureContractRegistry {
    public static final String VERSION = V50BuildManifest.FEATURE_SCHEMA;

    public static final class Contract {
        public final String name;
        public final String formula;
        public final String owner;
        public final double min;
        public final double max;
        public final String windows;
        public final String smoothing;
        public final String missingValue;
        public final String trainingName;
        public final String javaName;
        public final String version;

        Contract(
                String name,
                String formula,
                String owner,
                double min,
                double max,
                String windows,
                String smoothing,
                String missingValue,
                String trainingName,
                String javaName) {
            this.name = name;
            this.formula = formula;
            this.owner = owner;
            this.min = min;
            this.max = max;
            this.windows = windows;
            this.smoothing = smoothing;
            this.missingValue = missingValue;
            this.trainingName = trainingName;
            this.javaName = javaName;
            this.version = VERSION;
        }
    }

    private static final ArrayList<Contract> CONTRACTS = new ArrayList<Contract>();

    static {
        frequency("freq_w6", 6);
        frequency("freq_w12", 12);
        frequency("freq_w24", 24);
        frequency("freq_w48", 48);
        frequency("freq_w96", 96);
        frequency("freq_w180", 180);
        frequency("freq_w360", 360);
        frequency("freq_w720", 720);
        frequency("freq_w1440", 1440);
        frequency("freq_w2880", 2880);

        register(
                "trend_slope",
                "freq_w12-freq_w48",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                -1,
                1,
                "W12,W48",
                "none",
                "0",
                "trend_slope",
                "trendSlope");
        register(
                "trend_accel",
                "(freq_w6-freq_w12)-(freq_w24-freq_w48)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                -2,
                2,
                "W6,W12,W24,W48",
                "none",
                "0",
                "trend_accel",
                "trendAcceleration");
        register(
                "gap_norm",
                "min(max(gap,0),60)/60",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "PIT history",
                "clip60",
                "1",
                "gap_norm",
                "gapNorm");
        register(
                "gap_percentile",
                "rank(current_gap in completed prior gaps)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "PIT history",
                "empirical_cdf",
                "0.5",
                "gap_percentile",
                "gapPercentile");
        register(
                "gap_hazard",
                "completed_gap_events_at_g/(completed_gap_survivors_at_g+beta)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "PIT history",
                "beta_shrink",
                "0.1",
                "gap_hazard",
                "gapHazard");
        register(
                "release_failure_rate",
                "failed_release_signals/max(release_signals,1)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "W96,W360",
                "beta_shrink",
                "0",
                "release_failure_rate",
                "releaseFailureRate");
        register(
                "volatility",
                "sqrt(mean((block_rate-mean_block_rate)^2))",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "W24,W48,W96",
                "population_std",
                "0",
                "volatility",
                "volatility");
        register(
                "position_entropy",
                "-sum(p_i*ln(p_i))/ln(10)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "per-position distribution",
                "epsilon=1e-12",
                "1",
                "position_entropy",
                "positionEntropy");
        register(
                "top3_concentration",
                "sum(top3(number_distribution))",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "per-position distribution",
                "none",
                "0.3",
                "top3_concentration",
                "top3Concentration");
        register(
                "third_fourth_margin",
                "p_rank3-p_rank4",
                V50BuildManifest.ACTION_BUILDER_OWNER,
                -1,
                1,
                "per-position distribution",
                "none",
                "0",
                "third_fourth_margin",
                "thirdMargin");
        register(
                "distribution_drift_js",
                "JS(p_short,p_long)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "W24,W360",
                "epsilon=1e-12",
                "0",
                "distribution_drift_js",
                "distributionDriftJs");
        register(
                "last_distance",
                "abs(number-last_number)/9",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "last draw",
                "none",
                "0.5",
                "last_distance",
                "lastDistance");
        register(
                "neighbor_last",
                "1(abs(number-last_number)==1)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "last draw",
                "none",
                "0",
                "neighbor_last",
                "neighborLast");
        register(
                "same_parity_last",
                "1(number%2==last_number%2)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "last draw",
                "none",
                "0",
                "same_parity_last",
                "sameParityLast");
        register(
                "same_big_last",
                "1((number>=6)==(last_number>=6))",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "last draw",
                "none",
                "0",
                "same_big_last",
                "sameBigLast");
        register(
                "relation_conditional_lift",
                "P(target|source)/max(P(target),epsilon)-1",
                "V50RelationGraphVerifier",
                -1,
                9,
                "verified lagged relation",
                "hierarchical_shrink",
                "0",
                "relation_conditional_lift",
                "relationConditionalLift");
        register(
                "relation_permutation_p",
                "(1+perm_ge_observed)/(1+permutations)",
                "V50RelationGraphVerifier",
                0,
                1,
                "PIT relation samples",
                "none",
                "1",
                "relation_permutation_p",
                "relationPermutationP");
        register(
                "state_hazard",
                "BOCPD_hazard+duration_hazard+CUSUM_drift combined by artifact",
                V50BuildManifest.STATE_OWNER,
                0,
                1,
                "state trajectory",
                "artifact",
                "1",
                "state_hazard",
                "stateHazard");
        register(
                "effective_consensus",
                "agreement*signal_strength*historical_reliability*recent_reliability*state_reliability",
                "V50EffectiveConsensusEngine",
                0,
                1,
                "expert distributions",
                "artifact",
                "0",
                "effective_consensus",
                "effectiveConsensus");
        register(
                "raw_action_p",
                "sum(NumberP for exact position Top3)",
                V50BuildManifest.ACTION_BUILDER_OWNER,
                0,
                1,
                "NumberP",
                "none",
                "0.3",
                "raw_action_p",
                "rawActionP");
        register(
                "rank_adjusted_p",
                "action ranker probability before selection calibration",
                V50BuildManifest.ACTION_RANK_OWNER,
                0,
                1,
                "10 position action group",
                "model_artifact",
                "0.3",
                "rank_adjusted_p",
                "rankAdjustedP");
        register(
                "selected_hit_p",
                "selection-conditioned calibration(rank1 action)",
                V50BuildManifest.SELECTED_PROBABILITY_OWNER,
                0,
                1,
                "rank1 only",
                "calibration_artifact",
                "0.3",
                "selected_hit_p",
                "selectedHitP");
    }

    private FeatureContractRegistry() {}

    private static void frequency(String name, int window) {
        register(
                name,
                "(count_w" + window + "+0.5)/(observed_w" + window + "+5)",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                0,
                1,
                "W" + window,
                "Jeffreys-like fixed contract alpha=0.5 denominator_add=5",
                "0.1",
                name,
                camel(name));
    }

    private static void register(
            String name,
            String formula,
            String owner,
            double min,
            double max,
            String windows,
            String smoothing,
            String missingValue,
            String trainingName,
            String javaName) {
        for (int i = 0; i < CONTRACTS.size(); i++) {
            if (CONTRACTS.get(i).name.equals(name)) {
                throw new IllegalStateException("V50_DUPLICATE_FEATURE:" + name);
            }
        }
        CONTRACTS.add(
                new Contract(
                        name,
                        formula,
                        owner,
                        min,
                        max,
                        windows,
                        smoothing,
                        missingValue,
                        trainingName,
                        javaName));
    }

    public static List<Contract> all() {
        return Collections.unmodifiableList(CONTRACTS);
    }

    public static Contract find(String name) {
        if (name == null) return null;
        for (int i = 0; i < CONTRACTS.size(); i++) {
            Contract c = CONTRACTS.get(i);
            if (name.equals(c.name)) return c;
        }
        return null;
    }

    public static double smoothFrequency(int count, int observed) {
        if (observed < 0) observed = 0;
        if (count < 0) count = 0;
        if (count > observed) count = observed;
        return (count + 0.5d) / (observed + 5.0d);
    }

    public static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0d;
        if (value < 0.0d) return 0.0d;
        if (value > 1.0d) return 1.0d;
        return value;
    }

    public static double safeLog(double value) {
        return Math.log(Math.max(1.0e-12d, value));
    }

    public static void normalize10(double[] values) {
        if (values == null || values.length != 10) return;
        double sum = 0.0d;
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i]) || Double.isInfinite(values[i]) || values[i] < 0.0d)
                values[i] = 0.0d;
            sum += values[i];
        }
        if (sum <= 1.0e-12d) {
            for (int i = 0; i < values.length; i++) values[i] = 0.1d;
            return;
        }
        for (int i = 0; i < values.length; i++) values[i] /= sum;
    }

    public static double entropy10(double[] probabilities) {
        if (probabilities == null || probabilities.length != 10) return 1.0d;
        double entropy = 0.0d;
        for (int i = 0; i < probabilities.length; i++) {
            double p = Math.max(1.0e-12d, probabilities[i]);
            entropy -= p * Math.log(p);
        }
        return clamp01(entropy / Math.log(10.0d));
    }

    public static String contractChecksum() {
        StringBuilder sb = new StringBuilder(VERSION);
        for (int i = 0; i < CONTRACTS.size(); i++) {
            Contract c = CONTRACTS.get(i);
            sb.append('|')
                    .append(c.name)
                    .append('=')
                    .append(c.formula)
                    .append('@')
                    .append(c.owner)
                    .append('[')
                    .append(String.format(Locale.US, "%.12f", c.min))
                    .append(',')
                    .append(String.format(Locale.US, "%.12f", c.max))
                    .append(']')
                    .append('{')
                    .append(c.windows)
                    .append('|')
                    .append(c.smoothing)
                    .append('|')
                    .append(c.missingValue)
                    .append('|')
                    .append(c.trainingName)
                    .append('|')
                    .append(c.javaName)
                    .append('}');
        }
        return sha256(sb.toString());
    }

    public static String exportJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"version\":\"")
                .append(escape(VERSION))
                .append("\",")
                .append("\"checksum\":\"")
                .append(contractChecksum())
                .append("\",")
                .append("\"features\":[");
        for (int i = 0; i < CONTRACTS.size(); i++) {
            if (i > 0) sb.append(',');
            Contract c = CONTRACTS.get(i);
            sb.append('{')
                    .append("\"id\":\"")
                    .append(escape(c.name))
                    .append("\",")
                    .append("\"formula\":\"")
                    .append(escape(c.formula))
                    .append("\",")
                    .append("\"owner\":\"")
                    .append(escape(c.owner))
                    .append("\",")
                    .append("\"windows\":\"")
                    .append(escape(c.windows))
                    .append("\",")
                    .append("\"smoothing\":\"")
                    .append(escape(c.smoothing))
                    .append("\",")
                    .append("\"missing\":\"")
                    .append(escape(c.missingValue))
                    .append("\",")
                    .append("\"min\":")
                    .append(Double.toString(c.min))
                    .append(',')
                    .append("\"max\":")
                    .append(Double.toString(c.max))
                    .append(',')
                    .append("\"training_name\":\"")
                    .append(escape(c.trainingName))
                    .append("\",")
                    .append("\"java_name\":\"")
                    .append(escape(c.javaName))
                    .append("\"}");
        }
        return sb.append("]}").toString();
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(data.length * 2);
            for (int i = 0; i < data.length; i++) {
                int x = data[i] & 0xff;
                if (x < 16) sb.append('0');
                sb.append(Integer.toHexString(x));
            }
            return sb.toString();
        } catch (Throwable e) {
            throw new IllegalStateException("V50_FEATURE_SHA256_UNAVAILABLE", e);
        }
    }

    private static String camel(String value) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(ch) : ch);
            upper = false;
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    public static final String B02_VERSION = "STARCORE_TIANSHU_B02_FEATURE_CONTRACT_V1";
    public static final boolean B02_MODEL_PROVEN = false;
    public static final boolean B02_SHADOW_ONLY = true;
    public static final boolean B02_PARITY_REQUIRED = true;

    public static final class B02ArtifactContract {
        public final String id,
                name,
                owner,
                schemaVersion,
                expectedHash,
                trainingName,
                javaName,
                missingPolicy,
                rangePolicy,
                parityPolicy,
                failureMode;
        public final boolean required, trained, parityPassed;

        public B02ArtifactContract(
                String id,
                String name,
                String owner,
                String schemaVersion,
                String expectedHash,
                String trainingName,
                String javaName,
                String missingPolicy,
                String rangePolicy,
                String parityPolicy,
                String failureMode,
                boolean required,
                boolean trained,
                boolean parityPassed) {
            this.id = safeB02(id);
            this.name = safeB02(name);
            this.owner = safeB02(owner);
            this.schemaVersion = safeB02(schemaVersion);
            this.expectedHash = safeB02(expectedHash);
            this.trainingName = safeB02(trainingName);
            this.javaName = safeB02(javaName);
            this.missingPolicy = safeB02(missingPolicy);
            this.rangePolicy = safeB02(rangePolicy);
            this.parityPolicy = safeB02(parityPolicy);
            this.failureMode = safeB02(failureMode);
            this.required = required;
            this.trained = trained;
            this.parityPassed = parityPassed;
        }

        public String canonical() {
            return id
                    + ':'
                    + name
                    + ':'
                    + owner
                    + ':'
                    + schemaVersion
                    + ':'
                    + expectedHash
                    + ':'
                    + trainingName
                    + ':'
                    + javaName
                    + ':'
                    + missingPolicy
                    + ':'
                    + rangePolicy
                    + ':'
                    + parityPolicy
                    + ':'
                    + failureMode
                    + ':'
                    + (required ? 1 : 0)
                    + ':'
                    + (trained ? 1 : 0)
                    + ':'
                    + (parityPassed ? 1 : 0);
        }
    }

    public static final class B02BundleGate {
        public final boolean schemaMatch,
                hashMatch,
                parityPassed,
                trained,
                shadowOnly,
                productionEligible;
        public final java.util.List<String> blocks;
        public final String gateHash;

        public B02BundleGate(
                boolean schemaMatch,
                boolean hashMatch,
                boolean parityPassed,
                boolean trained,
                java.util.List<String> blocks) {
            this.schemaMatch = schemaMatch;
            this.hashMatch = hashMatch;
            this.parityPassed = parityPassed;
            this.trained = trained;
            this.shadowOnly = true;
            this.productionEligible = false;
            this.blocks =
                    java.util.Collections.unmodifiableList(new java.util.ArrayList<String>(blocks));
            this.gateHash = sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return (schemaMatch ? 1 : 0)
                    + "|"
                    + (hashMatch ? 1 : 0)
                    + "|"
                    + (parityPassed ? 1 : 0)
                    + "|"
                    + (trained ? 1 : 0)
                    + "|1|0|"
                    + listTextB02(blocks);
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + gateHash;
        }
    }

    private static String safeB02(String value) {
        return value == null ? "" : value;
    }

    private static String listTextB02(java.util.List<String> values) {
        StringBuilder sb = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(values.get(i));
            }
        return sb.toString();
    }

    public static java.util.List<B02ArtifactContract> b02Contracts() {
        java.util.ArrayList<B02ArtifactContract> out =
                new java.util.ArrayList<B02ArtifactContract>();
        out.add(
                new B02ArtifactContract(
                        "B02-AF-001",
                        "game_code",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "game_code",
                        "game_code",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-002",
                        "provider_issue",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "provider_issue",
                        "provider_issue",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-003",
                        "gmt_issue",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "gmt_issue",
                        "gmt_issue",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-004",
                        "issue",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "issue",
                        "issue",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-005",
                        "raw_open_time",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "raw_open_time",
                        "raw_open_time",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-006",
                        "provider_open_time",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "provider_open_time",
                        "provider_open_time",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-007",
                        "fetch_time",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "fetch_time",
                        "fetch_time",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-008",
                        "source_line",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "source_line",
                        "source_line",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-009",
                        "canonical_draw_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "canonical_draw_hash",
                        "canonical_draw_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-010",
                        "raw_payload_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "raw_payload_hash",
                        "raw_payload_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-011",
                        "history_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "history_hash",
                        "history_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-012",
                        "pit_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "pit_hash",
                        "pit_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-013",
                        "feature_schema_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "feature_schema_hash",
                        "feature_schema_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-014",
                        "state_schema_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "state_schema_hash",
                        "state_schema_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-015",
                        "model_bundle_hash",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "model_bundle_hash",
                        "model_bundle_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-016",
                        "future_leak_free",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "future_leak_free",
                        "future_leak_free",
                        "EXPLICIT",
                        "BOOL",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-017",
                        "draw_gap_count",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "draw_gap_count",
                        "draw_gap_count",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-018",
                        "draw_conflict_count",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "draw_conflict_count",
                        "draw_conflict_count",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-019",
                        "draw_duplicate_count",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "draw_duplicate_count",
                        "draw_duplicate_count",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-020",
                        "draw_age_ms",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "draw_age_ms",
                        "draw_age_ms",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-021",
                        "target_until_open_ms",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "target_until_open_ms",
                        "target_until_open_ms",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-022",
                        "provider_clock_skew_ms",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "provider_clock_skew_ms",
                        "provider_clock_skew_ms",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-023",
                        "provider_latency_ms",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "provider_latency_ms",
                        "provider_latency_ms",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-024",
                        "source_failover_count",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "source_failover_count",
                        "source_failover_count",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-025",
                        "freq_w6",
                        "DrawIntegrityRecoveryEngine",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w6",
                        "freq_w6",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-026",
                        "freq_w12",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w12",
                        "freq_w12",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-027",
                        "freq_w24",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w24",
                        "freq_w24",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-028",
                        "freq_w48",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w48",
                        "freq_w48",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-029",
                        "freq_w96",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w96",
                        "freq_w96",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-030",
                        "freq_w180",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w180",
                        "freq_w180",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-031",
                        "freq_w360",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w360",
                        "freq_w360",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-032",
                        "freq_w720",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w720",
                        "freq_w720",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-033",
                        "freq_w1440",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w1440",
                        "freq_w1440",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-034",
                        "freq_w2880",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "freq_w2880",
                        "freq_w2880",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-035",
                        "trend_slope",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "trend_slope",
                        "trend_slope",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-036",
                        "trend_accel",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "trend_accel",
                        "trend_accel",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-037",
                        "gap_norm",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "gap_norm",
                        "gap_norm",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-038",
                        "gap_percentile",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "gap_percentile",
                        "gap_percentile",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-039",
                        "gap_hazard",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "gap_hazard",
                        "gap_hazard",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-040",
                        "release_failure_rate",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "release_failure_rate",
                        "release_failure_rate",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-041",
                        "volatility",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "volatility",
                        "volatility",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-042",
                        "position_entropy",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "position_entropy",
                        "position_entropy",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-043",
                        "top3_concentration",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "top3_concentration",
                        "top3_concentration",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-044",
                        "third_fourth_margin",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "third_fourth_margin",
                        "third_fourth_margin",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-045",
                        "distribution_drift_js",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "distribution_drift_js",
                        "distribution_drift_js",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-046",
                        "last_distance",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "last_distance",
                        "last_distance",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-047",
                        "neighbor_last",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "neighbor_last",
                        "neighbor_last",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-048",
                        "same_parity_last",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "same_parity_last",
                        "same_parity_last",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-049",
                        "same_big_last",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "same_big_last",
                        "same_big_last",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-050",
                        "relation_conditional_lift",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "relation_conditional_lift",
                        "relation_conditional_lift",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-051",
                        "relation_permutation_p",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "relation_permutation_p",
                        "relation_permutation_p",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-052",
                        "relation_fdr_q",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "relation_fdr_q",
                        "relation_fdr_q",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-053",
                        "motif_similarity",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "motif_similarity",
                        "motif_similarity",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-054",
                        "state_distance",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "state_distance",
                        "state_distance",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-055",
                        "domain_sample",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "domain_sample",
                        "domain_sample",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-056",
                        "domain_recent_sample",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "domain_recent_sample",
                        "domain_recent_sample",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-057",
                        "domain_oos_hit_rate",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "domain_oos_hit_rate",
                        "domain_oos_hit_rate",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-058",
                        "domain_oos_coverage",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "domain_oos_coverage",
                        "domain_oos_coverage",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-059",
                        "domain_ece",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "domain_ece",
                        "domain_ece",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-060",
                        "expert_regret",
                        "FeatureContractRegistry",
                        B02_VERSION,
                        "UNTRAINED",
                        "expert_regret",
                        "expert_regret",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-061",
                        "expert_weight",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "expert_weight",
                        "expert_weight",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-062",
                        "number_probability",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "number_probability",
                        "number_probability",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-063",
                        "sinkhorn_row_error",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "sinkhorn_row_error",
                        "sinkhorn_row_error",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-064",
                        "sinkhorn_column_error",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "sinkhorn_column_error",
                        "sinkhorn_column_error",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-065",
                        "cross_position_pressure",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "cross_position_pressure",
                        "cross_position_pressure",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-066",
                        "action_raw_p",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "action_raw_p",
                        "action_raw_p",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-067",
                        "action_calibrated_p",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "action_calibrated_p",
                        "action_calibrated_p",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-068",
                        "selected_hit_p",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "selected_hit_p",
                        "selected_hit_p",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-069",
                        "certificate_p",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "certificate_p",
                        "certificate_p",
                        "EXPLICIT",
                        "PROBABILITY_0_1",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-070",
                        "funnel_stage",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "funnel_stage",
                        "funnel_stage",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-071",
                        "champion_rank",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "champion_rank",
                        "champion_rank",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-072",
                        "shadow_only",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "shadow_only",
                        "shadow_only",
                        "EXPLICIT",
                        "BOOL",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-073",
                        "model_proven",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "model_proven",
                        "model_proven",
                        "EXPLICIT",
                        "BOOL",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-074",
                        "real_bet_authorized",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "real_bet_authorized",
                        "real_bet_authorized",
                        "EXPLICIT",
                        "BOOL",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-075",
                        "artifact_hash",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "artifact_hash",
                        "artifact_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-076",
                        "parity_hash",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "parity_hash",
                        "parity_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-077",
                        "training_schema_hash",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "training_schema_hash",
                        "training_schema_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-078",
                        "java_schema_hash",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "java_schema_hash",
                        "java_schema_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-079",
                        "bundle_hash",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "bundle_hash",
                        "bundle_hash",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        out.add(
                new B02ArtifactContract(
                        "B02-AF-080",
                        "bundle_created_at",
                        "B02ArtifactBundle",
                        B02_VERSION,
                        "UNTRAINED",
                        "bundle_created_at",
                        "bundle_created_at",
                        "EXPLICIT",
                        "FINITE",
                        "EXACT_REPLAY",
                        "SHADOW_LOCK",
                        true,
                        false,
                        false));
        return java.util.Collections.unmodifiableList(out);
    }

    public static final class B02ParityRule {
        public final String id, trainingField, javaField, tolerance, missingPolicy, failureMode;
        public final boolean exact;

        public B02ParityRule(
                String id,
                String trainingField,
                String javaField,
                String tolerance,
                String missingPolicy,
                String failureMode,
                boolean exact) {
            this.id = safeB02(id);
            this.trainingField = safeB02(trainingField);
            this.javaField = safeB02(javaField);
            this.tolerance = safeB02(tolerance);
            this.missingPolicy = safeB02(missingPolicy);
            this.failureMode = safeB02(failureMode);
            this.exact = exact;
        }

        public String canonical() {
            return id
                    + ':'
                    + trainingField
                    + ':'
                    + javaField
                    + ':'
                    + tolerance
                    + ':'
                    + missingPolicy
                    + ':'
                    + failureMode
                    + ':'
                    + (exact ? 1 : 0);
        }
    }

    public static java.util.List<B02ParityRule> b02ParityRules() {
        java.util.ArrayList<B02ParityRule> out = new java.util.ArrayList<B02ParityRule>();
        out.add(
                new B02ParityRule(
                        "B02-PARITY-001",
                        "game_code",
                        "game_code",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-002",
                        "provider_issue",
                        "provider_issue",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-003",
                        "gmt_issue",
                        "gmt_issue",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-004",
                        "issue",
                        "issue",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-005",
                        "raw_open_time",
                        "raw_open_time",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-006",
                        "provider_open_time",
                        "provider_open_time",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-007",
                        "fetch_time",
                        "fetch_time",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-008",
                        "source_line",
                        "source_line",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-009",
                        "canonical_draw_hash",
                        "canonical_draw_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-010",
                        "raw_payload_hash",
                        "raw_payload_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-011",
                        "history_hash",
                        "history_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-012",
                        "pit_hash",
                        "pit_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-013",
                        "feature_schema_hash",
                        "feature_schema_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-014",
                        "state_schema_hash",
                        "state_schema_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-015",
                        "model_bundle_hash",
                        "model_bundle_hash",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-016",
                        "future_leak_free",
                        "future_leak_free",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-017",
                        "draw_gap_count",
                        "draw_gap_count",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-018",
                        "draw_conflict_count",
                        "draw_conflict_count",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-019",
                        "draw_duplicate_count",
                        "draw_duplicate_count",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-020",
                        "draw_age_ms",
                        "draw_age_ms",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-021",
                        "target_until_open_ms",
                        "target_until_open_ms",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-022",
                        "provider_clock_skew_ms",
                        "provider_clock_skew_ms",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-023",
                        "provider_latency_ms",
                        "provider_latency_ms",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-024",
                        "source_failover_count",
                        "source_failover_count",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-025",
                        "freq_w6",
                        "freq_w6",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-026",
                        "freq_w12",
                        "freq_w12",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-027",
                        "freq_w24",
                        "freq_w24",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-028",
                        "freq_w48",
                        "freq_w48",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-029",
                        "freq_w96",
                        "freq_w96",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-030",
                        "freq_w180",
                        "freq_w180",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-031",
                        "freq_w360",
                        "freq_w360",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-032",
                        "freq_w720",
                        "freq_w720",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-033",
                        "freq_w1440",
                        "freq_w1440",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-034",
                        "freq_w2880",
                        "freq_w2880",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-035",
                        "trend_slope",
                        "trend_slope",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-036",
                        "trend_accel",
                        "trend_accel",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-037",
                        "gap_norm",
                        "gap_norm",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-038",
                        "gap_percentile",
                        "gap_percentile",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-039",
                        "gap_hazard",
                        "gap_hazard",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-040",
                        "release_failure_rate",
                        "release_failure_rate",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-041",
                        "volatility",
                        "volatility",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-042",
                        "position_entropy",
                        "position_entropy",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-043",
                        "top3_concentration",
                        "top3_concentration",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-044",
                        "third_fourth_margin",
                        "third_fourth_margin",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-045",
                        "distribution_drift_js",
                        "distribution_drift_js",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-046",
                        "last_distance",
                        "last_distance",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-047",
                        "neighbor_last",
                        "neighbor_last",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-048",
                        "same_parity_last",
                        "same_parity_last",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-049",
                        "same_big_last",
                        "same_big_last",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-050",
                        "relation_conditional_lift",
                        "relation_conditional_lift",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-051",
                        "relation_permutation_p",
                        "relation_permutation_p",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-052",
                        "relation_fdr_q",
                        "relation_fdr_q",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-053",
                        "motif_similarity",
                        "motif_similarity",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-054",
                        "state_distance",
                        "state_distance",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-055",
                        "domain_sample",
                        "domain_sample",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-056",
                        "domain_recent_sample",
                        "domain_recent_sample",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-057",
                        "domain_oos_hit_rate",
                        "domain_oos_hit_rate",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-058",
                        "domain_oos_coverage",
                        "domain_oos_coverage",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-059",
                        "domain_ece",
                        "domain_ece",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-060",
                        "expert_regret",
                        "expert_regret",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-061",
                        "expert_weight",
                        "expert_weight",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-062",
                        "number_probability",
                        "number_probability",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-063",
                        "sinkhorn_row_error",
                        "sinkhorn_row_error",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        out.add(
                new B02ParityRule(
                        "B02-PARITY-064",
                        "sinkhorn_column_error",
                        "sinkhorn_column_error",
                        "0.0",
                        "EXPLICIT",
                        "SHADOW_LOCK",
                        true));
        return java.util.Collections.unmodifiableList(out);
    }

    public static B02BundleGate b02EvaluateBundle(
            String schemaHash, String artifactHash, String parityHash, boolean trained) {
        java.util.ArrayList<String> blocks = new java.util.ArrayList<String>();
        boolean schema = schemaHash != null && schemaHash.equals(b02ContractHash());
        boolean hash = artifactHash != null && artifactHash.length() == 64;
        boolean parity = parityHash != null && parityHash.length() == 64;
        if (!schema) blocks.add("SCHEMA_HASH_MISMATCH");
        if (!hash) blocks.add("ARTIFACT_HASH_MISSING");
        if (!parity) blocks.add("PARITY_NOT_PROVEN");
        if (!trained) blocks.add("MODEL_NOT_TRAINED");
        blocks.add("B02_SHADOW_ONLY");
        return new B02BundleGate(schema, hash, parity, trained, blocks);
    }

    public static String b02ContractHash() {
        StringBuilder sb = new StringBuilder(B02_VERSION);
        for (B02ArtifactContract c : b02Contracts()) sb.append('|').append(c.canonical());
        return sha256(sb.toString());
    }

    public static boolean b02FeatureSelfTest() {
        java.util.List<B02ArtifactContract> cs = b02Contracts();
        if (cs.size() != 80 || b02ParityRules().size() != 64) return false;
        B02BundleGate g =
                b02EvaluateBundle(b02ContractHash(), sha256("artifact"), sha256("parity"), false);
        return g.schemaMatch
                && g.hashMatch
                && g.parityPassed
                && !g.productionEligible
                && g.shadowOnly
                && g.blocks.contains("MODEL_NOT_TRAINED");
    }
}
