package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B08-C04: typed contract between a research product and its consumer.
 *
 * <p>A registered consumer string proves nothing; a consumption is valid only when the product is PRODUCED, carries
 * every action receipt (source run, input hash, role, ABI, source revision, consumer version, before/after hash,
 * budget, product hash), matches the expected dataset/source/revision/ABI/role/target/parent, used only TRAIN/SELECT
 * labels available before the decision, really changed the consumer's object (before != after) and was not consumed
 * before under the same key. R11/R14/R18 are formal read-only views: NOT_APPLICABLE_OPTIMIZATION, never an
 * optimisation input and not a generator fault. A product is EFFECTIVE only when the same-budget ablation without
 * the input is measurably worse.</p>
 */
public final class ResearchResultContractR31 {
    private ResearchResultContractR31() {}
    public static final String VERSION = "R31_RESEARCH_RESULT_CONTRACT_V1";
    public enum ProductState { PRODUCED, NOT_APPLICABLE_OPTIMIZATION, WAITING_STUDENT, WAITING_SOURCE, NO_INPUT, INSUFFICIENT, INVALID }
    public enum ConsumptionKind { RESEARCH_ACTION, FORMAL_READ_ONLY, DIAGNOSTIC }
    public static final List<String> REQUIRED_ACTION_RECEIPTS = Collections.unmodifiableList(Arrays.asList(
            "source_run", "input_hash", "role", "abi", "source_revision", "consumer_version", "before_hash", "after_hash", "budget", "product_hash"));

    public static ConsumptionKind consumptionKindOf(String viewId) {
        if(viewId!=null&&viewId.startsWith("MATRIX:")){
            String kind=viewId.substring("MATRIX:".length());
            for(MatrixRegistryR30.Definition definition:MatrixRegistryR30.all())
                if("R930".equals(definition.namespace)&&kind.equals(definition.kind))return ConsumptionKind.RESEARCH_ACTION;
            throw new Rejected("R31_RESULT_UNKNOWN_MATRIX:"+kind);
        }
        switch (viewId) {
            case "R11": case "R14": case "R18": return ConsumptionKind.FORMAL_READ_ONLY;
            default: ResearchMeasureKindR31.fromViewId(viewId); return ConsumptionKind.RESEARCH_ACTION;
        }
    }

    public static final class Rejected extends IllegalArgumentException { public final String code; Rejected(String code) { super(code); this.code = code; } }

    /** One typed product. `receipts` holds the action receipt fields; latestLabelOrdinal is the newest label read. */
    public static final class Result {
        public final String viewId, datasetHash, abi, role, target, parentHash; public final long sourceRevision, latestLabelOrdinal; public final ProductState state;
        public final Map<String, Object> receipts;
        public Result(String viewId, ProductState state, String datasetHash, long sourceRevision, String abi, String role, String target, String parentHash, long latestLabelOrdinal, Map<String, Object> receipts) {
            this.viewId = viewId; this.state = state; this.datasetHash = datasetHash; this.sourceRevision = sourceRevision; this.abi = abi; this.role = role; this.target = target;
            this.parentHash = parentHash; this.latestLabelOrdinal = latestLabelOrdinal; this.receipts = receipts == null ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(receipts));
        }
        public String consumeKey(String consumer, String child) { return StatsR27.sha256(VERSION + "|" + receipts.get("source_run") + "|" + consumer + "|" + child + "|" + receipts.get("product_hash")); }
    }
    /** What the consumer expects (its own dataset, revision, ABI, target and parent). */
    public static final class Expect {
        public final String datasetHash, abi, target, parentHash; public final long sourceRevision, decisionOrdinal;
        public Expect(String datasetHash, long sourceRevision, String abi, String target, String parentHash, long decisionOrdinal) {
            this.datasetHash = datasetHash; this.sourceRevision = sourceRevision; this.abi = abi; this.target = target; this.parentHash = parentHash; this.decisionOrdinal = decisionOrdinal;
        }
    }

    /** Identity of a product against its consumer. */
    public static void validateIdentity(Result r, Expect e) {
        if (!Objects.equals(r.datasetHash, e.datasetHash)) throw new Rejected("R31_RESULT_DATASET");
        if (r.sourceRevision != e.sourceRevision) throw new Rejected("R31_RESULT_SOURCE_REVISION");
        if (!Objects.equals(r.abi, e.abi)) throw new Rejected("R31_RESULT_ABI");
        if (!Objects.equals(r.target, e.target)) throw new Rejected("R31_RESULT_TARGET");
        if (!Objects.equals(r.parentHash, e.parentHash)) throw new Rejected("R31_RESULT_PARENT");
    }

    /** Full check before a research action consumes a product; returns the consume key to record in the same transaction. */
    public static String validateForConsumption(Result r, Expect e, String consumer, String child, Set<String> consumedKeys) {
        if (consumptionKindOf(r.viewId) == ConsumptionKind.FORMAL_READ_ONLY) throw new Rejected("R31_RESULT_NOT_APPLICABLE_OPTIMIZATION:" + r.viewId);
        if (r.state != ProductState.PRODUCED) throw new Rejected("R31_RESULT_STATE:" + r.state);
        for (String k : REQUIRED_ACTION_RECEIPTS) { Object v = r.receipts.get(k); if (v == null || String.valueOf(v).isEmpty()) throw new Rejected("R31_RESULT_RECEIPT_MISSING:" + k); }
        if (!"TRAIN".equals(r.role) && !"SELECT".equals(r.role)) throw new Rejected("R31_RESULT_FORMAL_ROLE:" + r.role);
        if (!r.role.equals(String.valueOf(r.receipts.get("role")))) throw new Rejected("R31_RESULT_ROLE_RECEIPT");
        validateIdentity(r, e);
        if(!Objects.equals(r.abi,r.receipts.get("abi")))throw new Rejected("R31_RESULT_ABI_RECEIPT");
        Object revision=r.receipts.get("source_revision"),budget=r.receipts.get("budget");
        if(!(revision instanceof Number)||((Number)revision).longValue()!=r.sourceRevision)throw new Rejected("R31_RESULT_REVISION_RECEIPT");
        if(!(budget instanceof Number)||!Double.isFinite(((Number)budget).doubleValue())||((Number)budget).doubleValue()<=0)throw new Rejected("R31_RESULT_BUDGET");
        if (r.latestLabelOrdinal >= e.decisionOrdinal) throw new Rejected("R31_RESULT_LATE_LABEL:" + r.latestLabelOrdinal + ">=" + e.decisionOrdinal);
        if (String.valueOf(r.receipts.get("before_hash")).equals(String.valueOf(r.receipts.get("after_hash")))) throw new Rejected("R31_RESULT_NO_EFFECTIVE_CHANGE");
        String key = r.consumeKey(consumer, child);
        if (consumedKeys != null && consumedKeys.contains(key)) throw new Rejected("R31_RESULT_DUPLICATE_CONSUME");
        return key;
    }

    /** Same-budget ablation verdict: EFFECTIVE only if the run with the input beats the run without it beyond noise. */
    public static String effectiveness(ResearchGainR30R1.Estimate withInputMinusWithout, long budgetWith, long budgetWithout) {
        if (budgetWith != budgetWithout) return "NOT_COMPARABLE_DIFFERENT_BUDGET";
        if (withInputMinusWithout == null || !withInputMinusWithout.supported()) return "INSUFFICIENT_SUPPORT";
        return withInputMinusWithout.lower95 > 0 ? "EFFECTIVE" : "NOT_SHOWN_EFFECTIVE";
    }
}
