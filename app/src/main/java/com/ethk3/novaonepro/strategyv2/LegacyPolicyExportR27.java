package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Preserves original executable programs and full state without approximating them as R27 ASTs.
 * A strict state decode is an availability check; golden trajectory and natural qualification are separate.
 * The first immutable program/state export is retained, as are all original tables and teacher assets. */
public final class LegacyPolicyExportR27 {
    private LegacyPolicyExportR27() {}
    static volatile boolean done; static volatile long lastLineageSync;

    static ResearchPoolR27.Outcome step(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        if (done && System.currentTimeMillis() - lastLineageSync < 3_600_000L) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:export complete", 1, 1);
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis(); long n = 0;
        try (Cursor c = w.ro.rawQuery("SELECT child_id,parent_id,parent_role,created_issue FROM r974_model_lineage ORDER BY created_issue LIMIT 20000", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();b.add("INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin,created_issue) VALUES(?,?,?,?,-1,?,'R974',?)", c.getString(0), c.getString(1), String.valueOf(c.getString(2)), "R974_IMPORT", now, String.valueOf(c.getString(3))); n++; }
        } catch (RuntimeException missingTable) { if(!String.valueOf(missingTable.getMessage()).contains("no such table"))throw missingTable;b.add("INSERT OR REPLACE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES('r974_model_lineage','r974_model_lineage','ABSENT',?,NULL,0,0,?)", "table not present on this database", now); }
        if (done) { RuntimeR27.submit(w, "LEGACY_LINEAGE_SYNC", b);lastLineageSync=now;return ResearchPoolR27.Outcome.progress(Math.max(1, n), n); }
        Map<String, Integer> reasons = new LinkedHashMap<>();
        try (Cursor c = w.ro.rawQuery("SELECT genome_id,genome_spec,state_blob,state_updates FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME + " LIMIT 5000", null)) {
            while (c.moveToNext()) {
                CooperativeContinuationR26.poll();String status,reason;String program=c.getString(1),state=c.getString(2);
                try {
                    AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(program);
                    if(g==null)throw new IllegalArgumentException("GENOME_PARSE_NULL");
                    AlgorithmGenomeVmR908.State decoded=AlgorithmGenomeVmR908.State.decodeStrict(state);
                    if(decoded.updates!=c.getLong(3))throw new IllegalArgumentException("STATE_UPDATE_COUNTER_MISMATCH");
                    String programHash=StatsR27.sha256(program),stateHash=StatsR27.sha256(state);
                    status="ORIGINAL_VM_STATE_PRESERVED";
                    Map<String,Object> descriptor=new LinkedHashMap<>();descriptor.put("originalVm","AlgorithmGenomeVmR908");descriptor.put("programHash",programHash);descriptor.put("stateHash",stateHash);descriptor.put("updates",decoded.updates);descriptor.put("sourceOperatorAbi",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);descriptor.put("astConversion","NOT_EQUIVALENT_NOT_ATTEMPTED");descriptor.put("goldenTrajectory","SEPARATE_RECEIPT_REQUIRED");descriptor.put("naturalEvidenceTransferred",false);reason=JsonR27.write(descriptor);
                    b.add("INSERT OR IGNORE INTO r29_legacy_program_snapshot(source_id,program_hash,state_hash,program_spec,state_blob,state_updates,source_abi,exported_at) VALUES(?,?,?,?,?,?,?,?)",c.getString(0),programHash,stateHash,program,state,decoded.updates,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,now);
                } catch (RuntimeException parse) {status="RESTORE_BLOCKED_OR_MISSING_DEPENDENCY";reason="STRICT_ORIGINAL_VM_RESTORE:"+parse.getClass().getSimpleName()+":"+parse.getMessage();}
                reasons.merge(status,1,Integer::sum);
                b.add("INSERT OR IGNORE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES(?,?,?,?,NULL,0,0,?)",c.getString(0),DbHelper.TABLE_R908_ALGORITHM_GENOME,status,reason,now);
                b.add("UPDATE r27_legacy_export SET status=?,reason=?,exported_at=? WHERE source_id=? AND reproduced=0 AND compared=0",status,reason,now,c.getString(0));
                n++;
            }
        } catch (RuntimeException missingTable) { if(!String.valueOf(missingTable.getMessage()).contains("no such table"))throw missingTable;}
        for (String[] ref : new String[][] {{"R943_ATOMIC_NETWORK", AtomicToolWorldR943.MODEL}, {"CP12_FAMILY_LEARNERS", "r967_numeric_training_run"}}) {
            long rows = 0; try (Cursor c = w.ro.rawQuery("SELECT count(*) FROM " + ref[1], null)) { if (c.moveToFirst()) rows = c.getLong(0); } catch (RuntimeException absent) {if(!String.valueOf(absent.getMessage()).contains("no such table"))throw absent;}
            b.add("INSERT OR REPLACE INTO r27_legacy_export(source_id,source_table,status,reason,policy_hash,reproduced,compared,exported_at) VALUES(?,?,'READONLY_REFERENCE',?,NULL,0,0,?)", ref[0], ref[1],
                    "rows=" + rows + "; weight shapes differ from AtomNetR27 (172→32→16→1/10); no warm start; AtomNetR27 pre-trains on history; old future samples not inherited", now);
        }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("importedRows", n); m.put("availabilityStatuses", new LinkedHashMap<String, Object>(reasons)); m.put("at", now);
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('legacy','export',?,?)", JsonR27.write(m), now);
        RuntimeR27.submit(w, "LEGACY_EXPORT", b);done=true;lastLineageSync=now;
        return ResearchPoolR27.Outcome.progress(Math.max(1, n), n);
    }
}
