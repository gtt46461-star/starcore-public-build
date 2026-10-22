package com.ethk3.novaonepro;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.lang.reflect.*;
/** Entire release product bytecode; only HTTP and Android storage transports are synthetic. */
public final class LegacyDualApiConsumerR995 {
 static int checks,primary,backup;static String selectedKey,mode;
 static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why+" check="+checks);}
 static void bind(PrivateApiCredentialCP12 owner)throws Exception{Field f=KeyProvider.class.getDeclaredField("credential");f.setAccessible(true);f.set(null,owner);}
 static String payload(long timestamp){return "{\"info\":\"success\",\"data\":[{\"betNo\":\"202610050100\",\"openNumber\":\"1,2,3,4,5,6,7,8,9,10\",\"openTime\":"+timestamp+"}]}";}
 static void installTransport(){URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){
  protected URLConnection openConnection(URL url){return new HttpURLConnection(url){
   public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
   public int getResponseCode(){
    need(url.getPath().equals("/data"),"actual data path");
    need(url.getQuery().contains("code=Binance1-PK10"),"existing game identity");
    need(url.getQuery().contains("key="+selectedKey),"actual selected credential in request");
    if(url.getHost().equals("api.duoqu.online")){primary++;return mode.equals("http")?502:200;}
    need(url.getHost().equals("api1.duoqu.online"),"actual backup host");backup++;return 200;
   }
   public InputStream getInputStream(){return new ByteArrayInputStream(payload(System.currentTimeMillis()-(url.getHost().equals("api.duoqu.online")?3600000:0)).getBytes(StandardCharsets.UTF_8));}
   public InputStream getErrorStream(){return new ByteArrayInputStream("synthetic primary unavailable".getBytes(StandardCharsets.UTF_8));}
  };}
 }:null);}
 public static void main(String[]args)throws Exception{
  mode=args[0];File root=new File(args[1]);root.mkdirs();bind(null);
  String embedded=BuildConfig.DUOQU_API_KEY_A+BuildConfig.DUOQU_API_KEY_B;
  need(!embedded.isEmpty()&&KeyProvider.getApiKey().equals(embedded)&&KeyProvider.isKeyReady(),"release default ready before private startup");
  need(KeyProvider.usesReleaseDefault(),"release source is explicit");
  PrivateCredentialConsumerR988.Store store=new PrivateCredentialConsumerR988.Store(new File(root,"override.sealed"));
  PrivateApiCredentialCP12 owner=new PrivateApiCredentialCP12(store);bind(owner);
  need(KeyProvider.isKeyReady()&&KeyProvider.getApiKey().equals(embedded),"async private loading cannot disable legacy fetch");owner.load();
  need(owner.state().equals("KEY_NOT_CONFIGURED")&&KeyProvider.isKeyReady(),"missing override keeps default available");
  String override="SYNTHETICOVERRIDE995";owner.configure(override.toCharArray());
  need(KeyProvider.getApiKey().equals(override)&&!KeyProvider.usesReleaseDefault(),"configured device override has priority");
  need(!store.read().contains(override),"device override remains encrypted");
  String sanitized=SafeLog.sanitize(embedded+" "+override);
  need(!sanitized.contains(embedded)&&!sanitized.contains(override),"both active and inactive credentials redacted");
  PrivateApiCredentialCP12 reloaded=new PrivateApiCredentialCP12(store);reloaded.load();bind(reloaded);
  need(KeyProvider.getApiKey().equals(override),"reload retains encrypted override");
  Files.writeString(store.file.toPath(),"invalid synthetic envelope");reloaded.load();
  need(reloaded.state().equals("KEY_STORE_LOAD_FAILED")&&KeyProvider.getApiKey().equals(embedded),"broken override retains default and original store failure");
  need(KeyProvider.maskedStatus().contains("KEY_STORE_LOAD_FAILED")&&!KeyProvider.maskedStatus().contains(embedded),"truthful source diagnostic without secret");
  selectedKey=embedded;installTransport();Models.ApiResult result=new ApiClient().fetchLatest("202610050100");
  need(result.success&&result.latestDraw!=null&&result.fromBackup,"real ApiClient governance and parser accept fresh backup");
  need(primary>0&&backup>0,"primary rejection really reaches backup");
  need(result.latestDraw.openTime>System.currentTimeMillis()-60000,"stale primary never becomes current draw");
  System.out.println("PASS mode="+mode+" checks="+checks+" primaryCalls="+primary+" backupCalls="+backup+" REAL_PRODUCT_SYNTHETIC_HTTP NO_PROVIDER_NETWORK");
 }
}
