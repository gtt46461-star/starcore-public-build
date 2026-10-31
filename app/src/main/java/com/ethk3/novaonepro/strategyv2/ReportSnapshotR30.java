package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Captures every report value in one SELECT read view; chapter formatting never returns to the database.
 * API 35 also exposes an explicit read-only DEFERRED transaction. Older Android uses the SELECT's implicit
 * deferred read transaction. Export's source is already the native BEGIN-pinned, verified read-only copy.
 * No EXCLUSIVE/IMMEDIATE transaction or private platform API is used. */
public final class ReportSnapshotR30 {
    private ReportSnapshotR30() {}
    static final String READ_CLOCK="CAST((julianday('now')-2440587.5)*86400000 AS INTEGER)";
    static final String[] TABLES={"draws","r27_history_import","r26_task","r26_progress","r26_work_total","r26_capability","r26_capability_source","r26_live_model","r26_evolution","r26_cohort","r26_certificate_revocation","r27_policy","r27_lineage","r30_behavior_dup","r27_generation","r27_evolution_cursor","r27_replay_dataset","r27_replay_score","r27_live_shadow","r27_live_frozen","r27_eprocess","r27_certificate","r27_calibration","r27_atom_score","r27_molecule","r27_expert_proposal","r27_ablation","r27_chart_cache","r27_legacy_export","r27_opportunity","r27_semantic_fingerprint","r27_abi_continuity","r29_qual_receipt","r29_qual_conflict","r29_qual_cursor","r29_qual_rebuild","r29_pending_draw","r29_model_checkpoint","r30_frozen_confirm","r30_confirm_read","r30_writer_hold","r30_issue_timeline","r30_pit_violation","r30_atom_contribution","r30_model_artifact","r30_model_surface","r30_model_call","r30_decision_explain","r30_decision_mismatch","r30_certificate_revocation_event","r30_revoked_policy","r30_revoked_certificate","r30_live_source_revision","research_matrix_snapshot_r930","su2_matrix_snapshot","su2_live_decision","su2_certificate","su2_mainline_choice","su2_storage_budget","su2_track_block"};
    static final class Table {
        final String name;final LinkedHashMap<String,String> columns=new LinkedHashMap<>();final List<String> pk=new ArrayList<>();boolean withoutRowid;
        Table(String name){this.name=name;}
    }
    static final class Cell {String key,sql;boolean numeric;String table;List<String> fields;Cell(String key,String sql,boolean numeric){this.key=key;this.sql=sql;this.numeric=numeric;}}
    static final class Query {
        final Map<String,Table> tables;final List<Cell> cells=new ArrayList<>();final List<String> errors;
        Query(Map<String,Table> tables,List<String> errors){this.tables=tables;this.errors=errors;}
        boolean has(String table,String... columns){Table t=tables.get(table);if(t==null)return false;for(String c:columns)if(!t.columns.containsKey(c))return false;return true;}
        void add(String key,String sql,boolean numeric){cells.add(new Cell(key,sql,numeric));}
        void count(String key,String table){add(key,has(table)?"(SELECT count(*) FROM "+id(table)+")":"NULL",true);}
        void rows(String key,String table,String[] wanted,String order,int limit,String where,int maxCell){
            Table t=tables.get(table);if(t==null){add("data:"+key,"NULL",false);return;}
            List<String> fields=new ArrayList<>();for(String field:wanted)if(t.columns.containsKey(field)&&!t.columns.get(field).equalsIgnoreCase("BLOB"))fields.add(field);
            if(fields.isEmpty()){add("data:"+key,"NULL",false);return;}
            StringJoiner encoded=new StringJoiner("||';'||");for(String field:fields){String c=id(field);encoded.add("CASE WHEN "+c+" IS NULL THEN 'N:' WHEN length(CAST("+c+" AS BLOB))>"+maxCell+" THEN 'X:'||length(CAST("+c+" AS BLOB)) WHEN typeof("+c+")='real' THEN 'R:'||hex(printf('%!.17g',"+c+")) ELSE upper(substr(typeof("+c+"),1,1))||':'||hex(CAST("+c+" AS BLOB)) END");}
            String sql="(SELECT group_concat(encoded,char(10)) FROM (SELECT "+encoded+" AS encoded FROM "+id(table)+(where==null?"":" WHERE "+where)+(order==null?"":" ORDER BY "+order)+" LIMIT "+limit+"))";
            Cell cell=new Cell("data:"+key,sql,false);cell.fields=fields;cell.table=table;cells.add(cell);
        }
        String sql(){StringJoiner out=new StringJoiner(",","SELECT ","");for(int i=0;i<cells.size();i++)out.add(cells.get(i).sql+" AS v"+i);return out.toString();}
    }
    public static ReportR27.R30Input capture(SQLiteDatabase db){return capture(db,null);}
    public static ReportR27.R30Input capture(SQLiteDatabase db,Map<String,Object> source){
        Map<String,Object> facts=new LinkedHashMap<>(),watermark=new LinkedHashMap<>(),data=new LinkedHashMap<>();List<String> errors=new ArrayList<>();boolean own=false;
        try {
            if(db.inTransaction())throw new IllegalStateException("REPORT_READ_CALLED_IN_EXISTING_TRANSACTION");
            if(Build.VERSION.SDK_INT>=35){db.beginTransactionReadOnly();own=true;}
            watermark.put("readMode",own?"PUBLIC_READ_ONLY_DEFERRED":"IMPLICIT_DEFERRED_SINGLE_SELECT");watermark.put("readOnlyConnection",db.isReadOnly());
            long schema=schema(db),at=System.currentTimeMillis();Map<String,Table> tables=metadata(db);Query q=new Query(tables,errors);
            q.add("schemaVersion","(SELECT schema_version FROM pragma_schema_version)",true);
            q.add("factsReadAt",READ_CLOCK,true);
            q.add("latestIssue",q.has("draws","issue","dirty")?"(SELECT max(issue) FROM draws WHERE dirty=0)":"NULL",false);
            for(String name:TABLES){Table t=tables.get(name);q.count("count:"+name,name);String alias=null;if(t!=null&&!t.withoutRowid)for(String candidate:new String[]{"_rowid_","rowid","oid"})if(!t.columns.containsKey(candidate)){alias=candidate;break;}
                q.add("rowid:"+name,alias==null?"NULL":"(SELECT max("+id(alias)+") FROM "+id(name)+")",true);
                if(t!=null&&t.withoutRowid)for(String key:t.pk)q.add("pk:"+name+":"+key,"(SELECT max("+id(key)+") FROM "+id(name)+")",false);
            }
            metrics(q,at);rowsets(q);
            Map<String,Object> values=new LinkedHashMap<>();try(Cursor c=db.rawQuery(q.sql(),null)){
                if(!c.moveToFirst())throw new IllegalStateException("REPORT_SNAPSHOT_ROW_MISSING");
                for(int i=0;i<q.cells.size();i++){Cell cell=q.cells.get(i);Object value=c.isNull(i)?null:cell.numeric?number(c,i):c.getString(i);values.put(cell.key,value);if(cell.fields!=null)data.put(cell.key.substring(5),decode((String)value,cell.table,cell.fields));}
                if(c.moveToNext())throw new IllegalStateException("REPORT_SNAPSHOT_EXPECTED_SINGLE_ROW");
            }
            if(!(values.get("schemaVersion") instanceof Number)||((Number)values.get("schemaVersion")).longValue()!=schema)throw new IllegalStateException("REPORT_SCHEMA_CHANGED_DURING_CAPTURE");
            for(Cell cell:q.cells)if(!cell.key.contains(":"))facts.put(cell.key,values.get(cell.key));
            Map<String,Object> marks=new LinkedHashMap<>();for(String name:TABLES){Table t=tables.get(name);Map<String,Object> mark=new LinkedHashMap<>();mark.put("available",t!=null);mark.put("rows",values.get("count:"+name));mark.put("maxRowid",values.get("rowid:"+name));mark.put("withoutRowid",t==null?null:t.withoutRowid);
                if(t!=null&&t.withoutRowid){Map<String,Object> keys=new LinkedHashMap<>();for(String key:t.pk)keys.put(key,values.get("pk:"+name+":"+key));mark.put("maxPrimaryKeyByColumn",keys);mark.put("rowidReason","WITHOUT_ROWID_NOT_APPLICABLE");}marks.put(name,mark);}
            watermark.put("schemaVersion",schema);watermark.put("captureStartedAt",at);watermark.put("factsReadAt",values.get("factsReadAt"));watermark.put("captureFinishedAt",System.currentTimeMillis());watermark.put("latestIssue",values.get("latestIssue"));watermark.put("tables",marks);watermark.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);watermark.put("reportHostModel",Build.MODEL);
            if(source!=null)watermark.put("sourceManifest",source);
            List<CapabilityRegistryR30.Definition> definitions=CapabilityRegistryR30.all();facts.put("registeredCapabilities",(long)definitions.size());List<Object> capabilities=new ArrayList<>();for(CapabilityRegistryR30.Definition def:definitions){Map<String,Object> row=new LinkedHashMap<>(def.json());row.put("effective",null);row.put("availabilityScope","CODE_AND_CURRENT_BINDINGS_SEPARATE_FROM_STATISTICAL_SNAPSHOT");row.put("evidenceState","SEPARATE_PERSISTED_SOURCE_REQUIRED");capabilities.add(row);}data.put("capabilities",capabilities);
            cachedViews(data);
            data.put("taskLiveness",taskLiveness(data.get("tasks"),(String)values.get("latestIssue"),facts.get("runtimeTasks")));
            Map<String,Object> refs=new LinkedHashMap<>();refs.put("tables",marks);refs.put("inlineRowsAreBounded",true);refs.put("fullEvidence","verified snapshot.db and legacy R26/R27 report attachments");data.put("snapshotSources",refs);
            String digest=StatsR27.sha256(JsonR27.write(facts)+"|"+JsonR27.write(watermark));watermark.put("factsSha256",digest);watermark.put("digestScope","DETACHED_REPORT_FACTS_AND_WATERMARK_NOT_FULL_DATABASE_HASH");
            if(own)db.setTransactionSuccessful();
        }catch(RuntimeException unavailable){errors.add(unavailable.getClass().getSimpleName()+":"+unavailable.getMessage());watermark.put("status","UNKNOWN_CAPTURE_FAILED");facts.clear();data.clear();}
        finally{if(own)db.endTransaction();}
        return new ReportR27.R30Input(facts,watermark,data,errors);
    }
    static Number number(Cursor c,int i){String text=c.getString(i);try{return Long.valueOf(text);}catch(NumberFormatException real){try{double parsed=Double.parseDouble(text);return Double.isFinite(parsed)?c.getDouble(i):null;}catch(NumberFormatException invalid){return null;}}}
    static long schema(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())throw new IllegalStateException("REPORT_SCHEMA_VERSION_MISSING");return c.getLong(0);}}
    static Map<String,Table> metadata(SQLiteDatabase db){Map<String,Table> out=new LinkedHashMap<>();Set<String> wanted=new HashSet<>(Arrays.asList(TABLES));
        try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table' ORDER BY name",null)){while(c.moveToNext()){String name=c.getString(0);if(!wanted.contains(name))continue;Table t=new Table(name);t.withoutRowid=c.getString(1)!=null&&c.getString(1).toUpperCase(Locale.ROOT).contains("WITHOUT ROWID");out.put(name,t);}}
        for(Table t:out.values())try(Cursor c=db.rawQuery("PRAGMA table_info("+id(t.name)+")",null)){while(c.moveToNext()){t.columns.put(c.getString(1),c.getString(2)==null?"":c.getString(2));if(c.getInt(5)>0)t.pk.add(c.getString(1));}}
        return out;
    }
    static void metrics(Query q,long at){
        String[][] counts={{"draws","draws"},{"importedRows","r27_history_import"},{"runtimeTasks","r26_task"},{"taskProgressRows","r26_progress"},{"policyObjects","r27_policy"},{"generations","r27_generation"},{"confirmationPackets","r30_frozen_confirm"},{"confirmationReservations","r30_confirm_read"},{"behaviorDuplicates","r30_behavior_dup"},{"writerWindows","r30_writer_hold"},{"timelineEvents","r30_issue_timeline"},{"pitViolations","r30_pit_violation"},{"qualificationReceipts","r29_qual_receipt"},{"allCheckpointRows","r29_model_checkpoint"},{"modelSurfaceRows","r30_model_surface"},{"modelCalls","r30_model_call"},{"calibratedPolicies","r27_calibration"},{"atoms","r27_atom_score"},{"molecules","r27_molecule"},{"nativeMatrixRows","research_matrix_snapshot_r930"},{"su2MatrixRows","su2_matrix_snapshot"},{"decisionRecords","r30_decision_explain"},{"decisionMismatches","r30_decision_mismatch"},{"revocationEvents","r30_certificate_revocation_event"},{"revokedPolicies","r30_revoked_policy"},{"abiFingerprints","r27_semantic_fingerprint"},{"abiChecks","r27_abi_continuity"},{"legacyExports","r27_legacy_export"},{"ablationRows","r27_ablation"},{"storageCategories","su2_storage_budget"},{"retainedCertificateArchives","r30_revoked_certificate"},{"retainedModelObjects","r26_live_model"}};
        for(String[] count:counts)q.count(count[0],count[1]);
        boolean active=q.has("r27_live_shadow","policy_hash","status")&&q.has("r27_eprocess","policy_hash","global_index","state","n","acts","hits");
        String join=" FROM r27_eprocess e JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash WHERE s.status='ACTIVE' AND e.global_index>0";
        q.add("qualifiedExperiments",active?"(SELECT count(*)"+join+" AND e.state='"+SequentialQualificationR27.QUALIFIED+"')":"NULL",true);
        q.add("maxFuturePeriods",active?"(SELECT coalesce(max(e.n),0)"+join+")":"NULL",true);
        q.add("bestFutureHr",active?"(SELECT max(CASE WHEN e.acts>0 THEN 1.0*e.hits/e.acts END)"+join+")":"NULL",true);
        boolean receipts=active&&q.has("r29_qual_receipt","experiment_id","target_issue","act","hit");
        String rjoin=" FROM r29_qual_receipt r JOIN r27_live_shadow s ON s.policy_hash=r.experiment_id JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.status='ACTIVE' AND e.global_index>0";
        for(String[] value:new String[][]{{"futurePeriods","count(*)"},{"futureActs","coalesce(sum(r.act),0)"},{"futureHits","coalesce(sum(CASE WHEN r.act=1 AND r.hit=1 THEN 1 ELSE 0 END),0)"},{"independentFutureIssues","count(DISTINCT r.target_issue)"}})q.add(value[0],receipts?"(SELECT "+value[1]+rjoin+")":"NULL",true);
        q.add("receiptStateMismatches",receipts?"(SELECT count(*)"+join+" AND e.n<>(SELECT count(*) FROM r29_qual_receipt r WHERE r.experiment_id=e.policy_hash))":"NULL",true);
        q.add("activeCertificates",active&&q.has("r27_certificate","policy_hash","status","gate_version","issued_at","expires_at")?"(SELECT count(*) FROM r27_certificate k JOIN r27_live_shadow s ON s.policy_hash=k.policy_hash JOIN r27_eprocess e ON e.policy_hash=k.policy_hash WHERE s.status='ACTIVE' AND e.state='"+SequentialQualificationR27.QUALIFIED+"' AND k.status='ACTIVE' AND k.gate_version='"+CertificateGateR27.GATE_VERSION+"' AND k.issued_at<="+READ_CLOCK+" AND k.expires_at>"+READ_CLOCK+")":"NULL",true);
        q.add("evolutionAttempts",q.has("r26_evolution")?"(SELECT count(*) FROM r26_evolution)":"NULL",true);
        q.add("confirmedPackets",q.has("r30_frozen_confirm","state","verified")?"(SELECT count(*) FROM r30_frozen_confirm WHERE state='DONE' AND verified=1)":"NULL",true);
        q.add("modelCheckpoints",q.has("r29_model_checkpoint","owner")?"(SELECT count(*) FROM r29_model_checkpoint WHERE owner IN ('ATOMNET_TEN_R30','ATOMNET_FTRL'))":"NULL",true);
        q.add("birthRejections",q.has("r27_generation","rejected")?"(SELECT coalesce(sum(rejected),0) FROM r27_generation)":"NULL",true);
        q.add("writerHardViolations",q.has("r30_writer_hold","over_hard")?"(SELECT coalesce(sum(over_hard),0) FROM r30_writer_hold)":"NULL",true);
        q.add("openQualificationConflicts",q.has("r29_qual_conflict","resolved_at")?"(SELECT count(*) FROM r29_qual_conflict WHERE resolved_at=0)":"NULL",true);
        q.add("unsafeDecisionMismatches",q.has("r30_decision_mismatch","direction")?"(SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_MORE_PERMISSIVE')":"NULL",true);
    }
    static void rowsets(Query q){
        q.rows("qualification","r27_eprocess",new String[]{"policy_hash","global_index","n","acts","hits","last_ordinal","state"},null,32,null,256);
        q.rows("certificates","r27_certificate",new String[]{"policy_hash","status","gate_version","evidence_digest","issued_at","expires_at"},null,32,null,256);
        q.rows("evolution","r27_generation",new String[]{"generation","loop","births","rejected","no_op","duplicates","survivors","verified","finished_at"},q.has("r27_generation","generation")?"generation DESC":null,20,null,256);
        q.rows("confirmation","r30_frozen_confirm",new String[]{"task_key","policy_hash","state","verified","registration_state","registered_at"},null,20,null,256);
        q.rows("writerWindows","r30_writer_hold",new String[]{"window_start","operation","critical","count","hold_p95","hold_max","wait_p95","over_hard"},q.has("r30_writer_hold","window_start")?"window_start DESC":null,40,null,256);
        q.rows("timeline","r30_issue_timeline",new String[]{"issue","boot_id","wall_source_published","wall_first_observed","wall_validated","wall_draw_committed","wall_settled","wall_next_requested","wall_next_prepared","wall_next_frozen","wall_next_deadline","mono_first_observed","mono_validated","mono_draw_committed","mono_settled","mono_next_requested","mono_next_prepared","mono_next_frozen","outcome","updated_at"},null,40,null,256);
        q.rows("tasks","r26_task",new String[]{"task_id","lane","status","blocked_reason","wait_kind","wait_detail","pending","last_committed_seq","cursor","steps","last_cpu_ms"},null,100,null,512);
        q.rows("qualificationConflicts","r29_qual_conflict",new String[]{"experiment_id","target_ordinal","kind","detail","detected_at","resolved_at"},null,20,null,512);
        q.rows("pit","r30_pit_violation",new String[]{"policy_hash","target_ordinal","code","source","recorded_at"},null,20,null,256);
        q.rows("checkpoints","r29_model_checkpoint",new String[]{"owner","feature_abi","dataset_hash","trained_through","state_hash","updated_at"},null,16,null,256);
        q.rows("calibration","r27_calibration",new String[]{"policy_hash","method","fit_rows","validation_rows","excluded_late","excluded_gap","ece","state","updated_at"},null,32,null,256);
        q.rows("modelCalls","r30_model_call",new String[]{"policy_hash","model_hash","target_ordinal","position","status","reason"},null,20,null,256);
        q.rows("runtimeCaches","r27_chart_cache",new String[]{"panel_id","key","payload_json","updated_at"},q.has("r27_chart_cache","updated_at")?"updated_at DESC":null,8,q.has("r27_chart_cache","panel_id","key")?"(panel_id IN ('health','atomnet','calibration-r30','r30','mainline') OR (panel_id='asset' AND key IN ('build-identity','baseline-identity','legacy-preservation')) )":null,16384);
        q.rows("atoms","r30_atom_contribution",new String[]{"atom_id","dataset_hash","from_row","to_row","n_periods","paired_comparisons","mean_gain","lcb95","source","recorded_at"},null,20,null,256);
        q.rows("molecules","r27_molecule",new String[]{"molecule_hash","generation","state","select_gain","reason"},null,20,null,256);
        q.rows("matrices","research_matrix_snapshot_r930",new String[]{"matrix_kind","kind","target_issue","frozen_at","matrix_hash"},null,20,null,256);
        q.rows("decisions","r30_decision_explain",new String[]{"issue","entry_point","original_allow","explain_allow","original_reason","first_blocker","items_json","recorded_at"},q.has("r30_decision_explain","recorded_at")?"recorded_at DESC":null,20,null,2048);
        q.rows("revocations","r30_certificate_revocation_event",new String[]{"protocol","issue","cause","scope","affected","detected_at"},null,20,null,256);
        q.rows("abi","r27_abi_continuity",new String[]{"policy_hash","old_build","new_build","result","first_diff_issue","checked_at"},null,20,null,256);
        q.rows("legacy","r27_legacy_export",new String[]{"source_id","source_table","status","reason","policy_hash","exported_at"},null,20,null,256);
        q.rows("ablation","r27_ablation",new String[]{"generation","champion_hash","node_index","branch","delta_u","delta_logloss"},null,30,null,256);
        q.rows("birthRejects","r27_generation",new String[]{"generation","rejected","detail_json"},q.has("r27_generation","generation")?"generation DESC":null,12,null,4096);
        q.rows("storage","su2_storage_budget",new String[]{"category","cap_bytes","measured_bytes","measured_at","action"},null,20,null,256);
    }
    static List<Object> decode(String encoded,String table,List<String> fields){List<Object> out=new ArrayList<>();if(encoded==null||encoded.isEmpty())return out;
        for(String line:encoded.split("\n",-1)){String[] cells=line.split(";",-1);if(cells.length!=fields.size())throw new IllegalStateException("REPORT_ROW_WIDTH:"+table);Map<String,Object> row=new LinkedHashMap<>();row.put("sourceTable",table);
            for(int i=0;i<cells.length;i++){String cell=cells[i];char kind=cell.charAt(0);String value=cell.substring(2);Object parsed;
                if(kind=='N')parsed=null;else if(kind=='X'){Map<String,Object> clipped=new LinkedHashMap<>();clipped.put("status","TRUNCATED_REFERENCE_ONLY");clipped.put("bytes",Long.parseLong(value));clipped.put("source",table+"."+fields.get(i));parsed=clipped;}
                else{String text=new String(hex(value),StandardCharsets.UTF_8);if(kind=='I')parsed=Long.valueOf(text);else if(kind=='R'){try{double number=Double.parseDouble(text);parsed=Double.isFinite(number)?number:null;}catch(NumberFormatException invalid){Map<String,Object> bad=new LinkedHashMap<>();bad.put("status","INVALID_STORED_NUMERIC");bad.put("source",table+"."+fields.get(i));bad.put("storedText",text);parsed=bad;}}else parsed=text;}
                row.put(fields.get(i),parsed);
            }out.add(row);
        }return out;
    }
    static byte[] hex(String text){if(text.length()%2!=0)throw new IllegalStateException("REPORT_HEX_WIDTH");byte[] bytes=new byte[text.length()/2];for(int i=0;i<bytes.length;i++){int a=Character.digit(text.charAt(i*2),16),b=Character.digit(text.charAt(i*2+1),16);if(a<0||b<0)throw new IllegalStateException("REPORT_HEX_VALUE");bytes[i]=(byte)((a<<4)|b);}return bytes;}
    @SuppressWarnings("unchecked") static Object taskLiveness(Object raw,String latest,Object total){Map<String,Object> out=new LinkedHashMap<>();if(!(raw instanceof List)){out.put("status","UNKNOWN_SOURCE_MISSING");return out;}List<TaskLivenessR30.State> states=new ArrayList<>();List<Object> rows=new ArrayList<>();long ordinal=-1;try{if(latest!=null)ordinal=TargetIssueResolver.sequenceOrdinalSU1(latest);}catch(RuntimeException invalid){ordinal=-1;}
        for(Object item:(List<?>)raw){Map<String,Object> row=(Map<String,Object>)item;long pending=longValue(row.get("pending")),commit=longValue(row.get("last_committed_seq")),since=commit<0||ordinal<0?-1:Math.max(0,ordinal-commit);String reason=String.valueOf(row.get("blocked_reason")),wait=String.valueOf(row.get("wait_kind"))+" "+row.get("wait_detail");if(row.get("blocked_reason") instanceof Map||row.get("wait_detail") instanceof Map||row.get("wait_kind") instanceof Map)pending=-1;TaskLivenessR30.State state=TaskLivenessR30.classify(pending,since,reason.startsWith("DEPENDENCY")||reason.startsWith("WAITING_"),reason.contains("WAITING_NEW_DRAW")||wait.contains("WAITING_NEW_DRAW"),"RUNNING".equals(row.get("status")));states.add(state);Map<String,Object> value=new LinkedHashMap<>();value.put("taskId",row.get("task_id"));value.put("state",state.name());value.put("pending",pending<0?null:pending);value.put("periodsSinceCommit",since<0?null:since);rows.add(value);}
        out.put("tasks",rows);Map<String,Object> summary=new LinkedHashMap<>(TaskLivenessR30.summarize(states.toArray(new TaskLivenessR30.State[0])));long omitted=total instanceof Number?Math.max(0,((Number)total).longValue()-rows.size()):-1;out.put("omittedTasks",omitted<0?null:omitted);if(omitted!=0)summary.put("m12",null);out.put("summary",summary);out.put("scope","BOUNDED_DISPLAY; omitted task rows remain in the snapshot, never a measured healthy state");return out;
    }
    @SuppressWarnings("unchecked") static void cachedViews(Map<String,Object> data){
        Object rows=data.get("runtimeCaches");if(!(rows instanceof List))return;
        for(Object item:(List<?>)rows){Map<String,Object> row=(Map<String,Object>)item;String panel=String.valueOf(row.get("panel_id")),key=String.valueOf(row.get("key"));Object payload=row.get("payload_json");
            if(payload instanceof String)try{payload=JsonR27.parse((String)payload);}catch(RuntimeException malformed){Map<String,Object> error=new LinkedHashMap<>();error.put("status","UNREADABLE_CACHED_REPORT");error.put("source",panel+"/"+key);payload=error;}
            Map<String,Object> view=new LinkedHashMap<>();view.put("payload",payload);view.put("updatedAt",row.get("updated_at"));view.put("source",panel+"/"+key);view.put("scope","PERSISTED_REPORT_CACHE; NOT_MODEL_ARTIFACT_OR_NATURAL_QUALITY_VALIDATION");
            if(panel.equals("r30")&&key.equals("q08"))data.put("syntheticControl",view);
            if(panel.equals("atomnet")&&key.equals("ten-position-r30"))data.put("neuralTraining",view);
        }
    }
    static long longValue(Object value){return value instanceof Number?((Number)value).longValue():-1;}
    static String id(String name){return "\""+name.replace("\"","\"\"")+"\"";}
}
