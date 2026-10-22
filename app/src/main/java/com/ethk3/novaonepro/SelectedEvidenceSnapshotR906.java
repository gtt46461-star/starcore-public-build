package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Typed selected-action evidence frozen in the canonical transaction; missing values stay missing, never become fake zeroes. */
public final class SelectedEvidenceSnapshotR906 {
    public final String targetIssue,actionHash,recordHash,metricSchemaHash;
    public final int actionOrdinal,rankNo;
    public final long frozenAt;
    public final MetricValueR906 rawActionP,rankScore,thirdMargin,entropyRisk,stability,windowConsensus,assignmentSupport;

    private SelectedEvidenceSnapshotR906(String target,String actionHash,int ordinal,int rank,long at,
            MetricValueR906 raw,MetricValueR906 score,MetricValueR906 third,MetricValueR906 entropy,
            MetricValueR906 stability,MetricValueR906 window,MetricValueR906 assignment){
        targetIssue=safe(target);this.actionHash=safe(actionHash);actionOrdinal=ordinal;rankNo=rank;frozenAt=Math.max(0L,at);
        rawActionP=raw;rankScore=score;thirdMargin=third;entropyRisk=entropy;this.stability=stability;windowConsensus=window;assignmentSupport=assignment;
        metricSchemaHash=V51ChecksumEngine.sha256("R906_SELECTED_METRIC_SCHEMA_V1|raw_action_p|rank_no|rank_score|third_margin|entropy_risk|stability|window_consensus|assignment_support");
        recordHash=V51ChecksumEngine.sha256(canonical());
    }

