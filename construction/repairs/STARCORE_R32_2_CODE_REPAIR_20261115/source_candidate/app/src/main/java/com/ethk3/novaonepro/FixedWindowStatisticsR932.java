package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** One terminal calculation. Block-t is diagnostic; the p bound states its conditional-null assumption. */
public final class FixedWindowStatisticsR932 {
    public static final String TABLE="research_fixed_window_statistics_r932";
    private FixedWindowStatisticsR932(){}
    static void schema(SQLiteDatabase db){
        if (TransactionSchemaCacheR941.readyCommitted(db,"FixedWindowStatisticsR932")) return;
db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(window_id TEXT PRIMARY KEY,metrics_json TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        TransactionSchemaCacheR941.markCommitted(db,"FixedWindowStatisticsR932");
    }
    static final class Result {final String state,json,hash;final Double rawP;Result(String state,String json,String hash,Double rawP){this.state=state;this.json=json;this.hash=hash;this.rawP=rawP;}}
    static Result evaluate(SQLiteDatabase db,String window,int planned,String endpoint) {
        String candidate,first,last;
        try(Cursor registered=db.rawQuery("SELECT planned_n,last_issue,candidate_key,first_issue FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id=?",new String[]{window})){
            if(!registered.moveToFirst()||planned!=registered.getInt(0))throw new IllegalStateException("R968_EVALUATION_DENOMINATOR_MISMATCH");
            last=registered.getString(1);candidate=registered.getString(2);first=registered.getString(3);
            if(endpoint.compareTo(last)<0)throw new IllegalStateException("R932_ENDPOINT_NOT_REGISTERED_OR_EARLY");
        }
        ResearchGatePlanR967.Plan plan=ResearchGatePlanR967.beforeEvaluation(db,window);
        ResearchGatePlanR967.Policy policy=plan==null?null:plan.policy;
        if(policy!=null&&policy.isCp3)return evaluateCp4(db,window,planned,endpoint,plan,candidate,first,last);
        int n=0,hits=0,base=0,probN=0,pending=0;double brier=0,ece=0;int[] bins=new int[10];double[] probSum=new double[10],hitSum=new double[10];
        ArrayList<Double> deltas=new ArrayList<Double>(),errors=new ArrayList<Double>();StringBuilder evidence=new StringBuilder(window);String calibration="";boolean calibrationDrift=false;boolean scheduleGap=false;String expected=first;int captured=0;
        try(Cursor rows=db.rawQuery("SELECT target_issue,candidate_hit,champion_hit,selected_hit_p,calibration_hash,candidate_hash,champion_prediction_hash FROM "+FixedResearchWindowR932.PAIR+" WHERE window_id=? ORDER BY target_issue LIMIT 1201",new String[]{window})) {
            while(rows.moveToNext()) {
                ResearchWorkBudgetR927.checkpoint();String issue=rows.getString(0);
                if(ResearchEvaluationRunR967.versioned(candidate)){
                    if(!ResearchEvaluationRunR967.eligibleEvaluation(candidate,issue)||issue.compareTo(first)<0||issue.compareTo(last)>0||captured>=planned)throw new IllegalStateException("R969_FORMAL_OBSERVATION_OUTSIDE_PLAN");
                    if(!issue.equals(expected))scheduleGap=true;expected=ResearchEvaluationRunR967.nextEvaluation(candidate,issue);captured++;
                }
                int hit=rows.getInt(1),baseline=rows.getInt(2);if(hit>1||baseline>1)throw new IllegalStateException("R969_FORMAL_HIT_NOT_BINARY");if(hit<0||baseline<0){pending++;continue;}n++;hits+=hit;base+=baseline;deltas.add((double)(hit-baseline));
                double probability=rows.getDouble(3);String cal=rows.getString(4);
                if(Double.isFinite(probability)&&probability>=0&&probability<=1&&cal.length()==64){probN++;double error=(probability-hit)*(probability-hit);brier+=error;errors.add(error);int bin=Math.min(9,(int)(probability*10));bins[bin]++;probSum[bin]+=probability;hitSum[bin]+=hit;if(calibration.length()==0)calibration=cal;else if(!calibration.equals(cal))calibrationDrift=true;}
                else errors.add(Double.NaN);
                evidence.append('|').append(rows.getString(0)).append(':').append(hit).append(':').append(baseline).append(':').append(rows.getString(5)).append(':').append(rows.getString(6)).append(':').append(Double.toHexString(probability)).append(':').append(cal);
            }
        }
        if(pending>0)return null;
        double alpha;try(Cursor c=db.rawQuery("SELECT alpha,last_issue FROM "+ResearchMultiplicityR932.LOOK+" WHERE window_id=?",new String[]{window})) {
            if(!c.moveToFirst()||endpoint.compareTo(c.getString(1))<0)throw new IllegalStateException("R932_ENDPOINT_NOT_REGISTERED_OR_EARLY");alpha=c.getDouble(0);
        }
        double rate=n==0?0:hits/(double)n,delta=n==0?0:(hits-base)/(double)n,z=1.959963984540054;
        double lower=n==0?0:(rate+z*z/(2*n)-z*Math.sqrt(rate*(1-rate)/n+z*z/(4d*n*n)))/(1+z*z/n);
        double blockLower=PromotionWindowR925.stats(deltas,errors,0,n)[1];
        // Azuma-Hoeffding for bounded paired deltas in [-1,1], ONLY under E[D_t|F_(t-1)]<=0.
        double rawP=ResearchMultiplicityR932.pairedP(n,hits-base),adjusted=Math.min(1,rawP*ResearchMultiplicityR932.TOTAL_ALPHA/alpha);
        for(int i=0;i<10;i++)ece+=Math.abs(probSum[i]-hitSum[i])/Math.max(1,n);
        brier=probN==n&&n>0?brier/n:-1;ece=probN==n&&n>0?ece:-1;
        String state=(scheduleGap||n!=planned)?"PAUSED_CAPTURE":probN!=n?"FAIL_PROBABILITY_CONTRACT":calibrationDrift?"FAIL_CALIBRATION_DRIFT":
            rate>=(policy==null?CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE:policy.minRate)&&lower>=(policy==null?.30:policy.minLower)&&brier<=(policy==null?.24:policy.maxBrier)&&ece<=(policy==null?.04:policy.maxEce)&&delta>(policy==null?0:policy.minDelta)&&blockLower>(policy==null?0:policy.minBlock)&&adjusted<=(policy==null?.05:policy.totalAlpha)?"PASS":"FAIL_EFFECT";
        try {
            if(plan!=null&&!plan.calibrationHash.equals(calibration)&&n>0)state="FAIL_PLAN_CALIBRATION_MISMATCH";
            JSONObject json=new JSONObject();
            json.put("plan_hash",plan==null?"LEGACY_NO_R967_PLAN":plan.hash);
            json.put("policy_hash",policy==null?"LEGACY_R932":policy.hash);
            json.put("policy_revision",policy==null?"LEGACY_R932":policy.revision);
            json.put("policy_consumer_cap",policy==null?.05:policy.consumerCap);
            json.put("evaluation_schedule",ResearchEvaluationRunR967.versioned(candidate)?ResearchEvaluationRunR967.SPLIT:"LEGACY_ALL_PREREGISTERED_ISSUES");
            json.put("window_id",window);json.put("state",state);json.put("planned_n",planned);json.put("paired_n",n);json.put("candidate_hits",hits);json.put("champion_hits",base);json.put("probability_n",probN);
            json.put("hit_rate",rate);json.put("wilson_lower",lower);json.put("paired_delta",delta);json.put("brier",brier);json.put("ece",ece);json.put("calibration_hash",calibration);json.put("reliability_bin_n",new JSONArray(bins));json.put("allocated_alpha",alpha);json.put("raw_p",rawP);json.put("fwer_adjusted_p",adjusted);
            json.put("block_size",20);json.put("block_t_lower_diagnostic",blockLower);json.put("block_t_assumptions","20-issue blocks; t approximation; not unconditional significance");
            json.put("p_bound_assumption","bounded paired deltas in [-1,1] with conditional null E[D_t|past]<=0; no guarantee under arbitrary dependence");
            json.put("evidence_hash",V51ChecksumEngine.sha256(evidence.toString()));json.put("production_authority",false);json.put("scope","CANDIDATE_FIXED_WINDOW_ONLY; GLOBAL_FINAL_SELECTOR_CALIBRATION_AND_ALL_HARD_GATES_STILL_REQUIRED");
            String text=json.toString();return new Result(state,text,V51ChecksumEngine.sha256(text),rawP);
        }catch(JSONException error){throw new IllegalStateException("R932_ENDPOINT_JSON",error);}
    }
    static void persist(SQLiteDatabase db,String window,Result result) {
        db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?)",new Object[]{window,result.json,result.hash,System.currentTimeMillis()});
    }

