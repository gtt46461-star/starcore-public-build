package com.ethk3.novaonepro;
import java.io.*;
/** Real vendored JNI against the product's complete DDL fixture, not a tiny substitute schema. */
public final class ProductionSchemaBackupR992 {
 public static void main(String[] args)throws Exception {
  ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
  File source=new File(args[0]),root=new File(args[1]);root.mkdirs();
  for(int mib:new int[]{4,16,32,64}){
   File target=new File(root,"backup-"+mib+".db");if(target.exists())target.delete();
   long started=System.nanoTime();
   try{ReportNativeCP4.backup(source,target,(long)mib<<20,()->true);
    long peak;try(ReportNativeCP4.RowReader r=ReportNativeCP4.openRows(target,(long)mib<<20,()->true)){peak=r.nativePeakBytes();}
    System.out.println("{\"mib\":"+mib+",\"status\":\"PASS\",\"bytes\":"+target.length()+",\"peakBytes\":"+peak+",\"ms\":"+(System.nanoTime()-started)/1000000+"}");
   }catch(ReportNativeCP4.Failure e){System.out.println("{\"mib\":"+mib+",\"status\":\"FAIL\",\"code\":"+e.nativeCode+",\"stage\":\""+e.stage+"\"}");}
  }
 }
}
