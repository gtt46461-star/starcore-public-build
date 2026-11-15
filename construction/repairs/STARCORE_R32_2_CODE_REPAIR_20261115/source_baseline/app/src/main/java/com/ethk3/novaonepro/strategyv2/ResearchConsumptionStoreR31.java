package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.util.*;

/** The original BORN transaction owns action receipts. SELECT ablations are
 * descriptive development evidence and never change credit or certificates. */
public final class ResearchConsumptionStoreR31 {
    private ResearchConsumptionStoreR31(){}
    public static final String VERSION="R31_RESEARCH_ACTION_CONSUMER_V1";
    public static final String ABLATION="RESEARCH_SOURCE_ABLATION_R31";
    static Map<String,Object> map(){return new LinkedHashMap<>();}
    static long number(Map<?,?> m,String key){Object value=m.get(key);if(!(value instanceof Number))throw new IllegalArgumentException("R31_ACTION_NUMBER:"+key);return ((Number)value).longValue();}
    static String string(Map<?,?> m,String key){Object value=m.get(key);if(!(value instanceof String)||((String)value).isEmpty())throw new IllegalArgumentException("R31_ACTION_STRING:"+key);return (String)value;}
    @SuppressWarnings("unchecked") static Map<String,Object> object(Object raw){if(!(raw instanceof Map))throw new IllegalArgumentException("R31_ACTION_OBJECT");return (Map<String,Object>)raw;}

    /** Canonical originals sort their keys. Authenticate only the top-level
     * small contract without allocating/parsing the original matrix again. */
    static boolean originalContractMatches(String original,Map<String,Object> contract){
        String token="\"sourceContract\":",identity=JsonR27.write(contract);int depth=0;
        for(int i=0;i<original.length();i++){
            char c=original.charAt(i);
            if(c=='\"'){
                if(depth==1&&original.startsWith(token,i)){
                    int start=i+token.length(),end=start+identity.length();
                    return end<original.length()&&original.regionMatches(start,identity,0,identity.length())&&(original.charAt(end)==','||original.charAt(end)=='}');
                }
                for(i++;i<original.length();i++){char quoted=original.charAt(i);if(quoted=='\\')i++;else if(quoted=='\"')break;}
            }else if(c=='{'||c=='[')depth++;
            else if(c=='}'||c==']')depth--;
        }
        return false;
    }

    /** Same family and seed, one proposal per arm and the same declared cost
     * ceiling. Both policies are evaluated on the complete original SELECT
     * calendar; a different target or unsupported counterfactual is not a gain. */
    static Map<String,Object> ablation(PolicySpecR27 child,Map<String,Object> recipe,EvalContextR27 ctx,long budget){
        Map<String,Object> out=map();out.put("protocol",VERSION);out.put("role","SELECT");out.put("formalEvidenceInherited",false);out.put("naturalQuality","NOT_PROVEN");
        out.put("sourceRun",recipe.get("sourceRun"));out.put("seed",recipe.get("seed"));out.put("family",recipe.get("family"));out.put("proposalAttemptsWith",1);out.put("proposalAttemptsWithout",1);out.put("priorAssetsFixed",true);out.put("ablationScope","ONE_PROPOSAL_DIRECT_SOURCE_REMOVAL_WITH_FIXED_PAST_ASSETS");
        out.put("budgetWith",budget);out.put("budgetWithout",budget);out.put("withHash",child.policyHash());out.put("datasetHash",ctx.data.datasetHash);
        out.put("fromOrdinal",ctx.data.ordinals[ctx.split.selectFrom]);out.put("throughOrdinal",ctx.data.ordinals[ctx.split.selectTo-1]);
        if(!(recipe.get("withoutSpec") instanceof String)){out.put("state","UNSUPPORTED_COUNTERFACTUAL_PRODUCT");out.put("effectiveness","NOT_EVALUATED");return out;}
        PolicySpecR27 without=PolicySpecR27.parse((String)recipe.get("withoutSpec"));out.put("withoutHash",without.policyHash());
        if(child.position!=without.position){out.put("state","NOT_COMPARABLE_DIFFERENT_TARGET");out.put("effectiveness","NOT_EVALUATED");return out;}
        long withCost=CreationBudgetR31.costUnits(child,ctx.split.selectSize()),withoutCost=CreationBudgetR31.costUnits(without,ctx.split.selectSize());
        out.put("withDeclaredReplayUnits",withCost);out.put("withoutDeclaredReplayUnits",withoutCost);out.put("budgetScope","EQUAL_ALLOCATED_COST_CEILINGS_NOT_EQUAL_OBSERVED_CPU");
        if(budget<=0||withCost>budget||withoutCost>budget){out.put("state","WAIT_DECLARED_REPLAY_BUDGET");out.put("effectiveness","NOT_EVALUATED");return out;}
        if(ctx.labels!=ctx.data.draws)throw new IllegalArgumentException("R31_ACTION_ORIGINAL_SELECT_REQUIRED");
        ResearchGainR30R1 gain=FusionStudyR30R1.gain(without,child,ctx);ResearchGainR30R1.Estimate e=gain.estimate();
        out.put("state","MEASURED_SELECT_ONLY");out.put("effectiveness",ResearchResultContractR31.effectiveness(e,budget,budget));
        out.put("intervalScope",e.status);out.put("analysisContractHash",e.analysisContractHash);out.put("evaluatedCalendarIssues",e.evaluatedIssues);out.put("completeBlocks",e.completeBlocks);
        out.put("meanGain",e.meanBlockGain);out.put("lower95",e.lower95);out.put("upper95",e.upper95);out.put("sourceRemovedFromCounterfactual",true);
        return out;
    }

