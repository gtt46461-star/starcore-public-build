package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Atomic artifact bundle manifest; partial promotion is forbidden. */
public final class V51ArtifactBundleManifest {
    private V51ArtifactBundleManifest() {}

    public static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_MATERIALIZED_20260805";
    public static final String NUMBER_MODEL_ID = V51NumberModelArtifact.MODEL_ID;
    public static final String EXPERT_GATE_ID = V51ExpertGateArtifact.ARTIFACT_ID;
    public static final String RANKER_ID = V51ActionRankerArtifact.ARTIFACT_ID;
    public static final String CALIBRATION_ID = V51SelectedCalibrationArtifact.ARTIFACT_ID;
    public static final String PREDICTABILITY_ID = V51PredictabilityArtifact.ARTIFACT_ID;
    public static final String POLICY_ID = V51PolicySurfaceArtifact.ARTIFACT_ID;
    public static final String DATA_SHA256 = V51NumberModelArtifact.DATA_SHA256;
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String BUNDLE_STATE = "MODEL_NOT_PROVEN_CONTROLLED_PROBE";
    public static final boolean TRAINED_FOR_BINANCE = false;
    public static final boolean PARITY_PROVEN = false;
    public static final boolean OOS_PROVEN = false;
    public static final boolean REALTIME_PROVEN = false;

    public static final class Component {
        public final String id;
        public final String value;
        public final String dataHash;
        public final boolean trained;
        public final boolean proven;

        Component(String id, String value, String dataHash, boolean trained, boolean proven) {
            this.id = id;
            this.value = value == null ? "" : value;
            this.dataHash = dataHash == null ? "" : dataHash;
            this.trained = trained;
            this.proven = proven;
        }

        public String canonical() {
            return id
                    + ':'
                    + value
                    + ':'
                    + dataHash
                    + ':'
                    + (trained ? '1' : '0')
                    + ':'
                    + (proven ? '1' : '0');
        }
    }

    public static final class BundleRequirement {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        BundleRequirement(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0');
        }
    }

