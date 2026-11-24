package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.ResearchMainlineContributionAuditR922;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import com.ethk3.novaonepro.ReportSourceStateR341;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded follow-up from the immutable report copy. Diagnostic states never synthesize observations. */
final class FieldFollowupR34 {
    private FieldFollowupR34(){}

    static Map<String,Object> capture(SQLiteDatabase db,CancellationSignal cancel,Map<String,Object> source){
        Map<String,Object> out=new LinkedHashMap<>();
        Object checked=source==null?null:source.get("verifiedInstallationR341");
        Object snapshot=source==null?null:source.get("snapshotSha256");
        boolean verified=checked instanceof Map&&"VERIFIED_INSTALLATION".equals(((Map<?,?>)checked).get("state"));
        String scope=verified&&snapshot instanceof String?StatsR27.sha256(JsonR27.write(checked)+"|"+snapshot):"UNVERIFIED_IDENTITY";
        out.put("sourceCommit",verified?((Map<?,?>)checked).get("sourceCommit"):null);
        out.put("snapshotHash",snapshot);
        out.put("scopeId",scope);out.put("verifiedInstallation",checked);
        out.put("trainingRuleSha256",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
        out.put("readAt",System.currentTimeMillis());
        out.put("scope","NEW_REPORT_DATABASE_SNAPSHOT; FIELD_EFFECT_REQUIRES_A_MATCHED_ENABLED_WINDOW");
        Map<String,Object> historical=new LinkedHashMap<>();
        historical.put("sourceCommit","273641e838e0b8dd842b42a67a674e184942a30a");
        historical.put("reportKeys",Arrays.asList("870cd99","89d1cac","fabedd0"));
        historical.put("state","STALE_UNRECHECKED");out.put("historicalBaseline",historical);
        out.put("sameWindowComparison","WAITING_FOR_MATCHED_FIELD_OPPORTUNITY_CALENDAR");
        out.put("training","Current production commits, with captured labels separate from formal future observations.");
        List<Map<String,Object>> sources=new ArrayList<>();
        sources.add(projection(db,cancel,"R34-B02-03","SELECT count(*) AS retainedCleanDraws,min(issue) AS fromIssue,max(issue) AS throughIssue FROM draws WHERE dirty=0"));
        sources.add(projection(db,cancel,"R34-B04-01","SELECT target_issue,base_issue,input_hash,state,returned_wall,settled_wall,label_hash,hit FROM r34_s0_shadow WHERE source_commit=? ORDER BY target_issue DESC LIMIT 32",BuildConfig.SOURCE_COMMIT_R26));
        sources.add(projection(db,cancel,"R34-B06-01","SELECT source_kind,source_id,child_id,child_campaign,target_abi,source_through FROM r34_cold_rebirth WHERE target_abi=? ORDER BY source_through DESC LIMIT 32",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256));
        sources.add(projection(db,cancel,"R34-B07-02","SELECT g.source_key,g.role,g.input_hash,g.gradient_norm,g.parameter_change,g.state_hash,p.model_hash,p.published_at FROM r34_atomnet_optimizer_step g JOIN r31_atomnet_publication p ON p.publication_id=g.publication_id AND p.state_hash=g.state_hash WHERE p.status='PUBLISHED' ORDER BY g.recorded_at DESC LIMIT 32"));
        sources.add(projection(db,cancel,"R34-B07-01","SELECT issue,reason,first_seen_at,recorded_at FROM r34_atomnet_unavailable_label ORDER BY issue DESC LIMIT 32"));
        sources.add(projection(db,cancel,"R34-B07-03","SELECT c.policy_hash,c.model_hash,c.target_ordinal,c.position,c.probability_hash,c.status FROM r30_model_call c JOIN r30_model_artifact a ON a.model_hash=c.model_hash WHERE c.status='MATCHED' AND c.frozen_at>0 ORDER BY c.target_ordinal DESC LIMIT 32"));
        sources.add(projection(db,cancel,"R34-B09-01","SELECT s.policy_hash,c.first_ordinal,c.registered_at,e.global_index,e.alpha,e.n AS independentOpportunities,e.acts,e.hits,e.state FROM r27_eprocess e JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash JOIN r29_qual_calendar c ON c.experiment_id=e.policy_hash WHERE s.status NOT LIKE 'CONTROL_%' ORDER BY c.registered_at DESC LIMIT 32"));
        sources.add(projection(db,cancel,"R34-B09-05","SELECT genome_key,certificate_id,window_start,window_end,acts,hits,lower_bound,coverage FROM su2_certificate ORDER BY window_end DESC LIMIT 32"));
        sources.add(projection(db,cancel,"R341-B11-04","SELECT p.policy_hash,p.spec_json,p.idea_family,p.birth_issue,p.state,p.state_reason,p.dataset_hash,l.parent_hash,l.role AS parent_role,l.operator FROM r27_policy p LEFT JOIN r27_lineage l ON l.child_hash=p.policy_hash ORDER BY p.created_at DESC,p.policy_hash,l.parent_hash LIMIT 32"));
        try{
            if(cancel!=null)cancel.throwIfCanceled();
            List<Map<String,Object>> actual=ResearchMainlineContributionAuditR922.exactRealAttributionR34(db,32);
            Map<String,Object> row=result("R341-B11-02",actual,actual.isEmpty()?"OBSERVED_EMPTY":"EXACT_ORIGINAL_KEYS");
            sources.add(row);
        }catch(OperationCanceledException stopped){throw stopped;}
        catch(RuntimeException unavailable){sources.add(error("R341-B11-02",unavailable));}
        for(Map<String,Object> row:sources)row.put("scopeId",scope);
        out.put("sources",sources);
        out.put("sourceHealth",ReportSourceStateR341.merge(sources));
        Object epoch=source==null?null:source.get("runtimeProcessEpochR341");
        out.put("repairEffectsR341",RepairEffectLedgerR341.read(db,checked instanceof Map?(Map<?,?>)checked:java.util.Collections.emptyMap(),
                epoch instanceof String?(String)epoch:null,cancel));
        return out;
    }

    private static Map<String,Object> projection(SQLiteDatabase db,CancellationSignal cancel,String id,String sql,String... args){
        try(Cursor c=db.rawQuery(sql,args.length==0?null:args,cancel)){
            List<Map<String,Object>> rows=new ArrayList<>();
            while(c.moveToNext()){
                Map<String,Object> row=new LinkedHashMap<>();
                for(int col=0;col<c.getColumnCount();col++){
                    int type=c.getType(col);
                    Object value=type==Cursor.FIELD_TYPE_INTEGER?Long.valueOf(c.getLong(col)):
                        type==Cursor.FIELD_TYPE_FLOAT?Double.valueOf(c.getDouble(col)):type==Cursor.FIELD_TYPE_NULL?null:c.getString(col);
                    row.put(c.getColumnName(col),value);
                }
                rows.add(row);
            }
            return result(id,rows,rows.isEmpty()?"OBSERVED_EMPTY":"READBACK");
        }catch(OperationCanceledException stopped){throw stopped;}
        catch(RuntimeException unavailable){return error(id,unavailable);}
    }
    private static Map<String,Object> error(String id,RuntimeException failure){
        Map<String,Object> result=ReportSourceStateR341.observation(id,"IMMUTABLE_REPORT_COPY","SOURCE_UNAVAILABLE",
                RuntimeFaultEvidenceR31.clean(failure.getMessage(),200),null,null);
        result.put("issueId",id);result.put("rows",null);return result;
    }
    private static Map<String,Object> result(String id,List<Map<String,Object>> rows,String state){
        Map<String,Object> result=ReportSourceStateR341.observation(id,"IMMUTABLE_REPORT_COPY",state,"",rows.size(),null);
        result.put("issueId",id);result.put("rows",rows);return result;
    }
}
