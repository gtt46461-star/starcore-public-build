package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
/** Uses only a private in-memory database, never the application's retained fixture. */
@RunWith(AndroidJUnit4.class)
public final class SchemaFingerprintAndroidTest {
 static String s(String x){return x==null?"":x.trim();}
 static String original(SQLiteDatabase db,SchemaDomainCoordinatorR926.Contract ct){Set<String>w=new HashSet<>(Arrays.asList(ct.objects));ArrayList<String>rs=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name",null)){while(c.moveToNext()){String n=s(c.getString(1)),t=s(c.getString(2));if(w.contains(n)||w.contains(t))rs.add(s(c.getString(0))+"|"+n+"|"+t+"|"+s(c.getString(3)));}}Collections.sort(rs);StringBuilder b=new StringBuilder(ct.domain).append('|').append(ct.revision).append('|').append(ct.contractHash);for(String r:rs)b.append('\n').append(r.length()).append(':').append(r);return V51ChecksumEngine.sha256(b.toString());}

 interface FingerprintCall { String run(); }
 static android.database.sqlite.SQLiteException expectBlobFailure(FingerprintCall call) {
  try { call.run(); }
  catch (android.database.sqlite.SQLiteException failure) {
   assertTrue("Expected Android CursorWindow BLOB conversion failure", failure.getMessage()!=null
           && failure.getMessage().contains("Unable to convert BLOB to string"));
   return failure;
  }
  throw new AssertionError("Expected Android CursorWindow to reject BLOB getString");
 }
 static void assertSameFailure(Throwable original, Throwable actual) {
  while (original != null && actual != null) {
   assertEquals(original.getClass(), actual.getClass());
   assertEquals(original.getMessage(), actual.getMessage());
   original=original.getCause(); actual=actual.getCause();
  }
  assertNull(original); assertNull(actual);
 }
 @Test public void boundCatalogPredicateMatchesOriginalOnAndroid() {
  try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
   String chars="char(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32)";
   try(Cursor c=db.rawQuery("SELECT sqlite_version(),trim(char(9)||'quoted'||char(10),"+chars+"),instr('quoted'||char(0),char(0)), 'Quoted' COLLATE BINARY IN (?)",new String[]{"quoted"})) {assertTrue(c.moveToFirst());android.util.Log.i("SchemaFingerprintR80","SQLITE="+c.getString(0));assertEquals("quoted",c.getString(1));assertEquals(7,c.getInt(2));assertEquals(0,c.getInt(3));}
   for(String n:new String[]{"quoted"," quoted ","\tquoted\n","CASEONLY","雪","\u00a0quoted\u00a0"})db.execSQL("CREATE TABLE \""+n+"\"(x TEXT)");
   SchemaDomainCoordinatorR926.Contract ascii=new SchemaDomainCoordinatorR926.Contract("EDGE",1,new String[]{"quoted","caseonly","absent"});
   for(SchemaDomainCoordinatorR926.Contract c:new SchemaDomainCoordinatorR926.Contract[]{ascii,SchemaDomainCoordinatorR926.criticalContract(),SchemaDomainCoordinatorR926.researchContract(),SchemaDomainCoordinatorR926.legacyContract(),new SchemaDomainCoordinatorR926.Contract("UNICODE",1,new String[]{"雪"})})assertEquals(original(db,c),SchemaDomainCoordinatorR926.fingerprint(db,c));
   db.beginTransaction();try {
    db.execSQL("PRAGMA writable_schema=ON");
    for(String edge:new String[]{"\u0000quoted","quoted\u0000","\u0000 quoted \u0000","\u001fquoted\u0020","\u2003quoted\u2003"}) {db.execSQL("UPDATE sqlite_master SET tbl_name=? WHERE name='CASEONLY'",new Object[]{edge});assertEquals(original(db,ascii),SchemaDomainCoordinatorR926.fingerprint(db,ascii));}
    db.execSQL("UPDATE sqlite_master SET tbl_name=CAST('quoted' AS BLOB),sql=NULL WHERE name='CASEONLY'");
    // Android CursorWindow rejects BLOB getString; the host bridge stringifies it.
    // Preserve the original failure rather than treating this corrupt row as hashable.
    assertSameFailure(expectBlobFailure(() -> original(db,ascii)),
            expectBlobFailure(() -> SchemaDomainCoordinatorR926.fingerprint(db,ascii)));
    db.execSQL("UPDATE sqlite_master SET tbl_name=NULL WHERE name='CASEONLY'");assertEquals(original(db,ascii),SchemaDomainCoordinatorR926.fingerprint(db,ascii));
   }finally{db.execSQL("PRAGMA writable_schema=OFF");db.endTransaction();}
   String[] names=new String[499];for(int i=0;i<names.length;i++)names[i]="missing"+i;names[0]="quoted";SchemaDomainCoordinatorR926.Contract boundary=new SchemaDomainCoordinatorR926.Contract("BINDS998",1,names);assertEquals(original(db,boundary),SchemaDomainCoordinatorR926.fingerprint(db,boundary));
   android.util.Log.i("SchemaFingerprintR80","PARITY_PASS original_hashes_and_998_binds");
  }
 }
}
