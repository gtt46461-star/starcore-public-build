package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import com.ethk3.novaonepro.r952.*;
/** Actual uploaded quality and network; continuation draws are synthetic. */
public final class ReportedWorldHarnessR964 {
 static int checks;
 static void check(boolean x,String n){if(!x)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static void table(SQLiteDatabase db,JSONObject root,String name)throws Exception{
  JSONObject t=root.getJSONObject(name);JSONArray cols=t.getJSONArray("columns"),rows=t.getJSONArray("rows");StringBuilder columns=new StringBuilder(),args=new StringBuilder();
  for(int i=0;i<cols.length();i++){if(i>0){columns.append(',');args.append(',');}columns.append(cols.getString(i));args.append('?');}
  for(int r=0;r<rows.length();r++){JSONArray row=rows.getJSONArray(r);Object[] values=new Object[cols.length()];for(int i=0;i<values.length;i++){String v=row.getString(i);values[i]=v.startsWith("base64:")?Base64.getDecoder().decode(v.substring(7)):v.equals("\\N")?null:v;}db.execSQL("INSERT INTO "+name+"("+columns+") VALUES("+args+")",values);}
 }
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=DevelopmentTrainingHarnessR959.init(db);DevelopmentTrainingHarnessR959.draw(db,"202609200204",new Random(964));
   JSONObject report=new JSONObject(new String(Files.readAllBytes(Paths.get("verification/r958/reported_r963_world.json")),"UTF-8"));
   table(db,report,AtomicToolWorldR943.QUALITY);table(db,report,AtomicToolWorldR943.MODEL);
   check(count(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.QUALITY+" WHERE state='RESEARCH_PARENT'")==2,"REPORT_HAS_EXACTLY_TWO_MATURE_ATOMS");
   check(AtomicToolWorldR943.network(db).network.updates==429,"REPORT_NETWORK_PAYLOAD_HASH_AND_NUMERIC_UPDATES_VERIFY");
   String status=MolecularToolWorldR947.maintain(store);System.out.println(status);
   check(count(db,"SELECT COUNT(*) FROM r952_world_atom")==2,"ACTUAL_MATURE_REPORT_ATOMS_ENTER_WORLD");
   for(int i=0;i<12&&count(db,"SELECT COUNT(*) FROM r952_world_molecule")==0;i++){DevelopmentTrainingHarnessR959.draw(db,"202609200"+(205+i),new Random(964+i));MolecularToolWorldR947.maintain(store);}
   check(count(db,"SELECT COUNT(*) FROM r952_world_molecule")>0,"BINARY_MOLECULES_WITH_TWO_QUALIFIED_PARENTS");
   final MolecularToolWorldR947.Plan[] plan={null};
   check(store.coordinatorR36().runAtomicWrite("R964_WORLD_HANDOFF",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase d){plan[0]=MolecularToolWorldR947.creationPlan(d,new int[]{6,16,20,40,60,80,100,120,140,160,180,21},964);}}),"MOLECULE_HANDOFF_TRANSACTION");
   check(plan[0]!=null&&plan[0].executable!=null,"REPORT_MOLECULE_ENTERS_NEW_GENESIS_PROGRAM");
   check(count(db,"SELECT COUNT(*) FROM r952_knowledge WHERE kind='ATOM_ADMISSION'")>=2,"ADMISSION_RETAINS_REAL_NUMERIC_QUALITY_EVIDENCE");
   System.out.println("R964_REPORTED_WORLD_PASS|checks="+checks+"|ACTUAL_REPORT_ATOMS_NOT_PHONE_EXECUTION");
  }finally{db.shutdown();}
 }
}