    public static SelectedEvidenceSnapshotR906 freeze(V51RuntimeResult r,DeferredEvidenceSnapshotR36.Header h,long frozenAt){
        if(r==null||r.selected==null||r.selected.action==null||h==null)throw new IllegalArgumentException("R906_SELECTED_FREEZE_INPUT");
        V51Models.PositionAction a=r.selected.action; String target=h.targetIssue; String src="V51_SELECTED_ACTION";
        MetricValueR906 raw=prob(a.rawActionP,src,target,frozenAt,h.actionSpaceHash,"RAW_ACTION_P_INVALID");
        MetricValueR906 score=number(a.rankScore,MetricValueR906.Unit.SCORE,src,target,frozenAt,h.actionSpaceHash,"RANK_SCORE_INVALID");
        MetricValueR906 third=number(a.thirdMargin,MetricValueR906.Unit.SCORE,src,target,frozenAt,h.actionSpaceHash,"THIRD_MARGIN_INVALID");
        MetricValueR906 entropy=prob(a.entropy,"V51_ACTION_ENTROPY",target,frozenAt,h.actionSpaceHash,"ENTROPY_INVALID");
        MetricValueR906 stable=prob(a.stability,"V51_ACTION_STABILITY",target,frozenAt,h.actionSpaceHash,"STABILITY_INVALID");
        MetricValueR906 assignment=metaProbability(a.metaFeatures,15,"META_ASSIGNMENT_SUPPORT",target,frozenAt,h.actionSpaceHash);
        MetricValueR906 window=metaProbability(a.metaFeatures,20,"META_WINDOW_CONSENSUS",target,frozenAt,h.actionSpaceHash);
        return new SelectedEvidenceSnapshotR906(target,a.checksum,K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3),a.rank,frozenAt,
                raw,score,third,entropy,stable,window,assignment);
    }

    /** R907 post-canonical factory. Uses immutable runtime/selected hashes; never reads draw data. */
    public static SelectedEvidenceSnapshotR906 freezePostCommit(V51RuntimeResult r,long frozenAt){
        if(r==null||r.context==null||r.selected==null||r.selected.action==null)
            throw new IllegalArgumentException("R907_SELECTED_FREEZE_INPUT");
        V51Models.PositionAction a=r.selected.action;String target=safe(r.context.targetIssue);
        String provenance=safe(r.selected.contractHash);if(provenance.length()!=64)provenance=safe(r.checksum);
        String src="V51_SELECTED_ACTION_R907_POST_COMMIT";
        MetricValueR906 raw=prob(a.rawActionP,src,target,frozenAt,provenance,"RAW_ACTION_P_INVALID");
        MetricValueR906 score=number(a.rankScore,MetricValueR906.Unit.SCORE,src,target,frozenAt,provenance,"RANK_SCORE_INVALID");
        MetricValueR906 third=number(a.thirdMargin,MetricValueR906.Unit.SCORE,src,target,frozenAt,provenance,"THIRD_MARGIN_INVALID");
        MetricValueR906 entropy=prob(a.entropy,"V51_ACTION_ENTROPY",target,frozenAt,provenance,"ENTROPY_INVALID");
        MetricValueR906 stable=prob(a.stability,"V51_ACTION_STABILITY",target,frozenAt,provenance,"STABILITY_INVALID");
        MetricValueR906 assignment=metaProbability(a.metaFeatures,15,"META_ASSIGNMENT_SUPPORT",target,frozenAt,provenance);
        MetricValueR906 window=metaProbability(a.metaFeatures,20,"META_WINDOW_CONSENSUS",target,frozenAt,provenance);
        return new SelectedEvidenceSnapshotR906(target,a.checksum,K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3),a.rank,frozenAt,raw,score,third,entropy,stable,window,assignment);
    }

    public boolean insertBestEffort(final DbStore store){
        if(store==null)return false;final boolean[] ok=new boolean[]{false};
        try{
            store.coordinatorR36().runWrite("R907_SELECTED_EVIDENCE_ASYNC",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase db){insertWithinTransaction(db);ok[0]=true;}
            });
        }catch(Throwable ignore){return false;}
        return ok[0];
    }

    public void insertWithinTransaction(SQLiteDatabase db){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R906_SELECTED_EVIDENCE_TX_REQUIRED");
        Cursor c=null;try{
            c=db.rawQuery("SELECT record_hash FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" WHERE target_issue=? LIMIT 1",new String[]{targetIssue});
            if(c.moveToFirst()){
                // R907: first pre-draw evidence wins. A same-target retry may have a different
                // createdAt/recordHash even when the canonical selected action is unchanged; that
                // must never invalidate prediction or force a retry storm.
                return;
            }
        }finally{if(c!=null)c.close();}
        ContentValues cv=new ContentValues();cv.put("target_issue",targetIssue);cv.put("action_ordinal",actionOrdinal);cv.put("action_hash",actionHash);
        putMetric(cv,"raw_action_p",rawActionP,true);cv.put("raw_action_p_source",rawActionP.source);cv.put("raw_action_p_reason",rawActionP.reasonCode);
        cv.put("rank_no",rankNo>0?Integer.valueOf(rankNo):null);
        // R912 canonical schema uses rank_status (not the legacy rank_score_status). The old
        // dynamic prefix writer made the whole auxiliary INSERT fail and left every metric blank.
        if(rankScore.status==MetricValueR906.Status.AVAILABLE)cv.put("rank_score_value",rankScore.value);else cv.putNull("rank_score_value");
        cv.put("rank_status",rankScore.status.name());
        putMetric(cv,"third_margin",thirdMargin,false);
        putMetric(cv,"entropy_risk",entropyRisk,false);cv.put("entropy_risk_source",entropyRisk.source);
        putMetric(cv,"stability",stability,false);cv.put("stability_source",stability.source);cv.put("stability_reason",stability.reasonCode);
        putMetric(cv,"window_consensus",windowConsensus,false);putMetric(cv,"assignment_support",assignmentSupport,false);
        cv.put("metric_schema_hash",metricSchemaHash);cv.put("frozen_at",frozenAt);cv.put("record_hash",recordHash);
        db.insertOrThrow(DbHelper.TABLE_R906_SELECTED_EVIDENCE,null,cv);
    }

    private static void putMetric(ContentValues cv,String prefix,MetricValueR906 m,boolean raw){
        String value=prefix+(raw?"_value":"_value"); String status=prefix+(raw?"_status":"_status");
        if(m.status==MetricValueR906.Status.AVAILABLE)cv.put(value,m.value);else cv.putNull(value);
        cv.put(status,m.status.name());
    }
    private static MetricValueR906 metaProbability(double[] x,int idx,String source,String target,long at,String hash){
        if(x==null||idx<0||idx>=x.length||!finite(x[idx]))return MetricValueR906.unavailable(MetricValueR906.Unit.PROBABILITY,source,target,"FEATURE_NOT_FROZEN");
        return prob(x[idx],source,target,at,hash,"FEATURE_OUT_OF_RANGE");
    }
    private static MetricValueR906 prob(double v,String source,String target,long at,String hash,String bad){
        if(!finite(v)||v<0d||v>1d)return MetricValueR906.unavailable(MetricValueR906.Unit.PROBABILITY,source,target,bad);
        return MetricValueR906.available(MetricValueR906.Unit.PROBABILITY,v,source,target,at,hash);
    }
    private static MetricValueR906 number(double v,MetricValueR906.Unit unit,String source,String target,long at,String hash,String bad){
        if(!finite(v))return MetricValueR906.unavailable(unit,source,target,bad);return MetricValueR906.available(unit,v,source,target,at,hash);
    }
    private String canonical(){return "R906_SELECTED|"+targetIssue+'|'+actionOrdinal+'|'+actionHash+'|'+rankNo+'|'+metric(rawActionP)+'|'+metric(rankScore)+'|'+metric(thirdMargin)+'|'+metric(entropyRisk)+'|'+metric(stability)+'|'+metric(windowConsensus)+'|'+metric(assignmentSupport)+'|'+frozenAt;}
    private static String metric(MetricValueR906 m){return m.status.name()+"|"+m.unit.name()+"|"+(m.status==MetricValueR906.Status.AVAILABLE?Double.toString(m.value):"NA")+'|'+m.source+'|'+m.reasonCode;}
    private static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
    private static String safe(String x){return x==null?"":x.trim();}
}
