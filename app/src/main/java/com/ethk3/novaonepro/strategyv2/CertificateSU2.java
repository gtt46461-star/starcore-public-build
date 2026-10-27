package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/**
 * Selective certificates for SU2 league members (P27/P28/P29).
 * Issued only from settled LIVE records (never from replay), at the alpha this look
 * may spend (shared legacy/SU1/SU2 lifetime ledger; summable family and look allocations), with post-
 * selection calibration of the shown probability (Platt on earlier ACT rows, ECE<=0.04).
 * Valid 24 hours; fixed overlapping 4000-period windows start every 720 periods.
 * Family alpha is registered at birth, and every calendar look has its fixed ordinal. Replaces the SU1
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
        if (!RuntimeSchemaR26.ready(db)) return null;
        try (Cursor c = db.rawQuery("SELECT c.certificate_id,c.program_hash,c.calibration_a,c.calibration_b,c.issued_at,c.expires_at,g.canonical_text FROM su2_certificate c JOIN su2_model_genome g ON g.genome_key=c.genome_key "
            + "WHERE NOT EXISTS(SELECT 1 FROM r26_certificate_revocation revoked WHERE revoked.certificate_id=c.certificate_id) AND EXISTS(SELECT 1 FROM r26_certificate_protocol p WHERE p.certificate_id=c.certificate_id AND p.protocol='R26_PREREGISTERED_ROLLING_LCB_V2') AND c.genome_key=? AND c.issued_at<=? AND c.expires_at>? AND g.state='LEAGUE' ORDER BY c.issued_at DESC LIMIT 1", new String[]{genome, String.valueOf(now), String.valueOf(now)})) {
            if (!c.moveToFirst()) return null;
            String canonical=c.getString(6);
            if(!canonical.contains("|ID=R26|"))return null;
            if(canonical.startsWith("SU2|")&&!ModelGenomeSU2.parse(canonical).key().equals(genome))return null;
            if (!HashingSU1.text(c.getString(6)).equals(c.getString(1))) return null;        // certificate bound to the exact program
            return new Authority(c.getString(0), genome, c.getString(1), c.getDouble(2), c.getDouble(3), c.getLong(4), c.getLong(5));
        }
    }

    /** Compatibility entry: all new live policies also receive independent round-robin checks. */
    public static String maintain(DbStore store,long seq){return maintainPolicy(store,ModelStoreSU2.currentChoice(store.readable(),seq),seq);}

    /** Called in the same short transaction that publishes a policy, before its future outcomes. */
    public static void preregister(SQLiteDatabase db,String genome,long birth){
        if(!db.inTransaction())throw new IllegalStateException("R26_COHORT_WRITER");
        try(Cursor c=db.rawQuery("SELECT 1 FROM r26_cohort WHERE genome_key=?",new String[]{genome})){if(c.moveToFirst())return;}
        String benchmark="";for(String[] m:ModelStoreSU2.league(db))if(m[1].equals("XI_INCUMBENT")){benchmark=m[3];break;}
        double alpha=AlphaLedgerSU1.family(db,HashingSU1.fields("SU2_LIFETIME_FAMILY",genome));
        db.execSQL("INSERT INTO r26_cohort(genome_key,from_sequence,to_sequence,incumbent,protocol,family_alpha) VALUES(?,?,?,?,?,?)",new Object[]{genome,birth,Math.addExact(birth,QualificationWindowR26.LENGTH),benchmark,QualificationWindowR26.PROTOCOL,alpha});
    }
    public static long maintainOne(DbStore store,long seq){
        String policy=null;
        try(Cursor c=store.readable().rawQuery("SELECT l.policy_hash FROM r26_live_model l LEFT JOIN r26_cohort q ON q.genome_key=l.policy_hash WHERE l.status='ACTIVE' AND coalesce(q.checked_sequence,-1)<? ORDER BY coalesce(q.checked_sequence,-1),l.policy_hash LIMIT 1",new String[]{String.valueOf(seq)})){if(c.moveToFirst())policy=c.getString(0);}
        if(policy==null)return 0;final String g=policy,result=maintainPolicy(store,"SU2:"+policy,seq);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_QUALIFICATION_RECEIPT",db->{db.execSQL("UPDATE r26_cohort SET checked_sequence=?,state=? WHERE genome_key=?",new Object[]{seq,result,g});return 0;});return 1;
    }
    private static String maintainPolicy(DbStore store,String member,long seq){
        SQLiteDatabase r=store.readable();
        boolean incumbent = member.startsWith("XI_INCUMBENT:");                  // the old method is certified by the same rule
        if (!member.startsWith("SU2:") && !incumbent) return "SU2_CERT_NO_SU2_MAINLINE";
        String genome = member.substring(member.indexOf(':') + 1);
        if (read(r, genome, System.currentTimeMillis()) != null) return "SU2_CERT_VALID";
        if (!RuntimeSchemaR26.ready(r)) return "R26_CERT_SCHEMA_PENDING";
        try(Cursor c=r.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE genome_key=?",new String[]{genome})){
            if(!c.moveToFirst())return "R26_CERT_POLICY_MISSING";
            String canonical=c.getString(0);
            if(!canonical.contains("|ID=R26|"))return "R26_CERT_LEGACY_POLICY_REBIND_REQUIRED";
            if(canonical.startsWith("SU2|")&&!ModelGenomeSU2.parse(canonical).key().equals(genome))return "R26_CERT_NUMERICAL_ABI_CHANGED";
        }

        long birth=-1;String benchmark="",protocol="";double registeredAlpha=0;
        try(Cursor c=r.rawQuery("SELECT from_sequence,incumbent,protocol,family_alpha FROM r26_cohort WHERE genome_key=?",new String[]{genome})){
            if(c.moveToFirst()){birth=c.getLong(0);benchmark=c.getString(1);protocol=c.getString(2);registeredAlpha=c.getDouble(3);}
        }
        if(birth<0){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_COHORT_PREREGISTER",db->{preregister(db,genome,seq);return 0;});return "R26_CERT_COHORT_PREREGISTERED";}
        if(!QualificationWindowR26.PROTOCOL.equals(protocol)||!(registeredAlpha>0))return "R26_CERT_PREREGISTRATION_REBIND_REQUIRED";
        QualificationWindowR26.Window window=QualificationWindowR26.completed(birth,seq);
        if(window==null)return "R26_CERT_ACCUMULATING:"+Math.max(0,seq-birth)+"/"+QualificationWindowR26.LENGTH;
        final long from=window.from,endpoint=window.to;
        final int lk=window.look;final double familyAlpha=registeredAlpha;
        boolean[][] me = ModelStoreSU2.record(r, genome, from, endpoint);
        boolean[][] inc = null;
        if (!incumbent) {
            if(benchmark.isEmpty()) return "R26_CERT_BENCHMARK_MISSING";
            inc=ModelStoreSU2.record(r,benchmark,from,endpoint);
            try(Cursor c=r.rawQuery("SELECT count(*) FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",new String[]{benchmark,String.valueOf(from),String.valueOf(endpoint)})){if(!c.moveToFirst()||c.getInt(0)!=MainlineBridgeSU2.WINDOW)return "R26_CERT_BENCHMARK_CALENDAR_INCOMPLETE";}
        }
        int acted=0;for(boolean a:me[0])if(a)acted++;
        if(acted<MainlineBridgeSU2.MIN_ACTS)return "SU2_CERT_WAIT_ACTS";
        boolean[] missing=new boolean[me[0].length];java.util.Arrays.fill(missing,true);
        try(Cursor c=r.rawQuery("SELECT sequence_ordinal FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",new String[]{genome,String.valueOf(from),String.valueOf(endpoint)})){while(c.moveToNext())missing[(int)(c.getLong(0)-from)]=false;}
        // calibration from earlier ACT rows (phat recorded at freeze time)
        double[] ph = new double[MainlineBridgeSU2.WINDOW]; boolean[] hh = new boolean[MainlineBridgeSU2.WINDOW]; int n = 0;
        try (Cursor c = r.rawQuery("SELECT phat,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND act=1 AND hit IS NOT NULL AND phat IS NOT NULL ORDER BY sequence_ordinal",
            new String[]{genome, String.valueOf(from), String.valueOf(endpoint)})) { while (c.moveToNext() && n < ph.length) { ph[n] = c.getDouble(0); hh[n] = c.getInt(1) == 1; n++; } }
        int fitN=Math.max(0,n-Math.max(120,n/4)),validationN=n-fitN;
        SelectiveCalibrationSU2.Platt cal = SelectiveCalibrationSU2.fit(ph, hh, fitN, 10.0);
        double[] adj = new double[validationN]; boolean[] held=new boolean[validationN];
        for(int i=0;i<validationN;i++){adj[i]=cal.apply(ph[fitN+i]);held[i]=hh[fitN+i];}
        double ece=fitN<120||validationN<120?Double.POSITIVE_INFINITY:SelectiveCalibrationSU2.ece(adj,held,validationN);
        if(!(ece<=0.04))return "R26_CERT_CALIBRATION_PREFLIGHT_FAILED";
        for(boolean absent:missing)if(absent)return "R26_CERT_CALENDAR_INCOMPLETE";
        StringBuilder evidence=new StringBuilder(QualificationWindowR26.PROTOCOL+"|").append(genome).append('|').append(from).append('|').append(endpoint);
        for(int i=0;i<me[0].length;i++)evidence.append(me[0][i]?'A':'S').append(me[1][i]?'1':'0').append(inc!=null&&inc[0][i]?'A':'S').append(inc!=null&&inc[1][i]?'1':'0');
        for(int i=0;i<n;i++)evidence.append('|').append(Double.toHexString(ph[i])).append(hh[i]?'1':'0');
        final String digest=PolicyIdentityR26.sha256(evidence.toString()),cohort=from+":"+endpoint;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_IDEMPOTENT_LOOK",db->{
            try(Cursor c=db.rawQuery("SELECT evidence_digest,look,family_alpha FROM r26_look WHERE genome_key=? AND cohort=?",new String[]{genome,cohort})){
                if(c.moveToFirst()){if(!digest.equals(c.getString(0))||c.getInt(1)!=lk||Double.compare(c.getDouble(2),familyAlpha)!=0)throw new IllegalStateException("R26_FIXED_WINDOW_EVIDENCE_CONFLICT");return 0;}
            }
            AlphaLedgerSU1.reserveClaim(db,HashingSU1.fields("R26_PREDECLARED_LOOK",genome,cohort),familyAlpha,lk);
            db.execSQL("INSERT INTO r26_look VALUES(?,?,?,?,?)",new Object[]{genome,cohort,digest,lk,familyAlpha});return 0;
        });
        SelectiveEvidenceSU2.Verdict v=SelectiveEvidenceSU2.evaluate(me[0],me[1],missing,inc==null?null:inc[0],inc==null?null:inc[1],0,MainlineBridgeSU2.WINDOW,lk,familyAlpha,MainlineBridgeSU2.MIN_ACTS,MainlineBridgeSU2.BLOCK_FLOOR,MainlineBridgeSU2.MIN_COVERAGE);
        final boolean pass=v.pass;
        String program; try (Cursor c = r.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE genome_key=?", new String[]{genome})) { if (!c.moveToFirst()) return "SU2_CERT_GENOME_MISSING"; program = HashingSU1.text(c.getString(0)); }
        final String prog = program; final long now = System.currentTimeMillis();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CERT_LOOK", db -> {
            if (pass) {
                String id = HashingSU1.fields("SU2_CERTIFICATE_V1", genome, prog, String.valueOf(from), String.valueOf(endpoint), String.valueOf(lk), String.valueOf(now));
                db.execSQL("INSERT INTO su2_certificate(certificate_id,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,paired_upper,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{id, genome, prog, from, endpoint, v.acts, v.hits, v.lower, v.coverage, v.worstBlock, Double.isNaN(v.pairedUpper) ? null : v.pairedUpper, lk, SelectiveEvidenceSU2.alphaForLook(familyAlpha, lk), cal.a, cal.b, ece, now, now + 86400000L});
                db.execSQL("INSERT INTO r26_certificate_protocol VALUES(?,?,?,?)",new Object[]{id,QualificationWindowR26.PROTOCOL,Double.isNaN(v.pairedLower)?null:v.pairedLower,digest});
            }
            return 0;
        });
        return (pass ? "SU2_CERT_ISSUED" : "SU2_CERT_REFUSED:" + (v.pass ? "ECE_GT_004" : v.reason)) + "|acts=" + v.acts + "|rate=" + String.format(java.util.Locale.ROOT, "%.4f", v.rate) + "|lower=" + String.format(java.util.Locale.ROOT, "%.4f", v.lower) + "|look=" + lk;
    }
}
