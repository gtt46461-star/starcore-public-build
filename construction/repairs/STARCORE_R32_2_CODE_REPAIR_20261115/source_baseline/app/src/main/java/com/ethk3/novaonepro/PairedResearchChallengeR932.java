package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** New, pre-registered future comparisons for repair outcomes and family incumbent replacement. */
public final class PairedResearchChallengeR932 {
    public static final String TABLE="research_paired_challenge_r932",PREDICTION="research_challenge_prediction_r932";
    public static final int PLANNED=120;
    private PairedResearchChallengeR932(){}
    /** Closed quality candidates may execute only as a fixed comparison reference, never influence. */
    static String referenceCondition(String keySql,String target){
        if(!TargetIssueResolver.isValid(target))throw new IllegalArgumentException("R932_REFERENCE_TARGET");
        if(ResearchGatePlanR967.installed().isCp3)return "0=1";
        return "EXISTS(SELECT 1 FROM "+TABLE+" ref JOIN "+ResearchLifecycleR932.TABLE+" life ON life.candidate_key=ref.parent_key WHERE ref.parent_key=("+keySql+") AND ref.state='OPEN' AND ref.first_issue<='"+target+"' AND ref.last_issue>='"+target+"' AND life.state='QUALITY_PRUNED' AND life.structural_hash=ref.parent_structure)";
    }
    static boolean requiresReference(SQLiteDatabase db,String key,String target){
        try(Cursor c=db.rawQuery("SELECT 1 WHERE "+referenceCondition("?",target),new String[]{key})){return c.moveToFirst();}
    }
    static void schema(SQLiteDatabase db){
        if (TransactionSchemaCacheR941.readyCommitted(db,"PairedResearchChallengeR932")) return;

        CandidateCalibrationR932.schema(db);ResearchMultiplicityR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(challenge_id TEXT PRIMARY KEY,purpose TEXT NOT NULL,parent_key TEXT NOT NULL,child_key TEXT NOT NULL,parent_structure TEXT NOT NULL,child_structure TEXT NOT NULL,reason_hash TEXT NOT NULL,state TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,parent_cal TEXT NOT NULL,child_cal TEXT NOT NULL,contract_hash TEXT NOT NULL,result_json TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_challenge_endpoint ON "+TABLE+"(state,last_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PREDICTION+"(challenge_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,prediction_hash TEXT NOT NULL,ordinal INTEGER NOT NULL,probability REAL NOT NULL,calibration_hash TEXT NOT NULL,cost_ns INTEGER NOT NULL,hit INTEGER NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(challenge_id,target_issue,candidate_key))");
    
        TransactionSchemaCacheR941.markCommitted(db,"PairedResearchChallengeR932");
    }
    static String request(SQLiteDatabase db,String purpose,String parent,String child,String parentStructure,String childStructure,String reason){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        if(!db.inTransaction()||!Arrays.asList("REPAIR","FAMILY_REPLACEMENT").contains(purpose)||parent.equals(child)||parentStructure.equals(childStructure)||!reason.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R932_PAIRED_CHALLENGE_IDENTITY");
        schema(db);String id=V51ChecksumEngine.sha256("R932_NEW_FUTURE_PAIRED_120|"+purpose+'|'+parent+'|'+child+'|'+parentStructure+'|'+childStructure);
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,'WAIT_EARLIER_CALIBRATION','','','','','','','',?,?)",new Object[]{id,purpose,parent,child,parentStructure,childStructure,reason,System.currentTimeMillis(),System.currentTimeMillis()});
        return id;
    }
    /** Called before ordinary window early returns, in the actual full1200 prediction transaction. */
    static void freeze(SQLiteDatabase db,String key,String structure,String target,String prediction,double raw,int position,int n1,int n2,int n3){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        schema(db);if(!db.inTransaction())throw new IllegalStateException("R932_CHALLENGE_TRANSACTION_REQUIRED");
        try(Cursor actual=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{target})){if(actual.moveToFirst())throw new IllegalStateException("R932_CHALLENGE_POSTDRAW_FREEZE");}
        if(!Double.isFinite(raw))return;
        try(Cursor c=db.rawQuery("SELECT challenge_id,parent_key,parent_structure,child_structure,parent_cal,child_cal,contract_hash,child_key FROM "+TABLE+" WHERE state='OPEN' AND first_issue<=? AND last_issue>=? AND (parent_key=? OR child_key=?) LIMIT 8",new String[]{target,target,key,key})){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();
                if(!ResearchEvaluationRunR967.eligibleEvaluation(c.getString(1),target)||!ResearchEvaluationRunR967.eligibleEvaluation(c.getString(7),target))continue;
                boolean parent=key.equals(c.getString(1));
                if(!structure.equals(c.getString(parent?2:3))||!contract(db).equals(c.getString(6)))continue;
                String cal=c.getString(parent?4:5),json="";
                try(Cursor m=db.rawQuery("SELECT model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key=? AND model_hash=? LIMIT 1",new String[]{key,cal})){
                    if(!m.moveToFirst()||target.compareTo(m.getString(1))<=0)throw new IllegalStateException("R932_CHALLENGE_EARLIER_CAL_MISSING");json=m.getString(0);
                }
                if(!cal.equals(V51ChecksumEngine.sha256(json)))throw new IllegalStateException("R932_CHALLENGE_CAL_HASH");
                long cost;
                try(Cursor e=db.rawQuery("SELECT elapsed_ns,prediction_hash FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key=? AND target_issue=? AND structural_hash=? AND action_count=1200",new String[]{key,target,structure})){
                    if(!e.moveToFirst()||!prediction.equals(e.getString(1)))throw new IllegalStateException("R932_CHALLENGE_FULL1200_PROOF_MISSING");cost=e.getLong(0);
                }
                try{
                    double p=CandidateCalibrationR932.predict(new JSONObject(json),raw);
                    db.execSQL("INSERT OR IGNORE INTO "+PREDICTION+" VALUES(?,?,?,?,?,?,?,?,-1,?)",new Object[]{c.getString(0),target,key,prediction,K3ActionSpaceEngine.globalOrdinal(position,n1,n2,n3),p,cal,cost,System.currentTimeMillis()});
                    try(Cursor old=db.rawQuery("SELECT prediction_hash FROM "+PREDICTION+" WHERE challenge_id=? AND target_issue=? AND candidate_key=?",new String[]{c.getString(0),target,key})){if(!old.moveToFirst()||!prediction.equals(old.getString(0)))throw new IllegalStateException("R932_CHALLENGE_FREEZE_CONFLICT");}
                }catch(JSONException error){throw new IllegalStateException("R932_CHALLENGE_CAL_JSON",error);}
            }
        }
    }
    static void settle(SQLiteDatabase db,Models.Draw draw){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        schema(db);
        try(Cursor c=db.rawQuery("SELECT challenge_id,candidate_key,ordinal FROM "+PREDICTION+" WHERE target_issue=? AND hit=-1 ORDER BY challenge_id,candidate_key LIMIT 16",new String[]{draw.issue})){
            int completed=0;
            while(c.moveToNext()){
                try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield y){if(completed==0)throw y;break;}
                int ordinal=c.getInt(2),position=ordinal/120+1,local=ordinal%120,index=0;int a=0,b=0,d=0;
                outer:for(int x=1;x<=8;x++)for(int y=x+1;y<=9;y++)for(int z=y+1;z<=10;z++)if(index++==local){a=x;b=y;d=z;break outer;}
                db.execSQL("UPDATE "+PREDICTION+" SET hit=? WHERE challenge_id=? AND candidate_key=? AND target_issue=? AND hit=-1",new Object[]{draw.hitStrictPositionK3(position,a,b,d)?1:0,c.getString(0),c.getString(1),draw.issue});completed++;
            }
        }
    }
    public static void maintain(final DbStore store){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        String latest="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(latest.isEmpty())return;
        String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");if(active.isEmpty())active=ModelBundleRuntimeR932.current().hash;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,TargetIssueResolver.next(latest).targetIssue,System.currentTimeMillis())){
        if(!store.coordinatorR36().runAtomicWrite("R932_NEW_FUTURE_CHALLENGES",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            schema(db);ResearchLifecycleR932.schema(db);ResearchCohortR932.schema(db);
            String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}if(latest.isEmpty())return;
            try(Cursor c=db.rawQuery("SELECT child_key,parent_key,parent_structure,child_structure,plan_hash FROM "+ResearchLifecycleR932.CHILD+" WHERE challenged_id='' ORDER BY created_at LIMIT 1",null)){
                if(c.moveToFirst()){String id=request(db,"REPAIR",c.getString(1),c.getString(0),c.getString(2),c.getString(3),c.getString(4));db.execSQL("UPDATE "+ResearchLifecycleR932.CHILD+" SET challenged_id=? WHERE child_key=? AND challenged_id=''",new Object[]{id,c.getString(0)});}
            }
            try(Cursor c=db.rawQuery("SELECT challenge_id,parent_key,child_key,parent_structure,child_structure FROM "+TABLE+" WHERE state='WAIT_EARLIER_CALIBRATION' ORDER BY updated_at,challenge_id LIMIT 1",null)){
                if(c.moveToFirst())activate(db,c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),TargetIssueResolver.next(latest).targetIssue);
            }
            try(Cursor c=db.rawQuery("SELECT challenge_id FROM "+TABLE+" WHERE state='OPEN' AND last_issue<=? ORDER BY last_issue,challenge_id LIMIT 1",new String[]{latest})){
                if(c.moveToFirst())evaluate(db,c.getString(0),latest);
            }
        }}))throw new IllegalStateException("R932_CHALLENGE_MAINTENANCE_COMMIT");
        }
    }
    static void activate(SQLiteDatabase db,String id,String parent,String child,String ps,String cs,String first){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        String streamKey=ResearchEvaluationRunR967.versioned(parent)?parent:child;
        if(!ResearchEvaluationRunR967.eligibleEvaluation(streamKey,first))first=ResearchEvaluationRunR967.nextEvaluation(streamKey,first);
        String pc=calibration(db,parent,ps,first),cc=calibration(db,child,cs,first);
        db.execSQL("UPDATE "+TABLE+" SET updated_at=? WHERE challenge_id=?",new Object[]{System.currentTimeMillis(),id});
        if(pc.isEmpty()||cc.isEmpty())return;
        String family="";try(Cursor c=db.rawQuery("SELECT h.family FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{child})){if(c.moveToFirst())family=c.getString(0);}
        if(family.isEmpty())return;
        String last=ResearchEvaluationRunR967.endpoint(streamKey,first,PLANNED);
        // A separate comparison claim pays new alpha in the existing immutable statistical family.
        ResearchMultiplicityR932.registerCandidate(db,"paired_claim:"+id,family,V51ChecksumEngine.sha256("COMPARISON_CLAIM|"+id),first);
        ResearchMultiplicityR932.registerLook(db,"paired_claim:"+id,id,1,first,last);
        db.execSQL("UPDATE "+TABLE+" SET state='OPEN',first_issue=?,last_issue=?,parent_cal=?,child_cal=?,contract_hash=? WHERE challenge_id=? AND state='WAIT_EARLIER_CALIBRATION'",new Object[]{first,last,pc,cc,contract(db),id});
    }
    private static String calibration(SQLiteDatabase db,String key,String structure,String first){
        String study;try{study=CandidateCalibrationR932.study(db,key,structure,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash);}catch(IllegalStateException absent){return "";}
        try(Cursor c=db.rawQuery("SELECT model_hash,model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE study_hash=?",new String[]{study})){
            if(!c.moveToFirst()||c.getString(2).compareTo(first)>=0)return "";
            if(!c.getString(0).equals(V51ChecksumEngine.sha256(c.getString(1))))throw new IllegalStateException("R932_CHALLENGE_CALIBRATION_HASH");
            try{JSONObject m=new JSONObject(c.getString(1));return m.getDouble("validation_brier")<=.21&&m.getDouble("validation_ece")<=.04?c.getString(0):"";}catch(JSONException e){throw new IllegalStateException(e);}
        }
    }
    static void evaluate(SQLiteDatabase db,String id,String endpoint){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        String parent,child,parentStructure,childStructure,first,last,frozen,purpose;
        try(Cursor c=db.rawQuery("SELECT parent_key,child_key,parent_structure,child_structure,first_issue,last_issue,contract_hash,purpose,state FROM "+TABLE+" WHERE challenge_id=?",new String[]{id})){
            if(!c.moveToFirst()||!"OPEN".equals(c.getString(8)))return;parent=c.getString(0);child=c.getString(1);parentStructure=c.getString(2);childStructure=c.getString(3);first=c.getString(4);last=c.getString(5);frozen=c.getString(6);purpose=c.getString(7);
        }
        if(endpoint.compareTo(last)<0)throw new IllegalStateException("R932_CHALLENGE_EARLY_LOOK");
        // Existing captured rows must be settled before judging capture completeness.
        try(Cursor pending=db.rawQuery("SELECT 1 FROM "+PREDICTION+" WHERE challenge_id=? AND hit=-1 LIMIT 1",new String[]{id})){if(pending.moveToFirst())return;}
        String streamKey=ResearchEvaluationRunR967.versioned(parent)?parent:child;String expected=first;boolean gap=false;
        int n=0,ph=0,ch=0,childMissRun=0,childMaxMiss=0;double pb=0,cb=0,ece=0;long maxCost=0;double[] ps=new double[10],hs=new double[10];StringBuilder chain=new StringBuilder(id);
        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.hit,c.hit,p.probability,c.probability,p.prediction_hash,c.prediction_hash,c.cost_ns FROM "+PREDICTION+" p JOIN "+PREDICTION+" c ON p.challenge_id=c.challenge_id AND p.target_issue=c.target_issue WHERE p.challenge_id=? AND p.candidate_key=? AND c.candidate_key=? AND p.hit>=0 AND c.hit>=0 ORDER BY p.target_issue LIMIT 121",new String[]{id,parent,child})){
            while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();if(ResearchEvaluationRunR967.versioned(streamKey)){String issue=c.getString(0);if(!ResearchEvaluationRunR967.eligibleEvaluation(streamKey,issue)||issue.compareTo(first)<0||issue.compareTo(last)>0||n>=PLANNED)throw new IllegalStateException("R969_CHALLENGE_SCHEDULE");if(!issue.equals(expected))gap=true;expected=ResearchEvaluationRunR967.nextEvaluation(streamKey,issue);}n++;int py=c.getInt(1),cy=c.getInt(2);double pp=c.getDouble(3),cp=c.getDouble(4);ph+=py;ch+=cy;childMissRun=cy==0?childMissRun+1:0;childMaxMiss=Math.max(childMaxMiss,childMissRun);pb+=(py-pp)*(py-pp);cb+=(cy-cp)*(cy-cp);int bin=Math.min(9,(int)(cp*10));ps[bin]+=cp;hs[bin]+=cy;maxCost=Math.max(maxCost,c.getLong(7));chain.append('|').append(c.getString(0)).append(':').append(c.getString(5)).append(':').append(c.getString(6));}
        }
        for(int i=0;i<10;i++)ece+=Math.abs(ps[i]-hs[i])/Math.max(1,n);
        double alpha;try(Cursor c=db.rawQuery("SELECT alpha FROM "+ResearchMultiplicityR932.LOOK+" WHERE window_id=?",new String[]{id})){if(!c.moveToFirst())throw new IllegalStateException("R932_CHALLENGE_PREREGISTRATION_MISSING");alpha=c.getDouble(0);}
        double delta=(ch-ph)/(double)Math.max(1,n),raw=ResearchMultiplicityR932.pairedP(n,ch-ph);
        String state=!frozen.equals(contract(db))?"PAUSED_DEPENDENCY":purpose.equals("FAMILY_REPLACEMENT")&&!incumbentStillCurrent(db,parent,parentStructure)?"PAUSED_DEPENDENCY":(gap||n!=PLANNED)?"PAUSED_CAPTURE":ch/(double)n>=CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE&&childMaxMiss<=8&&cb/n<=CertificateBundleServiceR929.MAX_ENTRY_BRIER&&cb<=pb&&ece<=.04&&delta>0&&raw<=alpha&&maxCost<=250000000L?"PASSED":"FAILED";
        try{
            JSONObject result=new JSONObject();result.put("challenge_id",id);result.put("purpose",purpose);result.put("parent",parent);result.put("child",child);result.put("first_issue",first);result.put("last_issue",last);result.put("planned",PLANNED);result.put("paired",n);result.put("parent_hits",ph);result.put("child_hits",ch);result.put("paired_delta",delta);result.put("parent_brier",n==0?JSONObject.NULL:pb/n);result.put("child_brier",n==0?JSONObject.NULL:cb/n);result.put("child_ece",ece);result.put("child_max_miss",childMaxMiss);result.put("minimum_hit_rate",CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE);result.put("raw_p",raw);result.put("allocated_alpha",alpha);result.put("conditional_null","E[child_hit-parent_hit|past]<=0; bounded deltas");result.put("max_cost_ns",maxCost);result.put("state",state);result.put("predictions_hash",V51ChecksumEngine.sha256(chain.toString()));result.put("may_authorize_real",false);
            String json=result.toString(),hash=V51ChecksumEngine.sha256(json);
            ResearchMultiplicityR932.close(db,id,endpoint,state,hash,n==PLANNED?raw:null);
            db.execSQL("UPDATE "+TABLE+" SET state=?,result_json=?,result_hash=?,updated_at=? WHERE challenge_id=? AND state='OPEN'",new Object[]{state,json,hash,System.currentTimeMillis(),id});
            if(purpose.equals("REPAIR"))db.execSQL("UPDATE "+ResearchLifecycleR932.CHILD+" SET outcome=? WHERE challenged_id=?",new Object[]{state,id});
            if(purpose.equals("FAMILY_REPLACEMENT")&&state.equals("PASSED"))replace(db,parent,child,parentStructure,childStructure,hash,TargetIssueResolver.next(endpoint).targetIssue);
        }catch(JSONException error){throw new IllegalStateException("R932_CHALLENGE_RESULT_JSON",error);}
    }
    static boolean incumbentStillCurrent(SQLiteDatabase db,String parent,String structure){
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchCohortR932.CHAMPION+" f JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" pc ON pc.certificate_hash=f.certificate_hash WHERE f.candidate_key=? AND f.structural_hash=? AND pc.state='ACTIVE' AND pc.all_gates_pass=1 AND pc.maximum_weight>0 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=pc.certificate_hash) LIMIT 1",new String[]{parent,structure})){return c.moveToFirst();}
    }
    static void replace(SQLiteDatabase db,String parent,String child,String parentStructure,String childStructure,String proof,String next){
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_PAIRED_CHALLENGE_RETIRED");
        String consumer=child.startsWith("algorithm:")?"INVENTED_ALGORITHM":child.startsWith("genesis:")?"GENESIS_ASSET":"FORMULA_GRAPH";
        try(Cursor c=db.rawQuery("SELECT family_id,structural_hash,certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND structural_hash=? AND state='ACTIVE' AND all_gates_pass=1 AND maximum_weight>0 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash="+ResearchPlatformStoreR929.TABLE_CERTIFICATE+".certificate_hash) ORDER BY issued_at DESC LIMIT 2",new String[]{consumer,child.substring(child.indexOf(':')+1),childStructure})){
            if(!c.moveToFirst())throw new IllegalStateException("R932_REPLACEMENT_CHILD_CERTIFICATE_MISSING");
            String family=c.getString(0),structure=c.getString(1),certificate=c.getString(2);
            if(c.moveToNext())throw new IllegalStateException("R932_REPLACEMENT_CHILD_CERTIFICATE_AMBIGUOUS");
            ContentValues values=new ContentValues();values.put("candidate_key",child);values.put("structural_hash",structure);values.put("certificate_hash",certificate);values.put("evidence_hash",proof);values.put("selected_issue",next);
            int rows=db.update(ResearchCohortR932.CHAMPION,values,"family=? AND candidate_key=? AND structural_hash=?",new String[]{family,parent,parentStructure});
            if(rows!=1)throw new IllegalStateException("R932_REPLACEMENT_INCUMBENT_CAS_FAILED");
            try(Cursor verify=db.rawQuery("SELECT candidate_key,structural_hash,certificate_hash,evidence_hash,selected_issue FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",new String[]{family})){
                if(!verify.moveToFirst()||!child.equals(verify.getString(0))||!childStructure.equals(verify.getString(1))||!certificate.equals(verify.getString(2))||!proof.equals(verify.getString(3))||!next.equals(verify.getString(4)))throw new IllegalStateException("R932_REPLACEMENT_CAS_VERIFY_FAILED");
            }
            ResearchPopulationR966.archiveReplaced(db,parent,child,proof,next);
        }
    }
    private static String contract(SQLiteDatabase db){return V51ChecksumEngine.sha256(FixedResearchWindowR932.contract(db)+'|'+ModelBundleRuntimeR932.current().hash+'|'+SelectionEvidenceContractR929.installedPolicy().hash);}

    /** Read-only CP4 diagnostic of the latest preregistered candidate/reference window. */
    static String describeLatestCp4(DbStore store){
        String window="";try(Cursor c=store.readable().rawQuery("SELECT b.window_id FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id WHERE w.state='PASS' ORDER BY b.last_issue DESC,b.window_id LIMIT 1",null)){if(c.moveToFirst())window=c.getString(0);}
        if(window.isEmpty())return "WAIT_FORMAL_FUTURE";
        ResearchGatePlanR967.Plan p=ResearchGatePlanR967.forWindow(store.readable(),window,true);
        if(p==null||!p.policy.isCp3)throw new IllegalStateException("CP4_DIAGNOSTIC_PLAN");
        int n=0,h=0,b=0;StringBuilder proof=new StringBuilder(window);
        try(Cursor c=store.readable().rawQuery("SELECT target_issue,candidate_hit,champion_hit,candidate_hash,champion_prediction_hash FROM "+FixedResearchWindowR932.PAIR+" WHERE window_id=? AND candidate_hit IN (0,1) AND champion_hit IN (0,1) ORDER BY target_issue",new String[]{window})){
            while(c.moveToNext()){n++;h+=c.getInt(1);b+=c.getInt(2);proof.append('|').append(c.getString(0)).append('|').append(c.getString(3)).append('|').append(c.getString(4)).append('|').append(c.getInt(1)).append('|').append(c.getInt(2));}
        }
        return n==0?"WAIT_FORMAL_FUTURE":"CP3_PAIRED_DESCRIPTIVE_FROM_FORMAL_FUTURE|n="+n+"|delta="+((h-b)/(double)n)+"|evidence="+V51ChecksumEngine.sha256(proof.toString());
    }

}
