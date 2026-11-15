package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Four-level Source/Semantic/Persistence/Behavior lock for every registered pre-R915 asset. */
public final class LegacyIntegrityGuardR915 {
    public static final String VERSION = "R915_LEGACY_INTEGRITY_V4_1";

    public static final class Result {
        public final boolean passed;
        public final int expected, registered, locked, unchanged, violations;
        public final int mutationAttempts, writePathsDetected, legacyEvolutionEvents;
        public final String sourceDigest, semanticDigest, dbDigest, behaviorDigest;
        public final List<String> reasons;

        Result(
                boolean passed,
                int expected,
                int registered,
                int locked,
                int unchanged,
                int mutationAttempts,
                int writePathsDetected,
                int legacyEvolutionEvents,
                String sourceDigest,
                String semanticDigest,
                String dbDigest,
                String behaviorDigest,
                List<String> reasons) {
            this.passed = passed;
            this.expected = expected;
            this.registered = registered;
            this.locked = locked;
            this.unchanged = unchanged;
            this.mutationAttempts = mutationAttempts;
            this.writePathsDetected = writePathsDetected;
            this.legacyEvolutionEvents = legacyEvolutionEvents;
            this.sourceDigest = safe(sourceDigest);
            this.semanticDigest = safe(semanticDigest);
            this.dbDigest = safe(dbDigest);
            this.behaviorDigest = safe(behaviorDigest);
            this.reasons = java.util.Collections.unmodifiableList(new ArrayList<String>(reasons));
            violations = reasons.size();
        }

        public String canonical() {
            return "LegacyIntegrity=" + (passed ? "PASS" : "FAIL")
                    + "|assets=" + expected + '/' + registered + '/' + locked + '/' + unchanged
                    + "|violations=" + violations
                    + "|mutationAttempts=" + mutationAttempts
                    + "|writePaths=" + writePathsDetected
                    + "|legacyEvolution=" + legacyEvolutionEvents
                    + "|source=" + sourceDigest
                    + "|semantic=" + semanticDigest
                    + "|db=" + dbDigest
                    + "|behavior=" + behaviorDigest
                    + "|reasons=" + reasons;
        }
    }

    private LegacyIntegrityGuardR915() {}

    public static Result verify(DbStore store) {
        ArrayList<String> reasons = new ArrayList<String>();
        LegacyFixedAssetRegistryR915.Entry[] expected = LegacyFixedAssetRegistryR915.entries();
        HashMap<String,Row> actual = new HashMap<String,Row>();
        int locked = 0, unchanged = 0, mutation = 0, writePaths = 0, evolution = 0;
        if (store == null) reasons.add("STORE_NULL");
        else {
            Cursor cursor = null;
            try {
                cursor = store.readable().rawQuery(
                        "SELECT asset_key,name_zh,owner_class,output_schema,consumer,source_digest,"
                                + "semantic_digest,db_contract_digest,baseline_behavior_digest,locked "
                                + "FROM " + AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY
                                + " ORDER BY asset_key", null);
                while (cursor.moveToNext()) {
                    Row row = new Row(
                            cursor.getString(0), cursor.getString(1), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4), cursor.getString(5),
                            cursor.getString(6), cursor.getString(7), cursor.getString(8),
                            cursor.getInt(9));
                    if (actual.put(row.id, row) != null) reasons.add("DUPLICATE:" + row.id);
                    if (row.locked == 1) locked++;
                }
            } catch (Throwable failure) {
                reasons.add("REGISTRY_READ:" + failure.getClass().getSimpleName());
            } finally { if (cursor != null) cursor.close(); }

            for (LegacyFixedAssetRegistryR915.Entry item : expected) {
                Row row = actual.get(item.id);
                if (row == null) { reasons.add("MISSING_LOCK:" + item.id); continue; }
                String semantic = V51ChecksumEngine.sha256("SEMANTIC|" + item.canonical());
                String db = V51ChecksumEngine.sha256("DB_READ_ONLY|" + item.id);
                String behavior = V51ChecksumEngine.sha256("BASELINE|" + item.canonical());
                boolean same = row.locked == 1
                        && item.nameZh.equals(row.name)
                        && item.sourceClass.equals(row.owner)
                        && item.output.equals(row.output)
                        && item.consumer.equals(row.consumer)
                        && item.sourceHash.equals(row.source)
                        && semantic.equals(row.semantic)
                        && db.equals(row.db)
                        && behavior.equals(row.behavior);
                if (same) unchanged++; else reasons.add("CONTRACT_CHANGED:" + item.id);
            }
            for (String id : actual.keySet()) if (!contains(expected, id)) reasons.add("UNDECLARED:" + id);
            evolution = scalar(store, "SELECT COUNT(1) FROM " + AssetGenesisEngineR915.TABLE_EVOLUTION_EVENT
                    + " WHERE entity_type='LEGACY_FIXED_ASSET'");
            if (evolution != 0) reasons.add("LEGACY_EVOLUTION_EVENT:" + evolution);
            mutation = parseInt(store.getState("r915_legacy_mutation_attempts", "0"));
            writePaths = parseInt(store.getState("r915_legacy_write_paths_detected", "0"));
            if (mutation != 0) reasons.add("LEGACY_MUTATION_ATTEMPTS:" + mutation);
            if (writePaths != 0) reasons.add("LEGACY_WRITE_PATHS:" + writePaths);
            int namespace = scalar(store, "SELECT COUNT(1) FROM " + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                    + " WHERE namespace<>'GENESIS_NEW' OR may_authorize_real<>0");
            if (namespace != 0) reasons.add("GENESIS_NAMESPACE_OR_AUTHORITY:" + namespace);
            verifyAtomSchema(store, reasons);
        }
        Result result = new Result(
                reasons.isEmpty(), expected.length, actual.size(), locked, unchanged,
                mutation, writePaths, evolution, digest(actual,"SOURCE"), digest(actual,"SEMANTIC"),
                digest(actual,"DB"), digest(actual,"BEHAVIOR"), reasons);
        if (store != null) try {
            LinkedHashMap<String,String> states = new LinkedHashMap<String,String>();
            states.put("r915_legacy_integrity", result.passed ? "PASS" : "FAIL");
            states.put("r915_legacy_integrity_truth", result.canonical());
            states.put("r915_legacy_integrity_violation_count", String.valueOf(result.violations));
            states.put("r915_legacy_integrity_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(states);
        } catch (Throwable ignored) {}
        return result;
    }

