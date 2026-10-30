package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Real SQLite revision-2 upgrade of an already advanced FULL domain. Synthetic engineering only. */
public final class LegacyDomainExtensionR29HostTest {
    static final String HASH=StatsR27.sha256("original FULL capture"),ARTIFACT="v57_research_artifact_r811";
    static final long EPOCH=17;static int checks;
    static void check(String name,boolean good){System.out.println("R29_DOMAIN_EXTENSION|"+name+"|"+(good?"OK":"FAIL"));if(!good)throw new AssertionError(name);checks++;}
    static SQLiteDatabase open()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static long n(SQLiteDatabase db,String query){try(Cursor c=db.rawQuery(query,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase db,String query){try(Cursor c=db.rawQuery(query,null)){return c.moveToFirst()?c.getString(0):null;}}
    static LegacyDomainUpgradeR29.Check unlimited(){return new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,()->false);}
    static void artifact(SQLiteDatabase db){db.execSQL("CREATE TABLE "+ARTIFACT+"(model TEXT PRIMARY KEY,steps INTEGER,weight REAL,state BLOB,description TEXT)");}
    static void root(SQLiteDatabase db){
        artifact(db);db.execSQL("INSERT INTO "+ARTIFACT+" VALUES('FTRL_LEARNED',739,1.25,X'0000FF','optimizer and weights'),('nullable',NULL,NULL,NULL,NULL)");
        db.execSQL("CREATE INDEX extension_model_steps ON "+ARTIFACT+"(steps)");
        db.execSQL("CREATE TRIGGER extension_model_immutable BEFORE DELETE ON "+ARTIFACT+" BEGIN SELECT RAISE(ABORT,'ORIGINAL_MODEL_STATE'); END");
        db.execSQL("CREATE TABLE v57_batch34_evidence_link_r930(a TEXT,b INTEGER,state BLOB,PRIMARY KEY(a,b)) WITHOUT ROWID");db.execSQL("INSERT INTO v57_batch34_evidence_link_r930 VALUES('typed',-7,X'00FF')");
        db.execSQL("CREATE TABLE v57_controlled_opportunity_r922(id INTEGER PRIMARY KEY AUTOINCREMENT,v TEXT)");db.execSQL("INSERT INTO v57_controlled_opportunity_r922 VALUES(8,'kept'),(99,'reserved')");db.execSQL("DELETE FROM v57_controlled_opportunity_r922 WHERE id=99");
        db.execSQL("CREATE TABLE v57_legacy_predraw_context_r915(v TEXT)");db.execSQL("INSERT INTO v57_legacy_predraw_context_r915 VALUES('duplicate'),('duplicate'),(NULL)");
        db.execSQL("CREATE TABLE v57_legacy_postsettlement_context_r919(k TEXT PRIMARY KEY,v TEXT)");db.execSQL("INSERT INTO v57_legacy_postsettlement_context_r919 VALUES('settled','retained')");
        for(String name:new String[]{"r26_runtime","su1_model","su2_model","r27_policy"}){db.execSQL("CREATE TABLE "+name+"(k TEXT PRIMARY KEY,steps INTEGER)");db.execSQL("INSERT INTO "+name+" VALUES('learned',1)");}
        db.execSQL("CREATE TABLE r26_feedback(target INTEGER PRIMARY KEY)");db.execSQL("INSERT INTO r26_feedback VALUES(4)");
    }
    static void full(SQLiteDatabase db){
        db.execSQL(ResearchRouteR29.OWNER_DDL);db.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','schema','root',42,1)");
        db.execSQL(LegacyDomainUpgradeR29.DDL);db.execSQL("INSERT INTO r29_legacy_domain_owner VALUES(1,'FULL',?,?,1)",new Object[]{EPOCH,HASH});
        for(String name:new String[]{"r26_runtime","su1_model","su2_model","r27_policy"}){db.execSQL("CREATE TABLE "+name+"(k TEXT PRIMARY KEY,steps INTEGER)");db.execSQL("INSERT INTO "+name+" VALUES('learned',9999)");}
    }
    static LegacyDomainUpgradeR29.Snapshot capture(SQLiteDatabase root){return LegacyDomainUpgradeR29.capture(root,unlimited(),64L<<20,LegacyResearchOwnershipR29::newlyOwnedResearch,()->{});}
    static void apply(SQLiteDatabase target,LegacyDomainUpgradeR29.Snapshot capture,LegacyDomainUpgradeR29.Check check,LegacyDomainUpgradeR29.Fault fault){
        target.beginTransactionNonExclusive();try{LegacyDomainExtensionR29.mergeWithin(target,capture,EPOCH,check,fault);target.setTransactionSuccessful();}finally{target.endTransaction();}
    }
    static boolean fails(Runnable work,String reason){try{work.run();return false;}catch(RuntimeException expected){return String.valueOf(expected.getMessage()).contains(reason);}}
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);full(rp);LegacyDomainUpgradeR29.Snapshot captured=capture(root);
            check("E1_EXACT_FIVE_CLASSES_ONLY",captured.tables.size()==5&&captured.tables.stream().allMatch(t->LegacyResearchOwnershipR29.newlyOwnedResearch(t.table.name)));
            check("E2_OLD_FULL_IS_NOT_REVISION2_PROOF",!LegacyDomainExtensionR29.readyWithin(rp,EPOCH));
            artifact(rp);rp.execSQL("INSERT INTO "+ARTIFACT+" VALUES('FTRL_LEARNED',739,1.25,X'0000FF','optimizer and weights')");
            apply(rp,captured,unlimited(),name->{});
            check("E3_REVISION2_MARKER_AFTER_VERIFIED_DELTA",LegacyDomainExtensionR29.readyWithin(rp,EPOCH)&&captured.hash.equals(s(rp,"SELECT source_hash FROM r29_legacy_domain_extension")));
            check("E4_ADVANCED_R26_R27_SU1_SU2_NOT_REIMPORTED",n(rp,"SELECT steps FROM r26_runtime")==9999&&n(rp,"SELECT steps FROM r27_policy")==9999&&n(rp,"SELECT steps FROM su1_model")==9999&&n(rp,"SELECT steps FROM su2_model")==9999);
            check("E5_LEARNED_FTRL_TYPED_STATE_AND_NULLS_SURVIVE",n(rp,"SELECT steps FROM "+ARTIFACT+" WHERE state=X'0000FF' AND weight=1.25")==739&&n(rp,"SELECT count(*) FROM "+ARTIFACT+" WHERE steps IS NULL AND state IS NULL AND weight IS NULL AND description IS NULL")==1);
            check("E6_DUPLICATES_WITHOUT_ROWID_AND_SEQUENCE_SURVIVE",n(rp,"SELECT count(*) FROM v57_legacy_predraw_context_r915 WHERE v='duplicate'")==2&&n(rp,"SELECT count(*) FROM v57_batch34_evidence_link_r930 WHERE b=-7 AND state=X'00FF'")==1&&n(rp,"SELECT seq FROM sqlite_sequence WHERE name='v57_controlled_opportunity_r922'")==99);
            check("E7_ROOT_AND_ORIGINAL_FULL_PROOF_UNCHANGED",capture(root).hash.equals(captured.hash)&&HASH.equals(s(rp,"SELECT source_hash FROM r29_legacy_domain_owner"))&&n(root,"SELECT steps FROM r26_runtime")==1);
            check("E8_AUX_OBJECTS_AND_LOCAL_LEARNING_SCHEMA_EXIST",n(rp,"SELECT count(*) FROM sqlite_master WHERE name IN ('extension_model_steps','extension_model_immutable','r29_research_tx_receipt','r29_legacy_local_learned')")==4);
            rp.execSQL("UPDATE "+ARTIFACT+" SET steps=800 WHERE model='FTRL_LEARNED'");apply(rp,captured,unlimited(),name->{});
            check("E9_RETRY_PRESERVES_ADVANCED_DELTA_MODEL",n(rp,"SELECT steps FROM "+ARTIFACT+" WHERE model='FTRL_LEARNED'")==800&&n(rp,"SELECT count(*) FROM r29_legacy_domain_extension")==1);
            check("E10_ROUTE_EPOCH_CONFLICT_IS_EXPLICIT",fails(()->LegacyDomainExtensionR29.readyWithin(rp,EPOCH+1),"MARKER_CONFLICT"));
            rp.execSQL("UPDATE r29_legacy_domain_extension SET source_hash='invalid'");check("E11_MALFORMED_MARKER_IS_NOT_ACCEPTED",fails(()->LegacyDomainExtensionR29.readyWithin(rp,EPOCH),"MARKER_CONFLICT"));
        }
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);full(rp);artifact(rp);rp.execSQL("INSERT INTO "+ARTIFACT+" VALUES('FTRL_LEARNED',999,2.5,X'99','different learned model')");LegacyDomainUpgradeR29.Snapshot captured=capture(root);
            check("E12_CONFLICT_ROLLS_BACK_AND_PRESERVES_MODEL",fails(()->apply(rp,captured,unlimited(),name->{}),"EXISTING_STATE_CONFLICT")&&!LegacyDomainExtensionR29.readyWithin(rp,EPOCH)&&n(rp,"SELECT steps FROM "+ARTIFACT)==999);
        }
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);full(rp);LegacyDomainUpgradeR29.Snapshot captured=capture(root);
            check("E13_KILL_ROLLS_BACK_DATA_AND_MARKER",fails(()->apply(rp,captured,unlimited(),name->{throw new IllegalStateException("INJECTED_PROCESS_KILL");}),"INJECTED_PROCESS_KILL")&&!LegacyDomainUpgradeR29.table(rp,ARTIFACT)&&!LegacyDomainExtensionR29.readyWithin(rp,EPOCH));
            AtomicBoolean cancelled=new AtomicBoolean();check("E14_CANCEL_ROLLS_BACK",fails(()->apply(rp,captured,new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,cancelled::get),name->cancelled.set(true)),"CANCELLED")&&!LegacyDomainUpgradeR29.table(rp,ARTIFACT));
            AtomicLong clock=new AtomicLong();check("E15_BUDGET_ROLLS_BACK",fails(()->apply(rp,captured,new LegacyDomainUpgradeR29.Check(clock::get,1,()->false),name->clock.set(2)),"BUDGET")&&!LegacyDomainExtensionR29.readyWithin(rp,EPOCH));
            check("E16_CAPTURE_STORAGE_LIMIT_FAILS_CLOSED",fails(()->LegacyDomainUpgradeR29.capture(root,unlimited(),1,LegacyResearchOwnershipR29::newlyOwnedResearch,()->{}),"STORAGE_BUDGET"));
            check("E17_NONDELTA_SNAPSHOT_IS_REJECTED",fails(()->apply(rp,LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20),unlimited(),name->{}),"SCOPE_CONFLICT")&&!LegacyDomainUpgradeR29.table(rp,ARTIFACT));
        }
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);full(rp);artifact(rp);rp.execSQL("ALTER TABLE "+ARTIFACT+" ADD COLUMN unexpected TEXT");
            check("E18_SCHEMA_CONFLICT_ROLLS_BACK",fails(()->apply(rp,capture(root),unlimited(),name->{}),"SCHEMA_CONFLICT")&&!LegacyDomainExtensionR29.readyWithin(rp,EPOCH));
        }
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);full(rp);byte[] blob=new byte[(3<<20)+17];new Random(29).nextBytes(blob);root.execSQL("UPDATE "+ARTIFACT+" SET state=? WHERE model='FTRL_LEARNED'",new Object[]{blob});LegacyDomainUpgradeR29.Snapshot captured=capture(root);apply(rp,captured,unlimited(),name->{});
            byte[] restored;try(Cursor c=rp.rawQuery("SELECT state FROM "+ARTIFACT+" WHERE model='FTRL_LEARNED'",null)){c.moveToFirst();restored=c.getBlob(0);}
            check("E19_LARGE_LEARNED_ARTIFACT_RETAINS_ALL_BYTES",Arrays.equals(blob,restored));
        }
        try(SQLiteDatabase root=open()){
            root(root);check("E20_SOURCE_CHANGE_BEFORE_VERIFICATION_IS_REJECTED",fails(()->LegacyDomainUpgradeR29.capture(root,unlimited(),1L<<20,LegacyResearchOwnershipR29::newlyOwnedResearch,()->root.execSQL("UPDATE "+ARTIFACT+" SET steps=800 WHERE model='FTRL_LEARNED'")),"SOURCE_CHANGED"));
        }
        System.out.println("R29_DOMAIN_EXTENSION|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_UPGRADE_PENDING");
    }
}
