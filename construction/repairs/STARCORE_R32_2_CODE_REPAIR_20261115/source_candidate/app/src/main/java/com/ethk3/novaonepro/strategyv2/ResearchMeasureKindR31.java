package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B08-C05: the one enumeration of research measure kinds shared by producers (ResearchGraphSpecsR30R1, the
 * observatory, matrix and study tasks) and readers (ResearchTableAdaptersR30R1). R10 writes OPERATOR_GAIN_COST, so T09
 * reads OPERATOR_GAIN_COST; the pre-R31 names OPERATOR_COST / PARENT_CHILD_GAIN were never written by a current
 * producer and are readable only as HISTORICAL rows, never as current evidence.
 */
public enum ResearchMeasureKindR31 {
    STRUCTURE_STATE("R01", "CONDITIONAL_GAIN_PER_ISSUE"), STRUCTURE_FORECAST_ERROR("R02", "FORECAST_ERROR_PER_ISSUE"),
    POSITION_COMPATIBILITY("R03", "ASSOCIATION_EXCESS"), SET_INTERSECTION_SUPPORT("R04", "CANDIDATE_COUNT"),
    FAILURE_PRESCRIPTION("R05", "RECEIPT_COUNT"), ATOM_PAIRED_GAIN("R06", "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"),
    MOLECULE_SYNERGY("R07", "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"), BEHAVIOR_DIVERSITY("R08", "BEHAVIOR_DISTANCE"),
    EXPERT_EXECUTABLE_CREDIT("R09", "RECEIPT_COUNT"), OPERATOR_GAIN_COST("R10", "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE|AST_NODES"),
    PARENT_CHILD_PAIRED("R11", "FORMAL_READ_ONLY_VERDICT"), FUSION_COMPLEMENTARITY("R12", "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"),
    CALIBRATION_SUPPORT("R13", "RELIABILITY_PROBABILITY"), MODEL_CONSUMPTION_CHAIN("R14", "RECEIPT_COUNT"),
    DRIFT_REPLAY_VALUE("R15", "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"), RESEARCH_BUDGET("R16", "SHARE_OF_RESEARCH_BUDGET"),
    FAILED_MECHANISM_MEMORY("R17", "RECEIPT_COUNT"), END_TO_END_RECEIPTS("R18", "RECEIPT_COUNT"),
    // machine kinds written by research tasks rather than one observatory view
    FAILURE_MEMORY(null, "RECEIPT_COUNT"), MODEL_PAIR_FROZEN(null, "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"),
    MODEL_PAIR_SETTLED(null, "PAIRED_TEN_CLASS_PROPER_GAIN_PER_ISSUE"), RESEARCH_SOURCE_CONSUMPTION(null, "RECEIPT_COUNT");

    public final String viewId, unit;
    ResearchMeasureKindR31(String viewId, String unit) { this.viewId = viewId; this.unit = unit; }

    /** Pre-R31 names: readable for historical layering only. */
    public static final Map<String, ResearchMeasureKindR31> HISTORICAL;
    static { Map<String, ResearchMeasureKindR31> h = new LinkedHashMap<>(); h.put("OPERATOR_COST", OPERATOR_GAIN_COST); h.put("PARENT_CHILD_GAIN", PARENT_CHILD_PAIRED); HISTORICAL = Collections.unmodifiableMap(h); }

    /** The measure produced by an observatory view (R01..R18). */
    public static ResearchMeasureKindR31 fromViewId(String viewId) {
        for (ResearchMeasureKindR31 k : values()) if (viewId != null && viewId.equals(k.viewId)) return k;
        throw new IllegalArgumentException("R31_RESEARCH_VIEW_UNKNOWN:" + viewId);
    }
    /** A current canonical kind; historical or unknown names are refused (never mapped to 0 or to current evidence). */
    public static ResearchMeasureKindR31 parseCanonical(String kind) {
        if (kind != null && HISTORICAL.containsKey(kind)) throw new IllegalArgumentException("R31_RESEARCH_KIND_HISTORICAL:" + kind);
        try { return valueOf(kind); } catch (IllegalArgumentException | NullPointerException unknown) { throw new IllegalArgumentException("R31_RESEARCH_KIND_UNKNOWN:" + kind); }
    }
    /** Historical layering: a pre-R31 name is labelled HISTORICAL with its successor; a current name is CURRENT. */
    public static String normalizeHistoricalKind(String kind) {
        if (kind != null && HISTORICAL.containsKey(kind)) return "HISTORICAL:" + kind + "->" + HISTORICAL.get(kind).name();
        return "CURRENT:" + parseCanonical(kind).name();
    }
    /** SQL literal list of the given kinds (only enum names, so no injection surface). */
    public static String sqlList(ResearchMeasureKindR31... kinds) {
        StringBuilder b = new StringBuilder();
        for (ResearchMeasureKindR31 k : kinds) { if (b.length() > 0) b.append(','); b.append('\'').append(k.name()).append('\''); }
        return b.toString();
    }
}
