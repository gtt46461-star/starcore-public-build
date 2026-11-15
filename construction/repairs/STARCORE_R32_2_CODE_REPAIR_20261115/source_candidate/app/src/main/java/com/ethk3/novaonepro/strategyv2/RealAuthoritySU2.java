package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;

/**
 * Replaces the compile-time REAL locks for certified selective lanes (P04).
 * Root cause in both repositories: AppConfig.R35_REAL_AUTO_UNLOCK_ENABLED=false and
 * V51NumberModelArtifact/ExpertGate/ActionRanker/SelectedCalibration/PolicySurface
 * PROVEN=false are compile-time constants, so DecisionGateEngine.b12Reject returns
 * MODEL_NOT_PROVEN for every REAL candidate no matter how good it is.
 * Now a REAL action needs, at run time: the user's explicit authorisation switch
 * (default OFF), a valid SU2 selective certificate for the chosen league member, the
 * member's frozen ACT decision for this exact target, and an unopened target.
 */
public final class RealAuthoritySU2 {
    private RealAuthoritySU2() {}
    public static final String USER_SWITCH = "su2_real_authorized_by_user";

    public static boolean allows(DbStore store, String candidate, String certificate, String target, long openTime) {
        if (store == null || candidate == null || !candidate.startsWith(CertificateSU2.PREFIX)) return false;
        if (!"1".equals(store.getState(USER_SWITCH, "0"))) return false;
        long now = System.currentTimeMillis(); if (now >= openTime) return false;
        String genome = candidate.substring(CertificateSU2.PREFIX.length());
        CertificateSU2.Authority a = CertificateSU2.read(store.readable(), genome, now);
        if (a == null || !a.certificate.equals(certificate)) return false;
        long seq = TargetIssueResolver.sequenceOrdinalSU1(target);
        String chosen = ModelStoreSU2.currentChoice(store.readable(), seq);
        if (!("SU2:" + genome).equals(chosen) && !("XI_INCUMBENT:" + genome).equals(chosen)) return false;
        try (Cursor c = store.readable().rawQuery("SELECT act FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NULL", new String[]{genome, String.valueOf(seq)})) {
            if(!c.moveToFirst() || c.getInt(0)!=1)return false;
        }
        return completeFreezeAndNoRealR30(store,candidate,certificate,target,seq,openTime,now);
    }

    /** R30-B07-03: a surviving ACT row alone is not a complete forecast, and canonical REAL is
     * authoritative even when the retired legacy trade projection contains no row. Read-only, stricter checks. */
    static boolean completeFreezeAndNoRealR30(DbStore store,String candidate,String certificate,String target,long seq,long openTime,long now) {
        try {
            if(store.getTradeByTargetIssue(target)!=null)return false;
            try(Cursor c=store.readable().rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? AND decision_action IN ('BET','REAL','REAL_TRADE') LIMIT 1",new String[]{target})){
                if(c.moveToFirst())return false;
            }
            MainlineBridgeSU2.ProjectionR30 p=MainlineBridgeSU2.projectionR30(store.readable(),seq,now,candidate);
            Object deadline=p.sources.get("deadlineAt");
            return candidate.equals(p.candidate)&&certificate.equals(p.certificate)&&Boolean.TRUE.equals(p.certificateActive)
                    &&Boolean.TRUE.equals(p.freezeComplete)&&Boolean.TRUE.equals(p.modelAvailable)
                    &&Boolean.TRUE.equals(p.selectorConfirmed)&&Boolean.TRUE.equals(p.actRuleFired)
                    &&deadline instanceof Number&&((Number)deadline).longValue()==openTime;
        }catch(RuntimeException missingOrConflictingSource){return false;}
    }
}