    public static void assertEvolutionTarget(String namespace, String operation) {
        String zone = safe(namespace).toUpperCase(Locale.US);
        String op = safe(operation).toUpperCase(Locale.US);
        if (zone.contains("LEGACY"))
            throw new UnsupportedOperationException("R915_LEGACY_FIXED_READ_ONLY:" + zone + ':' + op);
        if (!"GENESIS_NEW".equals(zone) && !"RESEARCH_ALGORITHM".equals(zone)
                && !"ATOM_PROPOSAL".equals(zone) && !"RUNTIME_POLICY".equals(zone))
            throw new IllegalArgumentException("R915_EVOLUTION_NAMESPACE_UNKNOWN:" + zone);
    }

    public static void rejectMutationAttempt(DbStore store, String assetId, String operation) {
        if (store == null) return;
        int prior = parseInt(store.getState("r915_legacy_mutation_attempts", "0"));
        LinkedHashMap<String,String> states = new LinkedHashMap<String,String>();
        states.put("r915_legacy_mutation_attempts", String.valueOf(prior + 1));
        states.put("r915_legacy_last_rejected_asset", safe(assetId));
        states.put("r915_legacy_last_rejected_operation", safe(operation));
        states.put("r915_legacy_last_rejected_at", String.valueOf(System.currentTimeMillis()));
        store.setStates(states);
    }

    public static boolean selfTest() {
        LegacyFixedAssetRegistryR915.assertImmutableContract();
        boolean rejected = false;
        try { assertEvolutionTarget("LEGACY_FIXED_ASSETS", "MUTATE"); }
        catch (UnsupportedOperationException expected) { rejected = true; }
        assertEvolutionTarget("GENESIS_NEW", "DIRECTED_MUTATION");
        assertEvolutionTarget("RESEARCH_ALGORITHM", "CROSSOVER");
        return rejected
                && LegacyFixedAssetRegistryR915.entries().length == 133
                && LegacyFixedAssetRegistryR915.sourceLockCount() == 133
                && !LegacyFixedAssetRegistryR915.MUTATION_ALLOWED
                && !LegacyFixedAssetRegistryR915.PRUNE_ALLOWED;
    }

    private static int scalar(DbStore store, String sql) {
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery(sql, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Throwable ignored) { return -1; }
        finally { if (cursor != null) cursor.close(); }
    }

