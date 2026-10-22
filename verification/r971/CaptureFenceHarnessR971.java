package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.lang.reflect.*;
import java.nio.file.*;
/** Tests the actual precomputed-capture commit fence; reflection is confined to this host test. */
public final class CaptureFenceHarnessR971 {
 static int checks;static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static String get(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
 public static void main(String[] a)throws Exception{
  Path copy=Paths.get(a[2]);Files.copy(Paths.get(a[1]),copy,StandardCopyOption.REPLACE_EXISTING);
  SQLiteDatabase db=new SQLiteDatabase(a[0],copy.toString());
  try{
   String id=get(db,"SELECT candidate_id FROM r952_candidate ORDER BY candidate_id LIMIT 1");
   CandidateAstR952 ast=CandidateAstR952.parse(get(db,"SELECT ast FROM r952_candidate WHERE candidate_id='"+id+"'"));
   Method load=ResearchDirectorR952.class.getDeclaredMethod("loadModel",SQLiteDatabase.class,String.class,String.class);load.setAccessible(true);
   CandidateCompilerR952.Model model=(CandidateCompilerR952.Model)load.invoke(null,db,id,"202609211439");
   Class<?> pc=Class.forName("com.ethk3.novaonepro.r952.ResearchDirectorR952$PreparedCaptureR971");
   Constructor<?> pred=CandidateSandboxR952.Prediction.class.getDeclaredConstructor(int.class,String.class);pred.setAccessible(true);
   Constructor<?> ctor=pc.getDeclaredConstructors()[0];ctor.setAccessible(true);
   CandidateCompilerR952.Program program=CandidateCompilerR952.compile(ast);
   Object p=ctor.newInstance(id,ast,model,program.codeHash,"data",pred.newInstance(7,"captured-features"),"input");
   Method fence=ResearchDirectorR952.class.getDeclaredMethod("captureStillCurrentR971",SQLiteDatabase.class,pc,String.class);fence.setAccessible(true);
   boolean rejected=false;try{fence.invoke(null,db,p,"202609211439");}catch(InvocationTargetException e){rejected=e.getCause() instanceof IllegalStateException;}
   check(rejected,"CAPTURE_CAS_REQUIRES_OWNER_TRANSACTION");
   db.beginTransaction();try{
    check((Boolean)fence.invoke(null,db,p,"202609211439"),"UNCHANGED_ACTUAL_REPORT_MODEL_AST_ACCEPTED");
    db.execSQL("UPDATE r952_candidate SET state='QUARANTINED' WHERE candidate_id=?",new Object[]{id});
    check(!(Boolean)fence.invoke(null,db,p,"202609211439"),"QUARANTINED_DURING_COMPUTE_NOT_PUBLISHED");
   }finally{db.endTransaction();}
   db.beginTransaction();try{
    db.execSQL("UPDATE r952_candidate SET ast=ast||'tampered' WHERE candidate_id=?",new Object[]{id});
    check(!(Boolean)fence.invoke(null,db,p,"202609211439"),"AST_CHANGED_DURING_COMPUTE_NOT_PUBLISHED");
   }finally{db.endTransaction();}
   db.beginTransaction();try{
    CandidateCompilerR952.Model replacement=CandidateCompilerR952.Model.initial();
    db.execSQL("UPDATE r952_artifact SET model_payload=?,payload_hash=? WHERE artifact_hash=(SELECT artifact_hash FROM r952_learning_cursor WHERE consumer_id=?)",new Object[]{replacement.payload(),replacement.hash(),"R952:"+id});
    check(!(Boolean)fence.invoke(null,db,p,"202609211439"),"MODEL_CHANGED_DURING_COMPUTE_NOT_PUBLISHED");
   }finally{db.endTransaction();}
   db.beginTransaction();try{check((Boolean)fence.invoke(null,db,p,"202609211439"),"ORIGINAL_MODEL_RESTORED_AFTER_ROLLBACK");}finally{db.endTransaction();}
   System.out.println("R971_CAPTURE_CAS_PASS checks="+checks+"; physical_concurrent_phone_execution=NOT_RUN");
  }finally{db.shutdown();}
 }
}
