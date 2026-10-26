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
            return c.moveToFirst() && c.getInt(0) == 1;
        }
    }
}
