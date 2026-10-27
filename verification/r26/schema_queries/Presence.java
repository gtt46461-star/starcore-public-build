package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;
public final class Presence {
 static int assertions;static void ok(boolean b){assertions++;if(!b)throw new AssertionError();}
 static class Db extends SQLiteDatabase {int probes;Db(String a,String b)throws Exception{super(a,b);}public Cursor rawQuery(String q,String[]a){if(q.contains("FROM sqlite_master"))probes++;return super.rawQuery(q,a);}}
 static List<String> old(Db db,SchemaDomainCoordinatorR926.Contract c){List<String> m=new ArrayList<>();for(String n:c.objects)try(Cursor r=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=? LIMIT 1",new String[]{n})){if(!r.moveToFirst())m.add(n);}return m;}
 static void check(Db db,SchemaDomainCoordinatorR926.Contract c,int expected){List<String> missing=old(db,c);db.probes=0;try{SchemaDomainCoordinatorR926.verifyObjects(db,c);ok(missing.isEmpty());}catch(IllegalStateException e){ok(e.getMessage().equals("R926_"+c.domain+"_OBJECTS_MISSING:"+String.join(",",missing)));}ok(db.probes==expected);}
 public static void main(String[] a)throws Exception{try(Db db=new Db(a[0],a[1])){
  for(SchemaDomainCoordinatorR926.Contract c:new SchemaDomainCoordinatorR926.Contract[]{SchemaDomainCoordinatorR926.criticalContract(),SchemaDomainCoordinatorR926.researchContract(),SchemaDomainCoordinatorR926.legacyContract()})check(db,c,1);
  for(int n:new int[]{0,1,499,500}){String[] names=new String[n];for(int i=0;i<n;i++)names[i]="missing"+i;if(n>0)names[0]="draws";check(db,new SchemaDomainCoordinatorR926.Contract("BOUND",1,names),n==0?0:n>499?n:1);}
  for(String edge:new String[]{"雪"," draws ","","q-x",String.valueOf((char)0)+"draws"})check(db,new SchemaDomainCoordinatorR926.Contract("EDGE",1,new String[]{"draws",edge}),2);
  SchemaDomainCoordinatorR926.Contract one=new SchemaDomainCoordinatorR926.Contract("PRESENCE",1,new String[]{"draws"});SchemaDomainCoordinatorR926.verifyAndStamp(db,one);ok(SchemaDomainCoordinatorR926.matches(db,one));db.execSQL("CREATE INDEX host_presence_idx ON draws(value)");ok(!SchemaDomainCoordinatorR926.matches(db,one));SchemaDomainCoordinatorR926.verifyAndStamp(db,one);ok(SchemaDomainCoordinatorR926.matches(db,one));db.execSQL("DROP INDEX host_presence_idx");ok(!SchemaDomainCoordinatorR926.matches(db,one));
  System.out.println("R26_SCHEMA_MEMBERSHIP_PASS|assertions="+assertions+"|researchRequiredObjects="+SchemaDomainCoordinatorR926.researchContract().objects.length+"|freshQueries=1|noReadinessCache=true|scope=HOST_SQLITE_WITH_ANDROID_API_ADAPTER");
 }}
}
