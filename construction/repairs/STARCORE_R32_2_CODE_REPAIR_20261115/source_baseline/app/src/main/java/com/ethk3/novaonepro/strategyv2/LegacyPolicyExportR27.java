package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** R27-019/020 old-asset import (docs/02 2.4). One pass, idempotent:
 *  1. r974_model_lineage rows are copied into r27_lineage (origin 'R974'); the old table stays read-only.
 *  2. Every R908 genome is classified for reproduction as an R27 policy. R908 genomes are register programs with
 *     persistent per-position/global memory and an outcome-driven update law over R908 profile inputs (X0–X13); the
 *     R27 AST is stateless scoring + the registered bias updater, so such genomes are recorded as
 *     LEGACY_NOT_REPRODUCIBLE with the exact reason — never "converted" into something they are not. Their original
 *     VM remains available to R26 as the F23 teacher where bound.
 *  3. Old learner/network weights: shapes (R943 7→16→8→1, CP12 learners) do not match AtomNetR27; they are recorded
 *     as read-only references and AtomNetR27 starts from historical pre-training instead (seconds, docs/06 6.3).
 * Old future samples are never inherited. */
public final class LegacyPolicyExportR27 {
    private LegacyPolicyExportR27() {}
    static volatile boolean done; static volatile long lastLineageSync;

    static ResearchPoolR27.Outcome step(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        if (done && System.currentTimeMillis() - lastLineageSync < 3_600_000L) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:export complete", 1, 1);
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis(); long n = 0;
        try (Cursor c = w.ro.rawQuery("SELECT child_id,parent_id,parent_role,created_issue FROM r974_model_lineage ORDER BY created_issue LIMIT 20000", null)) {
            while (c.moveToNext()) { b.add("INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin,created_issue) VALUES(?,?,?,?,-1,?,'R974',?)", c.getString(0), c.getString(1), String.valueOf(c.getString(2)), "R974_IMPORT", now, String.valueOf(c.getString(3))); n++; }
        } catch (RuntimeException missingTable) { b.add("INSERT OR REPLACE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES('r974_model_lineage','r974_model_lineage','ABSENT',?,NULL,0,0,?)", "table not present on this database", now); }
        lastLineageSync = now;
        if (done) { RuntimeR27.submit(w, "LEGACY_LINEAGE_SYNC", b); return ResearchPoolR27.Outcome.progress(Math.max(1, n), n); }
        Map<String, Integer> reasons = new LinkedHashMap<>();
        try (Cursor c = w.ro.rawQuery("SELECT genome_id,genome_spec,status FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME + " LIMIT 5000", null)) {
            while (c.moveToNext()) {
                String reason;
                try {
                    AlgorithmGenomeR908 g = AlgorithmGenomeR908.parse(c.getString(1));
                    EnumSet<AlgorithmGenomeR908.Op> ops = EnumSet.noneOf(AlgorithmGenomeR908.Op.class);
                    for (String line : c.getString(1).split("[;\\n]")) for (AlgorithmGenomeR908.Op op : AlgorithmGenomeR908.Op.values()) if (line.contains(op.name())) ops.add(op);
                    boolean memory = ops.contains(AlgorithmGenomeR908.Op.WRITE_S) || ops.contains(AlgorithmGenomeR908.Op.WRITE_G) || ops.contains(AlgorithmGenomeR908.Op.LOAD_S) || ops.contains(AlgorithmGenomeR908.Op.LOAD_G) || ops.contains(AlgorithmGenomeR908.Op.LOAD_PEER);
                    reason = memory ? "STATEFUL_MEMORY_PROGRAM_NOT_EXPRESSIBLE_IN_R27_AST" : "R908_PROFILE_INPUTS_X0_X13_HAVE_NO_BITWISE_R27_LEAF_EQUIVALENT";
                    if (g == null) reason = "PARSE_NULL";
                } catch (RuntimeException parse) { reason = "GENOME_PARSE_FAILED:" + parse.getClass().getSimpleName(); }
                reasons.merge(reason, 1, Integer::sum);
                b.add("INSERT OR IGNORE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES(?,?,'LEGACY_NOT_REPRODUCIBLE',?,NULL,0,0,?)", c.getString(0), DbHelper.TABLE_R908_ALGORITHM_GENOME, reason, now);
                n++;
            }
        } catch (RuntimeException missingTable) { }
        for (String[] ref : new String[][] {{"R943_ATOMIC_NETWORK", AtomicToolWorldR943.MODEL}, {"CP12_FAMILY_LEARNERS", "r967_numeric_training_run"}}) {
            long rows = 0; try (Cursor c = w.ro.rawQuery("SELECT count(*) FROM " + ref[1], null)) { if (c.moveToFirst()) rows = c.getLong(0); } catch (RuntimeException absent) { }
            b.add("INSERT OR REPLACE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES(?,?,'READONLY_REFERENCE',?,NULL,0,0,?)", ref[0], ref[1],
                    "rows=" + rows + "; weight shapes differ from AtomNetR27 (172→32→16→1/10); no warm start; AtomNetR27 pre-trains on history; old future samples not inherited", now);
        }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("importedRows", n); m.put("notReproducibleReasons", new LinkedHashMap<String, Object>(reasons)); m.put("at", now);
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('legacy','export',?,?)", JsonR27.write(m), now);
        RuntimeR27.submit(w, "LEGACY_EXPORT", b); done = true;
        return ResearchPoolR27.Outcome.progress(Math.max(1, n), n);
    }
}
