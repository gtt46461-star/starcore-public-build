package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Evidence-preserving lifecycle governor; creation cadence is independent from active-pool fullness. */
public final class ResearchEvolutionGovernorR906 {
    private ResearchEvolutionGovernorR906(){}
    public static Summary govern(DbStore store){if(store==null)return new Summary(0,0,0,0,"STORE_NULL");final Summary[]box=new Summary[1];store.coordinatorR36().runWrite("R906_EVOLUTION_GOVERN",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){box[0]=governWithin(db);}});return box[0]==null?new Summary(0,0,0,0,"WRITE_FAILED"):box[0];}
    private static Summary governWithin(SQLiteDatabase db){ResearchCertificateR906.revokeDriftedWithin(db);Cursor c=null;int nursery=0,active=0,challenger=0,archived=0;try{c=db.rawQuery("SELECT a.asset_id,a.tier,COALESCE(e.frozen_n,0),COALESCE(e.settled_n,0),COALESCE(e.hit_rate,0),COALESCE(e.interval_lower,0),COALESCE(e.brier,1),COALESCE(e.ece,1),COALESCE(e.block_stability,0),COALESCE(e.drift_score,1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" a LEFT JOIN "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" e ON e.asset_id=a.asset_id WHERE a.tier<>'ARCHIVED' ORDER BY a.created_at ASC",null);while(c.moveToNext()){
            String id=c.getString(0);int frozen=c.getInt(2),n=c.getInt(3);double rate=c.getDouble(4),low=c.getDouble(5),brier=c.getDouble(6),ece=c.getDouble(7),stability=c.getDouble(8),drift=c.getDouble(9);ResearchAssetContractR906.Tier next;
            if(n>=24 && (rate<0.20d || (n>=60&&low<0.20d) || drift>0.35d)){next=ResearchAssetContractR906.Tier.ARCHIVED;archived++;}
            else if(n>=120&&low>=0.30d&&brier<=0.24d&&ece<=0.10d&&stability>=0.55d&&drift<=0.20d){next=ResearchAssetContractR906.Tier.CHALLENGER;challenger++;}
            else if(n>=60&&frozen>=n&&brier<=0.28d){next=ResearchAssetContractR906.Tier.ACTIVE;active++;}
            else {next=ResearchAssetContractR906.Tier.NURSERY;nursery++;}
            ResearchAssetRegistryR906.transition(db,id,next,"R906_EVIDENCE_GOVERN|n="+n+"|rate="+rate+"|low="+low+"|brier="+brier+"|ece="+ece+"|drift="+drift);
        }}finally{if(c!=null)c.close();}return new Summary(nursery,active,challenger,archived,"OK");}
    /** Archive is a tier transition only; predraw/outcome rows are retained by design. */
    public static void archiveEvidencePreserving(SQLiteDatabase db,String assetId,String reason){ResearchAssetRegistryR906.transition(db,assetId,ResearchAssetContractR906.Tier.ARCHIVED,"ARCHIVE_EVIDENCE_PRESERVED|"+reason);}
    public static final class Summary{public final int nursery,active,challenger,archived;public final String status;Summary(int n,int a,int c,int r,String s){nursery=n;active=a;challenger=c;archived=r;status=s;}public String canonical(){return "R906_EVOLUTION|nursery="+nursery+"|active="+active+"|challenger="+challenger+"|archived="+archived+"|status="+status;}}
}
