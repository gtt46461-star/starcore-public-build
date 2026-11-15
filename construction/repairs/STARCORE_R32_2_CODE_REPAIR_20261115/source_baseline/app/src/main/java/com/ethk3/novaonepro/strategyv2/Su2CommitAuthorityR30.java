package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;

/** Read-only additional binding; all original SU2 authority and calibration owners remain unchanged. */
public final class Su2CommitAuthorityR30 {
    private Su2CommitAuthorityR30() {}
    /** Read-only final validation for the existing canonical REAL writer. */
    public static boolean verifyFrozen(DbStore store,android.database.sqlite.SQLiteDatabase db,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        return proof!=null&&action!=null&&matchesFrozenR30(store,db,proof.candidate,proof.certificate,
                proof.structure,proof.modelHash,proof.selectionHash,proof.calibrationHash,proof.target,
                proof.deadlineAt,proof.selectedHitP,action.position,action.n1,action.n2,action.n3);
    }
    static boolean matchesFrozenR30(DbStore store,android.database.sqlite.SQLiteDatabase db,String candidate,String certificate,
            String program,String model,String selector,String calibration,String target,long deadline,double probability,
            int position,int n1,int n2,int n3){
        try{
            if(!RealAuthoritySU2.allows(store,candidate,certificate,target,deadline))return false;
            String genome=candidate.substring(CertificateSU2.PREFIX.length());
            CertificateSU2.Authority a=CertificateSU2.read(db,genome,System.currentTimeMillis());
            if(a==null||!a.certificate.equals(certificate)||!a.program.equals(program)||!a.program.equals(model)||!MainlineAdapterSU2.CONTRACT.equals(selector))return false;
            String currentCalibration=V51ChecksumEngine.sha256("SU2_PLATT|"+Double.toHexString(a.a)+"|"+Double.toHexString(a.b));
            if(!currentCalibration.equals(calibration)||position<1||position>10||n1<1||n1>10||n2<1||n2>10||n3<1||n3>10||n1==n2||n1==n3||n2==n3)return false;
            int mask=(1<<(n1-1))|(1<<(n2-1))|(1<<(n3-1));
            try(Cursor c=db.rawQuery("SELECT position,number_mask,act,phat FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NULL",new String[]{genome,String.valueOf(TargetIssueResolver.sequenceOrdinalSU1(target))})){
                if(!c.moveToFirst()||c.getInt(0)!=position||c.getInt(1)!=mask||c.getInt(2)!=1)return false;
                double phat=c.isNull(3)?Double.NaN:c.getDouble(3);
                double currentProbability=Double.isNaN(phat)?.30:new SelectiveCalibrationSU2.Platt(a.a,a.b).apply(EvidenceBlockR26.quantized(phat));
                return Double.compare(probability,currentProbability)==0;
            }
        }catch(RuntimeException unavailable){
            if(unavailable instanceof ResearchWorkBudgetR927.Yield)throw unavailable;
            return false;
        }
    }

}
