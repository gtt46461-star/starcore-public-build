package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Generated, immutable 96-index research feature contract. */
public final class ResearchFeatureSchemaR815 {
    public static final int VERSION = 2;
    public static final int SIZE = 96;

    public static final class FeatureDef {
        public final int index;
        public final String name;
        public final String formula;
        public final String source;
        public final String missingPolicy;
        public final String normalization;

        FeatureDef(
                int index,
                String name,
                String formula,
                String source,
                String missingPolicy,
                String normalization) {
            this.index = index;
            this.name = name;
            this.formula = formula;
            this.source = source;
            this.missingPolicy = missingPolicy;
            this.normalization = normalization;
        }

        public String canonical() {
            return index
                    + ","
                    + name
                    + ","
                    + formula
                    + ","
                    + source
                    + ","
                    + missingPolicy
                    + ","
                    + normalization;
        }
    }

    private static final FeatureDef[] DEFS = build();
    private static final Map<String, Integer> INDEX_BY_NAME = indexByName();
    private static final String SCHEMA_SHA256 = schema();

    private ResearchFeatureSchemaR815() {}

    public static FeatureDef byIndex(int index) {
        if (index < 0 || index >= SIZE) throw new IllegalArgumentException("R815_FEATURE_INDEX_RANGE");
        return DEFS[index];
    }

    public static int indexOf(String name) {
        Integer index = INDEX_BY_NAME.get(name == null ? "" : name.trim());
        if (index == null) throw new IllegalArgumentException("R815_FEATURE_NAME_UNKNOWN");
        return index.intValue();
    }

    public static Map<String, Integer> indexes() {
        return INDEX_BY_NAME;
    }

    public static String schemaSha256() {
        return SCHEMA_SHA256;
    }

    public static void assertIntegrity() {
        if (DEFS.length != SIZE || INDEX_BY_NAME.size() != SIZE || SCHEMA_SHA256.length() != 64)
            throw new IllegalStateException("R815_FEATURE_SCHEMA_INTEGRITY");
        for (int i = 0; i < SIZE; i++) {
            FeatureDef definition = DEFS[i];
            if (definition == null
                    || definition.index != i
                    || definition.name.length() == 0
                    || definition.formula.length() == 0
                    || definition.source.length() == 0)
                throw new IllegalStateException("R815_FEATURE_SCHEMA_ROW_" + i);
        }
    }

    private static FeatureDef[] build() {
        String[] names = new String[] {
            "q_p1_n1","q_p1_n2","q_p1_n3","q_p1_n4","q_p1_n5","q_p1_n6","q_p1_n7","q_p1_n8","q_p1_n9","q_p1_n10",
            "q_p2_n1","q_p2_n2","q_p2_n3","q_p2_n4","q_p2_n5","q_p2_n6","q_p2_n7","q_p2_n8","q_p2_n9","q_p2_n10",
            "assignment_p1","assignment_p2","assignment_p3","assignment_p4","assignment_p5","assignment_p6","assignment_p7","assignment_p8","assignment_p9","assignment_p10",
            "assignment_score","assignment_margin","location_diag_mean","location_entropy","subset_pair_mean","subset_triplet_mean","subset_1013_mean","subset_1013_max","subset_1013_min","history_suffix",
            "history_transition_ready","history_long_ready","history_full_ready","number_entropy_p1","number_entropy_p2","number_entropy_p3","number_entropy_p4","number_entropy_p5","number_entropy_p6","number_entropy_p7",
            "number_entropy_p8","number_entropy_p9","number_entropy_p10","q_global_mean","q_global_std","q_global_max","q_global_min","assignment_q_mean","assignment_q_min","assignment_q_max",
            "selected_position","selected_n1","selected_n2","selected_n3","selected_k3_mass","selected_k3_rank","selected_q_mean","selected_q_min","selected_q_max","selected_assignment_match",
            "cross_position_collision","cross_position_coverage","location_vote_p1","location_vote_p2","location_vote_p3","location_vote_p4","location_vote_p5","location_vote_p6","location_vote_p7","location_vote_p8",
            "location_vote_p9","location_vote_p10","dataset_scope_train","dataset_scope_validation","dataset_scope_oos","dataset_scope_future_live","cache_dp_calls","cache_vote_calls","schema_version","binding_hash_fraction",
            "cache_subset_count","cache_assignment_score","cache_qhash_fraction","dataset_hash_fraction","target_issue_day_fraction","target_issue_sequence_fraction"
        };
        if (names.length != SIZE) throw new IllegalStateException("R815_FEATURE_SCHEMA_SIZE");
        FeatureDef[] defs = new FeatureDef[SIZE];
        for (int i = 0; i < names.length; i++) {
            String source;
            String formula;
            String normalization;
            if (i < 20) {
                source = "CrossPositionPrimitiveCacheR815.q";
                formula = "normalized_q[position,number]";
                normalization = "[0,1]";
            } else if (i < 30) {
                source = "CrossPositionPrimitiveCacheR815.assignment";
                formula = "assigned_number[position]/10";
                normalization = "[0.1,1]";
            } else if (i < 40) {
                source = "CrossPositionPrimitiveCacheR815.summary";
                formula = "immutable_cache_summary";
                normalization = "bounded";
            } else if (i < 63) {
                source = "HistoryIntegrityVerifierR815|CrossPositionPrimitiveCacheR815";
                formula = "history_or_distribution_statistic";
                normalization = "[0,1]";
            } else if (i < 73) {
                source = "ResearchFeatureMaterializerR815.action";
                formula = "selected_action_projection";
                normalization = "bounded";
            } else if (i < 83) {
                source = "CrossPositionPrimitiveCacheR815.location";
                formula = "location_vote_projection";
                normalization = "[0,1]";
            } else {
                source = "DatasetIdentityGateR815|ResearchFeatureSchemaR815";
                formula = "identity_or_counter_projection";
                normalization = "bounded";
            }
            defs[i] = new FeatureDef(i, names[i], formula, source, "UNAVAILABLE_BIT_FALSE", normalization);
        }
        return defs;
    }

    private static Map<String, Integer> indexByName() {
        HashMap<String, Integer> out = new HashMap<String, Integer>();
        for (int i = 0; i < DEFS.length; i++) {
            if (out.put(DEFS[i].name, Integer.valueOf(i)) != null)
                throw new IllegalStateException("R815_FEATURE_SCHEMA_DUPLICATE_NAME");
        }
        return Collections.unmodifiableMap(out);
    }

    private static String schema() {
        StringBuilder canonical = new StringBuilder("R815_FEATURE_SCHEMA_V").append(VERSION);
        for (int i = 0; i < DEFS.length; i++) canonical.append('\n').append(DEFS[i].canonical());
        return V51ChecksumEngine.sha256(canonical.toString());
    }
}
