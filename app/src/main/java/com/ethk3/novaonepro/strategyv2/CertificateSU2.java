package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/**
 * Selective certificates for SU2 league members (P27/P28/P29).
 * Issued only from settled LIVE records (never from replay), at the alpha this look
 * may spend (shared legacy/SU1/SU2 lifetime ledger; summable family and look allocations), with post-
 * selection calibration of the shown probability (Platt on earlier ACT rows, ECE<=0.04).
 * Valid 24 hours; fixed overlapping 69120-period windows start every 720 periods; bounded 720-period aggregates preserve evidence beyond raw retention.
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
            + "WHERE NOT EXISTS(SELECT 1 FROM r26_certificate_revocation revoked WHERE revoked.certificate_id=c.certificate_id) AND EXISTS(SELECT 1 FROM r26_certificate_protocol p WHERE p.certificate_id=c.certificate_id AND p.protocol='R26_PREREGISTERED_AGGREGATE_LCB_V3') AND c.genome_key=? AND c.issued_at<=? AND c.expires_at>? AND g.state='LEAGUE' ORDER BY c.issued_at DESC LIMIT 1", new String[]{genome, String.valueOf(now), String.valueOf(now)})) {
            if (!c.moveToFirst()) return null;
            String canonical=c.getString(6);
            if(!CurrentPolicyR26.matches(canonical,genome))return null;
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
        // Birth, benchmark availability and alpha reservation commit in the same writer.
        if(benchmark.isEmpty()){IncumbentLoopSU2.seedWithin(db,birth);benchmark=IncumbentLoopSU2.KEY;}
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
            if(!CurrentPolicyR26.matches(canonical,genome))return "R26_CERT_NUMERICAL_ABI_OR_CLOSED_IDENTITY_CHANGED";
        }

        long birth=-1;String benchmark="",protocol="";double registeredAlpha=0;
        try(Cursor c=r.rawQuery("SELECT from_sequence,incumbent,protocol,family_alpha FROM r26_cohort WHERE genome_key=?",new String[]{genome})){
            if(c.moveToFirst()){birth=c.getLong(0);benchmark=c.getString(1);protocol=c.getString(2);registeredAlpha=c.getDouble(3);}
        }
        if(birth<0){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_COHORT_PREREGISTER",db->{preregister(db,genome,seq);return 0;});return "R26_CERT_COHORT_PREREGISTERED";}
        if(!QualificationWindowR26.PROTOCOL.equals(protocol)||!(registeredAlpha>0))return "R26_CERT_PREREGISTRATION_REBIND_REQUIRED";
        if(!incumbent&&benchmark.isEmpty())return "R26_CERT_BENCHMARK_MISSING_FROM_BIRTH_REQUIRES_NEW_IDENTITY";
        QualificationWindowR26.Window window=QualificationWindowR26.completed(birth,seq);
        if(window==null)return "R26_CERT_ACCUMULATING:"+Math.max(0,seq-birth)+"/"+QualificationWindowR26.LENGTH;
        final long from=window.from,endpoint=window.to;
        final int lk=window.look;final double familyAlpha=registeredAlpha;
        final EvidenceAccumulatorR26.Window aggregate;
        try{aggregate=EvidenceAccumulatorR26.read(r,genome,window);}catch(java.io.IOException corrupt){android.util.Log.e("STARCORE_R26","qualification aggregate corrupt",corrupt);return "R26_CERT_AGGREGATE_CORRUPT:"+corrupt.getMessage();}
        if(aggregate==null)return "R26_CERT_AGGREGATES_NOT_COMPLETE";
        if(!incumbent&&benchmark.isEmpty())return "R26_CERT_BENCHMARK_MISSING";
        SelectiveEvidenceSU2.Verdict v=EvidenceBlockR26.evaluate(aggregate.quarters,lk,familyAlpha,!incumbent);
        SelectiveCalibrationSU2.Platt cal=EvidenceBlockR26.fit(aggregate.fit);
        double measuredEce=aggregate.validation.ece(cal);
        final double ece=aggregate.fit.acts>=120&&aggregate.validation.acts>=120?measuredEce:Double.POSITIVE_INFINITY;
        final String resultReason=!Double.isFinite(ece)?"CALIBRATION_INSUFFICIENT":ece>.04?"HELD_LATER_ECE_GT_004":v.reason;
        final String digest=aggregate.digest,cohort=from+":"+endpoint;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_QUALIFICATION_METRICS",db->{db.execSQL("INSERT OR REPLACE INTO r26_qualification_result VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{genome,from,endpoint,lk,digest,v.acts,v.hits,v.rate,v.lower,v.coverage,v.worstBlock,Double.isNaN(v.pairedLower)?null:v.pairedLower,aggregate.fit.acts,aggregate.validation.acts,Double.isFinite(ece)?ece:null,resultReason,System.currentTimeMillis()});return 0;});
        if(!Double.isFinite(ece)||ece>.04||v.missing>0||v.reason.equals("PENDING_LABELS")||v.reason.equals("BENCHMARK_CALENDAR_INCOMPLETE"))return "R26_CERT_PREFLIGHT:"+resultReason;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_IDEMPOTENT_LOOK",db->{
            try(Cursor c=db.rawQuery("SELECT evidence_digest,look,family_alpha FROM r26_look WHERE genome_key=? AND cohort=?",new String[]{genome,cohort})){
                if(c.moveToFirst()){if(!digest.equals(c.getString(0))||c.getInt(1)!=lk||Double.compare(c.getDouble(2),familyAlpha)!=0)throw new IllegalStateException("R26_FIXED_WINDOW_EVIDENCE_CONFLICT");return 0;}
            }
            AlphaLedgerSU1.reserveClaim(db,HashingSU1.fields("R26_PREDECLARED_LOOK",genome,cohort),familyAlpha,lk);
            db.execSQL("INSERT INTO r26_look VALUES(?,?,?,?,?)",new Object[]{genome,cohort,digest,lk,familyAlpha});return 0;
        });
        final boolean pass=v.pass;
        String program; try (Cursor c = r.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE genome_key=?", new String[]{genome})) { if (!c.moveToFirst()) return "SU2_CERT_GENOME_MISSING"; program = HashingSU1.text(c.getString(0)); }
        final String prog = program; final long now = System.currentTimeMillis();
        if(pass&&!store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_CERT_LOOK",db->issueWithin(db,genome,prog,window,digest,now)))return "R26_CERT_COMMIT_REVALIDATION_REFUSED";
        return (pass ? "SU2_CERT_ISSUED" : "SU2_CERT_REFUSED:" + (v.pass ? "ECE_GT_004" : v.reason)) + "|acts=" + v.acts + "|rate=" + String.format(java.util.Locale.ROOT, "%.4f", v.rate) + "|lower=" + String.format(java.util.Locale.ROOT, "%.4f", v.lower) + "|look=" + lk;
    }
    /** Single production issuance path, also exercised on an isolated SYNTHETIC Android database. */
    static boolean issueWithin(SQLiteDatabase db,String genome,String program,QualificationWindowR26.Window window,String digest,long now)throws java.io.IOException {
        if(!db.inTransaction())throw new IllegalStateException("R26_CERTIFICATE_WRITER");
        double family;long birth;String benchmark,canonical;
        try(Cursor c=db.rawQuery("SELECT q.family_alpha,q.from_sequence,q.incumbent,g.canonical_text FROM r26_cohort q JOIN su2_model_genome g ON g.genome_key=q.genome_key WHERE q.genome_key=? AND q.protocol=? AND g.state='LEAGUE'",new String[]{genome,QualificationWindowR26.PROTOCOL})){if(!c.moveToFirst())return false;family=c.getDouble(0);birth=c.getLong(1);benchmark=c.getString(2);canonical=c.getString(3);}
        QualificationWindowR26.Window planned=QualificationWindowR26.completed(birth,window.to);
        if(planned==null||planned.from!=window.from||planned.look!=window.look||!CurrentPolicyR26.matches(canonical,genome)||!HashingSU1.text(canonical).equals(program))return false;
        try(Cursor c=db.rawQuery("SELECT evidence_digest,look,family_alpha FROM r26_look WHERE genome_key=? AND cohort=?",new String[]{genome,window.from+":"+window.to})){if(!c.moveToFirst()||!digest.equals(c.getString(0))||c.getInt(1)!=window.look||Double.compare(family,c.getDouble(2))!=0)return false;}
        EvidenceAccumulatorR26.Window aggregate=EvidenceAccumulatorR26.read(db,genome,window);if(aggregate==null||!digest.equals(aggregate.digest)||benchmark.isEmpty())return false;
        SelectiveEvidenceSU2.Verdict v=EvidenceBlockR26.evaluate(aggregate.quarters,window.look,family,true);SelectiveCalibrationSU2.Platt cal=EvidenceBlockR26.fit(aggregate.fit);double ece=aggregate.validation.ece(cal);
        if(!v.pass||aggregate.fit.acts<120||aggregate.validation.acts<120||!Double.isFinite(ece)||ece>.04)return false;
        String id=HashingSU1.fields("R26_CERTIFICATE_V3",genome,program,String.valueOf(window.from),String.valueOf(window.to),String.valueOf(window.look),digest);
        try(Cursor c=db.rawQuery("SELECT 1 FROM su2_certificate WHERE certificate_id=?",new String[]{id})){if(c.moveToFirst())return true;}
        db.execSQL("INSERT INTO su2_certificate(certificate_id,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,paired_upper,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,genome,program,window.from,window.to,v.acts,v.hits,v.lower,v.coverage,v.worstBlock,v.pairedUpper,window.look,SelectiveEvidenceSU2.alphaForLook(family,window.look),cal.a,cal.b,ece,now,now+86400000L});
        db.execSQL("INSERT INTO r26_certificate_protocol VALUES(?,?,?,?)",new Object[]{id,QualificationWindowR26.PROTOCOL,v.pairedLower,digest});return true;
    }

}