    /** Reads the staged immutable action plan, rechecks actual source and child,
     * then decorates the existing source-consumption receipt in the same writer. */
    public static void completeWithin(SQLiteDatabase db,String run,String proposal,String child,int generation,String consumer,
            Map<?,?> proposalPayload,String originalSource,Map<String,Object> link){
        if(!db.inTransaction())throw new IllegalStateException("R31_ACTION_EXISTING_WRITER_REQUIRED");
        Object raw=proposalPayload.get("actionContractR31");
        if(!(raw instanceof Map)){link.put("resultContractR31",Collections.singletonMap("state","LEGACY_UNVALIDATED_ACTION_PRESERVED"));return;}
        Map<String,Object> plan=object(raw),contract=object(object(plan.get("sourceContracts")).get(run));
        String sourceInput=string(object(plan.get("sourceInputs")),run);
        Map<String,Object> views=object(plan.get("sourceViews")),hashes=object(plan.get("sourceHashes"));
        if(!StatsR27.sha256(originalSource).equals(hashes.get(run)))throw new IllegalStateException("R31_ACTION_PRODUCT_CHANGED");
        if(!originalContractMatches(originalSource,contract))throw new IllegalStateException("R31_ACTION_SOURCE_CONTRACT_CHANGED");
        PolicySpecR27 actual;
        try(Cursor c=db.rawQuery("SELECT spec_json,birth_generation,dataset_hash,state FROM r27_policy WHERE policy_hash=?",new String[]{child})){
            if(!c.moveToFirst()||c.getInt(1)!=generation||!string(plan,"datasetHash").equals(c.getString(2))||!"BORN".equals(c.getString(3)))throw new IllegalStateException("R31_ACTION_ACTUAL_BORN_IDENTITY");
            actual=PolicySpecR27.parse(c.getString(0));if(!child.equals(actual.policyHash()))throw new IllegalStateException("R31_ACTION_CHILD_HASH");
        }
        String before=string(plan,"beforeHash");
        if(actual.meta!=null&&!actual.meta.parents.isEmpty()){
            if(!before.equals(actual.meta.parents.get(0)))throw new IllegalStateException("R31_ACTION_PARENT_SUBSTITUTED");
            try(Cursor c=db.rawQuery("SELECT spec_json FROM r27_policy WHERE policy_hash=?",new String[]{before})){if(!c.moveToFirst()||!before.equals(PolicySpecR27.parse(c.getString(0)).policyHash()))throw new IllegalStateException("R31_ACTION_PARENT_ORIGINAL_MISSING");}
        }else if(!before.equals(StatsR27.sha256("R31_ABSENT_CANDIDATE_BEFORE_BIRTH|"+child)))throw new IllegalStateException("R31_ACTION_ABSENCE_IDENTITY");
        if(!BuildConfig.SOURCE_COMMIT_R26.equals(contract.get("source")))throw new IllegalStateException("R31_ACTION_SOURCE_COMMIT");
        if(!Objects.equals(sourceInput,contract.get("input")))throw new IllegalStateException("R31_ACTION_SOURCE_INPUT_HASH");
        if(number(contract,"available")>number(plan,"preparedAt"))throw new IllegalStateException("R31_ACTION_SOURCE_AVAILABLE_AFTER_PREPARATION");
        Map<String,Object> receipts=map();receipts.put("source_run",run);receipts.put("input_hash",sourceInput);receipts.put("role",string(contract,"role"));receipts.put("abi",string(contract,"abi"));
        receipts.put("source_revision",number(contract,"revision"));receipts.put("consumer_version",VERSION+":"+consumer);receipts.put("before_hash",before);receipts.put("after_hash",child);
        receipts.put("budget",number(plan,"budget"));receipts.put("product_hash",StatsR27.sha256(originalSource));
        String view=string(views,run),target="POSITION:"+actual.position;
        ResearchResultContractR31.Result result=new ResearchResultContractR31.Result(view,ResearchResultContractR31.ProductState.PRODUCED,
            string(contract,"dataset"),number(contract,"revision"),string(contract,"abi"),string(contract,"role"),target,before,number(contract,"through"),receipts);
        ResearchResultContractR31.Expect expected=new ResearchResultContractR31.Expect(string(plan,"datasetHash"),number(plan,"sourceRevision"),FrozenConfirmationR30.currentSemantics(),string(plan,"target"),string(plan,"beforeHash"),number(plan,"decisionOrdinal"));
        String key=ResearchResultContractR31.validateForConsumption(result,expected,consumer,child,Collections.emptySet());
        try(Cursor c=db.rawQuery("SELECT 1 FROM r30r1_research_measure WHERE run_id=? AND measure_key=? LIMIT 1",new String[]{run,view})){
            if(!c.moveToFirst())throw new IllegalStateException("R31_ACTION_ORIGINAL_VIEW_MISMATCH");
        }
        Map<String,Object> typed=map();typed.put("version",VERSION);typed.put("validated",true);typed.put("consumeKey",key);typed.put("view",view);typed.put("receipts",receipts);typed.put("state","ACTUAL_BORN_CONSUMPTION_NOT_EFFECTIVENESS");
        typed.put("datasetHash",result.datasetHash);typed.put("target",target);typed.put("decisionOrdinal",number(plan,"decisionOrdinal"));typed.put("latestLabelOrdinal",result.latestLabelOrdinal);
        typed.put("usages",plan.get("usages"));link.put("resultContractR31",typed);
        Object ablations=plan.get("ablations");if(ablations instanceof List)for(Object item:(List<?>)ablations){Map<String,Object> measurement=object(item);if(!run.equals(measurement.get("sourceRun")))continue;
            if(!child.equals(measurement.get("withHash"))||!expected.datasetHash.equals(measurement.get("datasetHash")))throw new IllegalStateException("R31_ACTION_ABLATION_IDENTITY");
            if(number(measurement,"budgetWith")!=number(plan,"budget")||number(measurement,"budgetWithout")!=number(plan,"budget"))throw new IllegalStateException("R31_ACTION_ABLATION_BUDGET");
            String json=JsonR27.write(measurement);
            db.execSQL("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{run,"SOURCE_ABLATION:"+proposal,ABLATION,child,"SELECT_ONLY_SAME_ALLOCATED_BUDGET",measurement.get("fromOrdinal"),measurement.get("throughOrdinal"),measurement.getOrDefault("evaluatedCalendarIssues",0),measurement.get("meanGain"),measurement.get("lower95"),measurement.get("upper95"),json});
        }
    }
    /** Bounded read-only report consumer. Unknown schema is never zero success. */
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=map();out.put("version",VERSION);out.put("state","UNKNOWN");out.put("scope","CURRENT_SOURCE_SELECT_RESEARCH_ACTIONS_NOT_REAL_OR_NATURAL_QUALITY");
        try(Cursor c=db.rawQuery("SELECT count(*),sum(CASE WHEN json_extract(m.payload_json,'$.resultContractR31.validated')=1 THEN 1 ELSE 0 END) FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind='RESEARCH_SOURCE_CONSUMPTION' AND r.source_commit=?",new String[]{BuildConfig.SOURCE_COMMIT_R26})){
            if(c.moveToFirst()){out.put("sourceConsumptionReceipts",c.getLong(0));out.put("validatedActionReceipts",c.isNull(1)?0:c.getLong(1));out.put("state","OBSERVED_RECEIPTS_ONLY");}
        }catch(RuntimeException unavailable){out.put("reason","ACTION_RECEIPTS_UNAVAILABLE");return out;}
        List<Object> effects=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT m.run_id,m.measure_key,m.payload_json FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind=? AND r.source_commit=? ORDER BY r.recorded_at DESC,m.measure_key LIMIT 8",new String[]{ABLATION,BuildConfig.SOURCE_COMMIT_R26})){
            while(c.moveToNext()){String original=c.getString(2);if(original==null||original.length()>16384)throw new IllegalStateException("R31_ACTION_ABLATION_RECEIPT_BOUND");Map<String,Object> raw=object(JsonR27.parse(original)),row=map();row.put("sourceRun",c.getString(0));row.put("receiptSha256",StatsR27.sha256(original));for(String field:Arrays.asList("state","effectiveness","withHash","withoutHash","budgetWith","budgetWithout","evaluatedCalendarIssues","lower95","upper95","intervalScope"))if(raw.containsKey(field))row.put(field,raw.get(field));effects.add(row);}
            out.put("lastAblations",effects);out.put("ablationScope","SAME_FAMILY_SEED_PROPOSAL_ATTEMPTS_AND_DECLARED_COST_CEILINGS; DESCRIPTIVE_SELECT_ONLY");
        }catch(RuntimeException unavailable){out.put("ablationState","UNKNOWN_RECEIPTS_UNAVAILABLE");}
        return Collections.unmodifiableMap(out);
    }
}
