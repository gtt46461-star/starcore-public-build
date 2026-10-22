package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Immutable per-window plans. No predictions, settlements, certificates or REAL are owned here. */
public final class ResearchGatePlanR967 {
    public static final String POLICY="research_gate_policy_r967";
    public static final String PLAN="research_experiment_plan_r967";
    public static final String REQUIRED="research_plan_required_r967";
    public static final String REVISION="R967_FORMAL_PLAN_V1";
    public static final String REASON="FIXED_ENDPOINT_IMMUTABLE_PLAN_R967";
    private ResearchGatePlanR967(){}
private static final String CP3_POLICY_CANONICAL = "{\"allocation_revision\":\"CP3_GLOBAL_LIFETIME_SUMMABLE_V1\",\"alpha_formula\":\"0.05/(j*(j+1));j=global_index+1\",\"baseline_probability\":0.3,\"brier_relative_gate\":\"LTE_SAME_WINDOW_CONSTANT_0_30_EMPIRICAL_BRIER\",\"calibration_revision\":\"R932_CANDIDATE_CAL_LOGISTIC_V1\",\"certificate_valid_issues\":120,\"comparison_reference_required\":true,\"consumer_cap\":0.05,\"crossing_rule\":\"CURRENT_LOG_E_GTE_NEG_LOG_ALPHA\",\"data_roles\":{\"calibration_fit\":120,\"calibration_validation\":120,\"development\":\"TRAINABLE_ONLY\",\"embargo\":1,\"formal\":\"SEALED_NOT_TRAINABLE\"},\"decision_p\":\"MIN_1_EXP_NEG_MAX_LOG_E\",\"duplicate_jaccard_min\":0.98,\"duplicate_paired_advantage_max\":0.0,\"ece_bins\":10,\"eprocess_alternatives\":[0.31,0.32,0.34,0.36,0.4,0.45,0.5,0.6,0.7,0.8,0.9],\"eprocess_weights\":\"UNIFORM_FIXED\",\"heap_increment_bytes_max\":67108864,\"hypotheses_per_freeze_protocol\":1,\"latency_measurements_min\":50,\"latency_p95_ms_max\":700,\"legacy_point_rate_alone_is_certificate\":false,\"max_brier\":0.24,\"max_ece\":0.04,\"max_js_drift\":0.1,\"max_looks\":5,\"max_loss_streak\":12,\"max_residual_drawdown\":8.0,\"min_certificate_n\":360,\"min_coverage\":0.95,\"min_fixed_window_hit_rate\":0.3,\"min_hit_rate\":0.34,\"min_regime_hit_rate\":0.27,\"min_regime_n\":50,\"min_wilson_lower\":0.3,\"min_window_integrity\":1.0,\"missing_policy\":\"KEEP_ORIGINAL_ENDPOINT_PAUSED_CAPTURE_NO_IMPUTATION\",\"mode\":\"STATIC_FROZEN\",\"null_assumption\":\"CONDITIONAL_HIT_PROBABILITY_LE_0_30\",\"planned_endpoints\":[120,360,600,840,1200],\"real_authority\":false,\"renewal_new_future_n\":120,\"resource_unmeasured\":\"BLOCK\",\"restart_policy\":\"AFTER_OLD_ENDPOINT_NEW_PAID_CLAIM_SAME_FAMILY_NO_REFUND\",\"revision\":\"CP3_STATIC_FIXED_POLICY_V1\",\"rolling_hit_rate_min\":{\"100\":0.27,\"20\":0.15,\"300\":0.34,\"50\":0.22},\"statistical_family\":\"CP3_ALL_PRODUCTION\",\"statistics_revision\":\"CP3_PREQUENTIAL_EPROCESS_V1\",\"terminal_look_id\":\"CP3_PROTOCOL_FINAL\",\"total_alpha\":0.05,\"unsupported_regime_policy\":\"EXCLUDE_FROM_CERTIFICATE_DOMAIN_NO_SUPPORTED_GROUP_BLOCKS\",\"wilson_comparison\":\"STRICT_GREATER\",\"wilson_role\":\"IID_DESCRIPTIVE_ADDITIONAL_GATE\",\"window_sizes\":[120,240,240,240,360]}";
public static final class Cp3Options {
    private final JSONObject value;
    private Cp3Options(JSONObject p) throws JSONException { value=new JSONObject(p.toString()); }
    public String text(String key) { try { Object x=value.get(key);if(!(x instanceof String))throw new IllegalStateException("CP3_POLICY_TYPE:"+key);return (String)x;}catch(JSONException e){throw new IllegalStateException("CP3_POLICY_FIELD:"+key,e);} }
    public double number(String key) { try { Object x=value.get(key);if(!(x instanceof Number))throw new IllegalStateException("CP3_POLICY_TYPE:"+key);double v=((Number)x).doubleValue();if(Double.isNaN(v)||Double.isInfinite(v))throw new IllegalStateException("CP3_POLICY_NONFINITE");return v;}catch(JSONException e){throw new IllegalStateException("CP3_POLICY_FIELD:"+key,e);} }
    public int integer(String key) { double v=number(key);if(v!=(int)v)throw new IllegalStateException("CP3_POLICY_INTEGER:"+key);return (int)v; }
    public boolean flag(String key) { try { Object x=value.get(key);if(!(x instanceof Boolean))throw new IllegalStateException("CP3_POLICY_TYPE:"+key);return (Boolean)x;}catch(JSONException e){throw new IllegalStateException("CP3_POLICY_FIELD:"+key,e);} }
    public double[] numbers(String key) { try { JSONArray a=value.getJSONArray(key);double[] out=new double[a.length()];for(int i=0;i<out.length;i++){Object x=a.get(i);if(!(x instanceof Number))throw new IllegalStateException("CP3_POLICY_ARRAY_TYPE");out[i]=((Number)x).doubleValue();}return out;}catch(JSONException e){throw new IllegalStateException("CP3_POLICY_FIELD:"+key,e);} }
    public double rollingMinimum(int window) { try { Object x=value.getJSONObject("rolling_hit_rate_min").get(String.valueOf(window));if(!(x instanceof Number))throw new IllegalStateException("CP3_POLICY_ROLLING_TYPE");return ((Number)x).doubleValue();}catch(JSONException e){throw new IllegalStateException("CP3_POLICY_ROLLING_WINDOW",e);} }
    public int roleCount(String role) { try { return ResearchGatePlanR967.integer(value.getJSONObject("data_roles").get(role),role); }catch(JSONException e){throw new IllegalStateException("CP3_POLICY_ROLE",e);} }
}

