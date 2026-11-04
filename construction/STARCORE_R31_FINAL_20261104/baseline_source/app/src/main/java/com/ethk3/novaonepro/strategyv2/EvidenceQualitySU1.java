package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Read-only aggregation first; SQL independently derives counts and binds the published lower bound. */
public final class EvidenceQualitySU1 {
    private EvidenceQualitySU1() {}
    public static final class Look {
        final String cohort,claim,protocol,source,metrics,state,hash;final int ordinal,n,paired,parentMissing,childMissing,unpaired,parentHits,childHits,childWins,parentWins;
        final double alpha,mean,radius,lower,coverage;final long at;
        Look(String cohort,String claim,String protocol,int ordinal,int n,int paired,int pm,int cm,int ph,int ch,int cw,int pw,double familyAlpha,String source,String metrics,long at){
            this.cohort=cohort;this.claim=claim;this.protocol=protocol;this.ordinal=ordinal;this.n=n;this.paired=paired;parentMissing=pm;childMissing=cm;unpaired=n-paired;parentHits=ph;childHits=ch;childWins=cw;parentWins=pw;
            alpha=familyAlpha/(double)ordinal/(ordinal+1d)/2;mean=(cw-pw-unpaired)/(double)n;radius=Math.sqrt(2*Math.log(1/alpha)/n);lower=Math.max(-1,mean-radius);coverage=paired/(double)n;this.source=source;this.metrics=metrics;this.at=at;state=coverage<.95?"INSUFFICIENT":lower>0?"PASS":"FAIL";
            hash=HashingSU1.fields("SU1_FIXED_PAIRED_LOOK_V1",cohort,claim,protocol,String.valueOf(n),source,metrics,Double.toHexString(alpha),Double.toHexString(radius),state);
        }
    }
    public static Look prepare(SQLiteDatabase db,String cohort,long now,BudgetTokenSU1 budget){
        String claim,protocol;int ordinal,n;double family;
        try(Cursor c=db.rawQuery("SELECT claim_id,protocol_hash,global_claim_ordinal,planned_opportunities,family_alpha FROM su1_eval_cohort WHERE cohort_id=?",new String[]{cohort})){if(!c.moveToFirst())throw new IllegalArgumentException("SU1_COHORT_MISSING");claim=c.getString(0);protocol=c.getString(1);ordinal=c.getInt(2);n=c.getInt(3);family=c.getDouble(4);}
        int count=0,paired=0,pm=0,cm=0,ph=0,ch=0,cw=0,pw=0,probN=0,streak=0,maxStreak=0;double brier=0,baselineBrier=0;int[] bins=new int[10],blockN=new int[4],blockK=new int[4];double[] binP=new double[10],binH=new double[10];StringBuilder chain=new StringBuilder("SU1_PAIRED_CHAIN_V1|");
        String sql="SELECT o.opportunity_ordinal,o.opportunity_id,a.canonical_draw_hash,sa.hit,sb.hit,pb.selected_hit_p,sa.label_hash,sb.label_hash FROM su1_opportunity o JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id JOIN su1_opportunity_outcome a ON a.opportunity_id=o.opportunity_id LEFT JOIN su1_prediction pa ON pa.opportunity_id=o.opportunity_id AND pa.strategy_id=c.parent_strategy_id LEFT JOIN su1_settlement sa ON sa.prediction_id=pa.prediction_id AND sa.committed_at<=? LEFT JOIN su1_prediction pb ON pb.opportunity_id=o.opportunity_id AND pb.strategy_id=c.strategy_id LEFT JOIN su1_settlement sb ON sb.prediction_id=pb.prediction_id AND sb.committed_at<=? WHERE o.cohort_id=? AND o.opportunity_ordinal<=? AND a.completed_at<=? ORDER BY o.opportunity_ordinal";
        try(Cursor c=db.rawQuery(sql,new String[]{String.valueOf(now),String.valueOf(now),cohort,String.valueOf(n),String.valueOf(now)})){
            while(c.moveToNext()){
                budget.checkpointOrYield();if(c.getInt(0)!=++count)return null;boolean parent=!c.isNull(3),child=!c.isNull(4);if(!parent)pm++;if(!child)cm++;
                if(parent&&child){paired++;int p=c.getInt(3),h=c.getInt(4);ph+=p;ch+=h;if(p==0&&h==1)cw++;if(p==1&&h==0)pw++;}
                int block=Math.min(3,(count-1)*4/n);if(child){int h=c.getInt(4);blockN[block]++;blockK[block]+=h;streak=h==1?0:streak+1;if(!c.isNull(5)){double p=c.getDouble(5);if(!Double.isFinite(p)||p<0||p>1)throw new IllegalStateException("SU1_FROZEN_PROBABILITY_RANGE");brier+=(p-h)*(p-h);baselineBrier+=(.3-h)*(.3-h);int bin=Math.min(9,(int)(p*10));bins[bin]++;binP[bin]+=p;binH[bin]+=h;probN++;}}else streak++;
                maxStreak=Math.max(maxStreak,streak);chain.append(c.getString(1)).append('|').append(c.getString(2)).append('|').append(c.getString(6)).append('|').append(c.getString(7)).append(';');
            }
        }
        if(count!=n)return null;double ece=0;for(int i=0;i<10;i++)if(bins[i]>0)ece+=Math.abs(binP[i]-binH[i])/Math.max(1,probN);
        Map<String,Object> metrics=new TreeMap<>();metrics.put("scope","FIXED_PAIRED_CALENDAR_K3");metrics.put("probabilityN",probN);metrics.put("brier",probN==0?"UNKNOWN":Double.toHexString(brier/probN));metrics.put("baselineBrier",probN==0?"UNKNOWN":Double.toHexString(baselineBrier/probN));metrics.put("ece",probN==0?"UNKNOWN":Double.toHexString(ece));metrics.put("maxLossStreak",maxStreak);List<Object> blocks=new ArrayList<>();for(int i=0;i<4;i++)blocks.add(Arrays.asList(blockN[i],blockK[i]));metrics.put("blocks",blocks);
        return new Look(cohort,claim,protocol,ordinal,n,paired,pm,cm,ph,ch,cw,pw,family,HashingSU1.text(chain.toString()),StrictCanonicalJson.write(metrics),now);
    }
    public static void publish(SQLiteDatabase db,Look e){
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_evidence_look WHERE cohort_id=? AND look_ordinal=1",e.cohort))return;
        StrategyRepositorySU1.row(db,"su1_evidence_look","evidence_hash",e.hash,"cohort_id",e.cohort,"claim_id",e.claim,"global_claim_ordinal",e.ordinal,"look_ordinal",1,"protocol_hash",e.protocol,"planned_opportunities",e.n,"paired_n",e.paired,"parent_missing_n",e.parentMissing,"child_missing_n",e.childMissing,"unpaired_n",e.unpaired,"parent_hits",e.parentHits,"child_hits",e.childHits,"child_only_wins",e.childWins,"parent_only_wins",e.parentWins,"paired_delta",e.paired==0?null:(e.childHits-e.parentHits)/(double)e.paired,"conservative_mean",e.mean,"bound_radius",e.radius,"lower_bound",e.lower,"allocated_alpha",e.alpha,"cumulative_alpha_spent",e.alpha,"coverage",e.coverage,"result_state",e.state,"source_chain_hash",e.source,"metrics_json",e.metrics,"recorded_at",e.at);
    }
    /** The reader and issuer both recompute radius; a forged persisted radius never becomes authority. */
    public static boolean verifiedPass(SQLiteDatabase db,String evidence){
        try(Cursor c=db.rawQuery("SELECT e.planned_opportunities,e.allocated_alpha,e.bound_radius,e.conservative_mean,e.lower_bound,e.result_state,e.coverage,c.family_alpha,c.global_claim_ordinal,e.look_ordinal FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id WHERE e.evidence_hash=?",new String[]{evidence})){
            if(!c.moveToFirst())return false;double alpha=c.getDouble(7)/(c.getDouble(8)*(c.getDouble(8)+1)*c.getDouble(9)*(c.getDouble(9)+1)),radius=Math.sqrt(2*Math.log(1/alpha)/c.getInt(0));return alpha>0&&Math.abs(alpha-c.getDouble(1))<=1e-12&&Math.abs(radius-c.getDouble(2))<=1e-12&&Math.abs(Math.max(-1,c.getDouble(3)-radius)-c.getDouble(4))<=1e-12&&c.getDouble(4)>0&&"PASS".equals(c.getString(5))&&c.getDouble(6)>=.95;
        }
    }
    public static void maintain(DbStore store){
        List<String> ids=new ArrayList<>();try(Cursor c=store.readable().rawQuery("SELECT cohort_id FROM su1_eval_cohort c WHERE evidence_role='CONFIRM' AND EXISTS(SELECT 1 FROM su1_cohort_runtime r WHERE r.cohort_id=c.cohort_id AND r.state<>'STOPPED') AND NOT EXISTS(SELECT 1 FROM su1_evidence_look e WHERE e.cohort_id=c.cohort_id AND look_ordinal=1) ORDER BY created_at LIMIT 8",null)){while(c.moveToNext())ids.add(c.getString(0));}
        for(String id:ids){Look look=prepare(store.readable(),id,System.currentTimeMillis(),new BudgetTokenSU1(2000,1000000,SettlementExecutionGateR819::isActive));if(look!=null)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_PAIRED_LOOK",db->{publish(db,look);return 0;});}
    }
}
