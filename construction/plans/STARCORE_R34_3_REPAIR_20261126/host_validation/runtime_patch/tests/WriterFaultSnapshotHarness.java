package com.ethk3.novaonepro;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.util.*;
public final class WriterFaultSnapshotHarness {
 static final String HASH="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 static Map<String,Object> project(String raw,WriterFaultSnapshotViewR343.Anchor a){return WriterFaultSnapshotViewR343.project(raw,99,HASH,"release","source","db",a);}
 public static void main(String[] args){
  Map<String,Object> sample=new LinkedHashMap<>();sample.put("format","R342_MAIN_WRITER_FAULT_PROJECTION_1");
  sample.put("releaseId","release");sample.put("sourceCommit","source");sample.put("databaseIdentity","db");
  sample.put("bootId","boot");sample.put("producerPid",123);sample.put("processEpoch","epoch");
  sample.put("sampleElapsedMs",1000L);sample.put("validUntilElapsedMs",13000L);sample.put("pending",25);
  String original=JsonR27.write(sample);Map<String,Object> envelope=new LinkedHashMap<>();
  envelope.put("original",original);envelope.put("sha256",StatsR27.sha256(original));String raw=JsonR27.write(envelope);
  Map<String,Object> absent=project(raw,null);check("UNKNOWN".equals(absent.get("observationState"))&&absent.get("pending")==null,"no clock no fresh pending");
  check(original.equals(absent.get("historicalOriginalJson")),"preserve exact historical bytes");
  Map<String,Object> fresh=project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","boot","db",2000L,3000L));
  check("OBSERVED_AS_OF_SNAPSHOT_INTERVAL".equals(fresh.get("observationState"))&&((Number)fresh.get("pending")).intValue()==25,"independent bracket fresh");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","boot","db",14000L,15000L)).get("observationState")),"expired at snapshot");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","boot","db",12000L,14000L)).get("observationState")),"bracket crosses expiry");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","other-boot","db",2000L,3000L)).get("observationState")),"different boot");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","boot","db",3000L,2000L)).get("observationState")),"reversed pin interval");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"release","source","boot","db",500L,3000L)).get("observationState")),"cannot anchor to future sample");
  check("UNKNOWN".equals(project(raw,new WriterFaultSnapshotViewR343.Anchor(HASH,"other-release","source","boot","db",2000L,3000L)).get("observationState")),"release mismatch");
  System.out.println("PASS: 9 snapshot freshness/clock/identity/historical preservation checks; actual report passes null anchor");
 }
}
