package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.*;
public final class DriftFastPathR80{
 static class CountDb extends SQLiteDatabase{
  int queries,ddl,masterRows;long sqlChars;
  CountDb(String b,String p)throws Exception{super(b,p);}
  void reset(){queries=ddl=masterRows=0;sqlChars=0;}
  @Override public Cursor rawQuery(String sql,String[] args){queries++;Cursor c=super.rawQuery(sql,args);if(!sql.startsWith("SELECT type,name,tbl_name"))return c;return (Cursor)Proxy.newProxyInstance(Cursor.class.getClassLoader(),new Class[]{Cursor.class},(o,m,a)->{Object r;try{r=m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}if(m.getName().equals("moveToNext")&&Boolean.TRUE.equals(r)){masterRows++;sqlChars+=c.getString(3).length();}return r;});}
  @Override public void execSQL(String q){ddl++;super.execSQL(q);}
  @Override public void execSQL(String q,Object[] a){ddl++;super.execSQL(q,a);}
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
     if(q!=1||ddl!=3||rows!=0||chars!=0)throw new AssertionError("intermediate state still scans schema");
    }else if(!actual)throw new AssertionError("eligible contract drift unexpectedly ignored");
   }finally{db.endTransaction();}
  }
  for(String control:new String[]{"READY_VALID","FAILED_VALID","FAILED_INCOMPLETE","REVISION_CHANGED","NO_DOMAIN"}){
   db.beginTransaction();try{
    if(control.equals("FAILED_VALID"))db.execSQL("UPDATE v57_schema_domain_r926 SET state='FAILED' WHERE domain='RESEARCH'");
    if(control.equals("FAILED_INCOMPLETE")){db.execSQL("UPDATE v57_schema_domain_r926 SET state='FAILED',contract_hash='changed' WHERE domain='RESEARCH'");db.execSQL("UPDATE v57_schema_migration_journal_r926 SET status='RUNNING' WHERE step_id='R811_RESEARCH'");}
    if(control.equals("REVISION_CHANGED"))db.execSQL("UPDATE v57_schema_domain_r926 SET revision=revision+1 WHERE domain='RESEARCH'");
    if(control.equals("NO_DOMAIN"))db.execSQL("DELETE FROM v57_schema_domain_r926 WHERE domain='RESEARCH'");
    String before=snapshot(db);db.reset();boolean result=SchemaDomainCoordinatorR926.prepareRepairIfDrift(db,contract);int count=db.queries;
    if(result||!before.equals(snapshot(db)))throw new AssertionError("control changed behavior:"+control);
    if((control.equals("REVISION_CHANGED")||control.equals("NO_DOMAIN"))&&count!=1)throw new AssertionError("early boundary queried extra metadata");
    System.out.println("CONTROL "+control+" result=false changed=false queries="+count);
   }finally{db.endTransaction();}
  }
  System.out.println("DRIFT_FAST_PATH_PASS cases=19");
 }}
}
