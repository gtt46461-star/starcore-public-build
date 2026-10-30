package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Real installer and immutable SQL journal. Query counts are HOST evidence, never Android timing. */
public final class ResearchPhaseStampR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_PHASE_STAMP|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){return ResearchSchemaOwnerR29HostTest.n(db,sql);}
    static final Method ADVANCE;
    static {try{ADVANCE=ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance",SQLiteDatabase.class);ADVANCE.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static boolean advance(SQLiteDatabase db)throws Exception{
        final boolean[] ready={false};ResearchSchemaOwnerR29HostTest.tx(db,()->{try{ready[0]=(Boolean)ADVANCE.invoke(null,db);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw(Exception)cause;throw(Error)cause;}});return ready[0];
    }
    static long units(SQLiteDatabase db){return n(db,"SELECT count(*) FROM sqlite_master WHERE name='su1_migration_unit_v2'")==0?0:n(db,"SELECT count(*) FROM su1_migration_unit_v2");}
    static void bootstrap(SQLiteDatabase root,SQLiteDatabase rp)throws Exception{
        ResearchSchemaOwnerR29HostTest.base(root);ResearchSchemaOwnerR29HostTest.base(rp);ResearchSchemaOwnerR29HostTest.rootBaseAndOwnedCopy(root,rp);
        ResearchSchemaOwnerR29HostTest.owner(rp);
        ResearchSchemaOwnerR29HostTest.tx(rp,()->{LegacyLearnedInputR29.installResearch(rp);CriticalInputMirrorR29.initialized(rp,CriticalInputMirrorR29.sourceEpoch(root),CriticalInputMirrorR29.sourceSequence(root),CriticalInputMirrorR29.sourceViewSql(root));});
        for(int i=0;i<1000&&!CriticalInputMirrorR29.canonicalReady(rp);i++){CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,rp);ResearchSchemaOwnerR29HostTest.tx(rp,()->CriticalInputMirrorR29.apply(rp,step));}
        for(int i=0;i<100&&!LegacyLearnedInputR29.ready(rp);i++){LegacyLearnedInputR29.Capture captured=LegacyLearnedInputR29.capture(root,rp);ResearchSchemaOwnerR29HostTest.tx(rp,()->LegacyLearnedInputR29.apply(rp,captured));}
        check("P0_ACTUAL_ROOT_INPUT_BOOTSTRAPS_READY",CriticalInputMirrorR29.canonicalReady(rp)&&LegacyLearnedInputR29.ready(rp));
    }
    static void damagedPendingStage(SQLiteDatabase rp,boolean wrongDefinition)throws Exception{
        long before=units(rp);rp.beginTransaction();try{
            rp.execSQL("DROP TABLE su1_schema_revision");
            if(wrongDefinition)rp.execSQL("CREATE TABLE su1_schema_revision(revision INTEGER PRIMARY KEY,contract_name TEXT)");
            boolean ready=false;for(int i=0;i<SqlStatementsSU1.SQL.length+8&&!ready;i++)ready=advance(rp);
            check(wrongDefinition?"P4_WRONG_OBJECT_NEVER_BECOMES_READY":"P3_MISSING_OBJECT_NEVER_BECOMES_READY",!ready&&!StrategySchemaSU1.ready(rp)&&!SchemaDomainCoordinatorR926.matches(rp,SchemaDomainCoordinatorR926.researchContract(rp)));
        }finally{rp.endTransaction();}
        check(wrongDefinition?"P6_WRONG_OBJECT_ROLLBACK_RESTORES_EXACT_PREFIX":"P5_MISSING_OBJECT_ROLLBACK_RESTORES_EXACT_PREFIX",units(rp)==before&&n(rp,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='su1_schema_revision'")==1);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("r29-phase-stamp-");
        try(SQLiteDatabase root=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("root.db")));SQLiteDatabase rp=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("rp.db")))){
            bootstrap(root,rp);int entered=0;while(units(rp)<1&&entered++<1000){advance(rp);if(ResearchSchemaCoordinatorR926.summary().startsWith("FAILED"))throw new AssertionError(ResearchSchemaCoordinatorR926.summary());}
            check("P1_REAL_INSTALLER_REACHES_FIRST_SU1_UNIT",units(rp)==1);
            long[] fingerprints={0};SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==rp&&sql.startsWith("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master"))fingerprints[0]++;};
            for(int i=0;i<8;i++){long before=units(rp);boolean ready=advance(rp);if(ready||units(rp)!=before+1)throw new AssertionError("REAL_IMMUTABLE_UNIT_REQUIRED");}
            SQLiteDatabase.hostBeforeQuery=null;
            System.out.println("R29_PHASE_STAMP|MEASURED|sameStageUnits=8|fullCatalogueFingerprints="+fingerprints[0]);
            check("P2_SAME_PENDING_STAGE_HAS_NO_REPEATED_FULL_FINGERPRINT",fingerprints[0]==0);
            damagedPendingStage(rp,false);damagedPendingStage(rp,true);
            boolean ready=false;for(int i=0;i<1000&&!ready;i++)ready=advance(rp);
            check("P7_UNMODIFIED_INSTALLER_COMPLETES_REAL_READY",ready&&StrategySchemaSU1.ready(rp)&&SchemaDomainCoordinatorR926.matches(rp,SchemaDomainCoordinatorR926.researchContract(rp)));
            boolean exact=true;int count=0;try(Cursor c=rp.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit_v2 ORDER BY unit",null)){while(c.moveToNext()){exact&=count<SqlStatementsSU1.SQL.length&&c.getInt(0)==count&&c.getString(1).equals(HashingSU1.text(SqlStatementsSU1.SQL[count]));count++;}}
            check("P8_ALL_ORIGINAL_265_UNIT_HASHES_UNCHANGED",exact&&count==SqlStatementsSU1.SQL.length&&count==265);
            long[] verifiedHashes={0};SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==rp&&sql.startsWith("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master"))verifiedHashes[0]++;};
            check("P9_READY_REVALIDATES_CURRENT_FULL_CATALOGUE",advance(rp)&&verifiedHashes[0]>0);SQLiteDatabase.hostBeforeQuery=null;
            ResearchSchemaOwnerR29HostTest.noRootAuthorities(rp);
        }finally{SQLiteDatabase.hostBeforeQuery=null;try(java.util.stream.Stream<Path> paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("R29_PHASE_STAMP|ALL_PASS|checks="+checks+"|ACTUAL_SQLITE_INSTALLER_NO_READY_CACHE_HOST_QUERY_COUNT_NOT_ANDROID_TIMING");
    }
}
