package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** Existing maintenance worker consumes immutable snapshots, one module per bounded pulse. */
public final class ModuleAblationR932 {
    public static final String TABLE="research_module_ablation_r932";
    public static final int MIN_FAILURE_SUPPORT=30;
    private ModuleAblationR932(){}
    public static final class Summary {
        public final int cases,failures,brierCases;public final double meanDeltaHit,meanDeltaBrier;public final boolean sufficient;
        Summary(int cases,int failures,double sum){this(cases,failures,sum,0,0);}
        Summary(int cases,int failures,double sum,int brierCases,double brierSum){this.cases=cases;this.failures=failures;this.brierCases=brierCases;meanDeltaBrier=brierCases==0?Double.NaN:brierSum/brierCases;meanDeltaHit=cases==0?0:sum/cases;sufficient=failures>=MIN_FAILURE_SUPPORT;}
    }
    public static Summary summary(DbStore store,ModuleReplayR932.Module module) {
        int n=0,failures=0,brierN=0;double sum=0,brierSum=0;
        try(Cursor exists=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){if(!exists.moveToFirst())return new Summary(0,0,0);}
        try(Cursor c=store.readable().rawQuery("SELECT payload,payload_hash FROM "+TABLE+" WHERE code_hash=? AND module=? AND state='REEXECUTED' ORDER BY target_issue DESC LIMIT 240",new String[]{SourceManifestR815.SOURCE_TREE_SHA256,module.name()})) {
            while(c.moveToNext()) {
                if(!V51ChecksumEngine.sha256(c.getString(0)).equals(c.getString(1)))throw new IllegalStateException("R932_ABLATION_REPORT_HASH");
                try{JSONObject row=new JSONObject(c.getString(0));n++;if(row.getBoolean("original_failure"))failures++;sum+=row.getInt("delta_hit");if(row.has("delta_brier")&&!row.isNull("delta_brier")){double delta=row.getDouble("delta_brier");if(!Double.isFinite(delta))throw new IllegalStateException("R932_ABLATION_REPORT_NONFINITE");brierN++;brierSum+=delta;}}catch(JSONException error){throw new IllegalStateException("R932_ABLATION_REPORT_JSON",error);}
            }
        }
        return new Summary(n,failures,sum,brierN,brierSum);
    }
    public static String report(DbStore store) {
        StringBuilder out=new StringBuilder("\n同输入模块消融（最近240条已重跑样本）\n");
        for(ModuleReplayR932.Module module:ModuleReplayR932.Module.values())if(module!=ModuleReplayR932.Module.BASELINE) {
            Summary s=summary(store,module);out.append(module.name()).append("：N=").append(s.cases).append("，原失败样本=").append(s.failures).append('/').append(MIN_FAILURE_SUPPORT);
            out.append(s.sufficient?"，平均命中变化="+s.meanDeltaHit:"，不足以归因");out.append("，校准配对N=").append(s.brierCases).append(s.sufficient&&s.brierCases>=MIN_FAILURE_SUPPORT?"，平均ΔBrier="+s.meanDeltaBrier:"，ΔBrier支持不足").append('\n');
        }
        return out.append("未激活模块另记负对照；缺冻结输入/原结果复现失败不计样本。ΔBrier使用严格更早120拟合/1隔离/120验证的模块专用校准；不足或验证失败显示缺失。消融不是未来晋级证据。\n").toString();
    }
    static void schema(SQLiteDatabase db){
        ModuleCalibrationR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT NOT NULL,module TEXT NOT NULL,source_hash TEXT NOT NULL,code_hash TEXT NOT NULL,state TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,module,source_hash,code_hash))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_module_summary ON "+TABLE+"(code_hash,module,state,target_issue)");
    }
    static JSONObject analyze(List<String> proofs,String sourceHash,Models.Draw actual,ModuleReplayR932.Module module)throws Exception {
        try(RecipeBudgetScopeCP12 recipeScope=RecipeBudgetScopeCP12.open()){
        FrozenModuleInputR932.Input input=FrozenModuleInputR932.decode(proofs);
        if(actual==null||!actual.isValid()||!actual.issue.equals(input.target))throw new IllegalArgumentException("R932_ABLATION_ACTUAL_ISSUE");
        long start=System.nanoTime();
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forHistoricalEvidence(input.bundle,input.target,input.features.context.createdAt)) {
            // Every case first reproduces the original result. A parity failure invalidates all deltas.
            ModuleReplayR932.Result baseline=ModuleReplayR932.rerun(input,ModuleReplayR932.Module.BASELINE);
            ResearchWorkBudgetR927.checkpoint();
            ModuleReplayR932.Result counter=module==ModuleReplayR932.Module.BASELINE?baseline:ModuleReplayR932.rerun(input,module);
            int originalHit=actual.hitStrictPositionK3(baseline.selected.position,baseline.selected.n1,baseline.selected.n2,baseline.selected.n3)?1:0;
            int newHit=actual.hitStrictPositionK3(counter.selected.position,counter.selected.n1,counter.selected.n2,counter.selected.n3)?1:0;
            JSONObject result=new JSONObject();result.put("target_issue",input.target);result.put("module",module.name());result.put("source_hash",sourceHash);result.put("model_bundle_hash",input.bundle);
            result.put("state",module==ModuleReplayR932.Module.BASELINE?"BASELINE_PARITY_PASS":counter.disabledActive?"REEXECUTED":"INACTIVE_MODULE_NEGATIVE_CONTROL");
            result.put("original_ordinal",K3ActionSpaceEngine.globalOrdinal(baseline.selected.position,baseline.selected.n1,baseline.selected.n2,baseline.selected.n3));result.put("counterfactual_ordinal",K3ActionSpaceEngine.globalOrdinal(counter.selected.position,counter.selected.n1,counter.selected.n2,counter.selected.n3));
            result.put("original_hit",originalHit);result.put("counterfactual_hit",newHit);result.put("delta_hit",newHit-originalHit);result.put("original_failure",originalHit==0);
            result.put("original_ranking_hash",baseline.actionHash);result.put("counterfactual_ranking_hash",counter.actionHash);result.put("formal_action_count",counter.actions.length);
            result.put("consumer_lane",input.replay.getString("consumer_lane"));result.put("consumer_id",input.replay.getString("consumer_id"));
            V51Models.StateTrajectorySnapshot state=input.states[baseline.selected.position-1];result.put("predraw_domain",state.domainId);result.put("predraw_lifecycle",state.lifecycle.name());result.put("state_hash",V51ChecksumEngine.sha256(input.raw.getJSONArray("states").toString()+input.replay.getJSONArray("state_metadata").toString()));
            result.put("original_third_margin",baseline.selected.thirdMargin);result.put("original_rank_margin",baseline.selected.rankMargin);result.put("counterfactual_third_margin",counter.selected.thirdMargin);
            result.put("changed_rank",!baseline.actionHash.equals(counter.actionHash));result.put("selected_rank_score",counter.selected.rankScore);result.put("original_rank_score",baseline.selected.rankScore);result.put("delta_score",counter.selected.rankScore-baseline.selected.rankScore);
            result.put("calibration_identity",ModuleCalibrationR932.identity(input,module));
            result.put("delta_brier",JSONObject.NULL);result.put("brier_status","MODULE_SPECIFIC_CALIBRATION_REQUIRED; ORIGINAL_SELECTOR_CAL_IS_NOT_REUSED");
            result.put("measured_cost_ns",Math.max(0,System.nanoTime()-start));result.put("scope","POSTMORTEM_SAME_INPUT_NOT_FUTURE_PROMOTION_EVIDENCE");result.put("minimum_failure_support",MIN_FAILURE_SUPPORT);result.put("may_authorize_real",false);return result;
        }
        }
    }
    static boolean hasRunnableWork(SQLiteDatabase db) {
        String current="",cursor="",source="";
        try(Cursor c=db.rawQuery("SELECT state_key,state_value FROM "+DbHelper.TABLE_APP_STATE
                +" WHERE state_key IN ('r932_ablation_current','r932_ablation_cursor','r965_ablation_source')",null)){
            while(c.moveToNext()){
                if("r932_ablation_current".equals(c.getString(0)))current=c.getString(1);
                else if("r932_ablation_cursor".equals(c.getString(0)))cursor=c.getString(1);
                else source=c.getString(1);
            }
        }
        if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(source)){current="";cursor="";}
        String predicate=current.length()>0?" AND j.target_issue=?":cursor.length()>0?" AND j.target_issue>?":"";
        String[] args=current.length()>0?new String[]{current}:cursor.length()>0?new String[]{cursor}:null;
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                +" j WHERE j.snapshot_blob IS NOT NULL AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS
                +" d WHERE d.issue=j.target_issue AND d.dirty=0)"+predicate+" LIMIT 1",args)){return c.moveToFirst();}
    }
    public static void maintain(final DbStore store) { maintainReviewed(store); }
    static String maintainReviewed(final DbStore store) {
        if(store==null)return "ERROR:STORE_NULL";
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
        if(!store.coordinatorR36().runAtomicWrite("R932_ABLATION_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){schema(db);}}))throw new IllegalStateException("R932_ABLATION_SCHEMA_COMMIT");
        // Source-bound replay cannot consume a previous APK's backlog. Preserve that cursor as
        // unprocessed history and anchor the new code at the current boundary exactly once.
        if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(store.getState("r965_ablation_source",""))) {
            if(!store.coordinatorR36().runAtomicWrite("R965_ABLATION_SOURCE_BOUNDARY",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                String source=store.getState("r965_ablation_source","");
                if(SourceManifestR815.SOURCE_TREE_SHA256.equals(source))return;
                LinkedHashMap<String,String> state=new LinkedHashMap<String,String>();
                state.put("r965_ablation_previous_cursor_"+(source.isEmpty()?"LEGACY":source),
                        "cursor="+store.getState("r932_ablation_cursor","")+"|current="+store.getState("r932_ablation_current","")+"|module="+store.getState("r932_ablation_module","0")+"|NOT_REPLAYED_WITH_NEW_CODE");
                state.put("r965_ablation_source",SourceManifestR815.SOURCE_TREE_SHA256);
                state.put("r932_ablation_current","");state.put("r932_ablation_cursor","");state.put("r932_ablation_module","0");
                DbStore.putStatesR36(db,state);
            }}))throw new IllegalStateException("R965_ABLATION_SOURCE_BOUNDARY_COMMIT_FAILED");
        }
        String current=store.getState("r932_ablation_current",""),cursor=store.getState("r932_ablation_cursor","");int module=0;
        try{module=Integer.parseInt(store.getState("r932_ablation_module","0"));}catch(NumberFormatException ignored){}
        if(module<0||module>=ModuleReplayR932.Module.values().length)throw new IllegalStateException("R932_ABLATION_MODULE_CURSOR");
        byte[] blob=null;String source="",target="";
        String order=current.length()>0?" AND target_issue=?":cursor.length()>0?" AND target_issue>?":"";
        String[] args=current.length()>0?new String[]{current}:cursor.length()>0?new String[]{cursor}:null;
        // First start establishes a current boundary; old missing formats are never fabricated.
        try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT target_issue,snapshot_blob,snapshot_hash FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" j WHERE snapshot_blob IS NOT NULL AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=j.target_issue AND d.dirty=0)"+order+" ORDER BY target_issue "+(current.length()==0&&cursor.length()==0?"DESC":"ASC")+" LIMIT 1",args)) {
            if(c.moveToFirst()){target=c.getString(0);blob=c.getBlob(1);source=c.getString(2);}
        }
        if(blob==null)return "WAIT_FROZEN_MODULE_INPUT";
        final String issue=target,hash=source;final int ordinal=module;JSONObject result;boolean blocked=false;
        try {
            if(!hash.equals(DeferredEvidenceSnapshotR36.sha256(blob)))throw new IllegalStateException("R932_ABLATION_SNAPSHOT_HASH");
            DeferredEvidenceSnapshotR36.Snapshot snapshot=DeferredEvidenceSnapshotR36.decode(blob);
            if(!issue.equals(snapshot.targetIssue))throw new IllegalStateException("R932_ABLATION_SNAPSHOT_TARGET");
            ResearchWorkBudgetR927.checkpoint();result=analyze(snapshot.proofs,hash,store.getDrawByIssue(issue),ModuleReplayR932.Module.values()[module]);
        }catch(ResearchWorkBudgetR927.Yield yielded){throw yielded;}
        catch(Exception error){
            blocked=true;result=new JSONObject();try{result.put("target_issue",issue);result.put("module",ModuleReplayR932.Module.values()[module].name());result.put("state","BLOCKED_INPUT_OR_PARITY");result.put("reason",error.getClass().getSimpleName()+":"+error.getMessage());result.put("source_hash",hash);result.put("may_authorize_real",false);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
        }
        final JSONObject output=result;final boolean finished=blocked||module==ModuleReplayR932.Module.values().length-1;
        boolean ok=store.coordinatorR36().runAtomicWrite("R932_ABLATION_RESULT_AND_CURSOR",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            try{ModuleCalibrationR932.enrich(db,output);}catch(JSONException error){throw new IllegalStateException("R932_MODULE_CAL_SERIALIZATION",error);}
            String text=output.toString(),resultHash=V51ChecksumEngine.sha256(text),name=ModuleReplayR932.Module.values()[ordinal].name(),state=output.optString("state");
            // A successful replay may have a different measured cost on retry. First result wins.
            db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{issue,name,hash,SourceManifestR815.SOURCE_TREE_SHA256,state,text,resultHash,System.currentTimeMillis()});
            Map<String,String> next=new LinkedHashMap<String,String>();next.put("r932_ablation_current",finished?"":issue);next.put("r932_ablation_module",finished?"0":String.valueOf(ordinal+1));if(finished)next.put("r932_ablation_cursor",issue);DbStore.putStatesR36(db,next);
        }});if(!ok)throw new IllegalStateException("R932_ABLATION_RESULT_COMMIT_FAILED");
        return hasRunnableWork(store.readable())?"MODULE_PENDING":"MODULE_RESEARCH_REVIEWED";
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 out=new com.ethk3.novaonepro.ReportTextCP12(report, "\n同输入模块消融（最近240条已重跑样本）\n");
        for(ModuleReplayR932.Module module:ModuleReplayR932.Module.values())if(module!=ModuleReplayR932.Module.BASELINE) {
            Summary s=summarySnapshotCP4(report, store,module);out.append(module.name()).append("：N=").append(s.cases).append("，原失败样本=").append(s.failures).append('/').append(MIN_FAILURE_SUPPORT);
            out.append(s.sufficient?"，平均命中变化="+s.meanDeltaHit:"，不足以归因");out.append("，校准配对N=").append(s.brierCases).append(s.sufficient&&s.brierCases>=MIN_FAILURE_SUPPORT?"，平均ΔBrier="+s.meanDeltaBrier:"，ΔBrier支持不足").append('\n');
        }
        return out.append("未激活模块另记负对照；缺冻结输入/原结果复现失败不计样本。ΔBrier使用严格更早120拟合/1隔离/120验证的模块专用校准；不足或验证失败显示缺失。消融不是未来晋级证据。\n").toString();
    }

