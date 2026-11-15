package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.Arrays;
import java.util.Comparator;

/**
 * RETIRED compatibility facade: UnifiedResearchChoiceR932 + CertifiedPredictionR974 own selection.
 * Missing/ambiguous/expired/revoked/read-error evidence returns the default V51 ordering byte-for-
 * byte. This class has no Trade or REAL authorization API.
 */
public final class ResearchMainlineGovernorR903 {
    public static final boolean MAY_AUTHORIZE_REAL=false;
    private ResearchMainlineGovernorR903(){}

    public static Result govern(DbStore store,String baseIssue,String targetIssue,V51Models.PositionAction[] defaultRanked){
        return Result.fallback(defaultRanked,"LEGACY_MAINLINE_RETIRED_USE_UNIFIED_EXACT_OWNER");
    }

    private static Result fallback(DbStore store,V51Models.PositionAction[] actions,String status){try{store.setState("r906_research_governor_status",status);store.setState("r906_research_certificate_id","");store.setState("r906_research_applied_asset","");store.setState("r906_research_applied_asset_zh","");}catch(Throwable ignore){}return Result.fallback(actions,status);}

    private static Certified loadSingleCertificate(DbStore store,String target){
        UnifiedResearchChoiceR932.Evidence e=UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.FORMULA_GRAPH);
        if(e==null)return null;
        try(Cursor c=store.readable().rawQuery("SELECT display_name_zh,canonical_spec,artifact_hash,trained_until_issue,earliest_use_issue,artifact_status,runtime_executable FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE asset_id=? AND artifact_hash=? AND may_authorize_real=0",new String[]{e.id,e.structure})){
            if(!c.moveToFirst()||!TargetIssueResolver.isValid(c.getString(3))||TargetIssueResolver.compare(c.getString(3),target)>=0||c.getInt(6)!=1||!"PARITY_PASS".equals(c.getString(5)))return null;
            String earliest=safe(c.getString(4));if(earliest.length()>0&&(!TargetIssueResolver.isValid(earliest)||TargetIssueResolver.compare(earliest,target)>0))return null;
            return new Certified(e.certificate,e.evidence,e.id,e.count("FUTURE_N"),e.metric("WILSON_LOWER"),e.metric("BRIER"),e.metric("COVERAGE"),e.metric("PAIRED_DELTA"),c.getString(0),c.getString(1),c.getString(2),e.metric("HIT_RATE"));
        }catch(RuntimeException error){if(error instanceof ResearchWorkBudgetR927.Yield)throw error;return null;}
    }
    private static String lastStatus(DbStore store,String d){try{return store.getState("r906_research_governor_status",d);}catch(Throwable e){return d;}}
    private static double meta(double[] x,int i){return x==null||i<0||i>=x.length?0d:finite(x[i]);}private static double finite(double x){return Double.isNaN(x)||Double.isInfinite(x)?0d:x;}private static String safe(String x){return x==null?"":x.trim();}
    private static final class Certified{final String certificateId,evidenceHash,assetId,displayNameZh,canonicalSpec,artifactHash;final int futureN;final double adjustedLower,brier,coverage,pairedDelta,hitRate;Certified(String c,String ev,String a,int n,double l,double b,double cov,double delta,String zh,String spec,String hash,double hit){certificateId=c;evidenceHash=ev;assetId=a;futureN=n;adjustedLower=l;brier=b;coverage=cov;pairedDelta=delta;displayNameZh=zh;canonicalSpec=spec;artifactHash=hash;hitRate=hit;}}

    public static final class Result {
        public final String frozenRecipe;public final boolean applied;public final V51Models.PositionAction[] actions;public final V51Models.PositionAction rankOne;public final String status,candidateId;public final int futureN;public final double hitRate,wilsonLower,brier,influenceWeight;
        Result(boolean applied,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String status,String candidateId,int futureN,double hitRate,double lower,double brier){this(applied,actions,rankOne,status,candidateId,futureN,hitRate,lower,brier,0);}
        Result(boolean applied,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String status,String candidateId,int futureN,double hitRate,double lower,double brier,double weight){this(applied,actions,rankOne,status,candidateId,futureN,hitRate,lower,brier,weight,"");}
        Result(boolean applied,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String status,String candidateId,int futureN,double hitRate,double lower,double brier,double weight,String frozenRecipe){this.frozenRecipe=frozenRecipe;this.applied=applied;this.actions=actions;this.rankOne=rankOne;this.status=safe(status);this.candidateId=safe(candidateId);this.futureN=Math.max(0,futureN);this.hitRate=hitRate;wilsonLower=lower;this.brier=brier;this.influenceWeight=applied?weight:0;}

        static Result fallback(V51Models.PositionAction[] actions,String status){V51Models.PositionAction one=null;if(actions!=null)for(V51Models.PositionAction a:actions)if(a!=null&&a.rank==1){one=a;break;}return new Result(false,actions,one,status,"",0,0d,0d,-1d);}
    }
}
