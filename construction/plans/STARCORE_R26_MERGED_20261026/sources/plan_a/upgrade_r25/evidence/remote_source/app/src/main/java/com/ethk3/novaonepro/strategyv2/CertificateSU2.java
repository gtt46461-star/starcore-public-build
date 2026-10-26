package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/**
 * Selective certificates for SU2 league members (P27/P28/P29).
 * Issued only from settled LIVE records (never from replay), at the alpha this look
 * may spend (shared legacy/SU1/SU2 lifetime ledger; summable family and look allocations), with post-
 * selection calibration of the shown probability (Platt on earlier ACT rows, ECE<=0.04).
 * Valid 24 hours; re-issued daily while the evidence holds. Replaces the SU1
 * requirement "coverage>=0.999", which made every 15-30% strategy uncertifiable.
 */
public final class CertificateSU2 {
    private CertificateSU2() {}
    public static final String PREFIX = "SU2-F-";

    public static final class Authority {
        public final String certificate, genome, program; public final double a, b; public final long issued, expires;
        Authority(String c, String g, String p, double a, double b, long i, long e) { certificate = c; genome = g; program = p; this.a = a; this.b = b; issued = i; expires = e; }
        public String candidate() { return PREFIX + genome; }
    }

    public static Authority read(SQLiteDatabase db, String genome, long now) {
        try (Cursor c = db.rawQuery("SELECT c.certificate_id,c.program_hash,c.calibration_a,c.calibration_b,c.issued_at,c.expires_at,g.canonical_text FROM su2_certificate c JOIN su2_model_genome g ON g.genome_key=c.genome_key "
            + "WHERE c.genome_key=? AND c.issued_at<=? AND c.expires_at>? AND g.state='LEAGUE' ORDER BY c.issued_at DESC LIMIT 1", new String[]{genome, String.valueOf(now), String.valueOf(now)})) {
            if (!c.moveToFirst()) return null;
            if (!HashingSU1.text(c.getString(6)).equals(c.getString(1))) return null;        // certificate bound to the exact program
            return new Authority(c.getString(0), genome, c.getString(1), c.getDouble(2), c.getDouble(3), c.getLong(4), c.getLong(5));
        }
    }

