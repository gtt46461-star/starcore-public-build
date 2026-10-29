package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.*;
import java.io.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Complete policy bytes in isolated SQLite, independent of future genotype interpretation. */
@RunWith(AndroidJUnit4.class)
public final class R26PolicyIdentityDeviceTest {
    @Test public void fullTypedPolicyIsImmutableIdempotentAndMatchesItsStoredHash()throws Exception {try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        db.beginTransaction();try{RuntimeSchemaR26.ensure(db);ModelGenomeSU2 g=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_SOURCE"),100);
            assertEquals(g.key(),g.identityR26().hash);assertTrue(PolicyIdentityLedgerR26.record(db,g.text(),g.key()));assertTrue(PolicyIdentityLedgerR26.record(db,g.text(),g.key()));String stored=PolicyIdentityLedgerR26.canonical(db,g.key());assertEquals(g.key(),PolicyIdentityR26.sha256(stored));for(String field:PolicyIdentityR26.RULES)assertTrue(stored.contains("\""+field+"\""));assertTrue(stored.contains(com.ethk3.novaonepro.ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256));
            try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_policy_identity",null)){c.moveToFirst();assertEquals(1,c.getInt(0));}assertNull(PolicyIdentityLedgerR26.canonical(db,PolicyIdentityR26.sha256("unknown")));
            try{db.execSQL("UPDATE r26_policy_identity SET canonical_policy='changed'");fail("canonical identity changed");}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.getMessage().contains("IMMUTABLE"));}
            File target=File.createTempFile("r26-identity-",".jsonl",androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());try{
                PolicyIdentityLedgerR26.export(db,target,PolicyIdentityR26.sha256("SYNTHETIC_SNAPSHOT"),true,()->{});try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(target),java.nio.charset.StandardCharsets.UTF_8))){JSONObject header=new JSONObject(in.readLine()),body=new JSONObject(in.readLine());assertEquals(1,header.getInt("rows"));assertTrue(header.getBoolean("canonicalBodiesIncluded"));assertEquals(stored,body.getString("canonicalPolicy"));assertEquals(g.key(),PolicyIdentityR26.sha256(body.getString("canonicalPolicy")));assertNull(in.readLine());}
                PolicyIdentityLedgerR26.export(db,target,PolicyIdentityR26.sha256("SYNTHETIC_SNAPSHOT"),false,()->{});try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(target),java.nio.charset.StandardCharsets.UTF_8))){JSONObject header=new JSONObject(in.readLine()),body=new JSONObject(in.readLine());assertEquals("NOT_RUN_COMPACT_BYTE_BUDGET",header.getString("canonicalByteValidation"));assertFalse(header.getBoolean("canonicalBodiesIncluded"));assertFalse(body.has("canonicalPolicy"));}
            }finally{assertTrue(target.delete());}
            try{PolicyIdentityLedgerR26.record(db,g.text(),PolicyIdentityR26.sha256("wrong"));fail("wrong identity hash accepted");}catch(IllegalStateException expected){assertEquals("R26_POLICY_IDENTITY_HASH_MISMATCH",expected.getMessage());}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void compositeAndFittedPolicyRecordsKeepTheirExactAbiAndGraph(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        db.beginTransaction();try{RuntimeSchemaR26.ensure(db);String source=PolicyIdentityR26.sha256("SYNTHETIC_SOURCE");ModelGenomeSU2 a=FamilyRegistryR26.genome("F01").bindBirth(source,100),b=FamilyRegistryR26.genome("F17").bindBirth(source,100);CommitteeSU2 committee=new CommitteeSU2(Arrays.asList(a,b),.3);assertTrue(PolicyIdentityLedgerR26.record(db,committee.text(),committee.key()));assertEquals(committee.key(),PolicyIdentityR26.sha256(PolicyIdentityLedgerR26.canonical(db,committee.key())));
            // Identity-only fitted-bundle reference; no executable bundle or quality is claimed.
            String abi=PolicyIdentityR26.sha256("SYNTHETIC_PRIOR_NUMERICAL_ABI"),bundle=PolicyIdentityR26.sha256("SYNTHETIC_FITTED_BUNDLE"),text=StageStrategySU2.PREFIX_R26+abi+"|"+bundle,key=StageStrategySU2.keyOf(text);assertTrue(PolicyIdentityLedgerR26.record(db,text,key));String canonical=PolicyIdentityLedgerR26.canonical(db,key);assertTrue(canonical.contains(abi));assertTrue(canonical.contains(bundle));assertEquals(key,PolicyIdentityR26.sha256(canonical));assertFalse(StageStrategySU2.currentPolicy(text));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
}
