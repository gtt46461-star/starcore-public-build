package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B08-C05: typed object projection of a stored research measure. ResearchObservatoryR30R1.product writes the
 * observatory rows with top-level mean_gain/lower95/upper95 = NULL; the object-level measured values live in
 * payload_json.frame.points. This projects value/lower/upper/n and the node cost of each object together with the
 * hash of the original payload. A NULL stays NULL (never 0), an unknown per-object CPU stays UNKNOWN, and a unit or
 * kind that does not match its schema is refused rather than plotted.
 */
public final class ResearchMeasureProjectionR31 {
    private ResearchMeasureProjectionR31() {}
    public static final String SCHEMA = "R30_REPAIR_OBSERVATORY_SOURCE_V1", UNKNOWN = "UNKNOWN";
    public static final String NODE_COST_SCOPE = "AST_NODE_COMPLEXITY_ONLY_CPU_UNKNOWN_NOT_POOL_CPU";

    public static final class ObjectRow {
        public final String label, parentHash, childHash, operator, originalSha256; public final Double value, lower, upper; public final long n;
        public final Long nodeCost; public final Object perObjectCpuNs; public final ResearchMeasureKindR31 kind; public final String unit;
        ObjectRow(ResearchMeasureKindR31 kind, String label, Double value, Double lower, Double upper, long n, Long nodeCost, Object cpu, String parent, String child, String operator, String sha) {
            this.kind = kind; this.unit = kind.unit; this.label = label; this.value = value; this.lower = lower; this.upper = upper; this.n = n; this.nodeCost = nodeCost;
            this.perObjectCpuNs = cpu; this.parentHash = parent; this.childHash = child; this.operator = operator; this.originalSha256 = sha;
        }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", kind.name()); m.put("unit", unit); m.put("label", label); m.put("value", value); m.put("lower", lower); m.put("upper", upper); m.put("n", n);
            m.put("nodeCost", nodeCost); m.put("perObjectCpuNs", perObjectCpuNs); m.put("parentHash", parentHash); m.put("childHash", childHash); m.put("operator", operator);
            m.put("originalPayloadSha256", originalSha256);
            return m;
        }
    }

    static Double number(Object o, String field) {
        if (o == null) return null;
        if (!(o instanceof Number)) throw new IllegalArgumentException("R31_PROJECTION_NOT_NUMBER:" + field);
        double d = ((Number) o).doubleValue(); if (!Double.isFinite(d)) throw new IllegalArgumentException("R31_PROJECTION_NONFINITE:" + field);
        return d;
    }

    /** Object rows of one stored measure (kind column + payload_json). */
    @SuppressWarnings("unchecked")
    public static List<ObjectRow> projectObjects(String kindColumn, String payloadJson) {
        ResearchMeasureKindR31 kind = ResearchMeasureKindR31.parseCanonical(kindColumn);
        Object parsed = JsonR27.parse(payloadJson);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("R31_PROJECTION_PAYLOAD");
        Map<String, Object> source = (Map<String, Object>) parsed;
        if (kind.viewId != null && !kind.viewId.equals(source.get("viewId"))) throw new IllegalArgumentException("R31_PROJECTION_VIEW_KIND_MISMATCH:" + source.get("viewId") + "/" + kind);
        if (!(source.get("frame") instanceof Map)) throw new IllegalArgumentException("R31_PROJECTION_SCHEMA:NO_FRAME");
        Object points = ((Map<String, Object>) source.get("frame")).get("points");
        if (!(points instanceof List)) throw new IllegalArgumentException("R31_PROJECTION_SCHEMA:NO_POINTS");
        String sha = StatsR27.sha256(payloadJson); List<ObjectRow> out = new ArrayList<>();
        for (Object o : (List<Object>) points) {
            if (!(o instanceof Map)) throw new IllegalArgumentException("R31_PROJECTION_POINT");
            Map<String, Object> p = (Map<String, Object>) o;
            Long nodeCost = p.get("nodeCost") == null ? null : ((Number) p.get("nodeCost")).longValue();
            Object cpu = p.get("perObjectCpuNs") instanceof Number ? (Object) ((Number) p.get("perObjectCpuNs")).longValue() : UNKNOWN;
            long n = p.get("n") instanceof Number ? ((Number) p.get("n")).longValue() : 0;
            out.add(new ObjectRow(kind, String.valueOf(p.get("label")), number(p.get("value"), "value"), number(p.get("lower"), "lower"), number(p.get("upper"), "upper"), n, nodeCost, cpu,
                    p.get("parentHash") instanceof String ? (String) p.get("parentHash") : null, p.get("childHash") instanceof String ? (String) p.get("childHash") : null,
                    p.get("operator") instanceof String ? (String) p.get("operator") : null, sha));
            validateUnits(kind, p);
        }
        return Collections.unmodifiableList(out);
    }
    /** R10 rows: paired gain per issue with AST node cost; CPU per object stays UNKNOWN until a CPU receipt exists. */
    public static List<ObjectRow> operatorGainCost(String kindColumn, String payloadJson) {
        if (!ResearchMeasureKindR31.OPERATOR_GAIN_COST.name().equals(kindColumn)) throw new IllegalArgumentException("R31_PROJECTION_NOT_OPERATOR_GAIN_COST:" + kindColumn);
        return projectObjects(kindColumn, payloadJson);
    }
    /** Units of a point must be the ones its kind declares. */
    public static void validateUnits(ResearchMeasureKindR31 kind, Map<String, Object> point) {
        Object declared = point.get("unit");
        if (declared != null && !kind.unit.equals(declared)) throw new IllegalArgumentException("R31_PROJECTION_UNIT_MISMATCH:" + declared + "/" + kind.unit);
        if (kind == ResearchMeasureKindR31.OPERATOR_GAIN_COST) {
            if (!NODE_COST_SCOPE.equals(point.get("costScope"))) throw new IllegalArgumentException("R31_PROJECTION_UNIT_MISMATCH:costScope=" + point.get("costScope"));
            if (!(point.get("nodeCost") instanceof Number) || ((Number) point.get("nodeCost")).longValue() < 1) throw new IllegalArgumentException("R31_PROJECTION_NODE_COST");
            if (point.get("perObjectCpuNs") != null && !(point.get("perObjectCpuNs") instanceof Number)) throw new IllegalArgumentException("R31_PROJECTION_CPU_UNIT");
        }
    }
}
