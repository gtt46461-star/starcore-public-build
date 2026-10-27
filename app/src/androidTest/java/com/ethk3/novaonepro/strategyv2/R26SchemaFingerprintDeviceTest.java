package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** The complete original catalogue hash is the independent reference; no app database is opened. */
@RunWith(AndroidJUnit4.class)
public final class R26SchemaFingerprintDeviceTest {
    private static String safe(String value){return value==null?"":value.trim();}
    private static String original(SQLiteDatabase db,SchemaDomainCoordinatorR926.Contract contract){
        Set<String> wanted=new HashSet<>(Arrays.asList(contract.objects));List<String> rows=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name",null)){
            while(c.moveToNext()){String name=safe(c.getString(1)),table=safe(c.getString(2));if(wanted.contains(name)||wanted.contains(table))rows.add(safe(c.getString(0))+'|'+name+'|'+table+'|'+safe(c.getString(3)));}
        }
        Collections.sort(rows);StringBuilder all=new StringBuilder(contract.domain).append('|').append(contract.revision).append('|').append(contract.contractHash);for(String row:rows)all.append('\n').append(row.length()).append(':').append(row);return V51ChecksumEngine.sha256(all.toString());
    }
    private static void parity(SQLiteDatabase db,SchemaDomainCoordinatorR926.Contract contract){
        String expected;try{expected=original(db,contract);}catch(RuntimeException invalid){
            try{SchemaDomainCoordinatorR926.fingerprint(db,contract);fail("Prefilter hid an invalid catalogue value");}catch(RuntimeException same){assertEquals(invalid.getClass(),same.getClass());}return;
        }
        assertEquals(expected,SchemaDomainCoordinatorR926.fingerprint(db,contract));
    }
    private static void membershipParity(SQLiteDatabase db,SchemaDomainCoordinatorR926.Contract contract){
        List<String> missing=new ArrayList<>();for(String name:contract.objects)try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=? LIMIT 1",new String[]{name})){if(!c.moveToFirst())missing.add(name);}
        try{SchemaDomainCoordinatorR926.verifyObjects(db,contract);assertTrue("Required objects were silently omitted",missing.isEmpty());}
        catch(IllegalStateException failure){assertEquals("R926_"+contract.domain+"_OBJECTS_MISSING:"+String.join(",",missing),failure.getMessage());}
    }
    @Test public void cataloguePrefilterRetainsOriginalBytesAndDetectsSameVersionChanges(){
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            SchemaDomainCoordinatorR926.Contract c=SchemaDomainCoordinatorR926.criticalContract();
            db.execSQL("CREATE TABLE draws(value TEXT)");db.execSQL("CREATE TABLE external(value TEXT)");db.execSQL("CREATE TABLE \" draws \"(value TEXT)");db.execSQL("CREATE TABLE \"\tdraws\n\"(value TEXT)");parity(db,c);membershipParity(db,c);
            String before=SchemaDomainCoordinatorR926.fingerprint(db,c);db.execSQL("CREATE INDEX extra_draw_index ON draws(value)");parity(db,c);assertNotEquals(before,SchemaDomainCoordinatorR926.fingerprint(db,c));db.execSQL("DROP INDEX extra_draw_index");assertEquals(before,SchemaDomainCoordinatorR926.fingerprint(db,c));
            db.beginTransaction();try{
                db.execSQL("PRAGMA writable_schema=ON");long version;try(Cursor v=db.rawQuery("PRAGMA schema_version",null)){v.moveToFirst();version=v.getLong(0);}
                for(String edge:new String[]{"draws"," draws ","\tdraws\n",String.valueOf((char)0)+"draws","draws"+(char)0,"\u001fdraws\u0020","DRAWS","draws\u00a0","雪"}){
                    db.execSQL("UPDATE sqlite_master SET tbl_name=? WHERE name='external'",new Object[]{edge});parity(db,c);
                }
                db.execSQL("UPDATE sqlite_master SET tbl_name=CAST('draws' AS BLOB),sql=NULL WHERE name='external'");parity(db,c);
                db.execSQL("UPDATE sqlite_master SET tbl_name=NULL WHERE name='external'");parity(db,c);
                db.execSQL("UPDATE sqlite_master SET tbl_name='draws',sql='CHANGED CATALOGUE SQL' WHERE name='external'");parity(db,c);assertNotEquals(before,SchemaDomainCoordinatorR926.fingerprint(db,c));
                try(Cursor v=db.rawQuery("PRAGMA schema_version",null)){v.moveToFirst();assertEquals("Same-version edits must not be hidden behind a schema-version cache",version,v.getLong(0));}
            }finally{db.execSQL("PRAGMA writable_schema=OFF");db.endTransaction();}
            parity(db,c);
        }
    }
}
