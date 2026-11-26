package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.LifecycleSchemaR31;
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

    private static void completeRuleUseWithinR342(SQLiteDatabase db,String run,String proposal,String child,
            int generation,Map<?,?> product,Map<String,Object> link){
        String id=ResearchContractR30R1.sha(String.valueOf(product.get("typedRule")));
        String dataset=ResearchContractR30R1.sha(String.valueOf(product.get("ruleTransferDataset")));
        TypedGenerationRuleR342 rule;
        try(Cursor row=db.rawQuery("SELECT canonical FROM "+AtomPublicationStoreR31.RULES_R342
                +" WHERE rule_id=?",new String[]{id})){
            if(!row.moveToFirst())throw new ResearchStoreR30R1.StaleResearch();
            rule=TypedGenerationRuleR342.decode(row.getString(0));
        }
        if(!id.equals(rule.ruleId)||!FrozenConfirmationR30.currentSemantics().equals(rule.abi)
                ||ResearchInputPinR30R1.admittedRevisionR341(db,rule.dataset)!=rule.revision)throw new ResearchStoreR30R1.StaleResearch();
        try(Cursor source=db.rawQuery("SELECT dataset_hash,source_revision,from_ordinal,through_ordinal "
                +"FROM r30r1_analysis_run WHERE run_id=?",new String[]{run})){
            if(!source.moveToFirst()||!dataset.equals(source.getString(0))
                    ||ResearchInputPinR30R1.admittedRevisionR341(db,dataset)!=source.getLong(1))throw new ResearchStoreR30R1.StaleResearch();
            boolean derived=false;
            for(PolicySpecR27 exact:rule.instantiate(dataset,source.getLong(2),source.getLong(3),generation))
                if(exact.policyHash().equals(child))derived=true;
            if(!derived)throw new IllegalStateException("R342_RULE_BORN_CHILD_IDENTITY");
        }
        String receipt=HashingSU1.fields("R342_RULE_BORN_USE",id,dataset,run,proposal,child,Integer.toString(generation));
        db.execSQL("INSERT INTO "+AtomPublicationStoreR31.RULE_USES_R342+" VALUES(?,?,?,?,?,?,?,?)",
                new Object[]{proposal,id,child,dataset,generation,run,receipt,System.currentTimeMillis()});
        link.put("typedRule",id);link.put("typedRuleUseReceipt",receipt);link.put("firstEvaluation","PENDING_ORIGINAL_EVOLUTION_EVALUATOR");
    }

    /** Reads the staged immutable action plan, rechecks actual source and child,
     * then decorates the existing source-consumption receipt in the same writer. */
    public static void completeWithin(SQLiteDatabase db,String run,String proposal,String child,int generation,String consumer,
            Map<?,?> proposalPayload,String originalSource,Map<String,Object> link){
        if(!db.inTransaction())throw new IllegalStateException("R31_ACTION_EXISTING_WRITER_REQUIRED");
        if(proposalPayload.containsKey("typedRule")){
            completeRuleUseWithinR342(db,run,proposal,child,generation,proposalPayload,link);
            return;
        }
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
    private static final String TYPED_RULE_CATALOG_R343 = "SELECT count(*),count(CASE WHEN abi=? THEN 1 END) FROM "
            + AtomPublicationStoreR31.RULES_R342;
    private static final String TYPED_RULE_TRANSFER_R343 = "SELECT p.proposal_id,p.state,p.run_id,a.dataset_hash,"
            + "g.rule_id,CASE WHEN json_valid(p.payload_json) THEN json_extract(p.payload_json,'$.candidateHash') END AS child_hash"
            + " FROM r30r1_proposal p JOIN r30r1_analysis_run a ON a.run_id=p.run_id"
            + " JOIN " + AtomPublicationStoreR31.RULES_R342 + " g ON g.rule_id=CASE"
            + " WHEN json_valid(p.payload_json) THEN json_extract(p.payload_json,'$.typedRule') END"
            + " WHERE p.kind='TYPED_GENERATION_RULE' AND a.status='COMMITTED' AND a.role='SELECT'"
            + " AND a.source_commit=? AND a.dataset_hash=CASE WHEN json_valid(p.payload_json)"
            + " THEN json_extract(p.payload_json,'$.ruleTransferDataset') END"
            + " AND a.dataset_hash<>g.dataset_hash AND a.from_ordinal>g.through_ordinal";
    private static final String TYPED_RULE_BORN_R343 = "SELECT u.child_hash,u.dataset_hash,u.created_at FROM "
            + AtomPublicationStoreR31.RULE_USES_R342 + " u"
            + " JOIN transfer p ON p.proposal_id=u.proposal_id AND p.run_id=u.source_run"
            + " AND p.rule_id=u.rule_id AND p.child_hash=u.child_hash AND p.dataset_hash=u.dataset_hash"
            + " JOIN r30r1_consume c ON c.proposal_id=u.proposal_id AND c.child_policy_hash=u.child_hash"
            + " AND c.source_generation=u.generation AND c.consumer_version='ORIGINAL_BIRTH_GATE_R30_REPAIR1'"
            + " JOIN r30r1_research_measure m ON m.run_id=u.source_run AND m.measure_key='CONSUMED_BY:'||u.proposal_id"
            + " AND m.kind='RESEARCH_SOURCE_CONSUMPTION' AND m.parent_hash=u.child_hash"
            + " AND m.block_id='ORIGINAL_BIRTH_GATE_RECEIPT'"
            + " WHERE p.state='CONSUMED' AND CASE WHEN json_valid(m.payload_json) THEN"
            + " json_extract(m.payload_json,'$.typedRuleUseReceipt')=u.receipt_hash"
            + " AND json_extract(m.payload_json,'$.receiptHash')=c.receipt_hash END";
    private static final String TYPED_RULE_PROPOSALS_R343 = "WITH transfer AS (" + TYPED_RULE_TRANSFER_R343
            + ") SELECT count(*),count(DISTINCT dataset_hash) FROM transfer";
    private static final String TYPED_RULE_RESULTS_R343 = "WITH transfer AS (" + TYPED_RULE_TRANSFER_R343
            + "), born AS (" + TYPED_RULE_BORN_R343 + ") SELECT"
            + " (SELECT count(DISTINCT child_hash) FROM born),"
            + " (SELECT count(DISTINCT b.child_hash) FROM born b JOIN r27_replay_score s"
            + " ON s.policy_hash=b.child_hash AND s.dataset_hash=b.dataset_hash AND s.split=?"
            + " AND s.n_sched>0 AND s.computed_at>=b.created_at)";

    /** Retained originals for R343-028. Missing observations have no numeric denominator. */
    static Map<String,Object> typedRuleReportR343(SQLiteDatabase db) {
        Map<String,Object> stages = map();
        stages.put("eligibleDonorPairs", typedRuleStageR343(null, "DONOR_PAIR_ATTEMPT_NOT_PERSISTED"));
        stages.put("incompatibleTypedSlotPairs", typedRuleStageR343(null, "INDUCTION_REJECTION_NOT_PERSISTED"));
        stages.put("rawChildProposals", typedRuleStageR343(null, "PRE_STAGE_GENERATOR_OUTPUT_NOT_PERSISTED"));
        stages.put("independentFutureResults", typedRuleStageR343(null, "NO_RULE_USE_TO_INDEPENDENT_FUTURE_BINDING"));
        Map<String,Object> result = map();
        result.put("schema", "R343_TYPED_GENERATION_EVIDENCE_V1");
        result.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26);
        result.put("scope", "RETAINED_RULE_CATALOG_ALL_SOURCES_AND_CURRENT_SOURCE_TRANSFERS");
        result.put("developmentScope", "RETAINED_TARGET_DATASET_H_SELECT_SCORE_AT_OR_AFTER_USE");
        result.put("stages", stages);
        boolean ready = false;
        try {
            LifecycleSchemaR31.requireDefinitions(db, AtomPublicationStoreR31.ruleDdlR342(), "R342_TYPED_RULE");
            ready = true;
        } catch (DatabaseCoordinator.SchemaPendingR31 missing) {
            // An incomplete schema cannot prove an observed empty set.
        }
        readTypedRuleStagesR343(db, ready, stages, TYPED_RULE_CATALOG_R343,
                new String[]{FrozenConfirmationR30.currentSemantics()}, "retainedRulesAllSources", "retainedRulesCurrentAbi");
        readTypedRuleStagesR343(db, ready, stages, TYPED_RULE_PROPOSALS_R343,
                new String[]{BuildConfig.SOURCE_COMMIT_R26}, "persistedTransferProposals", "distinctTransferDatasets");
        readTypedRuleStagesR343(db, ready, stages, TYPED_RULE_RESULTS_R343,
                new String[]{BuildConfig.SOURCE_COMMIT_R26, SplitR27.SELECT}, "exactConsumedChildren", "childrenWithRetainedDevScores");
        return result;
    }

    private static void readTypedRuleStagesR343(SQLiteDatabase db, boolean ready, Map<String,Object> stages,
            String sql, String[] args, String... keys) {
        String reason = ready ? "ORIGINAL_READ_FAILED" : "ORIGINAL_SCHEMA_UNAVAILABLE";
        if (ready) {
            try (Cursor row = db.rawQuery(sql, args)) {
                if (row.moveToFirst()) {
                    for (int i = 0; i < keys.length; i++) stages.put(keys[i], typedRuleStageR343(row.getLong(i), null));
                    return;
                }
            } catch (SQLiteException unreadable) {
                // Preserve the observations obtained from the other original tables.
            }
        }
        for (String key : keys) stages.put(key, typedRuleStageR343(null, reason));
    }

    private static Map<String,Object> typedRuleStageR343(Long count, String reason) {
        Map<String,Object> stage = map();
        stage.put("count", count);
        stage.put("status", count == null ? "UNKNOWN" : "OBSERVED_RETAINED_ROWS");
        if (reason != null) stage.put("reason", reason);
        return stage;
    }

    /** Bounded read-only report consumer. Unknown schema is never zero success. */
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=map();out.put("version",VERSION);out.put("nonBornActions",nonBornReportR335(db));out.put("typedGenerationR343",typedRuleReportR343(db));out.put("state","UNKNOWN");out.put("scope","CURRENT_SOURCE_SELECT_RESEARCH_ACTIONS_NOT_REAL_OR_NATURAL_QUALITY");
        try(Cursor c=db.rawQuery("SELECT count(*),sum(CASE WHEN json_extract(m.payload_json,'$.resultContractR31.validated')=1 THEN 1 ELSE 0 END) FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind='RESEARCH_SOURCE_CONSUMPTION' AND r.source_commit=?",new String[]{BuildConfig.SOURCE_COMMIT_R26})){
            if(c.moveToFirst()){out.put("sourceConsumptionReceipts",c.getLong(0));out.put("validatedActionReceipts",c.isNull(1)?0:c.getLong(1));out.put("state","OBSERVED_RECEIPTS_ONLY");}
        }catch(RuntimeException unavailable){out.put("reason","ACTION_RECEIPTS_UNAVAILABLE");return out;}
        List<Object> effects=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT m.run_id,m.measure_key,m.payload_json FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind=? AND r.source_commit=? ORDER BY r.recorded_at DESC,m.measure_key LIMIT 8",new String[]{ABLATION,BuildConfig.SOURCE_COMMIT_R26})){
            while(c.moveToNext()){String original=c.getString(2);if(original==null||original.length()>16384)throw new IllegalStateException("R31_ACTION_ABLATION_RECEIPT_BOUND");Map<String,Object> raw=object(JsonR27.parse(original)),row=map();row.put("sourceRun",c.getString(0));row.put("receiptSha256",StatsR27.sha256(original));for(String field:Arrays.asList("state","effectiveness","withHash","withoutHash","budgetWith","budgetWithout","evaluatedCalendarIssues","lower95","upper95","intervalScope"))if(raw.containsKey(field))row.put(field,raw.get(field));effects.add(row);}
            out.put("lastAblations",effects);out.put("ablationScope","SAME_FAMILY_SEED_PROPOSAL_ATTEMPTS_AND_DECLARED_COST_CEILINGS; DESCRIPTIVE_SELECT_ONLY");
        }catch(RuntimeException unavailable){out.put("ablationState","UNKNOWN_RECEIPTS_UNAVAILABLE");}
        return Collections.unmodifiableMap(out);
    }
 /** Separate exact original actions; a configuration receipt is never a BORN or effectiveness receipt. */
 public static Map<String,Object> nonBornReportR335(SQLiteDatabase db){Map<String,Object> out=map();out.put("scope","PERSISTED_ORIGINAL_ACTIONS_NOT_BIRTH_NOT_NATURAL_QUALITY");out.put("status","NOT_RECORDED");
  try(Cursor q=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r33_research_applied'",null)){if(!q.moveToFirst())return out;}
  List<Object> groups=new ArrayList<>(),sample=new ArrayList<>();
  try(Cursor q=db.rawQuery("SELECT action_kind,view_id,role,count(*) FROM r33_research_applied WHERE source_commit=? GROUP BY action_kind,view_id,role ORDER BY action_kind,view_id,role",new String[]{BuildConfig.SOURCE_COMMIT_R26})){while(q.moveToNext()){Map<String,Object> row=map();row.put("actionKind",q.getString(0));row.put("view",q.getString(1));row.put("role",q.getString(2));row.put("retainedOriginalRows",q.getLong(3));groups.add(row);}}
  long valid=0;try(Cursor q=db.rawQuery("SELECT a.consume_key,a.source_run,a.view_id,a.action_kind,a.before_hash,a.after_hash,a.applied_json,a.product_hash,r.summary_json,a.object_key,a.action_ordinal,a.latest_label_ordinal,a.applied_at FROM r33_research_applied a LEFT JOIN r30r1_analysis_run r ON r.run_id=a.source_run WHERE a.source_commit=? ORDER BY a.applied_at DESC,a.consume_key LIMIT 20",new String[]{BuildConfig.SOURCE_COMMIT_R26})){while(q.moveToNext()){Map<String,Object> row=map();row.put("consumeKey",q.getString(0));row.put("sourceRun",q.getString(1));row.put("view",q.getString(2));row.put("actionKind",q.getString(3));row.put("object",q.getString(9));row.put("actionOrdinal",q.getLong(10));row.put("latestLabelOrdinal",q.getLong(11));row.put("appliedAt",q.getLong(12));String applied=q.getString(6),source=q.getString(8);boolean ok=applied!=null&&source!=null&&applied.length()<=262144&&source.length()<=262144&&!q.getString(4).equals(q.getString(5))&&q.getString(5).equals(StatsR27.sha256(applied))&&q.getString(7).equals(StatsR27.sha256(source))&&ResearchStoreR30R1.currentSource(BuildConfig.SOURCE_COMMIT_R26,source)&&q.getLong(10)>q.getLong(11);row.put("originalHashesAndOrderingValid",ok);if(ok)valid++;sample.add(row);}}
  out.put("status","ORIGINAL_RECEIPTS_READ");out.put("groups",groups);out.put("last20",sample);out.put("sampledRows",sample.size());out.put("sampledValidOriginalHashes",valid);out.put("validityScope","BOUNDED_ORIGINAL_HASH_AND_CAUSAL_ORDER_AUDIT; ALL_RETAINED_COUNTS_ARE_NOT_ALL_VALIDATED_COUNTS");out.put("bornOrQualityCount",false);return Collections.unmodifiableMap(out);
 }

}
