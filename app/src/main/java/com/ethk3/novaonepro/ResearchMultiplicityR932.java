package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Durable global -> family -> executable hypothesis -> five fixed terminal looks.
 * Allocation uses the predeclared summable 1/(n(n+1)) sequence. No refund/delete API.
 */
public final class ResearchMultiplicityR932 {
    public static final String FAMILY="research_alpha_family_r932",HYPOTHESIS="research_alpha_hypothesis_r932",ALIAS="research_alpha_alias_r932",LOOK="research_alpha_look_r932";
    /** Research registration has no inferential authority and spends no formal alpha. */
    public static final String RESEARCH="research_identity_r967";
    public static final String IDENTITY="research_identity_view_r967";
    public static final double TOTAL_ALPHA=.05;
    private ResearchMultiplicityR932(){}
    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.readyCommitted(db,"ResearchMultiplicityR967")) return;

        db.execSQL("CREATE TABLE IF NOT EXISTS "+FAMILY+"(family TEXT PRIMARY KEY,ordinal INTEGER NOT NULL UNIQUE,alpha REAL NOT NULL,registered_issue TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+HYPOTHESIS+"(hypothesis_hash TEXT PRIMARY KEY,structure_hash TEXT NOT NULL UNIQUE,family TEXT NOT NULL,ordinal INTEGER NOT NULL,alpha REAL NOT NULL,registered_issue TEXT NOT NULL,UNIQUE(family,ordinal))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ALIAS+"(candidate_key TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+LOOK+"(window_id TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL,window_no INTEGER NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,alpha REAL NOT NULL,state TEXT NOT NULL,result_hash TEXT NOT NULL,raw_p REAL,UNIQUE(hypothesis_hash,window_no))");
        for(String table:new String[]{FAMILY,HYPOTHESIS,ALIAS}) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_IDENTITY_IMMUTABLE'); END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_DENOMINATOR_PERMANENT'); END");
        }
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r932_look_identity BEFORE UPDATE OF window_id,hypothesis_hash,window_no,first_issue,last_issue,alpha ON "+LOOK+" BEGIN SELECT RAISE(ABORT,'R932_LOOK_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r932_look_delete BEFORE DELETE ON "+LOOK+" BEGIN SELECT RAISE(ABORT,'R932_LOOK_DENOMINATOR_PERMANENT'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r932_look_terminal BEFORE UPDATE ON "+LOOK+" WHEN OLD.state<>'REGISTERED' BEGIN SELECT RAISE(ABORT,'R932_TERMINAL_LOOK_IMMUTABLE'); END");
    
        db.execSQL("CREATE TABLE IF NOT EXISTS "+RESEARCH+"(candidate_key TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL,structure_hash TEXT NOT NULL,family TEXT NOT NULL,registered_issue TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r967_research_structure ON "+RESEARCH+"(structure_hash,family)");
        for(String operation:new String[]{"UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_research_identity_"+operation.toLowerCase(java.util.Locale.ROOT)+" BEFORE "+operation+" ON "+RESEARCH+" BEGIN SELECT RAISE(ABORT,'R967_RESEARCH_IDENTITY_IMMUTABLE'); END");
        // UNION gives metadata readers one stable interface while old paid ledgers stay untouched.
        db.execSQL("CREATE VIEW IF NOT EXISTS "+IDENTITY+" AS SELECT a.candidate_key,h.hypothesis_hash,h.structure_hash,h.family,h.registered_issue FROM "+ALIAS+" a JOIN "+HYPOTHESIS+" h ON a.hypothesis_hash=h.hypothesis_hash UNION ALL SELECT r.candidate_key,r.hypothesis_hash,r.structure_hash,r.family,r.registered_issue FROM "+RESEARCH+" r WHERE NOT EXISTS(SELECT 1 FROM "+ALIAS+" a WHERE a.candidate_key=r.candidate_key)");
        TransactionSchemaCacheR941.markCommitted(db,"ResearchMultiplicityR967");
    }
    /** Immutable metadata only; this is deliberately not formal trial registration. */
    public static String enrollResearchIdentity(SQLiteDatabase db,String key,String family,String structure,String issue) {
        transaction(db);
        if(key==null||key.isEmpty()||family==null||family.isEmpty()||structure==null||structure.isEmpty()
                ||!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("R967_RESEARCH_IDENTITY_INPUT");
        String hypothesis=V51ChecksumEngine.sha256("R932_FROZEN_HYPOTHESIS|"+family+'|'+structure);
        try(Cursor c=db.rawQuery("SELECT hypothesis_hash,structure_hash,family FROM "+IDENTITY+" WHERE candidate_key=?",new String[]{key})) {
            if(c.moveToFirst()) {
                if(!hypothesis.equals(c.getString(0))||!structure.equals(c.getString(1))||!family.equals(c.getString(2)))
                    throw new IllegalStateException("R967_RESEARCH_IDENTITY_REUSED");
                return hypothesis;
            }
        }
        try(Cursor c=db.rawQuery("SELECT family FROM "+IDENTITY+" WHERE structure_hash=?",new String[]{structure})) {
            while(c.moveToNext())if(!family.equals(c.getString(0)))throw new IllegalStateException("R932_STATISTICAL_FAMILY_REASSIGNMENT");
        }
        db.execSQL("INSERT INTO "+RESEARCH+" VALUES(?,?,?,?,?)",new Object[]{key,hypothesis,structure,family,issue});
        return hypothesis;
    }
    /** Compatibility entrypoint for existing creators/DEV/roster owners. */
    static String registerCandidate(SQLiteDatabase db,String key,String family,String structure,String issue) {
        return enrollResearchIdentity(db,key,family,structure,issue);
    }
    public static final class ResearchIdentity {
        public final String candidateKey,hypothesisHash,structureHash,family,registeredIssue;
        ResearchIdentity(String k,String h,String s,String f,String i){candidateKey=k;hypothesisHash=h;structureHash=s;family=f;registeredIssue=i;}
    }
    public static ResearchIdentity identity(SQLiteDatabase db,String key) {
        try(Cursor c=db.rawQuery("SELECT hypothesis_hash,structure_hash,family,registered_issue FROM "+IDENTITY+" WHERE candidate_key=?",new String[]{key})) {
            return c.moveToFirst()?new ResearchIdentity(key,c.getString(0),c.getString(1),c.getString(2),c.getString(3)):null;
        }
    }
    private static String allocateFormalHypothesis(SQLiteDatabase db,String key,String family,String structure,String issue) {
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_ALPHA_OWNER_RETIRED");
        transaction(db);
        String hypothesis=V51ChecksumEngine.sha256("R932_FROZEN_HYPOTHESIS|"+family+'|'+structure);
        try(Cursor c=db.rawQuery("SELECT h.hypothesis_hash,h.family FROM "+HYPOTHESIS+" h WHERE structure_hash=?",new String[]{structure})) {
            if(c.moveToFirst()) {if(!family.equals(c.getString(1)))throw new IllegalStateException("R932_STATISTICAL_FAMILY_REASSIGNMENT");hypothesis=c.getString(0);}
            else {
                double familyAlpha=0;
                try(Cursor f=db.rawQuery("SELECT alpha FROM "+FAMILY+" WHERE family=?",new String[]{family})){if(f.moveToFirst())familyAlpha=f.getDouble(0);}
                if(familyAlpha==0) {
                    long ordinal=1;try(Cursor f=db.rawQuery("SELECT COALESCE(MAX(ordinal),0)+1 FROM "+FAMILY,null)){if(f.moveToFirst())ordinal=f.getLong(0);}
                    familyAlpha=allocation(TOTAL_ALPHA,ordinal);db.execSQL("INSERT INTO "+FAMILY+" VALUES(?,?,?,?)",new Object[]{family,ordinal,familyAlpha,issue});
                }
                long ordinal=1;try(Cursor h=db.rawQuery("SELECT COALESCE(MAX(ordinal),0)+1 FROM "+HYPOTHESIS+" WHERE family=?",new String[]{family})){if(h.moveToFirst())ordinal=h.getLong(0);}
                db.execSQL("INSERT INTO "+HYPOTHESIS+" VALUES(?,?,?,?,?,?)",new Object[]{hypothesis,structure,family,ordinal,allocation(familyAlpha,ordinal),issue});
            }
        }
        db.execSQL("INSERT OR IGNORE INTO "+ALIAS+" VALUES(?,?)",new Object[]{key,hypothesis});
        try(Cursor c=db.rawQuery("SELECT hypothesis_hash FROM "+ALIAS+" WHERE candidate_key=?",new String[]{key})){if(!c.moveToFirst()||!hypothesis.equals(c.getString(0)))throw new IllegalStateException("R932_HYPOTHESIS_ALIAS_REUSED");}
        return hypothesis;
    }
    static double registerLook(SQLiteDatabase db,String key,String window,int number,String first,String last) {
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_ALPHA_OWNER_RETIRED");
        transaction(db);if(number<1||number>5)throw new IllegalArgumentException("R932_LOOK_NUMBER");
        ResearchIdentity research=identity(db,key);
        if(research==null||research.registeredIssue.compareTo(first)>0)throw new IllegalStateException("R967_FORMAL_REQUIRES_PRIOR_RESEARCH_IDENTITY");
        // The old allocation algorithm and irrevocable ledger are retained. Allocation now
        // happens at the formal endpoint registration, never while exploring or calibrating.
        allocateFormalHypothesis(db,key,research.family,research.structureHash,research.registeredIssue);
        String hypothesis;double alpha;
        try(Cursor c=db.rawQuery("SELECT h.hypothesis_hash,h.alpha,h.registered_issue FROM "+ALIAS+" a JOIN "+HYPOTHESIS+" h ON a.hypothesis_hash=h.hypothesis_hash WHERE a.candidate_key=?",new String[]{key})) {
            if(!c.moveToFirst()||c.getString(2).compareTo(first)>0)throw new IllegalStateException("R932_LOOK_REQUIRES_PREDRAW_HYPOTHESIS");hypothesis=c.getString(0);alpha=c.getDouble(1)/5;
        }
        db.execSQL("INSERT OR IGNORE INTO "+LOOK+" VALUES(?,?,?,?,?,?,'REGISTERED','',NULL)",new Object[]{window,hypothesis,number,first,last,alpha});
        try(Cursor c=db.rawQuery("SELECT window_id,first_issue,last_issue FROM "+LOOK+" WHERE hypothesis_hash=? AND window_no=?",new String[]{hypothesis,String.valueOf(number)})) {
            if(!c.moveToFirst()||!window.equals(c.getString(0))||!first.equals(c.getString(1))||!last.equals(c.getString(2)))throw new IllegalStateException("R932_LOOK_RENAME_OR_RETRY_REJECTED");
        }
        return alpha;
    }
    static void close(SQLiteDatabase db,String window,String endpoint,String state,String result,Double rawP) {
        if(ResearchGatePlanR967.installed().isCp3)throw new IllegalStateException("CP3_LEGACY_ALPHA_OWNER_RETIRED");
        transaction(db);
        try(Cursor c=db.rawQuery("SELECT last_issue,state,result_hash,raw_p FROM "+LOOK+" WHERE window_id=?",new String[]{window})) {
            if(!c.moveToFirst())throw new IllegalStateException("R932_LOOK_NOT_REGISTERED");
            if(endpoint.compareTo(c.getString(0))<0)throw new IllegalStateException("R932_EARLY_LOOK_FORBIDDEN");
            try(Cursor actual=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{c.getString(0)})){if(!actual.moveToFirst())throw new IllegalStateException("R932_LOOK_ENDPOINT_NOT_OBSERVED");}
            if(!"REGISTERED".equals(c.getString(1))) {
                if(!state.equals(c.getString(1))||!result.equals(c.getString(2))||(rawP==null)!=c.isNull(3)||rawP!=null&&Double.compare(rawP,c.getDouble(3))!=0)throw new IllegalStateException("R932_TERMINAL_LOOK_CHANGED");return;
            }
        }
        if(rawP!=null&&(!Double.isFinite(rawP)||rawP<0||rawP>1))throw new IllegalArgumentException("R932_LOOK_P_VALUE");
        db.execSQL("UPDATE "+LOOK+" SET state=?,result_hash=?,raw_p=? WHERE window_id=? AND state='REGISTERED'",new Object[]{state,result,rawP,window});
    }
    static double pairedP(int n,int signedHitDifference){
        if(n<0||Math.abs((long)signedHitDifference)>n)throw new IllegalArgumentException("R932_PAIRED_P_SUPPORT");
        return n==0||signedHitDifference<=0?1:Math.exp(-signedHitDifference*(double)signedHitDifference/(2d*n));
    }
    private static double allocation(double budget,long ordinal){double alpha=budget/(ordinal*(ordinal+1d));if(!Double.isFinite(alpha)||alpha<=0)throw new IllegalStateException("R932_ALPHA_EXHAUSTED");return alpha;}
    private static void transaction(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R932_MULTIPLICITY_TRANSACTION_REQUIRED");}
}
