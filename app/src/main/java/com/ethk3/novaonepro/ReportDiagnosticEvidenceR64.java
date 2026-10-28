package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Compact evidence derived only from the same immutable snapshot as the 96 checks. */
public final class ReportDiagnosticEvidenceR64 {
    private ReportDiagnosticEvidenceR64() {}
    public static File write(SQLiteDatabase snapshot, File job, String hash,
            ReportNativeCP4.Control control) throws Exception {
        if (!snapshot.isReadOnly()) throw new IOException("R64_REPORT_READONLY_REQUIRED");
        File result = new File(job, "DIAGNOSTIC_SCOPE.json");
        try (FileOutputStream fd = new FileOutputStream(result);
             Writer out = new BufferedWriter(new OutputStreamWriter(fd, StandardCharsets.UTF_8))) {
            out.write("{\"profile\":\"COMPLETE_DIAGNOSTIC_R70\",\"snapshotSha256\":");
            out.write(JSONObject.quote(hash));
            out.write(",\"evidenceRevision\":\"R70_CALIBRATION_RETENTION_RUNTIME_METADATA_V3\",\"rawDatabaseIncluded\":false,\"allChecks\":96,"
                    + "\"coverage\":\"All checks use the full immutable database. All schema objects, table counts, grouped task states and current construction ledgers are retained."
                    + " Static calls are not proof of runtime execution. Selected research and period metadata include every row and every text column without truncation; binary cells are described by byte length and existing registry hashes. Raw payloads are not required to evaluate these checks.\","
                    + "\"omitted\":[\"APK binary\",\"raw SQLite\",\"training/model/retention payloads\",\"historical duplicate ledgers\"],\"tables\":[");
            boolean first = true;
            try (Cursor names = snapshot.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null)) {
                while (names.moveToNext()) {
                    checkpoint(control);
                    String table = names.getString(0);
                    if (!first) out.write(','); first = false;
                    JSONObject row = new JSONObject().put("table", table);
                    try (Cursor n = snapshot.rawQuery("SELECT COUNT(*) FROM " + ident(table), null)) {
                        if (!n.moveToFirst()) throw new IOException("R64_REPORT_COUNT_MISSING");
                        row.put("rows", n.getLong(0));
                    }
                    org.json.JSONArray schema = new org.json.JSONArray();
                    java.util.ArrayList<String> groupColumns = new java.util.ArrayList<>();
                    try (Cursor columns = snapshot.rawQuery("PRAGMA table_info(" + ident(table) + ")", null)) {
                        while (columns.moveToNext()) {
                            String column = columns.getString(1);
                            schema.put(new JSONObject().put("name",column).put("type",columns.getString(2))
                                    .put("notNull",columns.getInt(3)!=0).put("primaryKeyOrder",columns.getInt(5)));
                            if (!column.matches("state|status|kind|stage|settled|terminal_code|failure_class")) continue;
                            groupColumns.add(column);
                        }
                    }
                    row.put("columns",schema);
                    String header=row.toString();out.write(header.substring(0,header.length()-1));out.write(",\"states\":[");
                    boolean precedingGroups=false;
                    for(String column:groupColumns)if(ReportMetadataStreamR81.writeGroups(snapshot,table,column,out,control,precedingGroups)>0)precedingGroups=true;
                    out.write("]}");
                }
            }
            out.write("],\"recentEvidence\":["); first = true;
            for (String table : new String[]{DbHelper.TABLE_DRAWS, RuntimeEventLedgerR926.TABLE_EVENT,
                    RuntimeEventLedgerR926.TABLE_PROJECTION, SchemaRevisionR925.TABLE_FETCH_LEDGER,
                    ResearchPlatformStoreR929.TABLE_BUNDLE, "research_training_input_v2",
                    "research_training_attachment_cp12", "research_evidence_archive_v2",
                    "research_evidence_archive_member_v2", "research_evidence_locator_v2",
                    "research_atom_spec_v2", "research_atom_binding_v2", "research_atom_admission_cp12",
                    "research_matrix_spec_cp12", "research_matrix_regime5_cp12",
                    "r952_matrix_snapshot", "r952_matrix_feedback", "r952_runtime_event",
                    "research_capture_gap_v2", "research_capture_reservation_v2",
                    "v57_runtime_event_outbox_r926", "v57_runtime_epoch_r925",
                    "v57_draw_canonical_r811", "v57_draw_provenance_r811",
                    "v57_shadow_observation_r35", "v57_settlement_task_r36",
                    "v57_schema_domain_r926", "v57_schema_migration_journal_r926",
                    "v57_research_task_r967", "v57_research_lane_cursor_r967", "r952_research_outbox", "r952_candidate_funnel",
                    "research_atom_catalog_import_cp12", "research_molecule_import_cp12",
                    "research_molecule_import_checkpoint_cp12", "research_family_cursor_cp12",
                    "research_family_creation_cp12", "r952_candidate", "research_training_protocol_v2",
                    "research_training_feature_cursor_cp12", "research_action_v2",
                    "research_matrix_checkpoint_v2", "research_matrix_terminal_cp12",
                    "research_task_checkpoint_v2", "research_development_receipt_v2",
                    "research_artifact_identity_v2", "r967_frozen_evaluation_v2",
                    "r952_learning_cursor", "r952_artifact", "r952_candidate_predraw",
                    "r958_frozen_candidate", "r958_frozen_predraw",
                    MultiHorizonProbeR80.INPUT, MultiHorizonProbeR80.FORECAST, MultiHorizonProbeR80.LABEL, MultiHorizonProbeR80.OPPORTUNITY, MultiHorizonProbeR80.DIAGNOSTIC_REF,
                    "research_bound_cohort_r80", "research_bound_parent_r80", "research_bound_source_r80", "research_bound_input_r80",
                    "research_lifecycle_r932", "r975_evolution_review",
                    "research_fixed_window_r932", "research_fixed_pair_r932", ChampionProbabilityEvidenceR82.TABLE,
                    "research_window_publication_r932", "research_certificate_r929",
                    "research_mainline_contribution_r929", "research_calibration_quality_cp12",
                    "research_candidate_cal_study_r932", "research_candidate_cal_model_r932",
                    "r975_calibration_failure", "research_action_result_binding_cp12",
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.EPOCH,
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.HEAD,
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.SNAPSHOT,
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.JOURNAL,
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.PUBLICATION,
                    com.ethk3.novaonepro.r952.LongScaleSummaryR80.TOKEN,
                    com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.CREATION,
                    com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.SPEC,
                    com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.FROZEN_SOURCE}) {
                checkpoint(control);
                try (Cursor exists = snapshot.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{table})) {
                    if (!exists.moveToFirst()) {
                        if(!first)out.write(',');first=false;
                        out.write(new JSONObject().put("table",table).put("status","MISSING_SCHEMA").put("complete",false).toString());
                        continue;
                    }
                }
                java.util.ArrayList<String> columns = new java.util.ArrayList<>();
                try (Cursor info = snapshot.rawQuery("PRAGMA table_info(" + ident(table) + ")", null)) {
                    while (info.moveToNext()) {
                        String column = info.getString(1);
                        columns.add(column);
                    }
                }
                if (columns.isEmpty()) continue;
                if (!first) out.write(','); first = false;
                // Bounded projections keep large checkpoint/model metadata out of
                // CursorWindow. Text is streamed losslessly from this same snapshot.
                // R13-P33: above 20,000 rows export the last 5,000 in explicit stable table order;
                // the full table remains available through the explicit raw-evidence export.
                long totalRows;try(Cursor total=snapshot.rawQuery("SELECT COUNT(*) FROM "+ident(table),null)){if(!total.moveToFirst())throw new IOException("R66_METADATA_ROW_COUNT_MISSING:"+table);totalRows=total.getLong(0);}
                boolean bounded=totalRows>20000;
                out.write("{\"table\":"+JSONObject.quote(table)+",\"columns\":"+new org.json.JSONArray(columns).toString()+",\"scope\":"+JSONObject.quote(bounded?"LAST_5000_IN_STABLE_TABLE_ORDER":"ALL_ROWS_FULL_METADATA_TEXT")+",\"rowOrder\":"+JSONObject.quote(ReportMetadataStreamR81.rowOrder(snapshot,table))+",\"rows\":[");
                long exportedRows=bounded?ReportMetadataStreamR81.writeLastRows(snapshot,table,columns,out,control,5000):ReportMetadataStreamR81.writeRows(snapshot,table,columns,out,control);
                if(exportedRows!=(bounded?5000:totalRows))throw new IOException("R66_METADATA_ROW_COUNT_MISMATCH:"+table);
                out.write("],\"exportedRows\":"+exportedRows+",\"totalRows\":"+totalRows+",\"complete\":"+(exportedRows==totalRows)+"}");
            }
            out.write("],\"multiHorizonProbeResearch\":"+MultiHorizonProbeR80.reportJson(snapshot,()->control.shouldContinue())+",\"researchSchedulerState\":[");first=true;
            // Exact allow-list: never export unrelated app_state credentials or preferences.
            String[] keys={"cp12_import_subslot","cp12_import_last_result","r975_director_priority",
                    "r65_director_last_slot","r65_director_last_slot_at","r912_research_continuity_status",
                    "r912_research_continuity_at","r908_research_tick_status","r908_research_tick_at",
                    "r908_research_tick_cost_ms","r962_research_next_lane","r971_maintenance_turn",
                    "r980_hot_lane_cursor","r922_dynamic_bootstrap_truth","r48_draw_ingest_pending_issue",
                    "r900_retention_truth","r979_retention_due_at","r943_storage_reclaim",
                    "r943_storage_reclaim_at","retention_cp12_retry_until","cp12_draw_tail_eviction",
                    "cp11_capture_reservation_status","r958_extension_predraw_error",
                    "b07r2_low_memory_at","b07r2_low_memory_action","b07r2_trim_memory_level",
                    "draw_save_error","draw_history_backfill_error","r80_probe_last_status","r80_bound_source_last_status",
                    "r81_bound_position_cursor","r81_bound_maintenance_turn","r81_bound_progress_cursor",
                    "r81_bound_position_status_1","r81_bound_position_status_2","r81_bound_position_status_3","r81_bound_position_status_4","r81_bound_position_status_5","r81_bound_position_status_6","r81_bound_position_status_7","r81_bound_position_status_8","r81_bound_position_status_9","r81_bound_position_status_10"};
            for(String key:keys){
                checkpoint(control);if(!first)out.write(',');first=false;
                ReportMetadataStreamR81.writeState(snapshot,key,out,control);
            }
            for(int position=1;position<=10;position++)for(String suffix:new String[]{"issue","route","reason","drift_policy_hash","drift_observation","drift_observation_hash","drift_cooldown_until","runtime_health_reason"}){checkpoint(control);if(!first)out.write(',');first=false;ReportMetadataStreamR81.writeState(snapshot,"r932_domain_position_"+position+"_"+suffix,out,control);}
            out.write("]}"); out.flush(); fd.getFD().sync();
        }
        return result;
    }
    private static String ident(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }
    /** File contents are not duplicated in reports. Live metadata is explicitly observational. */
    public static File writeFileInventory(File dataRoot,File job,ReportNativeCP4.Control control)throws Exception {
        File root=dataRoot.getCanonicalFile(),own=job.getCanonicalFile(),result=new File(job,"FILE_STATE.jsonl");
        java.util.ArrayDeque<File> pending=new java.util.ArrayDeque<>();pending.add(root);
        long entries=0,unreadable=0;boolean complete=true;
        try(FileOutputStream fd=new FileOutputStream(result);Writer out=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8))) {
            out.write(new JSONObject().put("kind","scope").put("profile","R68_FILE_STATE")
                    .put("boundary","App private data tree metadata, observed during export; live files are not an atomic snapshot. Current export temporaries are excluded. No file content or credential values are included. Hash verification is reported by the attachment registries and checks, not implied by this inventory.")
                    .put("startedWallMs",System.currentTimeMillis()).toString()+"\n");
            while(!pending.isEmpty()) {
                checkpoint(control);File file=pending.removeLast();String relative=file.equals(root)?".":file.getAbsolutePath().substring(root.getPath().length()+1);
                JSONObject row=new JSONObject().put("kind","file").put("path",relative);
                if(!file.getCanonicalFile().equals(file.getAbsoluteFile())) {
                    row.put("state","LINK_NOT_FOLLOWED");
                } else if(file.equals(own)) {
                    row.put("state","CURRENT_EXPORT_TEMPORARIES");
                } else if(file.isDirectory()) {
                    File[] children=file.listFiles();row.put("state",children==null?"DIRECTORY_UNREADABLE":"DIRECTORY");
                    if(children==null){complete=false;unreadable++;}else{row.put("entries",children.length);java.util.Arrays.sort(children);for(File child:children)pending.add(child);}
                } else {
                    boolean readable=file.isFile()&&file.canRead();row.put("state",readable?"FILE_PRESENT":"UNAVAILABLE_OR_CHANGED_DURING_CAPTURE")
                            .put("bytes",file.length()).put("modifiedWallMs",file.lastModified()).put("readable",readable);
                    if(!readable){complete=false;unreadable++;}
                }
                out.write(row.toString());out.write('\n');entries++;
            }
            out.write(new JSONObject().put("kind","receipt").put("entries",entries).put("unreadable",unreadable)
                    .put("enumerationComplete",complete).put("completedWallMs",System.currentTimeMillis()).toString()+"\n");
            out.flush();fd.getFD().sync();
        }
        return result;
    }
    private static void checkpoint(ReportNativeCP4.Control control) throws IOException {
        if (Thread.currentThread().isInterrupted() || !control.shouldContinue())
            throw new InterruptedIOException("REPORT_CANCELLED");
    }
}
