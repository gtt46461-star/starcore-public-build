package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;import java.nio.file.*;
import com.ethk3.novaonepro.r952.*;
/** Eight actual developed/frozen candidates, one PIT read per public callback. */
public final class ReservedBatchR78 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Object mutable(Object v){if(v instanceof Map){Map<String,Object> n=new LinkedHashMap<>();for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())n.put((String)e.getKey(),mutable(e.getValue()));return n;}if(v instanceof List){List<Object> n=new ArrayList<>();for(Object x:(List<?>)v)n.add(mutable(x));return n;}return v;}
 static final class Db extends SQLiteDatabase {
  int pitReads;
  Db(String a,String b)throws Exception{super(a,b);}
  @Override public synchronized Cursor rawQuery(String sql,String[]a){if(sql.contains("FROM draws WHERE dirty=0 AND issue<?")&&sql.contains("open_time,fetch_time"))pitReads++;return super.rawQuery(sql,a);}
 }
 public static void main(String[]a)throws Exception{
  try(Db d=new Db(a[0],a[1])){
   DbStore s=HostProductR974.store(d);RealtimePressureGateR811.resetForTests();
   String original=Files.readString(Path.of(System.getProperty("starcore.test.ast")));
   Path variant=Path.of(a[1]+".variant.ast");
   for(int i=0;i<8;i++){
    Map<String,Object> root=(Map<String,Object>)mutable(StrictCanonicalJson.readObject(original.substring(5)));
    Map<String,Object> node=(Map<String,Object>)((List<?>)root.get("nodes")).get(1);
    node.put("parameters",Collections.singletonList(Double.toHexString(2.0+i/100.0)));
    ((Map<String,Object>)root.get("featureManifest")).put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));
    Files.writeString(variant,"AST3:"+StrictCanonicalJson.write(root));System.setProperty("starcore.test.ast",variant.toString());
    MainlineCaptureR75.seed(d,"batch-frozen-"+i,false);
   }
   long now=System.currentTimeMillis();d.beginTransaction();try{for(int i=342;i<=441;i++)MainlineCaptureR75.draw(d,i,now-1000L-(441-i)*60000L);d.setTransactionSuccessful();}finally{d.endTransaction();}
   String base=MainlineCaptureR75.issue(441),target=MainlineCaptureR75.issue(442),bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
   DeferredEvidenceSnapshotR36.Snapshot snap=MainlineCaptureR75.snapshot(base,target,bundle);
   d.beginTransaction();try{d.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snap.recordHash,selector,MainlineCaptureR75.h("batch-champion"),System.currentTimeMillis()});ok(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(d,target,bundle,selector)==8,"all eight admitted");d.setTransactionSuccessful();}finally{d.endTransaction();}
   long memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();int calls=0,reads=0;
   for(;calls<8&&MainlineCaptureR75.n(d,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)<8;calls++){
    int before=d.pitReads;try{FrozenCandidateAdapterR958.captureReservedCP12(s,snap);}catch(ResearchWorkBudgetR927.Yield transientBudget){}
    int used=d.pitReads-before;reads+=used;ok(used==1,"one immutable PIT per callback: "+used);ok(RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()==memory,"memory returned after callback");
   }
   ok(MainlineCaptureR75.n(d,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)==8,"all eight actual frozen predictions");
   ok(MainlineCaptureR75.n(d,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==8,"all eight actual CAL captures");
   ok(FrozenCandidateAdapterR958.captureReservedCP12(s,snap)==0,"repeat idempotent");
   ok(MainlineCaptureR75.n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0,"no manufactured certificate");
   System.out.println("PASS ReservedBatchR78 checks="+checks+" captured=8 PIT_reads="+reads+" callbacks="+calls+" memory_restored=1");
  }
 }
}
