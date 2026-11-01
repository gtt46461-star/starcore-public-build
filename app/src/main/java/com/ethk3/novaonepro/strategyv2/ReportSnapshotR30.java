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
    static final String[] TABLES={"r29_gate_v2","r29_qual_calendar","r30r1_task_progress","r30r1_analysis_run","r30r1_research_measure","r30r1_proposal","r30r1_consume","r30r1_chart_cache","trades","r952_research_outbox","r30_research_chart_frame","draws","r27_history_import","r26_task","r26_progress","r26_work_total","r26_capability","r26_capability_source","r26_live_model","r26_evolution","r26_cohort","r26_certificate_revocation","r27_policy","r27_lineage","r30_behavior_dup","r27_generation","r27_evolution_cursor","r27_replay_dataset","r27_replay_score","r27_live_shadow","r27_live_frozen","r27_eprocess","r27_certificate","r27_calibration","r27_atom_score","r27_molecule","r27_expert_proposal","r27_ablation","r27_chart_cache","r27_legacy_export","r27_opportunity","r27_semantic_fingerprint","r27_abi_continuity","r29_qual_receipt","r29_qual_conflict","r29_qual_cursor","r29_qual_rebuild","r29_pending_draw","r29_model_checkpoint","r30_frozen_confirm","r30_confirm_read","r30_writer_hold","r30_issue_timeline","r30_pit_violation","r30_atom_contribution","r30_model_artifact","r30_model_surface","r30_model_call","r30_decision_explain","r30_decision_mismatch","r30_certificate_revocation_event","r30_revoked_policy","r30_revoked_certificate","r30_live_source_revision","research_matrix_snapshot_r930","su2_matrix_snapshot","su2_live_decision","su2_certificate","su2_mainline_choice","su2_storage_budget","su2_track_block","su2_diag_snapshot","su2_diag_state","su1_model_ablation","r30_retention_archive_index","r30_retention_run","r30_retention_file_action"};
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
            add("total:"+key,"(SELECT count(*) FROM "+id(table)+(where==null?"":" WHERE "+where)+")",true);
            List<String> fields=new ArrayList<>();for(String field:wanted)if(t.columns.containsKey(field)&&!t.columns.get(field).equalsIgnoreCase("BLOB"))fields.add(field);
            for(String pk:t.pk)if(!fields.contains(pk)&&!t.columns.get(pk).equalsIgnoreCase("BLOB"))fields.add(pk);
            for(String field:t.columns.keySet())if((field.endsWith("_hash")||field.endsWith("_sha256"))&&!fields.contains(field)&&!t.columns.get(field).equalsIgnoreCase("BLOB"))fields.add(field);
            if(fields.isEmpty()){add("data:"+key,"NULL",false);return;}
            StringJoiner encoded=new StringJoiner("||';'||");for(String field:fields){String c=id(field);encoded.add("CASE WHEN "+c+" IS NULL THEN 'N:' WHEN length(CAST("+c+" AS BLOB))>"+maxCell+" THEN 'X:'||length(CAST("+c+" AS BLOB)) WHEN typeof("+c+")='real' THEN 'R:'||hex(printf('%!.17g',"+c+")) ELSE upper(substr(typeof("+c+"),1,1))||':'||hex(CAST("+c+" AS BLOB)) END");}
            String sql="(SELECT group_concat(encoded,char(10)) FROM (SELECT "+encoded+" AS encoded FROM "+id(table)+(where==null?"":" WHERE "+where)+(order==null?"":" ORDER BY "+order)+" LIMIT "+limit+"))";
            Cell cell=new Cell("data:"+key,sql,false);cell.fields=fields;cell.table=table;cells.add(cell);
        }
        String sql(){StringJoiner out=new StringJoiner(",","SELECT ","");for(int i=0;i<cells.size();i++)out.add(cells.get(i).sql+" AS v"+i);return out.toString();}
    }
    public static ReportR27.R30Input capture(SQLiteDatabase db){return capture(db,null);}
    public static ReportR27.R30Input capture(SQLiteDatabase db,Map<String,Object> source){return capture(db,source,null);}
    public static ReportR27.R30Input capture(SQLiteDatabase db,Map<String,Object> source,android.os.CancellationSignal cancellation){
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
            Map<String,Object> values=new LinkedHashMap<>();try(Cursor c=cancellation==null?db.rawQuery(q.sql(),null):db.rawQuery(q.sql(),null,cancellation)){
                if(!c.moveToFirst())throw new IllegalStateException("REPORT_SNAPSHOT_ROW_MISSING");
                for(int i=0;i<q.cells.size();i++){Cell cell=q.cells.get(i);Object value=c.isNull(i)?null:cell.numeric?number(c,i):c.getString(i);values.put(cell.key,value);if(cell.fields!=null)data.put(cell.key.substring(5),decode((String)value,cell.table,cell.fields));}
                if(c.moveToNext())throw new IllegalStateException("REPORT_SNAPSHOT_EXPECTED_SINGLE_ROW");
            }
            if(!(values.get("schemaVersion") instanceof Number)||((Number)values.get("schemaVersion")).longValue()!=schema)throw new IllegalStateException("REPORT_SCHEMA_CHANGED_DURING_CAPTURE");
            for(Cell cell:q.cells)if(!cell.key.contains(":"))facts.put(cell.key,values.get(cell.key));
            List<Object> matrixIndexes=new ArrayList<>();for(MatrixRegistryR30.Definition def:MatrixRegistryR30.all())if(def.namespace.equals("R930")){Map<String,Object> index=new LinkedHashMap<>();index.put("kind",def.kind);index.put("retainedCount",values.get("matrixCount:"+def.id));index.put("sourceTable",def.table);index.put("latestOriginalHash",values.get("matrixHash:"+def.id));index.put("latestIssue",values.get("matrixIssue:"+def.id));matrixIndexes.add(index);}data.put("matrixObjectIndex",matrixIndexes);
            List<Object> rowsetIndexes=new ArrayList<>();for(Cell cell:q.cells)if(cell.fields!=null){String key=cell.key.substring(5);Map<String,Object> index=new LinkedHashMap<>();Object total=values.get("total:"+key);int shown=data.get(key) instanceof List?((List<?>)data.get(key)).size():0;index.put("key",key);index.put("sourceTable",cell.table);index.put("total",total);index.put("shown",shown);index.put("omitted",total instanceof Number?Math.max(0,((Number)total).longValue()-shown):null);index.put("fullOriginal","snapshot.db / catalog/"+cell.table);index.put("displayIsFullDenominator",total instanceof Number&&((Number)total).longValue()==shown);rowsetIndexes.add(index);}data.put("rowsetIndexes",rowsetIndexes);
            Map<String,Object> marks=new LinkedHashMap<>();for(String name:TABLES){Table t=tables.get(name);Map<String,Object> mark=new LinkedHashMap<>();mark.put("available",t!=null);mark.put("rows",values.get("count:"+name));mark.put("maxRowid",values.get("rowid:"+name));mark.put("withoutRowid",t==null?null:t.withoutRowid);
                if(t!=null&&t.withoutRowid){Map<String,Object> keys=new LinkedHashMap<>();for(String key:t.pk)keys.put(key,values.get("pk:"+name+":"+key));mark.put("maxPrimaryKeyByColumn",keys);mark.put("rowidReason","WITHOUT_ROWID_NOT_APPLICABLE");}marks.put(name,mark);}
            watermark.put("schemaVersion",schema);watermark.put("captureStartedAt",at);watermark.put("factsReadAt",values.get("factsReadAt"));watermark.put("captureFinishedAt",System.currentTimeMillis());watermark.put("latestIssue",values.get("latestIssue"));watermark.put("tables",marks);watermark.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);watermark.put("reportHostModel",Build.MODEL);
            if(source!=null){Map<String,Object> original=new LinkedHashMap<>(source);data.put("previousSuccessfulReport",original.remove("previousSuccessfulReport"));watermark.put("reportJobId",original.remove("reportJobId"));watermark.put("sourceManifest",original);}
            List<CapabilityRegistryR30.Definition> definitions=CapabilityRegistryR30.all();facts.put("registeredCapabilities",(long)definitions.size());List<Object> capabilities=new ArrayList<>();for(CapabilityRegistryR30.Definition def:definitions){Map<String,Object> row=new LinkedHashMap<>(def.json());row.put("effective",null);row.put("availabilityScope","CODE_AND_CURRENT_BINDINGS_SEPARATE_FROM_STATISTICAL_SNAPSHOT");row.put("evidenceState","SEPARATE_PERSISTED_SOURCE_REQUIRED");capabilities.add(row);}data.put("capabilities",capabilities);
            cachedViews(data);
            timelineViewsR30(data);
            confirmationViewsR30(data);data.put("qualificationGapProcessObservation",QualificationGapRepairR30R1.failureObservation());
            Map<String,Object> authority=new LinkedHashMap<>(CertificateGateR27.contract());authority.put("reportOnlyGate",GateV2R29.POLICY);authority.put("reportOnlyGateEnabled",GateV2R29.ENABLED);authority.put("realOwner","DecisionGateEngine");authority.put("settlementOwner","SettlementEngine");data.put("gateAuthority",authority);
            data.put("modelConsumption",ModelConsumptionCensusR30R1.parse(values.get("modelConsumptionCensus") instanceof String?(String)values.get("modelConsumptionCensus"):null));
            long taskCalendar=-1;try{if(values.get("latestIssue") instanceof String)taskCalendar=TargetIssueResolver.sequenceOrdinalSU1((String)values.get("latestIssue"));}catch(RuntimeException invalid){}
            data.put("researchTableContracts",ResearchTableAdaptersR30R1.contracts());
            data.put("taskLiveness",TaskProgressR30R1.summarize(data.get("tasks"),data.get("typedTaskProgress"),taskCalendar));
            data.put("objectIndex",ReportObjectIndexR30.build(facts,watermark,data));
            Map<String,Object> refs=new LinkedHashMap<>();refs.put("tables",marks);refs.put("inlineRowsAreBounded",true);refs.put("fullEvidence","verified snapshot.db and legacy R26/R27 report attachments");data.put("snapshotSources",refs);
            String digest=StatsR27.sha256(JsonR27.write(facts)+"|"+JsonR27.write(watermark));watermark.put("factsSha256",digest);watermark.put("digestScope","DETACHED_REPORT_FACTS_AND_WATERMARK_NOT_FULL_DATABASE_HASH");
            if(own)db.setTransactionSuccessful();
        }catch(android.os.OperationCanceledException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){errors.add(unavailable.getClass().getSimpleName()+":"+unavailable.getMessage());watermark.put("status","UNKNOWN_CAPTURE_FAILED");facts.clear();data.clear();}
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
        boolean census=q.has("r30_model_call","status","policy_hash","target_ordinal","position","required_count","model_hash","probability_hash","frozen_at","reason")&&q.has("r27_live_shadow","policy_hash","spec_json")&&q.has("r27_live_frozen","policy_hash","target_ordinal","status","frozen_at")&&q.has("r30_model_artifact","model_hash","feature_abi")&&q.has("r30_model_surface","target_ordinal","position","model_hash","probability_hash");
        q.add("modelConsumptionCensus",census?ModelConsumptionCensusR30R1.packedSql():"NULL",false);
        String[][] counts={{"draws","draws"},{"importedRows","r27_history_import"},{"runtimeTasks","r26_task"},{"taskProgressRows","r26_progress"},{"policyObjects","r27_policy"},{"generations","r27_generation"},{"confirmationPackets","r30_frozen_confirm"},{"confirmationReservations","r30_confirm_read"},{"behaviorDuplicates","r30_behavior_dup"},{"writerWindows","r30_writer_hold"},{"legacyTimelineEvents","r30_issue_timeline"},{"pitViolations","r30_pit_violation"},{"qualificationReceipts","r29_qual_receipt"},{"allCheckpointRows","r29_model_checkpoint"},{"modelSurfaceRows","r30_model_surface"},{"modelCalls","r30_model_call"},{"calibratedPolicies","r27_calibration"},{"atoms","r27_atom_score"},{"molecules","r27_molecule"},{"nativeMatrixRows","research_matrix_snapshot_r930"},{"su2MatrixRows","su2_matrix_snapshot"},{"decisionRecords","r30_decision_explain"},{"decisionMismatches","r30_decision_mismatch"},{"revocationEvents","r30_certificate_revocation_event"},{"revokedPolicies","r30_revoked_policy"},{"abiFingerprints","r27_semantic_fingerprint"},{"abiChecks","r27_abi_continuity"},{"legacyExports","r27_legacy_export"},{"ablationRows","r27_ablation"},{"storageCategories","su2_storage_budget"},{"retainedCertificateArchives","r30_revoked_certificate"},{"retainedModelObjects","r26_live_model"},{"upgradeSnapshots","su2_diag_snapshot"},{"upgradeStateRows","su2_diag_state"},{"modelAblationTasks","su1_model_ablation"},{"retentionRuns","r30_retention_run"},{"retentionArchives","r30_retention_archive_index"},{"retentionFileActions","r30_retention_file_action"}};
        for(String[] count:counts)q.count(count[0],count[1]);
        q.add("timelineEvents",q.has("r27_chart_cache","panel_id","key")?"(SELECT count(*) FROM r27_chart_cache WHERE panel_id IN ('"+IssueTimelineR30.EVENT_RAW_PANEL+"','"+IssueTimelineR30.EVENT_DRAW_PANEL+"','"+IssueTimelineR30.EVENT_PRED_PANEL+"'))":"NULL",true);
        boolean active=q.has("r27_live_shadow","policy_hash","status")&&q.has("r27_eprocess","policy_hash","global_index","state","n","acts","hits");
        String join=" FROM r27_eprocess e JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash WHERE s.status IN ('ACTIVE','ACTIVE_REPAIR1') AND e.global_index>0";
        q.add("qualifiedExperiments",active?"(SELECT count(*)"+join+" AND e.state='"+SequentialQualificationR27.QUALIFIED+"')":"NULL",true);
        q.add("maxFuturePeriods",active?"(SELECT coalesce(max(e.n),0)"+join+")":"NULL",true);
        q.add("bestFutureHr",active?"(SELECT max(CASE WHEN e.acts>0 THEN 1.0*e.hits/e.acts END)"+join+")":"NULL",true);
        boolean receipts=active&&q.has("r29_qual_receipt","experiment_id","target_issue","act","hit");
        String rjoin=" FROM r29_qual_receipt r JOIN r27_live_shadow s ON s.policy_hash=r.experiment_id JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.status IN ('ACTIVE','ACTIVE_REPAIR1') AND e.global_index>0";
        for(String[] value:new String[][]{{"futurePeriods","count(*)"},{"futureActs","coalesce(sum(r.act),0)"},{"futureHits","coalesce(sum(CASE WHEN r.act=1 AND r.hit=1 THEN 1 ELSE 0 END),0)"},{"independentFutureIssues","count(DISTINCT r.target_issue)"}})q.add(value[0],receipts?"(SELECT "+value[1]+rjoin+")":"NULL",true);
        q.add("receiptStateMismatches",receipts?"(SELECT count(*)"+join+" AND e.n<>(SELECT count(*) FROM r29_qual_receipt r WHERE r.experiment_id=e.policy_hash))":"NULL",true);
        q.add("activeCertificates",active&&q.has("r27_certificate","policy_hash","status","gate_version","issued_at","expires_at")?"(SELECT count(*) FROM r27_certificate k JOIN r27_live_shadow s ON s.policy_hash=k.policy_hash JOIN r27_eprocess e ON e.policy_hash=k.policy_hash WHERE s.status IN ('ACTIVE','ACTIVE_REPAIR1') AND e.state='"+SequentialQualificationR27.QUALIFIED+"' AND k.status='ACTIVE' AND k.gate_version='"+CertificateGateR27.GATE_VERSION+"' AND k.issued_at<="+READ_CLOCK+" AND k.expires_at>"+READ_CLOCK+")":"NULL",true);
        q.add("evolutionAttempts",q.has("r26_evolution")?"(SELECT count(*) FROM r26_evolution)":"NULL",true);
        q.add("confirmedPackets",q.has("r30_frozen_confirm","state","verified")?"(SELECT count(*) FROM r30_frozen_confirm WHERE state='DONE' AND verified=1)":"NULL",true);
        q.add("modelCheckpoints",q.has("r29_model_checkpoint","owner")?"(SELECT count(*) FROM r29_model_checkpoint WHERE owner IN ('ATOMNET_TEN_R30','ATOMNET_TEN_R30_REPAIR1','ATOMNET_FTRL'))":"NULL",true);
        q.add("birthRejections",q.has("r27_generation","rejected")?"(SELECT coalesce(sum(rejected),0) FROM r27_generation)":"NULL",true);
        q.add("writerHardViolations",q.has("r30_writer_hold","over_hard")?"(SELECT coalesce(sum(over_hard),0) FROM r30_writer_hold)":"NULL",true);
        q.add("openQualificationConflicts",q.has("r29_qual_conflict","resolved_at")?"(SELECT count(*) FROM r29_qual_conflict WHERE resolved_at=0)":"NULL",true);
        q.add("unsafeDecisionMismatches",q.has("r30_decision_mismatch","direction")?"(SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_MORE_PERMISSIVE')":"NULL",true);
        q.add("realRows",q.has("trades","is_realtime_real")?"(SELECT count(*) FROM trades WHERE is_realtime_real=1)":"NULL",true);
        q.add("duplicateRealIssues",q.has("trades","is_realtime_real","target_issue")?"(SELECT count(*) FROM (SELECT target_issue FROM trades WHERE is_realtime_real=1 GROUP BY target_issue HAVING count(*)>1))":"NULL",true);
        for(MatrixRegistryR30.Definition def:MatrixRegistryR30.all())if(def.namespace.equals("R930")){
            String kind=q.has(def.table,"matrix_kind")?"matrix_kind":q.has(def.table,"kind")?"kind":null;
            String where=kind==null?null:id(kind)+"='"+def.kind+"'",order=q.has(def.table,"frozen_at")?" ORDER BY frozen_at DESC":"";
            q.add("matrixCount:"+def.id,where==null?"NULL":"(SELECT count(*) FROM "+id(def.table)+" WHERE "+where+")",true);
            q.add("matrixHash:"+def.id,where==null||!q.has(def.table,"matrix_hash")?"NULL":"(SELECT matrix_hash FROM "+id(def.table)+" WHERE "+where+order+" LIMIT 1)",false);
            q.add("matrixIssue:"+def.id,where==null||!q.has(def.table,"target_issue")?"NULL":"(SELECT target_issue FROM "+id(def.table)+" WHERE "+where+order+" LIMIT 1)",false);
        }
    }
    static void rowsets(Query q){
        q.rows("qualification","r27_eprocess",new String[]{"policy_hash","global_index","n","acts","hits","last_ordinal","state"},null,32,null,256);
        q.rows("certificates","r27_certificate",new String[]{"policy_hash","status","gate_version","evidence_digest","gates_json","issued_at","expires_at"},null,32,null,65536);
        q.rows("gateComparisons","r29_gate_v2",new String[]{"policy_hash","policy_version","would_issue","failing","sections_json","evaluated_through","evaluated_at"},null,32,null,65536);
        q.rows("evolution","r27_generation",new String[]{"generation","loop","dataset_hash","champion_hash","births","rejected","no_op","duplicates","survivors","paired_lcb","verified","detail_json","finished_at"},q.has("r27_generation","generation")?"generation DESC":null,20,null,65536);
        q.rows("confirmation","r30_frozen_confirm",new String[]{"task_key","policy_hash","state","verified","registration_state","registered_at","registration_experiment_id","result_json"},null,20,null,65536);
        q.rows("writerWindows","r30_writer_hold",new String[]{"window_start","window_end","operation","critical","count","failures","hold_p95","hold_max","wait_p95","wait_max","over_hard","histogram_json","process_session"},q.has("r30_writer_hold","window_start")?"window_start DESC":null,2000,q.has("r30_writer_hold","window_start")?"window_start>="+READ_CLOCK+"-86400000":null,4096);
        q.rows("timeline","r30_issue_timeline",new String[]{"issue","event_id","event_role","boot_id","wall_source_published","wall_first_observed","wall_validated","wall_draw_committed","wall_settled","wall_next_requested","wall_next_prepared","wall_next_frozen","wall_next_deadline","mono_first_observed","mono_validated","mono_draw_committed","mono_settled","mono_next_requested","mono_next_prepared","mono_next_frozen","boot_first_observed","boot_validated","boot_draw_committed","boot_settled","boot_next_requested","boot_next_prepared","boot_next_frozen","outcome","updated_at"},q.has("r30_issue_timeline","updated_at")?"updated_at DESC":null,2000,q.has("r30_issue_timeline","updated_at")?"updated_at>="+READ_CLOCK+"-86400000":null,256);
        q.rows("timelineEventRows",IssueTimelineR30.EVENT_TABLE,new String[]{"panel_id","key","payload_json","updated_at"},"updated_at DESC,key",5000,"panel_id IN ('"+IssueTimelineR30.EVENT_DRAW_PANEL+"','"+IssueTimelineR30.EVENT_PRED_PANEL+"') AND updated_at>="+READ_CLOCK+"-86400000",16384);
        q.rows("matrixProduction","r27_chart_cache",new String[]{"key","payload_json","updated_at"},"key",20,"panel_id='matrix-r30-r1'",16384);
        q.rows("researchRuns","r30r1_analysis_run",new String[]{"run_id","dataset_hash","policy_hash","role","source_commit","source_revision","from_ordinal","through_ordinal","contract_hash","status","summary_json","recorded_at"},"recorded_at DESC,run_id",64,null,65536);
        q.rows("researchMeasures","r30r1_research_measure",new String[]{"run_id","measure_key","kind","parent_hash","from_ordinal","to_ordinal","evaluated_calendar_issues","mean_gain","lower95","upper95","payload_json"},"run_id,measure_key",128,null,65536);
        q.rows("researchProposals","r30r1_proposal",new String[]{"proposal_id","run_id","parent_hash","kind","payload_json","payload_hash","state","reason","created_at"},"created_at DESC,proposal_id",64,null,65536);
        q.rows("researchConsumption","r30r1_consume",new String[]{"proposal_id","child_policy_hash","source_generation","consumer_version","receipt_hash","consumed_at"},"consumed_at DESC,proposal_id",128,null,4096);
        q.rows("researchFrames","r30r1_chart_cache",new String[]{"view_id","frame_id","run_id","payload_json","payload_hash","updated_at"},"updated_at DESC,view_id",64,null,65536);
        q.rows("typedTaskProgress","r30r1_task_progress",new String[]{"task_id","owner_version","work_unit","work_cursor","known_latest","pending","oldest_pending_ordinal","last_commit_clock_ordinal","completed_input_ordinal","fence","session_id","artifact_identity","updated_at"},"task_id",100,null,1024);
        q.rows("tasks","r26_task",new String[]{"task_id","lane","status","blocked_reason","wait_kind","wait_detail","pending","last_committed_seq","cursor","steps","last_cpu_ms"},null,100,null,512);
        q.rows("qualificationGaps","r29_qual_cursor",new String[]{"experiment_id","blocked_ordinal","blocked_reason","consumed_total","updated_at"},"experiment_id",100,null,512);
        q.rows("qualificationGapDisposition","r27_chart_cache",new String[]{"panel_id","key","payload_json","updated_at"},"key",100,"panel_id='r30-qualification-gap'",16384);
        q.rows("qualificationConflicts","r29_qual_conflict",new String[]{"experiment_id","target_ordinal","kind","detail","detected_at","resolved_at"},null,20,null,512);
        q.rows("pit","r30_pit_violation",new String[]{"policy_hash","target_ordinal","code","source","recorded_at"},null,20,null,256);
        q.rows("checkpoints","r29_model_checkpoint",new String[]{"owner","feature_abi","dataset_hash","trained_through","state_hash","updated_at"},null,16,null,256);
        q.rows("calibration","r27_calibration",new String[]{"policy_hash","method","fit_rows","validation_rows","excluded_late","excluded_gap","ece","state","updated_at"},null,32,null,256);
        q.rows("modelCalls","r30_model_call",new String[]{"policy_hash","model_hash","target_ordinal","position","status","reason"},null,20,null,256);
        q.rows("runtimeCaches","r27_chart_cache",new String[]{"panel_id","key","payload_json","updated_at"},q.has("r27_chart_cache","updated_at")?"updated_at DESC,panel_id,key":null,64,q.has("r27_chart_cache","panel_id","key")?"(panel_id IN ('health','atomnet','calibration-r30','r30','mainline') OR (panel_id IN ('asset','r29') AND key IN ('build-identity','baseline-identity','legacy-preservation')) )":null,16384);
        q.rows("chartObjectIndex","r27_chart_cache",new String[]{"panel_id","key","updated_at"},"panel_id,key",512,q.has("r27_chart_cache","key")?"key='panel'":null,256);
        q.rows("repairFrameIndex","r30r1_chart_cache",new String[]{"view_id","frame_id","run_id","payload_hash","updated_at"},"view_id",18,"frame_id=(SELECT f.frame_id FROM r30r1_chart_cache f WHERE f.view_id=r30r1_chart_cache.view_id ORDER BY f.updated_at DESC,f.frame_id LIMIT 1)",256);
        for(ResearchGraphSpecsR30R1.Spec graph:ResearchGraphSpecsR30R1.ALL){q.add("researchProduced:"+graph.id,q.has("r30r1_chart_cache","view_id","run_id")&&q.has("r30r1_analysis_run","run_id","source_commit")?"(SELECT count(*) FROM r30r1_chart_cache f JOIN r30r1_analysis_run r ON r.run_id=f.run_id WHERE f.view_id='"+graph.id+"' AND r.source_commit='"+BuildConfig.SOURCE_COMMIT_R26+"')":"NULL",true);q.add("researchConsumed:"+graph.id,q.has("r30r1_research_measure","run_id","kind","measure_key")&&q.has("r30r1_chart_cache","view_id","run_id")?"(SELECT count(DISTINCT m.measure_key) FROM r30r1_research_measure m JOIN r30r1_chart_cache f ON f.run_id=m.run_id WHERE f.view_id='"+graph.id+"' AND m.kind='RESEARCH_SOURCE_CONSUMPTION')":"NULL",true);}
        q.rows("researchFrameIndex","r30_research_chart_frame",new String[]{"graph_id","frame_hash","input_hash","role","updated_at"},null,512,null,256);
        q.rows("outbox","r952_research_outbox",new String[]{"issue","status","attempts","last_error","updated_at"},null,64,null,256);
        q.rows("atoms","r30_atom_contribution",new String[]{"atom_id","dataset_hash","from_row","to_row","n_periods","paired_comparisons","mean_gain","lcb95","source","recorded_at"},null,20,null,256);
        q.rows("molecules","r27_molecule",new String[]{"molecule_hash","generation","state","select_gain","reason"},null,20,null,256);
        q.rows("matrices","research_matrix_snapshot_r930",new String[]{"matrix_kind","kind","target_issue","frozen_at","matrix_hash"},null,20,null,256);
        q.rows("decisions","r30_decision_explain",new String[]{"issue","decision_key","entry_point","original_allow","explain_allow","original_reason","certificate_active","first_blocker","items_json","source_json","recorded_at"},q.has("r30_decision_explain","recorded_at")?"recorded_at DESC":null,20,null,65536);
        q.rows("revocations","r30_certificate_revocation_event",new String[]{"protocol","issue","cause","scope","affected","detected_at"},null,20,null,256);
        q.rows("abi","r27_abi_continuity",new String[]{"policy_hash","old_build","new_build","result","first_diff_issue","checked_at"},null,20,null,256);
        q.rows("legacy","r27_legacy_export",new String[]{"source_id","source_table","status","reason","policy_hash","exported_at"},null,20,null,256);
        q.rows("ablation","r27_ablation",new String[]{"generation","champion_hash","node_index","removed_child","branch","delta_u","delta_logloss"},null,30,null,256);
        q.rows("birthRejects","r27_generation",new String[]{"generation","rejected","detail_json"},q.has("r27_generation","generation")?"generation DESC":null,12,null,4096);
        q.rows("upgradeSnapshots","su2_diag_snapshot",new String[]{"report_sequence","snapshot","created_at"},q.has("su2_diag_snapshot","report_sequence")?"report_sequence DESC":null,1,null,16384);
        q.rows("upgradeState","su2_diag_state",new String[]{"diagnosed_sequence","updated_at"},null,1,null,256);
        q.rows("modelAblation","su1_model_ablation",new String[]{"source_campaign","dataset_blob","kernel_hash","generation","phase","checkpoint_hash","frozen_hash","report_hash","updated_at"},q.has("su1_model_ablation","updated_at")?"updated_at DESC":null,16,null,256);
        q.rows("retentionRun","r30_retention_run",new String[]{"run_id","measured_at","budget_bytes","report_json"},q.has("r30_retention_run","measured_at")?"measured_at DESC,run_id DESC":null,1,null,16384);
        q.rows("retentionArchives","r30_retention_archive_index",new String[]{"source_table","archive_table","schema_hash","retained_rows","updated_at"},"source_table",64,null,256);
        q.rows("retentionFiles","r30_retention_file_action",new String[]{"relative_path","kind","bytes","copy_reference","state","recorded_at","finished_at"},"recorded_at DESC",20,null,512);
        q.rows("storage","su2_storage_budget",new String[]{"category","cap_bytes","measured_bytes","measured_at","action"},null,20,null,256);
    }
    static List<Object> decode(String encoded,String table,List<String> fields){List<Object> out=new ArrayList<>();if(encoded==null||encoded.isEmpty())return out;
        for(String line:encoded.split("\n",-1)){String[] cells=line.split(";",-1);if(cells.length!=fields.size())throw new IllegalStateException("REPORT_ROW_WIDTH:"+table);Map<String,Object> row=new LinkedHashMap<>();row.put("sourceTable",table);
            for(int i=0;i<cells.length;i++){String cell=cells[i];char kind=cell.charAt(0);String value=cell.substring(2);Object parsed;
                if(kind=='N')parsed=null;else if(kind=='X'){Map<String,Object> clipped=new LinkedHashMap<>();clipped.put("status","TRUNCATED_REFERENCE_ONLY");clipped.put("bytes",Long.parseLong(value));clipped.put("source",table+"."+fields.get(i));parsed=clipped;}
                else{String text=new String(hex(value),StandardCharsets.UTF_8);if(kind=='I')parsed=Long.valueOf(text);else if(kind=='R'){try{double number=Double.parseDouble(text);parsed=Double.isFinite(number)?number:null;}catch(NumberFormatException invalid){Map<String,Object> bad=new LinkedHashMap<>();bad.put("status","INVALID_STORED_NUMERIC");bad.put("source",table+"."+fields.get(i));bad.put("storedText",text);parsed=bad;}}else parsed=text;}
                row.put(fields.get(i),parsed);
                if(parsed instanceof String&&(fields.get(i).endsWith("_json")||fields.get(i).equals("snapshot")))row.put(fields.get(i)+"_inline_sha256",HashingSU1.text((String)parsed));
                if(kind=='X'){@SuppressWarnings("unchecked") Map<String,Object> ref=(Map<String,Object>)parsed;ref.put("sha256",null);ref.put("hashReason","CONTENT_NOT_READ_IN_BOUNDED_DISPLAY; use persisted *_hash or exact snapshot identity and primary key");ref.put("payloadIncludedByThisRow",false);}
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
            if(key.equals("baseline-identity"))data.put("baselineIdentity",payload);
            if(key.equals("legacy-preservation"))data.put("legacyPreservation",payload);
        }
    }
    static long longValue(Object value){return value instanceof Number?((Number)value).longValue():-1;}
    static void timelineViewsR30(Map<String,Object> data){
        Object raw=data.get("timelineEventRows");if(!(raw instanceof List))return;data.put("legacyTimeline",data.get("timeline"));
        List<Object> current=new ArrayList<>();Map<String,List<Map<String,Object>>> parents=new LinkedHashMap<>();
        for(Object x:(List<?>)raw){Map<String,Object> stored=MetricEvaluatorR29.map(x);Map<String,Object> row;
            if(stored.containsKey("payload_json")){try{if(!(stored.get("payload_json") instanceof String))throw new IllegalStateException("R30_TIMELINE_BOUNDED_PAYLOAD_UNAVAILABLE");row=new LinkedHashMap<>(MetricEvaluatorR29.map(JsonR27.parse((String)stored.get("payload_json"))));row.put("sourceTable",stored.get("sourceTable"));row.put("cachePanel",stored.get("panel_id"));row.put("cacheKey",stored.get("key"));}catch(RuntimeException invalid){row=new LinkedHashMap<>();row.put("event_role","UNKNOWN");row.put("event_kind","UNKNOWN");row.put("outcome","UNKNOWN_TIMELINE_PAYLOAD");row.put("failureType",invalid.getClass().getSimpleName());current.add(row);continue;}}
            else row=new LinkedHashMap<>(stored);String kind=String.valueOf(row.get("event_kind"));String stage=kind.equals("DRAW")?"settled":"next_frozen";
            row.put("precommit_wall_"+stage,row.get("wall_"+stage));row.put("precommit_mono_"+stage,row.get("mono_"+stage));
            boolean known=numberValueR30(row.get("durable_mono"))!=null&&row.get("durable_boot") instanceof String&&"CALLER_OBSERVED_SUCCESS_AFTER_SQLITE_COMMIT".equals(row.get("timing_boundary"))&&stage.toUpperCase(Locale.ROOT).equals(row.get("durable_stage"));
            row.put("wall_"+stage,known?row.get("durable_wall"):null);row.put("mono_"+stage,known?row.get("durable_mono"):null);row.put("boot_"+stage,known?row.get("durable_boot"):null);row.put("durableObservationState",known?"SUCCESS_OBSERVED_AFTER_COMMIT_UPPER_BOUND":"UNKNOWN_NO_PERSISTED_POSTCOMMIT_OBSERVATION");current.add(row);
            if(kind.equals("DRAW")&&"LIVE".equals(row.get("event_role"))&&!"EVENT_IDENTITY_CONFLICT".equals(row.get("outcome"))&&row.get("wall_draw_committed")!=null&&row.get("mono_settled")!=null)parents.computeIfAbsent(row.get("issue")+"|"+row.get("draw_hash"),key->new ArrayList<>()).add(row);
        }
        for(Object x:current){Map<String,Object> row=MetricEvaluatorR29.map(x);if(!"PREDICTION".equals(row.get("event_kind")))continue;List<Map<String,Object>> candidates=parents.get(row.get("source_issue")+"|"+row.get("source_draw_hash"));
            if(candidates!=null&&!candidates.isEmpty()){candidates.sort((a,b)->Long.compare(longValue(a.get("wall_draw_committed")),longValue(b.get("wall_draw_committed"))));Map<String,Object> parent=candidates.get(0);for(String clock:new String[]{"wall","mono","boot"})row.put(clock+"_settled",parent.get(clock+"_settled"));row.put("sourceSettlementEventId",parent.get("event_id"));row.put("sourceSettlementInstances",candidates.size());}
            else {row.put("wall_settled",null);row.put("mono_settled",null);row.put("boot_settled",null);row.put("sourceSettlementState","UNKNOWN_EXACT_BASE_DRAW_AND_COMMIT_OBSERVATION_REQUIRED");}
        }
        data.put("timeline",current);data.put("timelineSemantics","DRAW=request+exact drawHash; PREDICTION=attempt, base issue+drawHash. Durations end at persisted caller observation after successful COMMIT, an upper bound. Missing postcommit receipts remain UNKNOWN after process death. Original per-issue rows retained under legacyTimeline.");
    }
    static void confirmationViewsR30(Map<String,Object> data){Object source=data.get("confirmation");if(!(source instanceof List))return;List<Object> views=new ArrayList<>();for(Object value:(List<?>)source){Map<String,Object> row=new LinkedHashMap<>(MetricEvaluatorR29.map(value));Map<String,Object> result=new LinkedHashMap<>();try{if(row.get("result_json") instanceof String)result=MetricEvaluatorR29.map(JsonR27.parse((String)row.get("result_json")));}catch(RuntimeException malformed){row.put("resultReadFailure",malformed.getClass().getSimpleName());}row.put("confirmationState",ConfirmationViewR30R1.state(String.valueOf(row.get("state")),String.valueOf(result.get("status")),longValue(row.get("verified"))==1,longValue(row.get("registered_at"))));row.put("originalResult",result);row.put("role","CONFIRM_READ_ONLY_NOT_OPTIMIZER_REWARD");views.add(row);}data.put("confirmationViews",views);}
    static Number numberValueR30(Object value){return MetricEvaluatorR29.number(value);}
    static String id(String name){return "\""+name.replace("\"","\"\"")+"\"";}
}
