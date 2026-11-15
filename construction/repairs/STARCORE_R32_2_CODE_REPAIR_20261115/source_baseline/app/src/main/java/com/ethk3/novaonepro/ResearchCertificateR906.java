package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Strict future-live paired evidence certificate. A certificate can re-rank SHADOW only; it never
 * authorizes REAL. Issuance is explicit and evidence-bound, never "pick today's best candidate".
 */
public final class ResearchCertificateR906 {
    public static final int MIN_FUTURE_N=240;
    public static final double MIN_COVERAGE=0.95d;
    public static final double MAX_BRIER=CertificateBundleServiceR929.MAX_ENTRY_BRIER;
    public static final double MAX_ECE=0.06d;
    public static final double MIN_BLOCK_STABILITY=0.70d;
    public static final double MAX_DRIFT=0.12d;
    public static final boolean MAY_AUTHORIZE_REAL=false;

    public final String assetId,status,evidenceHash;
    public final int futureN;
    public final double coverage,pairedDelta,adjustedLower,brier,ece,blockStability,driftScore,latencyP95Ms;
    public final long ramP95Bytes;

    public ResearchCertificateR906(String id,int n,double coverage,double delta,double lower,double brier,double ece,
            double stability,double drift,double latency,long ram,String evidenceHash){
        assetId=safe(id);futureN=n;this.coverage=coverage;pairedDelta=delta;adjustedLower=lower;this.brier=brier;this.ece=ece;
        blockStability=stability;driftScore=drift;latencyP95Ms=latency;ramP95Bytes=ram;this.evidenceHash=safe(evidenceHash);
        status=passes()?"ACTIVE_SHADOW":"NOT_PROVEN";
    }

    public boolean passes(){
        return futureN>=MIN_FUTURE_N&&coverage>=MIN_COVERAGE&&pairedDelta>=0.0d&&adjustedLower>=0.0d
                &&brier>=0.0d&&brier<=MAX_BRIER&&ece>=0.0d&&ece<=MAX_ECE
                &&blockStability>=MIN_BLOCK_STABILITY&&driftScore>=0.0d&&driftScore<=MAX_DRIFT
                &&latencyP95Ms>=0.0d&&latencyP95Ms<=AppConfig.R906_P0_CANONICAL_P95_TARGET_MS;
    }
    public boolean mayAuthorizeReal(){return false;}

