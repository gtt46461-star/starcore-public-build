package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** Exact old predicate and query-count oracle on real SQLite; no readiness cache. */
public final class ExactCensus {
    static int checks;static void check(boolean condition){checks++;if(!condition)throw new AssertionError();}
    static final class Db extends SQLiteDatabase {int queries;Db(String bridge)throws Exception{super(bridge);}public Cursor rawQuery(String sql,String[] args){if(sql.contains("sqlite_master"))queries++;return super.rawQuery(sql,args);}}
    static boolean old(Db db,Map<String,String> expected){for(Map.Entry<String,String> e:expected.entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})){if(!c.moveToFirst()||!e.getValue().equals(c.getString(0))||c.moveToNext())return false;}return true;}
    static void parity(Db db,Map<String,String> expected,int queryCount){boolean before=old(db,expected);db.queries=0;check(SchemaObjectSnapshotR26.read(db,expected.keySet()).matches(expected)==before);check(db.queries==queryCount);}
    @SuppressWarnings("unchecked") static Map<String,String> ddl(Class<?> type)throws Exception{java.lang.reflect.Field f=type.getDeclaredField("DDL");f.setAccessible(true);return new LinkedHashMap<>((Map<String,String>)f.get(null));}
    public static void main(String[] args)throws Exception{try(Db db=new Db(args[0])){
        Map<String,String> expected=ddl(com.ethk3.novaonepro.r952.LongScaleSummaryR80.class);expected.putAll(ddl(com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.class));
        // Catalogue-only transport: retain every production DDL byte without executing its foreign references.
        db.execSQL("PRAGMA writable_schema=ON");for(Map.Entry<String,String> e:expected.entrySet())db.execSQL("INSERT INTO sqlite_master(type,name,tbl_name,rootpage,sql) VALUES('trigger',?,'host_transport',0,?)",new Object[]{e.getKey(),e.getValue()});
        parity(db,expected,1);check(old(db,expected));String first=expected.keySet().iterator().next();
        db.execSQL("UPDATE sqlite_master SET sql=sql||' ' WHERE name=?",new Object[]{first});parity(db,expected,1);check(!old(db,expected));db.execSQL("UPDATE sqlite_master SET sql=? WHERE name=?",new Object[]{expected.get(first),first});
        db.execSQL("INSERT INTO sqlite_master(type,name,tbl_name,rootpage,sql) VALUES('trigger',?,'host_transport',0,?)",new Object[]{first,expected.get(first)});parity(db,expected,1);check(!old(db,expected));db.execSQL("DELETE FROM sqlite_master WHERE rowid=(SELECT max(rowid) FROM sqlite_master WHERE name=?)",new Object[]{first});
        db.execSQL("DELETE FROM sqlite_master WHERE name=?",new Object[]{first});parity(db,expected,1);check(!old(db,expected));
        for(int n:new int[]{0,1,499,500}){Map<String,String> names=new LinkedHashMap<>();for(int i=0;i<n;i++)names.put("absent"+i,"missing");parity(db,names,n==0?0:n>499?n:1);}
        for(String edge:new String[]{null,""," 雪 ","x\u0000y","q'quote","sample\n"}){Map<String,String> names=new LinkedHashMap<>();names.put(edge,"CREATE TABLE hidden(x)");if(edge!=null)db.execSQL("INSERT INTO sqlite_master(type,name,tbl_name,rootpage,sql) VALUES('table',?,'host_transport',0,?)",new Object[]{edge,names.get(edge)});parity(db,names,1);}
        System.out.println("R26_EXACT_CENSUS_PASS|assertions="+checks+"|productionLongDdlObjects="+expected.size()+"|freshQueries=1|sameVersionSqlBytesAndDuplicatesPreserved=true|scope=HOST_SQLITE_CATALOGUE_ROWS_NOT_DDL_INSTALLATION_OR_DEVICE_PERFORMANCE");
    }}
}
