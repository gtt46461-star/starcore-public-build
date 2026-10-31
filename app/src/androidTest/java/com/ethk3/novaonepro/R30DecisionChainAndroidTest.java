package com.ethk3.novaonepro;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Isolated Android mechanics tests. Seeded certificates never count as natural quality or device endurance. */
@RunWith(AndroidJUnit4.class)
public final class R30DecisionChainAndroidTest {
    static final class Sandbox extends Application {
        final File root;final String prefix="r30-synthetic-decision-"+UUID.randomUUID();
        Sandbox(Context base){attachBaseContext(base);root=new File(base.getCacheDir(),prefix);if(!root.mkdirs())throw new IllegalStateException("sandbox");}
        File dir(String name){File f=new File(root,name);if(!f.isDirectory()&&!f.mkdirs())throw new IllegalStateException("sandbox directory");return f;}
        @Override public Context getApplicationContext(){return this;}
        @Override public Context createDeviceProtectedStorageContext(){return this;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+"-"+name,mode);}
        @Override public File getFilesDir(){return dir("files");}
        @Override public File getCacheDir(){return dir("cache");}
        @Override public File getDatabasePath(String name){return new File(dir("databases"),name);}
        @Override public ApplicationInfo getApplicationInfo(){ApplicationInfo info=new ApplicationInfo(super.getApplicationInfo());info.dataDir=root.getPath();return info;}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        @Override public boolean deleteDatabase(String name){return SQLiteDatabase.deleteDatabase(getDatabasePath(name));}
    }
    static final class Fixture implements AutoCloseable {
        final Sandbox app=new Sandbox(InstrumentationRegistry.getInstrumentation().getTargetContext());final DbStore store=new DbStore(app);final SQLiteDatabase db=store.readable();
        final ArrayList<Models.Draw> history=new ArrayList<>();final String base="202610310240",target="202610310241";final long seq=TargetIssueResolver.sequenceOrdinalSU1(target);
        final String certificate=HashingSU1.text("R30_SYNTHETIC_CERTIFICATE_MECHANICS_ONLY");final ModelGenomeSU2 genome=FamilyRegistryR26.genome("F17").bindBirth(HashingSU1.text("R30_SYNTHETIC_SOURCE"),seq-1);
        final long deadline;
        Fixture()throws Exception {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R30_DECISION_SCHEMA",w->{RuntimeSchemaR26.ensure(w);for(String sql:SqlStatementsSU1.SQL)w.execSQL(sql);StrategySchemaSU2.ensure(w);SchemaR27.ensure(w);return 0;});
            long latest=System.currentTimeMillis()-1000;
            for(int i=1;i<=240;i++){Models.Draw d=new Models.Draw();d.issue=String.format(Locale.ROOT,"20261031%04d",i);d.openTime=latest-(240-i)*60000L;d.fetchTime=d.openTime+500;for(int p=0;p<10;p++)d.numbers[p]=(p+i-1)%10+1;history.add(d);}
            assertEquals(240,store.saveDraws(history));deadline=TargetIssueResolver.predrawDeadlineWallR20261008(store.getLatestDraw());
            assertEquals("0",store.getState(RealAuthoritySU2.USER_SWITCH,"0"));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){for(int i=0;i<100;i++)out.writeFloat(.1f);}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R30_CERTIFICATE_AND_FORECAST",w->{
                long now=System.currentTimeMillis();String key=genome.key();
                w.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'synthetic','F15','SEED','R30_SYNTHETIC',?,'LEAGUE',?)",new Object[]{key,genome.text(),seq-1,now});
                w.execSQL("INSERT INTO su2_mainline_choice VALUES(?,?,1,0,'R30_SYNTHETIC',?,?)",new Object[]{seq,"SU2:"+key,seq-1,now});
                w.execSQL("INSERT INTO su2_certificate(certificate_id,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at) VALUES(?,?,?,1,70000,1000,400,.35,.2,.3,1,.001,1,0,.03,?,?)",new Object[]{certificate,key,HashingSU1.text(genome.text()),now-1000,now+3600000});
                w.execSQL("INSERT INTO r26_certificate_protocol VALUES(?,'R26_PREREGISTERED_AGGREGATE_LCB_V3',.02,?)",new Object[]{certificate,HashingSU1.text("SYNTHETIC_NOT_NATURAL_QUALITY")});
                w.execSQL("INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time) VALUES(?,?,1,7,1,.5,.4,?,?)",new Object[]{key,seq,now,deadline});
                w.execSQL("INSERT INTO r26_frozen VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{key,seq,seq-1,HashingSU1.text("SYNTHETIC_CHECKPOINT"),HashingSU1.text("SYNTHETIC_STATE"),HashingSU1.text("SYNTHETIC_SOURCE"),EventFrameBuilderSU1.generation(w),now,deadline,bytes.toByteArray()});return 0;
            });
            store.setState(RealAuthoritySU2.USER_SWITCH,"1");assertTrue(RealAuthoritySU2.allows(store,candidate(),certificate,target,deadline));
        }
        String candidate(){return CertificateSU2.PREFIX+genome.key();}
        @Override public void close(){store.close();remove(app.root);}
    }
    static void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);if(file.exists()&&!file.delete())throw new IllegalStateException("synthetic cleanup");}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}

    @Test public void originalOwnerPositiveAndIndependentNegativeAuthorities()throws Exception {
        // Each independent counterexample starts with an allowed original lane and no previous REAL.
        for(String mode:Arrays.asList("expired","missing-freeze","existing-real","user-off"))try(Fixture f=new Fixture()){
            assertEquals(0,count(f.db,"SELECT count(*) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE decision_action='BET'"));
            if(mode.equals("user-off"))f.store.setState(RealAuthoritySU2.USER_SWITCH,"0");
            else f.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R30_"+mode,w->{
                if(mode.equals("expired"))w.execSQL("UPDATE su2_certificate SET expires_at=issued_at+1 WHERE certificate_id=?",new Object[]{f.certificate});
                // Loss injection affects only this disposable synthetic database.
                if(mode.equals("missing-freeze"))w.execSQL("DELETE FROM r26_frozen WHERE policy_hash=?",new Object[]{f.genome.key()});
                if(mode.equals("existing-real"))w.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,bundle_hash,contract_hash,authorization_hash,record_hash,created_at) VALUES(?,'Binance1-PK10',?,1,1,2,3,'SYNTHETIC','BET','NONE','synthetic','synthetic','synthetic',?,?)",new Object[]{f.target,f.base,HashingSU1.text("SYNTHETIC_EXISTING_REAL"),System.currentTimeMillis()});return 0;
            });
            assertFalse(mode,RealAuthoritySU2.allows(f.store,f.candidate(),f.certificate,f.target,f.deadline));
            System.out.println("R30_ANDROID_INDEPENDENT_AUTHORITY_REJECT|"+mode+"|SYNTHETIC");
        }
        try(Fixture f=new Fixture()) {
            String identity=BuildIdentity.verifyAndPersist(f.app,f.store);assertTrue(identity,BuildIdentity.isBetCompatible(identity));
            AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(f.store);AnalysisSingleFlightR36.Request request=requests.ensurePending(f.base,f.target,f.deadline,"R30_SYNTHETIC_LEGAL_POSITIVE");
            AnalysisSingleFlightR36.SubmitResult submit=AnalysisSingleFlightR36.shared().submit(request);assertTrue(submit.startNow);AnalysisSingleFlightR36.Lease lease=submit.lease;assertNotNull(lease);lease.bindCurrentThread();
            try {
                request=requests.claim(request,lease.ownerId,System.currentTimeMillis());assertNotNull(request);V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(f.history,request);request=requests.bindHistoryHash(request,input.historyHash,lease.ownerId);
                V51RuntimeOrchestrator.OutcomeR36 outcome=V51AutoFetchBridge.runR36(f.store,input,request,lease);
                assertTrue(outcome.runtime.success);assertTrue(outcome.commit.success);assertEquals(V51Enums.DecisionAction.BET,outcome.runtime.authorized.action);
                assertEquals(1,count(f.db,"SELECT count(*) FROM r30_decision_explain WHERE original_allow=1 AND explain_allow=1"));assertEquals(0,count(f.db,"SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_MORE_PERMISSIVE'"));
                assertFalse("Canonical REAL must close the target",RealAuthoritySU2.allows(f.store,f.candidate(),f.certificate,f.target,f.deadline));
                V51Models.AuthorizedDecision duplicate=new DecisionGateEngine().decideV51B12(f.store,outcome.runtime.contract);assertNotEquals(V51Enums.DecisionAction.BET,duplicate.action);
                System.out.println("R30_ANDROID_ORIGINAL_OWNER_POSITIVE|1200_ACTIONS|CERTIFICATE_FROZEN_MAINLINE_REAL|EXPLAIN_MATCH|CANONICAL_DUPLICATE_REJECTED|SYNTHETIC_NOT_NATURAL_OR_PHYSICAL_QUALITY|PASS");
            }finally{AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
        }
    }
}