    /**
     * Verifies all 240 persisted rows against the signed Java registry, field by field.
     *
     * <p>R920 compared the count of one legal pre-draw subset with the total registry cardinality.
     * A6/A11 intentionally use TRAINED_UNTIL timing and A9 is settlement-only, so that subset can
     * never contain 240 rows.  The false comparison produced the observed permanent
     * YIELD_LEGACY_INTEGRITY_FAIL and stopped genesis generation.  R922 audits the actual contract
     * instead of inferring it from a lossy subset count.</p>
     */
    private static void verifyAtomSchema(DbStore store, List<String> reasons) {
        LinkedHashMap<String,String> actual = new LinkedHashMap<String,String>();
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery(
                    "SELECT atom_id,group_id,name,datatype,value_range,window_spec,max_lag,"
                            + "min_support,leakage_class,update_timing,evidence_stage,cost_class,"
                            + "version,enabled,semantic_digest FROM "
                            + AssetGenesisEngineR915.TABLE_ATOM_SCHEMA + " ORDER BY atom_id", null);
            while (cursor.moveToNext()) {
                String id = safe(cursor.getString(0));
                String row = atomRowCanonical(
                        id, cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getString(5), cursor.getInt(6), cursor.getInt(7),
                        cursor.getString(8), cursor.getString(9), cursor.getString(10),
                        cursor.getString(11), cursor.getInt(12), cursor.getInt(13), cursor.getString(14));
                if (actual.put(id, row) != null) reasons.add("ATOM_SCHEMA_DUPLICATE:" + id);
            }
        } catch (Throwable failure) {
            reasons.add("ATOM_SCHEMA_READ:" + failure.getClass().getSimpleName());
            return;
        } finally { if (cursor != null) cursor.close(); }
        for (String reason : auditAtomRowsForTest(actual)) reasons.add("ATOM_SCHEMA_" + reason);
    }

    /** Package-visible deterministic fixture used by the R922 host regression. */
    static Map<String,String> expectedAtomRowsForTest() {
        LinkedHashMap<String,String> expected = new LinkedHashMap<String,String>();
        for (GenesisSchemaRegistryR915.AtomSpec atom : GenesisSchemaRegistryR915.atoms()) {
            expected.put(atom.id, atomRowCanonical(
                    atom.id, atom.group, atom.name, atom.valueType, atom.valueRange, atom.windowSpec,
                    atom.maxLag, atom.minSupport, atom.leakagePolicy, atom.updateTiming,
                    atom.evidenceStage, atom.costClass, atom.version, 1,
                    V51ChecksumEngine.sha256(atom.canonical())));
        }
        return expected;
    }

    /** Package-visible common audit: an exact registry passes and any row mutation fails closed. */
    static List<String> auditAtomRowsForTest(Map<String,String> actual) {
        ArrayList<String> reasons = new ArrayList<String>();
        Map<String,String> expected = expectedAtomRowsForTest();
        if (actual == null) {
            reasons.add("NULL");
            return reasons;
        }
        if (actual.size() != GenesisSchemaRegistryR915.EXPECTED_ATOMS)
            reasons.add("CARDINALITY:" + actual.size());
        for (Map.Entry<String,String> row : expected.entrySet()) {
            String seen = actual.get(row.getKey());
            if (seen == null) reasons.add("MISSING:" + row.getKey());
            else if (!row.getValue().equals(seen)) reasons.add("CONTRACT_CHANGED:" + row.getKey());
        }
        for (String id : actual.keySet()) if (!expected.containsKey(id)) reasons.add("UNDECLARED:" + id);
        return reasons;
    }

    private static String atomRowCanonical(
            String id, String group, String name, String type, String range, String window,
            int maxLag, int minSupport, String leakage, String timing, String stage, String cost,
            int version, int enabled, String semanticDigest) {
        return safe(id)+'|'+safe(group)+'|'+safe(name)+'|'+safe(type)+'|'+safe(range)+'|'
                +safe(window)+'|'+maxLag+'|'+minSupport+'|'+safe(leakage)+'|'+safe(timing)+'|'
                +safe(stage)+'|'+safe(cost)+'|'+version+'|'+enabled+'|'+safe(semanticDigest);
    }

    private static boolean contains(LegacyFixedAssetRegistryR915.Entry[] rows, String id) {
        for (LegacyFixedAssetRegistryR915.Entry row : rows) if (row.id.equals(id)) return true;
        return false;
    }

    private static String digest(Map<String,Row> rows, String part) {
        ArrayList<String> keys = new ArrayList<String>(rows.keySet());
        java.util.Collections.sort(keys);
        StringBuilder out = new StringBuilder(VERSION).append('|').append(part);
        for (String key : keys) {
            Row row = rows.get(key);
            out.append('|').append(key).append('=');
            if ("SOURCE".equals(part)) out.append(row.source);
            else if ("SEMANTIC".equals(part)) out.append(row.semantic);
            else if ("DB".equals(part)) out.append(row.db);
            else out.append(row.behavior);
        }
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static int parseInt(String value) { try { return Integer.parseInt(safe(value)); } catch (Throwable e) { return 0; } }
    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static final class Row {
        final String id,name,owner,output,consumer,source,semantic,db,behavior;
        final int locked;
        Row(String id,String name,String owner,String output,String consumer,String source,
                String semantic,String db,String behavior,int locked) {
            this.id=safe(id);this.name=safe(name);this.owner=safe(owner);this.output=safe(output);
            this.consumer=safe(consumer);this.source=safe(source);this.semantic=safe(semantic);
            this.db=safe(db);this.behavior=safe(behavior);this.locked=locked;
        }
    }
}
