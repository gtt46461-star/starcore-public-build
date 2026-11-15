package com.ethk3.novaonepro;
import java.util.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import org.json.*;

/** Signed POLICY component's earlier-TUNE monitor; no fit or mutable threshold at inference. */
public final class FrozenDriftPolicyR82 {
    public static final String REVISION="R82_FROZEN_DRIFT_TUNE_V1";
    public static final String PROTOCOL="R82_JS_RECENT20_PRECEDING_REFERENCE_LAPLACE.5_ISSUE_MAX10_STRICT_GREATER_V1";
    public static final String PROTOCOL_HASH=V51ChecksumEngine.sha256(PROTOCOL);
    private FrozenDriftPolicyR82(){}
    public static final class Policy {
        public final String hash,tuneFirst,tuneLast,sourceRowsHash,trialGridHash;
        public final int referenceWindow,cooldownIssues,tuneN;
        public final double threshold,falseAlarmBudget;
        private Policy(JSONObject value)throws JSONException{
            Map<String,Object> body=new TreeMap<>(StrictCanonicalJson.readObject(value.toString()));Object hash=body.remove("policy_hash");
            if(!(hash instanceof String)||!hash.equals(V51ChecksumEngine.sha256(StrictCanonicalJson.write(body))))throw new IllegalArgumentException("R82_DRIFT_POLICY_HASH");this.hash=(String)hash;
            Set<String> keys=new HashSet<>(Arrays.asList("revision","protocol_hash","tune_first","tune_last","tune_n","short_window","reference_window","cooldown_issues","threshold_hex","false_alarm_budget_hex","tune_exceedance_n","source_rows_hash","source_max_issue","trial_grid_hash","budget_interpretation"));
            if(!body.keySet().equals(keys)||!REVISION.equals(value.getString("revision"))||!PROTOCOL_HASH.equals(value.getString("protocol_hash")))throw new IllegalArgumentException("R82_DRIFT_POLICY_PROTOCOL");
            tuneFirst=value.getString("tune_first");tuneLast=value.getString("tune_last");tuneN=value.getInt("tune_n");referenceWindow=value.getInt("reference_window");cooldownIssues=value.getInt("cooldown_issues");threshold=Double.valueOf(value.getString("threshold_hex"));falseAlarmBudget=Double.valueOf(value.getString("false_alarm_budget_hex"));sourceRowsHash=value.getString("source_rows_hash");trialGridHash=value.getString("trial_grid_hash");
            if(!TargetIssueResolver.isValid(tuneFirst)||!TargetIssueResolver.isValid(tuneLast)||tuneFirst.compareTo(tuneLast)>0||!tuneLast.equals(value.getString("source_max_issue"))||tuneN<120||tuneN>100000||value.getInt("short_window")!=20||!(referenceWindow==120||referenceWindow==300||referenceWindow==1000)||!(cooldownIssues==3||cooldownIssues==5||cooldownIssues==10)||!Double.isFinite(threshold)||threshold<0||threshold>1||!Double.isFinite(falseAlarmBudget)||falseAlarmBudget<.001||falseAlarmBudget>.25||value.getInt("tune_exceedance_n")<0||value.getInt("tune_exceedance_n")>Math.floor(falseAlarmBudget*tuneN+1e-9)||!sourceRowsHash.matches("[0-9a-f]{64}")||!trialGridHash.matches("[0-9a-f]{64}")||!"EMPIRICAL_TUNE_ISSUE_MAX10_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE".equals(value.getString("budget_interpretation")))throw new IllegalArgumentException("R82_DRIFT_POLICY_CONTRACT");
        }
    }
    public static Policy parse(JSONObject value){if(value==null)return null;try{return new Policy(value);}catch(JSONException malformed){throw new IllegalArgumentException("R82_DRIFT_POLICY_JSON",malformed);}}
    public static final class Observation {
        public final String policyHash,reason,receiptJson,baseIssue,targetIssue;
        public final boolean ready,triggered;
        public final long targetOrdinal;
        private Observation(String policy,String reason,String receipt,String base,String target,boolean ready,boolean trigger){policyHash=policy;this.reason=reason;receiptJson=receipt;baseIssue=base;targetIssue=target;this.ready=ready;triggered=trigger;targetOrdinal=ordinal(target);}
    }
    /** Current caller supplies the original immutable PIT history; this method performs no SQL. */
    public static Observation observe(Policy policy,int position,List<Models.Draw> frozen,String base,String target){
        String reason="",input="";double risk=Double.NaN;boolean triggered=false;String hash=policy==null?"":policy.hash;
        if(policy==null)reason="WAIT_FROZEN_DRIFT_TUNE_ARTIFACT";
        else if(!TargetIssueResolver.exact(base,target).valid||target.compareTo(policy.tuneLast)<=0)reason="DRIFT_POLICY_NOT_STRICTLY_EARLIER_THAN_TARGET";
        else if(position<1||position>10||frozen==null||frozen.size()<policy.referenceWindow+20)reason="WAIT_DRIFT_REFERENCE_HISTORY";
        else{
            ArrayList<Models.Draw> rows=new ArrayList<>(frozen);Collections.sort(rows,Comparator.comparing(d->d==null||d.issue==null?"":d.issue));int first=rows.size()-policy.referenceWindow-20;int[] shortCounts=new int[10],reference=new int[10];String prior="";StringBuilder wire=new StringBuilder(PROTOCOL_HASH).append('|').append(policy.hash).append('|').append(position);
            for(int i=first;i<rows.size();i++){
                Models.Draw draw=rows.get(i);if(draw==null||!draw.isValid()||draw.dirty!=0||draw.issue.compareTo(target)>=0||!prior.isEmpty()&&!TargetIssueResolver.exact(prior,draw.issue).valid){reason="DRIFT_REFERENCE_GAP_OR_INVALID_PIT";break;}
                prior=draw.issue;int digit=draw.getNumberAtPosition(position);if(digit<1||digit>10){reason="DRIFT_REFERENCE_DIGIT_INVALID";break;}if(i<rows.size()-20)reference[digit-1]++;else shortCounts[digit-1]++;wire.append('|').append(draw.issue).append(':').append(digit);
            }
            if(reason.isEmpty()&&!prior.equals(base))reason="DRIFT_REFERENCE_BASE_MISMATCH";
            if(reason.isEmpty()){risk=js(shortCounts,reference);triggered=risk>policy.threshold;input=V51ChecksumEngine.sha256(wire.toString());}
        }
        try{JSONObject receipt=new JSONObject();receipt.put("revision",REVISION);receipt.put("policy_hash",hash);receipt.put("target_issue",target);receipt.put("base_issue",base);receipt.put("position",position);receipt.put("input_hash",input);receipt.put("risk_js",Double.isFinite(risk)?risk:JSONObject.NULL);receipt.put("reason",reason);receipt.put("triggered",triggered);receipt.put("reference_window",policy==null?JSONObject.NULL:policy.referenceWindow);receipt.put("cooldown_issues",policy==null?JSONObject.NULL:policy.cooldownIssues);receipt.put("threshold",policy==null?JSONObject.NULL:policy.threshold);receipt.put("false_alarm_budget",policy==null?JSONObject.NULL:policy.falseAlarmBudget);receipt.put("budget_scope","EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE");receipt.put("may_authorize_real",false);return new Observation(hash,reason,receipt.toString(),base,target,reason.isEmpty(),triggered);}catch(JSONException impossible){throw new IllegalStateException("R82_DRIFT_RECEIPT_JSON",impossible);}
    }
    public static double js(int[] shortCounts,int[] reference){
        if(shortCounts==null||reference==null||shortCounts.length!=10||reference.length!=10)throw new IllegalArgumentException("R82_DRIFT_JS_SHAPE");double n=0,m=0;for(int i=0;i<10;i++){if(shortCounts[i]<0||reference[i]<0)throw new IllegalArgumentException("R82_DRIFT_JS_COUNT");n+=shortCounts[i];m+=reference[i];}if(n<=0||m<=0)throw new IllegalArgumentException("R82_DRIFT_JS_SUPPORT");double js=0;for(int i=0;i<10;i++){double a=(shortCounts[i]+.5)/(n+5),b=(reference[i]+.5)/(m+5),mean=(a+b)/2;js+=.5*(a*Math.log(a/mean)+b*Math.log(b/mean));}return Math.max(0,Math.min(1,js/Math.log(2)));
    }
    /** UTC issue ordinal is only a gap/cooldown clock, never an outcome. */
    public static long ordinal(String issue){if(!TargetIssueResolver.isValid(issue))return -1;try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyyMMdd",Locale.ROOT);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.parse(issue.substring(0,8)).getTime()/86400000L*1440+Integer.parseInt(issue.substring(8))-1;}catch(java.text.ParseException bad){return -1;}}
}