    public static List<Component> components() {
        ArrayList<Component> out = new ArrayList<Component>();
        out.add(
                new Component(
                        "NUMBER_MODEL",
                        NUMBER_MODEL_ID,
                        V51NumberModelArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(
                new Component(
                        "EXPERT_GATE",
                        EXPERT_GATE_ID,
                        V51ExpertGateArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(
                new Component(
                        "ACTION_RANKER",
                        RANKER_ID,
                        V51ActionRankerArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(
                new Component(
                        "SELECTED_CALIBRATION",
                        CALIBRATION_ID,
                        V51SelectedCalibrationArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(
                new Component(
                        "PREDICTABILITY",
                        PREDICTABILITY_ID,
                        V51PredictabilityArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(
                new Component(
                        "POLICY_SURFACE",
                        POLICY_ID,
                        V51PolicySurfaceArtifact.DATA_SHA256,
                        false,
                        false));
        out.add(new Component("TRANSPORT", V51BuildManifest.TRANSPORT_SCHEMA, "", true, true));
        out.add(new Component("PARSER", V51BuildManifest.PARSER_SCHEMA, "", true, true));
        out.add(
                new Component(
                        "SECRET_POLICY",
                        V51BuildManifest.SECRET_SCHEMA,
                        // Model identity must not vary with async credential loading/process startup.
                        V51BuildManifest.SECRET_SCHEMA,
                        true,
                        true));
        return Collections.unmodifiableList(out);
    }

    public static List<BundleRequirement> requirements() {
        ArrayList<BundleRequirement> rules = new ArrayList<BundleRequirement>();
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-001", "NUMBER_MODEL", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-002", "EXPERT_GATE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-003", "STATE_PROTOTYPE", "GROUP_01", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-004", "CROSS_POSITION_TENSOR", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-005", "FUNNEL", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-006", "ACTION_RANKER", "GROUP_01", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-007", "SELECTED_CALIBRATION", "GROUP_01", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-008", "WINDOW_CERTIFICATE", "GROUP_01", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-009", "DOMAIN_CERTIFICATE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-010", "POLICY_SURFACE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-011", "PREDICTABILITY", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-012", "SCHEMA", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-013", "PARSER", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-014", "TRANSPORT", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-015", "SECRET_POLICY", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-016", "OWNER_REGISTRY", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-017", "REPLAY_HASH", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-018", "OOS_BOUNDARY", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-019", "TRAIN_RANGE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-020", "VALID_RANGE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-021", "TEST_RANGE", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-022", "PARITY", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-023", "CALIBRATION", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-024", "ABLATION", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-025", "REALTIME_SHADOW", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-026", "ROLLBACK", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-027", "PROMOTION", "GROUP_01", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-028", "NUMBER_MODEL", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-029", "EXPERT_GATE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-030", "STATE_PROTOTYPE", "GROUP_02", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-031", "CROSS_POSITION_TENSOR", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-032", "FUNNEL", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-033", "ACTION_RANKER", "GROUP_02", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-034", "SELECTED_CALIBRATION", "GROUP_02", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-035", "WINDOW_CERTIFICATE", "GROUP_02", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-036", "DOMAIN_CERTIFICATE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-037", "POLICY_SURFACE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-038", "PREDICTABILITY", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-039", "SCHEMA", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-040", "PARSER", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-041", "TRANSPORT", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-042", "SECRET_POLICY", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-043", "OWNER_REGISTRY", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-044", "REPLAY_HASH", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-045", "OOS_BOUNDARY", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-046", "TRAIN_RANGE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-047", "VALID_RANGE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-048", "TEST_RANGE", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-049", "PARITY", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-050", "CALIBRATION", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-051", "ABLATION", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-052", "REALTIME_SHADOW", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-053", "ROLLBACK", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-054", "PROMOTION", "GROUP_02", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-055", "NUMBER_MODEL", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-056", "EXPERT_GATE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-057", "STATE_PROTOTYPE", "GROUP_03", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-058", "CROSS_POSITION_TENSOR", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-059", "FUNNEL", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-060", "ACTION_RANKER", "GROUP_03", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-061", "SELECTED_CALIBRATION", "GROUP_03", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-062", "WINDOW_CERTIFICATE", "GROUP_03", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-063", "DOMAIN_CERTIFICATE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-064", "POLICY_SURFACE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-065", "PREDICTABILITY", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-066", "SCHEMA", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-067", "PARSER", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-068", "TRANSPORT", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-069", "SECRET_POLICY", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-070", "OWNER_REGISTRY", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-071", "REPLAY_HASH", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-072", "OOS_BOUNDARY", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-073", "TRAIN_RANGE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-074", "VALID_RANGE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-075", "TEST_RANGE", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-076", "PARITY", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-077", "CALIBRATION", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-078", "ABLATION", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-079", "REALTIME_SHADOW", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-080", "ROLLBACK", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-081", "PROMOTION", "GROUP_03", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-082", "NUMBER_MODEL", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-083", "EXPERT_GATE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-084", "STATE_PROTOTYPE", "GROUP_04", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-085", "CROSS_POSITION_TENSOR", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-086", "FUNNEL", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-087", "ACTION_RANKER", "GROUP_04", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-088", "SELECTED_CALIBRATION", "GROUP_04", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-089", "WINDOW_CERTIFICATE", "GROUP_04", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-090", "DOMAIN_CERTIFICATE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-091", "POLICY_SURFACE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-092", "PREDICTABILITY", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-093", "SCHEMA", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-094", "PARSER", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-095", "TRANSPORT", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-096", "SECRET_POLICY", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-097", "OWNER_REGISTRY", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-098", "REPLAY_HASH", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-099", "OOS_BOUNDARY", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-100", "TRAIN_RANGE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-101", "VALID_RANGE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-102", "TEST_RANGE", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-103", "PARITY", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-104", "CALIBRATION", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-105", "ABLATION", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-106", "REALTIME_SHADOW", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-107", "ROLLBACK", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-108", "PROMOTION", "GROUP_04", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-109", "NUMBER_MODEL", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-110", "EXPERT_GATE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-111", "STATE_PROTOTYPE", "GROUP_05", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-112", "CROSS_POSITION_TENSOR", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-113", "FUNNEL", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-114", "ACTION_RANKER", "GROUP_05", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-115", "SELECTED_CALIBRATION", "GROUP_05", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-116", "WINDOW_CERTIFICATE", "GROUP_05", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-117", "DOMAIN_CERTIFICATE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-118", "POLICY_SURFACE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-119", "PREDICTABILITY", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-120", "SCHEMA", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-121", "PARSER", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-122", "TRANSPORT", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-123", "SECRET_POLICY", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-124", "OWNER_REGISTRY", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-125", "REPLAY_HASH", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-126", "OOS_BOUNDARY", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-127", "TRAIN_RANGE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-128", "VALID_RANGE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-129", "TEST_RANGE", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-130", "PARITY", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-131", "CALIBRATION", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-132", "ABLATION", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-133", "REALTIME_SHADOW", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-134", "ROLLBACK", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-135", "PROMOTION", "GROUP_05", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-136", "NUMBER_MODEL", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-137", "EXPERT_GATE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-138", "STATE_PROTOTYPE", "GROUP_06", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-139", "CROSS_POSITION_TENSOR", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-140", "FUNNEL", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-141", "ACTION_RANKER", "GROUP_06", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-142", "SELECTED_CALIBRATION", "GROUP_06", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-143", "WINDOW_CERTIFICATE", "GROUP_06", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-144", "DOMAIN_CERTIFICATE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-145", "POLICY_SURFACE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-146", "PREDICTABILITY", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-147", "SCHEMA", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-148", "PARSER", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-149", "TRANSPORT", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-150", "SECRET_POLICY", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-151", "OWNER_REGISTRY", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-152", "REPLAY_HASH", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-153", "OOS_BOUNDARY", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-154", "TRAIN_RANGE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-155", "VALID_RANGE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-156", "TEST_RANGE", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-157", "PARITY", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-158", "CALIBRATION", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-159", "ABLATION", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-160", "REALTIME_SHADOW", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-161", "ROLLBACK", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-162", "PROMOTION", "GROUP_06", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-163", "NUMBER_MODEL", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-164", "EXPERT_GATE", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-165", "STATE_PROTOTYPE", "GROUP_07", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-166", "CROSS_POSITION_TENSOR", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-167", "FUNNEL", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-168", "ACTION_RANKER", "GROUP_07", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-169", "SELECTED_CALIBRATION", "GROUP_07", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-170", "WINDOW_CERTIFICATE", "GROUP_07", true));
        rules.add(
                new BundleRequirement(
                        "TIANSHU-BUNDLE-171", "DOMAIN_CERTIFICATE", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-172", "POLICY_SURFACE", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-173", "PREDICTABILITY", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-174", "SCHEMA", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-175", "PARSER", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-176", "TRANSPORT", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-177", "SECRET_POLICY", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-178", "OWNER_REGISTRY", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-179", "REPLAY_HASH", "GROUP_07", true));
        rules.add(new BundleRequirement("TIANSHU-BUNDLE-180", "OOS_BOUNDARY", "GROUP_07", true));
        return Collections.unmodifiableList(rules);
    }

    public static final String BUNDLE_HASH = calculateBundleHash();

    private static String calculateBundleHash() {
        StringBuilder sb = new StringBuilder();
        List<Component> components = components();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(components.get(i).canonical());
        }
        List<BundleRequirement> requirements = requirements();
        for (int i = 0; i < requirements.size(); i++)
            sb.append('|').append(requirements.get(i).canonical());
        sb.append('|').append(V51BuildManifest.FEATURE_SCHEMA).append('|').append(GAME_CODE);
        return V51ChecksumEngine.sha256(sb.toString());
    }

    public static boolean isInternallyConsistent() {
        if (!GAME_CODE.equals(AppConfig.LOTTERY_CODE)) return false;
        if (NUMBER_MODEL_ID.length() == 0
                || EXPERT_GATE_ID.length() == 0
                || RANKER_ID.length() == 0
                || CALIBRATION_ID.length() == 0
                || PREDICTABILITY_ID.length() == 0
                || POLICY_ID.length() == 0
                || DATA_SHA256.length() == 0) return false;
        return DATA_SHA256.equals(V51ExpertGateArtifact.DATA_SHA256)
                && DATA_SHA256.equals(V51ActionRankerArtifact.DATA_SHA256)
                && DATA_SHA256.equals(V51SelectedCalibrationArtifact.DATA_SHA256)
                && DATA_SHA256.equals(V51PredictabilityArtifact.DATA_SHA256)
                && DATA_SHA256.equals(V51PolicySurfaceArtifact.DATA_SHA256);
    }

    public static boolean mayPromote() {
        return false;
    }

    public static String promotionBlockReason() {
        if (!isInternallyConsistent()) return "BUNDLE_HASH_OR_DATA_MISMATCH";
        if (!TRAINED_FOR_BINANCE) return "BINANCE_ARTIFACTS_NOT_TRAINED";
        if (!PARITY_PROVEN) return "PARITY_NOT_PROVEN";
        if (!OOS_PROVEN) return "STRICT_OOS_NOT_PROVEN";
        if (!REALTIME_PROVEN) return "REALTIME_SHADOW_NOT_PROVEN";
        return "B01_SHADOW_ONLY";
    }
}
