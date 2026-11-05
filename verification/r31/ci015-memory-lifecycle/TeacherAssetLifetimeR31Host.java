package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.LegacyTeacherOwnerR26;
import android.content.Context;
import android.content.res.AssetManager;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
/** HOST boundary. Actual immutable125 repository asset/VM; no teacher or memory-probe substitute. */
public final class TeacherAssetLifetimeR31Host {
 static int assertions;static void need(boolean c,String m){assertions++;if(!c)throw new AssertionError(m);}
 static byte[] codec(OnlineModelsSU2.Model model)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();model.write(new DataOutputStream(o));return o.toByteArray();}
 public static void main(String[] args)throws Exception{
  MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();need(Runtime.getRuntime().maxMemory()==512L*1024*1024,"unchanged512MiB");
  Context context=new Context(new File(args[0]));AssetManager assets=context.getAssets();LegacyTeacherOwnerR26.register(context);
  Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL125_ARCHIVE_VM_RELOAD_HOST_BOUNDARY_NOT_ANDROID_NOT_NATURAL_QUALITY");result.put("inputRole","SYNTHETIC_CAUSAL_ENGINEERING_FRAMES");
  List<String> roots=new ArrayList<>();for(Field f:LegacyTeacherOwnerR26.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())){f.setAccessible(true);if(f.get(null)==context)roots.add(f.getName());}
  result.put("retainedContextFields",roots);result.put("binding",FamilyRegistryR26.legacyBinding());long metadata=((Number)LegacyTeacherOwnerR26.memorySnapshotR31().get("registryMetadataDeclaredBytes")).longValue();
  long[] issues=new long[60],open=new long[60],available=new long[60];int[][] draws=new int[60][10];for(int i=0;i<60;i++){issues[i]=202610270001L+i;open[i]=1793059200000L+i*60000;available[i]=open[i]+1000;for(int p=0;p<10;p++)draws[i][p]=(p+i)%10+1;}
  byte[] trained;Map<String,Object> audit;
  try(DistillationFamilyR26 student=(DistillationFamilyR26)FamilyRegistryR26.build("F23")){
   try{LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);audit=LegacyTeacherOwnerR26.auditCurrentFrame();student.update(draws[40]);trained=codec(student);}finally{LegacyTeacherOwnerR26.leave();}
  }
  need("ORIGINAL_VM_EXECUTED".equals(audit.get("availability")),"original unchanged VM executes actual asset");need(((Number)LegacyTeacherOwnerR26.memorySnapshotR31().get("studentReferences")).longValue()==0,"student actualclose ends reference");need(((Number)LegacyTeacherOwnerR26.memorySnapshotR31().get("frameReferences")).longValue()==0,"frame finally actually leaves");need(LegacyTeacherOwnerR26.releaseUnusedOwnerR31(),"VM retires after actual consumer exit");
  result.put("originalAudit",audit);result.put("trainedCodecSha256",StatsR27.sha256(trained));result.put("beforeReloadAssetIo",assets.snapshotR31());need(((Number)assets.snapshotR31().get("active")).intValue()==0,"initial actualFileInputStreams close");
  // Deterministic HOST caller-lifetime boundary: manager's real file source remains valid. No collection request.
  context.retireR31();System.out.println("BEFORE_RELOAD "+JsonR27.write(result));
  try(DistillationFamilyR26 restored=(DistillationFamilyR26)FamilyRegistryR26.build("F23")){
   restored.read(new DataInputStream(new ByteArrayInputStream(trained)));need(Arrays.equals(trained,codec(restored)),"full trained model codec restored bitforbit");
   try{LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);need(audit.equals(LegacyTeacherOwnerR26.auditCurrentFrame()),"reloaded originalVM exact causal output/digests");}finally{LegacyTeacherOwnerR26.leave();}
  }
  need(roots.isEmpty(),"static Context must not outlive caller");need(((Number)LegacyTeacherOwnerR26.memorySnapshotR31().get("registryMetadataDeclaredBytes")).longValue()==metadata,"fixed binding no duplicate metadata lease");need(LegacyTeacherOwnerR26.releaseUnusedOwnerR31(),"reloaded VM retires after usersclose");need(((Number)assets.snapshotR31().get("active")).intValue()==0,"reload descriptors allclosed");need(((Number)assets.snapshotR31().get("opened")).intValue()>=4,"reload opens original archive through stable manager");need(((Number)((Map<?,?>)memory.snapshot().get("optionalHeapRecovery")).get("attemptCount")).longValue()==0,"no explicit collector request");
  result.put("assetIo",assets.snapshotR31());result.put("finalOwner",LegacyTeacherOwnerR26.memorySnapshotR31());result.put("assertions",assertions);result.put("explicitGc",false);result.put("fakeProbe",false);result.put("device","NOT_RUN");result.put("naturalQuality","NOT_EVALUATED");System.out.println("PASS "+JsonR27.write(result));
 }
}
