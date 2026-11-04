package com.ethk3.novaonepro.strategyv2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only engineering counterexample. Synthetic rows are never quality evidence. */
public final class FamilyStreamIsolationR31Host {
    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    static Object stream(FeatureCacheR27 c, int family) throws Exception {
        return ((Map<?, ?>) field(c, "streams")).get(family);
    }
    static String sha(byte[] b) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(b); StringBuilder s = new StringBuilder();
        for (byte v : h) s.append(String.format(java.util.Locale.ROOT, "%02x", v & 255)); return s.toString();
    }
    static String modelHash(Object stream) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ((OnlineModelsSU2.Model) field(stream, "model")).write(new DataOutputStream(bytes));
        return sha(bytes.toByteArray());
    }
    static Map<String, Object> state(Object stream) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("identity", System.identityHashCode(stream));
        for (String f : Arrays.asList("fed", "lastFedOrdinal", "checkedFed", "checkedHash")) out.put(f, field(stream, f));
        out.put("modelIdentity", System.identityHashCode(field(stream, "model")));
        out.put("stateIdentity", System.identityHashCode(field(stream, "st")));
        out.put("predictionScratchIdentity", System.identityHashCode(field(stream, "p")));
        out.put("valuesIdentity", System.identityHashCode(field(stream, "values")));
        out.put("valuesLength", ((double[]) field(stream, "values")).length);
        ReplayDatasetR27 verified = (ReplayDatasetR27) field(stream, "verifiedData");
        out.put("verifiedDataIdentity", verified == null ? null : System.identityHashCode(verified));
        out.put("verifiedDataHash", verified == null ? null : verified.datasetHash);
        out.put("modelStateSha256", modelHash(stream));
        return out;
    }
    static Map<String, Object> compare(double[] actual, double[] expected) {
        long mismatches = Math.abs((long) actual.length - expected.length); int first = -1;
        for (int i = 0; i < Math.min(actual.length, expected.length); i++) {
            if (Double.doubleToLongBits(actual[i]) != Double.doubleToLongBits(expected[i])) { mismatches++; if (first < 0) first = i; }
        }
        Map<String, Object> out = new LinkedHashMap<>(); out.put("actualValues", actual.length);
        out.put("expectedValues", expected.length); out.put("bitMismatches", mismatches); out.put("firstMismatch", first); return out;
    }
    static ReplayDatasetR27 prefix(ReplayDatasetR27 full, int count) {
        List<ReplayDatasetR27.Row> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) rows.add(new ReplayDatasetR27.Row(full.issues[i], full.ordinals[i], full.draws[i].clone()));
        return ReplayDatasetR27.of(rows);
    }
    static Map<String, Object> run(int family) throws Exception {
        ReplayDatasetR27 full = ReplayDatasetR27.synthetic(251, 31L, new int[0]);
        ReplayDatasetR27 oldData = prefix(full, 250);
        ReplayDatasetR27 newData = oldData.append(new ReplayDatasetR27.Row(full.issues[250], full.ordinals[250], full.draws[250].clone()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("family", family); out.put("oldDatasetHash", oldData.datasetHash); out.put("newDatasetHash", newData.datasetHash);
        out.put("oldRows", oldData.size()); out.put("newRows", newData.size()); out.put("rejectedRows", oldData.rejectedRows + newData.rejectedRows);
        out.put("inputRole", "SYNTHETIC_ENGINEERING_FIXTURE_NOT_SHADOW_REPLAY_FUTURE_REAL_QUALITY_EVIDENCE");
        try (FeatureCacheR27 older = new FeatureCacheR27(oldData); FeatureCacheR27 child = new FeatureCacheR27(newData);
             FeatureCacheR27 independentOld = new FeatureCacheR27(oldData); FeatureCacheR27 independentChild = new FeatureCacheR27(newData)) {
            double[] oldWarm = older.leaf("family", 1, family, 0);
            Object original = stream(older, family);
            Map<String, Object> before = state(original); out.put("olderBeforeExtend", before);
            child.extendFrom(older);
            Object inherited = stream(child, family);
            Map<String, Object> aliases = new LinkedHashMap<>(); aliases.put("familyStream", original == inherited);
            for (String f : Arrays.asList("model", "st", "p", "values", "lastFedDraw", "verifiedData")) aliases.put(f, field(original, f) == field(inherited, f));
            out.put("aliasesImmediatelyAfterExtend", aliases);
            out.put("childPosition1VsIndependent", compare(child.leaf("family", 1, family, 0), independentChild.leaf("family", 1, family, 0)));
            Map<String, Object> afterChild = state(stream(older, family)); out.put("olderAfterOnlyChildWasRead", afterChild);
            out.put("olderOwnDataIdentityUnchanged", older.data == oldData);
            out.put("olderStreamNowVerifiesChildDataset", field(stream(older, family), "verifiedData") == newData);
            out.put("olderModelMutatedByChild", !before.get("modelStateSha256").equals(afterChild.get("modelStateSha256")));
            out.put("olderCachedPosition1RetainsSameArray", older.leaf("family", 1, family, 0) == oldWarm);
            long beforeRebuild = FeatureCacheR27.REBUILDS_R30.get();
            int beforeEvents = FeatureCacheR27.REBUILD_EVENTS_R30.size();
            out.put("olderUncachedPosition2VsIndependent", compare(older.leaf("family", 2, family, 0), independentOld.leaf("family", 2, family, 0)));
            out.put("oldReadRebuildDelta", FeatureCacheR27.REBUILDS_R30.get() - beforeRebuild);
            List<Object> added = new ArrayList<>(); int n = 0;
            for (Object[] event : FeatureCacheR27.REBUILD_EVENTS_R30) if (n++ >= beforeEvents) added.add(Arrays.asList(event));
            out.put("oldReadAddedRebuildEvents", added);
            out.put("oldReadReplacedPreviouslyAliasedStream", stream(older, family) != original);
            List<Object> allPositions = new ArrayList<>();
            for (int position = 1; position <= 10; position++) {
                Map<String, Object> one = new LinkedHashMap<>(); one.put("position", position);
                one.put("child", compare(child.leaf("family", position, family, 0), independentChild.leaf("family", position, family, 0)));
                one.put("older", compare(older.leaf("family", position, family, 0), independentOld.leaf("family", position, family, 0)));
                allPositions.add(one);
            }
            out.put("allTenPositionsVsIndependent", allPositions);
            out.put("olderAfterItsRead", state(stream(older, family)));
            out.put("childAfterOlderRead", state(stream(child, family)));
            out.put("contract", original == inherited && (Boolean) out.get("olderModelMutatedByChild")
                    && ((Long) out.get("oldReadRebuildDelta")) > 0 ? "MUTABLE_ISOLATION_COUNTEREXAMPLE_REPRODUCED" : "NOT_REPRODUCED");
        }
        return out;
    }
    static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Map<?, ?>) {
            StringBuilder s = new StringBuilder("{"); boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) { if (!first) s.append(','); first = false; s.append(json(e.getKey().toString())).append(':').append(json(e.getValue())); }
            return s.append('}').toString();
        }
        if (value instanceof Iterable<?>) {
            StringBuilder s = new StringBuilder("["); boolean first = true;
            for (Object v : (Iterable<?>) value) { if (!first) s.append(','); first = false; s.append(json(v)); } return s.append(']').toString();
        }
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return '"' + value.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + '"';
    }
    public static void main(String[] args) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("scope", "READ_ONLY_HOST_PUBLIC_FAMILY_LEAF_SYNTHETIC_ENGINEERING_NOT_NATURAL_QUALITY");
        Class<?> identity = Class.forName("com.ethk3.novaonepro.BuildConfig");
        result.put("actualCompiledCommit", identity.getField("SOURCE_COMMIT_R26").get(null));
        result.put("actualCompiledDirty", identity.getField("SOURCE_DIRTY_R31").get(null));
        result.put("featureClassResource", FeatureCacheR27.class.getResource("FeatureCacheR27.class").toString());
        result.put("featureClassSha256", sha(FeatureCacheR27.class.getResourceAsStream("FeatureCacheR27.class").readAllBytes()));
        List<Object> cases = new ArrayList<>(); for (int f : new int[] {8, 11, 12, 20, 21}) cases.add(run(f)); result.put("cases", cases);
        System.out.println(json(result));
    }
}