    public static void schema(SQLiteDatabase db){
        schemaCp4(db);
        if(TransactionSchemaCacheR941.readyCommitted(db,"ResearchGatePlanR967"))return;
        boolean column=false;
        try(Cursor c=db.rawQuery("PRAGMA table_info("+FixedResearchWindowR932.WINDOW+")",null)){
            while(c.moveToNext())if("formal_plan_revision".equals(c.getString(1)))column=true;
        }
        if(!column)db.execSQL("ALTER TABLE "+FixedResearchWindowR932.WINDOW+" ADD COLUMN formal_plan_revision TEXT NOT NULL DEFAULT 'LEGACY_R932'");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+POLICY+"(policy_hash TEXT PRIMARY KEY,policy_json TEXT NOT NULL,registered_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PLAN+"(window_id TEXT PRIMARY KEY,plan_hash TEXT NOT NULL UNIQUE,policy_hash TEXT NOT NULL,plan_json TEXT NOT NULL,registered_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+REQUIRED+"(window_id TEXT PRIMARY KEY,plan_hash TEXT NOT NULL)");
        for(String table:new String[]{POLICY,PLAN,REQUIRED}){
            for(String operation:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_"+operation.toLowerCase(java.util.Locale.ROOT)+" BEFORE "+operation+" ON "+table+" BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END");
        }
        // REPLACE does not reliably execute DELETE triggers with recursive_triggers disabled.
        // Guard conflicting inserts directly rather than relying on connection PRAGMAs.
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_policy_no_replace BEFORE INSERT ON "+POLICY+" WHEN EXISTS(SELECT 1 FROM "+POLICY+" WHERE policy_hash=NEW.policy_hash) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_plan_no_replace BEFORE INSERT ON "+PLAN+" WHEN EXISTS(SELECT 1 FROM "+PLAN+" WHERE window_id=NEW.window_id OR plan_hash=NEW.plan_hash) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_required_no_replace BEFORE INSERT ON "+REQUIRED+" WHEN EXISTS(SELECT 1 FROM "+REQUIRED+" WHERE window_id=NEW.window_id) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_window_no_replace BEFORE INSERT ON "+FixedResearchWindowR932.WINDOW+" WHEN EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" w WHERE w.formal_plan_revision='"+REVISION+"' AND (w.window_id=NEW.window_id OR (w.candidate_key=NEW.candidate_key AND w.window_no=NEW.window_no))) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_PERMANENT'); END");
        // Window states may advance. Their preregistered denominators and dependencies may not.
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_planned_window_identity BEFORE UPDATE OF window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,formal_plan_revision ON "+FixedResearchWindowR932.WINDOW+" WHEN OLD.formal_plan_revision='"+REVISION+"' OR EXISTS(SELECT 1 FROM "+REQUIRED+" r WHERE r.window_id=OLD.window_id) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_planned_window_delete BEFORE DELETE ON "+FixedResearchWindowR932.WINDOW+" WHEN OLD.formal_plan_revision='"+REVISION+"' OR EXISTS(SELECT 1 FROM "+REQUIRED+" r WHERE r.window_id=OLD.window_id) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_PERMANENT'); END");
        TransactionSchemaCacheR941.markCommitted(db,"ResearchGatePlanR967");
    }

