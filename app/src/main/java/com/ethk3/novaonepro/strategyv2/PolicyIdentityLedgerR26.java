package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
/** Persist the complete thirteen-field typed policy, not only a genotype interpreted by a later APK. */
public final class PolicyIdentityLedgerR26 {
    private PolicyIdentityLedgerR26() {}
    public static boolean record(SQLiteDatabase db,String text,String expected){
        if(!db.inTransaction())throw new IllegalStateException("R26_POLICY_IDENTITY_WRITER");
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_policy_identity'",null)){if(!c.moveToFirst())return false;}
        PolicyIdentityR26.Policy identity;String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        if(StageStrategySU2.isStage(text)){
            if(!text.startsWith(StageStrategySU2.PREFIX_R26))return false;String[] fields=text.split("\\|");if(fields.length!=4)throw new IllegalArgumentException("R26_STAGE_IDENTITY_TEXT");abi=fields[2];identity=StrategyPolicyIdentityR26.policy(text,"FITTED_BUNDLE_AND_BOUND_BOOTSTRAP",abi);
        }else if(CommitteeSU2.isCommittee(text)){
            CommitteeSU2 committee=CommitteeSU2.parse(text);if(!committee.legalReadoutR26())return false;identity=StrategyPolicyIdentityR26.policy(text,"DETERMINISTIC_BOOTSTRAP_OF_BOUND_MEMBER_DATASETS",abi);
        }else{ModelGenomeSU2 genome=ModelGenomeSU2.parse(text);if(genome.legacyIdentity())return false;identity=genome.identityR26();}
        if(!identity.hash.equals(expected)||!PolicyIdentityR26.sha256(identity.canonical).equals(expected))throw new IllegalStateException("R26_POLICY_IDENTITY_HASH_MISMATCH");
        db.execSQL("INSERT OR IGNORE INTO r26_policy_identity VALUES(?,?,?,?,?)",new Object[]{expected,identity.canonical,text,abi,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT canonical_policy,genotype_text,numerical_abi FROM r26_policy_identity WHERE policy_hash=?",new String[]{expected})){if(!c.moveToFirst()||!identity.canonical.equals(c.getString(0))||!text.equals(c.getString(1))||!abi.equals(c.getString(2)))throw new IllegalStateException("R26_POLICY_IDENTITY_CONFLICT");}return true;
    }
    public static String canonical(SQLiteDatabase db,String hash){
        try(Cursor c=db.rawQuery("SELECT canonical_policy FROM r26_policy_identity WHERE policy_hash=?",new String[]{hash})){if(!c.moveToFirst())return null;String canonical=c.getString(0);if(!PolicyIdentityR26.sha256(canonical).equals(hash))throw new IllegalStateException("R26_POLICY_IDENTITY_CORRUPT");return canonical;}
    }
    public interface Progress {void check()throws IOException;}
    /** Streaming full identity attachment; compact requests explicitly exclude canonical bodies. */
    public static void export(SQLiteDatabase db,File target,String snapshot,boolean bodies,Progress progress)throws IOException {
        boolean present;try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_policy_identity'",null)){present=c.moveToFirst();}long count=0;if(present)try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_policy_identity",null)){c.moveToFirst();count=c.getLong(0);}
        try(FileOutputStream fd=new FileOutputStream(target);Writer out=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),32768)){
            out.write(new JSONObject().put("schema","R26_POLICY_IDENTITY_EXPORT_V1").put("snapshotSha256",snapshot).put("rows",count).put("canonicalByteValidation",bodies&&present?"EACH_CANONICAL_SHA256_VERIFIED":"NOT_RUN_COMPACT_BYTE_BUDGET").put("canonicalBodiesIncluded",bodies&&present).put("state",!present?"LEGACY_IDENTITY_TABLE_NOT_AVAILABLE":bodies?"FULL_CANONICAL_IDENTITIES":"CANONICAL_BODIES_EXCLUDED_BY_COMPACT_REQUEST").put("qualityQualificationGranted",false).toString());out.write('\n');
            if(present)try(Cursor c=db.rawQuery(bodies?"SELECT policy_hash,canonical_policy,genotype_text,numerical_abi,created_at FROM r26_policy_identity ORDER BY policy_hash":"SELECT policy_hash,NULL,NULL,numerical_abi,created_at FROM r26_policy_identity ORDER BY policy_hash",null)){while(c.moveToNext()){progress.check();String hash=c.getString(0),canonical=c.getString(1);if(!hash.matches("[0-9a-f]{64}")||bodies&&!PolicyIdentityR26.sha256(canonical).equals(hash))throw new IOException("R26_POLICY_IDENTITY_EXPORT_CORRUPT");JSONObject row=new JSONObject().put("policyHash",hash).put("numericalAbi",c.getString(3)).put("createdAt",c.getLong(4));if(bodies)row.put("canonicalPolicy",canonical).put("genotypeText",c.getString(2));out.write(row.toString());out.write('\n');}}
            progress.check();out.flush();fd.getFD().sync();
        }catch(JSONException bad){throw new IOException("R26_POLICY_IDENTITY_EXPORT_JSON",bad);}
    }
}