    static Result evaluateCp4(SQLiteDatabase db,String window,int planned,String endpoint,ResearchGatePlanR967.Plan plan,String candidate,String first,String last){
        int n=0,hits=0,base=0;double brier=0,baseBrier=0;double[] pbin=new double[10],ybin=new double[10];String expected=first;StringBuilder evidence=new StringBuilder(plan.hash);boolean gap=false;
        try(Cursor c=db.rawQuery("SELECT target_issue,candidate_hit,champion_hit,selected_hit_p,calibration_hash,candidate_hash,champion_prediction_hash FROM "+FixedResearchWindowR932.PAIR+" WHERE window_id=? ORDER BY target_issue",new String[]{window})){
            while(c.moveToNext()){
                if(c.getInt(1)<0||c.getInt(2)<0)return null;
                String issue=c.getString(0);if(!issue.equals(expected)||issue.compareTo(last)>0)gap=true;expected=ResearchEvaluationRunR967.nextEvaluation(candidate,issue);
                int y=c.getInt(1),by=c.getInt(2);double p=c.getDouble(3);
                if((y!=0&&y!=1)||(by!=0&&by!=1)||!Double.isFinite(p)||p<0||p>1||!plan.calibrationHash.equals(c.getString(4)))throw new IllegalStateException("CP4_FORMAL_PROBABILITY_OR_LABEL");
                n++;hits+=y;base+=by;brier+=(p-y)*(p-y);baseBrier+=(.3-y)*(.3-y);int bin=Math.min(9,(int)(p*10));pbin[bin]+=p;ybin[bin]+=y;
                evidence.append('|').append(issue).append('|').append(c.getString(5)).append('|').append(c.getString(6)).append('|').append(y).append('|').append(by).append('|').append(Double.toHexString(p));
            }
        }
        String state=gap||n!=planned?"PAUSED_CAPTURE":"PASS";double ece=0;for(int i=0;i<10;i++)ece+=Math.abs(pbin[i]-ybin[i]);
        try{JSONObject j=new JSONObject();j.put("revision","CP4_ENDPOINT_INTEGRITY_V1");j.put("window_id",window);j.put("state",state);j.put("planned_n",planned);j.put("paired_n",n);j.put("candidate_hits",hits);j.put("champion_hits",base);j.put("plan_hash",plan.hash);j.put("policy_hash",plan.policy.hash);j.put("calibration_hash",plan.calibrationHash);j.put("brier",n==0?JSONObject.NULL:brier/n);j.put("baseline_brier",n==0?JSONObject.NULL:baseBrier/n);j.put("ece",n==0?JSONObject.NULL:ece/n);j.put("evidence_hash",V51ChecksumEngine.sha256(evidence.toString()));j.put("certificate_authority",false);j.put("stage_pass_meaning","COMPLETE_IMMUTABLE_ENDPOINT_NOT_SIGNIFICANCE");String json=j.toString();return new Result(state,json,V51ChecksumEngine.sha256(json),null);}catch(JSONException e){throw new IllegalStateException("CP4_ENDPOINT_JSON",e);}
    }

}
