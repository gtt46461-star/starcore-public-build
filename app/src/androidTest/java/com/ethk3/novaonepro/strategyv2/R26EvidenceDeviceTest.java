package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** SYNTHETIC wiring only: isolated SQLite, never the application's canonical DB or natural-quality ledger. */
@RunWith(AndroidJUnit4.class)
public final class R26EvidenceDeviceTest {
    private static void setup(SQLiteDatabase db){RuntimeSchemaR26.ensure(db);StrategySchemaSU2.ensure(db);for(String sql:SqlStatementsSU1.SQL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS su1_family_budget")||sql.startsWith("CREATE TABLE IF NOT EXISTS su1_alpha_reservation"))db.execSQL(sql);}
    @Test public void completeSyntheticAggregateCanIssueAndReadExactlyOneBoundCertificate()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){db.beginTransaction();try{
            setup(db);long birth=1000,now=System.currentTimeMillis();ModelGenomeSU2 g=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_ISOLATED_DEVICE_TEST"),birth-1);String policy=g.key(),benchmark=PolicyIdentityR26.sha256("SYNTHETIC_REGISTERED_BENCHMARK");
            ModelStoreSU2.joinLeague(db,"XI_INCUMBENT:"+benchmark,"XI_INCUMBENT","SYNTHETIC",benchmark,birth);
            ModelStoreSU2.saveGenome(db,new ModelEvolutionSU2.Ranked(g,"SYNTHETIC_TEST_ONLY","",""),"SYNTHETIC",birth,0,0,"RESEARCH",now);ModelStoreSU2.joinLeague(db,"SU2:"+policy,"SU2","SYNTHETIC",policy,birth);CertificateSU2.preregister(db,policy,birth);
            EvidenceBlockR26 block=new EvidenceBlockR26();for(int t=0;t<720;t++)block.add(t<108?3:2,t%5<2,.4,t<108?3:2,t%10<3);
            for(int i=0;i<96;i++)db.execSQL("INSERT INTO r26_evidence_block VALUES(?,?,?,?,?,?)",new Object[]{policy,i,birth+i*720,birth+(i+1)*720,block.bytes(),PolicyIdentityR26.sha256("SYNTHETIC_BLOCK:"+i)});
            QualificationWindowR26.Window calendar=QualificationWindowR26.completed(birth,birth+QualificationWindowR26.LENGTH);EvidenceAccumulatorR26.Window aggregate=EvidenceAccumulatorR26.read(db,policy,calendar);assertNotNull(aggregate);assertTrue(aggregate.validation.acts>120);
            double alpha;try(Cursor c=db.rawQuery("SELECT family_alpha FROM r26_cohort WHERE genome_key=?",new String[]{policy})){c.moveToFirst();alpha=c.getDouble(0);}
            String cohort=calendar.from+":"+calendar.to;AlphaLedgerSU1.reserveClaim(db,HashingSU1.fields("R26_PREDECLARED_LOOK",policy,cohort),alpha,1);db.execSQL("INSERT INTO r26_look VALUES(?,?,?,?,?)",new Object[]{policy,cohort,aggregate.digest,1,alpha});
            assertTrue(CertificateSU2.issueWithin(db,policy,HashingSU1.text(g.text()),calendar,aggregate.digest,now));assertTrue(CertificateSU2.issueWithin(db,policy,HashingSU1.text(g.text()),calendar,aggregate.digest,now+1000));
            CertificateSU2.Authority authority=CertificateSU2.read(db,policy,now+2000);assertNotNull(authority);assertEquals(policy,authority.genome);
            try(Cursor c=db.rawQuery("SELECT count(*) FROM su2_certificate",null)){c.moveToFirst();assertEquals(1,c.getInt(0));}
            assertFalse(CertificateSU2.issueWithin(db,policy,HashingSU1.text(g.text()),calendar,PolicyIdentityR26.sha256("changed evidence"),now));
            assertNull(CertificateSU2.read(db,policy,now+86400001));
            db.execSQL("INSERT INTO r26_certificate_revocation VALUES(?,'SYNTHETIC_REVOKED',?)",new Object[]{authority.certificate,now+3000});assertNull(CertificateSU2.read(db,policy,now+4000));db.setTransactionSuccessful();
        }finally{db.endTransaction();}}
    }
    @Test public void aggregateCalendarRejectsMissingOrRevisedBlocks()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){db.beginTransaction();try{setup(db);
            long birth=100;String policy="SYNTHETIC_AGGREGATE";EvidenceBlockR26 block=new EvidenceBlockR26();for(int i=0;i<720;i++)block.add(2,false,Double.NaN,2,false);
            db.execSQL("INSERT INTO r26_evidence_block VALUES(?,0,100,820,?,'SYNTHETIC')",new Object[]{policy,block.bytes()});assertNull(EvidenceAccumulatorR26.read(db,policy,QualificationWindowR26.completed(birth,birth+QualificationWindowR26.LENGTH)));
            try{db.execSQL("UPDATE r26_evidence_block SET block_hash='revised'");fail("sealed statistics changed");}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.getMessage().contains("IMMUTABLE"));}db.setTransactionSuccessful();
        }finally{db.endTransaction();}}
    }
}
