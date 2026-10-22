package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.File;import org.json.JSONObject;
public class ReportProfileCompatibilityR80{
 public static void main(String[]args)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){db.makeReadOnly();String hash=ReportArchiveCP4.hash(new File(args[1]));
   for(String profile:new String[]{"COMPLETE_DIAGNOSTIC_R68","COMPACT_DIAGNOSTIC_R64","FULL_RAW_EVIDENCE"}){
    try(ReportReadContextCP4 c=new ReportReadContextCP4(db,"fixture",hash,1,new ReportReadContextCP4.RuntimeObservationCP4("{}"),new ReportReadContextCP4.SourceManifestCP4(new JSONObject().put("reportProfile",profile).toString()),new android.os.CancellationSignal(),3L<<20)){
     String wanted=profile.equals("FULL_RAW_EVIDENCE")?"snapshot.db":"DIAGNOSTIC_SCOPE.json";if(!wanted.equals(c.evidenceMemberR64()))throw new AssertionError("profile="+profile+" expected="+wanted+" actual="+c.evidenceMemberR64());System.out.println("PASS PROFILE "+profile+" -> "+wanted);
    }
   }
  }
 }
}