    /** Daily maintenance: evaluate the current mainline member and issue a certificate if the live evidence passes. */
    public static String maintain(DbStore store, long seq) {
        SQLiteDatabase r = store.readable();
        String member = ModelStoreSU2.currentChoice(r, seq);
        boolean incumbent = member.startsWith("XI_INCUMBENT:");                  // the old method is certified by the same rule
        if (!member.startsWith("SU2:") && !incumbent) return "SU2_CERT_NO_SU2_MAINLINE";
        String genome = member.substring(member.indexOf(':') + 1);
        if (read(r, genome, System.currentTimeMillis()) != null) return "SU2_CERT_VALID";
        long from = seq - MainlineBridgeSU2.WINDOW;
        boolean[][] me = ModelStoreSU2.record(r, genome, from, seq);
        boolean[][] inc = null;
        if (!incumbent) for (String[] m : ModelStoreSU2.league(r)) if (m[1].equals("XI_INCUMBENT")) inc = ModelStoreSU2.record(r, m[3], from, seq);
        int acted=0;for(boolean a:me[0])if(a)acted++;
        if(acted<MainlineBridgeSU2.MIN_ACTS)return "SU2_CERT_WAIT_ACTS";
        boolean[] missing=new boolean[me[0].length];java.util.Arrays.fill(missing,true);
        try(Cursor c=r.rawQuery("SELECT sequence_ordinal FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",new String[]{genome,String.valueOf(from),String.valueOf(seq)})){while(c.moveToNext())missing[(int)(c.getLong(0)-from)]=false;}
        int look; try (Cursor c = r.rawQuery("SELECT looks FROM su2_alpha_look WHERE genome_key=?", new String[]{genome})) { look = (c.moveToFirst() ? c.getInt(0) : 0) + 1; }
        final int reservedLook=look;
        final double familyAlpha=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CERT_RESERVE",db->{
            double budget=AlphaLedgerSU1.family(db,HashingSU1.fields("SU2_LIFETIME_FAMILY",genome));
            db.execSQL("INSERT OR IGNORE INTO su2_alpha_look(genome_key,looks) VALUES(?,0)",new Object[]{genome});
            db.execSQL("UPDATE su2_alpha_look SET looks=? WHERE genome_key=? AND looks=?",new Object[]{reservedLook,genome,reservedLook-1});
            try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("SU2_LOOK_RACE");}
            AlphaLedgerSU1.reserveClaim(db,HashingSU1.fields("SU2_SELECTIVE_LOOK",genome,String.valueOf(reservedLook)),budget,reservedLook);
            return budget;
        });
        SelectiveEvidenceSU2.Verdict v = SelectiveEvidenceSU2.evaluate(me[0], me[1], missing, inc == null ? null : inc[0], inc == null ? null : inc[1], 0, MainlineBridgeSU2.WINDOW,
            look, familyAlpha, MainlineBridgeSU2.MIN_ACTS, MainlineBridgeSU2.BLOCK_FLOOR, MainlineBridgeSU2.MIN_COVERAGE);
        // calibration from earlier ACT rows (phat recorded at freeze time)
        double[] ph = new double[MainlineBridgeSU2.WINDOW]; boolean[] hh = new boolean[MainlineBridgeSU2.WINDOW]; int n = 0;
        try (Cursor c = r.rawQuery("SELECT phat,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND act=1 AND hit IS NOT NULL AND phat IS NOT NULL ORDER BY sequence_ordinal",
            new String[]{genome, String.valueOf(from), String.valueOf(seq)})) { while (c.moveToNext() && n < ph.length) { ph[n] = c.getDouble(0); hh[n] = c.getInt(1) == 1; n++; } }
        int fitN=Math.max(0,n-Math.max(120,n/4)),validationN=n-fitN;
        SelectiveCalibrationSU2.Platt cal = SelectiveCalibrationSU2.fit(ph, hh, fitN, 10.0);
        double[] adj = new double[validationN]; boolean[] held=new boolean[validationN];
        for(int i=0;i<validationN;i++){adj[i]=cal.apply(ph[fitN+i]);held[i]=hh[fitN+i];}
        double ece=fitN<120||validationN<120?Double.POSITIVE_INFINITY:SelectiveCalibrationSU2.ece(adj,held,validationN);
        final int lk = look; final boolean pass = v.pass && ece <= 0.04;
        String program; try (Cursor c = r.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE genome_key=?", new String[]{genome})) { if (!c.moveToFirst()) return "SU2_CERT_GENOME_MISSING"; program = HashingSU1.text(c.getString(0)); }
        final String prog = program; final long now = System.currentTimeMillis();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CERT_LOOK", db -> {
            if (pass) {
                String id = HashingSU1.fields("SU2_CERTIFICATE_V1", genome, prog, String.valueOf(from), String.valueOf(seq), String.valueOf(lk), String.valueOf(now));
                db.execSQL("INSERT INTO su2_certificate(certificate_id,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,paired_upper,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{id, genome, prog, from, seq, v.acts, v.hits, v.lower, v.coverage, v.worstBlock, Double.isNaN(v.pairedUpper) ? null : v.pairedUpper, lk, SelectiveEvidenceSU2.alphaForLook(familyAlpha, lk), cal.a, cal.b, ece, now, now + 86400000L});
            }
            return 0;
        });
        return (pass ? "SU2_CERT_ISSUED" : "SU2_CERT_REFUSED:" + (v.pass ? "ECE_GT_004" : v.reason)) + "|acts=" + v.acts + "|rate=" + String.format(java.util.Locale.ROOT, "%.4f", v.rate) + "|lower=" + String.format(java.util.Locale.ROOT, "%.4f", v.lower) + "|look=" + lk;
    }
}
