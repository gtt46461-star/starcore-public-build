package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/** Bounded future challengers retain their own birth/alpha; a research score never replaces a parent. */
public final class ShadowAdmissionR26 {
    private ShadowAdmissionR26(){}
    public static final int BASE_LIMIT=32,CHALLENGER_LIMIT=2,LIMIT=BASE_LIMIT+CHALLENGER_LIMIT,PER_FAMILY=2;
    static long count(SQLiteDatabase db,String family){try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'"+(family==null?"":" AND family_id=?"),family==null?null:new String[]{family})){c.moveToFirst();return c.getLong(0);}}
    static long challengers(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_live_model l JOIN r26_shadow_admission a ON a.policy_hash=l.policy_hash WHERE l.status='ACTIVE' AND a.role='CHALLENGER'",null)){c.moveToFirst();return c.getLong(0);}}
    public static boolean canAdmit(SQLiteDatabase db,String family){long total=count(db,null),same=count(db,family),challengers=challengers(db);return total<LIMIT&&same<PER_FAMILY&&(same==0?total-challengers<BASE_LIMIT:challengers<CHALLENGER_LIMIT);}
    public static String family(ModelGenomeSU2 g){for(ModelGenomeSU2.Member m:g.members)if(m.kind.matches("R26F(0[1-9]|1[0-9]|2[0-4])"))return m.kind.substring(3);return "";}
    static void register(SQLiteDatabase db,String policy,String family,long birth,String parent){
        if(!db.inTransaction())throw new IllegalStateException("R26_SHADOW_ADMISSION_WRITER");boolean challenger=count(db,family)>1;
        db.execSQL("INSERT INTO r26_shadow_admission VALUES(?,?,?,?,?,?)",new Object[]{policy,family,challenger?"CHALLENGER":"BASE",birth,challenger?Math.addExact(birth,QualificationWindowR26.LENGTH):0,parent==null?"":parent});
    }
    /** Only a preregistered challenger can expire after its full first future window and evaluation.
     * Mainline/current certificates and the family's last active state are never evicted. */
    static long retireOne(SQLiteDatabase db,long nextTarget,long now){
        if(!db.inTransaction())throw new IllegalStateException("R26_SHADOW_RETIRE_WRITER");
        return retireOne(db,LegacyResearchScopeR29.realtime(db),nextTarget,now);
    }
    static long retireOne(SQLiteDatabase db,SQLiteDatabase realtime,long nextTarget,long now){
        if(!db.inTransaction())throw new IllegalStateException("R26_SHADOW_RETIRE_WRITER");String choice=ModelStoreSU2.currentChoice(realtime,nextTarget);
        try(Cursor c=db.rawQuery("SELECT l.policy_hash,l.family_id,a.expires_sequence FROM r26_shadow_admission a JOIN r26_live_model l ON l.policy_hash=a.policy_hash JOIN r26_cohort q ON q.genome_key=l.policy_hash WHERE a.role='CHALLENGER' AND l.status='ACTIVE' AND a.expires_sequence<=? AND q.checked_sequence>=a.expires_sequence AND q.protocol=? ORDER BY a.expires_sequence,l.policy_hash",new String[]{String.valueOf(nextTarget),QualificationWindowR26.PROTOCOL})){while(c.moveToNext()){
            String policy=c.getString(0);if(choice.equals("SU2:"+policy)||count(db,c.getString(1))<2||CertificateSU2.read(db,policy,now)!=null)continue;
            db.execSQL("UPDATE r26_live_model SET status='RETIRED_PREREGISTERED_CHALLENGER' WHERE policy_hash=? AND status='ACTIVE'",new Object[]{policy});
            db.execSQL("UPDATE su2_league SET state='RETIRED' WHERE genome_key=?",new Object[]{policy});db.execSQL("UPDATE su2_model_genome SET state='RETIRED' WHERE genome_key=?",new Object[]{policy});
            db.execSQL("UPDATE r26_cohort SET state='RETIRED_CHALLENGER_WINDOW_COMPLETE' WHERE genome_key=?",new Object[]{policy});
            db.execSQL("INSERT OR IGNORE INTO r26_certificate_revocation SELECT certificate_id,'CHALLENGER_RETIRED_AFTER_PREREGISTERED_WINDOW',? FROM su2_certificate WHERE genome_key=?",new Object[]{now,policy});
            db.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{policy,c.getLong(2),"PREREGISTERED_CHALLENGER_WINDOW_ENDED_WITHOUT_CURRENT_CERTIFICATE; NOT_A_QUALITY_COMPARISON; WEIGHTS_AND_ALPHA_RETAINED",now});return 1;
        }}return 0;
    }
}
