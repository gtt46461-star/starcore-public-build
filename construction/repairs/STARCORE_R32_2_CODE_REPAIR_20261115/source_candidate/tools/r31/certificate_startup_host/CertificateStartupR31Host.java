package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.nio.file.*;
import java.util.*;

/** P01 reproducer for SCHEMA_PENDING:R31_CERTIFICATE_REVOCATION:UNTRUSTED_CRITICAL on native SQLite (not ART/device).
 *  Real Critical predecessors -> production Critical stamp -> production startup migrations -> the Android
 *  R31RuntimeProtocolInstrumentedTest.open() sequence; Critical trust is printed after every step. */
public final class CertificateStartupR31Host {
    static Map<String,String> critical(SQLiteDatabase db){
        Set<String> names=new HashSet<>(Arrays.asList(SchemaDomainCoordinatorR926.criticalContract().objects));Map<String,String> out=new TreeMap<>();
        try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%'",null)){while(c.moveToNext())if(names.contains(c.getString(1))||names.contains(c.getString(2)))out.put(c.getString(0)+":"+c.getString(1),String.valueOf(c.getString(3)));}
        return out;
    }
    static void reason(SQLiteDatabase db,String stage){System.out.println("CRITICAL_TRUST|"+stage+"|"+SchemaDomainCoordinatorR926.matchReason(db,SchemaDomainCoordinatorR926.criticalContract()));}
    interface Body{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Body body)throws Exception{db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void diff(Map<String,String> before,Map<String,String> after){
        Set<String> keys=new TreeSet<>(before.keySet());keys.addAll(after.keySet());int n=0;
        for(String k:keys){String a=before.get(k),b=after.get(k);if(!Objects.equals(a,b)){n++;System.out.println("CRITICAL_DIFF|"+k+"|"+(a==null?"ADDED":b==null?"REMOVED":"CHANGED")+"|before="+(a==null?"":a.substring(0,Math.min(a.length(),300)))+"|after="+(b==null?"":b.substring(0,Math.min(b.length(),300))));}}
        System.out.println("CRITICAL_DIFF_COUNT|"+n);
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");MainChainSchemaR31Host.Owner owner=new MainChainSchemaR31Host.Owner(Paths.get(args[0]));SQLiteDatabase db=owner.db;
        try{
            MainChainSchemaR31Host.createLegacyBase(owner);
            tx(db,()->{SchemaDomainCoordinatorR926.ensureInfrastructure(db);SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.criticalContract());});
            reason(db,"AFTER_PRODUCTION_CRITICAL_STAMP");final Map<String,String> stamped=critical(db);
            tx(db,()->SchemaDomainCoordinatorR926.migrateR31TransportTiming(db));reason(db,"AFTER_TRANSPORT_TIMING");
            tx(db,()->SchemaDomainCoordinatorR926.migrateR31PlatformPrerequisites(db));reason(db,"AFTER_R929_PLATFORM_PREREQUISITES");
            tx(db,()->RuntimeSchemaR26.ensure(db));reason(db,"AFTER_RUNTIME_SCHEMA_R26");
            String order=args.length>1?args[1]:"TEST_OPEN";
            if(order.startsWith("NEG_")){
                if(order.equals("NEG_TAMPERED_SU1_TRIGGER"))tx(db,()->db.execSQL("CREATE TRIGGER su1_draw_insert_clock AFTER INSERT ON draws BEGIN SELECT 1; END"));
                if(order.equals("NEG_FOREIGN_DRAW_TRIGGER"))tx(db,()->db.execSQL("CREATE TRIGGER foreign_unreviewed_draw AFTER INSERT ON draws BEGIN SELECT 1; END"));
                reason(db,"AFTER_INJECTED_"+order);
                try{int guard=0;while(!StrategySchemaSU1.advance(db))if(++guard>SqlStatementsSU1.SQL.length+16)throw new AssertionError("SU1_NOT_CONVERGED");System.out.println("NEGATIVE_NOT_REFUSED|"+order);}
                catch(RuntimeException refused){System.out.println("NEGATIVE_REFUSED|"+order+"|"+refused.getMessage());}
                reason(db,"AFTER_NEGATIVE");return;
            }
            if(order.equals("SU_BEFORE_R27")){{int guard=0;while(!StrategySchemaSU1.advance(db))if(++guard>SqlStatementsSU1.SQL.length+16)throw new AssertionError("SU1_NOT_CONVERGED");}reason(db,"AFTER_PRODUCTION_SU1_ADVANCE");tx(db,()->StrategySchemaSU2.ensure(db));reason(db,"AFTER_SU2_SCHEMA");}
            try{tx(db,()->{try{SchemaR27.ensure(db);}catch(RuntimeException failure){System.out.println("SCHEMA_R27_FAILURE|"+failure);reason(db,"INSIDE_FAILED_SCHEMA_R27");diff(stamped,critical(db));throw failure;}});System.out.println("SCHEMA_R27|PASS");}
            catch(RuntimeException expected){System.out.println("REPRODUCED|"+expected.getMessage());}
            reason(db,"AFTER_SCHEMA_R27");
            if(order.equals("SU_AFTER_R27")){
                try{{int guard=0;while(!StrategySchemaSU1.advance(db))if(++guard>SqlStatementsSU1.SQL.length+16)throw new AssertionError("SU1_NOT_CONVERGED");}reason(db,"AFTER_PRODUCTION_SU1_ADVANCE");
                    tx(db,()->{try{StrategySchemaSU2.ensure(db);}catch(RuntimeException failure){System.out.println("SU2_FAILURE|"+failure);diff(stamped,critical(db));throw failure;}});reason(db,"AFTER_SU2_SCHEMA");
                    tx(db,()->{try{SchemaR27.ensure(db);}catch(RuntimeException failure){System.out.println("SCHEMA_R27_SECOND_FAILURE|"+failure);diff(stamped,critical(db));throw failure;}});System.out.println("SCHEMA_R27_SECOND|PASS");reason(db,"AFTER_SECOND_SCHEMA_R27");}
                catch(RuntimeException expected){System.out.println("REPRODUCED|"+expected.getMessage());}
            }
        }finally{db.close();}
    }
}