    /** This interpreter supports one statistic and role protocol, not arbitrary JSON-defined code. */
    public static final class Policy {
        public final boolean isCp3;
        public final Cp3Options cp3;
        public final String hash,json,revision;
        public final double totalAlpha,minRate,minLower,maxBrier,maxEce,minDelta,minBlock,consumerCap;
        private final int[] sizes;
        private Policy(String text,String expected){
            hash=V51ChecksumEngine.sha256(text);json=text;
            if(!hash.equals(expected))throw new IllegalStateException("R967_POLICY_HASH_MISMATCH");
            try{
                JSONObject p=new JSONObject(text);
if ("CP3_PREQUENTIAL_EPROCESS_V1".equals(p.optString("statistics_revision", ""))) {
    // Exact canonical bytes reject duplicate keys, changed key types, removed or extra gates.
    if (!CP3_POLICY_CANONICAL.equals(text)) throw new IllegalStateException("CP3_POLICY_FIELDS_OR_VALUE_DRIFT");
    isCp3=true;
    cp3=new Cp3Options(p);
    revision=p.getString("revision");
    totalAlpha=number(p,"total_alpha",.05,.05);
    minRate=number(p,"min_hit_rate",.34,.34);
    minLower=number(p,"min_wilson_lower",.30,.30);
    maxBrier=number(p,"max_brier",.24,.24);
    maxEce=number(p,"max_ece",.04,.04);
    consumerCap=number(p,"consumer_cap",.05,.05);
    minDelta=Double.NaN; minBlock=Double.NaN; // legacy-only sentinel; never a CP3 threshold
    sizes=new int[]{120,240,240,240,360};
    return;
}
isCp3=false; cp3=null;
                fields(p,"revision,mode,statistics_revision,calibration_revision,data_roles,window_sizes,missing_policy,restart_policy,allocation_revision,total_alpha,max_looks,min_hit_rate,min_wilson_lower,max_brier,max_ece,min_paired_delta_exclusive,min_block_lower_exclusive,consumer_cap,real_authority,null_assumption,legacy_point_rate_alone_is_certificate");
                for(String key:new String[]{"revision","mode","statistics_revision","calibration_revision","allocation_revision","missing_policy","restart_policy","null_assumption"})if(!(p.get(key) instanceof String))throw new IllegalStateException("R967_POLICY_TYPE:"+key);
                if(!(p.get("real_authority") instanceof Boolean)||!(p.get("legacy_point_rate_alone_is_certificate") instanceof Boolean))throw new IllegalStateException("R967_POLICY_TYPE:authority");
                revision=p.getString("revision");
                if(!revision.matches("[A-Z0-9_]{1,80}")||!"STATIC_FROZEN".equals(p.getString("mode"))
                    ||!"R932_BOUNDED_PAIRED_AZUMA_V1".equals(p.getString("statistics_revision"))
                    ||!"R932_CANDIDATE_CAL_LOGISTIC_V1".equals(p.getString("calibration_revision"))
                    ||!"R932_GLOBAL_FAMILY_HYPOTHESIS_SUMMABLE_FIVE_LOOKS_V1".equals(p.getString("allocation_revision"))
                    ||p.getBoolean("real_authority")||p.getBoolean("legacy_point_rate_alone_is_certificate"))throw new IllegalStateException("R967_POLICY_INTERPRETER_UNSUPPORTED");
                JSONObject roles=p.getJSONObject("data_roles");
                fields(roles,"development,formal,calibration_fit,embargo,calibration_validation");
                for(String key:new String[]{"calibration_fit","embargo","calibration_validation"})integer(roles.get(key),key);
                integer(p.get("max_looks"),"max_looks");
                if(!"TRAINABLE_ONLY".equals(roles.getString("development"))||!"SEALED_NOT_TRAINABLE".equals(roles.getString("formal"))
                    ||roles.getInt("calibration_fit")!=120||roles.getInt("embargo")!=1||roles.getInt("calibration_validation")!=120
                    ||!"KEEP_ORIGINAL_ENDPOINT_PAUSED_CAPTURE_NO_IMPUTATION".equals(p.getString("missing_policy"))
                    ||!"AFTER_OLD_ENDPOINT_NEW_PAID_CLAIM_SAME_FAMILY_NO_REFUND".equals(p.getString("restart_policy")))throw new IllegalStateException("R967_ROLE_OR_MISSING_POLICY_UNSUPPORTED");
                totalAlpha=number(p,"total_alpha",.05,.05);if(totalAlpha!=.05||p.getInt("max_looks")!=5)throw new IllegalStateException("R967_ALLOCATION_UNSUPPORTED");
                JSONArray a=p.getJSONArray("window_sizes");int[] legacy={120,240,240,240,360};sizes=new int[5];
                if(a.length()!=5)throw new IllegalStateException("R967_WINDOW_SIZES_UNSUPPORTED");
                for(int i=0;i<5;i++){sizes[i]=integer(a.get(i),"window_sizes");if(sizes[i]!=legacy[i])throw new IllegalStateException("R967_WINDOW_SIZES_UNSUPPORTED");}
                minRate=number(p,"min_hit_rate",.34,1);minLower=number(p,"min_wilson_lower",.30,1);
                maxBrier=number(p,"max_brier",0,.24);maxEce=number(p,"max_ece",0,.04);
                minDelta=number(p,"min_paired_delta_exclusive",0,1);minBlock=number(p,"min_block_lower_exclusive",0,1);
                consumerCap=number(p,"consumer_cap",.05,.05);
                if(!"BOUNDED_PAIRED_DELTA_IN_MINUS1_PLUS1_CONDITIONAL_MEAN_NONPOSITIVE".equals(p.getString("null_assumption")))throw new IllegalStateException("R967_NULL_ASSUMPTION_UNSUPPORTED");
            }catch(JSONException e){throw new IllegalStateException("R967_POLICY_INVALID",e);}
        }
        public int planned(int stage){if(stage<1||stage>5)throw new IllegalArgumentException("R967_STAGE");return sizes[stage-1];}
        private static double number(JSONObject p,String field,double low,double high)throws JSONException{
            if(!(p.get(field) instanceof Number))throw new IllegalStateException("R967_POLICY_TYPE:"+field);
            double value=p.getDouble(field);if(!Double.isFinite(value)||value<low||value>high)throw new IllegalStateException("R967_POLICY_RANGE:"+field);return value;
        }
    }
    private static void fields(JSONObject object,String names){
        java.util.Set<String> expected=new java.util.HashSet<String>(java.util.Arrays.asList(names.split(",")));
        java.util.Iterator<String> it=object.keys();while(it.hasNext())if(!expected.remove(it.next()))throw new IllegalStateException("R967_POLICY_FIELDS");
        if(!expected.isEmpty())throw new IllegalStateException("R967_POLICY_FIELDS_MISSING");
    }
    private static int integer(Object value,String field){
        if(!(value instanceof Number))throw new IllegalStateException("R967_POLICY_TYPE:"+field);
        double d=((Number)value).doubleValue();int n=((Number)value).intValue();
        if(!Double.isFinite(d)||d!=n)throw new IllegalStateException("R967_POLICY_INTEGER:"+field);return n;
    }
    public static Policy installed(){return new Policy(ResearchSemanticsGeneratedR967.POLICY_JSON,ResearchSemanticsGeneratedR967.POLICY_HASH);}
    static Policy policy(String text,String hash){return new Policy(text,hash);}

