package com.ethk3.novaonepro;
import java.lang.reflect.*;
public final class SeedCredentialIndependenceR994 {
 public static void main(String[]args)throws Exception {
  if(args.length>0){
   PrivateApiCredentialCP12 owner=new PrivateApiCredentialCP12(new PrivateApiCredentialCP12.Storage(){public String read(){return "";}public void write(String x){}public byte[] encrypt(byte[] x){return x;}public byte[] decrypt(byte[] x){return x;}});
   Field key=PrivateApiCredentialCP12.class.getDeclaredField("key");key.setAccessible(true);key.set(owner,args[0]);
   Field credential=KeyProvider.class.getDeclaredField("credential");credential.setAccessible(true);credential.set(null,owner);
  }
  System.out.println(V51ArtifactBundleManifest.BUNDLE_HASH);
  Models.ApiResult r=Models.ApiResult.fail("KEY_NOT_READY","KEY_NOT_CONFIGURED|PRIVATE_CREDENTIAL_ABSENT");
  if(!ApiClient.failureSummary(r).contains("PRIVATE_CREDENTIAL_ABSENT"))throw new AssertionError("failed response lost");
 }
}