public static Summary summarySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,ModuleReplayR932.Module module) {
        int n=0,failures=0,brierN=0;double sum=0,brierSum=0;
        try(Cursor exists=report.select("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){if(!exists.moveToFirst())return new Summary(0,0,0);}
        try(Cursor c=report.select("SELECT payload,payload_hash FROM "+TABLE+" WHERE code_hash=? AND module=? AND state='REEXECUTED' ORDER BY target_issue DESC LIMIT 240",new String[]{SourceManifestR815.SOURCE_TREE_SHA256,module.name()})) {
            while(c.moveToNext()) {
                if(!com.ethk3.novaonepro.ReportTextCP12.sha256(report, c.getString(0)).equals(c.getString(1)))throw new IllegalStateException("R932_ABLATION_REPORT_HASH");
                try{JSONObject row=new JSONObject(c.getString(0));n++;if(row.getBoolean("original_failure"))failures++;sum+=row.getInt("delta_hit");if(row.has("delta_brier")&&!row.isNull("delta_brier")){double delta=row.getDouble("delta_brier");if(!Double.isFinite(delta))throw new IllegalStateException("R932_ABLATION_REPORT_NONFINITE");brierN++;brierSum+=delta;}}catch(JSONException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
            }
        }
        return new Summary(n,failures,sum,brierN,brierSum);
    }
}