    public static final class Plan {
        public final String hash,windowId,calibrationHash,trainingRuleHash,json;
        public final Policy policy;
        public final long registeredAt;
        private Plan(String h,String window,String cal,String rule,String text,Policy p,long at){hash=h;windowId=window;calibrationHash=cal;trainingRuleHash=rule;json=text;policy=p;registeredAt=at;}
    }

    /** Runs inside the same transaction as window/alpha registration, before any formal pair. */
    static void registerFormalExperiment(SQLiteDatabase db,String window,String key,String structure,String bundle,String selector,
            String first,String last,int planned,int serial,ObservationGenerationR932.Chain chain,boolean newlyInsertedWindow){
        if(!db.inTransaction())throw new IllegalStateException("R967_PLAN_TRANSACTION_REQUIRED");
        schema(db);
        if(!TargetIssueResolver.isValid(first)||!TargetIssueResolver.isValid(last))throw new IllegalArgumentException("R967_PLAN_ISSUE");
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{first})){
            if(c.moveToFirst())throw new IllegalStateException("R967_PLAN_POSTDRAW_REJECTED");
        }
        // Idempotent replay loads stored policy; a later template never mutates an existing plan.
        if(!newlyInsertedWindow){forWindow(db,window,true);return;}
        // Only the existing window owner can declare its own same-transaction INSERT.
        // An already committed window with missing plan bytes may never regenerate them.
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+PLAN+" WHERE window_id=?",new String[]{window})){
            if(c.moveToFirst())throw new IllegalStateException("R967_NEW_WINDOW_PLAN_CONFLICT");
        }
        Policy p=policyForGeneration(db,key,chain);
        if(planned!=p.planned(chain.stage))throw new IllegalStateException("R967_PLAN_DENOMINATOR_MISMATCH");
        String calculatedLast=ResearchEvaluationRunR967.endpoint(key,first,planned);
        if(!calculatedLast.equals(last))throw new IllegalStateException("R967_PLAN_ENDPOINT_MISMATCH");
        String study=CandidateCalibrationR932.study(db,key,structure,bundle,selector),cal="";
        try(Cursor c=db.rawQuery("SELECT model_hash,model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE study_hash=? AND candidate_key=?",new String[]{study,key})){
            if(!c.moveToFirst()||!c.getString(0).equals(V51ChecksumEngine.sha256(c.getString(1)))||c.getString(2).compareTo(first)>=0)throw new IllegalStateException("R967_FORMAL_EARLIER_CALIBRATION_REQUIRED");
            try{JSONObject m=new JSONObject(c.getString(1));
                if(!"R932_CANDIDATE_CAL_LOGISTIC_V1".equals(m.getString("revision"))||!study.equals(m.getString("study_hash")))throw new IllegalStateException("R967_CALIBRATION_CONTRACT_MISMATCH");
            }catch(JSONException e){throw new IllegalStateException("R967_CALIBRATION_CONTRACT_MISMATCH",e);}
            cal=c.getString(0);
        }
        if(p.isCp3){bindCp4(db,window,key,structure,bundle,selector,first,last,planned,chain,cal);return;}
        if(installed().isCp3)throw new IllegalStateException("CP4_LEGACY_PLAN_READ_ONLY");
        // These are the existing irrevocable allocations, not a new alpha budget.
        double alpha=ResearchMultiplicityR932.registerLook(db,chain.claim,window,chain.stage,first,last);
        ResearchMultiplicityR932.ResearchIdentity identity=ResearchMultiplicityR932.identity(db,chain.claim);
        if(identity==null)throw new IllegalStateException("R967_PLAN_RESEARCH_IDENTITY_REQUIRED");
        long now;
        try(Cursor c=db.rawQuery("SELECT registered_at FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id=?",new String[]{window})){
            if(!c.moveToFirst())throw new IllegalStateException("R967_PLAN_WINDOW_REQUIRED");now=c.getLong(0);
        }
        try{
            JSONObject j=new JSONObject();j.put("revision","R967_FORMAL_PLAN_V1");j.put("window_id",window);j.put("candidate_key",key);j.put("structure_hash",structure);
            j.put("bundle_hash",bundle);j.put("selector_hash",selector);j.put("first_issue",first);j.put("last_issue",last);j.put("planned_n",planned);j.put("window_no",serial);
            j.put("stage",chain.stage);j.put("claim_key",chain.claim);j.put("hypothesis_hash",identity.hypothesisHash);j.put("alpha",alpha);
            j.put("policy_hash",p.hash);j.put("calibration_hash",cal);j.put("study_hash",study);j.put("training_rule_hash",TrainingCodeIdentityR964.SHA256);j.put("registered_at",now);
            if(ResearchEvaluationRunR967.versioned(key)){j.put("revision","R969_FORMAL_PLAN_DISJOINT_V2");j.put("evaluation_schedule",ResearchEvaluationRunR967.SPLIT);}
            String planHash=hash(j);
            boolean policyExists=false;
            try(Cursor c=db.rawQuery("SELECT policy_json FROM "+POLICY+" WHERE policy_hash=?",new String[]{p.hash})){
                if(c.moveToFirst()){policyExists=true;if(!p.json.equals(c.getString(0)))throw new IllegalStateException("R967_POLICY_REPLAY_DRIFT");}
            }
            if(!policyExists)db.execSQL("INSERT INTO "+POLICY+" VALUES(?,?,?)",new Object[]{p.hash,p.json,now});
            db.execSQL("INSERT INTO "+PLAN+" VALUES(?,?,?,?,?)",new Object[]{window,planHash,p.hash,j.toString(),now});
            db.execSQL("INSERT INTO "+REQUIRED+" VALUES(?,?)",new Object[]{window,planHash});
            forWindow(db,window,true);
        }catch(JSONException e){throw new IllegalStateException("R967_PLAN_SERIALIZATION",e);}
    }
    private static Policy policyForGeneration(SQLiteDatabase db,String key,ObservationGenerationR932.Chain chain){
        if(chain.stage<=1)return installed();
        try(Cursor cp4=db.rawQuery("SELECT policy_json,policy_hash FROM "+CP4_LIFETIME+" WHERE candidate_key=? AND generation_id=?",new String[]{key,chain.generation})){
            if(cp4.moveToFirst())return policy(cp4.getString(0),cp4.getString(1));
        }
        try(Cursor c=db.rawQuery("SELECT p.policy_hash,g.policy_json FROM "+PLAN+" p JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=p.window_id JOIN "+POLICY+" g ON g.policy_hash=p.policy_hash WHERE w.candidate_key=? AND w.window_no=?",new String[]{key,String.valueOf(chain.start)})){
            if(c.moveToFirst())return policy(c.getString(1),c.getString(0));
        }
        // An old generation may not be reinterpreted with today's template.
        throw new IllegalStateException("R967_LEGACY_GENERATION_REQUIRES_NEW_PAID_PLAN");
    }
    private static String hash(JSONObject j)throws JSONException{
        boolean v2="R969_FORMAL_PLAN_DISJOINT_V2".equals(j.getString("revision"));
        if(j.length()!=(v2?20:19))throw new IllegalStateException("R967_PLAN_FIELDS");
        if(v2&&(!ResearchEvaluationRunR967.SPLIT.equals(j.getString("evaluation_schedule"))||!ResearchEvaluationRunR967.versioned(j.getString("candidate_key"))))throw new IllegalStateException("R969_PLAN_SCHEDULE");
        if(!v2&&ResearchEvaluationRunR967.versioned(j.getString("candidate_key")))throw new IllegalStateException("R969_PLAN_SCHEDULE_DOWNGRADE");
        for(String k:new String[]{"revision","window_id","structure_hash","bundle_hash","selector_hash","hypothesis_hash","policy_hash","calibration_hash","study_hash","training_rule_hash","candidate_key","claim_key","first_issue","last_issue"})
            if(!(j.get(k) instanceof String))throw new IllegalStateException("R967_PLAN_TYPE:"+k);
        for(String k:new String[]{"planned_n","window_no","stage","registered_at"}){
            Object value=j.get(k);if(!(value instanceof Integer)&&!(value instanceof Long))throw new IllegalStateException("R967_PLAN_INTEGER:"+k);
        }
        if(!(j.get("alpha") instanceof Number)||!Double.isFinite(j.getDouble("alpha")))throw new IllegalStateException("R967_PLAN_TYPE:alpha");
        if(!v2&&!"R967_FORMAL_PLAN_V1".equals(j.getString("revision")))throw new IllegalStateException("R967_PLAN_REVISION");
        ResearchSemanticsR967.Builder b=new ResearchSemanticsR967.Builder(v2?"FORMAL_PLAN_DISJOINT_V2":"FORMAL_PLAN_V1");
        if(v2)b.text("evaluation_schedule",j.getString("evaluation_schedule"));
        for(String k:new String[]{"window_id","structure_hash","bundle_hash","selector_hash","hypothesis_hash","policy_hash","calibration_hash","study_hash","training_rule_hash"})b.hash(k,j.getString(k));
        for(String k:new String[]{"candidate_key","claim_key","first_issue","last_issue"})b.text(k,j.getString(k));
        for(String k:new String[]{"planned_n","window_no","stage","registered_at"})b.integer(k,j.getLong(k));
        return b.number("alpha",j.getDouble("alpha")).digest();
    }

    /** Read-only revalidation of DB plan bytes, window denominator and paid look. */
    public static Plan forWindow(SQLiteDatabase db,String window,boolean required){
        try(Cursor present=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{CP4_BINDING})){if(present.moveToFirst()){Plan cp4=planCp4(db,window);if(cp4!=null)return cp4;}}
        String planHash=null,text=null,policyText=null,policyHash=null;long at=0;
        try(Cursor c=db.rawQuery("SELECT p.plan_hash,p.plan_json,p.policy_hash,p.registered_at,g.policy_json FROM "+PLAN+" p LEFT JOIN "+POLICY+" g ON g.policy_hash=p.policy_hash WHERE p.window_id=?",new String[]{window})){
            if(c.moveToFirst()){planHash=c.getString(0);text=c.getString(1);policyHash=c.getString(2);at=c.getLong(3);policyText=c.getString(4);}
        }
        if(planHash==null){
            // A stable tag stays on the original window even when both plan side tables are lost.
            try(Cursor c=db.rawQuery("SELECT formal_plan_revision FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id=?",new String[]{window})){
                if(c.moveToFirst()&&REVISION.equals(c.getString(0)))required=true;
            }
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+REQUIRED+" WHERE window_id=?",new String[]{window})){if(c.moveToFirst())required=true;}
            if(required)throw new IllegalStateException("R967_FORMAL_PLAN_MISSING");return null;
        }
        if(policyText==null)throw new IllegalStateException("R967_PLAN_POLICY_MISSING");
        try{
            JSONObject j=new JSONObject(text);Policy p=policy(policyText,policyHash);
            if(!planHash.equals(hash(j))||!window.equals(j.getString("window_id"))||!policyHash.equals(j.getString("policy_hash"))||at!=j.getLong("registered_at"))throw new IllegalStateException("R967_PLAN_HASH_MISMATCH");
            try(Cursor c=db.rawQuery("SELECT plan_hash FROM "+REQUIRED+" WHERE window_id=?",new String[]{window})){if(!c.moveToFirst()||!planHash.equals(c.getString(0)))throw new IllegalStateException("R967_PLAN_REQUIREMENT_MISSING");}
            try(Cursor c=db.rawQuery("SELECT candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,formal_plan_revision FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id=?",new String[]{window})){
                if(!c.moveToFirst())throw new IllegalStateException("R967_PLAN_WINDOW_MISSING");
                if(!REVISION.equals(c.getString(9)))throw new IllegalStateException("R967_PLAN_WINDOW_REVISION_DRIFT");
                String[] keys={"candidate_key","structure_hash","bundle_hash","selector_hash","first_issue","last_issue"};
                for(int i=0;i<keys.length;i++)if(!c.getString(i).equals(j.getString(keys[i])))throw new IllegalStateException("R967_PLAN_WINDOW_DRIFT:"+keys[i]);
                if(c.getInt(6)!=j.getInt("planned_n")||c.getInt(7)!=j.getInt("window_no")||c.getLong(8)!=at||c.getInt(6)!=p.planned(j.getInt("stage")))throw new IllegalStateException("R967_PLAN_DENOMINATOR_DRIFT");
            }
            try(Cursor c=db.rawQuery("SELECT hypothesis_hash,window_no,first_issue,last_issue,alpha FROM "+ResearchMultiplicityR932.LOOK+" WHERE window_id=?",new String[]{window})){
                if(!c.moveToFirst()||!c.getString(0).equals(j.getString("hypothesis_hash"))||c.getInt(1)!=j.getInt("stage")
                    ||!c.getString(2).equals(j.getString("first_issue"))||!c.getString(3).equals(j.getString("last_issue"))||Double.compare(c.getDouble(4),j.getDouble("alpha"))!=0)throw new IllegalStateException("R967_PLAN_ALPHA_DRIFT");
            }
            if(!ResearchEvaluationRunR967.endpoint(j.getString("candidate_key"),j.getString("first_issue"),j.getInt("planned_n")).equals(j.getString("last_issue")))throw new IllegalStateException("R969_PLAN_ENDPOINT_DRIFT");
            return new Plan(planHash,window,j.getString("calibration_hash"),j.getString("training_rule_hash"),text,p,at);
        }catch(JSONException e){throw new IllegalStateException("R967_PLAN_INVALID",e);}
    }
    static Plan beforeEvaluation(SQLiteDatabase db,String window){
        String reason="";try(Cursor c=db.rawQuery("SELECT reason FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id=?",new String[]{window})){if(c.moveToFirst())reason=c.getString(0);}
        return forWindow(db,window,REASON.equals(reason));
    }

    static final String CP4_LIFETIME="research_cp4_frozen_lifetime";
    static final String CP4_BINDING="research_cp4_window_binding";
    public static void schemaCp4(SQLiteDatabase db){
        if(TransactionSchemaCacheR941.readyCommitted(db,"CP4_FORMAL_BINDING_V1"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CP4_LIFETIME+"(lifetime_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,generation_id TEXT NOT NULL,identity_hash TEXT NOT NULL,identity_json TEXT NOT NULL,policy_hash TEXT NOT NULL,policy_json TEXT NOT NULL,hypothesis_id TEXT NOT NULL UNIQUE,registration_json TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(candidate_key,generation_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CP4_BINDING+"(window_id TEXT PRIMARY KEY,lifetime_id TEXT NOT NULL REFERENCES "+CP4_LIFETIME+"(lifetime_id),stage INTEGER NOT NULL CHECK(stage BETWEEN 1 AND 5),first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,planned_n INTEGER NOT NULL,plan_hash TEXT NOT NULL UNIQUE,UNIQUE(lifetime_id,stage))");
        for(String table:new String[]{CP4_LIFETIME,CP4_BINDING})for(String op:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_"+op+" BEFORE "+op+" ON "+table+" BEGIN SELECT RAISE(ABORT,'CP4_FORMAL_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp4_lifetime_no_replace BEFORE INSERT ON "+CP4_LIFETIME+" WHEN EXISTS(SELECT 1 FROM "+CP4_LIFETIME+" WHERE lifetime_id=NEW.lifetime_id OR (candidate_key=NEW.candidate_key AND generation_id=NEW.generation_id) OR hypothesis_id=NEW.hypothesis_id) BEGIN SELECT RAISE(ABORT,'CP4_LIFETIME_REPLACE_FORBIDDEN'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp4_binding_no_replace BEFORE INSERT ON "+CP4_BINDING+" WHEN EXISTS(SELECT 1 FROM "+CP4_BINDING+" WHERE window_id=NEW.window_id OR (lifetime_id=NEW.lifetime_id AND stage=NEW.stage) OR plan_hash=NEW.plan_hash) BEGIN SELECT RAISE(ABORT,'CP4_BINDING_REPLACE_FORBIDDEN'); END");
        TransactionSchemaCacheR941.markCommitted(db,"CP4_FORMAL_BINDING_V1");
    }
    static final class FrozenIdentityCp4 {
        final String json,hash,program,state,code,schema,atom,bundle,selector,calibration,structure,key,family;
        FrozenIdentityCp4(String key,String structure,String bundle,String selector,String cal,String family,JSONObject execution)throws JSONException{
            if(family==null||family.isEmpty())throw new IllegalStateException("CP4_ALGORITHM_FAMILY_MISSING");this.family=family;
            this.key=key;this.structure=structure;this.bundle=bundle;this.selector=selector;calibration=cal;
            program=execution.getString("program_hash");state=execution.getString("state_hash");
            if(!CalibrationRuntimeIdentityF04.REVISION.equals(execution.optString("semantic_revision","")))
                throw new IllegalStateException("F04_LEGACY_SCOPE_PROOF_REQUIRED");
            if(!execution.getString("code_hash").equals(execution.getString("build_source_hash")))
                throw new IllegalStateException("F04_BUILD_PROVENANCE_CONFLICT");
            code=execution.getString("effective_numeric_hash");schema=execution.getString("schema_hash");atom=execution.getString("execution_scope_hash");
            if(!CalibrationRuntimeIdentityF04.numericHash().equals(code)
                    ||!CalibrationRuntimeIdentityF04.executionScope(key,structure,bundle).equals(atom))
                throw new IllegalStateException("F04_EFFECTIVE_SCOPE_MISMATCH");
            for(String h:new String[]{structure,bundle,selector,cal,program,state,code,schema,atom})if(!h.matches("[0-9a-f]{64}"))throw new IllegalStateException("CP4_FREEZE_HASH");
            if(!key.equals(execution.getString("candidate_key"))||!structure.equals(execution.getString("structural_hash"))||!bundle.equals(execution.getString("model_bundle_hash")))throw new IllegalStateException("CP4_FREEZE_IDENTITY");
            JSONObject v=new JSONObject();v.put("family",family);v.put("candidate",key);v.put("structure",structure);v.put("bundle",bundle);v.put("selector",selector);v.put("calibration",cal);v.put("program",program);v.put("state",state);v.put("code",code);v.put("schema",schema);v.put("atom",atom);v.put("semantic_revision",CalibrationRuntimeIdentityF04.REVISION);
            v.put("build_source_hash",execution.getString("build_source_hash"));
            v.put("atom_admission_hash",execution.getString("atom_graph_hash"));
            v.put("action_space","FULL1200_SINGLE_POSITION_FIXED_K3");
            String exactScope=key.startsWith("algorithm:"+FrozenCandidateAdapterR958.PREFIX)
                    ?CalibrationRuntimeIdentityF04.exactScope(key.substring("algorithm:".length()),structure,state,selector):"";
            if(!exactScope.isEmpty()&&!exactScope.equals(execution.optString("exact_scope_policy_hash","")))throw new IllegalStateException("F05_FORMAL_EXACT_SCOPE_NOT_PREREGISTERED");
            v.put("exact_scope_policy_hash",exactScope);
            // Hash a fixed ordered list; JSONObject iteration ordering is not identity.
            hash=V51ChecksumEngine.sha256("CP4_FREEZE1|"+CalibrationRuntimeIdentityF04.REVISION+'|'+key+'|'+structure+'|'+bundle+'|'+selector+'|'+cal+'|'+program+'|'+state+'|'+code+'|'+schema+'|'+atom+"|FULL1200_SINGLE_POSITION_FIXED_K3|"+family+"|F05_EXACT_SCOPE="+exactScope);json=v.toString();
        }
    }
    static FrozenIdentityCp4 captureIdentityCp4(SQLiteDatabase db,String key,String structure,String bundle,String selector,String first,String cal){
        try(Cursor c=db.rawQuery("SELECT payload,proof_hash,base_issue,action_count FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key=? AND target_issue=? AND structural_hash=?",new String[]{key,first,structure})){
            if(!c.moveToFirst()||!V51ChecksumEngine.sha256(c.getString(0)).equals(c.getString(1))||!TargetIssueResolver.exact(c.getString(2),first).valid||c.getInt(3)!=1200)throw new IllegalStateException("CP4_FIRST_EXECUTION_REQUIRED");
            ResearchMultiplicityR932.ResearchIdentity metadata=ResearchMultiplicityR932.identity(db,key);if(metadata==null||!metadata.structureHash.equals(structure))throw new IllegalStateException("CP4_FAMILY_IDENTITY_MISSING");
            return new FrozenIdentityCp4(key,structure,bundle,selector,cal,metadata.family,new JSONObject(c.getString(0)));
        }catch(JSONException e){throw new IllegalStateException("CP4_EXECUTION_JSON",e);}
    }
    static MultipleComparisonRegistryR930.Registration registrationCp4(String json){
        try{JSONObject r=new JSONObject(json);return new MultipleComparisonRegistryR930.Registration(r.getString("game"),r.getString("schema"),r.getString("source"),"CP3_ALL_PRODUCTION",r.getString("structure"),r.getString("hyper"),r.getString("protocol"),"",r.getString("lifetime"),r.getString("first"),r.getString("last"),"CP3_PROTOCOL_FINAL");}catch(JSONException e){throw new IllegalStateException("CP4_REGISTRATION_JSON",e);}
    }
    static MultipleComparisonRegistryR930.Registration registrationForWindowCp4(SQLiteDatabase db,String window){
        try(Cursor c=db.rawQuery("SELECT l.registration_json FROM "+CP4_BINDING+" b JOIN "+CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id WHERE b.window_id=?",new String[]{window})){
            if(!c.moveToFirst())throw new IllegalStateException("CP4_LIFETIME_BINDING_MISSING");return registrationCp4(c.getString(0));
        }
    }
    static void bindCp4(SQLiteDatabase db,String window,String key,String structure,String bundle,String selector,String first,String last,int planned,ObservationGenerationR932.Chain chain,String cal){
        schemaCp4(db);Policy p=installed();if(!p.isCp3)throw new IllegalStateException("CP4_POLICY_REQUIRED");
        FrozenIdentityCp4 identity=captureIdentityCp4(db,key,structure,bundle,selector,first,cal);
        String life="",registration="",lifeFirst="",lifeLast="",identityHash="";
        try(Cursor c=db.rawQuery("SELECT lifetime_id,registration_json,first_issue,last_issue,identity_hash,policy_hash FROM "+CP4_LIFETIME+" WHERE candidate_key=? AND generation_id=?",new String[]{key,chain.generation})){
            if(c.moveToFirst()){life=c.getString(0);registration=c.getString(1);lifeFirst=c.getString(2);lifeLast=c.getString(3);identityHash=c.getString(4);if(!p.hash.equals(c.getString(5)))throw new IllegalStateException("CP4_GENERATION_POLICY_DRIFT");}
        }
        try{
            if(life.isEmpty()){
                if(chain.stage!=1)throw new IllegalStateException("CP4_FIRST_WINDOW_REQUIRED");
                lifeFirst=first;lifeLast=ResearchEvaluationRunR967.endpoint(key,first,1200);
                life=V51ChecksumEngine.sha256("CP4_LIFETIME|"+key+'|'+chain.generation+'|'+identity.hash+'|'+p.hash+'|'+first+'|'+lifeLast);
                JSONObject r=new JSONObject();r.put("game",V51Repository.GAME_CODE);r.put("schema",identity.schema);r.put("source",identity.code);r.put("structure",structure);r.put("hyper",V51ChecksumEngine.sha256(identity.program+'|'+identity.state+'|'+bundle+'|'+cal));r.put("protocol",V51ChecksumEngine.sha256(p.hash+'|'+selector+'|'+identity.atom));r.put("lifetime",life);r.put("first",first);r.put("last",lifeLast);registration=r.toString();
                MultipleComparisonRegistryR930.Registration reg=registrationCp4(registration);MultipleComparisonRegistryR930.reserveCp3LifetimeWithinTransaction(db,reg);
                db.execSQL("INSERT INTO "+CP4_LIFETIME+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{life,key,chain.generation,identity.hash,identity.json,p.hash,p.json,reg.hypothesisId,registration,first,lifeLast,System.currentTimeMillis()});
            }else if(!identity.hash.equals(identityHash))throw new IllegalStateException("CP4_FROZEN_MODEL_DRIFT");
            int preceding=0;for(int stage=1;stage<chain.stage;stage++)preceding+=p.planned(stage);
            String expectedFirst=preceding==0?lifeFirst:ResearchEvaluationRunR967.nextEvaluation(key,ResearchEvaluationRunR967.endpoint(key,lifeFirst,preceding));
            if(!first.equals(expectedFirst)||planned!=p.planned(chain.stage)||!last.equals(ResearchEvaluationRunR967.endpoint(key,first,planned)))throw new IllegalStateException("CP4_FIXED_ENDPOINT_DRIFT");
            String h=V51ChecksumEngine.sha256("CP4_PLAN|"+window+'|'+life+'|'+chain.stage+'|'+first+'|'+last+'|'+planned+'|'+p.hash);
            db.execSQL("INSERT INTO "+CP4_BINDING+" VALUES(?,?,?,?,?,?,?)",new Object[]{window,life,chain.stage,first,last,planned,h});
        }catch(JSONException e){throw new IllegalStateException("CP4_PLAN_JSON",e);}
    }
    static Plan planCp4(SQLiteDatabase db,String window){
        try(Cursor c=db.rawQuery("SELECT b.lifetime_id,b.stage,b.first_issue,b.last_issue,b.planned_n,b.plan_hash,l.policy_json,l.policy_hash,l.registration_json,l.identity_json,l.identity_hash,l.created_at,w.candidate_key,w.first_issue,w.last_issue,w.planned_n FROM "+CP4_BINDING+" b JOIN "+CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id WHERE b.window_id=?",new String[]{window})){
            if(!c.moveToFirst())return null;
            Policy p=policy(c.getString(6),c.getString(7));if(!p.isCp3)throw new IllegalStateException("CP4_PLAN_POLICY");
            String h=V51ChecksumEngine.sha256("CP4_PLAN|"+window+'|'+c.getString(0)+'|'+c.getInt(1)+'|'+c.getString(2)+'|'+c.getString(3)+'|'+c.getInt(4)+'|'+p.hash);
            if(!h.equals(c.getString(5))||!c.getString(2).equals(c.getString(13))||!c.getString(3).equals(c.getString(14))||c.getInt(4)!=c.getInt(15)||c.getInt(4)!=p.planned(c.getInt(1)))throw new IllegalStateException("CP4_PLAN_DRIFT");
            MultipleComparisonRegistryR930.Registration r=registrationCp4(c.getString(8));MultipleComparisonRegistryR930.Snapshot s=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,r.hypothesisId);
            if(s==null||!s.windowId.equals(c.getString(0))||!s.family.equals("CP3_ALL_PRODUCTION"))throw new IllegalStateException("CP4_ALPHA_IDENTITY_DRIFT");
            try{JSONObject identity=new JSONObject(c.getString(9));return new Plan(h,window,identity.getString("calibration"),TrainingCodeIdentityR964.SHA256,c.getString(9),p,c.getLong(11));}catch(JSONException e){throw new IllegalStateException("CP4_PLAN_IDENTITY_JSON",e);}
        }
    }
    static void observeCp4(SQLiteDatabase db,String window,String key,String issue,String prediction){
        Plan plan=planCp4(db,window);if(plan==null||!plan.policy.isCp3)throw new IllegalStateException("CP4_FORMAL_PLAN_MISSING");
        try{JSONObject identity=new JSONObject(plan.json);FrozenIdentityCp4 current=captureIdentityCp4(db,key,identity.getString("structure"),identity.getString("bundle"),identity.getString("selector"),issue,identity.getString("calibration"));
            String expected="";try(Cursor c=db.rawQuery("SELECT l.identity_hash FROM "+CP4_BINDING+" b JOIN "+CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id WHERE b.window_id=?",new String[]{window})){if(c.moveToFirst())expected=c.getString(0);}
            if(!current.hash.equals(expected))throw new IllegalStateException("CP4_FROZEN_MODEL_DRIFT");
            MultipleComparisonRegistryR930.markObservingWithinTransactionCp3(db,registrationForWindowCp4(db,window),prediction);
        }catch(JSONException e){throw new IllegalStateException("CP4_IDENTITY_JSON",e);}
    }

}