    public static String persistShadowCertificate(DbStore store,String assetId,String evidenceStart,String evidenceEnd,
            int futureN,double coverage,double pairedDelta,double adjustedLower,double brier,double ece,
            double blockStability,double drift,double latencyP95,long ramP95,String validFrom,String validUntil,String evidenceHash){
        if(store==null)return "STORE_NULL";
        final String[] result=new String[]{"WRITE_FAILED"};
        final ResearchCertificateR906 cert=new ResearchCertificateR906(assetId,futureN,coverage,pairedDelta,adjustedLower,brier,ece,blockStability,drift,latencyP95,ramP95,evidenceHash);
        if(!cert.passes())return "NOT_PROVEN";
        if(!validIssue(evidenceStart)||!validIssue(evidenceEnd)||!validIssue(validFrom)||!validIssue(validUntil))return "ISSUE_RANGE_INVALID";
        if(TargetIssueResolver.compare(evidenceStart,evidenceEnd)>0||TargetIssueResolver.compare(evidenceEnd,validFrom)>=0||TargetIssueResolver.compare(validFrom,validUntil)>0)return "PIT_RANGE_INVALID";
        store.coordinatorR36().runWrite("R906_CERTIFICATE_PERSIST",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){result[0]=persistWithin(db,cert,evidenceStart,evidenceEnd,validFrom,validUntil);}
        });
        return result[0];
    }

    private static String persistWithin(SQLiteDatabase db,ResearchCertificateR906 cert,String evidenceStart,String evidenceEnd,String validFrom,String validUntil){
        Cursor c=null;String artifactHash="",trainedUntil="",earliestUse="",artifactStatus="";int executable=0;
        try{
            c=db.rawQuery("SELECT artifact_hash,trained_until_issue,earliest_use_issue,artifact_status,runtime_executable FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE asset_id=? LIMIT 1",new String[]{cert.assetId});
            if(!c.moveToFirst())return "ASSET_NOT_FOUND";
            artifactHash=safe(c.getString(0));trainedUntil=safe(c.getString(1));earliestUse=safe(c.getString(2));artifactStatus=safe(c.getString(3));executable=c.getInt(4);
        }finally{if(c!=null)c.close();}
        if(executable!=1)return "ASSET_NOT_EXECUTABLE";
        if(!"PARITY_PASS".equals(artifactStatus))return "ARTIFACT_PARITY_NOT_PASS";
        if(artifactHash.length()==0)return "ARTIFACT_HASH_MISSING";
        if(!validIssue(trainedUntil)||TargetIssueResolver.compare(trainedUntil,validFrom)>=0)return "TRAINED_UNTIL_PIT_VIOLATION";
        if(TargetIssueResolver.compare(trainedUntil,evidenceStart)>=0)return "EVIDENCE_NOT_FUTURE_OF_TRAINING";
        if(cert.evidenceHash.length()!=64)return "EVIDENCE_HASH_INVALID";
        if(validIssue(earliestUse)&&TargetIssueResolver.compare(earliestUse,validFrom)>0)return "EARLIEST_USE_AFTER_VALID_FROM";
        String id="R906-CERT-"+V51ChecksumEngine.sha256(cert.assetId+'|'+cert.evidenceHash+'|'+validFrom+'|'+validUntil).substring(0,28);
        String recordHash=V51ChecksumEngine.sha256("R906_CERT|"+id+'|'+cert.assetId+'|'+evidenceStart+'|'+evidenceEnd+'|'+cert.futureN+'|'+cert.coverage+'|'+cert.pairedDelta+'|'+cert.adjustedLower+'|'+cert.brier+'|'+cert.ece+'|'+cert.blockStability+'|'+cert.driftScore+'|'+cert.latencyP95Ms+'|'+cert.ramP95Bytes+'|'+validFrom+'|'+validUntil+'|'+cert.evidenceHash+'|'+artifactHash);
        Cursor prior=db.rawQuery("SELECT status FROM "+DbHelper.TABLE_R906_RESEARCH_CERTIFICATE+" WHERE asset_id=? AND evidence_hash=? AND status='REVOKED' LIMIT 1",new String[]{cert.assetId,cert.evidenceHash});
        try{if(prior.moveToFirst())return "REVOKED_EVIDENCE_CANNOT_REISSUE";}finally{prior.close();}
        // Cardinality invariant: at most one active certificate. Preserve old certificate history by revoking it.
        ContentValues revoke=new ContentValues();revoke.put("status","REVOKED");revoke.put("revocation_code","SUPERSEDED_BY_"+id);revoke.put("updated_at",System.currentTimeMillis());
        db.update(DbHelper.TABLE_R906_RESEARCH_CERTIFICATE,revoke,"status='ACTIVE_SHADOW'",null);
        long now=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("certificate_id",id);v.put("asset_id",cert.assetId);v.put("certificate_type","PAIRED_FUTURE_SHADOW");v.put("evidence_start_issue",evidenceStart);v.put("evidence_end_issue",evidenceEnd);v.put("future_n",cert.futureN);v.put("coverage",cert.coverage);v.put("paired_delta",cert.pairedDelta);v.put("adjusted_lower",cert.adjustedLower);v.put("brier",cert.brier);v.put("ece",cert.ece);v.put("block_stability",cert.blockStability);v.put("drift_score",cert.driftScore);v.put("latency_p95_ms",cert.latencyP95Ms);v.put("ram_p95_bytes",cert.ramP95Bytes);v.put("valid_from_issue",validFrom);v.put("valid_until_issue",validUntil);v.put("status","ACTIVE_SHADOW");v.put("revocation_code","");v.put("evidence_hash",cert.evidenceHash);v.put("record_hash",recordHash);v.put("created_at",now);v.put("updated_at",now);
        db.insertWithOnConflict(DbHelper.TABLE_R906_RESEARCH_CERTIFICATE,null,v,SQLiteDatabase.CONFLICT_REPLACE);
        PromotionWindowR925.register(db,"asset:"+cert.assetId,evidenceEnd,cert.futureN,cert.evidenceHash);
        return id;
    }


    /**
     * Fixed-horizon, pre-registered cohort issuer. Every challenger is evaluated on its first 240
     * future paired issues; ResearchPairedEvidenceR906 already applies a conservative <=96-family
     * multiplicity lower bound. No short-window/"today best" candidate is eligible.
     */
    public static String tryIssuePreRegisteredCohort(DbStore store){
        if(store==null)return "STORE_NULL";
        Cursor c=null;String bestId="";ResearchPairedEvidenceR906.Evidence best=null;double bestLower=-Double.MAX_VALUE;double latency=0d;long ram=0L;double coverage=0d;
        try{
            c=store.readable().rawQuery("SELECT a.asset_id,COALESCE(e.eligible_n,0),COALESCE(e.frozen_n,0),COALESCE(e.latency_p95_ms,0),COALESCE(e.ram_p95_bytes,0) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" a LEFT JOIN "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" e ON e.asset_id=a.asset_id WHERE a.tier='CHALLENGER' AND a.runtime_executable=1 AND a.artifact_status='PARITY_PASS' ORDER BY a.created_at ASC LIMIT 96",null);
            while(c.moveToNext()){String id=safe(c.getString(0));int eligible=c.getInt(1),frozen=c.getInt(2);double cov=eligible<=0?0d:(double)frozen/eligible;ResearchPairedEvidenceR906.Evidence ev=ResearchPairedEvidenceR906.compute(store.readable(),id);if(!ev.available()||ev.n!=MIN_FUTURE_N||cov<MIN_COVERAGE)continue;if(ev.adjustedLower<0d||ev.brier>MAX_BRIER||ev.ece>MAX_ECE||ev.blockStability<MIN_BLOCK_STABILITY||ev.drift>MAX_DRIFT)continue;if(ev.adjustedLower>bestLower){bestLower=ev.adjustedLower;bestId=id;best=ev;coverage=cov;latency=c.getDouble(3);ram=c.getLong(4);}}
        }finally{if(c!=null)c.close();}
        if(best==null)return "NO_CERTIFIABLE_CHALLENGER";
        TargetIssueResolver.Resolution from=TargetIssueResolver.next(best.lastIssue);if(!from.valid)return "VALID_FROM_RESOLVE_FAILED";String until=advance(from.targetIssue,1440);if(!validIssue(until))return "VALID_UNTIL_RESOLVE_FAILED";
        return persistShadowCertificate(store,bestId,best.firstIssue,best.lastIssue,best.n,coverage,best.pairedDelta,best.adjustedLower,best.brier,best.ece,best.blockStability,best.drift,latency,ram,from.targetIssue,until,best.evidenceHash);
    }

    /** Revokes active evidence when live coverage metrics drift beyond the predeclared certificate envelope. */
    public static int revokeDriftedWithin(SQLiteDatabase db){
        if(db==null)return 0;ContentValues v=new ContentValues();v.put("status","REVOKED");v.put("revocation_code","LIVE_DRIFT_OR_CALIBRATION_REGRESSION");v.put("updated_at",System.currentTimeMillis());
        return db.update(DbHelper.TABLE_R906_RESEARCH_CERTIFICATE,v,"status='ACTIVE_SHADOW' AND asset_id IN (SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" WHERE COALESCE(drift_score,1)>CAST(? AS REAL) OR COALESCE(ece,1)>CAST(? AS REAL) OR COALESCE(brier,1)>CAST(? AS REAL))",new String[]{String.valueOf(MAX_DRIFT),String.valueOf(MAX_ECE+0.04d),String.valueOf(MAX_BRIER+0.05d)});
    }

    private static String advance(String issue,int count){String x=issue;for(int i=1;i<count;i++){TargetIssueResolver.Resolution r=TargetIssueResolver.next(x);if(!r.valid)return "";x=r.targetIssue;}return x;}

    public static boolean revoke(DbStore store,String certificateId,String reason){if(store==null||safe(certificateId).length()==0)return false;final boolean[]ok={false};store.coordinatorR36().runWrite("R906_CERTIFICATE_REVOKE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){ContentValues v=new ContentValues();v.put("status","REVOKED");v.put("revocation_code",safe(reason));v.put("updated_at",System.currentTimeMillis());ok[0]=db.update(DbHelper.TABLE_R906_RESEARCH_CERTIFICATE,v,"certificate_id=? AND status='ACTIVE_SHADOW'",new String[]{safe(certificateId)})==1;}});return ok[0];}
    private static boolean validIssue(String x){return TargetIssueResolver.isValid(safe(x));}
    private static String safe(String x){return x==null?"":x.trim();}
}
