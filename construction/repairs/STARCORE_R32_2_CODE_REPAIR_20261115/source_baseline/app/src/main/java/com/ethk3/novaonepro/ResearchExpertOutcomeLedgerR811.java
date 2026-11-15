package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Draw-after sidecar losses for frozen expert evidence; target_issue is the independent group. */
public final class ResearchExpertOutcomeLedgerR811 {
    private ResearchExpertOutcomeLedgerR811(){}

    public static List<Outcome> settleFromSnapshot(ResearchExpertPredrawSnapshotR811.Snapshot snapshot,Models.Draw draw){
        if(snapshot==null||draw==null||!draw.isValid()||!snapshot.targetIssue.equals(draw.issue)||snapshot.frozenAt>=draw.openTime)throw new IllegalArgumentException("R811_EXPERT_SETTLE_PIT");
        ArrayList<Outcome>out=new ArrayList<Outcome>(90);for(int e=0;e<snapshot.experts.length;e++){ResearchExpertPredrawSnapshotR811.Expert expert=snapshot.experts[e];for(int p=0;p<10;p++){int actual=draw.getNumberAtPosition(p+1);Loss loss=computeNumberLoss(expert.rawScore[p],actual);double k3p=-1.0d;int k3hit=-1;if(p+1==snapshot.championPosition){K3Loss k=computeK3Loss(loss.probability,snapshot.n1,snapshot.n2,snapshot.n3,actual);k3p=k.probability;k3hit=k.hit;}String hash=V51ChecksumEngine.sha256(snapshot.targetIssue+'|'+expert.expertId+'|'+(p+1)+'|'+actual+'|'+loss.pActual+'|'+loss.logLoss+'|'+loss.brier+'|'+loss.actualRank+'|'+k3p+'|'+k3hit);out.add(new Outcome(snapshot.targetIssue,expert.expertId,p+1,"",actual,loss.pActual,loss.logLoss,loss.brier,loss.actualRank,k3p,k3hit,hash));}}
        return Collections.unmodifiableList(out);
    }
    public static List<Outcome> settleFromPredrawSnapshot(ResearchExpertPredrawSnapshotR811.Snapshot s,Models.Draw d){return settleFromSnapshot(s,d);}
    public static boolean settleFromSnapshot(final DbStore store,final ResearchExpertPredrawSnapshotR811.Snapshot snapshot,final Models.Draw draw){if(store==null||RealtimePressureGateR811.hasActiveCriticalWork())return false;final List<Outcome>rows=settleFromSnapshot(snapshot,draw);if(rows.size()!=90||!store.coordinatorR36().ensureR811ResearchSchemaOnce())return false;Boolean out=store.coordinatorR36().runWrite("R811_EXPERT_OUTCOME",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){boolean owns=!db.inTransaction();if(owns)db.beginTransaction();try{for(Outcome x:rows){ContentValues cv=new ContentValues();cv.put("target_issue",x.targetIssue);cv.put("expert_id",x.expertId);cv.put("position",x.position);cv.put("regime_id",x.regimeId);cv.put("actual_number",x.actualNumber);cv.put("p_actual",x.pActual);cv.put("log_loss",x.logLoss);cv.put("multiclass_brier",x.multiclassBrier);cv.put("actual_rank",x.actualRank);cv.put("k3_probability",x.k3Probability);cv.put("k3_hit",x.k3Hit);cv.put("record_hash",x.recordHash);cv.put("created_at",TimeSourceR811.wallNowMs());long n=db.insertWithOnConflict("v57_research_expert_outcome_r811",null,cv,SQLiteDatabase.CONFLICT_IGNORE);if(n<0&&!matches(db,x))throw new IllegalStateException("R811_EXPERT_OUTCOME_CONFLICT");}if(owns)db.setTransactionSuccessful();return Boolean.TRUE;}finally{if(owns&&db.inTransaction())db.endTransaction();}}},Boolean.FALSE);return out.booleanValue();}
    public static Loss computeNumberLoss(double[]rawScore,int actualNumber){if(rawScore==null||rawScore.length!=10||!AppConfig.isValidNumber(actualNumber))throw new IllegalArgumentException("R811_NUMBER_LOSS_INPUT");double[]p=softmax(rawScore);double pa=Math.max(1e-15d,p[actualNumber-1]),brier=0.0d;int rank=1;for(int i=0;i<10;i++){double y=i==actualNumber-1?1.0d:0.0d;brier+=(p[i]-y)*(p[i]-y);if(p[i]>p[actualNumber-1]||(p[i]==p[actualNumber-1]&&i<actualNumber-1))rank++;}return new Loss(p,pa,-Math.log(pa),brier,rank);}
    public static K3Loss computeK3Loss(double[]probability,int n1,int n2,int n3,int actual){if(probability==null||probability.length!=10||!DecisionGateEngine.b12K3(n1,n2,n3))throw new IllegalArgumentException("R811_K3_LOSS_INPUT");double p=probability[n1-1]+probability[n2-1]+probability[n3-1];int hit=actual==n1||actual==n2||actual==n3?1:0;double clamped=Math.max(1e-15d,Math.min(1.0d-1e-15d,p));return new K3Loss(p,hit,-(hit*Math.log(clamped)+(1-hit)*Math.log(1.0d-clamped)));}
    public static Map<String,Aggregate> aggregateByPositionRegime(List<Outcome>rows){LinkedHashMap<String,Mutable>m=new LinkedHashMap<String,Mutable>();if(rows!=null)for(Outcome x:rows){String key=x.expertId+'|'+x.position+'|'+x.regimeId;Mutable a=m.get(key);if(a==null){a=new Mutable();m.put(key,a);}a.n++;a.log+=x.logLoss;a.brier+=x.multiclassBrier;}LinkedHashMap<String,Aggregate>out=new LinkedHashMap<String,Aggregate>();for(Map.Entry<String,Mutable>e:m.entrySet())out.put(e.getKey(),new Aggregate(e.getValue().n,e.getValue().log/e.getValue().n,e.getValue().brier/e.getValue().n));return out;}
    public static Map<String,Double> snapshotRegret(List<Outcome>rows){LinkedHashMap<String,Double>out=new LinkedHashMap<String,Double>();for(Map.Entry<String,Aggregate>e:aggregateByPositionRegime(rows).entrySet())out.put(e.getKey(),e.getValue().meanLogLoss);return out;}
    /** Deterministic Research-only G0..G5 gate challenger for one position/regime. */
    public static GateChallengerWeights challengerWeights(
            String family,
            double[] baseline,
            double[] evidenceQuality,
            double[] regret,
            double[] diversityPenalty,
            String trainedUntilIssue,
            String targetIssue) {
        if (family == null
                || !family.matches("G[0-5]")
                || baseline == null
                || evidenceQuality == null
                || regret == null
                || diversityPenalty == null
                || baseline.length != 9
                || evidenceQuality.length != 9
                || regret.length != 9
                || diversityPenalty.length != 9
                || !TargetIssueResolver.isValid(trainedUntilIssue)
                || !TargetIssueResolver.isValid(targetIssue)
                || TargetIssueResolver.compare(trainedUntilIssue, targetIssue) >= 0)
            throw new IllegalArgumentException("R811_GATE_CHALLENGER_INPUT");
        double[] raw = new double[9];
        for (int i = 0; i < raw.length; i++) {
            if (!isFinite(baseline[i])
                    || !isFinite(evidenceQuality[i])
                    || !isFinite(regret[i])
                    || !isFinite(diversityPenalty[i]))
                throw new IllegalArgumentException("R811_GATE_CHALLENGER_NONFINITE");
            if ("G0".equals(family)) raw[i] = Math.max(0.0d, baseline[i]);
            else if ("G1".equals(family)) raw[i] = 1.0d;
            else if ("G2".equals(family)) raw[i] = Math.max(0.0d, evidenceQuality[i]);
            else if ("G3".equals(family) || "G4".equals(family))
                raw[i] = Math.exp(Math.max(-60.0d, Math.min(60.0d, -regret[i])));
            else
                raw[i] =
                        Math.exp(Math.max(-60.0d, Math.min(60.0d, -regret[i])))
                                / (1.0d + Math.max(0.0d, diversityPenalty[i]));
        }
        double sum = 0.0d;
        for (int i = 0; i < raw.length; i++) sum += raw[i];
        if (!(sum > 0.0d)) {
            for (int i = 0; i < raw.length; i++) raw[i] = 1.0d / raw.length;
        } else {
            for (int i = 0; i < raw.length; i++) raw[i] /= sum;
        }
        return new GateChallengerWeights(
                family,
                trainedUntilIssue,
                targetIssue,
                raw,
                V51ChecksumEngine.hashDoubles(new double[][] {raw}));
    }
    private static boolean matches(SQLiteDatabase db,Outcome x){Cursor c=null;try{c=db.rawQuery("SELECT record_hash FROM v57_research_expert_outcome_r811 WHERE target_issue=? AND expert_id=? AND position=?",new String[]{x.targetIssue,x.expertId,String.valueOf(x.position)});return c.moveToFirst()&&x.recordHash.equals(c.getString(0));}finally{if(c!=null)c.close();}}
    private static double[]softmax(double[]x){double max=-Double.MAX_VALUE;for(double v:x)max=Math.max(max,finite(v));double[]p=new double[x.length];double sum=0;for(int i=0;i<x.length;i++){p[i]=Math.exp(Math.max(-60,Math.min(60,finite(x[i])-max)));sum+=p[i];}for(int i=0;i<p.length;i++)p[i]/=sum;return p;}private static double finite(double x){return Double.isNaN(x)||Double.isInfinite(x)?0:x;}
    private static boolean isFinite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
    public static final class Outcome{public final String targetIssue,expertId,regimeId,recordHash;public final int position,actualNumber,actualRank,k3Hit;public final double pActual,logLoss,multiclassBrier,k3Probability;Outcome(String t,String e,int p,String r,int a,double pa,double l,double b,int rank,double kp,int kh,String h){targetIssue=t;expertId=e;position=p;regimeId=r;actualNumber=a;pActual=pa;logLoss=l;multiclassBrier=b;actualRank=rank;k3Probability=kp;k3Hit=kh;recordHash=h;}}
    public static final class Loss{public final double[]probability;public final double pActual,logLoss,brier;public final int actualRank;Loss(double[]p,double pa,double l,double b,int r){probability=p;pActual=pa;logLoss=l;brier=b;actualRank=r;}}
    public static final class K3Loss{public final double probability,logLoss;public final int hit;K3Loss(double p,int h,double l){probability=p;hit=h;logLoss=l;}}
    public static final class Aggregate{public final int n;public final double meanLogLoss,meanBrier;Aggregate(int n,double l,double b){this.n=n;meanLogLoss=l;meanBrier=b;}}
    public static final class GateChallengerWeights{public final String family,trainedUntilIssue,targetIssue,hash;public final double[]weights;GateChallengerWeights(String family,String trained,String target,double[]weights,String hash){this.family=family;trainedUntilIssue=trained;targetIssue=target;this.weights=weights.clone();this.hash=hash;}}
    private static final class Mutable{int n;double log,brier;}
}
