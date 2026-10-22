package com.ethk3.novaonepro;
import java.net.URL;import java.util.*;import java.nio.file.*;import java.security.*;
/** Verifies loaded production owner bytecode comes from the locked release tree, not host/test overlays. */
public final class FinalMatrixClassOriginsR75 {
 public static void main(String[] args)throws Exception{
  Path expected=Paths.get(args[0]).toRealPath();
  for(String name:new String[]{"com.ethk3.novaonepro.DatabaseCoordinator","com.ethk3.novaonepro.RuntimeResourceAuditR927","com.ethk3.novaonepro.r952.MatrixRegistrationR75","com.ethk3.novaonepro.r952.MatrixSnapshotBusR952","com.ethk3.novaonepro.r952.MatrixSourceRouterCP12"}){
   Class<?> type=Class.forName(name,false,FinalMatrixClassOriginsR75.class.getClassLoader());Path actual=Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
   if(!actual.equals(expected))throw new AssertionError("PRODUCT_SHADOW:"+name+":"+actual);
   String resource=name.replace('.','/')+".class";List<URL> urls=Collections.list(type.getClassLoader().getResources(resource));if(urls.size()!=1)throw new AssertionError("DUPLICATE_PRODUCT_CLASS:"+name+":"+urls);
   byte[] digest=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(expected.resolve(resource)));StringBuilder hash=new StringBuilder();for(byte b:digest)hash.append(String.format(java.util.Locale.US,"%02x",b&255));
   System.out.println("RELEASE_CLASS_ONLY "+name+" sha256="+hash+" origin="+actual);
  }
 }
}
