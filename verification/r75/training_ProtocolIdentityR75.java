package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.io.File;
import java.util.*;
public final class training_ProtocolIdentityR75 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(db);File root=new File(a[2]);root.mkdirs();
   HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));
   HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));
   RealtimePressureGateR811.resetForTests();Map<String,Object> results=new TreeMap<>();
   try(Cursor c=db.rawQuery("SELECT ast,candidate_id,action_id,family FROM research_family_creation_cp12 ORDER BY family",null)){
    while(c.moveToNext())try(DevelopmentProductionCP12.PreparedProtocol p=DevelopmentProductionCP12.prepare(s,CandidateAstR952.parse(c.getString(0)),c.getString(1),c.getString(2),"202610130324")){
     Map<String,Object> row=new TreeMap<>();row.put("protocolJson",p.protocolJson);row.put("protocolHash",p.protocolHash);row.put("inputHash",p.inputHash);row.put("inputLastIssue",p.lastIssue);row.put("seed",Long.toString(p.seed));
     Map<String,Object> body=StrictCanonicalJson.readContextObject(p.protocolJson);
     if(!p.lastIssue.equals(body.get("sourceIssue"))||body.containsKey("developmentCutoff"))throw new AssertionError("ORDINARY_PROTOCOL_CHANGED");
     results.put(c.getString(3),row);
    }
   }
   if(results.size()!=11)throw new AssertionError("REPORT_FAMILIES_MISSING");
   Files.writeString(Path.of(a[4]),StrictCanonicalJson.writeContext(results));
   System.out.println("PASS ALL_11_ORDINARY_FAMILY_PROTOCOLS");
  }
 }
}
