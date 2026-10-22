package com.ethk3.novaonepro;

/** Test-only caller of the actual compiled policy consumer; no DB or authority. */
public final class PolicyConsumerProbe {
    private static void require(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    private static void rejected(String text, String hash, String reason) {
        try {
            ResearchGatePlanR967.policy(text, hash);
            throw new AssertionError("POLICY_CORRUPTION_ACCEPTED:" + reason);
        } catch (IllegalStateException expected) {
            require(expected.getMessage().contains(reason), "WRONG_REJECTION:" + expected.getMessage());
        }
    }
    public static void main(String[] args) {
        ResearchGatePlanR967.Policy policy = ResearchGatePlanR967.installed();
        require(policy.isCp3, "INSTALLED_CP3");
        require(policy.hash.equals(ResearchSemanticsGeneratedR967.POLICY_HASH), "INSTALLED_HASH");
        require(policy.json.equals(ResearchSemanticsGeneratedR967.POLICY_JSON), "INSTALLED_BYTES");
        require(policy.minRate == .34 && policy.minLower == .30 && policy.maxBrier == .24
                && policy.maxEce == .04 && policy.totalAlpha == .05 && policy.consumerCap == .05,
                "ORIGINAL_THRESHOLDS");
        int[] sizes = {120, 240, 240, 240, 360};
        for (int i = 0; i < sizes.length; i++) require(policy.planned(i + 1) == sizes[i], "ORIGINAL_WINDOW");
        for (int stage : new int[] {0, 6}) {
            try { policy.planned(stage); throw new AssertionError("INVALID_STAGE_ACCEPTED"); }
            catch (IllegalArgumentException expected) { require("R967_STAGE".equals(expected.getMessage()), "STAGE_REASON"); }
        }
        require(!policy.cp3.flag("real_authority") && !policy.cp3.flag("legacy_point_rate_alone_is_certificate"),
                "NO_AUTHORITY_FROM_GENERATED_POLICY");
        require(policy.cp3.integer("min_certificate_n") == 360
                && policy.cp3.integer("latency_p95_ms_max") == 700
                && policy.cp3.integer("heap_increment_bytes_max") == 67108864,
                "ORIGINAL_CERTIFICATE_AND_RESOURCE_LIMITS");
        require(policy.cp3.roleCount("calibration_fit") == 120
                && policy.cp3.roleCount("embargo") == 1
                && policy.cp3.roleCount("calibration_validation") == 120, "ORIGINAL_ROLES");
        require(policy.cp3.numbers("eprocess_alternatives").length == 11, "ORIGINAL_ALTERNATIVES");
        String text = ResearchSemanticsGeneratedR967.POLICY_JSON;
        rejected(text, "0".repeat(64), "R967_POLICY_HASH_MISMATCH");
        String changed = text.replace("\"min_hit_rate\":0.34", "\"min_hit_rate\":0.33");
        require(!changed.equals(text), "MUTATION_APPLIED");
        rejected(changed, V51ChecksumEngine.sha256(changed), "CP3_POLICY_FIELDS_OR_VALUE_DRIFT");
        String duplicate = text.substring(0, text.length() - 1) + ",\"max_looks\":5}";
        rejected(duplicate, V51ChecksumEngine.sha256(duplicate), "CP3_POLICY_FIELDS_OR_VALUE_DRIFT");
        String wrongType = text.replace("\"max_looks\":5,", "\"max_looks\":5.0,");
        require(!wrongType.equals(text), "TYPE_MUTATION_APPLIED");
        rejected(wrongType, V51ChecksumEngine.sha256(wrongType), "CP3_POLICY_FIELDS_OR_VALUE_DRIFT");
        System.out.println("ACTUAL_COMPILED_POLICY_CONSUMER_PASS|original_gates|no_REAL_authority|hash_threshold_duplicate_type_rejected|no_DB_side_effect");
    }
}
