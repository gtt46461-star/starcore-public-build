package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Candidate-only calibration, fitted on an earlier 120, embargo 1, validation 120.
 * This probability is never the final mixed mainline action's calibration.
 */
public final class CandidateCalibrationR932 {
    public static final String OBS="research_candidate_cal_observation_r932",MODEL="research_candidate_cal_model_r932",STUDY="research_candidate_cal_study_r932";
    public static final int OBSERVATIONS=241;
    public static final String QUALITY_CP12="research_calibration_quality_cp12";
    public static final String QUALITY_POLICY_CP12="R82_CAL_COMPARE_LOGISTIC_BETA_ISOTONIC_SCORE_DOMAIN_SHRINK_FIT80_SELECT40_REFIT120_EMBARGO1_VALID120_ECE.04_BRIER.24_RELATIVE.3_V1";
    public static final String QUALITY_POLICY_HASH_CP12=V51ChecksumEngine.sha256(QUALITY_POLICY_CP12);
    public enum CalibrationInputState {COLLECTING,READY_TO_FIT,FITTED,BLOCKED_INVALID_INPUT,UNKNOWN}
    public enum QualificationState {UNFITTED,CALIBRATION_QUALIFIED,FITTED_BUT_NOT_QUALIFIED}
    public static CalibrationInputState inputStateCP12(String stored){
        if("COLLECTING".equals(stored))return CalibrationInputState.COLLECTING;
        if("READY".equals(stored))return CalibrationInputState.READY_TO_FIT;
        if("FROZEN".equals(stored))return CalibrationInputState.FITTED;
        if("BLOCKED_INVALID_INPUT".equals(stored))return CalibrationInputState.BLOCKED_INVALID_INPUT;
        return CalibrationInputState.UNKNOWN;
    }
    public static final class Qualification {
        public final QualificationState state;public final String reason,validationUntil;
        public final double ece,brier,baselineBrier;
        private Qualification(QualificationState state,String reason,String until,double ece,double brier,double baseline){this.state=state;this.reason=reason;validationUntil=until;this.ece=ece;this.brier=brier;baselineBrier=baseline;}
        public boolean qualified(){return state==QualificationState.CALIBRATION_QUALIFIED;}
    }
    public static void qualitySchemaCP12(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("CALIBRATION_QUALITY_SCHEMA_TX");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+QUALITY_CP12+"(study_hash TEXT PRIMARY KEY REFERENCES "+MODEL+"(study_hash),model_hash TEXT NOT NULL CHECK(length(model_hash)=64),state TEXT NOT NULL CHECK(state IN('CALIBRATION_QUALIFIED','FITTED_BUT_NOT_QUALIFIED')),reason TEXT NOT NULL,policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),ece REAL NOT NULL CHECK(ece>=0 AND ece<=1),brier REAL NOT NULL CHECK(brier>=0 AND brier<=1),baseline_brier REAL NOT NULL CHECK(baseline_brier>=0 AND baseline_brier<=1),validation_until TEXT NOT NULL,published_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS cp12_cal_quality_state ON "+QUALITY_CP12+"(state,study_hash)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_cal_quality_immutable BEFORE UPDATE ON "+QUALITY_CP12+" BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_cal_quality_no_delete BEFORE DELETE ON "+QUALITY_CP12+" BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_PERMANENT'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_cal_quality_no_replace BEFORE INSERT ON "+QUALITY_CP12+" WHEN EXISTS(SELECT 1 FROM "+QUALITY_CP12+" q WHERE q.study_hash=NEW.study_hash) BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_REPLACE_FORBIDDEN'); END");
    }
    private static Qualification evaluateQualityCP12(JSONObject model,String study)throws JSONException{
        String until=model.optString("validation_last","");
        if(!QUALITY_POLICY_HASH_CP12.equals(model.optString("quality_policy_hash","")))return new Qualification(QualificationState.FITTED_BUT_NOT_QUALIFIED,"CALIBRATION_POLICY_MISMATCH",until,0,0,0);
        if(!study.equals(model.getString("study_hash"))||!model.getString("evidence_hash").matches("[0-9a-f]{64}"))throw new IllegalStateException("CALIBRATION_QUALITY_IDENTITY");
        String first=model.getString("fit_first"),last=model.getString("fit_last"),embargo=model.getString("embargo_issue"),vf=model.getString("validation_first");
        if(!TargetIssueResolver.isValid(first)||!TargetIssueResolver.isValid(last)||!TargetIssueResolver.isValid(embargo)||!TargetIssueResolver.isValid(vf)||!TargetIssueResolver.isValid(until)||first.compareTo(last)>0||last.compareTo(embargo)>=0||embargo.compareTo(vf)>=0||vf.compareTo(until)>0)throw new IllegalStateException("CALIBRATION_QUALITY_CHRONOLOGY");
        double ece=model.getDouble("validation_ece"),brier=model.getDouble("validation_brier"),baseline=model.getDouble("validation_baseline_brier");
        if(!Double.isFinite(ece)||!Double.isFinite(brier)||!Double.isFinite(baseline)||ece<0||ece>1||brier<0||brier>1||baseline<0||baseline>1)throw new IllegalStateException("CALIBRATION_QUALITY_NONFINITE");
        JSONArray bins=model.getJSONArray("validation_bin_counts");int n=0;if(bins.length()!=10)throw new IllegalStateException("CALIBRATION_QUALITY_BINS");for(int i=0;i<10;i++){int count=bins.getInt(i);if(count<0||count>120)throw new IllegalStateException("CALIBRATION_QUALITY_BINS");n+=count;}if(n!=120)throw new IllegalStateException("CALIBRATION_QUALITY_BINS");
        boolean qualified=ece<=.04&&brier<=.24&&brier<=baseline;
        return new Qualification(qualified?QualificationState.CALIBRATION_QUALIFIED:QualificationState.FITTED_BUT_NOT_QUALIFIED,qualified?"":"CALIBRATION_QUALITY_BLOCKED",until,ece,brier,baseline);
    }
    /** Preflight closes its tiny cursor before a model JSON CursorWindow or parser is allocated. */
    private static void requireModelBoundCP12(SQLiteDatabase db,String study){
        try(Cursor bound=db.rawQuery("SELECT length(CAST(model_json AS BLOB)) FROM "+MODEL+" WHERE study_hash=?",new String[]{study})){
            if(bound.moveToFirst()&&bound.getLong(0)>65536)throw new IllegalStateException("R932_CAL_MODEL_BOUND");
        }
    }
    /** Typed immutable quality read. Raw READY is only the input codec READY_TO_FIT. */
    public static Qualification qualificationForStudyCP12(SQLiteDatabase db,String study){
        requireModelBoundCP12(db,study);
        try(Cursor c=db.rawQuery("SELECT m.model_json,m.model_hash,q.model_hash,q.state,q.reason,q.policy_hash,q.ece,q.brier,q.baseline_brier,q.validation_until FROM "+MODEL+" m LEFT JOIN "+QUALITY_CP12+" q ON q.study_hash=m.study_hash WHERE m.study_hash=?",new String[]{study})){
            if(!c.moveToFirst())return new Qualification(QualificationState.UNFITTED,"WAIT_CALIBRATION_INPUT","",0,0,0);
            String json=c.getString(0),hash=c.getString(1);if(!V51ChecksumEngine.sha256(json).equals(hash))throw new IllegalStateException("R932_CAL_MODEL_HASH");
            try{Qualification q=evaluateQualityCP12(new JSONObject(json),study);if(c.isNull(2)||!hash.equals(c.getString(2))||!q.state.name().equals(c.getString(3))||!q.reason.equals(c.getString(4))||!QUALITY_POLICY_HASH_CP12.equals(c.getString(5))||Double.compare(q.ece,c.getDouble(6))!=0||Double.compare(q.brier,c.getDouble(7))!=0||Double.compare(q.baselineBrier,c.getDouble(8))!=0||!q.validationUntil.equals(c.getString(9)))return new Qualification(QualificationState.FITTED_BUT_NOT_QUALIFIED,"CALIBRATION_POLICY_MISMATCH",q.validationUntil,q.ece,q.brier,q.baselineBrier);return q;}catch(JSONException bad){throw new IllegalStateException("R932_CAL_MODEL_JSON",bad);}
        }
    }

    public static Qualification qualificationForStudySnapshotCP12(ReportReadContextCP4 ctx,String study){
        try(Cursor bound=ctx.select("SELECT length(CAST(model_json AS BLOB)) FROM "+MODEL+" WHERE study_hash=?",new String[]{study})){if(bound.moveToFirst()&&bound.getLong(0)>65536)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_CAL_MODEL_BOUND");}
        try(Cursor c=ctx.select("SELECT m.model_json,m.model_hash,q.model_hash,q.state,q.reason,q.policy_hash,q.ece,q.brier,q.baseline_brier,q.validation_until FROM "+MODEL+" m LEFT JOIN "+QUALITY_CP12+" q ON q.study_hash=m.study_hash WHERE m.study_hash=?",new String[]{study})){
            if(!c.moveToFirst())return new Qualification(QualificationState.UNFITTED,"WAIT_CALIBRATION_INPUT","",0,0,0);
            String json=c.getString(0),hash=c.getString(1);if(!V51ChecksumEngine.sha256(json).equals(hash))throw new IllegalStateException("R932_CAL_MODEL_HASH");
            try{Qualification q=evaluateQualityCP12(new JSONObject(json),study);if(c.isNull(2)||!hash.equals(c.getString(2))||!q.state.name().equals(c.getString(3))||!q.reason.equals(c.getString(4))||!QUALITY_POLICY_HASH_CP12.equals(c.getString(5))||Double.compare(q.ece,c.getDouble(6))!=0||Double.compare(q.brier,c.getDouble(7))!=0||Double.compare(q.baselineBrier,c.getDouble(8))!=0||!q.validationUntil.equals(c.getString(9)))return new Qualification(QualificationState.FITTED_BUT_NOT_QUALIFIED,"CALIBRATION_POLICY_MISMATCH",q.validationUntil,q.ece,q.brier,q.baselineBrier);return q;}catch(JSONException bad){throw new IllegalStateException("R932_CAL_MODEL_JSON",bad);}
        }
    }

    private CandidateCalibrationR932(){}
    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.readyCommitted(db,"CandidateCalibrationR980_CP12")) return;
        AlgorithmProofRecoveryR948.schema(db);

        db.execSQL("CREATE TABLE IF NOT EXISTS "+STUDY+"(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,captured_n INTEGER NOT NULL,settled_n INTEGER NOT NULL,state TEXT NOT NULL,first_issue TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS r975_calibration_failure(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_cal_ready ON "+STUDY+"(state,first_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS r979_calibration_head(base_study TEXT PRIMARY KEY,current_study TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS r979_calibration_restart(new_study TEXT PRIMARY KEY,base_study TEXT NOT NULL,previous_study TEXT NOT NULL,candidate_key TEXT NOT NULL,first_issue TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+OBS+"(study_hash TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,structure_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,raw_score REAL NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,prediction_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,hit INTEGER NOT NULL DEFAULT -1,PRIMARY KEY(study_hash,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_cal_pending ON "+OBS+"(hit,target_issue)");
        // The page owner orders by study within one issue. Keep settled history out
        // of this hot index; no observations, denominators or study hashes are changed.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r980_cal_unsettled_page ON "+OBS+"(target_issue,study_hash) WHERE hit=-1");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+MODEL+"(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,trained_until TEXT NOT NULL,validation_until TEXT NOT NULL,model_json TEXT NOT NULL,model_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL)");
    
        qualitySchemaCP12(db);
        TransactionSchemaCacheR941.markCommitted(db,"CandidateCalibrationR980_CP12");
    }
    static String study(SQLiteDatabase db,String key,String structure,String bundle,String selector) {
        String base=baseStudy(db,key,structure,bundle,selector);
        try(Cursor c=db.rawQuery("SELECT current_study FROM r979_calibration_head WHERE base_study=?",new String[]{base})){return c.moveToFirst()?c.getString(0):base;}
    }
    private static String baseStudy(SQLiteDatabase db,String key,String structure,String bundle,String selector) {
        String hypothesis;
        try(Cursor c=db.rawQuery("SELECT h.hypothesis_hash,h.structure_hash FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{key})) {
            if(!c.moveToFirst()||!structure.equals(c.getString(1)))throw new IllegalStateException("R932_CAL_UNREGISTERED_CANDIDATE");hypothesis=c.getString(0);
        }
        if(ResearchEvaluationRunR967.versioned(key)){
            String rule;
            try(Cursor c=db.rawQuery("SELECT structural_hash,rule_hash FROM "+ResearchEvaluationRunR967.EVAL+" WHERE candidate_id=?",new String[]{key.substring("algorithm:".length())})){
                if(!c.moveToFirst()||!structure.equals(c.getString(0))||!TrainingContinuationR973.compatible(c.getString(1)))throw new IllegalStateException("R969_CAL_FROZEN_VERSION_DRIFT");rule=c.getString(1);
            }
            return new ResearchSemanticsR967.Builder("R969_STATIC_DISJOINT_CALIBRATION").hash("hypothesis",hypothesis).hash("evaluation",structure).hash("bundle",bundle).hash("selector",selector).hash("rule",rule).hash("cal_policy",QUALITY_POLICY_HASH_CP12).hash("integrity",V51ChecksumEngine.sha256(AlgorithmProofRecoveryR948.epoch(db,key))).text("split",ResearchEvaluationRunR967.SPLIT).text("protocol","FIT120_EMBARGO1_VALIDATION120").text("selected_scope_protocol",CalibrationRuntimeIdentityF04.EXACT_SCOPE_POLICY_F04).digest();
        }
        return V51ChecksumEngine.sha256(CalibrationRuntimeIdentityF04.legacyCalibrationBase(key,hypothesis,structure,bundle,selector,
                V51ChecksumEngine.sha256(AlgorithmProofRecoveryR948.epoch(db,key)))+"|"+QUALITY_POLICY_HASH_CP12);
    }
    static final class Probability {final double value;final String hash;Probability(double value,String hash){this.value=value;this.hash=hash;}}
    static Probability captureOrPredict(SQLiteDatabase db,String key,String structure,String bundle,String selector,String target,String prediction,double score,int position,int n1,int n2,int n3) {
        if(db==null||!db.inTransaction()||!Double.isFinite(score)||!TargetIssueResolver.isValid(target)
                ||key==null||key.isEmpty()||structure==null||!structure.matches("[0-9a-f]{64}")||bundle==null||!bundle.matches("[0-9a-f]{64}")
                ||selector==null||!selector.matches("[0-9a-f]{64}")||prediction==null||!prediction.matches("[0-9a-f]{64}")
                ||position<1||position>10||!DecisionGateEngine.b12K3(n1,n2,n3))throw new IllegalArgumentException("R932_CAL_PREDRAW_INPUT");
        if(!ResearchEvaluationRunR967.eligibleEvaluation(key,target))throw new IllegalStateException("R969_CAL_TRAINING_ROLE_REJECTED");
        String base=baseStudy(db,key,structure,bundle,selector);
        String study=study(db,key,structure,bundle,selector);
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})) {
            if(c.moveToFirst())throw new IllegalStateException("CP5_CAL_POSTDRAW_REJECTED");
        }
        detectCaptureGapCp5(db,study,key,target);
        study=restartAfterGap(db,base,study,key,target);
        requireModelBoundCP12(db,study);
        try(Cursor c=db.rawQuery("SELECT validation_until,model_json,model_hash FROM "+MODEL+" WHERE study_hash=?",new String[]{study})) {
            if(c.moveToFirst()) {
                if(target.compareTo(c.getString(0))<=0)throw new IllegalStateException("R932_CAL_VALIDATION_OVERLAP");
                String json=c.getString(1),hash=c.getString(2);if(!hash.equals(V51ChecksumEngine.sha256(json)))throw new IllegalStateException("R932_CAL_MODEL_HASH");
                try {JSONObject model=new JSONObject(json);Qualification quality=qualificationForStudyCP12(db,study);if(!quality.qualified())return null;return new Probability(predict(model,score),hash);}catch(JSONException e){throw new IllegalStateException("R932_CAL_MODEL_JSON",e);}
            }
        }
        db.execSQL("INSERT OR IGNORE INTO "+STUDY+" VALUES(?,?,0,0,'COLLECTING',?)",new Object[]{study,key,target});
        try(Cursor c=db.rawQuery("SELECT prediction_hash,raw_score FROM "+OBS+" WHERE study_hash=? AND target_issue=?",new String[]{study,target})) {if(c.moveToFirst()){if(!prediction.equals(c.getString(0))||Double.compare(score,c.getDouble(1))!=0)throw new IllegalStateException("R932_CAL_OBSERVATION_DRIFT");return null;}}
        int count=0;try(Cursor c=db.rawQuery("SELECT captured_n FROM "+STUDY+" WHERE study_hash=?",new String[]{study})){if(c.moveToFirst())count=c.getInt(0);}
        if(count>=OBSERVATIONS)return null; // Training is done only by the existing maintenance worker.
        GlobalResearchScheduleR932.CaptureReservationCP11 reservation=null;
        if(ResearchEvaluationRunR967.versioned(key)){
            reservation=GlobalResearchScheduleR932.reservationForStudyCP11(db,study);
            if(reservation==null||!key.equals(reservation.key)||!structure.equals(reservation.freeze)||!target.equals(reservation.next)
                    ||!("RESERVED".equals(reservation.state)||"ACTIVE".equals(reservation.state)))
                throw new IllegalStateException("CALIBRATION_CAPTURE_WITHOUT_DUE_RESERVATION");
        }
        db.execSQL("INSERT OR IGNORE INTO "+OBS+"(study_hash,target_issue,candidate_key,structure_hash,bundle_hash,selection_hash,raw_score,position,n1,n2,n3,prediction_hash,frozen_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{study,target,key,structure,bundle,selector,score,position,n1,n2,n3,prediction,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT prediction_hash,raw_score FROM "+OBS+" WHERE study_hash=? AND target_issue=?",new String[]{study,target})) {if(!c.moveToFirst()||!prediction.equals(c.getString(0))||Double.compare(score,c.getDouble(1))!=0)throw new IllegalStateException("R932_CAL_OBSERVATION_DRIFT");}
        db.execSQL("UPDATE "+STUDY+" SET captured_n=captured_n+1 WHERE study_hash=? AND captured_n=? AND state='COLLECTING'",new Object[]{study,count});
        try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("CALIBRATION_COUNTER_CAS_LOST");}
        if(reservation!=null)GlobalResearchScheduleR932.completeReservationCaptureCP11(db,reservation,key,structure,study,target,count+1);
        return null;
    }
    /** Only broken or late capture schedules may start a new prospective calibration study.
     * No retry on low hit rate/Brier/ECE, no copying old labels, no replacing the
     * registered hypothesis or its paid statistical looks. Old studies remain intact.
     */
    static String restartAfterGap(SQLiteDatabase db,String base,String current,String key,String target) {
        if(!db.inTransaction())throw new IllegalStateException("R979_CAL_RESTART_TRANSACTION_REQUIRED");
        final String reason;
        try(Cursor c=db.rawQuery("SELECT f.reason FROM "+STUDY+" s JOIN r975_calibration_failure f ON f.study_hash=s.study_hash WHERE s.study_hash=? AND s.candidate_key=? AND s.state='BLOCKED_INVALID_INPUT' AND f.reason IN('R969_CAL_EVALUATION_SCHEDULE_GAP','R77_CAL_STRICT_TIME_REJECTED','R77_CAL_DRAW_TIME_UNAVAILABLE','R77_CAL_RETAINED_INPUT_INVALID')",new String[]{current,key})) {if(!c.moveToFirst())return current;reason=c.getString(0);}
        try(Cursor c=db.rawQuery("SELECT MAX(target_issue) FROM "+OBS+" WHERE study_hash=?",new String[]{current})) {if(c.moveToFirst()&&!c.isNull(0)&&target.compareTo(c.getString(0))<=0)return current;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})) {if(c.moveToFirst())return current;}
        String next=V51ChecksumEngine.sha256("R979_PROSPECTIVE_CAPTURE_GAP_RECOVERY|"+base+'|'+current+'|'+key+'|'+target);
        db.execSQL("INSERT OR IGNORE INTO r979_calibration_restart VALUES(?,?,?,?,?,?,?)",new Object[]{next,base,current,key,target,"R969_CAL_EVALUATION_SCHEDULE_GAP".equals(reason)?"MISSING_CAPTURE_NEW_PROSPECTIVE_STUDY_NO_LABEL_REUSE":reason+"_NEW_PROSPECTIVE_STUDY_NO_LABEL_REUSE",System.currentTimeMillis()});
        db.execSQL("INSERT OR IGNORE INTO r979_calibration_head VALUES(?,?,?)",new Object[]{base,current,System.currentTimeMillis()});
        db.execSQL("UPDATE r979_calibration_head SET current_study=?,updated_at=? WHERE base_study=? AND current_study=?",new Object[]{next,System.currentTimeMillis(),base,current});
        try(Cursor c=db.rawQuery("SELECT changes()",null)) {
            if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("CP5_CAL_HEAD_CAS_LOST");
        }
        return next;
    }
    static String prepareReservedStudyCP11(SQLiteDatabase db,String key,String structure,String bundle,String selector,String target){
        String base=baseStudy(db,key,structure,bundle,selector),current=study(db,key,structure,bundle,selector);
        detectCaptureGapCp5(db,current,key,target);
        return restartAfterGap(db,base,current,key,target);
    }
    static String restartReservedGapCP11(SQLiteDatabase db,String key,String structure,String bundle,String selector,String current,String target){
        return restartAfterGap(db,baseStudy(db,key,structure,bundle,selector),current,key,target);
    }
    /** Preserve missing-opportunity truth; never backfill after observing a draw. */
    private static void detectCaptureGapCp5(SQLiteDatabase db,String study,String key,String target) {
        if(!ResearchEvaluationRunR967.versioned(key))return; // legacy schedule identity is unchanged
        int captured;
        try(Cursor c=db.rawQuery("SELECT captured_n,state FROM "+STUDY+" WHERE study_hash=?",new String[]{study})) {
            if(!c.moveToFirst()||!"COLLECTING".equals(c.getString(1)))return;
            captured=c.getInt(0);
        }
        if(captured==0||captured>=OBSERVATIONS)return;
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+OBS+" WHERE study_hash=? AND target_issue=?",new String[]{study,target})) {
            if(c.moveToFirst())return; // captureOrPredict verifies exact prediction identity next
        }
        String last;
        try(Cursor c=db.rawQuery("SELECT MAX(target_issue) FROM "+OBS+" WHERE study_hash=?",new String[]{study})) {
            if(!c.moveToFirst()||c.isNull(0))throw new IllegalStateException("CP5_CAL_CAPTURE_COUNTER_DRIFT");
            last=c.getString(0);
        }
        String expected=ResearchEvaluationRunR967.nextEvaluation(key,last);
        if(target.compareTo(expected)<0)throw new IllegalStateException("CP5_CAL_CAPTURE_ORDER_DRIFT");
        if(target.equals(expected))return;
        long now=System.currentTimeMillis();
        db.execSQL("INSERT OR IGNORE INTO research_capture_gap_v2(study_hash,expected_issue,observed_issue,reason,request_ref,recorded_at) VALUES(?,?,?,'MISSING_SCHEDULED_CAPTURE','',?)",new Object[]{study,expected,target,now});
        db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure VALUES(?,?,'R969_CAL_EVALUATION_SCHEDULE_GAP',?)",new Object[]{study,key,now});
        db.execSQL("UPDATE "+STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state='COLLECTING'",new Object[]{study});
        db.execSQL("UPDATE research_capture_reservation_v2 SET state='FAILED_GAP',last_reason='R969_CAL_EVALUATION_SCHEDULE_GAP',generation=generation+1 WHERE study_hash=? AND state IN('RESERVED','ACTIVE')",new Object[]{study});
    }
    static double predict(JSONObject model,double score)throws JSONException {
        return ResearchLogisticCalibrationR932.predict(model,score);
    }
    /** One small committed batch; later work must never roll it back as a whole issue. */
    static void settle(SQLiteDatabase db,Models.Draw draw) {
        if(!db.inTransaction())throw new IllegalStateException("R948_CAL_SETTLEMENT_TRANSACTION_REQUIRED");
        int done=0;
        try(Cursor c=db.rawQuery("SELECT study_hash,position,n1,n2,n3,frozen_at,candidate_key FROM "+OBS+" WHERE target_issue=? AND hit=-1 ORDER BY study_hash LIMIT 16",new String[]{draw.issue})) {
            while(c.moveToNext()) {
                try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield y){if(done==0)throw y;break;}
                // A slow feed can arrive after its published open time. Absence of the
                // target row during capture is not sufficient prospective evidence.
                // Match the frozen-model settlement fence and retain the rejected row.
                if(c.getLong(5)<=0L||draw.openTime<=0L||c.getLong(5)>=draw.openTime){
                    rejectLateObservationR77(db,c.getString(0),c.getString(6),draw.issue);
                    done++;continue;
                }
                db.execSQL("UPDATE "+OBS+" SET hit=? WHERE study_hash=? AND target_issue=? AND hit=-1",new Object[]{draw.hitStrictPositionK3(c.getInt(1),c.getInt(2),c.getInt(3),c.getInt(4))?1:0,c.getString(0),draw.issue});
                int changed;try(Cursor count=db.rawQuery("SELECT changes()",null)){count.moveToFirst();changed=count.getInt(0);}
                if(changed==1)db.execSQL("UPDATE "+STUDY+" SET settled_n=settled_n+1,state=CASE WHEN state='COLLECTING' AND settled_n+1=241 THEN 'READY' ELSE state END WHERE study_hash=?",new Object[]{c.getString(0)});
                done++;
            }
        }
    }
    private static void rejectLateObservationR77(SQLiteDatabase db,String study,String key,String issue){
        // -2 is terminal invalid evidence, distinct from the -1 pending label. It
        // neither increases settled_n nor contributes a synthetic win or loss.
        db.execSQL("UPDATE "+OBS+" SET hit=-2 WHERE study_hash=? AND target_issue=? AND hit=-1",new Object[]{study,issue});
        db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure VALUES(?,?,?,?)",
                new Object[]{study,key,"R77_CAL_STRICT_TIME_REJECTED",System.currentTimeMillis()});
        db.execSQL("UPDATE "+STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state IN('COLLECTING','READY')",new Object[]{study});
        db.execSQL("UPDATE "+GlobalResearchScheduleR932.RESERVATION_CP11+" SET state='FAILED_GAP',last_reason='R77_CAL_STRICT_TIME_REJECTED',generation=generation+1"
                +" WHERE study_hash=? AND state IN('RESERVED','ACTIVE','COMPLETE')",new Object[]{study});
    }

    static final class FitSnapshot {
        final String study,key;final double[] scores=new double[OBSERVATIONS],hits=new double[OBSERVATIONS];
        final String[] issues=new String[OBSERVATIONS];final double[] championP=new double[OBSERVATIONS],championHits=new double[OBSERVATIONS];String evidenceHash;private String preparedModelJson;
        FitSnapshot(String study,String key){this.study=study;this.key=key;Arrays.fill(championP,Double.NaN);Arrays.fill(championHits,Double.NaN);}
    }
    static FitSnapshot loadFit(SQLiteDatabase db,String wanted) {
        FitSnapshot fit=null;
        String sql="SELECT study_hash,candidate_key FROM "+STUDY+" WHERE state='READY'"+(wanted==null?" ORDER BY first_issue,study_hash LIMIT 1":" AND study_hash=?");
        try(Cursor c=db.rawQuery(sql,wanted==null?null:new String[]{wanted})){if(c.moveToFirst())fit=new FitSnapshot(c.getString(0),c.getString(1));}
        if(fit==null)return null;
        try(Cursor identity=db.rawQuery("SELECT structure_hash,bundle_hash,selection_hash FROM "+OBS+" WHERE study_hash=? ORDER BY target_issue LIMIT 1",new String[]{fit.study})){
            if(!identity.moveToFirst())throw new IllegalStateException("R932_CAL_FIXED_SUPPORT");
            if(!fit.study.equals(study(db,fit.key,identity.getString(0),identity.getString(1),identity.getString(2))))throw new IllegalStateException("R82_CAL_POLICY_OR_CURRENT_HEAD_MISMATCH");
        }
        StringBuilder evidence=new StringBuilder(fit.study);int n=0;
        // Indexed issue join, not the wide UNION history view. Only when no hot
        // row exists, authenticate the immutable retained input by its original hash.
        try(Cursor c=db.rawQuery("SELECT o.target_issue,o.raw_score,o.hit,o.prediction_hash,o.frozen_at,"
                +"CASE WHEN d.dirty=0 THEN d.open_time ELSE 0 END,d.issue,"
                +"ch.prediction_hash,ch.input_hash,ch.selection_hash,ch.bundle_hash,ch.position,ch.n1,ch.n2,ch.n3,ch.frozen_at,"
                +"cp.probability,cp.calibration_hash,cp.status,cp.proof_json,cp.proof_hash,cp.frozen_at,"
                +"CASE ch.position WHEN 1 THEN COALESCE(d.n1,rd.n1) WHEN 2 THEN COALESCE(d.n2,rd.n2) WHEN 3 THEN COALESCE(d.n3,rd.n3) WHEN 4 THEN COALESCE(d.n4,rd.n4) WHEN 5 THEN COALESCE(d.n5,rd.n5) WHEN 6 THEN COALESCE(d.n6,rd.n6) WHEN 7 THEN COALESCE(d.n7,rd.n7) WHEN 8 THEN COALESCE(d.n8,rd.n8) WHEN 9 THEN COALESCE(d.n9,rd.n9) WHEN 10 THEN COALESCE(d.n10,rd.n10) END "
                +"FROM "+OBS+" o LEFT JOIN "+DbHelper.TABLE_DRAWS+" d ON d.issue=o.target_issue "
                +"LEFT JOIN "+DrawDependencyInputCP12.TABLE+" rd ON rd.issue=o.target_issue AND d.issue IS NULL "
                +"LEFT JOIN "+FixedResearchWindowR932.CHAMPION+" ch ON ch.target_issue=o.target_issue AND ch.bundle_hash=o.bundle_hash AND ch.selection_hash=o.selection_hash "
                +"LEFT JOIN "+ChampionProbabilityEvidenceR82.TABLE+" cp ON cp.target_issue=ch.target_issue AND cp.bundle_hash=ch.bundle_hash AND cp.champion_prediction_hash=ch.prediction_hash AND cp.input_hash=ch.input_hash AND cp.selection_hash=ch.selection_hash AND cp.position=ch.position AND cp.n1=ch.n1 AND cp.n2=ch.n2 AND cp.n3=ch.n3 "
                +"WHERE o.study_hash=? ORDER BY o.target_issue LIMIT 242",new String[]{fit.study})) {
            while(c.moveToNext()) {
                if(n>=OBSERVATIONS)throw new IllegalStateException("R948_CAL_EXTRA_OBSERVATIONS");
                fit.issues[n]=c.getString(0);fit.scores[n]=c.getDouble(1);fit.hits[n]=c.getInt(2);
                long openTime=c.isNull(6)?DrawDependencyInputCP12.verifiedOpenTimeR77(db,fit.issues[n]):c.getLong(5);
                if(openTime<=0L)throw new IllegalStateException("R77_CAL_DRAW_TIME_UNAVAILABLE");
                if(c.getLong(4)<=0L||c.getLong(4)>=openTime)throw new IllegalStateException("R77_CAL_STRICT_TIME_REJECTED");
                if(!TargetIssueResolver.isValid(fit.issues[n])||!Double.isFinite(fit.scores[n])||(fit.hits[n]!=0&&fit.hits[n]!=1)
                        ||n>0&&fit.issues[n].compareTo(fit.issues[n-1])<=0)throw new IllegalStateException("R948_CAL_INVALID_FIXED_OBSERVATION");
                if(ResearchEvaluationRunR967.versioned(fit.key)&&(!ResearchEvaluationRunR967.eligibleEvaluation(fit.key,fit.issues[n])||n>0&&!fit.issues[n].equals(ResearchEvaluationRunR967.nextEvaluation(fit.key,fit.issues[n-1]))))throw new IllegalStateException("R969_CAL_EVALUATION_SCHEDULE_GAP");
                if(n>=121&&!c.isNull(7)&&!c.isNull(22)&&c.getLong(15)>0&&c.getLong(15)<openTime&&DecisionGateEngine.b12K3(c.getInt(12),c.getInt(13),c.getInt(14))){
                    double original=ChampionProbabilityEvidenceR82.verifiedProbability(c,16,fit.issues[n],c.getString(10),c.getString(7),c.getString(8),c.getString(9),c.getInt(11),c.getInt(12),c.getInt(13),c.getInt(14),openTime);
                    int actual=c.getInt(22);if(Double.isFinite(original)&&actual>=1&&actual<=10){fit.championP[n]=original;fit.championHits[n]=(actual==c.getInt(12)||actual==c.getInt(13)||actual==c.getInt(14))?1:0;}
                }
                evidence.append('|').append(fit.issues[n]).append(':').append(c.getString(3)).append(':').append(Double.toHexString(fit.scores[n])).append(':').append(fit.hits[n]).append(':').append(Double.toHexString(fit.championP[n])).append(':').append(Double.toHexString(fit.championHits[n])).append(':').append(c.getString(20));n++;
            }
        }
        if(n!=OBSERVATIONS)throw new IllegalStateException("R932_CAL_FIXED_SUPPORT");
        fit.evidenceHash=V51ChecksumEngine.sha256(evidence.toString());return fit;
    }
    static String fitModel(FitSnapshot fit) {
        try {
            JSONObject model=ResearchLogisticCalibrationR932.fitCompared(fit.scores,fit.hits);model.put("revision","R82_CANDIDATE_CAL_COMPARE_V1");
            model.put("study_hash",fit.study);model.put("fit_first",fit.issues[0]);model.put("fit_last",fit.issues[119]);model.put("embargo_issue",fit.issues[120]);model.put("validation_first",fit.issues[121]);model.put("validation_last",fit.issues[240]);
            model.put("evidence_hash",fit.evidenceHash);model.put("scope","CANDIDATE_ONLY_NOT_FINAL_MIXED_ACTION");
            ResearchLogisticCalibrationR932.validate(model,fit.scores,fit.hits);
            int championN=0;double championBrier=0,championNll=0;for(int i=121;i<241;i++)if(Double.isFinite(fit.championP[i])&&(fit.championHits[i]==0||fit.championHits[i]==1)){championN++;double p=fit.championP[i],y=fit.championHits[i];championBrier+=(p-y)*(p-y)/120;double bounded=Math.max(1e-12,Math.min(1-1e-12,p));championNll-=(y*Math.log(bounded)+(1-y)*Math.log1p(-bounded))/120;}
            model.put("validation_champion_probability_n",championN);model.put("validation_champion_probability_scope","SAME_OUTER120_ORIGINAL_RANK1_CALIBRATED_PROBABILITY_REAL_POSITION_LABELS");model.put("validation_champion_probability_status",championN==120?"COMPLETE":"N_A_MISSING_ORIGINAL_PROBABILITY");model.put("validation_champion_brier",championN==120?championBrier:JSONObject.NULL);model.put("validation_champion_nll",championN==120?championNll:JSONObject.NULL);
            model.put("quality_policy_hash",QUALITY_POLICY_HASH_CP12);
            Qualification quality=evaluateQualityCP12(model,fit.study);model.put("quality_state",quality.state.name());model.put("quality_reason",quality.reason);
            String json=model.toString();if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("R932_CAL_FIT_MODEL_BOUND");fit.preparedModelJson=json;return json;
        }catch(JSONException error){throw new IllegalStateException("R932_CAL_MODEL_SERIALIZATION",error);}
    }
    static boolean publishFit(SQLiteDatabase db,FitSnapshot fit,String json) {
        if(!db.inTransaction())throw new IllegalStateException("R948_CAL_PUBLISH_TRANSACTION_REQUIRED");
        if(fit==null||fit.preparedModelJson==null||!fit.preparedModelJson.equals(json))throw new IllegalStateException("CALIBRATION_UNPREPARED_MODEL");
        FitSnapshot current=loadFit(db,fit.study);
        if(current==null)return false; // another exactly-once publisher already won
        if(!fit.evidenceHash.equals(current.evidenceHash)||!fit.key.equals(current.key))throw new IllegalStateException("R948_CAL_FIT_SOURCE_CHANGED");
        db.execSQL("INSERT INTO "+MODEL+" VALUES(?,?,?,?,?,?,?)",new Object[]{fit.study,fit.key,fit.issues[119],fit.issues[240],json,V51ChecksumEngine.sha256(json),System.currentTimeMillis()});
        try{JSONObject model=new JSONObject(json);Qualification quality=evaluateQualityCP12(model,fit.study);if(!quality.state.name().equals(model.getString("quality_state"))||!quality.reason.equals(model.getString("quality_reason")))throw new IllegalStateException("CALIBRATION_QUALITY_JSON_CONFLICT");
            db.execSQL("INSERT INTO "+QUALITY_CP12+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{fit.study,V51ChecksumEngine.sha256(json),quality.state.name(),quality.reason,QUALITY_POLICY_HASH_CP12,quality.ece,quality.brier,quality.baselineBrier,quality.validationUntil,System.currentTimeMillis()});
        }catch(JSONException bad){throw new IllegalStateException("R932_CAL_MODEL_JSON",bad);}
        db.execSQL("UPDATE "+STUDY+" SET state='FROZEN' WHERE study_hash=? AND state='READY'",new Object[]{fit.study});return true;
    }
    /** Production entry: 160 training epochs run outside the single database writer. */
    static void fitOne(final DbStore store) {
        ResearchWorkBudgetR927.checkpoint();
        final String selected;
        try(Cursor c=store.readable().rawQuery("SELECT study_hash FROM "+STUDY+" WHERE state='READY' ORDER BY first_issue,study_hash LIMIT 1",null)){if(!c.moveToFirst())return;selected=c.getString(0);}
        final FitSnapshot fit;
        try{fit=loadFit(store.readable(),selected);}catch(final IllegalStateException invalid){
            String message=String.valueOf(invalid.getMessage());
            if(!(message.equals("R948_CAL_EXTRA_OBSERVATIONS")||message.equals("R948_CAL_INVALID_FIXED_OBSERVATION")||message.equals("R969_CAL_EVALUATION_SCHEDULE_GAP")||message.equals("R932_CAL_FIXED_SUPPORT")
                    ||message.equals("R77_CAL_STRICT_TIME_REJECTED")||message.equals("R77_CAL_DRAW_TIME_UNAVAILABLE")||message.equals("R77_CAL_RETAINED_INPUT_INVALID")||message.equals("R82_CAL_POLICY_OR_CURRENT_HEAD_MISMATCH")||message.equals("R969_CAL_FROZEN_VERSION_DRIFT")||message.equals("R932_CAL_UNREGISTERED_CANDIDATE")))throw invalid;
            // One corrupt/partial READY study must not monopolize all later studies.
            // Preserve every original observation and its failure; no invented labels.
            boolean marked=store.coordinatorR36().runAtomicWrite("R975_CAL_INVALID_INPUT",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure SELECT study_hash,candidate_key,?,? FROM "+STUDY+" WHERE study_hash=? AND state='READY'",new Object[]{invalid.getMessage(),System.currentTimeMillis(),selected});
                db.execSQL("UPDATE "+STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state='READY'",new Object[]{selected});
                db.execSQL("UPDATE "+GlobalResearchScheduleR932.RESERVATION_CP11+" SET state='FAILED_GAP',last_reason=?,generation=generation+1 WHERE study_hash=? AND state IN('RESERVED','ACTIVE','COMPLETE')"
                        +" AND EXISTS(SELECT 1 FROM "+STUDY+" s WHERE s.study_hash=? AND s.state='BLOCKED_INVALID_INPUT')",new Object[]{invalid.getMessage(),selected,selected});
            }});
            if(!marked)throw new IllegalStateException("R975_CAL_FAILURE_RECEIPT_NOT_COMMITTED",invalid);
            return;
        }
        if(fit==null)return;
        final String json;
        try { json=fitModel(fit); }
        catch(final RuntimeException numeric) {
            final String reason=String.valueOf(numeric.getMessage());
            if(!(reason.startsWith("R932_CAL_FIT_")||reason.startsWith("R932_CAL_HOLDOUT_")
                    ||reason.equals("R932_CAL_MODEL_SERIALIZATION")||reason.equals("R932_CAL_SCORE_NONFINITE")))throw numeric;
            boolean recorded=store.coordinatorR36().runAtomicWrite("CP5_CAL_NUMERIC_FAILURE",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase db){
                    FitSnapshot current=loadFit(db,fit.study);
                    if(current==null)return;
                    if(!fit.evidenceHash.equals(current.evidenceHash))throw new IllegalStateException("CP5_CAL_FAILURE_SOURCE_DRIFT");
                    db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure VALUES(?,?,?,?)",new Object[]{fit.study,fit.key,reason,System.currentTimeMillis()});
                    db.execSQL("UPDATE "+STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state='READY'",new Object[]{fit.study});
                }
            });
            if(!recorded)throw new IllegalStateException("CP5_CAL_NUMERIC_RECEIPT_NOT_COMMITTED",numeric);
            return;
        }
        ResearchWorkBudgetR927.checkpoint();
        Boolean ok=store.coordinatorR36().runAtomicWrite("R948_CAL_MODEL_PUBLISH",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db){return publishFit(db,fit,json);}
        },null);
        if(ok==null)throw new IllegalStateException("R948_CAL_MODEL_PUBLISH_FAILED");
    }
    /** Retained deterministic fixture entry; production callers must use fitOne(DbStore). */
    static void fitOne(SQLiteDatabase db) {if(db==null||db.inTransaction())throw new IllegalStateException("R932_CAL_FIT_REQUIRES_NO_WRITER");FitSnapshot fit=loadFit(db,null);if(fit!=null)publishFit(db,fit,fitModel(fit));}
}
