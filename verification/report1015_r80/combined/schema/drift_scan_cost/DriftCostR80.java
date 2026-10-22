package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.*;
public final class DriftCostR80{
 static class CountDb extends SQLiteDatabase{
  int queries,ddl,masterRows;long sqlChars;
  CountDb(String b,String p)throws Exception{super(b,p);}
  void reset(){queries=ddl=masterRows=0;sqlChars=0;}
  @Override public Cursor rawQuery(String sql,String[] args){queries++;Cursor c=super.rawQuery(sql,args);if(!sql.startsWith("SELECT type,name,tbl_name"))return c;return (Cursor)Proxy.newProxyInstance(Cursor.class.getClassLoader(),new Class[]{Cursor.class},(o,m,a)->{Object r;try{r=m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}if(m.getName().equals("moveToNext")&&Boolean.TRUE.equals(r)){masterRows++;sqlChars+=c.getString(3).length();}return r;});}
  @Override public void execSQL(String q){ddl++;super.execSQL(q);}
  @Override public void execSQL(String q,Object[] a){ddl++;super.execSQL(q,a);}
 }
 static boolean guardedCandidate(SQLiteDatabase db,SchemaDomainCoordinatorR926.Contract contract){
  SchemaDomainCoordinatorR926.ensureInfrastructure(db);
  try(Cursor c=db.rawQuery("SELECT revision,contract_hash,fingerprint,state FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain=?",new String[]{contract.domain})){
   if(!c.moveToFirst()||c.getInt(0)!=contract.revision)return false;
   String state=c.getString(3)==null?"":c.getString(3).trim();if(!"READY".equals(state)&&!"FAILED".equals(state))return false;
  }
  return SchemaDomainCoordinatorR926.prepareRepairIfDrift(db,contract);
 }
 static String snapshot(SQLiteDatabase db){StringBuilder b=new StringBuilder();for(String table:new String[]{SchemaDomainCoordinatorR926.TABLE_DOMAIN,SchemaDomainCoordinatorR926.TABLE_JOURNAL})try(Cursor c=db.rawQuery("SELECT * FROM "+table+" ORDER BY 1,2,3",null)){while(c.moveToNext())for(int i=0;i<c.getColumnCount();i++)b.append(c.getString(i)).append('|');}return b.toString();}
 public static void main(String[] a)throws Exception{try(CountDb db=new CountDb(a[0],a[1])){
  SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.researchContract();System.out.println("CONTRACT_OBJECTS="+contract.objects.length);
  for(boolean partial:new boolean[]{false,true})for(String state:new String[]{"INITIALIZING","WAITING_FOR_BOUNDED_SLOT","REPAIR_REQUIRED","NOT_STARTED","","READY","FAILED"}){
   db.beginTransaction();try{
    if(partial){db.execSQL("DROP TRIGGER r969_run_no_replace");db.execSQL("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id='R952_EXTENSION' OR step_id LIKE 'R64_CP8_DDL_%'");}
    db.execSQL("UPDATE v57_schema_domain_r926 SET state=?,contract_hash='changed-even-if-in-progress',fingerprint='intermediate' WHERE domain='RESEARCH'",new Object[]{state});String before=snapshot(db);
    db.reset();long t=System.nanoTime();boolean actual=SchemaDomainCoordinatorR926.prepareRepairIfDrift(db,contract);long us=(System.nanoTime()-t)/1000;int q=db.queries,ddl=db.ddl,rows=db.masterRows;long chars=db.sqlChars;
    boolean changed=!before.equals(snapshot(db));System.out.println("ACTUAL partial="+partial+" state="+state+" result="+actual+" changed="+changed+" queries="+q+" exec="+ddl+" masterRows="+rows+" sqlChars="+chars+" micros="+us);
    if(!state.equals("READY")&&!state.equals("FAILED")){
     if(actual||changed)throw new AssertionError("noncandidate mutated or returned true");
     db.reset();t=System.nanoTime();boolean candidate=guardedCandidate(db,contract);us=(System.nanoTime()-t)/1000;
     System.out.println("CANDIDATE partial="+partial+" state="+state+" result="+candidate+" queries="+db.queries+" exec="+db.ddl+" masterRows="+db.masterRows+" sqlChars="+db.sqlChars+" micros="+us);if(candidate||!before.equals(snapshot(db)))throw new AssertionError("guard behavior mismatch");
    }else if(!actual)throw new AssertionError("eligible contract drift unexpectedly ignored");
   }finally{db.endTransaction();}
  }
 }}
}
