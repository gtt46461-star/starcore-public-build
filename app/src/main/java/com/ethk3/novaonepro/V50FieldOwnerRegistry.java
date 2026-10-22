package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Canonical V50 field ownership contract. Duplicate ownership is a hard startup error. */
public final class V50FieldOwnerRegistry {
    public static final class Entry {
        public final String field;
        public final String owner;
        public final String unit;
        public final String description;

        Entry(String field, String owner, String unit, String description) {
            this.field = field;
            this.owner = owner;
            this.unit = unit;
            this.description = description;
        }
    }

    private static final ArrayList<Entry> ENTRIES = new ArrayList<Entry>();

    static {
        add(
                "PredictionContext",
                "TargetIssueResolver",
                "object",
                "single canonical target issue context");
        add(
                "NumberFeatureSnapshot",
                V50BuildManifest.NUMBER_FEATURE_OWNER,
                "object",
                "100 position-number feature rows");
        add(
                "StateTrajectorySnapshot",
                V50BuildManifest.STATE_OWNER,
                "object",
                "state prototypes lifecycle and hazard");
        add(
                "NumberP",
                V50BuildManifest.NUMBER_PROBABILITY_OWNER,
                "probability",
                "10x10 mutually exclusive number distribution");
        add(
                "RawActionP",
                V50BuildManifest.ACTION_BUILDER_OWNER,
                "probability",
                "sum of the three selected NumberP values");
        add(
                "RankScore",
                V50BuildManifest.ACTION_RANK_OWNER,
                "score",
                "within-issue learned position rank score");
        add(
                "RankAdjustedP",
                V50BuildManifest.ACTION_RANK_OWNER,
                "probability",
                "rank-model action probability before selection calibration");
        add(
                "SelectedHitP",
                V50BuildManifest.SELECTED_PROBABILITY_OWNER,
                "probability",
                "selection-adjusted probability for final Rank1 action");
        add(
                "DomainCertificate",
                V50BuildManifest.DOMAIN_CERTIFICATE_OWNER,
                "object",
                "verified domain evidence and risk status");
        add("Lane", V50BuildManifest.LANE_OWNER, "enum", "CORE/DYNAMIC/PROBE/SHADOW/HOLD");
        add(
                "AuthorizationSignature",
                V50BuildManifest.AUTHORIZATION_OWNER,
                "sha256",
                "signature over immutable action contract");
        add(
                "DecisionAction",
                V50BuildManifest.BET_OWNER,
                "enum",
                "BET/SHADOW/HOLD; only DecisionGateEngine may set BET");
        add(
                "SettlementResult",
                V50BuildManifest.SETTLEMENT_OWNER,
                "enum",
                "strict single-position K3 WIN/MISS");
    }

    private V50FieldOwnerRegistry() {}

    private static void add(String field, String owner, String unit, String description) {
        ENTRIES.add(new Entry(field, owner, unit, description));
    }

    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static String ownerOf(String field) {
        if (field == null) return "";
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry e = ENTRIES.get(i);
            if (field.equals(e.field)) return e.owner;
        }
        return "";
    }

    public static void verifyUniqueOwners() {
        HashSet<String> fields = new HashSet<String>();
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry e = ENTRIES.get(i);
            if (e.field == null || e.field.length() == 0) {
                throw new IllegalStateException("V50_EMPTY_FIELD_OWNER_ENTRY");
            }
            if (e.owner == null || e.owner.length() == 0) {
                throw new IllegalStateException("V50_EMPTY_OWNER:" + e.field);
            }
            if (!fields.add(e.field)) {
                throw new IllegalStateException("V50_DUPLICATE_FIELD_OWNER:" + e.field);
            }
        }
    }
}
