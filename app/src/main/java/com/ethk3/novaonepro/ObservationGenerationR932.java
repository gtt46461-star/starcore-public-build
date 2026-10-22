package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Paid, append-only observation generations. A system pause is not a failed-model reset.
 * Old windows, planned denominators, terminal looks, family and candidate identity are retained.
 * This class owns no predictions, settlements, certificates, threads or REAL decisions.
 */
public final class ObservationGenerationR932 {
    public static final String META="research_window_generation_r932";
    public static final String RESTART="research_observation_restart_r932";
    private ObservationGenerationR932(){}
    static final class Chain {
        final String generation,claim;final int start,stage;
        Chain(String generation,String claim,int start,int stage){this.generation=generation;this.claim=claim;this.start=start;this.stage=stage;}
    }
    static void schema(SQLiteDatabase db){
        if (TransactionSchemaCacheR941.readyCommitted(db,"ObservationGenerationR932")) return;

        db.execSQL("CREATE TABLE IF NOT EXISTS "+META+"(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,generation_id TEXT NOT NULL,start_window_no INTEGER NOT NULL,stage_no INTEGER NOT NULL,claim_key TEXT NOT NULL,record_hash TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_window_generation ON "+META+"(candidate_key,generation_id,stage_no)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+RESTART+"(generation_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,start_window_no INTEGER NOT NULL,previous_window_id TEXT NOT NULL UNIQUE,family_id TEXT NOT NULL,claim_key TEXT NOT NULL,first_issue TEXT NOT NULL,prior_state TEXT NOT NULL,prior_reason TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,selector_hash TEXT NOT NULL,structural_hash TEXT NOT NULL,record_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        for(String table:new String[]{META,RESTART}){
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_GENERATION_IMMUTABLE'); END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_immutable_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_HISTORY_PERMANENT'); END");
        }
    
        TransactionSchemaCacheR941.markCommitted(db,"ObservationGenerationR932");
    }
    static Chain of(SQLiteDatabase db,String key,String id,int serial){
        try(Cursor c=db.rawQuery("SELECT generation_id,start_window_no,stage_no,claim_key,record_hash,candidate_key FROM "+META+" WHERE window_id=?",new String[]{id})){
            if(c.moveToFirst()){
                Chain chain=new Chain(c.getString(0),c.getString(3),c.getInt(1),c.getInt(2));
                if(!key.equals(c.getString(5))||chain.start<1||chain.stage<1||chain.stage>5||serial!=chain.start+chain.stage-1||!record(id,key,chain).equals(c.getString(4)))throw new IllegalStateException("R932_OBSERVATION_GENERATION_DRIFT");
                return chain;
            }
        }
        // Original 1..5 windows remain readable. They cannot be reinterpreted as a restart.
        if(serial<1||serial>5)throw new IllegalStateException("R932_OBSERVATION_GENERATION_MISSING");
        return new Chain(V51ChecksumEngine.sha256("R932_BASE_OBSERVATION|"+key),key,1,serial);
    }
    static Chain next(SQLiteDatabase db,String key,int serial,String first){
        if(serial==1)return new Chain(V51ChecksumEngine.sha256("R932_BASE_OBSERVATION|"+key),key,1,1);
        try(Cursor c=db.rawQuery("SELECT window_id,window_no,state,last_issue FROM "+FixedResearchWindowR932.WINDOW+" WHERE candidate_key=? ORDER BY window_no DESC LIMIT 1",new String[]{key})){
            if(!c.moveToFirst()||serial!=c.getInt(1)+1||!"PASS".equals(c.getString(2))||first.compareTo(c.getString(3))<=0)throw new IllegalStateException("R932_WINDOW_REQUIRES_PRIOR_PASS_AND_NEW_ISSUES");
            Chain prior=of(db,key,c.getString(0),c.getInt(1));if(prior.stage>=5)throw new IllegalArgumentException("R932_FIXED_WINDOW_SIZE");
            return new Chain(prior.generation,prior.claim,prior.start,prior.stage+1);
        }
    }
    static int firstSerial(SQLiteDatabase db,String key){
        try(Cursor c=db.rawQuery("SELECT window_id,window_no FROM "+FixedResearchWindowR932.WINDOW+" WHERE candidate_key=? ORDER BY window_no DESC LIMIT 1",new String[]{key})){
            return c.moveToFirst()?of(db,key,c.getString(0),c.getInt(1)).start:1;
        }
    }
    static void bind(SQLiteDatabase db,String id,String key,Chain chain){
        if(!db.inTransaction())throw new IllegalStateException("R932_OBSERVATION_TRANSACTION_REQUIRED");
        String hash=record(id,key,chain);
        db.execSQL("INSERT OR IGNORE INTO "+META+" VALUES(?,?,?,?,?,?,?)",new Object[]{id,key,chain.generation,chain.start,chain.stage,chain.claim,hash});
        try(Cursor c=db.rawQuery("SELECT record_hash FROM "+META+" WHERE window_id=?",new String[]{id})){if(!c.moveToFirst()||!hash.equals(c.getString(0)))throw new IllegalStateException("R932_OBSERVATION_BIND_CONFLICT");}
    }
    private static String record(String id,String key,Chain chain){return V51ChecksumEngine.sha256(id+'|'+key+'|'+chain.generation+'|'+chain.start+'|'+chain.stage+'|'+chain.claim);}
    static boolean isSystemPause(String state){return "PAUSED".equals(state)||"PAUSED_CAPTURE".equals(state)||"PAUSED_DEPENDENCY".equals(state);}

    /** Invoked only after a new actual predraw action and an earlier valid CAL are available. */
    static String recover(SQLiteDatabase db,String key,String structural,String bundle,String selector,String first,String cal){
        if(ResearchGatePlanR967.installed().isCp3)return recoverCp4(db,key,structural,bundle,selector,first,cal);
        if(!db.inTransaction()||!TargetIssueResolver.isValid(first))throw new IllegalStateException("R932_OBSERVATION_TRANSACTION_REQUIRED");
        String oldId,last,oldState,reason,oldStructure,oldBundle,oldSelector;int serial;
        try(Cursor c=db.rawQuery("SELECT window_id,last_issue,state,reason,structural_hash,window_no,champion_hash,selection_hash FROM "+FixedResearchWindowR932.WINDOW+" WHERE candidate_key=? ORDER BY window_no DESC LIMIT 1",new String[]{key})){
            if(!c.moveToFirst())throw new IllegalStateException("R932_OBSERVATION_PRIOR_WINDOW_MISSING");
            oldId=c.getString(0);last=c.getString(1);oldState=c.getString(2);reason=c.getString(3);oldStructure=c.getString(4);serial=c.getInt(5)+1;oldBundle=c.getString(6);oldSelector=c.getString(7);
        }
        String oldContract="";try(Cursor c=db.rawQuery("SELECT contract_hash FROM "+FixedResearchWindowR932.CONTRACT+" WHERE window_id=?",new String[]{oldId})){if(c.moveToFirst())oldContract=c.getString(0);}
        boolean completedDependencyChange="PASS".equals(oldState)&&(!bundle.equals(oldBundle)||!selector.equals(oldSelector)||!FixedResearchWindowR932.contract(db).equals(oldContract));
        if(!isSystemPause(oldState)&&!completedDependencyChange)throw new IllegalStateException("R932_FAILED_WINDOW_CANNOT_RESTART");
        if(completedDependencyChange)reason="DEPENDENCY_CHANGED_AFTER_COMPLETED_WINDOW|"+oldContract;
        if(!structural.equals(oldStructure))throw new IllegalStateException("R932_OBSERVATION_STRUCTURE_CHANGE_REQUIRES_NEW_CANDIDATE");
        if(first.compareTo(last)<=0)return null;
        // Never erase an unfinished denominator or select a restart inside the old planned window.
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(latest.compareTo(last)<0||!TargetIssueResolver.next(latest).targetIssue.equals(first))return null;
        if(!bundle.equals(ModelBundleRuntimeR932.current().hash)||!selector.equals(SelectionEvidenceContractR929.installedPolicy().hash))throw new IllegalStateException("R932_OBSERVATION_CURRENT_INPUT_REQUIRED");
        String study=CandidateCalibrationR932.study(db,key,structural,bundle,selector);
        try(Cursor c=db.rawQuery("SELECT model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key=? AND study_hash=? AND model_hash=?",new String[]{key,study,cal})){
            if(!c.moveToFirst()||!cal.equals(V51ChecksumEngine.sha256(c.getString(0)))||c.getString(1).compareTo(first)>=0)throw new IllegalStateException("R932_OBSERVATION_EARLIER_CAL_REQUIRED");
        }
        ResearchLifecycleR932.schema(db);ResearchPlatformStoreR929.createSchema(db);
        if(ResearchLifecycleR932.managed(db,key))try(Cursor c=db.rawQuery("SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",new String[]{key})){
            if(c.moveToFirst()&&("QUALITY_PRUNED".equals(c.getString(0))||"QUARANTINED".equals(c.getString(0))||"RESOURCE_RETIRED".equals(c.getString(0))))throw new IllegalStateException("R932_OBSERVATION_REQUIRES_EXPLICIT_QUALITY_AUDIT");
        }
        String family="",parentHypothesis="";
        try(Cursor c=db.rawQuery("SELECT h.family,h.hypothesis_hash,h.structure_hash FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{key})){
            if(!c.moveToFirst()||!structural.equals(c.getString(2)))throw new IllegalStateException("R932_OBSERVATION_ORIGINAL_FAMILY_REQUIRED");family=c.getString(0);parentHypothesis=c.getString(1);
        }
        String contract=FixedResearchWindowR932.contract(db);
        String generation=V51ChecksumEngine.sha256("R932_SYSTEM_RECOVERY_V1|"+key+'|'+structural+'|'+oldId+'|'+bundle+'|'+selector+'|'+contract+'|'+first);
        String claim="observation_recovery:"+generation;
        // A new comparison claim pays the next summable allocation in the SAME original family.
        // This is not a renamed candidate, a refunded look, or inherited Future support.
        ResearchMultiplicityR932.registerCandidate(db,claim,family,V51ChecksumEngine.sha256("R932_RECOVERY_CLAIM|"+parentHypothesis+'|'+generation),first);
        try(Cursor c=db.rawQuery("SELECT state FROM "+ResearchMultiplicityR932.LOOK+" WHERE window_id=?",new String[]{oldId})){
            if(!c.moveToFirst())throw new IllegalStateException("R932_OBSERVATION_PRIOR_LOOK_MISSING");
            if("REGISTERED".equals(c.getString(0))){
                String closure=V51ChecksumEngine.sha256("R932_SYSTEM_PAUSE_AT_ORIGINAL_ENDPOINT|"+oldId+'|'+last+'|'+oldState+'|'+reason);
                ResearchMultiplicityR932.close(db,oldId,latest,oldState,closure,null);
            }
        }
        String audit=V51ChecksumEngine.sha256(generation+'|'+key+'|'+serial+'|'+oldId+'|'+family+'|'+claim+'|'+first+'|'+oldState+'|'+reason+'|'+contract+'|'+bundle+'|'+selector+'|'+structural);
        db.execSQL("INSERT INTO "+RESTART+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{generation,key,serial,oldId,family,claim,first,oldState,reason,contract,bundle,selector,structural,audit,System.currentTimeMillis()});
        String window=FixedResearchWindowR932.persistWindow(db,key,structural,bundle,selector,first,120,serial,new Chain(generation,claim,serial,1));
        // Route revocation through the existing certificate store, in the SAME transaction.
        String consumer=key.startsWith("algorithm:")?"INVENTED_ALGORITHM":key.startsWith("genesis:")?"GENESIS_ASSET":key.startsWith("formula:")?"FORMULA_GRAPH":"";
        if(consumer.isEmpty())throw new IllegalStateException("R932_OBSERVATION_CONSUMER_UNKNOWN");
        try(Cursor c=db.rawQuery("SELECT certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND state='ACTIVE'",new String[]{consumer,key.substring(key.indexOf(':')+1)})){
            while(c.moveToNext())ResearchPlatformStoreR929.revokeInTransaction(db,c.getString(0),"NEW_PAID_OBSERVATION_GENERATION|"+generation,System.currentTimeMillis());
        }
        ResearchLifecycleR932.enroll(db,key,structural,first,"B");
        ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.OBSERVING,"SYSTEM_RECOVERED_NEW_PAID_OBSERVATION|"+generation,first,audit,0,0);
        return window;
    }

    static String recoverCp4(SQLiteDatabase db,String key,String structure,String bundle,String selector,String first,String cal){
        if(!db.inTransaction())throw new IllegalStateException("CP4_RECOVERY_TX_REQUIRED");
        String oldWindow="",life="",end="",state="",reason="";int serial=0;
        try(Cursor c=db.rawQuery("SELECT w.window_id,w.window_no,w.state,w.reason,b.lifetime_id,l.last_issue FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+ResearchGatePlanR967.CP4_BINDING+" b ON b.window_id=w.window_id JOIN "+ResearchGatePlanR967.CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id WHERE w.candidate_key=? ORDER BY w.window_no DESC LIMIT 1",new String[]{key})){
            if(!c.moveToFirst())throw new IllegalStateException("CP4_PRIOR_LIFETIME_MISSING");oldWindow=c.getString(0);serial=c.getInt(1)+1;state=c.getString(2);reason=c.getString(3);life=c.getString(4);end=c.getString(5);
        }
        if(!isSystemPause(state)&&!"PASS".equals(state))throw new IllegalStateException("CP4_QUALITY_FAILURE_REQUIRES_NEW_CANDIDATE");
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        // A gap does not reset the paid lifetime while its maximum registered endpoint is open.
        if(latest.compareTo(end)<0||!TargetIssueResolver.next(latest).targetIssue.equals(first))return null;
        String study=CandidateCalibrationR932.study(db,key,structure,bundle,selector);
        try(Cursor c=db.rawQuery("SELECT model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key=? AND study_hash=? AND model_hash=?",new String[]{key,study,cal})){
            if(!c.moveToFirst()||!cal.equals(V51ChecksumEngine.sha256(c.getString(0)))||c.getString(1).compareTo(first)>=0)throw new IllegalStateException("CP4_RECOVERY_CALIBRATION");
        }
        MultipleComparisonRegistryR930.Registration old=ResearchGatePlanR967.registrationForWindowCp4(db,oldWindow);
        MultipleComparisonRegistryR930.Snapshot prior=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,old.hypothesisId);
        if(prior==null)throw new IllegalStateException("CP4_OLD_HYPOTHESIS_MISSING");
        if(prior.state==MultipleComparisonRegistryR930.State.OBSERVING)MultipleComparisonRegistryR930.decideCp3WithinTransaction(db,old,1d,V51ChecksumEngine.sha256("CP4_INCOMPLETE_PROTOCOL_NO_ADVANTAGE_CLAIM|"+life+'|'+end+'|'+state+'|'+reason));
        String generation=V51ChecksumEngine.sha256("CP4_NEW_PAID_PROTOCOL|"+life+'|'+first+'|'+structure+'|'+bundle+'|'+selector+'|'+cal),claim="cp4_recovery:"+generation;
        String contract=FixedResearchWindowR932.contract(db),record=V51ChecksumEngine.sha256(generation+'|'+key+'|'+serial+'|'+oldWindow+'|'+first+'|'+contract);
        db.execSQL("INSERT INTO "+RESTART+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{generation,key,serial,oldWindow,"CP3_ALL_PRODUCTION",claim,first,state,reason,contract,bundle,selector,structure,record,System.currentTimeMillis()});
        String window=FixedResearchWindowR932.persistWindow(db,key,structure,bundle,selector,first,120,serial,new Chain(generation,claim,serial,1));
        String consumer=key.startsWith("algorithm:")?"INVENTED_ALGORITHM":key.startsWith("genesis:")?"GENESIS_ASSET":"FORMULA_GRAPH";
        try(Cursor c=db.rawQuery("SELECT certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND state='ACTIVE'",new String[]{consumer,key.substring(key.indexOf(':')+1)})){
            while(c.moveToNext())ResearchPlatformStoreR929.revokeInTransaction(db,c.getString(0),"CP4_NEW_PAID_PROTOCOL|"+generation,System.currentTimeMillis());
        }
        return window;
    }

}
