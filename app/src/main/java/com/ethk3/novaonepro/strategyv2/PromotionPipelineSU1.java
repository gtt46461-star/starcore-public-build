package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Sealed results admit a prospective trial; only prospective, paired evidence can issue a certificate. */
public final class PromotionPipelineSU1 {
    public static final String POLICY="SU1_ALL_CALENDAR_K3_CONFIRM_V1";
    private PromotionPipelineSU1() {}
    public static String nullBaseline(SQLiteDatabase db){return sham(db,"","NULL_BASELINE");}
    public static String nullControl(SQLiteDatabase db){return sham(db,"INDEPENDENT_NULL_CONTROL","NULL_CONTROL");}
    public static String shuffledControl(SQLiteDatabase db){return sham(db,"SHUFFLED_LABEL","NULL_CONTROL");}
    private static String sham(SQLiteDatabase db,String tag,String role){
        long now=System.currentTimeMillis();String seed;
        try(Cursor c=db.rawQuery("SELECT registration_hash FROM su1_alpha_root WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("SU1_ALPHA_ROOT");seed=c.getString(0);}
        if(!tag.isEmpty())seed=HashingSU1.fields(seed,tag);
        String execution=tag.equals("SHUFFLED_LABEL")?ShuffledLabelControlSU1.PROTOCOL:NullBaseline.PROTOCOL;
        String program=HashingSU1.text(execution),weights=StrategyRepositorySU1.blob(db,seed,"SU1_NULL_SEED_V1",now),ir=StrategyRepositorySU1.blob(db,execution,"SU1_NULL_PROTOCOL_V1",now),report=StrategyRepositorySU1.blob(db,tag.equals("SHUFFLED_LABEL")?"CAUSAL_WINDOW_LABEL_PERMUTATION_NO_STATIC_FITTED_PARAMETERS":"HISTORY_INDEPENDENT_UNIFORM_K3_NO_TRAINING","SU1_NULL_REPORT_V1",now);
        String train=HashingSU1.fields("SU1_NULL_NO_TRAIN_V1",program),artifact=HashingSU1.fields("SU1_NULL_ARTIFACT_V1",train,weights),id=HashingSU1.fields("SU1_NULL_STRATEGY_V1",artifact,StrategyRepositorySU1.ACTION,role);
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_strategy_version WHERE strategy_id=?",id))return id;
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_program WHERE program_hash=?",program))StrategyRepositorySU1.row(db,"su1_program","program_hash",program,"grammar_hash",program,"input_schema_hash",HashingSU1.text(tag.equals("SHUFFLED_LABEL")?"PK10_10X10_CAUSAL_WINDOW1000":"NO_HISTORY"),"typed_ir_hash",ir,"state_schema_hash",program,"state_byte_limit",0,"operation_limit",1000,"created_at",now);
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_train_protocol WHERE protocol_hash=?",train))StrategyRepositorySU1.row(db,"su1_train_protocol","protocol_hash",train,"program_hash",program,"dataset_hash",HashingSU1.text("NO_DATA"),"split_hash",HashingSU1.text("NO_DATA_SPLIT"),"protocol_json","{\"kind\":\"NO_TRAINING\"}","train_first_sequence",0,"train_last_sequence",1,"validation_first_sequence",2,"validation_last_sequence",2,"target_contract_hash",StrategyRepositorySU1.ACTION,"created_at",now);
        StrategyRepositorySU1.row(db,"su1_model_artifact","artifact_hash",artifact,"protocol_hash",train,"program_hash",program,"model_kind",tag.equals("SHUFFLED_LABEL")?"SU1_SHUFFLED_LABEL_CONTROL":"NULL_BASELINE","encoder_hash",program,"weights_hash",weights,"trained_through_sequence",2,"training_report_hash",report,"created_at",now);
        StrategyRepositorySU1.row(db,"su1_strategy_version","strategy_id",id,"codec","SU1","program_hash",program,"artifact_hash",artifact,"selector_hash",program,"calibration_state","MISSING","action_contract_hash",StrategyRepositorySU1.ACTION,"state_initial_hash",weights,"created_sequence",2,"created_at",now,"strategy_role",role);return id;
    }
    private static String advance(String first,int offset){return TargetIssueResolver.fromSequenceOrdinalSU1(TargetIssueResolver.sequenceOrdinalSU1(first)+offset);}
    public static String register(SQLiteDatabase db,String child,String parent,String family,String firstIssue,long registeredSequence,int planned,String kind,String role){
        if(!db.inTransaction()||!TargetIssueResolver.isValid(firstIssue)||planned<1||planned>1000000)throw new IllegalArgumentException("SU1_COHORT_REGISTRATION");
        HashingSU1.sha(child);if(parent!=null)HashingSU1.sha(parent);HashingSU1.sha(family);double alpha=AlphaLedgerSU1.family(db,family);long ordinal;
        boolean confirm=role.equals("CONFIRM");try(Cursor c=db.rawQuery("SELECT coalesce(max(global_claim_ordinal),?)+1 FROM su1_eval_cohort WHERE family_root_hash=? AND evidence_role"+(confirm?"='CONFIRM'":"<>'CONFIRM'"),new String[]{confirm?"0":"999999",family})){c.moveToFirst();ordinal=c.getLong(0);}
        Map<String,Object> spec=new TreeMap<>();spec.put("revision",POLICY);spec.put("first",firstIssue);spec.put("planned",planned);spec.put("scope","POLICY_K3");spec.put("missing","UNPAIRED_EQUALS_MINUS_ONE");spec.put("minimumCoverage","0x1.e666666666666p-1");spec.put("minimumPracticalGain","0x0.0p0");spec.put("root",family);spec.put("ordinal",ordinal);
        String json=StrictCanonicalJson.write(spec),protocol=HashingSU1.text(json),claim=HashingSU1.fields("SU1_CLAIM_V1",child,String.valueOf(parent),protocol,kind,role),cohort=HashingSU1.fields("SU1_COHORT_V1",claim);long now=System.currentTimeMillis();
        if(confirm)AlphaLedgerSU1.reserveClaim(db,claim,alpha,ordinal);
        StrategyRepositorySU1.row(db,"su1_eval_cohort","cohort_id",cohort,"strategy_id",child,"parent_strategy_id",parent,"claim_id",claim,"evidence_role",role,"claim_kind",kind,"registered_sequence",registeredSequence,"first_issue",Long.parseLong(firstIssue),"planned_opportunities",planned,"protocol_hash",protocol,"protocol_json",json,"family_root_hash",family,"family_alpha",alpha,"global_claim_ordinal",ordinal,"look_schedule_json","["+planned+"]","baseline_hash",HashingSU1.text(NullBaseline.PROTOCOL),"created_at",now);
        StrategyRepositorySU1.row(db,"su1_look_plan","cohort_id",cohort,"look_ordinal",1,"planned_opportunities",planned,"schedule_hash",HashingSU1.text("["+planned+"]"));
        StrategyRepositorySU1.row(db,"su1_cohort_runtime","cohort_id",cohort,"last_issue",advance(firstIssue,planned-1),"policy_revision",POLICY);return cohort;
    }
    /** Strict eligibility gate: persisted sealed opening, four supported blocks, no post-hoc winner selection. */
    public static List<String> fromSealed(SQLiteDatabase db,String campaign,String strategy,String parent,String family,String nextIssue,long registeredSequence,int planned){
        String result;
        try(Cursor c=db.rawQuery("SELECT b.local_locator FROM su1_sealed_open o JOIN su1_blob b ON b.blob_hash=o.result_hash WHERE o.campaign_id=? AND o.strategy_id=? AND b.storage_state='LOCAL'",new String[]{campaign,strategy})){if(!c.moveToFirst())throw new IllegalStateException("SU1_SEALED_OPEN_REQUIRED");result=c.getString(0);}
        Map<String,Object> report=StrictCanonicalJson.readObject(result);int n=((Number)report.get("actions")).intValue(),k=((Number)report.get("hits")).intValue();
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_strategy_version WHERE strategy_id=? AND calibration_state='FROZEN'",strategy))throw new IllegalStateException("SU1_FROZEN_CALIBRATION_REQUIRED");
        if(FoldStabilityFitnessSU1.wilsonLower(k,n)<=.30)throw new IllegalStateException("SU1_SEALED_NOT_ABOVE_NULL");
        List<?> blocks=(List<?>)report.get("blocks");if(blocks.size()!=4)throw new IllegalStateException("SU1_SEALED_BLOCKS");for(Object b:blocks){List<?> pair=(List<?>)b;int bn=((Number)pair.get(0)).intValue(),bk=((Number)pair.get(1)).intValue();if(bn<30||bk/(double)bn<=.30)throw new IllegalStateException("SU1_SEALED_UNSTABLE");}
        // V1 confirm opportunities are a fixed calendar sequence. Selective discoveries
        // remain in research until their complete, full-coverage policy passes a new sealed test.
        // R13-P27: a selective strategy (15-30%) is no longer rejected; it joins the league and is judged by
        // ACT-conditioned live evidence (CertificateSU2). Full-coverage SU1 certificates keep their protocol.
        try(Cursor c=db.rawQuery("SELECT coverage FROM su1_runtime_program WHERE strategy_id=?",new String[]{strategy})){
            if(!c.moveToFirst())throw new IllegalStateException("SU1_RUNTIME_PROGRAM_MISSING");
            if(c.getDouble(0)<.999){if(StrategySchemaSU2.ready(db))ModelStoreSU2.joinLeague(db,"SU1:"+strategy,"SU1",family+"@selective",null,registeredSequence);return new ArrayList<>();}
        }
        List<String> ids=new ArrayList<>();String baseline=nullBaseline(db);ids.add(register(db,strategy,baseline,family,nextIssue,registeredSequence,planned,"VS_NULL_BASELINE","CONFIRM"));if(parent!=null)ids.add(register(db,strategy,parent,family,nextIssue,registeredSequence,planned,"PARENT_CHILD","CONFIRM"));ids.add(register(db,nullControl(db),baseline,family,nextIssue,registeredSequence,planned,"NULL_CONTROL","CONFIRM"));ids.add(register(db,shuffledControl(db),baseline,family,nextIssue,registeredSequence,planned,"NULL_CONTROL","CONFIRM"));return ids;
    }
}
