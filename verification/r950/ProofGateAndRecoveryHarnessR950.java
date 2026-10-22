package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** R950 production-method regression on the real host SQLite bridge. */
public final class ProofGateAndRecoveryHarnessR950 {
    private static void check(boolean ok,String name){
        if(!ok) throw new AssertionError(name);
        System.out.println(name+"=PASS");
    }
    private static long scalar(SQLiteDatabase db,String sql,String[] args){
        try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()?c.getLong(0):-1L;}
    }
    public static void main(String[] args) throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            final String target="202609140101";
            db.execSQL("CREATE TABLE alg(genome_id TEXT PRIMARY KEY,state_updates INTEGER,future_n INTEGER)");
            db.execSQL("CREATE TABLE "+AlgorithmProofRecoveryR948.HEAD+"(genome_id TEXT PRIMARY KEY,status TEXT,epoch_start_issue TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+"(genome_id TEXT,target_issue TEXT)");
            String gate="SELECT COUNT(*) FROM alg g WHERE g.genome_id=? AND "+AlgorithmProofRecoveryR948.freezeReadySql("g.genome_id",target);
            db.execSQL("INSERT INTO alg VALUES('new',0,0)");
            check(scalar(db,gate,new String[]{"new"})==1,"NEW_UNTRAINED_HEAD_MAY_ENROLL_AT_FIRST_FREEZE");
            db.execSQL("INSERT INTO alg VALUES('legacy',7,7)");
            check(scalar(db,gate,new String[]{"legacy"})==0,"LEGACY_MUTABLE_HEAD_BLOCKED_BEFORE_AUDIT");
            db.execSQL("INSERT INTO "+AlgorithmProofRecoveryR948.HEAD+" VALUES('legacy','CLEAN','')");
            check(scalar(db,gate,new String[]{"legacy"})==1,"AUDITED_CLEAN_HEAD_ADMITTED");
            db.execSQL("INSERT INTO alg VALUES('reset',9,9)");
            db.execSQL("INSERT INTO "+AlgorithmProofRecoveryR948.HEAD+" VALUES('reset','RESET_CLEAN','202609140102')");
            check(scalar(db,gate,new String[]{"reset"})==0,"RESET_HEAD_CANNOT_REUSE_PRE_EPOCH_TARGET");
            db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET epoch_start_issue=? WHERE genome_id='reset'",new Object[]{target});
            check(scalar(db,gate,new String[]{"reset"})==1,"RESET_HEAD_ADMITTED_AT_NEW_EPOCH");
            db.execSQL("INSERT INTO alg VALUES('suspicious',0,0)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" VALUES('suspicious','202609140000')");
            check(scalar(db,gate,new String[]{"suspicious"})==0,"PRIOR_PREDRAW_CANNOT_REENROLL_AS_NEW_HEAD");

            // Canonical durable recovery: a complete immutable roster is terminal even when a
            // globally active but non-admitted candidate has no prediction.
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT,snapshot_blob BLOB,snapshot_hash TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT,dirty INTEGER)");
            db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.PLAN+"(target_issue TEXT PRIMARY KEY)");
            db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.TASK+"(target_issue TEXT,candidate_key TEXT,selected INTEGER)");
            db.execSQL("CREATE TABLE "+ResearchCaptureLedgerR932.TABLE+"(target_issue TEXT,candidate_key TEXT,frozen INTEGER,full1200 INTEGER)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,?,?)",new Object[]{target,new byte[]{1},V51ChecksumEngine.sha256("snapshot")});
            db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.PLAN+" VALUES(?)",new Object[]{target});
            db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.TASK+" VALUES(?,?,1)",new Object[]{target,"formula:admitted"});
            db.execSQL("INSERT INTO "+ResearchCaptureLedgerR932.TABLE+" VALUES(?,?,1,1)",new Object[]{target,"formula:admitted"});
            DbStore store=new DbStore(db);
            check(ResearchCaptureQueueR946.recover(store).isEmpty(),"COMPLETE_ROSTER_NOT_REOPENED_BY_GLOBAL_MISSING_ASSET");
            db.execSQL("UPDATE "+ResearchCaptureLedgerR932.TABLE+" SET full1200=0 WHERE target_issue=?",new Object[]{target});
            check(target.equals(ResearchCaptureQueueR946.recover(store)),"INCOMPLETE_ADMITTED_ROSTER_RECOVERS_EXACT_TARGET");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,0)",new Object[]{target});
            check(ResearchCaptureQueueR946.recover(store).isEmpty(),"DRAWN_TARGET_NEVER_RETROACTIVELY_RECAPTURED");
            System.out.println("R950_PROOF_GATE_AND_CANONICAL_RECOVERY_PASS|scope=PRODUCTION_METHODS_REAL_HOST_SQLITE_NOT_PHONE_EFFECT");
        } finally { db.shutdown(); }
    }
}
